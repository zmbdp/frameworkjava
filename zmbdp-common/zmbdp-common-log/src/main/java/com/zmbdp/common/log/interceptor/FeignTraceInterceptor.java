package com.zmbdp.common.log.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Feign 请求拦截器 - 链路追踪
 * <p>
 * 负责在 Feign 调用时传递 traceId，实现跨服务的全链路追踪。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>从 MDC 中获取当前请求的 traceId</li>
 *     <li>将 traceId 添加到 Feign 请求头中</li>
 *     <li>确保下游服务能够接收到相同的 traceId</li>
 *     <li>实现分布式链路追踪</li>
 * </ul>
 * <p>
 * <b>工作原理：</b>
 * <ol>
 *     <li>网关层生成 traceId 并设置到 MDC</li>
 *     <li>请求进入微服务后，通过过滤器将 traceId 设置到 MDC</li>
 *     <li>微服务调用其他服务时，本拦截器从 MDC 获取 traceId</li>
 *     <li>将 traceId 添加到 Feign 请求头，传递给下游服务</li>
 *     <li>下游服务接收到请求后，再次将 traceId 设置到 MDC</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 网关层生成 traceId
 * String traceId = UUID.randomUUID().toString();
 * MDC.put("traceId", traceId);
 *
 * // 2. 微服务 A 调用微服务 B
 * fileServiceApi.upload(file);  // Feign 调用
 *
 * // 3. 本拦截器自动将 traceId 添加到请求头
 * // 4. 微服务 B 接收到请求，traceId 保持一致
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>需要配合过滤器使用，确保 MDC 中有 traceId</li>
 *     <li>如果 MDC 中没有 traceId，不会添加请求头</li>
 *     <li>支持多级服务调用，traceId 始终保持一致</li>
 *     <li>建议在网关层统一生成 traceId</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see MDC
 * @see RequestInterceptor
 */
@Slf4j
public class FeignTraceInterceptor implements RequestInterceptor {

    /**
     * TraceId 请求头名称
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 中的 traceId 键名
     */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 在 Feign 请求发送前，将 traceId 添加到请求头
     * <p>
     * 从 MDC 中获取当前请求的 traceId，并添加到 Feign 请求头中，
     * 确保下游服务能够接收到相同的 traceId，实现全链路追踪。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>从 MDC 中获取 traceId</li>
     *     <li>如果 traceId 存在，添加到请求头</li>
     *     <li>如果 traceId 不存在，记录警告日志</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 MDC 中没有 traceId，不会添加请求头</li>
     *     <li>建议在过滤器或拦截器中统一设置 MDC 的 traceId</li>
     *     <li>异常不会影响 Feign 调用，只会记录日志</li>
     * </ul>
     *
     * @param template Feign 请求模板
     * @see MDC#get(String)
     */
    @Override
    public void apply(RequestTemplate template) {
        try {
            // 从 MDC 中获取 traceId
            // MDC 是 ThreadLocal 的，在当前线程（发起 Feign 调用的线程）中有效
            // TraceIdFilter 已经在请求进入时将 traceId 设置到 MDC 中了
            // 这里直接获取并传递给下游服务即可
            String traceId = MDC.get(TRACE_ID_MDC_KEY);

            if (traceId != null && !traceId.isEmpty()) {
                // 将 traceId 添加到 Feign 请求头，下游服务的 TraceIdFilter 会从请求头中提取
                template.header(TRACE_ID_HEADER, traceId);
                log.debug("Feign 请求添加 traceId: {}, 目标服务: {}", traceId, template.url());
            } else {
                log.warn("MDC 中没有 traceId，无法传递到下游服务，目标服务: {}", template.url());
            }
        } catch (Exception e) {
            log.error("Feign 拦截器添加 traceId 失败: {}", e.getMessage(), e);
        }
    }
}