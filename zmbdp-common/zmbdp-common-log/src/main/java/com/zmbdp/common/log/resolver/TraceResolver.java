package com.zmbdp.common.log.resolver;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 调用链追踪解析器
 * <p>
 * 负责从 MDC、请求头等来源解析 traceId、spanId 并填充到日志 DTO 中。<br>
 * 支持分布式链路追踪，便于跨服务日志关联和问题排查。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>解析 traceId：从 MDC 或请求头获取链路追踪 ID</li>
 *     <li>解析 spanId：从 MDC 或请求头获取跨度 ID</li>
 *     <li>多来源支持：支持从 MDC、标准请求头、自定义请求头获取</li>
 *     <li>异常容错处理：解析失败时不影响日志记录</li>
 * </ul>
 * <p>
 * <b>解析优先级：</b>
 * <ul>
 *     <li><b>traceId 解析顺序：</b>MDC.get("traceId") → Header("traceId") → Header("X-Trace-Id")</li>
 *     <li><b>spanId 解析顺序：</b>MDC.get("spanId") → Header("spanId") → Header("X-Span-Id")</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private TraceResolver traceResolver;
 *
 * OperationLogDTO logDTO = new OperationLogDTO();
 * // 填充调用链追踪信息
 * traceResolver.fill(logDTO);
 * // logDTO.getTraceId() 和 logDTO.getSpanId() 已被填充
 * }</pre>
 * <p>
 * <b>与链路追踪系统集成：</b>
 * <ul>
 *     <li>支持 Sleuth、SkyWalking 等链路追踪系统</li>
 *     <li>支持自定义链路追踪实现（通过 MDC 或请求头传递）</li>
 *     <li>支持标准的 X-Trace-Id 和 X-Span-Id 请求头</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
 *     <li>如果无法获取追踪信息，traceId 和 spanId 保持为 null</li>
 *     <li>MDC 中的追踪信息优先级高于请求头</li>
 *     <li>建议在网关或过滤器中统一设置 traceId 和 spanId 到 MDC</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see org.slf4j.MDC
 * @see OperationLogDTO
 */
@Slf4j
@Component
public class TraceResolver {

    /**
     * 填充调用链追踪信息到日志 DTO
     * <p>
     * 按优先级依次尝试从不同来源获取 traceId 和 spanId 并填充到日志 DTO 中。<br>
     * 所有异常都会被捕获，不会影响日志记录流程。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>从 MDC 获取 traceId，如果不存在则从请求头获取</li>
     *     <li>请求头支持两种格式：traceId 和 X-Trace-Id</li>
     *     <li>从 MDC 获取 spanId，如果不存在则从请求头获取</li>
     *     <li>请求头支持两种格式：spanId 和 X-Span-Id</li>
     *     <li>将获取到的 traceId 和 spanId 填充到日志 DTO</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * OperationLogDTO logDTO = new OperationLogDTO();
     * traceResolver.fill(logDTO);
     * // 追踪信息已填充
     * String traceId = logDTO.getTraceId();
     * String spanId = logDTO.getSpanId();
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 logDTO 为 null，可能抛出 NullPointerException</li>
     *     <li>如果所有来源都无法获取追踪信息，traceId 和 spanId 保持为 null</li>
     *     <li>MDC 中的追踪信息优先级高于请求头</li>
     *     <li>异常会被记录到日志中，但不会影响日志记录流程</li>
     *     <li>建议在过滤器或拦截器中统一设置 MDC 追踪信息</li>
     * </ul>
     *
     * @param logDTO 操作日志 DTO，不能为 null
     * @see org.slf4j.MDC#get(String)
     * @see ServletUtil#getRequest()
     */
    public void fill(OperationLogDTO logDTO) {
        try {
            // 解析 traceId
            String traceId = org.slf4j.MDC.get("traceId");
            if (StringUtil.isEmpty(traceId)) {
                HttpServletRequest request = ServletUtil.getRequest();
                if (request != null) {
                    traceId = request.getHeader("traceId");
                    if (StringUtil.isEmpty(traceId)) {
                        traceId = request.getHeader("X-Trace-Id");
                    }
                }
            }
            logDTO.setTraceId(traceId);

            // 解析 spanId
            String spanId = org.slf4j.MDC.get("spanId");
            if (StringUtil.isEmpty(spanId)) {
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
}