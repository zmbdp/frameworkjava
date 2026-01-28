package com.zmbdp.common.ratelimit.executor;

/**
 * 限流执行器接口
 * <p>
 * 封装限流算法的执行逻辑，支持不同的实现（令牌桶、滑动窗口等）。
 * 切面层不关心具体算法实现，只调用此接口。
 * <p>
 * <b>实现类：</b>
 * <ul>
 *     <li>{@link com.zmbdp.common.ratelimit.executor.impl.RedisTokenBucketRateLimiter}：令牌桶算法（默认）</li>
 *     <li>{@link com.zmbdp.common.ratelimit.executor.impl.RedisSlidingWindowRateLimiter}：滑动窗口算法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
public interface RateLimiterExecutor {

    /**
     * 尝试获取限流许可
     * <p>
     * 检查指定 key 是否允许请求（根据具体算法实现：令牌桶检查是否有可用令牌，滑动窗口检查是否超过时间窗口内最大请求数）。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>{@code true}：允许请求（令牌桶：有可用令牌；滑动窗口：未超过时间窗口内最大请求数）</li>
     *     <li>{@code false}：拒绝请求（令牌桶：无可用令牌；滑动窗口：超过时间窗口内最大请求数）</li>
     * </ul>
     * <p>
     * <b>异常说明：</b>
     * <ul>
     *     <li>如果 Redis 连接异常等系统错误，应抛出异常</li>
     *     <li>业务限流（超限）不抛异常，返回 false</li>
     * </ul>
     *
     * @param key      限流 key（如 ratelimit:ip:192.168.1.1:UserController#sendCode 或 ratelimit:identity:13800138000:UserController#sendCode）
     * @param limit    限流阈值（令牌桶：桶容量/最大令牌数；滑动窗口：时间窗口内最大请求数）
     * @param windowMs 时间窗口（毫秒），令牌桶用于计算补充速率（refillRate = limit / (windowMs / 1000)），滑动窗口用于统计时间范围
     * @return true 允许请求，false 被限流
     * @throws Exception Redis 连接异常等系统错误
     */
    boolean tryAcquire(String key, int limit, long windowMs) throws Exception;
}