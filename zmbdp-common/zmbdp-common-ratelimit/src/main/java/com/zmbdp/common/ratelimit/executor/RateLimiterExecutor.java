package com.zmbdp.common.ratelimit.executor;

/**
 * 限流执行器接口
 * <p>
 * 封装限流算法的执行逻辑，支持不同的实现（滑动窗口、令牌桶等）。
 * 切面层不关心具体算法实现，只调用此接口。
 *
 * @author 稚名不带撇
 */
public interface RateLimiterExecutor {

    /**
     * 尝试获取限流许可
     * <p>
     * 检查指定 key 在时间窗口内是否超过限流阈值。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>{@code true}：允许请求（未超限）</li>
     *     <li>{@code false}：拒绝请求（已超限）</li>
     * </ul>
     * <p>
     * <b>异常说明：</b>
     * <ul>
     *     <li>如果 Redis 连接异常等系统错误，应抛出异常</li>
     *     <li>业务限流（超限）不抛异常，返回 false</li>
     * </ul>
     *
     * @param key      限流 key（如 ratelimit:ip:192.168.1.1:UserController#sendCode 或 ratelimit:identity:13800138000:UserController#sendCode）
     * @param limit    限流阈值（时间窗口内最大请求数）
     * @param windowMs 时间窗口（毫秒）
     * @return true 允许请求，false 被限流
     * @throws Exception Redis 连接异常等系统错误
     */
    boolean tryAcquire(String key, int limit, long windowMs) throws Exception;
}