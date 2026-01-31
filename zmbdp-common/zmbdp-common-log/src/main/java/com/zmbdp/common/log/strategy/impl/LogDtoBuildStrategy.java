package com.zmbdp.common.log.strategy.impl;

import com.zmbdp.common.core.utils.ClientIpUtil;
import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.constants.HttpConstants;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.resolver.TraceResolver;
import com.zmbdp.common.log.resolver.UserContextResolver;
import com.zmbdp.common.log.strategy.ILogProcessStrategy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 日志 DTO 构建策略
 * <p>
 * 实现 {@link ILogProcessStrategy} 接口，提供操作日志 DTO 的构建功能。<br>
 * 负责构建 OperationLogDTO 的基础信息：操作描述、方法信息、请求信息，并委托解析器填充用户和追踪信息。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>构建带注解的日志 DTO：从 {@link LogAction} 注解中提取信息</li>
 *     <li>构建基础日志 DTO：无注解时构建基础日志信息</li>
 *     <li>填充方法信息：类名、方法名</li>
 *     <li>填充请求信息：请求路径、请求方法、客户端 IP、User-Agent</li>
 *     <li>填充用户信息：委托 {@link UserContextResolver} 填充</li>
 *     <li>填充追踪信息：委托 {@link TraceResolver} 填充</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private LogDtoBuildStrategy dtoBuildStrategy;
 *
 * // 构建带注解的日志 DTO
 * OperationLogDTO logDTO = dtoBuildStrategy.buildFromAnnotation(joinPoint, logAction);
 *
 * // 构建基础日志 DTO（无注解）
 * OperationLogDTO logDTO = dtoBuildStrategy.buildBasic(joinPoint);
 * }</pre>
 * <p>
 * <b>DTO 字段说明：</b>
 * <ul>
 *     <li><b>operation</b>：操作描述（从注解 value 获取，或使用类名.方法名）</li>
 *     <li><b>module</b>：模块名称（从注解 module 获取）</li>
 *     <li><b>businessType</b>：业务类型（从注解 businessType 获取）</li>
 *     <li><b>method</b>：方法全路径（类名#方法名）</li>
 *     <li><b>requestPath</b>：请求路径（从 HttpServletRequest 获取）</li>
 *     <li><b>requestMethod</b>：请求方法（GET、POST 等）</li>
 *     <li><b>clientIp</b>：客户端 IP</li>
 *     <li><b>userAgent</b>：User-Agent</li>
 *     <li><b>userId</b>：用户 ID（由 UserContextResolver 填充）</li>
 *     <li><b>userName</b>：用户名（由 UserContextResolver 填充）</li>
 *     <li><b>traceId</b>：链路追踪 ID（由 TraceResolver 填充）</li>
 *     <li><b>spanId</b>：跨度 ID（由 TraceResolver 填充）</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>如果不在 HTTP 请求上下文中，请求相关字段保持为 null</li>
 *     <li>用户信息和追踪信息由对应的解析器填充，填充失败不影响日志记录</li>
 *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogProcessStrategy
 * @see UserContextResolver
 * @see TraceResolver
 * @see com.zmbdp.common.log.domain.dto.OperationLogDTO
 */
@Slf4j
@Component
public class LogDtoBuildStrategy implements ILogProcessStrategy {

    /**
     * 用户上下文解析器
     * <p>
     * 用于从 Token、请求头等来源解析用户信息。
     */
    @Autowired
    private UserContextResolver userContextResolver;

    /**
     * 调用链追踪解析器
     * <p>
     * 用于从 MDC、请求头等来源解析追踪信息。
     */
    @Autowired
    private TraceResolver traceResolver;

    @Override
    public String getType() {
        return "DTO_BUILD";
    }

    /**
     * 构建带注解的完整日志 DTO
     * <p>
     * 从 {@link LogAction} 注解中提取操作描述、模块名称、业务类型等信息，<br>
     * 并填充方法信息、请求信息、用户信息、追踪信息。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>创建 OperationLogDTO 对象</li>
     *     <li>从注解中提取操作描述、模块名称、业务类型</li>
     *     <li>调用 {@link #fillMethodAndRequest(ProceedingJoinPoint, OperationLogDTO)} 填充方法和请求信息</li>
     *     <li>调用 {@link UserContextResolver#fill(OperationLogDTO)} 填充用户信息</li>
     *     <li>调用 {@link TraceResolver#fill(OperationLogDTO)} 填充追踪信息</li>
     *     <li>返回构建好的 DTO</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * @LogAction(value = "用户登录", module = "用户模块", businessType = "登录")
     * public Result login(LoginDTO loginDTO) { ... }
     *
     * // 构建日志 DTO
     * OperationLogDTO logDTO = dtoBuildStrategy.buildFromAnnotation(joinPoint, logAction);
     * // logDTO.getOperation() = "用户登录"
     * // logDTO.getModule() = "用户模块"
     * // logDTO.getBusinessType() = "登录"
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 joinPoint 或 logAction 为 null，可能抛出 NullPointerException</li>
     *     <li>用户信息和追踪信息填充失败不影响日志记录</li>
     *     <li>如果不在 HTTP 请求上下文中，请求相关字段保持为 null</li>
     * </ul>
     *
     * @param joinPoint 连接点，不能为 null
     * @param logAction 日志注解，不能为 null
     * @return 构建好的操作日志 DTO
     * @see LogAction
     * @see #fillMethodAndRequest(ProceedingJoinPoint, OperationLogDTO)
     */
    public OperationLogDTO buildFromAnnotation(ProceedingJoinPoint joinPoint, LogAction logAction) {
        // 创建日志 DTO 对象
        OperationLogDTO logDTO = new OperationLogDTO();
        // 设置操作描述（从注解获取）
        logDTO.setOperation(logAction.value());
        // 设置模块名称（从注解获取）
        logDTO.setModule(logAction.module());
        // 设置业务类型（从注解获取）
        logDTO.setBusinessType(logAction.businessType());
        // 填充方法和请求信息
        fillMethodAndRequest(joinPoint, logDTO);
        // 填充用户信息（用户 ID、用户名）
        userContextResolver.fill(logDTO);
        // 填充追踪信息（traceId、spanId）
        traceResolver.fill(logDTO);
        return logDTO;
    }

    /**
     * 构建全局默认记录的基础日志 DTO（无注解）
     * <p>
     * 当方法没有 {@link LogAction} 注解时，构建基础日志信息。<br>
     * 操作描述使用类名.方法名，不包含模块名称和业务类型。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>创建 OperationLogDTO 对象</li>
     *     <li>获取方法签名信息（类名、方法名）</li>
     *     <li>设置 method 字段为"类名#方法名"</li>
     *     <li>设置 operation 字段为"类名.方法名"</li>
     *     <li>调用 {@link #fillMethodAndRequest(ProceedingJoinPoint, OperationLogDTO)} 填充方法和请求信息</li>
     *     <li>调用 {@link UserContextResolver#fill(OperationLogDTO)} 填充用户信息</li>
     *     <li>调用 {@link TraceResolver#fill(OperationLogDTO)} 填充追踪信息</li>
     *     <li>返回构建好的 DTO</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 无注解的方法
     * public Result getUser(Long userId) { ... }
     *
     * // 构建基础日志 DTO
     * OperationLogDTO logDTO = dtoBuildStrategy.buildBasic(joinPoint);
     * // logDTO.getOperation() = "com.example.UserService.getUser"
     * // logDTO.getMethod() = "com.example.UserService#getUser"
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 joinPoint 为 null，可能抛出 NullPointerException</li>
     *     <li>module 和 businessType 字段保持为 null</li>
     *     <li>用户信息和追踪信息填充失败不影响日志记录</li>
     *     <li>如果不在 HTTP 请求上下文中，请求相关字段保持为 null</li>
     * </ul>
     *
     * @param joinPoint 连接点，不能为 null
     * @return 构建好的操作日志 DTO
     * @see #fillMethodAndRequest(ProceedingJoinPoint, OperationLogDTO)
     */
    public OperationLogDTO buildBasic(ProceedingJoinPoint joinPoint) {
        OperationLogDTO logDTO = new OperationLogDTO();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        // 设置方法签名：类名#方法名
        logDTO.setMethod(className + CommonConstants.HASH_SEPARATOR + methodName);
        // 设置操作描述：类名.方法名
        logDTO.setOperation(className + CommonConstants.DOT_SEPARATOR + methodName);
        // 填充方法和请求信息
        fillMethodAndRequest(joinPoint, logDTO);
        // 填充用户信息
        userContextResolver.fill(logDTO);
        // 填充追踪信息
        traceResolver.fill(logDTO);
        return logDTO;
    }

    /**
     * 填充方法和请求信息
     * <p>
     * 填充方法全路径（类名#方法名）和 HTTP 请求相关信息。<br>
     * 如果不在 HTTP 请求上下文中，请求相关字段保持为 null。
     * <p>
     * <b>填充字段：</b>
     * <ul>
     *     <li><b>method</b>：方法全路径（类名#方法名）</li>
     *     <li><b>requestPath</b>：请求路径（如 /api/user/login）</li>
     *     <li><b>requestMethod</b>：请求方法（GET、POST、PUT、DELETE 等）</li>
     *     <li><b>clientIp</b>：客户端 IP 地址</li>
     *     <li><b>userAgent</b>：User-Agent 请求头</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果不在 HTTP 请求上下文中，请求相关字段保持为 null</li>
     *     <li>客户端 IP 获取支持代理服务器（X-Forwarded-For、X-Real-IP 等）</li>
     *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
     * </ul>
     *
     * @param joinPoint 连接点，不能为 null
     * @param logDTO    操作日志 DTO，不能为 null
     * @see com.zmbdp.common.core.utils.ServletUtil#getRequest()
     * @see com.zmbdp.common.core.utils.ClientIpUtil#getClientIp(HttpServletRequest)
     */
    private void fillMethodAndRequest(ProceedingJoinPoint joinPoint, OperationLogDTO logDTO) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 设置方法全路径：类名#方法名
        logDTO.setMethod(method.getDeclaringClass().getName() + CommonConstants.HASH_SEPARATOR + method.getName());

        // 获取 HTTP 请求对象
        HttpServletRequest request = ServletUtil.getRequest();
        if (request != null) {
            // 设置请求路径
            logDTO.setRequestPath(request.getRequestURI());
            // 设置请求方法（GET、POST 等）
            logDTO.setRequestMethod(request.getMethod());
            // 设置客户端 IP
            logDTO.setClientIp(ClientIpUtil.getClientIp(request));
            // 设置 User-Agent
            logDTO.setUserAgent(request.getHeader(HttpConstants.HEADER_USER_AGENT));
        }
    }
}