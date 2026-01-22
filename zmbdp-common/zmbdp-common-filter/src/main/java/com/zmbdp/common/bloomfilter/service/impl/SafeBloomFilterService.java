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
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 布隆过滤器服务（线程安全版本）
 * <p>
 * 基于 Guava 的 BloomFilter 实现的线程安全布隆过滤器。
 * 使用读写锁（ReentrantReadWriteLock）保证并发安全，适合多线程环境。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *     <li>线程安全：使用读写锁保证并发安全</li>
 *     <li>精确计数：维护精确的元素计数器</li>
 *     <li>实际元素存储：维护实际元素集合，用于扩容时数据迁移</li>
 *     <li>负载率监控：自动检查负载率并打印警告</li>
 *     <li>支持扩容：支持手动扩容，迁移现有元素</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>单实例多线程环境</li>
 *     <li>需要线程安全的场景</li>
 *     <li>需要精确元素计数的场景</li>
 *     <li>需要支持扩容的场景</li>
 * </ul>
 * <p>
 * <b>配置要求：</b>
 * <ul>
 *     <li>需要在配置文件中设置：bloom.filter.type=safe</li>
 *     <li>需要依赖 Guava 库</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 添加元素（线程安全）
 * bloomFilterService.put("user:123");
 *
 * // 查询元素（线程安全）
 * if (bloomFilterService.mightContain("user:123")) {
 *     // 可能存在，继续查询
 * }
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>使用读写锁保证线程安全，读操作并发，写操作互斥</li>
 *     <li>维护实际元素集合，占用额外内存</li>
 *     <li>支持手动扩容，扩容时会迁移现有元素</li>
 *     <li>负载率超过阈值时会自动打印警告</li>
 *     <li>单实例部署，不支持分布式</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.bloomfilter.service.BloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.RedisBloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.FastBloomFilterService
 */
@Slf4j
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "safe")
public class SafeBloomFilterService implements BloomFilterService {

    /**
     * 精确元素计数器
     * <p>
     * 使用 AtomicLong 维护精确的元素计数（去重后的元素数量）。
     * 线程安全，支持并发更新。
     */
    private final AtomicLong elementCount = new AtomicLong(0);

    /**
     * 存储实际元素的集合（用于扩容时数据迁移）
     * <p>
     * 维护实际添加的元素集合，用于扩容时将现有元素迁移到新的布隆过滤器。
     * 注意：这会占用额外内存，如果元素数量很大，内存占用会较高。
     */
    private final Set<String> actualElements = new HashSet<>();

    /**
     * 读写锁
     * <p>
     * 用于保证线程安全：
     * <ul>
     *     <li>读操作（查询）使用读锁，支持并发</li>
     *     <li>写操作（添加、删除、扩容）使用写锁，互斥执行</li>
     * </ul>
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
     * 初始化/重置过滤器（线程安全）
     * <p>
     * 重置布隆过滤器，清空所有元素并重新初始化。
     * 使用写锁保证线程安全。在应用启动时自动调用（@PostConstruct）。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>在应用启动时自动调用</li>
     *     <li>重置后所有元素都会被删除</li>
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
     * 清空布隆过滤器（线程安全）
     * <p>
     * 清空布隆过滤器中的所有元素，但保留配置参数。
     * 使用写锁保证线程安全。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>清空后所有元素都会被删除</li>
     *     <li>配置参数保持不变</li>
     * </ul>
     *
     * @see #reset()
     */
    @Override
    public void clear() {
        refreshFilter();
    }

    /**
     * 刷新过滤器实例（线程安全）
     * <p>
     * 重新创建布隆过滤器实例，清空元素计数和实际元素集合。
     * 使用写锁保证线程安全。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取写锁</li>
     *     <li>使用配置参数创建新的布隆过滤器</li>
     *     <li>重置元素计数</li>
     *     <li>清空实际元素集合</li>
     *     <li>释放写锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>会校验和修正配置参数（预期插入数量、误判率）</li>
     *     <li>重置后需要重新添加元素</li>
     * </ul>
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
     * 添加元素到布隆过滤器（线程安全）
     * <p>
     * 将元素添加到布隆过滤器中。使用写锁保证线程安全。
     * 只有新元素才会被添加到布隆过滤器和计数器中。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 添加元素（线程安全）
     * bloomFilterService.put("user:123");
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取写锁</li>
     *     <li>检查元素是否已存在（通过 actualElements 集合）</li>
     *     <li>如果是新元素，添加到布隆过滤器和计数器</li>
     *     <li>检查负载率并打印警告（如果超过阈值）</li>
     *     <li>释放写锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>如果 key 为 null 或空字符串，操作会被忽略</li>
     *     <li>只有新元素才会被添加（通过 actualElements 集合判断）</li>
     *     <li>如果负载率超过阈值，会打印警告日志</li>
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
     * 批量添加元素到布隆过滤器（线程安全）
     * <p>
     * 将多个元素批量添加到布隆过滤器中。使用写锁保证线程安全。
     * 只有新元素才会被添加。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量添加元素（线程安全）
     * Collection<String> keys = Arrays.asList("user:1", "user:2", "user:3");
     * bloomFilterService.putAll(keys);
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取写锁</li>
     *     <li>遍历元素集合，检查每个元素是否已存在</li>
     *     <li>如果是新元素，添加到布隆过滤器和计数器</li>
     *     <li>检查负载率并打印警告（如果超过阈值）</li>
     *     <li>释放写锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>如果 keys 为 null 或空集合，操作会被忽略</li>
     *     <li>集合中的 null 或空字符串会被跳过</li>
     *     <li>只有新元素才会被添加</li>
     *     <li>批量添加比逐个添加效率更高（只需获取一次锁）</li>
     * </ul>
     *
     * @param keys 要添加的元素键集合，不能为 null
     * @see #put(String)
     * @see #mightContainAny(Collection)
     */
    @Override
    public void putAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.warn("尝试批量添加空集合到布隆过滤器");
            return;
        }
        rwLock.writeLock().lock();
        try {
            int addedCount = 0;
            // 再遍历集合，添加不为空的元素
            for (String key : keys) {
                if (key == null || key.isEmpty()) {
                    log.debug("跳过空键");
                    continue;
                }
                // 完全依赖 Set 判断新元素
                if (actualElements.add(key)) {
                    bloomFilter.put(key);
                    elementCount.incrementAndGet();
                    addedCount++;
                }
            }
            // 如果说有新元素添加成功，则检查负载率并打印警告
            if (addedCount > 0) {
                checkAndWarnLoadFactor(elementCount.get());
            }
            log.info("批量添加完成，共添加 {} 个新元素，当前状态: {}", addedCount, getStatus());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 检查元素是否可能存在（线程安全）
     * <p>
     * 使用读锁查询元素是否在布隆过滤器中。读操作支持并发，不会阻塞其他读操作。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询元素（线程安全，支持并发读）
     * if (bloomFilterService.mightContain("user:123")) {
     *     // 可能存在，继续查询
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全，支持并发读</li>
     *     <li>如果 key 为 null 或空字符串，返回 false</li>
     *     <li>返回 false 表示一定不存在</li>
     *     <li>返回 true 表示可能存在（可能误判）</li>
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
        rwLock.readLock().lock();
        try {
            return bloomFilter.mightContain(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 判断集合中是否有任意元素可能存在（线程安全）
     * <p>
     * 使用读锁批量查询，只要有一个元素返回 true，就立即返回 true（短路操作）。
     * 读操作支持并发。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量查询（线程安全，支持并发读）
     * Collection<String> keys = Arrays.asList("user:1", "user:2", "user:3");
     * if (bloomFilterService.mightContainAny(keys)) {
     *     // 至少有一个可能存在
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全，支持并发读</li>
     *     <li>只要有一个元素返回 true，就立即返回 true（短路）</li>
     *     <li>返回 false 表示所有元素一定都不存在</li>
     * </ul>
     *
     * @param keys 要查询的元素键集合，不能为 null
     * @return true 表示至少有一个可能存在，false 表示所有都不存在
     * @see #mightContain(String)
     * @see #putAll(Collection)
     */
    @Override
    public boolean mightContainAny(Collection<String> keys) {
        rwLock.readLock().lock();
        try {
            return keys.stream().anyMatch(bloomFilter::mightContain);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 手动扩容布隆过滤器（线程安全）
     * <p>
     * 创建新的布隆过滤器，并将现有元素迁移到新过滤器中。
     * 使用写锁保证线程安全。扩容后元素计数保持不变。
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
     *     <li>获取写锁</li>
     *     <li>使用当前配置创建新的布隆过滤器</li>
     *     <li>将现有元素迁移到新过滤器</li>
     *     <li>替换旧的布隆过滤器</li>
     *     <li>释放写锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>扩容会创建新的布隆过滤器，并将现有元素迁移</li>
     *     <li>元素计数保持不变</li>
     *     <li>扩容操作可能较慢，取决于元素数量</li>
     *     <li>如果配置已更新，会使用新配置创建过滤器</li>
     * </ul>
     *
     * @see #calculateLoadFactor()
     * @see #reset()
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
     * <p>
     * 返回布隆过滤器的详细状态信息，包括预期容量、当前元素数量、负载率、误判率等。
     * 使用读锁保证线程安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取状态信息（线程安全）
     * String status = bloomFilterService.getStatus();
     * log.info("布隆过滤器状态: {}", status);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全</li>
     *     <li>状态信息包括预期容量、近似元素数量、精确元素数量、负载率、误判率、实际存储元素数量</li>
     *     <li>获取状态是只读操作，不会修改布隆过滤器</li>
     * </ul>
     *
     * @return 布隆过滤器的状态信息字符串
     * @see #calculateLoadFactor()
     * @see #exactElementCount()
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
     * <p>
     * 负载率 = 已插入元素数 / 预期插入元素数。
     * 使用读锁保证线程安全。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全</li>
     *     <li>负载率范围：0.0 - 1.0+</li>
     *     <li>如果预期插入数量为 0，返回 0</li>
     * </ul>
     *
     * @return 负载率（0.0 - 1.0+）
     * @see #exactElementCount()
     * @see #expand()
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
     * <p>
     * 从 Guava 的 BloomFilter 获取近似元素数量。
     * 使用读锁保证线程安全。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全</li>
     *     <li>返回的是近似值，可能不够精确</li>
     *     <li>如果需要精确值，使用 {@link #exactElementCount()} 方法</li>
     * </ul>
     *
     * @return 近似元素数量
     * @see #exactElementCount()
     * @see #actualElementCount()
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
     * <p>
     * 从 AtomicLong 计数器获取精确的元素数量（去重后的元素数量）。
     * 使用读锁保证线程安全。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全</li>
     *     <li>返回的是精确值（通过计数器维护）</li>
     *     <li>精确值等于实际添加的元素数量（去重后）</li>
     * </ul>
     *
     * @return 精确元素数量
     * @see #approximateElementCount()
     * @see #actualElementCount()
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
     * <p>
     * 从 actualElements 集合获取实际存储的元素数量。
     * 使用读锁保证线程安全。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用读锁保证线程安全</li>
     *     <li>返回的是实际存储的元素数量（actualElements 集合的大小）</li>
     *     <li>实际元素数量用于扩容时的数据迁移</li>
     * </ul>
     *
     * @return 实际存储的元素数量
     * @see #exactElementCount()
     * @see #approximateElementCount()
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
     * 检查负载率超过阈值时打印警告
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
        return Math.max(100, expected);
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
        return Math.min(0.999, Math.max(0.000001, probability));
    }

    /**
     * 删除布隆过滤器（线程安全）
     * <p>
     * 清空布隆过滤器相关资源，包括布隆过滤器实例、元素计数和实际元素集合。
     * 使用写锁保证线程安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除布隆过滤器（线程安全）
     * boolean deleted = bloomFilterService.delete();
     * if (deleted) {
     *     log.info("布隆过滤器已删除");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用写锁保证线程安全</li>
     *     <li>删除操作不可逆，所有数据都会丢失</li>
     *     <li>删除后需要调用 reset() 重新初始化</li>
     *     <li>如果删除过程中发生异常，返回 false</li>
     * </ul>
     *
     * @return true 表示删除成功，false 表示删除失败
     * @see #clear()
     * @see #reset()
     */
    @Override
    public boolean delete() {
        rwLock.writeLock().lock();
        try {
            // 清空布隆过滤器相关资源
            if (bloomFilter != null) {
                bloomFilter = null;
            }
            elementCount.set(0);
            actualElements.clear();
            log.info("布隆过滤器删除成功");
            return true;
        } catch (Exception e) {
            log.error("删除布隆过滤器时发生错误", e);
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}