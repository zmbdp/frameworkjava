package com.zmbdp.common.bloomfilter.service.impl;

import com.zmbdp.common.bloomfilter.config.BloomFilterConfig;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.redis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Redis 实现的分布式布隆过滤器服务<p>
 * 特性：<p>
 * 1. 单条/批量添加元素<p>
 * 2. 单条/批量查询元素<p>
 * 3. 元素计数存 Redis，多实例安全<p>
 * 4. 异步更新 Redis 元素计数，减少主线程阻塞<p>
 * 5. 分布式锁保证初始化、重置、清空安全<p>
 * 6. 自动扩容阈值提示异步执行
 *
 * @author 稚名不带撇
 */
@Slf4j
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "redis")
public class RedisBloomFilterService implements BloomFilterService {

    /**
     * Redis 布隆过滤器 key
     */
    private static final String BLOOM_NAME = "frameworkjava:bloom";

    /**
     * Redis 元素计数 key
     */
    private static final String BLOOM_COUNT_KEY = BLOOM_NAME + ":count";

    /**
     * Redisson 客户端
     */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 布隆过滤器配置
     */
    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    /**
     * Redisson 分布式锁服务
     */
    @Autowired
    private RedissonLockService redissonLockService;

    /**
     * 注入线程池，用于执行异步更新元素计数，避免阻塞主线程
     */
    @Autowired
    private Executor threadPoolTaskExecutor;

    /**
     * 初始化布隆过滤器（MBbloom 类型）<p>
     * 通过分布式锁保证多实例同时启动时不会重复创建<p>
     * 创建成功后，将布隆过滤器信息写入 Redis
     */
    private void initBloomFilter() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:init");
        if (lock == null) {
            log.warn("获取布隆初始化锁失败");
            return;
        }
        try {
            double falseProbability = bloomFilterConfig.getFalseProbability();
            int expectedInsertions = bloomFilterConfig.getExpectedInsertions();

            // 校验参数合法性
            if (falseProbability <= 0 || falseProbability >= 1) {
                throw new IllegalArgumentException("误判率必须在 0-1 之间，当前值: " + falseProbability);
            }
            if (expectedInsertions <= 0) {
                throw new IllegalArgumentException("预期插入数量必须为正整数，当前值: " + expectedInsertions);
            }

            // Lua 脚本：如果 key 不存在，则创建布隆过滤器
            String lua = """
                    -- 如果 Bloom Filter 不存在则创建
                    if redis.call('EXISTS', KEYS[1]) == 0 then
                        return redis.call('BF.RESERVE', KEYS[1], %f, %d)
                    else
                        return 'EXISTS'
                    end
                    """
                    .formatted(falseProbability, expectedInsertions);

            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    lua,
                    RScript.ReturnType.VALUE,
                    List.of(BLOOM_NAME)
            );
            log.info("RedisBloom 初始化完成: {}, result = {}", BLOOM_NAME, result);

        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 异步增加 Redis 元素计数，并可选打印自动扩容提示
     *
     * @param delta 增加的元素数量
     */
    private void incrementBloomCountAsync(long delta) {
        threadPoolTaskExecutor.execute(() -> {
            try {
                RAtomicLong counter = redissonClient.getAtomicLong(BLOOM_COUNT_KEY);
                long total = counter.addAndGet(delta); // 异步增加计数
                log.trace("[RedisBloom] 异步增加计数: {}", delta);

                if (bloomFilterConfig.isCheckWarning()) {
                    double threshold = bloomFilterConfig.getExpectedInsertions() * bloomFilterConfig.getWarningThreshold();
                    if (total >= threshold) {
                        log.warn("[RedisBloom] 元素总数 {} 已超过阈值 {}，RediBloom 可能自动扩容", total, (int) threshold);
                        // 调用扩容方法
                        expand();
                    }
                }
            } catch (Exception e) {
                log.error("[RedisBloom] 异步增加元素计数失败", e);
            }
        });
    }

    /**
     * 单条添加元素<p>
     * 如果新增成功，则异步增加元素计数，并异步提示自动扩容
     *
     * @param key 待添加元素
     */
    @Override
    public void put(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        try {
            // Lua 调用 RedisBloom BF.ADD
            String lua = "return redis.call('BF.ADD', KEYS[1], ARGV[1])";
            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    lua,
                    RScript.ReturnType.INTEGER,
                    List.of(BLOOM_NAME),
                    key
            );

            boolean added = (result instanceof Integer i && i == 1) || (result instanceof Long l && l == 1L);
            if (added) {
                incrementBloomCountAsync(1); // 异步增加计数 + 自动扩容提示
                log.trace("[RedisBloom] 新增元素: {}", key);
            } else {
                log.debug("[RedisBloom] 元素已存在: {}", key);
            }

        } catch (Exception e) {
            log.error("[RedisBloom] 添加元素失败: {}", key, e);
        }
    }

    /**
     * 批量添加元素<p>
     * 使用 Lua BF.MADD 返回每个元素是否新增
     *
     * @param keys 待添加元素集合
     */
    @Override
    public void putAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        int batchSize = bloomFilterConfig.getBatchSize();
        List<String> keyList = new ArrayList<>(keys);

        for (int i = 0; i < keyList.size(); i += batchSize) {
            List<String> batch = keyList.subList(i, Math.min(i + batchSize, keyList.size()));
            try {
                // Lua 脚本：统计新增元素数量
                String lua = "return redis.call('BF.MADD', KEYS[1], unpack(ARGV))";
                Object result = redissonClient.getScript().eval(
                        RScript.Mode.READ_WRITE,
                        lua,
                        RScript.ReturnType.MULTI,
                        List.of(BLOOM_NAME),
                        batch.toArray(new String[0])
                );

                long addedCount = result instanceof Number n ? n.longValue() : 0;
                if (addedCount > 0) {
                    incrementBloomCountAsync(addedCount);
                    log.info("[RedisBloom] 批量新增 {} / {} 个元素", addedCount, batch.size());
                }

            } catch (Exception e) {
                log.error("[RedisBloom] 批量添加元素失败", e);
            }
        }
    }

    /**
     * 查询元素是否存在
     *
     * @param key 待查询元素
     * @return true 存在，false 不存在
     */
    @Override
    public boolean mightContain(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        log.trace("[RedisBloom] 查询元素: {}", key);
        try {
            String lua = "return redis.call('BF.EXISTS', KEYS[1], ARGV[1])";
            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    lua,
                    RScript.ReturnType.BOOLEAN,
                    List.of(BLOOM_NAME),
                    key
            );
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[RedisBloom] 查询元素失败: {}", key, e);
            return false;
        }
    }

    /**
     * 批量查询集合中是否有任意元素存在
     *
     * @param keys 待查询元素集合
     * @return true 至少有一个存在，false 一个都不存在
     */
    @Override
    public boolean mightContainAny(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        String lua = """
                -- 检查一组元素中是否至少有一个存在
                for i = 1, #ARGV do
                    if redis.call('BF.EXISTS', KEYS[1], ARGV[i]) == 1 then
                        return 1
                    end
                end
                return 0
                """;

        try {
            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    lua,
                    RScript.ReturnType.BOOLEAN,
                    List.of(BLOOM_NAME),
                    keys.toArray(new String[0])
            );
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[RedisBloom] 批量查询元素失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 清空布隆过滤器和计数（配置不变）<p>
     * 使用分布式锁保证多实例安全
     */
    @Override
    public void clear() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:clear");
        if (lock == null) {
            return;
        }
        try {
            redissonClient.getKeys().delete(BLOOM_NAME, BLOOM_COUNT_KEY);
            initBloomFilter();
            log.info("[RedisBloom] 已清空");
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 重置布隆过滤器（删除原有并重新初始化）<p>
     * 使用分布式锁保证多实例安全
     */
    @Override
    public void reset() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:reset");
        if (lock == null) {
            log.warn("获取 reset 锁失败");
            return;
        }
        try {
            redissonClient.getKeys().delete(BLOOM_NAME, BLOOM_COUNT_KEY);
            initBloomFilter();
            log.info("[RedisBloom] 已重置");
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * MBbloom 类型无需手动扩容
     */
    @Override
    public void expand() {
        log.info("[RedisBloom] 不需要手动扩容");
    }

    /**
     * 获取布隆过滤器状态信息
     */
    @Override
    public String getStatus() {
        long count = exactElementCount();
        return String.format(
                "RedisBloomFilter{name = %s, 元素计数 = %d, 预期误判率 = %.2f%%}",
                BLOOM_NAME,
                count,
                bloomFilterConfig.getFalseProbability() * 100
        );
    }

    /**
     * 获取负载因子<p>
     * 负载因子 = 已插入元素数 / 布隆过滤器位数组总长度<p>
     * 近似计算，基于布隆过滤器初始化参数
     */
    @Override
    public double calculateLoadFactor() {
        try {
            long inserted = exactElementCount();
            if (inserted <= 0) {
                return 0;
            }

            int n = bloomFilterConfig.getExpectedInsertions();
            double p = bloomFilterConfig.getFalseProbability();

            if (n <= 0 || p <= 0 || p >= 1) {
                return -1;
            }

            // 负载因子 = 已插入元素数 / 预期插入元素数
            return (double) inserted / n;

        } catch (Exception e) {
            log.error("[RedisBloom] 计算负载因子失败", e);
            return -1;
        }
    }

    /**
     * 获取近似元素数量
     */
    @Override
    public long approximateElementCount() {
        return exactElementCount();
    }

    /**
     * 获取精确元素数量（从 Redis 计数器获取）
     */
    @Override
    public long exactElementCount() {
        try {
            RAtomicLong counter = redissonClient.getAtomicLong(BLOOM_COUNT_KEY);
            return counter.get();
        } catch (Exception e) {
            log.error("[RedisBloom] 获取元素计数失败", e);
            return -1;
        }
    }

    /**
     * 获取实际元素数量（从 Redis 计数器获取）
     */
    @Override
    public int actualElementCount() {
        long count = exactElementCount();
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    /**
     * 删除布隆过滤器（不重新创建）
     *
     * @return true 删除成功，false 删除失败
     */
    @Override
    public boolean delete() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:delete");
        if (lock == null) {
            return false;
        }
        try {
            // 删除布隆过滤器和计数器
            long result = redissonClient.getKeys().delete(BLOOM_NAME, BLOOM_COUNT_KEY);

            // 删除配置信息
            redissonClient.getKeys().delete("{frameworkjava:bloom}:config");

            log.info("[RedisBloom] 已删除");
            return result > 0;
        } catch (Exception e) {
            log.error("[RedisBloom] 删除失败", e);
            return false;
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }
}