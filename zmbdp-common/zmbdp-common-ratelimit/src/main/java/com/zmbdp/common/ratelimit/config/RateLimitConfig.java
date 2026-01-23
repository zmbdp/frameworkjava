package com.zmbdp.common.ratelimit.config;

/**
 * 限流配置对象
 * <p>
 * 封装限流相关的配置值，包括阈值、窗口、消息、IP 获取方式等。<br>
 * 从注解和 Nacos 配置中解析得到。
 *
 * @param keyPrefix    Redis Key 前缀，用于区分限流相关的 Key。
 * @param limit        限流阈值（时间窗口内最大请求数）
 * @param windowSec    时间窗口（秒）
 * @param windowMs     时间窗口（毫秒）
 * @param message      触发限流时的提示信息
 * @param failOpen     降级策略（true=失败放行，false=失败拒绝）
 * @param ipHeaderName IP 请求头名称
 * @param allowIpParam 是否允许从请求参数获取 IP
 * @param ipParamName  IP 请求参数名称
 * @author 稚名不带撇
 */
public record RateLimitConfig(
        String keyPrefix, // Redis Key 前缀，用于区分限流相关的 Key。
        int limit, // 限流阈值（时间窗口内最大请求数）
        long windowSec, // 时间窗口（秒）
        long windowMs, // 时间窗口（毫秒）
        String message, // 触发限流时的提示信息
        boolean failOpen, // 降级策略（true=失败放行，false=失败拒绝）
        String ipHeaderName, // IP 请求头名称
        boolean allowIpParam, // 是否允许从请求参数获取 IP
        String ipParamName // IP 请求参数名称
) {
}