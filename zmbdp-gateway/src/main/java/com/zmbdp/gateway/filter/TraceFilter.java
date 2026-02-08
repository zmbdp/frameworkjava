package com.zmbdp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 链路追踪过滤器
 * <p>
 * 负责在网关层生成 traceId，并传递给下游服务，实现全链路追踪。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>生成全局唯一的 traceId</li>
 *     <li>将 traceId 设置到 MDC 中，用于日志输出</li>
 *     <li>将 traceId 添加到请求头，传递给下游服务</li>
 *     <li>请求结束后清理 MDC，避免内存泄漏</li>
 * </ul>
 * <p>
 * <b>执行顺序：</b>
 * <ul>
 *     <li>优先级：-300（在 AuthFilter 之前执行）</li>
 *     <li>确保所有请求都能获得 traceId</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class TraceFilter implements GlobalFilter, Ordered {

    /**
     * TraceId 请求头名称
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 中的 traceId 键名
     */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 链路追踪过滤逻辑
     *
     * @param exchange 服务器 Web 交换
     * @param chain    网关过滤器链
     * @return 服务器 Web 响应
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取或生成 traceId
        ServerHttpRequest request = exchange.getRequest();
        String traceId = getOrGenerateTraceId(request);

        // 2. 将 traceId 设置到 MDC 中（用于日志输出）
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        // 3. 将 traceId 添加到请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // 4. 继续执行过滤器链，并在完成后清理 MDC
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    // 清理 MDC，避免内存泄漏
                    MDC.remove(TRACE_ID_MDC_KEY);
                });
    }

    /**
     * 获取或生成 traceId
     * <p>
     * 优先级：
     * <ol>
     *     <li>SkyWalking 的 traceId（如果配置了 SkyWalking Agent）</li>
     *     <li>请求头中的 traceId</li>
     *     <li>生成新的 traceId</li>
     * </ol>
     *
     * @param request HTTP 请求
     * @return traceId
     */
    private String getOrGenerateTraceId(ServerHttpRequest request) {
        // 1. 优先使用 SkyWalking 的 traceId（如果配置了 Agent）
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            log.debug("使用 SkyWalking traceId: {}", traceId);
            return traceId;
        }

        // 2. 从请求头获取 traceId
        traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            log.debug("使用请求头 traceId: {}", traceId);
            return traceId;
        }

        // 3. 生成新的 traceId
        traceId = generateTraceId();
        log.debug("生成新的 traceId: {}", traceId);
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
     * 获取过滤器的执行顺序
     * <p>
     * 优先级设置为 -300，确保在 AuthFilter（-200）之前执行
     *
     * @return 过滤器的执行顺序
     */
    @Override
    public int getOrder() {
        return -300;
    }
}