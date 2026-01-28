package com.zmbdp.common.ratelimit.config;

/**
 * 限流配置对象
 * <p>
 * 封装限流相关的配置值，包括阈值、窗口、消息、IP 获取方式等。<br>
 * 从注解和 Nacos 配置中解析得到。
 * <p>
 * <b>参数说明（根据算法不同，含义不同）：</b>
 * <ul>
 *     <li><b>limit</b>：
 *         <ul>
 *             <li>令牌桶算法：桶容量（最大令牌数），允许的突发流量上限</li>
 *             <li>滑动窗口算法：时间窗口内最大请求数</li>
 *         </ul>
 *     </li>
 *     <li><b>windowSec / windowMs</b>：
 *         <ul>
 *             <li>令牌桶算法：用于计算令牌补充速率（refillRate = limit / windowSec）</li>
 *             <li>滑动窗口算法：滑动窗口的时间范围，限流统计基于此时间窗口内的请求数</li>
 *         </ul>
 *     </li>
 *     <li><b>其他参数</b>：两种算法通用</li>
 * </ul>
 *
 * @param keyPrefix    Redis Key 前缀，用于区分限流相关的 Key（两种算法通用）
 * @param limit        限流阈值（令牌桶：桶容量/最大令牌数；滑动窗口：时间窗口内最大请求数）
 * @param windowSec    时间窗口（秒）（令牌桶：用于计算补充速率；滑动窗口：滑动窗口的时间范围）
 * @param windowMs     时间窗口（毫秒）（令牌桶：用于计算补充速率；滑动窗口：滑动窗口的时间范围）
 * @param message      触发限流时的提示信息（两种算法通用）
 * @param failOpen     降级策略（true=失败放行，false=失败拒绝）（两种算法通用）
 * @param ipHeaderName IP 请求头名称（两种算法通用）
 * @param allowIpParam 是否允许从请求参数获取 IP（两种算法通用）
 * @param ipParamName   IP 请求参数名称（两种算法通用）
 * @author 稚名不带撇
 */
public record RateLimitConfig(
        String keyPrefix, // Redis Key 前缀，用于区分限流相关的 Key（两种算法通用）
        int limit, // 限流阈值（令牌桶：桶容量/最大令牌数；滑动窗口：时间窗口内最大请求数）
        long windowSec, // 时间窗口（秒）（令牌桶：用于计算补充速率；滑动窗口：滑动窗口的时间范围）
        long windowMs, // 时间窗口（毫秒）（令牌桶：用于计算补充速率；滑动窗口：滑动窗口的时间范围）
        String message, // 触发限流时的提示信息（两种算法通用）
        boolean failOpen, // 降级策略（true=失败放行，false=失败拒绝）（两种算法通用）
        String ipHeaderName, // IP 请求头名称（两种算法通用）
        boolean allowIpParam, // 是否允许从请求参数获取 IP（两种算法通用）
        String ipParamName // IP 请求参数名称（两种算法通用）
) {
}