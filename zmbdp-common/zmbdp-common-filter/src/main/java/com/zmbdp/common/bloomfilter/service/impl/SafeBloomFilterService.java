package com.zmbdp.common.bloomfilter.service.impl;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.zmbdp.common.bloomfilter.config.BloomFilterConfig;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 布隆过滤器服务（线程安全版本）
 *
 * @author 稚名不带撇
 */
@Slf4j
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "safe", matchIfMissing = true)
public class SafeBloomFilterService implements BloomFilterService {

    /**
     * 精确计数器
     */
    private final AtomicLong elementCount = new AtomicLong(0);

    /**
     * 存储实际元素的集合（用于扩容时数据迁移）
     */
    private final Set<String> actualElements = new HashSet<>();

    /**
     * 读写锁
     */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * 布隆过滤器配置
     */
    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    /**
     * 布隆过滤器实例
     */
    private BloomFilter<String> bloomFilter;

    /**
     * 初始化/重置过滤器
     */
    @Override
    @PostConstruct
    public void reset() {
        refreshFilter();
    }

    /**
     * 刷新过滤器实例（线程安全）
     */
    private void refreshFilter() {
        rwLock.writeLock().lock();
        try {
            this.bloomFilter = BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    sanitizeExpectedInsertions(bloomFilterConfig.getExpectedInsertions()),
                    sanitizeFalseProbability(bloomFilterConfig.getFalseProbability())
            );
            elementCount.set(0); // 重置元素计数器
            actualElements.clear(); // 清空实际元素集合
            log.info("布隆过滤器重置完成 - {}", getStatus());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 添加元素（线程安全）
     *
     * @param key 键
     */
    @Override
    public void put(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("尝试添加空键到布隆过滤器");
            return;
        }

        rwLock.writeLock().lock();
        try {
            // 完全依赖 Set 来判断是否是新元素
            boolean isNewElement = actualElements.add(key);

            if (isNewElement) {
                // 只有新元素才添加到布隆过滤器和计数器
                bloomFilter.put(key);
                long count = elementCount.incrementAndGet();
                checkAndWarnLoadFactor(count);
            } else {
                log.debug("键已存在: {}", key);
            }

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 检查元素是否存在（线程安全）
     *
     * @param key 键
     * @return 是否存在; 存在返回 true; 不存在则返回 false
     */
    @Override
    public boolean mightContain(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }

        rwLock.readLock().lock();
        try {
            return bloomFilter.mightContain(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 手动扩容布隆过滤器（线程安全）
     */
    @Override
    public void expand() {
        rwLock.writeLock().lock();
        try {
            log.info("开始扩容布隆过滤器，当前状态: {}", getStatus());

            BloomFilter<String> newBloomFilter = BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    sanitizeExpectedInsertions(bloomFilterConfig.getExpectedInsertions()),
                    sanitizeFalseProbability(bloomFilterConfig.getFalseProbability())
            );

            int migratedCount = 0;
            for (String element : actualElements) {
                newBloomFilter.put(element);
                migratedCount++;
            }

            this.bloomFilter = newBloomFilter;

            log.info("布隆过滤器扩容完成 - 迁移元素数量: {}, 新状态: {}", migratedCount, getStatus());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 获取当前状态报告（线程安全）
     *
     * @return 状态报告
     */
    @Override
    public String getStatus() {
        rwLock.readLock().lock();
        try {
            return String.format(
                    "BloomFilter{预期容量=%d, 当前元素≈%d(精确=%d), 负载率=%.2f%%, 误判率=%.6f, 存储元素=%d}",
                    bloomFilterConfig.getExpectedInsertions(),
                    bloomFilter.approximateElementCount(),
                    elementCount.get(),
                    calculateLoadFactor() * 100,
                    bloomFilter.expectedFpp(),
                    actualElements.size()
            );
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 计算当前负载率（线程安全）
     *
     * @return 负载率
     */
    @Override
    public double calculateLoadFactor() {
        rwLock.readLock().lock();
        try {
            long expected = bloomFilterConfig.getExpectedInsertions();
            return expected > 0 ? (double) elementCount.get() / expected : 0;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 获取近似元素数量（线程安全）
     *
     * @return 近似元素数量
     */
    @Override
    public long approximateElementCount() {
        rwLock.readLock().lock();
        try {
            return bloomFilter.approximateElementCount();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 获取精确元素数量（线程安全）
     *
     * @return 精确元素数量
     */
    @Override
    public long exactElementCount() {
        rwLock.readLock().lock();
        try {
            return elementCount.get();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 获取实际存储的元素数量（线程安全）
     *
     * @return 实际存储的元素数量
     */
    @Override
    public int actualElementCount() {
        rwLock.readLock().lock();
        try {
            return actualElements.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 检查负载率超过阈值时打印警告警告
     *
     * @param currentCount 当前元素数量
     */
    private void checkAndWarnLoadFactor(long currentCount) {
        long expected = bloomFilterConfig.getExpectedInsertions();
        if (expected > 0) {
            double loadFactor = (double) currentCount / expected;
            if (loadFactor >= bloomFilterConfig.getWarningThreshold()) {
                log.warn("布隆过滤器负载率已达到 {}%，建议扩容或重置布隆过滤器", String.format("%.2f", loadFactor * 100));
            }
        }
    }

    /**
     * 确保预期插入数量至少为 100
     *
     * @param expected 预期插入数量
     * @return 新的预期插入数量
     */
    private int sanitizeExpectedInsertions(int expected) {
        return Math.max(100, expected);
    }

    /**
     * 确保误判概率在 0.000001 到 0.999 之间
     *
     * @param probability 误判概率
     * @return 误判概率，如果输入的概率大于 0.999，则返回 0.999；小于 0.000001，则返回 0.000001
     */
    private double sanitizeFalseProbability(double probability) {
        return Math.min(0.999, Math.max(0.000001, probability));
    }
}