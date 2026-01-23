package com.zmbdp.common.ratelimit.aspect;

import com.zmbdp.common.core.utils.ClientIpUtil;
import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.RateLimitConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.ratelimit.annotation.RateLimit;
import com.zmbdp.common.ratelimit.builder.RateLimitKeyBuilder;
import com.zmbdp.common.ratelimit.config.RateLimitConfig;
import com.zmbdp.common.ratelimit.enums.RateLimitDimension;
import com.zmbdp.common.ratelimit.executor.RateLimiterExecutor;
import com.zmbdp.common.ratelimit.resolver.RateLimitConfigResolver;
import com.zmbdp.common.security.utils.JwtUtil;
import com.zmbdp.common.security.utils.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 频控 / 防刷切面
 * <p>
 * 拦截带 {@link RateLimit} 注解的方法，在方法执行前进行限流校验。<br>
 * 基于 Redis <b>滑动窗口</b>算法实现，支持 IP、账号双维度限流。
 * <p>
 * <b>滑动窗口 vs 固定窗口：</b><br>
 * 滑动窗口按「当前时刻往前 windowSec 秒」统计请求数，窗口随请求时间滑动，
 * 避免固定窗口在边界处的突发流量（如 59s、61s 各打满一次）。
 * <p>
 * <b>工作原理：</b>
 * <ol>
 *     <li>拦截带 {@code @RateLimit} 的方法</li>
 *     <li>从请求中提取 IP 和用户 ID（从网关下发的 userId 请求头）</li>
 *     <li>根据注解配置的维度（IP/ACCOUNT/BOTH）构建 Redis Key</li>
 *     <li>对每个 Key 用 ZSET 滑动窗口：ZREMRANGEBYSCORE 剔除窗口外、ZCARD 计数、未超限则 ZADD 当前请求</li>
 *     <li>任一 Key 超限则抛出 {@link ServiceException} 拒绝请求</li>
 *     <li>否则放行，继续执行原方法</li>
 * </ol>
 * <p>
 * <b>Redis Key 格式：</b>
 * <ul>
 *     <li>IP 维度：{@code ratelimit:ip:{ip}:{类名#方法名}}</li>
 *     <li>账号维度：{@code ratelimit:identity:{userIdentifier}:{类名#方法名}}</li>
 * </ul>
 * <p>
 * 其中 {@code userIdentifier} 可以是：
 * <ul>
 *     <li>已登录：{@code userId}（从 JWT Token 中提取）</li>
 *     <li>未登录：{@code account}（从请求参数或方法参数中提取，如发送验证码、登录接口）</li>
 * </ul>
 * <p>
 * <b>配置优先级：</b>
 * <ol>
 *     <li>注解参数（limit、windowSec、message）</li>
 *     <li>Nacos 全局配置（ratelimit.default-*）</li>
 *     <li>代码默认值</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>使用 {@code @RefreshScope} 支持 Nacos 热配置更新</li>
 *     <li>无 HTTP 请求上下文时（如内部调用）会跳过限流，直接放行</li>
 *     <li>账号维度支持多种获取方式：
 *         <ul>
 *             <li>已登录：从 JWT Token 中提取 {@code userId}</li>
 *             <li>未登录（GET 请求）：从 Query 参数中提取 {@code account}</li>
 *             <li>未登录（POST 请求）：从方法参数中提取 {@code account}（通过 SpEL 表达式）</li>
 *             <li>未找到账号标识时，退化为 IP 维度，避免重复计数</li>
 *         </ul>
 *     </li>
 *     <li>使用滑动窗口（ZSET + Lua）保证原子性，限流更平滑，无边界突发</li>
 *     <li>支持降级策略：Redis 异常时可配置失败放行（fail-open）或失败拒绝（fail-close）</li>
 *     <li>双维度限流时，如果未登录，identityKey == ipKey，只限流一次，避免重复计数</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see RateLimit
 * @see RateLimitDimension
 */
@Slf4j
@Aspect
@Component
@RefreshScope
public class RateLimitAspect {

    /**
     * SpEL 表达式解析器（用于从方法参数中提取 account）
     */
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * 参数名发现器（用于获取方法参数名）
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 限流执行器（滑动窗口实现）
     */
    @Autowired
    private RateLimiterExecutor rateLimiter;

    /**
     * Key 构建器
     */
    @Autowired
    private RateLimitKeyBuilder keyBuilder;

    /**
     * 配置解析器
     */
    @Autowired
    private RateLimitConfigResolver configResolver;

    /**
     * JWT Token 密钥（用于从 Token 中提取 userId）
     */
    @Value("${jwt.token.secret:}")
    private String jwtSecret;

    /**
     * 构建方法签名字符串，格式：{@code 类全限定名#方法名}
     * <p>
     * 用于区分不同接口的限流 Key，例如：
     * <ul>
     *     <li>{@code com.zmbdp.portal.service.user.controller.UserController#sendCode}</li>
     *     <li>{@code com.zmbdp.admin.service.user.controller.SysUserController#list}</li>
     * </ul>
     * <p>
     * <b>注意：</b>不包含请求路径，因为：
     * <ul>
     *     <li>同一方法可能对应多个路径（如 GET /api/v1/user 和 GET /api/v2/user）</li>
     *     <li>路径可能包含动态参数（如 /api/user/{id}），导致 key 不一致</li>
     *     <li>类名+方法名已经足够区分不同接口</li>
     * </ul>
     * <p>
     * 如果需要区分不同路径的限流，请使用注解的 {@code keySuffix} 参数。
     *
     * @param joinPoint 连接点
     * @param request   当前请求（当前未使用，保留以兼容重载方法）
     * @return 方法签名字符串
     */
    private static String buildMethodSignature(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        return clazz.getName() + "#" + method.getName();
    }

    /**
     * 构建方法签名字符串（重载方法，兼容无请求对象的情况）
     *
     * @param joinPoint 连接点
     * @return 方法签名字符串
     */
    private static String buildMethodSignature(ProceedingJoinPoint joinPoint) {
        return buildMethodSignature(joinPoint, null);
    }

    /**
     * 切点：拦截所有带 {@link RateLimit} 注解的方法
     */
    @Pointcut("@annotation(com.zmbdp.common.ratelimit.annotation.RateLimit)")
    public void rateLimitPointcut() {
    }

    /**
     * 环绕通知：在方法执行前进行限流校验
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>检查是否有 HTTP 请求上下文，无则跳过限流</li>
     *     <li>检查全局开关，关闭则跳过限流</li>
     *     <li>解析配置（注解 > Nacos > 默认值）</li>
     *     <li>提取 IP 和用户 ID</li>
     *     <li>构建限流 Key 列表</li>
     *     <li>对每个 Key 执行限流检查</li>
     *     <li>任一 Key 超限则抛出异常，否则放行</li>
     * </ol>
     * <p>
     * <b>职责说明：</b>
     * <ul>
     *     <li>只负责流程编排，不包含具体算法实现</li>
     *     <li>配置解析委托给 {@link RateLimitConfigResolver}</li>
     *     <li>Key 构建委托给 {@link RateLimitKeyBuilder}</li>
     *     <li>限流执行委托给 {@link RateLimiterExecutor}</li>
     * </ul>
     *
     * @param joinPoint 连接点（被拦截的方法）
     * @param rateLimit 限流注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常或限流异常
     */
    @Around("rateLimitPointcut() && @annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 先检查一下 Http 请求上下文
        HttpServletRequest request = ServletUtil.getRequest();
        if (request == null) {
            log.debug("RateLimit: 无 HTTP 请求上下文，跳过限流");
            return joinPoint.proceed();
        }

        // 看看是否需要开启限流
        if (!configResolver.isEnabled()) {
            return joinPoint.proceed();
        }

        // 走到这里说明需要开启限流
        // 然后解析配置（优先级：注解 > Nacos > 默认值）
        RateLimitConfig config = configResolver.resolve(rateLimit);

        // 构建接口标识（用于区分不同接口的限流 Key）
        String api = StringUtil.isNotEmpty(rateLimit.keySuffix())
                ? rateLimit.keySuffix()
                : buildMethodSignature(joinPoint, request);

        // 提取客户端 IP 和用户标识（userId 或 account）
        String ip = extractClientIp(request, config);
        String userIdentifier = extractUserIdentifier(request, config, joinPoint);

        // 构建限流 Key 列表（根据维度）
        List<String> keys = keyBuilder.buildKeys(
                rateLimit.dimensions(),
                config.keyPrefix(),
                api,
                ip,
                userIdentifier
        );

        // 对每个 Key 执行限流检查
        for (String key : keys) {
            boolean allowed;
            try {
                allowed = rateLimiter.tryAcquire(key, config.limit(), config.windowMs());
            } catch (Exception e) {
                // Redis 连接异常等系统错误
                log.error("RateLimit: 限流执行异常 key={}", key, e);
                if (config.failOpen()) {
                    // 降级策略：失败放行（保证可用性）
                    log.warn("RateLimit: Redis 异常，降级放行 key={}", key);
                    continue; // 跳过当前 key，继续下一个
                } else {
                    // 默认策略：失败拒绝（保证安全性）
                    throw new ServiceException(ResultCode.ERROR);
                }
            }

            if (!allowed) {
                // 业务限流：滑动窗口内已超限
                log.info("RateLimit: 触发限流 key={} limit={} windowSec={}", key, config.limit(), config.windowSec());
                throw new ServiceException(config.message(), ResultCode.REQUEST_TOO_FREQUENT.getCode());
            }
        }

        // 说明所有 Key 都符合要求，直接放行
        return joinPoint.proceed();
    }

    /**
     * 提取客户端 IP
     * <p>
     * 支持从请求头或请求参数获取 IP，优先级：请求头 > 请求参数 > 标准获取逻辑。
     * <p>
     * <b>获取优先级：</b>
     * <ol>
     *     <li>从配置的请求头获取（{@code config.ipHeaderName()}）</li>
     *     <li>如果允许从参数获取且请求头中没有，从请求参数获取（{@code config.ipParamName()}）</li>
     *     <li>如果都失败，回退到标准 IP 获取逻辑（X-Forwarded-For → X-Real-IP → getRemoteAddr()）</li>
     * </ol>
     *
     * @param request HTTP 请求对象
     * @param config  限流配置对象
     * @return 客户端 IP 地址
     */
    private String extractClientIp(HttpServletRequest request, RateLimitConfig config) {
        // 先从配置的请求头获取 IP
        if (StringUtil.isNotEmpty(config.ipHeaderName())) {
            String ip = request.getHeader(config.ipHeaderName());
            if (StringUtil.isNotEmpty(ip) && !RateLimitConstants.UNKNOWN.equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP（逗号分隔），只取第一个
                int idx = ip.indexOf(',');
                ip = idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
                if (StringUtil.isNotEmpty(ip)) {
                    return ip;
                }
            }
        }

        // 如果允许从请求参数获取且请求头中没有，从请求参数获取
        if (config.allowIpParam() && StringUtil.isNotEmpty(config.ipParamName())) {
            String ip = request.getParameter(config.ipParamName());
            if (StringUtil.isNotEmpty(ip) && !RateLimitConstants.UNKNOWN.equalsIgnoreCase(ip)) {
                return ip.trim();
            }
        }

        // 如果都失败，回退到标准 IP 获取逻辑（X-Forwarded-For → X-Real-IP → getRemoteAddr()）
        return ClientIpUtil.getClientIp(request);
    }

    /**
     * 提取用户标识（userId 或 account）
     * <p>
     * 优先级：
     * <ol>
     *     <li>从 JWT Token 中获取 userId（已登录）</li>
     *     <li>从请求参数中获取 account（未登录，发送验证码或登录时）</li>
     *     <li>从方法参数中获取 account（POST 请求的 JSON Body，通过 SpEL 表达式）</li>
     *     <li>从请求头中获取 userId（网关下发，兼容旧逻辑）</li>
     *     <li>返回 null（未登录且无账号参数）</li>
     * </ol>
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>已登录：从 JWT Token 中提取 userId，实现按用户限流</li>
     *     <li>发送验证码（GET）：从 Query 参数中提取 account，实现按身份限流</li>
     *     <li>登录接口（POST）：从方法参数中提取 account，实现按身份限流</li>
     * </ul>
     * <p>
     * <b>注意：</b>提取到的标识会用于构建 Redis Key，格式为 {@code ratelimit:identity:{userIdentifier}:{api}}
     *
     * @param request   HTTP 请求对象
     * @param config    限流配置对象
     * @param joinPoint 连接点（用于获取方法参数）
     * @return 用户标识（userId 或 account），未找到时返回 null
     */
    private String extractUserIdentifier(HttpServletRequest request, RateLimitConfig config, ProceedingJoinPoint joinPoint) {
        // 先从 JWT Token 中获取 userId（已登录）
        String userId = extractUserIdFromJwt(request);
        if (StringUtil.isNotEmpty(userId)) {
            return userId;
        }

        // 从请求参数中获取 account（GET 请求的 Query 参数，或 POST 请求的表单参数）
        String account = extractAccountFromRequest(request, config);
        if (StringUtil.isNotEmpty(account)) {
            return account;
        }

        // 从方法参数中获取 account（POST 请求的 JSON Body，通过 SpEL 表达式）
        account = extractAccountFromMethodArgs(joinPoint);
        if (StringUtil.isNotEmpty(account)) {
            return account;
        }

        // 从请求头中获取 userId（网关下发，兼容旧逻辑）
        String headerUserId = request.getHeader(RateLimitConstants.HEADER_USER_ID);
        if (StringUtil.isNotEmpty(headerUserId)) {
            return headerUserId.trim();
        }

        // 说明未找到用户标识
        return null;
    }

    /**
     * 从 JWT Token 中提取 userId
     * <p>
     * 如果请求头中有 Authorization Token，尝试解析并提取 userId。<br>
     * 提取到的 userId 会用于构建 Redis Key：{@code ratelimit:identity:{userId}:{api}}
     *
     * @param request HTTP 请求对象
     * @return 用户 ID，如果 Token 不存在或解析失败返回 null
     */
    private String extractUserIdFromJwt(HttpServletRequest request) {
        try {
            // 获取 JWT Token
            String token = SecurityUtil.getToken(request);
            if (StringUtil.isEmpty(token) || StringUtil.isEmpty(jwtSecret)) {
                return null;
            }

            // 从 Token 中提取 userId
            String userId = JwtUtil.getUserId(token, jwtSecret);
            return StringUtil.isNotEmpty(userId) ? userId.trim() : null;
        } catch (Exception e) {
            // Token 解析失败（可能是未登录或 Token 无效），忽略异常
            log.trace("RateLimit: 从 JWT Token 提取 userId 失败", e);
            return null;
        }
    }

    /**
     * 从请求参数中提取 account（账号）
     * <p>
     * 支持从 Query 参数和表单参数中获取 account。<br>
     * 主要用于发送验证码接口（GET 请求，Query 参数）。<br>
     * 提取到的 account 会用于构建 Redis Key：{@code ratelimit:identity:{account}:{api}}
     *
     * @param request HTTP 请求对象
     * @param config  限流配置对象
     * @return 账号（手机号或邮箱），如果不存在返回 null
     */
    private String extractAccountFromRequest(HttpServletRequest request, RateLimitConfig config) {
        try {
            // 从 Query 参数或表单参数中获取 account
            String account = request.getParameter("account");
            if (StringUtil.isNotEmpty(account)) {
                return account.trim();
            }
            return null;
        } catch (Exception e) {
            log.trace("RateLimit: 从请求参数提取 account 失败", e);
            return null;
        }
    }

    /**
     * 从方法参数中提取 account（账号）
     * <p>
     * 通过 SpEL 表达式从方法参数中获取 account，主要用于 POST 请求的 JSON Body。<br>
     * 支持以下表达式：
     * <ul>
     *     <li>{@code account}：从参数名为 account 的参数中获取</li>
     *     <li>{@code args[0].account}：从第一个参数的 account 属性中获取</li>
     *     <li>{@code codeLoginDTO.account}：从 codeLoginDTO 参数的 account 属性中获取</li>
     * </ul>
     * <p>
     * 提取到的 account 会用于构建 Redis Key：{@code ratelimit:identity:{account}:{api}}
     *
     * @param joinPoint 连接点（用于获取方法参数）
     * @return 账号（手机号或邮箱），如果不存在返回 null
     */
    private String extractAccountFromMethodArgs(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();

            if (parameterNames == null || args == null || parameterNames.length == 0) {
                return null;
            }

            // 创建 SpEL 上下文
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            context.setVariable("args", args); // 也支持用 args[0] 这种方式访问

            // 尝试多种表达式获取 account
            String[] expressions = {"account", "args[0].account", "codeLoginDTO.account", "loginDTO.account"};
            for (String expression : expressions) {
                try {
                    Expression expr = expressionParser.parseExpression(expression);
                    Object value = expr.getValue(context);
                    if (value != null && StringUtil.isNotEmpty(value.toString())) {
                        return value.toString().trim();
                    }
                } catch (Exception e) {
                    // 表达式解析失败，继续尝试下一个
                    continue;
                }
            }

            // 如果表达式都失败，尝试直接查找参数名为 account 的参数
            for (int i = 0; i < parameterNames.length; i++) {
                if ("account".equals(parameterNames[i]) && args[i] != null) {
                    String account = args[i].toString();
                    if (StringUtil.isNotEmpty(account)) {
                        return account.trim();
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.trace("RateLimit: 从方法参数提取 account 失败", e);
            return null;
        }
    }
}