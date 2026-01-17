package com.zmbdp.common.idempotent.aspect;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.IdempotentConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.idempotent.annotation.Idempotent;
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
            throw new ServiceException("幂等性Token不能为空", ResultCode.INVALID_PARA.getCode());
        }

        // 构建 Redis Key
        String redisKey = IdempotentConstants.IDEMPOTENT_KEY_PREFIX + idempotentToken;

        // 获取过期时间（优先级：注解值 > 全局配置 > 默认值 300）
        // 从 Environment 动态读取配置值，支持配置刷新
        long globalExpireTime = environment.getProperty("idempotent.expire-time", Long.class, 300L);
        // 如果注解中使用默认值 300，则使用全局配置；否则使用注解值
        long expireTime = (idempotent.expireTime() == 300) ? globalExpireTime : idempotent.expireTime();

        // 判断是否启用强幂等模式（优先级：注解值 > 全局配置）
        // 从 Environment 动态读取配置值，支持配置刷新
        boolean globalReturnCachedResult = environment.getProperty("idempotent.return-cached-result", Boolean.class, false);
        // 如果注解中明确设置为 true，则使用 true；否则使用全局配置
        boolean returnCachedResult = idempotent.returnCachedResult() || globalReturnCachedResult;

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
        Boolean success = redisService.setCacheObjectIfAbsent(redisKey, "1", expireTime, TimeUnit.SECONDS);
        if (!success) {
            // Token 已存在，说明是重复请求
            if (returnCachedResult) {
                // 强幂等模式：从缓存获取结果
                String resultKey = redisKey + ":result";
                // 第一次尝试获取缓存结果
                Object cachedResult = getCachedResult(joinPoint, resultKey);
                if (cachedResult != null) {
                    log.info("强幂等模式 - 返回缓存结果（并发场景），Token: {}", idempotentToken);
                    return cachedResult;
                }
                // 如果缓存结果不存在，可能是第一次请求还在执行中，等待后重试
                log.debug("强幂等模式 - 缓存结果不存在，等待后重试，Token: {}", idempotentToken);
                try {
                    Thread.sleep(100); // 等待100ms
                    cachedResult = getCachedResult(joinPoint, resultKey);
                    if (cachedResult != null) {
                        log.info("强幂等模式 - 返回缓存结果（等待后重试），Token: {}", idempotentToken);
                        return cachedResult;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // 如果仍然没有缓存结果（可能第一次请求执行失败或超时），允许重新执行
                log.warn("强幂等模式 - 缓存结果仍不存在，删除Token允许重新执行，Token: {}", idempotentToken);
                redisService.deleteObject(redisKey);
            } else {
                // 防重模式：直接报错
                log.warn("防重模式 - 重复请求，Token: {}", idempotentToken);
                throw new ServiceException(idempotent.message(), ResultCode.INVALID_PARA.getCode());
            }
        }

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 强幂等模式：缓存方法执行结果
            if (returnCachedResult) {
                cacheResult(redisKey + ":result", result, expireTime);
            }

            return result;
        } catch (Exception e) {
            // 方法执行失败，删除 Token，允许重试
            redisService.deleteObject(redisKey);
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
     * 使用反射检查 Message 类是否存在，避免编译时依赖
     *
     * @param joinPoint  连接点
     * @param headerName 消息头名称
     * @return 幂等性 Token
     */
    private String getTokenFromRabbitMQMessage(ProceedingJoinPoint joinPoint, String headerName) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }

            // 使用反射检查 Message 类是否存在（避免编译时依赖）
            Class<?> messageClass = Class.forName("org.springframework.amqp.core.Message");

            // 查找 Message 类型的参数（RabbitMQ 的原始消息对象）
            for (Object arg : args) {
                if (arg != null && messageClass.isInstance(arg)) {
                    // 使用反射调用 getMessageProperties().getHeaders().get(headerName)
                    Object messageProperties = messageClass.getMethod("getMessageProperties").invoke(arg);
                    Object headers = messageProperties.getClass().getMethod("getHeaders").invoke(messageProperties);
                    Object token = ((java.util.Map<?, ?>) headers).get(headerName);
                    if (token != null) {
                        return token.toString();
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // Message 类不存在，说明没有引入 spring-amqp-core 依赖，正常情况
            log.trace("Spring AMQP 未引入，跳过从 RabbitMQ 消息头获取 Token");
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
            // 将结果序列化为 JSON 字符串
            String resultJson = JsonUtil.classToJson(result);
            if (StringUtil.isNotEmpty(resultJson)) {
                // 缓存结果到 Redis
                redisService.setCacheObject(resultKey, resultJson, expireTime, TimeUnit.SECONDS);
                log.debug("强幂等模式 - 缓存方法结果，Key: {}", resultKey);
            }
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
            // 从 Redis 获取缓存的结果 JSON 字符串
            String resultJson = redisService.getCacheObject(resultKey, String.class);
            if (StringUtil.isEmpty(resultJson)) {
                return null;
            }

            // 获取方法返回类型
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> returnType = method.getReturnType();

            // 如果返回类型是 void，直接返回 null
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                return null;
            }

            // 尝试反序列化结果（支持 Result<T> 等泛型类型）
            return JsonUtil.jsonToClass(resultJson, returnType);
        } catch (Exception e) {
            log.warn("强幂等模式 - 从缓存获取结果失败: {}", e.getMessage());
            return null;
        }
    }
}
