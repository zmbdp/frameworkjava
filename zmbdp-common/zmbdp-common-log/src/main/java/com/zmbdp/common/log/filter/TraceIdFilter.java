package com.zmbdp.common.log.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器 - 微服务端
 * <p>
 * 负责在微服务接收到请求时，从请求头中提取 traceId 并设置到 MDC 中，
 * 实现全链路追踪和日志关联。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>从请求头中提取 traceId</li>
 *     <li>如果请求头中没有 traceId，生成一个新的</li>
 *     <li>将 traceId 设置到 MDC 中，用于日志输出</li>
 *     <li>请求结束后清理 MDC，避免内存泄漏</li>
 * </ul>
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *     <li>网关生成 traceId 并通过请求头传递</li>
 *     <li>微服务 A 接收请求，本过滤器提取 traceId 并设置到 MDC</li>
 *     <li>微服务 A 的日志输出包含 traceId</li>
 *     <li>微服务 A 调用微服务 B，Feign 拦截器传递 traceId</li>
 *     <li>微服务 B 接收请求，本过滤器提取 traceId 并设置到 MDC</li>
 *     <li>整个调用链的 traceId 保持一致</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 无需手动调用，Spring Boot 自动注册
 * // 日志配置中使用 %X{traceId} 即可输出 traceId
 *
 * logging:
 *   pattern:
 *     console: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] %-5level [%thread] %logger{36} : %msg%n'
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>优先级设置为 -100，确保在其他过滤器之前执行</li>
 *     <li>必须在 finally 块中清理 MDC，避免内存泄漏</li>
 *     <li>支持从多个请求头获取 traceId（X-Trace-Id、traceId）</li>
 *     <li>如果所有来源都没有 traceId，会生成一个新的</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see MDC
 */
@Slf4j
@Order(-100)
public class TraceIdFilter implements Filter {

    /**
     * TraceId 请求头名称（标准格式）
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * TraceId 请求头名称（备用格式）
     */
    private static final String TRACE_ID_HEADER_ALT = "traceId";

    /**
     * MDC 中的 traceId 键名
     */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 过滤器初始化
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        log.info("TraceIdFilter 初始化完成");
    }

    /**
     * 过滤器核心逻辑
     * <p>
     * 从请求头中提取 traceId 并设置到 MDC 中，确保日志输出包含 traceId。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>从请求头中提取 traceId（支持多种格式）</li>
     *     <li>如果没有 traceId，生成一个新的</li>
     *     <li>将 traceId 设置到 MDC 中</li>
     *     <li>继续执行过滤器链</li>
     *     <li>请求结束后清理 MDC</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 finally 块中清理 MDC</li>
     *     <li>异常不会影响请求处理</li>
     *     <li>支持从 X-Trace-Id 和 traceId 两种请求头获取</li>
     * </ul>
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 1. 获取或生成 traceId（优先使用 SkyWalking 的 traceId）
            String traceId = getOrGenerateTraceId(httpRequest);

            // 2. 将 traceId 设置到 MDC 中
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 3. 继续执行过滤器链
            chain.doFilter(request, response);
        } finally {
            // 4. 清理 MDC，避免内存泄漏
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 获取或生成 traceId
     * <p>
     * 优先级：
     * <ol>
     *     <li>请求头中的 traceId（上游服务传递过来的）</li>
     *     <li>生成新的 traceId（如果是第一个服务）</li>
     * </ol>
     *
     * @param request HTTP 请求对象
     * @return traceId
     */
    private String getOrGenerateTraceId(HttpServletRequest request) {
        // 1. 优先从请求头获取 traceId（上游服务通过 Feign 拦截器传递过来的）
        String traceId = getTraceIdFromHeader(request);
        if (traceId != null && !traceId.isEmpty()) {
            log.debug("使用请求头 traceId: {}", traceId);
            return traceId;
        }

        // 2. 如果请求头没有，说明是第一个服务（网关或直接调用），生成新的 traceId
        traceId = generateTraceId();
        log.debug("生成新的 traceId: {}", traceId);
        return traceId;
    }

    /**
     * 从请求头中获取 traceId
     * <p>
     * 支持多种请求头格式：
     * <ul>
     *     <li>X-Trace-Id（标准格式，优先级最高）</li>
     *     <li>traceId（备用格式）</li>
     * </ul>
     *
     * @param request HTTP 请求对象
     * @return traceId，如果不存在返回 null
     */
    private String getTraceIdFromHeader(HttpServletRequest request) {
        // 优先从标准请求头获取
        String traceId = request.getHeader(TRACE_ID_HEADER);

        // 如果标准请求头没有，尝试备用请求头
        if (traceId == null || traceId.isEmpty()) {
            traceId = request.getHeader(TRACE_ID_HEADER_ALT);
        }

        return traceId;
    }

    /**
     * 生成全局唯一的 traceId
     * <p>
     * 使用 UUID 去掉横线，生成 32 位的 traceId
     *
     * @return traceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 过滤器销毁
     */
    @Override
    public void destroy() {
        log.info("TraceIdFilter 销毁");
    }
}