package com.zmbdp.common.ratelimit.enums;

/**
 * 频控维度
 * <p>
 * 支持 IP、账号（userId）及双维度限流。
 *
 * @author 稚名不带撇
 */
public enum RateLimitDimension {

    /**
     * 仅按 IP 限流
     */
    IP,

    /**
     * 仅按账号（userId，来自网关请求头 userId）限流；未登录时退化为 IP
     */
    ACCOUNT,

    /**
     * IP + 账号双维度限流：两个维度均需满足限制，任一超限即拒绝
     */
    BOTH
}