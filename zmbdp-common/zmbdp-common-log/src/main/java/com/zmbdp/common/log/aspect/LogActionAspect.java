package com.zmbdp.common.log.aspect;

import com.zmbdp.common.core.utils.*;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.constants.LogConstants;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.service.ILogStorageService;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * <p>
 * 基于 Spring AOP 实现操作日志的自动收集和处理。<br>
 * 拦截标注了 {@link LogAction} 注解的方法，自动记录操作信息、参数、返回值、异常等。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>自动记录方法执行信息（操作描述、方法名、参数、返回值）</li>
 *     <li>自动记录审计信息（用户ID、用户名、IP、User-Agent、时间戳）</li>
 *     <li>自动记录异常信息（异常类型、异常消息、完整堆栈）</li>
 *     <li>自动记录性能信息（方法执行耗时）</li>
 *     <li>支持 SpEL 表达式进行条件记录和模板化</li>
 *     <li>支持敏感字段脱敏</li>
 *     <li>支持异步处理，不阻塞业务线程</li>
 * </ul>
 * <p>
 * <b>工作原理：</b>
 * <ol>
 *     <li>拦截标注了 {@code @LogAction} 的方法</li>
 *     <li>解析注解配置，判断是否需要记录日志</li>
 *     <li>执行条件表达式（SpEL），如果不满足条件则跳过</li>
 *     <li>记录方法执行前信息（参数、用户信息、IP等）</li>
 *     <li>执行原方法，记录执行耗时</li>
 *     <li>如果方法执行成功，记录返回值（如果配置）</li>
 *     <li>如果方法执行失败，记录异常信息（如果配置）</li>
 *     <li>异步保存日志到存储服务</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>使用 {@code @RefreshScope} 支持 Nacos 热配置更新</li>
 *     <li>无 HTTP 请求上下文时（如内部调用、单元测试）会跳过部分信息收集</li>
 *     <li>异步处理默认启用，可通过配置 {@code log.async-enabled} 控制</li>
 *     <li>日志存储失败不会影响业务逻辑，只记录错误日志</li>
 *     <li>SpEL 表达式执行失败时会降级使用默认值</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see LogAction
 * @see ILogStorageService
 */
@Slf4j
@Aspect
@Component
@RefreshScope
public class LogActionAspect {

    /**
     * SpEL 表达式解析器
     */
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * 参数名发现器（用于获取方法参数名）
     */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 日志存储服务（默认存储服务，通常是 ConsoleLogStorageService）
     */
    @Autowired
    @Qualifier("consoleLogStorageService")
    private ILogStorageService logStorageService;

    /**
     * Spring 应用上下文（用于动态获取存储服务Bean）
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Token 服务（用于获取用户信息）
     */
    @Autowired(required = false)
    private TokenService tokenService;

    /**
     * JWT 密钥（用于解析 Token）
     */
    @Value("${jwt.token.secret:}")
    private String jwtSecret;

    /**
     * 环境配置（用于动态读取配置值，支持配置刷新）
     */
    @Autowired
    private Environment environment;

    /**
     * 切点：标记了 <code>@LogAction</code> 注解的方法或类
     * <p>
     * 支持两种方式：
     * <ul>
     *     <li>方法注解：直接标注在方法上，优先级最高</li>
     *     <li>类注解：标注在类上，作为默认配置，方法注解可覆盖</li>
     * </ul>
     */
    @Pointcut("@annotation(com.zmbdp.common.log.annotation.LogAction) || @within(com.zmbdp.common.log.annotation.LogAction)")
    public void logActionPointcut() {
    }

    /**
     * 切点：全局默认记录（拦截所有 Controller 和 Service 方法）
     * <p>
     * 用于记录没有注解的方法的基本信息（异常堆栈、方法耗时、traceId、调用链等）
     * 可通过配置 {@code log.global-record-enabled} 控制是否启用
     * <p>
     * <b>排除说明：</b>
     * <ul>
     *     <li>排除 {@link ILogStorageService} 实现类，避免日志存储时触发无限递归</li>
     *     <li>排除 {@link LogActionAspect} 自身，避免切面方法被拦截</li>
     * </ul>
     */
    @Pointcut("(@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.stereotype.Service)) && " +
            "execution(public * *(..)) && " +
            "!within(com.zmbdp.common.log.service.impl..*) && " +
            "!within(com.zmbdp.common.log.aspect.LogActionAspect)")
    public void globalRecordPointcut() {
    }

    /**
     * 环绕通知：拦截带注解的方法（业务日志）
     * <p>
     * 支持类注解和方法注解，优先级：方法注解 > 类注解
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("logActionPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return processLogAction(joinPoint, true);
    }

    /**
     * 环绕通知：全局默认记录（拦截所有 Controller/Service 方法）
     * <p>
     * 即使没有注解也记录基本信息（异常堆栈、方法耗时、traceId、调用链等）
     * 可通过配置 {@code log.global-record-enabled} 控制是否启用
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("globalRecordPointcut() && !logActionPointcut()")
    public Object globalRecord(ProceedingJoinPoint joinPoint) throws Throwable {
        // 如果全局默认记录未启用，直接执行
        if (!isGlobalRecordEnabled()) {
            return joinPoint.proceed();
        }

        // 如果方法已经有注解，不重复处理（由 logActionPointcut 处理）
        LogAction methodLogAction = getMethodAnnotation(joinPoint);
        if (methodLogAction != null) {
            return joinPoint.proceed();
        }

        // 处理全局默认记录
        return processGlobalRecord(joinPoint);
    }

    /**
     * 处理带注解的日志记录
     *
     * @param joinPoint         连接点
     * @param requireAnnotation 是否要求必须有注解
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    private Object processLogAction(ProceedingJoinPoint joinPoint, boolean requireAnnotation) throws Throwable {
        // 获取方法注解（优先级最高）
        LogAction methodLogAction = getMethodAnnotation(joinPoint);
        // 获取类注解（作为默认配置）
        LogAction classLogAction = getClassAnnotation(joinPoint);

        // 三层策略：方法注解 > 类注解 > 全局默认策略
        // 方法注解是核心，必须存在才能记录业务日志（value 必填）
        // 类注解和全局默认策略仅提供默认配置值，不能单独使用来记录日志
        if (requireAnnotation && methodLogAction == null) {
            return joinPoint.proceed();
        }

        // 合并注解配置：方法注解优先，类注解补充，全局配置兜底
        LogAction logAction = mergeLogAction(methodLogAction, classLogAction);

        // 如果操作描述为空，跳过日志记录
        if (logAction == null || StringUtil.isEmpty(logAction.value())) {
            return joinPoint.proceed();
        }
        // 检查是否启用日志功能
        if (!isLogEnabled()) {
            return joinPoint.proceed();
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 构建日志对象
        OperationLogDTO logDTO = buildLogDTO(joinPoint, logAction);

        // 记录方法参数（如果配置）
        if (logAction.recordParams()) {
            try {
                String params = extractParams(joinPoint, logAction);
                logDTO.setParams(params);
            } catch (Exception e) {
                log.warn("提取方法参数失败: {}", e.getMessage());
            }
        }

        // 执行条件表达式，如果不满足条件则跳过日志记录
        if (StringUtil.isNotEmpty(logAction.condition())) {
            try {
                boolean shouldRecord = evaluateCondition(joinPoint, logAction.condition(), null);
                if (!shouldRecord) {
                    // 条件不满足，直接执行方法，不记录日志
                    return joinPoint.proceed();
                }
            } catch (Exception e) {
                log.warn("执行条件表达式失败: {}, 继续记录日志", e.getMessage());
            }
        }

        // 执行原方法
        Object result = null;
        Throwable exception = null;
        try {
            result = joinPoint.proceed();
            logDTO.setStatus("SUCCESS");

            // 记录返回值（如果配置）
            if (logAction.recordResult()) {
                try {
                    String resultStr = extractResult(result, logAction, joinPoint);
                    logDTO.setResult(resultStr);
                } catch (Exception e) {
                    log.warn("提取方法返回值失败: {}", e.getMessage());
                }
            }

            // 再次检查条件表达式（可能依赖返回值）
            if (StringUtil.isNotEmpty(logAction.condition())) {
                try {
                    boolean shouldRecord = evaluateCondition(joinPoint, logAction.condition(), result);
                    if (!shouldRecord) {
                        // 条件不满足，不记录日志
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("执行条件表达式失败: {}, 继续记录日志", e.getMessage());
                }
            }
        } catch (Throwable e) {
            exception = e;
            logDTO.setStatus("FAILED");

            // 记录异常信息（如果配置）
            if (logAction.recordException()) {
                logDTO.setException(e.getClass().getName() + ": " + e.getMessage());
                logDTO.setExceptionStack(getStackTrace(e));
            }

            // 根据配置决定是否抛出异常
            if (logAction.throwException()) {
                throw e;
            }
        } finally {
            // 计算执行耗时
            long costTime = System.currentTimeMillis() - startTime;
            logDTO.setCostTime(costTime);
            logDTO.setOperationTime(LocalDateTime.now());

            // 异步保存日志
            saveLog(logDTO, logAction);
        }

        return result;
    }

    /**
     * 构建日志对象
     *
     * @param joinPoint 连接点
     * @param logAction 日志注解
     * @return 日志对象
     */
    private OperationLogDTO buildLogDTO(ProceedingJoinPoint joinPoint, LogAction logAction) {
        OperationLogDTO logDTO = new OperationLogDTO();

        // 设置操作描述
        logDTO.setOperation(logAction.value());

        // 设置方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        logDTO.setMethod(className + "#" + methodName);

        // 设置业务模块和类型
        logDTO.setModule(logAction.module());
        logDTO.setBusinessType(logAction.businessType());

        // 设置请求信息（如果有 HTTP 请求上下文）
        HttpServletRequest request = ServletUtil.getRequest();
        if (request != null) {
            logDTO.setRequestPath(request.getRequestURI());
            logDTO.setRequestMethod(request.getMethod());
            logDTO.setClientIp(ClientIpUtil.getClientIp(request));
            logDTO.setUserAgent(request.getHeader("User-Agent"));
        }

        // 设置用户信息
        setUserInfo(logDTO);

        // 设置调用链追踪信息（traceId、spanId）
        setTraceInfo(logDTO);

        return logDTO;
    }

    /**
     * 设置调用链追踪信息（traceId、spanId）
     *
     * @param logDTO 日志对象
     */
    private void setTraceInfo(OperationLogDTO logDTO) {
        try {
            // 尝试从 MDC 获取 traceId
            String traceId = org.slf4j.MDC.get("traceId");
            if (StringUtil.isEmpty(traceId)) {
                // 尝试从请求头获取 traceId
                HttpServletRequest request = ServletUtil.getRequest();
                if (request != null) {
                    traceId = request.getHeader("traceId");
                    if (StringUtil.isEmpty(traceId)) {
                        traceId = request.getHeader("X-Trace-Id");
                    }
                }
            }
            logDTO.setTraceId(traceId);

            // 尝试从 MDC 获取 spanId
            String spanId = org.slf4j.MDC.get("spanId");
            if (StringUtil.isEmpty(spanId)) {
                // 尝试从请求头获取 spanId
                HttpServletRequest request = ServletUtil.getRequest();
                if (request != null) {
                    spanId = request.getHeader("spanId");
                    if (StringUtil.isEmpty(spanId)) {
                        spanId = request.getHeader("X-Span-Id");
                    }
                }
            }
            logDTO.setSpanId(spanId);
        } catch (Exception e) {
            log.warn("获取调用链追踪信息失败: {}", e.getMessage());
        }
    }

    /**
     * 处理全局默认记录（即使没有注解也记录基本信息）
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    private Object processGlobalRecord(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否启用日志功能
        if (!isLogEnabled()) {
            return joinPoint.proceed();
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 构建基础日志对象（只记录基本信息）
        OperationLogDTO logDTO = buildBasicLogDTO(joinPoint);

        Object result = null;
        Throwable exception = null;

        try {
            // 执行方法
            result = joinPoint.proceed();
            logDTO.setStatus("SUCCESS");
        } catch (Throwable e) {
            exception = e;
            logDTO.setStatus("FAILED");

            // 记录异常信息（全局默认记录始终记录异常）
            logDTO.setException(e.getClass().getName() + ": " + e.getMessage());
            logDTO.setExceptionStack(getStackTrace(e));

            // 重新抛出异常
            throw e;
        } finally {
            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;
            logDTO.setCostTime(costTime);
            logDTO.setOperationTime(LocalDateTime.now());

            // 获取存储服务（使用全局配置或默认）
            ILogStorageService storageService = getStorageService(null);

            // 异步保存日志
            if (isAsyncEnabled()) {
                saveLogAsync(logDTO, storageService);
            } else {
                storageService.save(logDTO);
            }
        }

        return result;
    }

    /**
     * 构建基础日志对象（只记录基本信息，不记录参数和返回值）
     *
     * @param joinPoint 连接点
     * @return 日志对象
     */
    private OperationLogDTO buildBasicLogDTO(ProceedingJoinPoint joinPoint) {
        OperationLogDTO logDTO = new OperationLogDTO();

        // 设置方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        logDTO.setMethod(className + "#" + methodName);

        // 设置操作描述（使用类名+方法名）
        logDTO.setOperation(className + "." + methodName);

        // 设置请求信息（如果有 HTTP 请求上下文）
        HttpServletRequest request = ServletUtil.getRequest();
        if (request != null) {
            logDTO.setRequestPath(request.getRequestURI());
            logDTO.setRequestMethod(request.getMethod());
            logDTO.setClientIp(ClientIpUtil.getClientIp(request));
            logDTO.setUserAgent(request.getHeader("User-Agent"));
        }

        // 设置用户信息
        setUserInfo(logDTO);

        // 设置调用链追踪信息（traceId、spanId）
        setTraceInfo(logDTO);

        return logDTO;
    }

    /**
     * 设置用户信息
     *
     * @param logDTO 日志对象
     */
    private void setUserInfo(OperationLogDTO logDTO) {
        try {
            // 尝试从 Token 中获取用户信息
            if (tokenService != null && StringUtil.isNotEmpty(jwtSecret)) {
                try {
                    LoginUserDTO loginUser = tokenService.getLoginUser(jwtSecret);
                    if (loginUser != null) {
                        logDTO.setUserId(loginUser.getUserId());
                        logDTO.setUserName(loginUser.getUserName());
                        return;
                    }
                } catch (Exception e) {
                    // Token 解析失败，继续尝试其他方式
                }
            }

            // 尝试从请求头中获取用户ID（网关可能已经设置）
            HttpServletRequest request = ServletUtil.getRequest();
            if (request != null) {
                String userIdStr = request.getHeader("userId");
                if (StringUtil.isNotEmpty(userIdStr)) {
                    try {
                        logDTO.setUserId(Long.parseLong(userIdStr));
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
                String userName = request.getHeader("userName");
                if (StringUtil.isNotEmpty(userName)) {
                    logDTO.setUserName(userName);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
        }
    }

    /**
     * 提取方法参数
     *
     * @param joinPoint 连接点
     * @param logAction 日志注解
     * @return 参数 JSON 字符串
     */
    private String extractParams(ProceedingJoinPoint joinPoint, LogAction logAction) {
        // 如果配置了参数表达式，优先使用表达式结果
        if (StringUtil.isNotEmpty(logAction.paramsExpression())) {
            try {
                Object paramsObj = evaluateExpression(joinPoint, logAction.paramsExpression(), null);
                if (paramsObj != null) {
                    return JsonUtil.classToJson(paramsObj);
                }
            } catch (Exception e) {
                log.warn("执行参数表达式失败: {}, 使用完整参数对象", e.getMessage());
            }
        }

        // 使用完整参数对象
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());

        // 构建参数 Map
        Map<String, Object> paramsMap = new HashMap<>();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                Object arg = args[i];
                // 对敏感字段进行脱敏
                if (arg != null && StringUtil.isNotEmpty(logAction.desensitizeFields())) {
                    arg = desensitizeObject(arg, logAction.desensitizeFields());
                }
                paramsMap.put(parameterNames[i], arg);
            }
        }

        return JsonUtil.classToJson(paramsMap);
    }

    /**
     * 提取方法返回值
     *
     * @param result    方法返回值
     * @param logAction 日志注解
     * @param joinPoint 连接点（用于执行表达式）
     * @return 返回值 JSON 字符串
     */
    private String extractResult(Object result, LogAction logAction, ProceedingJoinPoint joinPoint) {
        // 如果配置了返回值表达式，优先使用表达式结果
        if (StringUtil.isNotEmpty(logAction.resultExpression()) && result != null) {
            try {
                Object resultObj = evaluateExpression(joinPoint, logAction.resultExpression(), result);
                if (resultObj != null) {
                    return JsonUtil.classToJson(resultObj);
                }
            } catch (Exception e) {
                log.warn("执行返回值表达式失败: {}, 使用完整返回值对象", e.getMessage());
            }
        }

        // 使用完整返回值对象
        return JsonUtil.classToJson(result);
    }

    /**
     * 对对象进行脱敏处理
     *
     * @param obj               原始对象
     * @param desensitizeFields 需要脱敏的字段（逗号分隔）
     * @return 脱敏后的对象
     */
    private Object desensitizeObject(Object obj, String desensitizeFields) {
        if (obj == null || StringUtil.isEmpty(desensitizeFields)) {
            return obj;
        }

        try {
            // 如果是 Map，遍历字段进行脱敏
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                String[] fields = desensitizeFields.split(",");
                for (String field : fields) {
                    field = field.trim();
                    Object value = map.get(field);
                    if (value instanceof String) {
                        // 根据字段名推断脱敏类型
                        String desensitizedValue = desensitizeByFieldName(field, (String) value);
                        map.put(field, desensitizedValue);
                    }
                }
                return map;
            }

            // 如果是对象，使用反射进行脱敏（简化实现，实际可以使用 BeanUtils）
            // 这里简化处理，直接返回原对象
            return obj;
        } catch (Exception e) {
            log.warn("脱敏处理失败: {}", e.getMessage());
            return obj;
        }
    }

    /**
     * 根据字段名推断脱敏类型并进行脱敏
     *
     * @param fieldName 字段名
     * @param value     字段值
     * @return 脱敏后的值
     */
    private String desensitizeByFieldName(String fieldName, String value) {
        if (StringUtil.isEmpty(value)) {
            return value;
        }

        String lowerFieldName = fieldName.toLowerCase();
        if (lowerFieldName.contains("phone") || lowerFieldName.contains("mobile")) {
            return DesensitizeUtil.desensitizePhone(value);
        } else if (lowerFieldName.contains("idcard") || lowerFieldName.contains("id_card")) {
            return DesensitizeUtil.desensitizeIdCard(value);
        } else if (lowerFieldName.contains("email")) {
            return DesensitizeUtil.desensitizeEmail(value);
        } else if (lowerFieldName.contains("password") || lowerFieldName.contains("pwd")) {
            return DesensitizeUtil.desensitizePassword(value);
        } else if (lowerFieldName.contains("bankcard") || lowerFieldName.contains("bank_card")) {
            return DesensitizeUtil.desensitizeBankCard(value);
        }

        return value;
    }

    /**
     * 执行 SpEL 表达式
     *
     * @param joinPoint  连接点
     * @param expression 表达式
     * @param result     方法返回值（可能为 null）
     * @return 表达式执行结果
     */
    private Object evaluateExpression(ProceedingJoinPoint joinPoint, String expression, Object result) {
        try {
            EvaluationContext context = buildEvaluationContext(joinPoint, result);
            Expression expr = expressionParser.parseExpression(expression);
            return expr.getValue(context);
        } catch (Exception e) {
            log.warn("执行 SpEL 表达式失败: {}, 表达式: {}", e.getMessage(), expression);
            throw e;
        }
    }

    /**
     * 执行条件表达式
     *
     * @param joinPoint 连接点
     * @param condition 条件表达式
     * @param result    方法返回值（可能为 null）
     * @return 条件是否满足
     */
    private boolean evaluateCondition(ProceedingJoinPoint joinPoint, String condition, Object result) {
        try {
            Object value = evaluateExpression(joinPoint, condition, result);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            // 非布尔值视为 true
            return value != null;
        } catch (Exception e) {
            log.warn("执行条件表达式失败: {}, 表达式: {}, 默认返回 true", e.getMessage(), condition);
            return true; // 表达式执行失败时，默认记录日志
        }
    }

    /**
     * 构建 SpEL 表达式上下文
     *
     * @param joinPoint 连接点
     * @param result    方法返回值（可能为 null）
     * @return 表达式上下文
     */
    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();

        // 设置方法参数
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("args", args);

        // 设置方法返回值
        if (result != null) {
            context.setVariable("result", result);
        }

        return context;
    }

    /**
     * 获取异常堆栈信息
     *
     * @param throwable 异常对象
     * @return 堆栈信息字符串
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 保存日志
     *
     * @param logDTO    日志对象
     * @param logAction 日志注解
     */
    private void saveLog(OperationLogDTO logDTO, LogAction logAction) {
        try {
            // 获取存储服务（根据注解或配置选择）
            ILogStorageService storageService = getStorageService(logAction);

            if (isAsyncEnabled()) {
                // 异步保存
                saveLogAsync(logDTO, storageService);
            } else {
                // 同步保存
                storageService.save(logDTO);
            }
        } catch (Exception e) {
            // 保存失败不应该影响业务，只记录错误日志
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步保存日志
     *
     * @param logDTO         日志对象
     * @param storageService 存储服务
     */
    @Async(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
    public void saveLogAsync(OperationLogDTO logDTO, ILogStorageService storageService) {
        storageService.save(logDTO);
    }

    /**
     * 获取存储服务
     * <p>
     * 优先级：方法注解 > 类注解 > Nacos 全局配置 > 默认存储服务
     * <p>
     * <b>Bean 名称规则：</b>
     * <ul>
     *     <li>console → consoleLogStorageService</li>
     *     <li>database → databaseLogStorageService</li>
     *     <li>file → fileLogStorageService</li>
     *     <li>redis → redisLogStorageService</li>
     *     <li>mq → mqLogStorageService</li>
     * </ul>
     *
     * @param logAction 日志注解（已合并方法注解和类注解），如果为 null 则使用全局配置
     * @return 存储服务实例
     */
    private ILogStorageService getStorageService(LogAction logAction) {
        String storageType = null;

        // 优先级1：如果 logAction 不为 null，从注解获取 storageType
        if (logAction != null) {
            storageType = logAction.storageType();
        }

        // 优先级2：如果方法注解和类注解都未设置，使用全局配置
        if (StringUtil.isEmpty(storageType)) {
            storageType = environment.getProperty(
                    LogConstants.NACOS_LOG_STORAGE_TYPE_PREFIX,
                    String.class,
                    LogConstants.STORAGE_TYPE_DEFAULT
            );
        }

        // 如果未配置或配置为默认值，使用默认存储服务
        if (StringUtil.isEmpty(storageType) || LogConstants.STORAGE_TYPE_DEFAULT.equals(storageType)) {
            return logStorageService;
        }

        // 根据存储类型构建 Bean 名称
        // Bean 名称格式：{storageType}LogStorageService（首字母小写）
        // 例如：console → consoleLogStorageService, database → databaseLogStorageService
        String beanName = buildStorageServiceBeanName(storageType);

        try {
            // 尝试从 Spring 容器中获取对应的存储服务Bean
            if (applicationContext.containsBean(beanName)) {
                ILogStorageService storageService = applicationContext.getBean(beanName, ILogStorageService.class);
                log.debug("使用存储服务: {}, Bean名称: {}", storageType, beanName);
                return storageService;
            } else {
                // Bean 不存在，使用默认存储服务
                log.warn("存储服务Bean不存在，使用默认存储服务。存储类型: {}, Bean名称: {}",
                        storageType, beanName);
                return logStorageService;
            }
        } catch (Exception e) {
            // 获取失败，使用默认存储服务
            log.warn("获取存储服务失败，使用默认存储服务。存储类型: {}, Bean名称: {}, 错误: {}",
                    storageType, beanName, e.getMessage());
            return logStorageService;
        }
    }

    /**
     * 构建存储服务 Bean 名称
     * <p>
     * 将存储类型转换为 Bean 名称，格式：{storageType}LogStorageService（首字母小写）
     * <p>
     * <b>示例：</b>
     * <ul>
     *     <li>console → consoleLogStorageService</li>
     *     <li>database → databaseLogStorageService</li>
     *     <li>file → fileLogStorageService</li>
     *     <li>redis → redisLogStorageService</li>
     *     <li>mq → mqLogStorageService</li>
     * </ul>
     *
     * @param storageType 存储类型
     * @return Bean 名称
     */
    private String buildStorageServiceBeanName(String storageType) {
        // 首字母大写，然后拼接 LogStorageService
        String className = Character.toUpperCase(storageType.charAt(0)) + storageType.substring(1) + "LogStorageService";
        // 首字母小写
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * 检查是否启用日志功能
     *
     * @return 是否启用
     */
    private boolean isLogEnabled() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_ENABLED_PREFIX,
                Boolean.class,
                LogConstants.LOG_ENABLED_DEFAULT
        );
    }

    /**
     * 检查是否启用全局默认记录（即使没有注解也记录基本信息）
     *
     * @return true - 启用；false - 禁用
     */
    private boolean isGlobalRecordEnabled() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_GLOBAL_RECORD_ENABLED_PREFIX,
                Boolean.class,
                LogConstants.GLOBAL_RECORD_ENABLED_DEFAULT
        );
    }

    /**
     * 检查是否启用异步处理
     *
     * @return 是否启用异步
     */
    private boolean isAsyncEnabled() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_ASYNC_ENABLED_PREFIX,
                Boolean.class,
                LogConstants.LOG_ASYNC_ENABLED_DEFAULT
        );
    }

    /**
     * 获取全局默认是否记录参数
     *
     * @return 全局默认配置值
     */
    private boolean getGlobalDefaultRecordParams() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_DEFAULT_RECORD_PARAMS_PREFIX,
                Boolean.class,
                LogConstants.DEFAULT_RECORD_PARAMS
        );
    }

    /**
     * 获取全局默认是否记录返回值
     *
     * @return 全局默认配置值
     */
    private boolean getGlobalDefaultRecordResult() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_DEFAULT_RECORD_RESULT_PREFIX,
                Boolean.class,
                LogConstants.DEFAULT_RECORD_RESULT
        );
    }

    /**
     * 获取全局默认是否记录异常
     *
     * @return 全局默认配置值
     */
    private boolean getGlobalDefaultRecordException() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_DEFAULT_RECORD_EXCEPTION_PREFIX,
                Boolean.class,
                LogConstants.DEFAULT_RECORD_EXCEPTION
        );
    }

    /**
     * 获取全局默认异常时是否抛出异常
     *
     * @return 全局默认配置值
     */
    private boolean getGlobalDefaultThrowException() {
        return environment.getProperty(
                LogConstants.NACOS_LOG_DEFAULT_THROW_EXCEPTION_PREFIX,
                Boolean.class,
                LogConstants.DEFAULT_THROW_EXCEPTION
        );
    }

    /**
     * 获取方法上的注解
     *
     * @param joinPoint 连接点
     * @return 方法注解，如果不存在返回 null
     */
    private LogAction getMethodAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(LogAction.class);
    }

    /**
     * 获取类上的注解
     *
     * @param joinPoint 连接点
     * @return 类注解，如果不存在返回 null
     */
    private LogAction getClassAnnotation(ProceedingJoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return targetClass.getAnnotation(LogAction.class);
    }

    /**
     * 合并方法注解、类注解和全局默认策略
     * <p>
     * 优先级：方法注解 > 类注解 > Nacos 全局默认策略
     * <p>
     * 合并规则：
     * <ul>
     *     <li>方法注解必须存在（调用此方法前已校验）</li>
     *     <li>方法注解的配置优先，完全覆盖类注解和全局配置</li>
     *     <li>如果方法注解的某个属性为默认值，使用类注解的值</li>
     *     <li>如果类注解也不存在或为默认值，使用 Nacos 全局默认配置</li>
     * </ul>
     * <p>
     * <b>设计理念：</b>
     * <ul>
     *     <li>方法注解是核心：必须存在，用于记录具体业务操作（value 必填）</li>
     *     <li>类注解是辅助：提供默认策略，如全局开启参数记录、异步写入等</li>
     *     <li>Nacos 全局配置是兜底：提供全局默认值，可通过配置中心动态调整</li>
     *     <li>方法注解可以覆盖类注解和全局配置的所有配置</li>
     * </ul>
     *
     * @param methodLogAction 方法注解（必不为 null）
     * @param classLogAction  类注解（可能为 null）
     * @return 合并后的注解配置（使用代理对象）
     */
    private LogAction mergeLogAction(LogAction methodLogAction, LogAction classLogAction) {
        // 创建合并后的注解代理对象
        // 由于 Java 注解的特性，无法动态创建注解对象，这里使用代理模式
        // 规则：方法注解的属性优先，如果为默认值则使用类注解的值，再使用全局配置兜底
        return new LogAction() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LogAction.class;
            }

            @Override
            public String value() {
                // value 必须从方法注解获取（方法注解的 value 是必填的）
                return methodLogAction.value();
            }

            @Override
            public boolean recordParams() {
                // 三层策略合并：方法注解 > 类注解 > 全局默认配置
                if (methodLogAction.recordParams()) {
                    return true; // 方法注解为 true，直接返回
                }
                if (classLogAction != null && classLogAction.recordParams()) {
                    return true; // 类注解为 true，使用类注解的值
                }
                // 使用全局默认配置（从 Nacos 读取，如果不存在则使用代码默认值）
                return getGlobalDefaultRecordParams();
            }

            @Override
            public boolean recordResult() {
                // 三层策略合并：方法注解 > 类注解 > 全局默认配置
                if (methodLogAction.recordResult()) {
                    return true; // 方法注解为 true，直接返回
                }
                if (classLogAction != null && classLogAction.recordResult()) {
                    return true; // 类注解为 true，使用类注解的值
                }
                // 使用全局默认配置（从 Nacos 读取，如果不存在则使用代码默认值）
                return getGlobalDefaultRecordResult();
            }

            @Override
            public boolean recordException() {
                // recordException 默认是 true
                // 三层策略合并：方法注解 > 类注解 > 全局默认配置
                if (!methodLogAction.recordException()) {
                    return false; // 方法注解显式设置为 false，直接返回 false
                }
                if (classLogAction != null && !classLogAction.recordException()) {
                    return false; // 类注解显式设置为 false，返回 false
                }
                // 使用全局默认配置（从 Nacos 读取，如果不存在则使用代码默认值）
                return getGlobalDefaultRecordException();
            }

            @Override
            public boolean throwException() {
                // throwException 默认是 true，三层策略合并同上
                if (!methodLogAction.throwException()) {
                    return false; // 方法注解显式设置为 false，直接返回 false
                }
                if (classLogAction != null && !classLogAction.throwException()) {
                    return false; // 类注解显式设置为 false，返回 false
                }
                // 使用全局默认配置（从 Nacos 读取，如果不存在则使用代码默认值）
                return getGlobalDefaultThrowException();
            }

            @Override
            public String condition() {
                // 方法注解优先
                return StringUtil.isNotEmpty(methodLogAction.condition())
                        ? methodLogAction.condition()
                        : classLogAction.condition();
            }

            @Override
            public String paramsExpression() {
                return StringUtil.isNotEmpty(methodLogAction.paramsExpression())
                        ? methodLogAction.paramsExpression()
                        : classLogAction.paramsExpression();
            }

            @Override
            public String resultExpression() {
                return StringUtil.isNotEmpty(methodLogAction.resultExpression())
                        ? methodLogAction.resultExpression()
                        : classLogAction.resultExpression();
            }

            @Override
            public String module() {
                return StringUtil.isNotEmpty(methodLogAction.module())
                        ? methodLogAction.module()
                        : classLogAction.module();
            }

            @Override
            public String businessType() {
                return StringUtil.isNotEmpty(methodLogAction.businessType())
                        ? methodLogAction.businessType()
                        : classLogAction.businessType();
            }

            @Override
            public String desensitizeFields() {
                return StringUtil.isNotEmpty(methodLogAction.desensitizeFields())
                        ? methodLogAction.desensitizeFields()
                        : (classLogAction != null ? classLogAction.desensitizeFields() : "");
            }

            @Override
            public String storageType() {
                // 存储类型合并：方法注解优先，类注解补充
                return StringUtil.isNotEmpty(methodLogAction.storageType())
                        ? methodLogAction.storageType()
                        : (classLogAction != null ? classLogAction.storageType() : "");
            }
        };
    }
}
