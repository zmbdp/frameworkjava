package com.zmbdp.common.ratelimit.executor.impl;

import com.zmbdp.common.domain.constants.RateLimitConstants;
import com.zmbdp.common.ratelimit.executor.RateLimiterExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Redis 滑动窗口限流执行器
 * <p>
 * 基于 Redis ZSET 实现滑动窗口算法，使用 Lua 脚本保证原子性。
 * <p>
 * <b>算法说明：</b>
 * <ul>
 *     <li>使用 ZSET 存储请求时间戳（score = 时间戳毫秒，member = 唯一ID）</li>
 *     <li>每次请求：ZREMRANGEBYSCORE 移除窗口外记录 → ZCARD 统计 → 未超限则 ZADD 当前请求</li>
 *     <li>窗口随请求时间滑动，避免固定窗口的边界突发问题</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = RateLimitConstants.NACOS_ALGORITHM,
        havingValue = RateLimitConstants.ALGORITHM_SLIDING_WINDOW
)
public class RedisSlidingWindowRateLimiter implements RateLimiterExecutor {

    /**
     * Lua 脚本：滑动窗口限流（ZSET），原子执行
     * <p>
     * 脚本逻辑：
     * <ol>
     *     <li>ZREMRANGEBYSCORE 移除窗口外（score &lt; now - windowMs）的成员</li>
     *     <li>ZCARD 统计窗口内请求数</li>
     *     <li>若 count &gt;= limit，返回 -1 表示拒绝，不写入</li>
     *     <li>否则 ZADD 当前请求（score=nowMs, member=唯一id），EXPIRE 设置过期</li>
     *     <li>返回当前计数（即 count + 1）</li>
     * </ol>
     * <p>
     * 返回值：&gt;=0 为通过后的当前计数，-1 表示超限拒绝
     * <p>
     * 参数：
     * <ul>
     *     <li>KEYS[1]：限流 key</li>
     *     <li>ARGV[1]：当前时间戳（毫秒）</li>
     *     <li>ARGV[2]：窗口大小（毫秒）</li>
     *     <li>ARGV[3]：限流阈值 limit</li>
     *     <li>ARGV[4]：当前请求唯一 member（UUID）</li>
     * </ul>
     */
    private static final String SLIDING_WINDOW_LUA_SCRIPT = """
            -- 参数验证和转换
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = tostring(ARGV[4])
            
            -- 参数校验：确保所有必需参数都有效
            if not now or not window or not limit or not member then
                return -2  -- -2 表示参数错误
            end
            
            -- 移除窗口外的记录（score < now - window）
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - window)
            local count = redis.call('ZCARD', KEYS[1])
            
            if count >= limit then
                return -1  -- -1 表示超限拒绝
            end
            
            -- 添加当前请求到窗口
            redis.call('ZADD', KEYS[1], now, member)
            -- 设置过期时间（窗口大小 + 1 秒，确保数据不会残留）
            redis.call('EXPIRE', KEYS[1], math.ceil(window / 1000) + 1)
            return count + 1
            """;

    /**
     * 预编译的滑动窗口 Lua 脚本
     */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(SLIDING_WINDOW_LUA_SCRIPT, Long.class);

    /**
     * Redis 模板，用于执行 Lua 脚本
     * <p>
     * 使用 StringRedisTemplate 而不是 RedisTemplate，原因：
     * <ul>
     *     <li>Lua 脚本需要接收纯字符串参数，而不是 JSON 格式</li>
     *     <li>RedisTemplate 使用 GenericJackson2JsonRedisSerializer 会序列化参数为 JSON，导致 Lua 无法正确解析</li>
     *     <li>StringRedisTemplate 使用 StringRedisSerializer，保证参数是纯字符串，Lua 的 tonumber() 可以正确解析</li>
     * </ul>
     */
    private final StringRedisTemplate stringRedisTemplate;

    public RedisSlidingWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

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
    @Override
    public boolean tryAcquire(String key, int limit, long windowMs) throws Exception {
        long nowMs = System.currentTimeMillis();
        String member = UUID.randomUUID().toString();

        // 使用 StringRedisTemplate 执行 Lua 脚本，确保参数是纯字符串，Lua 可以正确解析
        Long result = stringRedisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(nowMs),
                String.valueOf(windowMs),
                String.valueOf(limit),
                member
        );

        // 返回值处理：
        // >= 0：允许（已加入窗口，返回当前计数）
        // -1：超限拒绝
        // -2：参数错误
        // null：脚本执行异常
        if (result == null) {
            throw new RuntimeException("滑动窗口 Lua 脚本返回 null，key=" + key + ", 可能是参数传递异常或 Redis 连接问题");
        }

        if (result == -2L) {
            throw new RuntimeException("滑动窗口 Lua 脚本参数错误，key=" + key + ", nowMs=" + nowMs + ", windowMs=" + windowMs + ", limit=" + limit);
        }

        return result >= 0;
    }
}