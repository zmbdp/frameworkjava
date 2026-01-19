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
     * 切点：标记了 @Idempotent 注解的方法
     */
    @Pointcut("@annotation(com.zmbdp.common.idempotent.annotation.Idempotent)")
    public void idempotentPointcut() {
    }

    /**
     * 环绕通知：在方法执行前后进行幂等性校验
     *
     * <p>支持两种模式：</p>
     * <ul>
     *     <li><b>防重模式（returnCachedResult=false）</b>：重复请求直接报错</li>
     *     <li><b>强幂等模式（returnCachedResult=true）</b>：重复请求返回第一次的结果</li>
     * </ul>
     *
     * @param joinPoint  连接点
     * @param idempotent 幂等性注解
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("idempotentPointcut() && @annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 获取幂等性 Token（支持多种方式：SpEL 表达式、HTTP 请求、MQ 消息头）
        String idempotentToken = getIdempotentToken(joinPoint, idempotent);
        if (StringUtil.isEmpty(idempotentToken)) {
            // 判断是否是 MQ 消费者场景
            boolean isMqConsumer = isMqConsumer(joinPoint);
            if (isMqConsumer) {
                // MQ 场景：抛出 AmqpRejectAndDontRequeueException，直接丢弃消息，不重新入队
                throw createAmqpRejectAndDontRequeueException("幂等性 Token 不能为空");
            } else {
                // HTTP 场景：抛出 ServiceException
                throw new ServiceException("幂等性 Token 不能为空", ResultCode.INVALID_PARA.getCode());
            }
        }

        // 构建 Redis Key（从 Environment 动态读取 Key 前缀，支持配置刷新）
        String keyPrefix = environment.getProperty(
                IdempotentConstants.NACOS_IDEMPOTENT_KEY_PREFIX_PREFIX,
                String.class, IdempotentConstants.IDEMPOTENT_KEY_PREFIX
        );
        String redisKey = keyPrefix + idempotentToken;

        // 获取过期时间（优先级：注解值 > 全局配置 > 默认值 300）
        // 从 Environment 动态读取配置值，支持配置刷新
        long globalExpireTime = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_EXPIRE_TIME_PREFIX, Long.class, 300L);
        // 如果注解中使用默认值 300，则使用全局配置；否则使用注解值
        long expireTime = (idempotent.expireTime() == 300) ? globalExpireTime : idempotent.expireTime();

        // 判断是否启用强幂等模式（三态设计：注解显式指定 > 全局配置 > 默认值）
        // 从 Environment 动态读取配置值，支持配置刷新
        boolean globalReturnCachedResult = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_RETURN_CACHED_RESULT_PREFIX, Boolean.class, false);
        // 根据三态设计决定是否启用强幂等模式
        boolean returnCachedResult = determineReturnCachedResult(idempotent.returnCachedResult(), globalReturnCachedResult);

        // 强幂等模式：尝试从缓存获取结果
        if (returnCachedResult) {
            String resultKey = redisKey + ":result";
            Object cachedResult = getCachedResult(joinPoint, resultKey);
            if (cachedResult != null) {
                log.info("强幂等模式 - 返回缓存结果，Token: {}", idempotentToken);
                return cachedResult;
            }
        }

        // 尝试设置 Redis（如果已存在说明是重复请求）
        // 使用状态值 PROCESSING 标识正在执行中
        // 使用循环处理失败状态的重试逻辑（仅防重模式）
        int maxRetries = 3; // 最多重试3次，避免无限循环
        int retryCount = 0;
        boolean lockAcquired = false;

        while (!lockAcquired && retryCount < maxRetries) {
            Boolean success = redisService.setCacheObjectIfAbsent(redisKey, STATUS_PROCESSING, expireTime, TimeUnit.SECONDS);
            if (!success) {
                // Token 已存在，说明是重复请求，需要检查当前状态
                String currentStatus = redisService.getCacheObject(redisKey, String.class);
                boolean isMqConsumer = isMqConsumer(joinPoint);

                if (returnCachedResult) {
                    // 强幂等模式：根据状态处理（不需要循环，直接返回或等待）
                    if (STATUS_SUCCESS.equals(currentStatus)) {
                        // 执行成功，返回缓存结果
                        String resultKey = redisKey + ":result";
                        Object cachedResult = getCachedResult(joinPoint, resultKey);
                        if (cachedResult != null) {
                            log.info("强幂等模式 - 返回缓存结果，Token: {}", idempotentToken);
                            return cachedResult;
                        } else {
                            // 状态是SUCCESS但结果不存在，可能是过期了，允许重新执行
                            log.warn("强幂等模式 - 状态为SUCCESS但结果不存在，可能已过期，Token: {}", idempotentToken);
                            // 不删除token，让过期时间自然失效，或者等待当前执行完成
                            return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
                        }
                    } else if (STATUS_PROCESSING.equals(currentStatus)) {
                        // 正在执行中，等待结果
                        String resultKey = redisKey + ":result";
                        log.debug("强幂等模式 - 检测到正在执行中，等待结果，Token: {}", idempotentToken);
                        return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
                    } else if (STATUS_FAILED.equals(currentStatus)) {
                        // 执行失败，可以重新执行（但需要先删除token，让下一个请求能获取锁）
                        // 注意：这里不能直接删除，因为可能其他请求正在等待
                        // 应该等待一段时间，如果还是FAILED，说明确实失败了，可以重新执行
                        log.warn("强幂等模式 - 检测到失败状态，等待后重试，Token: {}", idempotentToken);
                        String resultKey = redisKey + ":result";
                        return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
                    } else {
                        // 状态未知或已过期，等待结果或返回默认值
                        log.warn("强幂等模式 - 状态未知: {}，等待结果，Token: {}", currentStatus, idempotentToken);
                        String resultKey = redisKey + ":result";
                        return waitForResult(joinPoint, redisKey, resultKey, isMqConsumer);
                    }
                } else {
                    // 防重模式：检测到重复请求
                    if (STATUS_PROCESSING.equals(currentStatus)) {
                        // 正在执行中，直接报错（不允许并发执行）
                        log.warn("防重模式 - 检测到正在执行中，拒绝重复请求，Token: {}", idempotentToken);
                        if (isMqConsumer) {
                            // MQ 场景：抛出 AmqpRejectAndDontRequeueException，直接丢弃消息，不重新入队
                            throw createAmqpRejectAndDontRequeueException(idempotent.message());
                        } else {
                            // HTTP 场景：抛出 ServiceException
                            throw new ServiceException(idempotent.message(), ResultCode.INVALID_PARA.getCode());
                        }
                    } else if (STATUS_FAILED.equals(currentStatus)) {
                        // 业务执行失败，允许重试（使用原子操作删除Token，避免并发问题）
                        log.info("防重模式 - 检测到失败状态，尝试删除Token允许重试，Token: {}, 重试次数: {}/{}", idempotentToken, retryCount + 1, maxRetries);
                        // 使用原子操作：只有当状态为 FAILED 时才删除，避免并发问题
                        boolean deleted = deleteIfStatusEquals(redisKey, STATUS_FAILED);
                        if (deleted) {
                            // 删除成功，继续循环重新尝试 SETNX
                            retryCount++;
                            continue;
                        } else {
                            // 删除失败（可能状态已改变，如被其他请求删除或状态变为 PROCESSING），重新检查状态
                            // 继续循环，重新检查状态
                            retryCount++;
                            continue;
                        }
                    } else {
                        // 已执行成功（SUCCESS），直接报错
                        log.warn("防重模式 - 重复请求，当前状态: {}，Token: {}", currentStatus, idempotentToken);
                        if (isMqConsumer) {
                            // MQ 场景：抛出 AmqpRejectAndDontRequeueException，直接丢弃消息，不重新入队
                            throw createAmqpRejectAndDontRequeueException(idempotent.message());
                        } else {
                            // HTTP 场景：抛出 ServiceException
                            throw new ServiceException(idempotent.message(), ResultCode.INVALID_PARA.getCode());
                        }
                    }
                }
            } else {
                // SETNX 成功，获取到锁，跳出循环
                lockAcquired = true;
            }
        }

        // 如果重试次数超过限制，抛出异常
        if (!lockAcquired) {
            log.error("防重模式 - 重试次数超过限制，无法获取锁，Token: {}", idempotentToken);
            throw new ServiceException("请求处理失败，请稍后重试", ResultCode.ERROR.getCode());
        }

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 执行成功，更新状态为 SUCCESS
            redisService.setCacheObject(redisKey, STATUS_SUCCESS, expireTime, TimeUnit.SECONDS);

            // 强幂等模式：缓存方法执行结果
            if (returnCachedResult) {
                cacheResult(redisKey + ":result", result, expireTime);
            }

            return result;
        } catch (Exception e) {
            // 方法执行失败，更新状态为 FAILED（不删除token，让其他等待的请求知道失败了）
            // 注意：只有持有锁的线程（SETNX成功的）才能更新状态
            redisService.setCacheObject(redisKey, STATUS_FAILED, expireTime, TimeUnit.SECONDS);
            // 如果开启了强幂等模式，也删除结果缓存
            if (returnCachedResult) {
                redisService.deleteObject(redisKey + ":result");
            }
            throw e;
        }
        // 方法执行成功，Token 保留到过期时间，防止在过期时间内重复提交
    }

    /**
     * 获取幂等性 Token
     * 优先级：SpEL表达式 > HTTP请求头 > HTTP请求参数 > RabbitMQ消息头
     *
     * @param joinPoint  连接点
     * @param idempotent 幂等性注解
     * @return 幂等性 Token
     */
    private String getIdempotentToken(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        // 1. 优先从 SpEL 表达式获取（适用于 MQ 消费者等场景）
        if (StringUtil.isNotEmpty(idempotent.tokenExpression())) {
            String token = getTokenFromExpression(joinPoint, idempotent.tokenExpression());
            if (StringUtil.isNotEmpty(token)) {
                return token;
            }
        }

        // 2. 从 HTTP 请求中获取
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 2.1 从请求头获取
            String token = request.getHeader(idempotent.headerName());
            if (StringUtil.isNotEmpty(token)) {
                return token;
            }

            // 2.2 如果允许从参数获取，且请求头中没有，则从请求参数获取
            if (idempotent.allowParam()) {
                token = request.getParameter(idempotent.paramName());
                if (StringUtil.isNotEmpty(token)) {
                    return token;
                }
            }
        }

        // 3. 从 RabbitMQ 消息头获取（适用于 MQ 消费者）
        String token = getTokenFromRabbitMQMessage(joinPoint, idempotent.headerName());
        if (StringUtil.isNotEmpty(token)) {
            return token;
        }

        return null;
    }

    /**
     * 通过 SpEL 表达式从方法参数中获取 Token
     *
     * @param joinPoint       连接点
     * @param tokenExpression SpEL 表达式
     * @return 幂等性 Token
     */
    private String getTokenFromExpression(ProceedingJoinPoint joinPoint, String tokenExpression) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            Object[] args = joinPoint.getArgs();

            // 创建 SpEL 上下文
            EvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数到上下文（支持 #参数名 和 #args[index] 两种方式）
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            // 支持 #args[0] 方式访问参数
            context.setVariable("args", args);

            // 解析表达式
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
     * 支持两种方式：
     * 1. 从方法参数中的 Message 对象获取（org.springframework.amqp.core.Message）
     * 2. 从 Spring Messaging 的 Message 对象获取（org.springframework.messaging.Message）
     * 3. 从 RequestContextHolder 获取（如果 Spring AMQP 设置了消息上下文）
     *
     * @param joinPoint  连接点
     * @param headerName 消息头名称
     * @return 幂等性 Token
     */
    private String getTokenFromRabbitMQMessage(ProceedingJoinPoint joinPoint, String headerName) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                // 方式1：查找 org.springframework.amqp.core.Message 类型的参数
                try {
                    Class<?> amqpMessageClass = Class.forName("org.springframework.amqp.core.Message");
                    for (Object arg : args) {
                        if (arg != null && amqpMessageClass.isInstance(arg)) {
                            // 使用反射调用 getMessageProperties().getHeaders().get(headerName)
                            Object messageProperties = amqpMessageClass.getMethod("getMessageProperties").invoke(arg);
                            Object headers = messageProperties.getClass().getMethod("getHeaders").invoke(messageProperties);
                            Object token = ((java.util.Map<?, ?>) headers).get(headerName);
                            if (token != null) {
                                return token.toString();
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // AMQP Message 类不存在，继续尝试其他方式
                }

                // 方式2：查找 org.springframework.messaging.Message 类型的参数
                try {
                    Class<?> messagingMessageClass = Class.forName("org.springframework.messaging.Message");
                    for (Object arg : args) {
                        if (arg != null && messagingMessageClass.isInstance(arg)) {
                            // 使用反射调用 getHeaders().get(headerName)
                            Object headers = messagingMessageClass.getMethod("getHeaders").invoke(arg);
                            Object token = ((java.util.Map<?, ?>) headers).get(headerName);
                            if (token != null) {
                                return token.toString();
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Messaging Message 类不存在，继续尝试其他方式
                }
            }

            // 方式3：从 RequestContextHolder 获取（Spring AMQP 可能会设置消息上下文）
            try {
                // 尝试从 RequestContextHolder 获取消息头（如果 Spring AMQP 设置了）
                // 注意：这需要 Spring AMQP 配置支持
                // 由于 Spring AMQP 通常不会将消息头设置到 RequestContextHolder，
                // 所以这种方式可能不适用，但保留作为备用方案
            } catch (Exception e) {
                // 忽略
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
            // 将第一次结果缓存到 Redis，后续请求需要的话可以拿到
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
            // 获取方法返回类型
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> returnType = method.getReturnType();

            // 如果返回类型是 void，直接返回 null
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                return null;
            }

            // 直接从 Redis 获取 resultKey 对象
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
     * 重要：此方法不会删除token，只有持有锁的线程才能删除token。
     * </p>
     *
     * @param joinPoint    连接点
     * @param redisKey     幂等性 Redis Key
     * @param resultKey    结果缓存 Key
     * @param isMqConsumer 是否是 MQ 消费者场景
     * @return 缓存的结果，如果等待超时或失败，返回默认值或null
     */
    private Object waitForResult(ProceedingJoinPoint joinPoint, String redisKey, String resultKey, boolean isMqConsumer) {
        // 从 Environment 动态读取配置值，支持配置刷新
        int maxRetryCount = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_MAX_RETRY_COUNT_PREFIX, Integer.class, DEFAULT_MAX_RETRY_COUNT);
        long retryIntervalMs = environment.getProperty(IdempotentConstants.NACOS_IDEMPOTENT_RETRY_INTERVAL_MS_PREFIX, Long.class, DEFAULT_RETRY_INTERVAL_MS);

        for (int i = 0; i < maxRetryCount; i++) {
            try {
                Thread.sleep(retryIntervalMs);

                // 检查状态
                String status = redisService.getCacheObject(redisKey, String.class);

                if (STATUS_SUCCESS.equals(status)) {
                    // 执行成功，获取结果
                    Object cachedResult = getCachedResult(joinPoint, resultKey);
                    if (cachedResult != null) {
                        log.info("强幂等模式 - 等待后获取到结果，Token: {}", redisKey);
                        return cachedResult;
                    }
                } else if (STATUS_FAILED.equals(status)) {
                    // 执行失败，根据场景处理
                    if (isMqConsumer) {
                        // MQ 场景：静默处理，避免消息重新入队
                        log.info("强幂等模式 - MQ 检测到执行失败，跳过消费，Token: {}", redisKey);
                        return getDefaultReturnValue(joinPoint);
                    } else {
                        // HTTP 场景：等待超时后，如果还是失败状态，说明确实失败了
                        // 但不能删除token，让过期时间自然失效，或者抛出异常
                        log.warn("强幂等模式 - HTTP 检测到执行失败，等待超时，Token: {}", redisKey);
                        // 不删除token，让客户端重试或等待过期
                        throw new ServiceException("请求处理失败，请稍后重试", ResultCode.ERROR.getCode());
                    }
                } else if (STATUS_PROCESSING.equals(status)) {
                    // 仍在执行中，继续等待
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
                // 重新抛出业务异常
                throw e;
            }
        }

        // 等待超时
        log.warn("强幂等模式 - 等待结果超时，Token: {}", redisKey);
        if (isMqConsumer) {
            // MQ 场景：静默处理
            return getDefaultReturnValue(joinPoint);
        } else {
            // HTTP 场景：抛出超时异常
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
            // 注解显式指定为 TRUE，强制开启强幂等模式
            return true;
        } else if (annotationMode == IdempotentMode.FALSE) {
            // 注解显式指定为 FALSE，强制关闭强幂等模式（防重模式）
            return false;
        } else {
            // DEFAULT：使用全局配置
            return globalConfig;
        }
    }

    /**
     * 判断是否是 MQ 消费者场景
     * <p>
     * 通过检查是否存在 HttpServletRequest 来判断：
     * <ul>
     *     <li>如果存在 HttpServletRequest，说明是 HTTP 请求场景</li>
     *     <li>如果不存在 HttpServletRequest，说明是 MQ 消费者场景</li>
     * </ul>
     * </p>
     *
     * @param joinPoint 连接点
     * @return true - MQ 消费者场景；false - HTTP 请求场景
     */
    private boolean isMqConsumer(ProceedingJoinPoint joinPoint) {
        // 检查是否存在 HttpServletRequest
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 如果不存在 HttpServletRequest，说明是 MQ 消费者场景
        return attributes == null;
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
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getMethod().getReturnType();

        // void 类型：返回 null
        if (void.class.equals(returnType) || Void.class.equals(returnType)) {
            return null;
        }

        // 基本类型：使用 switch 表达式返回对应的默认值
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

        // 对象类型：返回 null
        return null;
    }

    /**
     * 原子操作：如果状态等于指定值，则删除Token
     * <p>
     * 使用 Lua 脚本实现原子操作，避免并发问题。
     * 只有当 Token 的状态等于指定状态时，才会删除 Token。
     * </p>
     *
     * @param redisKey       Redis Key
     * @param expectedStatus 期望的状态值
     * @return true - 删除成功（状态匹配）；false - 删除失败（状态不匹配或键不存在）
     */
    private boolean deleteIfStatusEquals(String redisKey, String expectedStatus) {
        try {
            // 使用 compareAndDelete 方法，但需要确保状态值不包含空格
            // 由于 STATUS_FAILED 等常量不包含空格，可以直接使用
            return redisService.compareAndDelete(redisKey, expectedStatus);
        } catch (Exception e) {
            log.warn("原子删除Token失败: {}, 错误: {}", redisKey, e.getMessage());
            return false;
        }
    }

    /**
     * 创建 AmqpRejectAndDontRequeueException 异常
     * <p>
     * 使用反射方式创建，避免编译时依赖 spring-amqp。
     * 该异常会告诉 RabbitMQ 拒绝消息且不要重新入队，直接丢弃消息。
     * </p>
     *
     * @param message 异常消息
     * @return RuntimeException 异常（如果 AmqpRejectAndDontRequeueException 不存在，则返回 ServiceException）
     */
    private RuntimeException createAmqpRejectAndDontRequeueException(String message) {
        try {
            // 尝试使用反射创建 AmqpRejectAndDontRequeueException
            Class<?> exceptionClass = Class.forName("org.springframework.amqp.AmqpRejectAndDontRequeueException");
            return (RuntimeException) exceptionClass.getConstructor(String.class).newInstance(message);
        } catch (ClassNotFoundException e) {
            // AmqpRejectAndDontRequeueException 类不存在，说明没有引入 spring-amqp 依赖
            // 这种情况下，抛出 ServiceException，但记录警告日志
            log.warn("AmqpRejectAndDontRequeueException 类不存在，使用 ServiceException 代替。建议引入 spring-amqp 依赖以确保消息不重新入队。");
            return new ServiceException(message, ResultCode.INVALID_PARA.getCode());
        } catch (Exception e) {
            // 反射创建失败，使用 ServiceException
            log.warn("创建 AmqpRejectAndDontRequeueException 失败: {}", e.getMessage());
            return new ServiceException(message, ResultCode.INVALID_PARA.getCode());
        }
    }
}
