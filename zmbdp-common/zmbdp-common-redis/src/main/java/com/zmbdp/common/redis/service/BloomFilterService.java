package com.zmbdp.common.redis.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.zmbdp.common.redis.config.BloomFilterConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 增强版布隆过滤器服务（保持原有方法签名）
 */
@Slf4j
@Service
public class BloomFilterService {

    /**
     * 精确计数器
     */
    private final AtomicLong elementCount = new AtomicLong(0);

    /**
     * 保护重置操作的锁
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 布隆过滤器配置
     */
    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    /**
     * Guava 布隆过滤器实例
     */
    private volatile BloomFilter<String> bloomFilter;

    /**
     * 初始化/重置过滤器（保持原方法名）
     */
    @PostConstruct
    public void reset() {
        refreshFilter();
    }

    /**
     * 刷新过滤器实例（线程安全）
     */
    private void refreshFilter() {
        lock.lock();
        try {
            this.bloomFilter = BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    sanitizeExpectedInsertions(bloomFilterConfig.getExpectedInsertions()),
                    sanitizeFalseProbability(bloomFilterConfig.getFalseProbability())
            );
            elementCount.set(0); // 重置计数器
            log.info("布隆过滤器重置完成 - {}", getStatus());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加元素
     */
    public void put(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("尝试添加空键到布隆过滤器");
            return;
        }
        lock.lock();
        try {
            bloomFilter.put(key);
            elementCount.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查元素是否存在
     */
    public boolean mightContain(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return bloomFilter.mightContain(key);
    }

    /**
     * 获取当前状态报告
     */
    public String getStatus() {
        return String.format(
                "BloomFilter{预期容量=%d, 当前元素≈%d(精确=%d), 负载率=%.2f%%, 误判率=%.6f}",
                bloomFilterConfig.getExpectedInsertions(),
                bloomFilter.approximateElementCount(),
                elementCount.get(),
                calculateLoadFactor(),
                bloomFilter.expectedFpp()
        );
    }

    /**
     * 计算当前负载率
     */
    public double calculateLoadFactor() {
        long expected = bloomFilterConfig.getExpectedInsertions();
        return expected > 0 ?
                (double) bloomFilter.approximateElementCount() / expected : 0;
    }

    /**
     * 安全参数处理
     */
    private int sanitizeExpectedInsertions(int expected) {
        return Math.max(1, expected); // 至少为 1
    }

    /**
     * 安全参数处理
     * @param probability 错误概率
     * @return 错误概率
     */
    private double sanitizeFalseProbability(double probability) {
        return Math.min(0.999, Math.max(0.000001, probability)); // 限制在 0.0001% ~ 99.9%
    }

    /**
     * 获取近似元素数量（新增）
     */
    public long approximateElementCount() {
        return bloomFilter.approximateElementCount();
    }

    /**
     * 获取精确元素数量（新增）
     */
    public long exactElementCount() {
        return elementCount.get();
    }
}