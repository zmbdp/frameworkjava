package com.zmbdp.common.ratelimit.executor.impl;

import com.zmbdp.common.domain.constants.RateLimitConstants;
import com.zmbdp.common.ratelimit.executor.RateLimiterExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Redis 令牌桶限流执行器
 * <p>
 * 基于 Redis Hash + Lua 脚本实现令牌桶算法，保证原子性。
 * <p>
 * <b>算法说明：</b>
 * <ul>
 *     <li>使用 Redis Hash 存储桶状态：{tokens: 当前令牌数, lastRefillTime: 上次补充时间戳}</li>
 *     <li>每次请求：计算需要补充的令牌数 → 更新令牌数（不超过容量）→ 消耗一个令牌</li>
 *     <li>令牌以固定速率（refillRate = limit / windowSec）持续补充</li>
 *     <li>允许突发流量（桶满时），但长期平均速率受限制</li>
 * </ul>
 * <p>
 * <b>令牌桶 vs 滑动窗口：</b>
 * <ul>
 *     <li>令牌桶：允许突发流量，适合需要平滑限流的场景</li>
 *     <li>滑动窗口：严格限制时间窗口内的请求数，适合需要精确控制的场景</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = RateLimitConstants.NACOS_ALGORITHM,
        havingValue = RateLimitConstants.ALGORITHM_TOKEN_BUCKET,
        matchIfMissing = true // 默认使用令牌桶算法
)
public class RedisTokenBucketRateLimiter implements RateLimiterExecutor {

    /**
     * Lua 脚本：令牌桶限流（Hash），原子执行
     * <p>
     * 脚本逻辑：
     * <ol>
     *     <li>获取当前时间 now（毫秒）</li>
     *     <li>获取桶状态（tokens, lastRefillTime）</li>
     *     <li>计算时间差（now - lastRefillTime），转换为秒</li>
     *     <li>计算需要补充的令牌数：时间差 * refillRate</li>
     *     <li>更新 tokens = min(capacity, tokens + 补充的令牌数）</li>
     *     <li>更新 lastRefillTime = now</li>
     *     <li>如果 tokens >= 1，则 tokens--，返回 1（允许）</li>
     *     <li>否则返回 0（拒绝）</li>
     * </ol>
     * <p>
     * 返回值：1 表示允许，0 表示拒绝
     * <p>
     * 参数：
     * <ul>
     *     <li>KEYS[1]：限流 key</li>
     *     <li>ARGV[1]：当前时间戳（毫秒）</li>
     *     <li>ARGV[2]：桶容量（capacity，最大令牌数）</li>
     *     <li>ARGV[3]：每秒补充速率（refillRate，令牌/秒）</li>
     * </ul>
     */
    private static final String TOKEN_BUCKET_LUA_SCRIPT = """
            -- 参数验证和转换
            local now = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local refillRate = tonumber(ARGV[3])
            
            -- 参数校验：确保所有必需参数都有效
            if not now or not capacity or not refillRate then
                return -2  -- -2 表示参数错误
            end
            
            -- 获取桶状态
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens')) or capacity
            local lastRefillTime = tonumber(redis.call('HGET', KEYS[1], 'lastRefillTime')) or now
            
            -- 计算需要补充的令牌数
            local elapsed = (now - lastRefillTime) / 1000.0  -- 转换为秒
            local tokensToAdd = elapsed * refillRate
            
            -- 更新令牌数（不超过容量）
            tokens = math.min(capacity, tokens + tokensToAdd)
            
            -- 更新桶状态
            redis.call('HSET', KEYS[1], 'tokens', tokens)
            redis.call('HSET', KEYS[1], 'lastRefillTime', now)
            
            -- 设置过期时间（避免数据残留，设置为窗口大小的 2 倍）
            local expireTime = math.ceil(capacity / refillRate * 2)
            redis.call('EXPIRE', KEYS[1], expireTime)
            
            -- 尝试消耗一个令牌
            if tokens >= 1 then
                tokens = tokens - 1
                redis.call('HSET', KEYS[1], 'tokens', tokens)
                return 1  -- 允许
            else
                return 0  -- 拒绝
            end
            """;

    /**
     * 预编译的令牌桶 Lua 脚本
     */
    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(TOKEN_BUCKET_LUA_SCRIPT, Long.class);

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

    public RedisTokenBucketRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取限流许可
     * <p>
     * 检查指定 key 的令牌桶是否有可用令牌。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>{@code true}：允许请求（有可用令牌）</li>
     *     <li>{@code false}：拒绝请求（无可用令牌）</li>
     * </ul>
     * <p>
     * <b>异常说明：</b>
     * <ul>
     *     <li>如果 Redis 连接异常等系统错误，应抛出异常</li>
     *     <li>业务限流（无令牌）不抛异常，返回 false</li>
     * </ul>
     *
     * @param key      限流 key（如 ratelimit:ip:192.168.1.1:UserController#sendCode 或 ratelimit:identity:13800138000:UserController#sendCode）
     * @param limit    桶容量（最大令牌数）
     * @param windowMs 时间窗口（毫秒），用于计算补充速率（refillRate = limit / (windowMs / 1000)）
     * @return true 允许请求，false 被限流
     * @throws Exception Redis 连接异常等系统错误
     */
    @Override
    public boolean tryAcquire(String key, int limit, long windowMs) throws Exception {
        long nowMs = System.currentTimeMillis();

        // 计算每秒补充速率：refillRate = limit / windowSec
        double windowSec = windowMs / 1000.0;
        double refillRate = limit / windowSec;

        // 使用 StringRedisTemplate 执行 Lua 脚本，确保参数是纯字符串，Lua 可以正确解析
        Long result = stringRedisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(nowMs),
                String.valueOf(limit),
                String.valueOf(refillRate)
        );

        // 返回值处理：
        // 1：允许（有令牌）
        // 0：拒绝（无令牌）
        // -2：参数错误
        // null：脚本执行异常（Redis 连接问题等）
        // 注意：虽然 IDE 可能提示 Dead code，但 execute() 方法在 Redis 连接异常时可能返回 null，此检查是必要的
        if (result == null) {
            throw new RuntimeException("令牌桶 Lua 脚本返回 null，key=" + key + ", 可能是参数传递异常或 Redis 连接问题");
        }
        if (result == -2L) {
            throw new RuntimeException("令牌桶 Lua 脚本参数错误，key=" + key + ", nowMs=" + nowMs + ", capacity=" + limit + ", refillRate=" + refillRate);
        }

        // 查询当前剩余令牌数（用于日志记录）
        Object tokensObj = stringRedisTemplate.opsForHash().get(key, "tokens");
        int remainingTokens = 0;
        if (tokensObj != null) {
            try {
                String tokensStr = tokensObj.toString();
                remainingTokens = Integer.parseInt(tokensStr);
            } catch (NumberFormatException e) {
                log.warn("令牌桶剩余令牌数解析失败：key={}, tokens={}", key, tokensObj);
            }
        }

        // 记录令牌桶状态日志
        if (result == 1L) {
            log.info("令牌桶限流通过：key = {}, 桶总容量 = {}, 剩余令牌 = {}", key, limit, remainingTokens);
        } else {
            log.info("令牌桶限流拒绝：key = {}, 桶总容量 = {}, 剩余令牌 = {}（无可用令牌）", key, limit, remainingTokens);
        }

        return result == 1L;
    }
}