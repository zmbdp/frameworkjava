package com.zmbdp.common.bloomfilter.service.impl;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.zmbdp.common.bloomfilter.config.BloomFilterConfig;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.core.utils.StringUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 布隆过滤器服务（快速版本，不加锁）
 * <p>
 * 基于 Guava 的 BloomFilter 实现的快速布隆过滤器。
 * 不使用锁，性能最高，但只适合单线程环境或对线程安全要求不高的场景。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *     <li>高性能：不使用锁，性能最高</li>
 *     <li>精确计数：维护精确的元素计数器</li>
 *     <li>实际元素存储：维护实际元素集合，用于扩容时数据迁移</li>
 *     <li>负载率监控：自动检查负载率并打印警告</li>
 *     <li>支持扩容：支持手动扩容，迁移现有元素</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>单线程环境</li>
 *     <li>对性能要求极高的场景</li>
 *     <li>对线程安全要求不高的场景</li>
 *     <li>需要精确元素计数的场景</li>
 * </ul>
 * <p>
 * <b>配置要求：</b>
 * <ul>
 *     <li>需要在配置文件中设置：bloom.filter.type=fast</li>
 *     <li>需要依赖 Guava 库</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 添加元素（高性能，不加锁）
 * bloomFilterService.put("user:123");
 *
 * // 查询元素（高性能，不加锁）
 * if (bloomFilterService.mightContain("user:123")) {
 *     // 可能存在，继续查询
 * }
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>不使用锁，性能最高，但线程不安全</li>
 *     <li>只适合单线程环境或对线程安全要求不高的场景</li>
 *     <li>维护实际元素集合，占用额外内存</li>
 *     <li>支持手动扩容，扩容时会迁移现有元素</li>
 *     <li>负载率超过阈值时会自动打印警告</li>
 *     <li>如果需要线程安全，使用 {@link SafeBloomFilterService}</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.bloomfilter.service.BloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.RedisBloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.SafeBloomFilterService
 */
@Slf4j
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "fast") // 只有读取到这个配置时，才会初始化
public class FastBloomFilterService implements BloomFilterService {

    /**
     * 精确元素计数器
     * <p>
     * 使用 AtomicLong 维护精确的元素计数（去重后的元素数量）。
     * 虽然使用 AtomicLong，但由于其他操作不加锁，整体仍不是线程安全的。
     */
    private final AtomicLong elementCount = new AtomicLong(0);

    /**
     * 存储实际元素的集合（用于扩容时的数据迁移）
     * <p>
     * 维护实际添加的元素集合，用于扩容时将现有元素迁移到新的布隆过滤器。
     * 注意：这会占用额外内存，如果元素数量很大，内存占用会较高。
     */
    private final Set<String> actualElements = new HashSet<>();

    /**
     * 布隆过滤器配置
     */
    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    /**
     * Guava 布隆过滤器实例
     * <p>
     * 使用 volatile 关键字保证可见性，但不保证原子性。
     * 在多线程环境下，替换操作可能不安全。
     */
    private volatile BloomFilter<String> bloomFilter;

    /**
     * 清空布隆过滤器
     * <p>
     * 清空布隆过滤器中的所有元素，但保留配置参数。
     * 通过重新初始化过滤器实现清空效果。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>清空后所有元素都会被删除</li>
     *     <li>配置参数保持不变</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @see #reset()
     */
    @Override
    public void clear() {
        refreshFilter(); // 重新初始化过滤器，达到清空效果
    }

    /**
     * 初始化/重置过滤器
     * <p>
     * 重置布隆过滤器，清空所有元素并重新初始化。
     * 在应用启动时自动调用（@PostConstruct）。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>在应用启动时自动调用</li>
     *     <li>重置后所有元素都会被删除</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @see #clear()
     */
    @Override
    @PostConstruct
    public void reset() {
        refreshFilter();
    }

    /**
     * 刷新过滤器实例
     * <p>
     * 重新创建布隆过滤器实例，清空元素计数和实际元素集合。
     * 使用当前配置参数创建新的布隆过滤器。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>使用配置参数创建新的布隆过滤器</li>
     *     <li>重置元素计数</li>
     *     <li>清空实际元素集合</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>会校验和修正配置参数（预期插入数量、误判率）</li>
     *     <li>重置后需要重新添加元素</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
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
     * 手动扩容布隆过滤器
     * <p>
     * 创建新的布隆过滤器，并将现有元素迁移到新过滤器中。
     * 扩容后元素计数保持不变。用户可以在修改配置后调用此方法进行扩容。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查负载率
     * double loadFactor = bloomFilterService.calculateLoadFactor();
     * if (loadFactor > 0.8) {
     *     log.warn("负载率过高，开始扩容");
     *     bloomFilterService.expand();
     * }
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>使用当前配置创建新的布隆过滤器</li>
     *     <li>将现有元素迁移到新过滤器</li>
     *     <li>替换旧的布隆过滤器</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>扩容会创建新的布隆过滤器，并将现有元素迁移</li>
     *     <li>元素计数保持不变</li>
     *     <li>扩容操作可能较慢，取决于元素数量</li>
     *     <li>如果配置已更新，会使用新配置创建过滤器</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     *     <li>如果扩容失败，会抛出 RuntimeException</li>
     * </ul>
     *
     * @throws RuntimeException 如果扩容失败
     * @see #calculateLoadFactor()
     * @see #reset()
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
     * 添加元素到布隆过滤器（高性能，不加锁）
     * <p>
     * 将元素添加到布隆过滤器中。操作不加锁，性能最高，但线程不安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 添加元素（高性能，不加锁）
     * bloomFilterService.put("user:123");
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>将元素添加到布隆过滤器</li>
     *     <li>增加元素计数</li>
     *     <li>将元素添加到实际元素集合</li>
     *     <li>检查负载率并打印警告（如果超过阈值）</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>操作不加锁，性能最高，但线程不安全</li>
     *     <li>如果 key 为 null 或空字符串，操作会被忽略</li>
     *     <li>元素会被添加到布隆过滤器和实际元素集合</li>
     *     <li>如果负载率超过阈值，会打印警告日志</li>
     *     <li>只适合单线程环境或对线程安全要求不高的场景</li>
     * </ul>
     *
     * @param key 要添加的元素键，不能为 null 或空字符串
     * @see #putAll(Collection)
     * @see #mightContain(String)
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
     * 批量添加元素到布隆过滤器（高性能，不加锁）
     * <p>
     * 将多个元素批量添加到布隆过滤器中。通过调用 put 方法逐个添加。
     * 操作不加锁，性能最高，但线程不安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量添加元素（高性能，不加锁）
     * Collection<String> keys = Arrays.asList("user:1", "user:2", "user:3");
     * bloomFilterService.putAll(keys);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>操作不加锁，性能最高，但线程不安全</li>
     *     <li>如果 keys 为 null，会抛出 NullPointerException</li>
     *     <li>通过调用 put 方法逐个添加元素</li>
     *     <li>只适合单线程环境或对线程安全要求不高的场景</li>
     * </ul>
     *
     * @param keys 要添加的元素键集合，不能为 null
     * @see #put(String)
     * @see #mightContainAny(Collection)
     */
    @Override
    public void putAll(Collection<String> keys) {
        keys.forEach(this::put);
    }

    /**
     * 检查负载率并在超过阈值时打印警告
     * <p>
     * 当负载率超过配置的警告阈值时，打印警告日志，提示建议扩容或重置。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>负载率 = 当前元素数量 / 预期插入数量</li>
     *     <li>如果负载率 >= 警告阈值，打印警告日志</li>
     *     <li>警告阈值从配置中读取</li>
     * </ul>
     *
     * @param currentCount 当前元素数量，必须大于等于 0
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
     * 检查元素是否可能存在（高性能，不加锁）
     * <p>
     * 查询元素是否在布隆过滤器中。操作不加锁，性能最高，但线程不安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询元素（高性能，不加锁）
     * if (bloomFilterService.mightContain("user:123")) {
     *     // 可能存在，继续查询
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>操作不加锁，性能最高，但线程不安全</li>
     *     <li>如果 key 为 null 或空字符串，返回 false</li>
     *     <li>返回 false 表示一定不存在</li>
     *     <li>返回 true 表示可能存在（可能误判）</li>
     *     <li>只适合单线程环境或对线程安全要求不高的场景</li>
     * </ul>
     *
     * @param key 要查询的元素键，不能为 null 或空字符串
     * @return true 表示可能存在，false 表示一定不存在
     * @see #mightContainAny(Collection)
     * @see #put(String)
     */
    @Override
    public boolean mightContain(String key) {
        if (StringUtil.isEmpty(key)) {
            return false;
        }
        return bloomFilter.mightContain(key);
    }

    /**
     * 判断集合中是否有任意元素可能存在（高性能，不加锁）
     * <p>
     * 批量查询，只要有一个元素返回 true，就立即返回 true（短路操作）。
     * 操作不加锁，性能最高，但线程不安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量查询（高性能，不加锁）
     * Collection<String> keys = Arrays.asList("user:1", "user:2", "user:3");
     * if (bloomFilterService.mightContainAny(keys)) {
     *     // 至少有一个可能存在
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>操作不加锁，性能最高，但线程不安全</li>
     *     <li>只要有一个元素返回 true，就立即返回 true（短路）</li>
     *     <li>返回 false 表示所有元素一定都不存在</li>
     *     <li>只适合单线程环境或对线程安全要求不高的场景</li>
     * </ul>
     *
     * @param keys 要查询的元素键集合，不能为 null
     * @return true 表示至少有一个可能存在，false 表示所有都不存在
     * @see #mightContain(String)
     * @see #putAll(Collection)
     */
    @Override
    public boolean mightContainAny(Collection<String> keys) {
        return keys.stream()
                .anyMatch(
                        key -> mightContain(key)
                );
    }

    /**
     * 获取当前状态报告
     * <p>
     * 返回布隆过滤器的详细状态信息，包括预期容量、当前元素数量、负载率、误判率等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取状态信息
     * String status = bloomFilterService.getStatus();
     * log.info("布隆过滤器状态: {}", status);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>状态信息包括预期容量、近似元素数量、精确元素数量、负载率、误判率、实际存储元素数量</li>
     *     <li>获取状态是只读操作，不会修改布隆过滤器</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @return 布隆过滤器的状态信息字符串
     * @see #calculateLoadFactor()
     * @see #exactElementCount()
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
     * <p>
     * 负载率 = 已插入元素数 / 预期插入元素数。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>负载率范围：0.0 - 1.0+</li>
     *     <li>如果预期插入数量为 0，返回 0</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @return 负载率（0.0 - 1.0+）
     * @see #exactElementCount()
     * @see #expand()
     */
    @Override
    public double calculateLoadFactor() {
        long expected = bloomFilterConfig.getExpectedInsertions();
        return expected > 0 ?
                (double) elementCount.get() / expected : 0;
    }

    /**
     * 校验和修正预期插入数量
     * <p>
     * 确保预期插入数量至少为 100，避免参数过小导致布隆过滤器效率低下。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果预期插入数量小于 100，返回 100</li>
     *     <li>如果预期插入数量大于等于 100，返回原值</li>
     *     <li>Guava 的 BloomFilter 要求预期插入数量至少为 1，这里设置为 100 更安全</li>
     * </ul>
     *
     * @param expected 预期插入数量，可能小于 100
     * @return 修正后的预期插入数量（至少为 100）
     */
    private int sanitizeExpectedInsertions(int expected) {
        return Math.max(100, expected); // 至少为 1
    }

    /**
     * 校验和修正误判概率
     * <p>
     * 确保误判概率在 0.000001 到 0.999 之间，避免参数超出有效范围。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果误判概率小于 0.000001，返回 0.000001</li>
     *     <li>如果误判概率大于 0.999，返回 0.999</li>
     *     <li>如果误判概率在有效范围内，返回原值</li>
     *     <li>Guava 的 BloomFilter 要求误判概率在 0 到 1 之间，这里进一步限制范围</li>
     * </ul>
     *
     * @param probability 误判概率，可能超出有效范围
     * @return 修正后的误判概率（0.000001 - 0.999）
     */
    private double sanitizeFalseProbability(double probability) {
        return Math.min(0.999, Math.max(0.000001, probability)); // 限制在 0.0001% ~ 99.9%
    }

    /**
     * 获取近似元素数量
     * <p>
     * 从 Guava 的 BloomFilter 获取近似元素数量。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是近似值，可能不够精确</li>
     *     <li>如果需要精确值，使用 {@link #exactElementCount()} 方法</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @return 近似元素数量
     * @see #exactElementCount()
     * @see #actualElementCount()
     */
    @Override
    public long approximateElementCount() {
        return bloomFilter.approximateElementCount();
    }

    /**
     * 获取精确元素数量
     * <p>
     * 从 AtomicLong 计数器获取精确的元素数量（去重后的元素数量）。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是精确值（通过计数器维护）</li>
     *     <li>精确值等于实际添加的元素数量（去重后）</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @return 精确元素数量
     * @see #approximateElementCount()
     * @see #actualElementCount()
     */
    @Override
    public long exactElementCount() {
        return elementCount.get();
    }

    /**
     * 获取实际存储的元素数量
     * <p>
     * 从 actualElements 集合获取实际存储的元素数量。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是实际存储的元素数量（actualElements 集合的大小）</li>
     *     <li>实际元素数量用于扩容时的数据迁移</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     * </ul>
     *
     * @return 实际存储的元素数量
     * @see #exactElementCount()
     * @see #approximateElementCount()
     */
    @Override
    public int actualElementCount() {
        return actualElements.size();
    }

    /**
     * 删除布隆过滤器
     * <p>
     * 清空布隆过滤器相关资源，包括布隆过滤器实例、元素计数和实际元素集合。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除布隆过滤器
     * boolean deleted = bloomFilterService.delete();
     * if (deleted) {
     *     log.info("布隆过滤器已删除");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>删除操作不可逆，所有数据都会丢失</li>
     *     <li>删除后需要调用 reset() 重新初始化</li>
     *     <li>操作不加锁，不适合多线程环境</li>
     *     <li>如果删除过程中发生异常，返回 false</li>
     * </ul>
     *
     * @return true 表示删除成功，false 表示删除失败
     * @see #clear()
     * @see #reset()
     */
    @Override
    public boolean delete() {
        try {
            // 清空布隆过滤器相关资源
            this.bloomFilter = null;
            elementCount.set(0);
            actualElements.clear();
            log.info("布隆过滤器删除成功");
            return true;
        } catch (Exception e) {
            log.error("删除布隆过滤器时发生错误", e);
            return false;
        }
    }
}