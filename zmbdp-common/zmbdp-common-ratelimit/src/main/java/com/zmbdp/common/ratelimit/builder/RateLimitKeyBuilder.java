package com.zmbdp.common.ratelimit.builder;

import com.zmbdp.common.ratelimit.enums.RateLimitDimension;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 限流 Key 构建器
 * <p>
 * 根据限流维度（IP/ACCOUNT/BOTH）构建 Redis Key 列表。<br>
 * 集中管理 Key 构建规则，便于后续扩展（如 tenant、appId、灰度维度等）。
 *
 * @author 稚名不带撇
 */
@Component
public class RateLimitKeyBuilder {

    /**
     * 构建限流 Key 列表
     * <p>
     * 根据维度选择需要限流的 Key：
     * <ul>
     *     <li>IP：只限流 IP Key</li>
     *     <li>ACCOUNT：限流账号 Key（userId 或 identity，未找到时退化为 IP Key）</li>
     *     <li>BOTH：同时限流 IP 和账号（未找到账号时只限流 IP，避免重复）</li>
     * </ul>
     * <p>
     * <b>Key 格式：</b>
     * <ul>
     *     <li>IP 维度：{@code {prefix}ip:{ip}:{api}}</li>
     *     <li>账号维度：{@code {prefix}identity:{userIdentifier}:{api}}</li>
     * </ul>
     * <p>
     * <b>userIdentifier 说明：</b>
     * <ul>
     *     <li>已登录：userId（从 JWT Token 中提取）</li>
     *     <li>未登录：account（从请求参数中提取，如发送验证码、登录接口）</li>
     *     <li>未找到：退化为 IP Key</li>
     * </ul>
     *
     * @param dimension      限流维度
     * @param prefix         Redis Key 前缀（如 ratelimit:）
     * @param api            接口标识（如 UserController#sendCode）
     * @param ip             客户端 IP
     * @param userIdentifier 用户标识（userId 或 identity，可为 null）
     * @return 需要限流的 Key 列表
     */
    public List<String> buildKeys(RateLimitDimension dimension, String prefix, String api, String ip, String userIdentifier) {
        String ipKey = prefix + "ip:" + ip + ":" + api;
        String identityKey = (userIdentifier != null)
                ? (prefix + "identity:" + userIdentifier + ":" + api)
                : ipKey; // 未找到用户标识时退化为 IP Key，避免重复

        return switch (dimension) {
            case IP -> List.of(ipKey);
            case ACCOUNT -> List.of(identityKey);
            case BOTH -> {
                List<String> keys = new ArrayList<>();
                keys.add(ipKey);
                // 如果找到用户标识且身份 Key 与 IP Key 不同，则同时限流身份维度
                // 如果未找到用户标识，identityKey == ipKey，避免重复限流
                if (userIdentifier != null && !identityKey.equals(ipKey)) {
                    keys.add(identityKey);
                }
                yield keys;
            }
        };
    }
}