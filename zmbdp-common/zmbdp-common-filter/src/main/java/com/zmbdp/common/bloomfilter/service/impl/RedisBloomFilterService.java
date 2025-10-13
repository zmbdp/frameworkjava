package com.zmbdp.common.bloomfilter.service.impl;

import com.zmbdp.common.bloomfilter.config.BloomFilterConfig;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.redis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "redis")
public class RedisBloomFilterService implements BloomFilterService {

    /**
     * 布隆过滤器名称
     */
    private static final String BLOOM_NAME = "frameworkjava:bloom";

    /**
     * 存放元素数量
     */
    private final AtomicLong elementCount = new AtomicLong(0);

    /**
     * RedissonClient
     */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 布隆过滤器配置
     */
    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    /**
     * RedissonLockService
     */
    @Autowired
    private RedissonLockService redissonLockService;

    /**
     * 初始化 MBbloom 类型布隆过滤器（分布式锁保证多实例安全）
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

            if (falseProbability <= 0 || falseProbability >= 1) {
                throw new IllegalArgumentException("误判率必须在 0-1 之间，当前值: " + falseProbability);
            }

            if (expectedInsertions <= 0) {
                throw new IllegalArgumentException("预期插入数量必须为正整数，当前值: " + expectedInsertions);
            }

            // 使用 Lua 脚本调用 BF.RESERVE 初始化布隆过滤器
            String lua = String.format(
                    "return redis.call('BF.RESERVE', '%s', %f, %d)",
                    BLOOM_NAME, falseProbability, expectedInsertions
            );

            redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    lua,
                    RScript.ReturnType.BOOLEAN
            );

            log.info("RedisBloom 初始化完成: {}", BLOOM_NAME);

        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 重置布隆过滤器（删除原 key 并重新初始化 MBbloom）
     */
    @Override
    public void reset() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:reset");
        if (lock == null) {
            log.warn("获取 reset 锁失败");
            return;
        }

        try {
            redissonClient.getKeys().delete(BLOOM_NAME);
            elementCount.set(0);
            initBloomFilter();
            log.info("RedisBloom 已重置: {}", BLOOM_NAME);
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 单条添加元素（MBbloom 类型）
     *
     * @param key 待添加的元素
     */
    @Override
    public void put(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        // 移除分布式锁，直接调用 Redis 命令
        try {
            String lua = "return redis.call('BF.ADD', KEYS[1], ARGV[1])";

            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    lua,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(BLOOM_NAME),
                    key
            );

            if ((result instanceof Integer && ((Integer) result) == 1) ||
                    (result instanceof Long && ((Long) result) == 1L)) {
                elementCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("添加元素失败: {}", key, e);
        }
    }


    /**
     * 批量添加元素（Lua + MBbloom 类型）
     *
     * @param keys 待添加的元素列表
     */
    @Override
    public void putAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        RLock lock = redissonLockService.acquire("lock:redis:bloom:putAll");
        if (lock == null) {
            log.warn("获取批量 put 锁失败");
            return;
        }

        try {
            // 使用 BF.MADD 批量添加
            String lua = "return redis.call('BF.MADD', KEYS[1], unpack(ARGV))";

            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    lua,
                    RScript.ReturnType.MULTI,
                    Collections.singletonList(BLOOM_NAME),
                    keys.toArray(new String[0])
            );

            // 统计新增的元素数量
            if (result instanceof Collection<?> results) {
                long addedCount = results.stream()
                        .filter(r -> (r instanceof Long l && l == 1) || (r instanceof Integer i && i == 1))
                        .count();

                elementCount.addAndGet(addedCount);
            }

            log.info("批量添加 {} 个元素完成", keys.size());
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 单条判断元素是否存在（MBbloom 类型）
     *
     * @param key 待判断的元素
     * @return true 存在，false 不存在
     */
    @Override
    public boolean mightContain(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        // 移除分布式锁，直接调用 Redis 命令
        try {
            String lua = "return redis.call('BF.EXISTS', KEYS[1], ARGV[1])";

            Object result = redissonClient.getScript().eval(
                    RScript.Mode.READ_ONLY,
                    lua,
                    RScript.ReturnType.BOOLEAN,
                    Collections.singletonList(BLOOM_NAME),
                    key
            );

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("查询元素失败: {}", key, e);
            return false;
        }
    }


    /**
     * 批量判断集合中是否有任意元素存在（MBbloom 类型）
     *
     * @param keys 待判断的元素集合
     * @return true 至少有一个存在，false 一个都不存在
     */
    @Override
    public boolean mightContainAny(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }

        String lua =
                "for i=1,#ARGV do " +
                        " if redis.call('BF.EXISTS', KEYS[1], ARGV[i]) == 1 then return 1 end " +
                        "end " +
                        "return 0";

        Object result = redissonClient.getScript().eval(
                RScript.Mode.READ_ONLY,
                lua,
                RScript.ReturnType.BOOLEAN,
                Collections.singletonList(BLOOM_NAME),
                keys.toArray(new String[0])
        );

        return Boolean.TRUE.equals(result);
    }

    /**
     * 清空布隆过滤器
     */
    @Override
    public void clear() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:clear");
        if (lock == null) {
            return;
        }

        try {
            redissonClient.getKeys().delete(BLOOM_NAME);
            elementCount.set(0);
            initBloomFilter();
            log.info("RedisBloom 已清空");
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    @Override
    public void expand() {
        log.info("RedisBloom 不需要手动扩容");
    }

    @Override
    public String getStatus() {
        return String.format(
                "RedisBloomFilter{name=%s, 本地计数=%d, 预期误判率=%.2f%%}",
                BLOOM_NAME,
                elementCount.get(),
                bloomFilterConfig.getFalseProbability() * 100
        );
    }

    @Override
    public double calculateLoadFactor() {
        return -1;
    }

    @Override
    public long approximateElementCount() {
        return -1;
    }

    @Override
    public long exactElementCount() {
        return elementCount.get();
    }

    @Override
    public int actualElementCount() {
        return (int) elementCount.get();
    }
}