package com.zmbdp.common.core.utils;

import com.zmbdp.common.domain.constants.RateLimitConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 频控工具类
 * <p>
 * 提供频控相关的工具方法，主要用于从 HTTP 请求中提取客户端真实 IP。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>频控限流：需要根据客户端 IP 进行限流控制</li>
 *     <li>日志记录：记录请求的真实客户端 IP</li>
 *     <li>安全审计：追踪请求来源</li>
 * </ul>
 * <p>
 * <b>IP 获取优先级：</b>
 * <ol>
 *     <li><b>X-Forwarded-For</b>：代理服务器转发的原始客户端 IP（取第一个，因为可能有多层代理）</li>
 *     <li><b>X-Real-IP</b>：Nginx 等反向代理设置的客户端真实 IP</li>
 *     <li><b>getRemoteAddr()</b>：直接连接的客户端 IP（可能是代理服务器 IP）</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>工具类设计为不可实例化（私有构造方法）</li>
 *     <li>所有方法均为静态方法，可直接调用</li>
 *     <li>自动过滤 "unknown" 等无效 IP 值</li>
 *     <li>无法获取 IP 时返回 "0.0.0.0"（避免 null）</li>
 *     <li>X-Forwarded-For 可能包含多个 IP（逗号分隔），只取第一个（最原始的客户端 IP）</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see HttpServletRequest
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RateLimitUtil {

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 从 HTTP 请求中提取客户端 IP，支持多种代理场景。
     * <p>
     * <b>获取流程：</b>
     * <ol>
     *     <li>优先从 {@code X-Forwarded-For} 请求头获取（取第一个 IP）</li>
     *     <li>如果失败，从 {@code X-Real-IP} 请求头获取</li>
     *     <li>如果仍失败，使用 {@code request.getRemoteAddr()} 获取</li>
     *     <li>如果都失败，返回默认值 "0.0.0.0"</li>
     * </ol>
     * <p>
     * <b>处理逻辑：</b>
     * <ul>
     *     <li>自动过滤 "unknown" 等无效值</li>
     *     <li>X-Forwarded-For 可能包含多个 IP（逗号分隔），只取第一个</li>
     *     <li>所有 IP 值都会进行 trim 处理，去除前后空格</li>
     *     <li>如果 request 为 null，直接返回 "0.0.0.0"</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在频控切面中使用
     * HttpServletRequest request = ServletUtil.getRequest();
     * String clientIp = RateLimitUtil.getClientIp(request);
     * // 结果可能是：192.168.1.100 或 0.0.0.0（无法获取时）
     * }</pre>
     * <p>
     * <b>典型场景：</b>
     * <ul>
     *     <li><b>直连场景</b>：直接访问服务器，使用 getRemoteAddr() 获取</li>
     *     <li><b>Nginx 代理</b>：Nginx 设置 X-Real-IP，优先使用此值</li>
     *     <li><b>多层代理</b>：X-Forwarded-For 包含多个 IP，取第一个（最原始客户端 IP）</li>
     *     <li><b>CDN 场景</b>：CDN 会在 X-Forwarded-For 中添加客户端 IP</li>
     * </ul>
     *
     * @param request HTTP 请求对象，可为 null（如内部调用、单元测试场景）
     * @return 客户端 IP 地址，无法解析时返回 "0.0.0.0"（避免返回 null）
     */
    public static String getClientIp(HttpServletRequest request) {
        // 1. 空值检查：如果请求对象为 null，直接返回默认值
        if (request == null) {
            return "0.0.0.0";
        }

        // 2. 优先从 X-Forwarded-For 请求头获取（支持多层代理场景）
        // X-Forwarded-For 格式：客户端IP, 代理1IP, 代理2IP, ...
        String ip = request.getHeader(RateLimitConstants.HEADER_X_FORWARDED_FOR);
        if (StringUtil.isNotEmpty(ip) && !RateLimitConstants.UNKNOWN.equalsIgnoreCase(ip)) {
            // 如果包含多个 IP（逗号分隔），只取第一个（最原始的客户端 IP）
            int idx = ip.indexOf(',');
            ip = idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }

        // 3. 如果 X-Forwarded-For 无效，尝试从 X-Real-IP 获取
        // X-Real-IP 通常由 Nginx 等反向代理设置，只有一个 IP
        if (StringUtil.isEmpty(ip) || RateLimitConstants.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader(RateLimitConstants.HEADER_X_REAL_IP);
        }

        // 4. 如果前两种方式都失败，使用 getRemoteAddr() 获取
        // 注意：在代理场景下，这可能返回代理服务器的 IP，而非真实客户端 IP
        if (StringUtil.isEmpty(ip) || RateLimitConstants.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 5. 最终检查：如果所有方式都失败，返回默认值 "0.0.0.0"
        // 返回固定值而非 null，避免后续处理时的空指针异常
        return StringUtil.isNotEmpty(ip) ? ip : "0.0.0.0";
    }
}