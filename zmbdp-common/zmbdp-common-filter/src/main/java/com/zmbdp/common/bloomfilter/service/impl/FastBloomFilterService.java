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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 布隆过滤器服务（不加锁版本）
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "fast") // 只有读取到这个配置时，才会初始化
public class FastBloomFilterService implements BloomFilterService {

    /**
     * 精确计数器
     */
    private final AtomicLong elementCount = new AtomicLong(0);

    /**
     * 存储实际元素的集合（用于扩容时的数据迁移）
     */
    private final Set<String> actualElements = new HashSet<>();

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
     * 初始化/重置过滤器
     */
    @Override
    @PostConstruct
    public void reset() {
        refreshFilter();
    }

    /**
     * 刷新过滤器实例
     */
    private void refreshFilter() {
        this.bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                sanitizeExpectedInsertions(bloomFilterConfig.getExpectedInsertions()),
                sanitizeFalseProbability(bloomFilterConfig.getFalseProbability())
        );
        elementCount.set(0); // 重置计数器
        actualElements.clear(); // 清空实际元素集合
        log.info("布隆过滤器重置完成 - {}", getStatus());
    }

    /**
     * 手动扩容布隆过滤器<p>
     * 用户可以在 Nacos 上修改配置后调用此方法进行扩容
     */
    @Override
    public void expand() {
        try {
            log.info("开始扩容布隆过滤器，当前状态: {}", getStatus());

            // 创建新的布隆过滤器
            BloomFilter<String> newBloomFilter = BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    sanitizeExpectedInsertions(bloomFilterConfig.getExpectedInsertions()),
                    sanitizeFalseProbability(bloomFilterConfig.getFalseProbability())
            );

            // 将原有元素重新添加到新的布隆过滤器中
            int migratedCount = 0;
            for (String element : actualElements) {
                newBloomFilter.put(element);
                migratedCount++;
            }

            // 替换旧的布隆过滤器
            this.bloomFilter = newBloomFilter;

            log.info("布隆过滤器扩容完成 - 迁移元素数量: {}, 新状态: {}", migratedCount, getStatus());
        } catch (Exception e) {
            log.error("布隆过滤器扩容失败", e);
            throw new RuntimeException("布隆过滤器扩容失败", e);
        }
    }

    /**
     * 添加元素
     *
     * @param key 键
     */
    @Override
    public void put(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("尝试添加空键到布隆过滤器");
            return;
        }

        bloomFilter.put(key);
        long count = elementCount.incrementAndGet();

        // 将元素添加到实际元素集合中（用于可能的扩容）
        actualElements.add(key);

        // 检查负载率并打印警告日志
        checkAndWarnLoadFactor(count);
    }

    /**
     * 检查负载率并在超过阈值时打印警告
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
     * 检查元素是否存在
     *
     * @param key 键
     * @return true:可能存在, false:一定不存在
     */
    @Override
    public boolean mightContain(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return bloomFilter.mightContain(key);
    }

    /**
     * 获取当前状态报告
     *
     * @return 状态报告
     */
    @Override
    public String getStatus() {
        return String.format(
                "BloomFilter{预期容量=%d, 当前元素≈%d(精确=%d), 负载率=%.2f%%, 误判率=%.6f, 存储元素=%d}",
                bloomFilterConfig.getExpectedInsertions(),
                bloomFilter.approximateElementCount(),
                elementCount.get(),
                calculateLoadFactor() * 100,
                bloomFilter.expectedFpp(),
                actualElements.size()
        );
    }

    /**
     * 计算当前负载率
     *
     * @return 负载率
     */
    @Override
    public double calculateLoadFactor() {
        long expected = bloomFilterConfig.getExpectedInsertions();
        return expected > 0 ?
                (double) elementCount.get() / expected : 0;
    }

    /**
     * 安全参数处理
     *
     * @param expected 预期插入数量
     * @return 预期插入数量
     */
    private int sanitizeExpectedInsertions(int expected) {
        return Math.max(100, expected); // 至少为 1
    }

    /**
     * 安全参数处理
     *
     * @param probability 错误概率
     * @return 错误概率
     */
    private double sanitizeFalseProbability(double probability) {
        return Math.min(0.999, Math.max(0.000001, probability)); // 限制在 0.0001% ~ 99.9%
    }

    /**
     * 获取近似元素数量
     *
     * @return 近似元素数量
     */
    @Override
    public long approximateElementCount() {
        return bloomFilter.approximateElementCount();
    }

    /**
     * 获取精确元素数量
     *
     * @return 精确元素数量
     */
    @Override
    public long exactElementCount() {
        return elementCount.get();
    }

    /**
     * 获取实际存储的元素数量
     *
     * @return 实际存储的元素数量
     */
    @Override
    public int actualElementCount() {
        return actualElements.size();
    }
}
