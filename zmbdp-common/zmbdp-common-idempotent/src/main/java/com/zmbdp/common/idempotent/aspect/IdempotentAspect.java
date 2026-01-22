package com.zmbdp.common.idempotent.aspect;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.IdempotentConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.idempotent.annotation.Idempotent;
import com.zmbdp.common.idempotent.enums.IdempotentMode;
import com.zmbdp.common.redis.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性切面
 * 基于 AOP 实现接口幂等性校验
 *
 * @author 稚名不带撇
 */
@Slf4j
@Aspect
@Component
@RefreshScope
public class IdempotentAspect {

    /**
     * 幂等性 Redis 上默认过期时间（秒）
     */
    public static final long IDEMPOTENT_EXPIRE_TIME_DEFAULT = 300L;

    /**
     * 幂等性状态：正在执行中
     */
    private static final String STATUS_PROCESSING = "PROCESSING";

    /**
     * 幂等性状态：执行成功
     */
    private static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * 幂等性状态：执行失败
     */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 强幂等模式：等待结果的最大重试次数（默认值）
     */
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    /**
     * 强幂等模式：每次重试的等待时间（毫秒，默认值）
     */
    private static final long DEFAULT_RETRY_INTERVAL_MS = 100;

    /**
     * SpEL 表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名发现器（用于获取方法参数名）
     */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Redis 服务
     */
    @Autowired
    private RedisService redisService;

    /**
     * 环境配置（用于动态读取配置值，支持配置刷新）
     */
    @Autowired
    private Environment environment;

    /**
     * 切点：标记了 <code>@Idempotent</code> 注解的方法
     */
    @Pointcut("@annotation(com.zmbdp.common.idempotent.annotation.Idempotent)")
    public void idempotentPointcut() {
    }

    /**
     * 环绕通知：在方法执行前后进行幂等性校验
     *
     * <p>支持两种模式：</p>
     * <ul>
     *     <li><b>防重模式（<code>returnCachedResult=false</code>）</b>：重复请求直接报错</li>
     *     <li><b>强幂等模式（<code>returnCachedResult=true</code>）</b>：重复请求返回第一次的结果</li>
     * </ul>
     *
     * @param joinPoint  连接点
     * @param idempotent 幂等性注解
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("idempotentPointcut() && @annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 先从 SpEL表达式、Http请求头、Http请求参数、RabbitMQ消息头 里面获取 Token，没有的话就直接报错
        String idempotentToken = getIdempotentToken(joinPoint, idempotent);
        // 如果说 Token 是空，那么就根据不同的场景报不同的错
        if (StringUtil.isEmpty(idempotentToken)) {
            boolean isMqConsumer = isMqConsumer(joinPoint);
            if (isMqConsumer) {
                // MQ 场景直接丢弃消息，不重新入队
                throw createAmqpRejectAndDontRequeueException("幂等性 Token 不能为空");
            } else {
                // 说明是 Http 场景的
                throw new ServiceException("幂等性 Token 不能为空", ResultCode.INVALID_PARA.getCode());
            }
        }

        // 拼接 Redis key，先从 Nacos 上获取，如果没有的话就是默认值
        String keyPrefix = environment.getProperty(
                IdempotentConstants.NACOS_IDEMPOTENT_KEY_PREFIX_PREFIX,
                String.class, IdempotentConstants.IDEMPOTENT_KEY_PREFIX
        );
        String redisKey = keyPrefix + idempotentToken;

        // 过期时间优先级：注解显示传参的值 > 全局配置的值 > 默认值（300s）
        long globalExpireTime = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_EXPIRE_TIME_PREFIX, Long.class, IDEMPOTENT_EXPIRE_TIME_DEFAULT);
        long expireTime = (idempotent.expireTime() == IDEMPOTENT_EXPIRE_TIME_DEFAULT) ? globalExpireTime : idempotent.expireTime();

        // 判断是否启用强幂等模式，优先级：注解显示传参的值 > 全局配置的值 > 默认值（false）
        boolean globalReturnCachedResult = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_RETURN_CACHED_RESULT_PREFIX, Boolean.class, false);
        boolean returnCachedResult = determineReturnCachedResult(idempotent.returnCachedResult(), globalReturnCachedResult);
        // 如果是 Http 请求，然后还要启动强幂等模式，就警告一下用户说很消耗性能
        if (!isMqConsumer(joinPoint) && returnCachedResult) {
            log.warn(
                    "Http 场景启用了 returnCachedResult = true（强幂等模式），可能导致 Tomcat 线程阻塞（Thread.sleep 轮询等待结果），建议仅在 MQ 场景使用，Token = {}",
                    idempotentToken
            );
        }

        // 看看是不是强幂等模式，是的话先尝试从缓存拿结果，有的话就直接返回
        if (returnCachedResult) {
            String resultKey = redisKey + ":result";
            Object cachedResult = getCachedResult(joinPoint, resultKey);
            if (cachedResult != null) {
                log.info("强幂等模式 - 返回缓存结果，Token: {}", idempotentToken);
                return cachedResult;
            }
        }

        // 强幂等模式下，最大重试次数
        int maxRetries = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_MAX_RETRY_COUNT_PREFIX, Integer.class, DEFAULT_MAX_RETRY_COUNT);
        // 先从 Redis 中获取重复次数
        String retryCountKey = redisKey + ":retry:count";
        boolean lockAcquired = false; // 锁是否获取成功

        while (!lockAcquired) {
            // 先看看重试了几次，没有的话就设置成 0
            // 有的话如果超过 Nacos 上设置的最大次数了，就抛出异常
            Integer currentRetryCount = redisService.getCacheObject(retryCountKey, Integer.class);
            if (currentRetryCount == null) {
                currentRetryCount = 0;
            }
            // 检查有没有超过最大次数，有的话就抛异常
            if (currentRetryCount >= maxRetries) {
                log.error("防重模式 - 重试次数超过限制，无法获取锁，Token: {}, 重试次数: {}/{}", idempotentToken, currentRetryCount, maxRetries);
                redisService.deleteObject(retryCountKey);
                throw new ServiceException("请求处理失败，请稍后重试", ResultCode.ERROR.getCode());
            }

            // 然后用 SETNX 获取锁，设置 PROCESSING 状态，表示正在执行
            Boolean success = redisService.setCacheObjectIfAbsent(redisKey, STATUS_PROCESSING, expireTime, TimeUnit.SECONDS);
            if (!success) {
                // Token已存在，说明有并发，检查当前状态决定怎么处理
                String currentStatus = redisService.getCacheObject(redisKey, String.class);

                if (returnCachedResult) {
                    // 强幂等模式：根据状态返回结果或等待
                    Object result = handleStrongIdempotentMode(joinPoint, redisKey, currentStatus, idempotentToken);
                    if (result != null) {
                        return result;
                    }
                } else {
                    // 防重模式：根据状态决定是否允许重试
                    boolean shouldContinue = handlePreventDuplicateMode(
                            joinPoint, redisKey, retryCountKey, currentStatus,
                            currentRetryCount, maxRetries, expireTime, idempotentToken, idempotent
                    );
                    if (shouldContinue) {
                        // 退避等待，避免忙等打爆 Redis 和 CPU，指数退避最大 300ms
                        // 第一次延迟 50ms，第二次 100ms，第三次 150ms，以此类推，最大延迟 300ms
                        try {
                            Thread.sleep(Math.min(50L * (currentRetryCount + 1), 300));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.warn("防重模式 - 退避等待被中断，Token: {}", idempotentToken);
                            throw new ServiceException("请求处理被中断", ResultCode.ERROR.getCode());
                        }
                        continue; // 继续下一轮循环
                    }
                }
            } else {
                // 获取到锁了，跳出循环去执行业务方法
                lockAcquired = true;
            }
        }


        try {
            // 执行业务方法
            Object result = joinPoint.proceed();

            // 执行成功，更新状态为 SUCCESS，清除重试计数
            redisService.setCacheObject(redisKey, STATUS_SUCCESS, expireTime, TimeUnit.SECONDS);
            redisService.deleteObject(retryCountKey);

            // 强幂等模式的话，就把结果缓存起来，后续相同请求方便直接返回
            if (returnCachedResult) {
                cacheResult(redisKey + ":result", result, expireTime);
            }

            return result;
        } catch (Exception e) {
            // 执行失败，更新状态为 FAILED，不删除 Token 让其他等待的请求知道失败了
            redisService.setCacheObject(redisKey, STATUS_FAILED, expireTime, TimeUnit.SECONDS);
            // 强幂等模式删除结果缓存，因为失败了
            if (returnCachedResult) {
                redisService.deleteObject(redisKey + ":result");
            }
            throw e;
        }
    }

    /**
     * 获取幂等性 Token
     * 优先级：<code>SpEL</code>表达式 > Http请求头 > Http请求参数 > RabbitMQ消息头
     *
     * @param joinPoint  连接点
     * @param idempotent 幂等性注解
     * @return 幂等性 Token
     */
    private String getIdempotentToken(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        // 优先级 1：从 SpEL表达式 获取，适合 MQ 场景
        if (StringUtil.isNotEmpty(idempotent.tokenExpression())) {
            String token = getTokenFromExpression(joinPoint, idempotent.tokenExpression());
            if (StringUtil.isNotEmpty(token)) {
                return token;
            }
        }

        // 优先级 2：从 Http 请求获取
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 先尝试从请求头拿
            String token = request.getHeader(idempotent.headerName());
            if (StringUtil.isNotEmpty(token)) {
                return token;
            }

            // 请求头没有，如果允许的话再从参数拿
            if (idempotent.allowParam()) {
                token = request.getParameter(idempotent.paramName());
                if (StringUtil.isNotEmpty(token)) {
                    return token;
                }
            }
        }

        // 优先级 3：从 MQ 消息头获取
        String token = getTokenFromRabbitMQMessage(joinPoint, idempotent.headerName());
        if (StringUtil.isNotEmpty(token)) {
            return token;
        }

        return null;
    }

    /**
     * 通过 <code>SpEL</code> 表达式从方法参数中获取 Token
     *
     * @param joinPoint       连接点
     * @param tokenExpression <code>SpEL</code> 表达式
     * @return 幂等性 Token
     */
    private String getTokenFromExpression(ProceedingJoinPoint joinPoint, String tokenExpression) {
        try {
            // 获取方法签名和参数
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            Object[] args = joinPoint.getArgs();

            // 创建 SpEL 上下文
            EvaluationContext context = new StandardEvaluationContext();

            // 把方法参数设置到上下文，支持 参数名 和 args[index] 两种方式
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            context.setVariable("args", args); // 也支持用 args[0] 这种方式访问

            // 解析表达式并获取值
            Expression expression = parser.parseExpression(tokenExpression);
            Object value = expression.getValue(context);

            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("从 SpEL 表达式获取 Token 失败: {}, 表达式: {}", e.getMessage(), tokenExpression);
            return null;
        }
    }

    /**
     * 从 RabbitMQ 消息头中获取 Token
     * 支持三种方式：
     * <ol>
     *     <li>从方法参数中的 <code>Message</code> 对象获取（<code>org.springframework.amqp.core.Message</code>）</li>
     *     <li>从 Spring Messaging 的 <code>Message</code> 对象获取（<code>org.springframework.messaging.Message</code>）</li>
     *     <li>从 <code>RequestContextHolder</code> 获取（如果 Spring AMQP 设置了消息上下文）</li>
     * </ol>
     *
     * @param joinPoint  连接点
     * @param headerName 消息头名称
     * @return 幂等性 Token
     */
    private String getTokenFromRabbitMQMessage(ProceedingJoinPoint joinPoint, String headerName) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 先尝试从 AMQP Message 获取，用反射避免编译依赖
                try {
                    Class<?> amqpMessageClass = Class.forName("org.springframework.amqp.core.Message");
                    for (Object arg : args) {
                        if (amqpMessageClass.isInstance(arg)) {
                            // 反射调用 getMessageProperties().getHeaders().get(headerName)
                            Object messageProperties = amqpMessageClass.getMethod("getMessageProperties").invoke(arg);
                            Object headers = messageProperties.getClass().getMethod("getHeaders").invoke(messageProperties);
                            Object token = ((java.util.Map<?, ?>) headers).get(headerName);
                            if (token != null) {
                                return token.toString();
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // 没引入 spring-amqp，继续尝试其他方式
                }

                // 再尝试从 Spring Messaging Message 获取
                try {
                    Class<?> messagingMessageClass = Class.forName("org.springframework.messaging.Message");
                    for (Object arg : args) {
                        if (messagingMessageClass.isInstance(arg)) {
                            // 反射调用getHeaders().get(headerName)
                            Object headers = messagingMessageClass.getMethod("getHeaders").invoke(arg);
                            Object token = ((java.util.Map<?, ?>) headers).get(headerName);
                            if (token != null) {
                                return token.toString();
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // 类不存在，忽略
                }
            }
        } catch (Exception e) {
            log.debug("从 RabbitMQ 消息头获取 Token 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 缓存方法执行结果（强幂等模式）
     *
     * @param resultKey  结果缓存 Key
     * @param result     方法执行结果
     * @param expireTime 过期时间（秒）
     */
    private void cacheResult(String resultKey, Object result, long expireTime) {
        try {
            redisService.setCacheObject(resultKey, result, expireTime, TimeUnit.SECONDS);
            log.debug("强幂等模式 - 缓存方法结果，Key: {}", resultKey);
        } catch (Exception e) {
            log.warn("强幂等模式 - 缓存方法结果失败: {}", e.getMessage());
        }
    }

    /**
     * 从缓存获取方法执行结果（强幂等模式）
     *
     * @param joinPoint 连接点
     * @param resultKey 结果缓存 Key
     * @return 缓存的结果，如果不存在返回 null
     */
    private Object getCachedResult(ProceedingJoinPoint joinPoint, String resultKey) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> returnType = method.getReturnType();

            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                return null;
            }

            return redisService.getCacheObject(resultKey, returnType);
        } catch (Exception e) {
            log.warn("强幂等模式 - 从缓存获取结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 等待结果（强幂等模式）
     * <p>
     * 当检测到正在执行中或状态未知时，轮询等待结果。
     * 重要：此方法不会删除 <code>token</code>，只有持有锁的线程才能删除 <code>token</code>。
     * </p>
     * <p>
     * <b>线程阻塞风险说明：</b>
     * <ul>
     *     <li>在 Http 场景下，Thread.sleep 会占用 Tomcat 线程，高并发时可能导致线程池耗尽、RT 飙升</li>
     *     <li>工程建议：强幂等模式默认只用于 MQ 场景，Http 场景建议直接返回 "处理中" 或 409/202 状态码</li>
     *     <li>当前实现：可用，但在高并发 Http 场景下不够优雅</li>
     * </ul>
     * </p>
     *
     * @param joinPoint    连接点
     * @param redisKey     幂等性 Redis Key
     * @param resultKey    结果缓存 Key
     * @param isMqConsumer 是否是 MQ 消费者场景
     * @return 缓存的结果，如果等待超时或失败，返回默认值或null
     */
    private Object waitForResult(ProceedingJoinPoint joinPoint, String redisKey, String resultKey, boolean isMqConsumer) {
        // 从配置中心读取重试次数和间隔，支持动态刷新
        int maxRetryCount = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_MAX_RETRY_COUNT_PREFIX, Integer.class, DEFAULT_MAX_RETRY_COUNT);
        long retryIntervalMs = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_RETRY_INTERVAL_MS_PREFIX, Long.class, DEFAULT_RETRY_INTERVAL_MS);

        // 轮询等待结果，最多重试 maxRetryCount 次
        for (int i = 0; i < maxRetryCount; i++) {
            try {
                Thread.sleep(retryIntervalMs); // 等待一段时间再检查

                // 检查当前状态
                String status = redisService.getCacheObject(redisKey, String.class);

                if (STATUS_SUCCESS.equals(status)) {
                    // 执行成功了，从缓存拿结果
                    Object cachedResult = getCachedResult(joinPoint, resultKey);
                    if (cachedResult != null) {
                        log.info("强幂等模式 - 等待后获取到结果，Token: {}", redisKey);
                        return cachedResult;
                    }
                } else if (STATUS_FAILED.equals(status)) {
                    // 执行失败了
                    if (isMqConsumer) {
                        // MQ 场景就静默处理，避免消息重新入队
                        log.info("强幂等模式 - MQ 检测到执行失败，跳过消费，Token: {}", redisKey);
                        return getDefaultReturnValue(joinPoint);
                    } else {
                        log.warn("强幂等模式 - Http 检测到执行失败，等待超时，Token: {}", redisKey);
                        throw new ServiceException("请求处理失败，请稍后重试", ResultCode.ERROR.getCode());
                    }
                } else if (STATUS_PROCESSING.equals(status)) {
                    // 还在执行中，继续等
                    log.debug("强幂等模式 - 仍在执行中，继续等待，Token: {}, 重试次数: {}/{}", redisKey, i + 1, maxRetryCount);
                    continue;
                } else {
                    // 状态未知或已过期
                    log.warn("强幂等模式 - 状态未知或已过期: {}，Token: {}", status, redisKey);
                    if (isMqConsumer) {
                        return getDefaultReturnValue(joinPoint);
                    } else {
                        throw new ServiceException("请求处理超时，请稍后重试", ResultCode.ERROR.getCode());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("强幂等模式 - 等待被中断，Token: {}", redisKey);
                break;
            } catch (ServiceException e) {
                throw e;
            }
        }

        // 等待超时了
        log.warn("强幂等模式 - 等待结果超时，Token: {}", redisKey);
        if (isMqConsumer) {
            return getDefaultReturnValue(joinPoint);
        } else {
            throw new ServiceException("请求处理超时，请稍后重试", ResultCode.ERROR.getCode());
        }
    }

    /**
     * 判断是否启用强幂等模式（三态设计）
     * <p>
     * 优先级：注解显式指定（TRUE/FALSE） > 全局配置 > 默认值（false，即防重模式）
     * </p>
     *
     * @param annotationMode 注解中指定的模式
     * @param globalConfig   全局配置值
     * @return true - 启用强幂等模式；false - 使用防重模式
     */
    private boolean determineReturnCachedResult(IdempotentMode annotationMode, boolean globalConfig) {
        if (annotationMode == IdempotentMode.TRUE) {
            return true; // 注解明确指定开启
        } else if (annotationMode == IdempotentMode.FALSE) {
            return false; // 注解明确指定关闭
        } else {
            return globalConfig; // 默认用全局配置
        }
    }

    /**
     * 判断是否是 MQ 消费者场景
     * <p>
     * 通过检查是否存在 <code>HttpServletRequest</code> 来判断：
     * <ul>
     *     <li>如果存在 <code>HttpServletRequest</code>，说明是 Http 请求场景</li>
     *     <li>如果不存在 <code>HttpServletRequest</code>，说明是 MQ 消费者场景</li>
     * </ul>
     * </p>
     *
     * @param joinPoint 连接点
     * @return true - MQ 消费者场景；false - Http 请求场景
     */
    private boolean isMqConsumer(ProceedingJoinPoint joinPoint) {
        // 通过判断有没有 HttpServletRequest 来区分 Http 和 MQ 场景
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null; // 没有就是 MQ 发的
    }

    /**
     * 获取方法的默认返回值
     * <p>
     * 根据方法的返回类型返回合适的默认值：
     * <ul>
     *     <li>void 类型：返回 null</li>
     *     <li>基本类型：返回对应的默认值（0, false, 0.0 等）</li>
     *     <li>对象类型：返回 null</li>
     * </ul>
     * </p>
     * <p>
     * 主要用于强幂等模式下，MQ 消费者检测到执行失败或等待超时时，返回默认值以避免消息重新入队。
     * </p>
     *
     * @param joinPoint 连接点
     * @return 方法的默认返回值
     */
    private Object getDefaultReturnValue(ProceedingJoinPoint joinPoint) {
        // 获取方法返回类型，返回对应的默认值
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getMethod().getReturnType();
        // 如果方法没有返回值，就返回 null
        if (void.class.equals(returnType) || Void.class.equals(returnType)) {
            return null;
        }
        // 基本类型返回默认值，对象类型返回 null
        if (returnType.isPrimitive()) {
            return switch (returnType.getName()) {
                case "boolean" -> false;
                case "byte" -> (byte) 0;
                case "short" -> (short) 0;
                case "int" -> 0;
                case "long" -> 0L;
                case "float" -> 0.0f;
                case "double" -> 0.0d;
                case "char" -> '\u0000';
                default -> null;
            };
        }
        return null;
    }

    /**
     * 原子操作：如果状态等于指定值，则删除 Token
     * <p>
     * 使用 <code>Lua</code> 脚本实现原子操作，避免并发问题。
     * 只有当 <code>Token</code> 的状态等于指定状态时，才会删除 <code>Token</code>。
     * </p>
     *
     * @param redisKey       Redis Key
     * @param expectedStatus 期望的状态值
     * @return true - 删除成功（状态匹配）；false - 删除失败（状态不匹配或键不存在）
     */
    private boolean deleteIfStatusEquals(String redisKey, String expectedStatus) {
        try {
            return redisService.compareAndDelete(redisKey, expectedStatus);
        } catch (Exception e) {
            log.warn("原子删除 Token 失败: {}, 错误: {}", redisKey, e.getMessage());
            return false;
        }
    }

    /**
     * 处理强幂等模式的重复请求
     * <p>
     * 根据当前状态返回缓存结果或等待执行完成
     *
     * @param joinPoint       连接点
     * @param redisKey        Redis Key
     * @param currentStatus   当前状态
     * @param idempotentToken 幂等性 Token
     * @return 缓存的结果，如果应该继续执行则返回 null
     */
    private Object handleStrongIdempotentMode(ProceedingJoinPoint joinPoint, String redisKey, String currentStatus, String idempotentToken) {
        // 先拼接 Redis 的 key
        String resultKey = redisKey + ":result";
        boolean isMqConsumer = isMqConsumer(joinPoint);

        if (STATUS_SUCCESS.equals(currentStatus)) {
            // 执行成功了，从缓存拿结果
            Object cachedResult = getCachedResult(joinPoint, resultKey);
            if (cachedResult != null) {
                log.info("强幂等模式 - 返回缓存结果，Token: {}", idempotentToken);
                return cachedResult;
            } else {
                // 状态是 SUCCESS 但结果不存在，可能过期了，等待一下
                log.warn("强幂等模式 - 状态为 SUCCESS 但结果不存在，可能已过期，Token: {}", idempotentToken);
                return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
            }
        } else if (STATUS_PROCESSING.equals(currentStatus)) {
            // 还在执行中，等待结果
            log.debug("强幂等模式 - 检测到正在执行中，等待结果，Token: {}", idempotentToken);
            return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
        } else if (STATUS_FAILED.equals(currentStatus)) {
            // 执行失败了，也等待一下看看
            log.warn("强幂等模式 - 检测到失败状态，等待后重试，Token: {}", idempotentToken);
            return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
        } else {
            // 状态未知，也等待一下
            log.warn("强幂等模式 - 状态未知: {}，等待结果，Token: {}", currentStatus, idempotentToken);
            return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
        }
    }

    /**
     * 处理防重模式的重复请求
     * <p>
     * 根据当前状态决定是否允许重试或直接拒绝
     *
     * @param joinPoint         连接点
     * @param redisKey          Redis Key
     * @param retryCountKey     重试次数 Key
     * @param currentStatus     当前状态
     * @param currentRetryCount 当前重试次数
     * @param maxRetries        最大重试次数
     * @param expireTime        过期时间（秒）
     * @param idempotentToken   幂等性 Token
     * @param idempotent        幂等性注解
     * @return true - 应该继续循环重试；false - 应该抛出异常（已在方法内处理）
     * @throws Throwable 重复请求异常
     */
    private boolean handlePreventDuplicateMode(
            ProceedingJoinPoint joinPoint, String redisKey, String retryCountKey,
            String currentStatus, int currentRetryCount, int maxRetries, long expireTime,
            String idempotentToken, Idempotent idempotent
    ) throws Throwable {
        boolean isMqConsumer = isMqConsumer(joinPoint);

        if (STATUS_PROCESSING.equals(currentStatus)) {
            // 正在执行中，不允许并发执行
            log.warn("防重模式 - 检测到正在执行中，拒绝重复请求，Token: {}", idempotentToken);
            throwDuplicateRequestException(idempotent.message(), isMqConsumer);
            return false;
        } else if (STATUS_FAILED.equals(currentStatus)) {
            // 业务执行失败，允许重试，用 incr 原子操作增加重试次数
            Long newRetryCountLong = redisService.incr(retryCountKey, 1);
            int newRetryCount;
            if (newRetryCountLong == null || newRetryCountLong < 0) {
                // incr 失败，降级处理
                log.warn("防重模式 - incr 操作失败，回退到非原子操作，Token: {}", idempotentToken);
                newRetryCount = currentRetryCount + 1;
                redisService.setCacheObject(retryCountKey, newRetryCount, expireTime, TimeUnit.SECONDS);
            } else {
                newRetryCount = newRetryCountLong.intValue();
                // 只在第一次 incr 时设置过期时间，避免多个线程同时 incr 导致多次 EXPIRE
                if (newRetryCountLong == 1L) {
                    redisService.expire(retryCountKey, expireTime, TimeUnit.SECONDS);
                }
            }
            log.info("防重模式 - 检测到失败状态，尝试删除 Token 允许重试，Token: {}, 重试次数: {}/{}",
                    idempotentToken, newRetryCount, maxRetries);

            // 用原子操作删除，只有状态是 FAILED 时才删除
            // 注意：删除 key 和下一次 SETNX 之间存在时间窗，其他实例可能插队，但 SETNX 是最终裁决者，这是可接受的竞争窗口
            boolean deleted = deleteIfStatusEquals(redisKey, STATUS_FAILED);
            // 无论删除成功失败都继续循环，删除失败说明状态已改变，下次循环会重新检查状态
            return true;
        } else {
            // 已执行成功或其他状态，直接报错
            log.warn("防重模式 - 重复请求，当前状态: {}，Token: {}", currentStatus, idempotentToken);
            redisService.deleteObject(retryCountKey);
            throwDuplicateRequestException(idempotent.message(), isMqConsumer);
            return false;
        }
    }

    /**
     * 抛出重复请求异常
     * 根据场景选择抛出 MQ 异常或 Http 异常
     *
     * @param message      异常消息
     * @param isMqConsumer 是否是 MQ 消费者场景
     * @throws Throwable 重复请求异常
     */
    private void throwDuplicateRequestException(String message, boolean isMqConsumer) throws Throwable {
        if (isMqConsumer) {
            // MQ 场景：抛出 AmqpRejectAndDontRequeueException，直接丢弃消息，不重新入队
            throw createAmqpRejectAndDontRequeueException(message);
        } else {
            // Http 场景：抛出 ServiceException
            throw new ServiceException(message, ResultCode.INVALID_PARA.getCode());
        }
    }

    /**
     * 创建 <code>AmqpRejectAndDontRequeueException</code> 异常
     * <p>
     * 使用反射方式创建，避免编译时依赖 <code>spring-amqp</code>。
     * 该异常会告诉 <code>RabbitMQ</code> 拒绝消息且不要重新入队，直接丢弃消息。
     * </p>
     *
     * @param message 异常消息
     * @return <code>RuntimeException</code> 异常（如果 <code>AmqpRejectAndDontRequeueException</code> 不存在，则返回 <code>ServiceException</code>）
     */
    private RuntimeException createAmqpRejectAndDontRequeueException(String message) {
        try {
            // 用反射创建异常，避免编译时依赖 spring-amqp
            Class<?> exceptionClass = Class.forName("org.springframework.amqp.AmqpRejectAndDontRequeueException");
            return (RuntimeException) exceptionClass.getConstructor(String.class).newInstance(message);
        } catch (ClassNotFoundException e) {
            // 没有引入 spring-amqp 依赖，用 ServiceException 代替
            log.warn("AmqpRejectAndDontRequeueException 类不存在，使用 ServiceException 代替。建议引入 spring-amqp 依赖以确保消息不重新入队。");
            return new ServiceException(message, ResultCode.INVALID_PARA.getCode());
        } catch (Exception e) {
            // 反射创建失败，降级处理
            log.warn("创建 AmqpRejectAndDontRequeueException 失败: {}", e.getMessage());
            return new ServiceException(message, ResultCode.INVALID_PARA.getCode());
        }
    }
}