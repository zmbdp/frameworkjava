package com.zmbdp.common.bloomfilter.service.impl;

import com.zmbdp.common.bloomfilter.config.BloomFilterConfig;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.common.redis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Redis 实现的分布式布隆过滤器服务
 * <p>
 * 基于 Redis 的 RedisBloom 模块实现的分布式布隆过滤器。<br>
 * 支持多实例部署，所有实例共享同一个布隆过滤器，保证数据一致性。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *     <li>分布式支持：多实例共享同一个布隆过滤器，数据一致</li>
 *     <li>高性能：基于 Redis 内存操作，查询速度快</li>
 *     <li>自动扩容：RedisBloom 支持自动扩容，无需手动管理</li>
 *     <li>异步计数：元素计数异步更新，不阻塞主线程</li>
 *     <li>线程安全：使用分布式锁保证并发安全</li>
 *     <li>持久化：数据存储在 Redis 中，支持持久化</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>多实例部署的应用，需要共享布隆过滤器</li>
 *     <li>需要持久化的场景</li>
 *     <li>需要自动扩容的场景</li>
 *     <li>高并发场景（Redis 性能优异）</li>
 * </ul>
 * <p>
 * <b>配置要求：</b>
 * <ul>
 *     <li>需要在配置文件中设置：bloom.filter.type=redis</li>
 *     <li>Redis 需要安装 RedisBloom 模块</li>
 *     <li>需要配置 Redisson 客户端</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 添加元素
 * bloomFilterService.put("user:123");
 *
 * // 查询元素
 * if (bloomFilterService.mightContain("user:123")) {
 *     // 可能存在，继续查询
 * }
 *
 * // 获取状态
 * String status = bloomFilterService.getStatus();
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>需要 Redis 支持 RedisBloom 模块（BF.ADD、BF.EXISTS 等命令）</li>
 *     <li>元素计数存储在 Redis 中，多实例共享</li>
 *     <li>初始化、重置、清空操作使用分布式锁保证安全</li>
 *     <li>RedisBloom 支持自动扩容，无需手动扩容</li>
 *     <li>异步更新计数，提高性能</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.bloomfilter.service.BloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.SafeBloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.FastBloomFilterService
 */
@Slf4j
@ConditionalOnProperty(value = "bloom.filter.type", havingValue = "redis")
public class RedisBloomFilterService implements BloomFilterService {

    /**
     * Redis 布隆过滤器 key
     * <p>
     * 用于在 Redis 中存储布隆过滤器的键名。
     */
    private static final String BLOOM_NAME = "frameworkjava:bloom";

    /**
     * Redis 元素计数 key
     * <p>
     * 用于在 Redis 中存储布隆过滤器元素计数的键名。<br>
     * 元素计数用于统计实际添加的元素数量（去重后）。
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
     * Redis 服务
     */
    @Autowired
    private RedisService redisService;

    /**
     * 初始化布隆过滤器（RedisBloom 类型）
     * <p>
     * 使用 RedisBloom 的 BF.RESERVE 命令创建布隆过滤器。<br>
     * 通过分布式锁保证多实例同时启动时不会重复创建。<br>
     * 如果布隆过滤器已存在，则跳过创建。
     * <p>
     * <b>初始化流程：</b>
     * <ol>
     *     <li>获取分布式锁，防止多实例重复创建</li>
     *     <li>检查布隆过滤器是否已存在</li>
     *     <li>如果不存在，使用 BF.RESERVE 命令创建</li>
     *     <li>释放分布式锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用分布式锁保证多实例安全</li>
     *     <li>如果布隆过滤器已存在，不会重复创建</li>
     *     <li>误判率和预期插入数量从配置中读取</li>
     *     <li>如果获取锁失败，会记录警告日志</li>
     * </ul>
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
     * <p>
     * 在后台线程中异步更新 Redis 中的元素计数，避免阻塞主线程。<br>
     * 如果启用了扩容检查，会在计数超过阈值时触发扩容提示。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>在后台线程中增加 Redis 计数</li>
     *     <li>如果启用了扩容检查，检查是否超过阈值</li>
     *     <li>如果超过阈值，记录警告日志并触发扩容</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>异步执行，不会阻塞主线程</li>
     *     <li>如果异步执行失败，会记录错误日志但不影响主流程</li>
     *     <li>扩容检查阈值 = 预期插入数量 × 警告阈值</li>
     *     <li>RedisBloom 支持自动扩容，这里只是提示</li>
     * </ul>
     *
     * @param delta 增加的元素数量，必须大于 0
     */
    private void incrementBloomCountAsync(long delta) {
        threadPoolTaskExecutor.execute(() -> {
            try {
                long total = redisService.incr(BLOOM_COUNT_KEY, delta); // 异步增加计数
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
     * 单条添加元素到布隆过滤器
     * <p>
     * 使用 RedisBloom 的 BF.ADD 命令添加元素。<br>
     * 如果元素是新增的（之前不存在），会异步更新元素计数。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 添加单个元素
     * bloomFilterService.put("user:123");
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>调用 RedisBloom 的 BF.ADD 命令</li>
     *     <li>如果返回 1，表示元素是新增的</li>
     *     <li>异步更新元素计数</li>
     *     <li>如果启用了扩容检查，检查是否需要扩容</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null 或空字符串，操作会被忽略</li>
     *     <li>如果元素已存在，返回 0，不会更新计数</li>
     *     <li>计数更新是异步的，不会阻塞主线程</li>
     *     <li>如果 Redis 操作失败，会记录错误日志</li>
     * </ul>
     *
     * @param key 待添加的元素键，不能为 null 或空字符串
     * @see #putAll(Collection)
     * @see #mightContain(String)
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
                log.info("[RedisBloom] 元素已存在: {}", key);
            }
        } catch (Exception e) {
            log.error("[RedisBloom] 添加元素失败: {}", key, e);
        }
    }

    /**
     * 批量添加元素到布隆过滤器
     * <p>
     * 使用 RedisBloom 的 BF.MADD 命令批量添加元素。
     * 为了提高性能，会按照配置的批次大小分批处理。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量添加元素
     * Collection<String> keys = Arrays.asList("user:1", "user:2", "user:3");
     * bloomFilterService.putAll(keys);
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>按照配置的批次大小分批处理</li>
     *     <li>每批调用 RedisBloom 的 BF.MADD 命令</li>
     *     <li>统计新增的元素数量</li>
     *     <li>异步更新元素计数</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 keys 为 null 或空集合，操作会被忽略</li>
     *     <li>会按照配置的批次大小分批处理，避免单次操作过大</li>
     *     <li>批量添加比逐个添加效率更高</li>
     *     <li>如果某批操作失败，会记录错误日志但继续处理下一批</li>
     * </ul>
     *
     * @param keys 待添加的元素键集合，不能为 null
     * @see #put(String)
     * @see #mightContainAny(Collection)
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
     * 查询元素是否可能存在
     * <p>
     * 使用 RedisBloom 的 BF.EXISTS 命令查询元素是否存在。
     * 如果返回 false，则元素一定不存在；如果返回 true，则元素可能存在（可能误判）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询元素是否存在
     * if (bloomFilterService.mightContain("user:123")) {
     *     // 可能存在，继续查询数据库
     *     User user = userService.findById(123L);
     * } else {
     *     // 一定不存在，直接返回
     *     return null;
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null 或空字符串，返回 false</li>
     *     <li>返回 false 表示一定不存在（不会漏判）</li>
     *     <li>返回 true 表示可能存在（可能误判，需要进一步验证）</li>
     *     <li>查询操作是只读的，不会修改布隆过滤器</li>
     *     <li>如果 Redis 操作失败，返回 false</li>
     * </ul>
     *
     * @param key 待查询的元素键，不能为 null 或空字符串
     * @return true 表示可能存在，false 表示一定不存在
     * @see #mightContainAny(Collection)
     * @see #put(String)
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
     * 批量查询集合中是否有任意元素可能存在
     * <p>
     * 使用 Lua 脚本批量查询，只要有一个元素返回 true，就立即返回 true（短路操作）。
     * 适用于批量检查场景，提高查询效率。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量检查用户ID是否存在
     * Collection<String> userIds = Arrays.asList("user:1", "user:2", "user:3");
     * if (bloomFilterService.mightContainAny(userIds)) {
     *     // 至少有一个可能存在，继续查询数据库
     *     List<User> users = userService.findByIds(userIds);
     * } else {
     *     // 所有都不存在，直接返回空列表
     *     return Collections.emptyList();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 keys 为 null 或空集合，返回 false</li>
     *     <li>只要有一个元素返回 true，就立即返回 true（短路）</li>
     *     <li>返回 false 表示所有元素一定都不存在</li>
     *     <li>使用 Lua 脚本批量查询，效率高于逐个查询</li>
     *     <li>如果 Redis 操作失败，返回 false</li>
     * </ul>
     *
     * @param keys 待查询的元素键集合，不能为 null
     * @return true 表示至少有一个可能存在，false 表示所有都不存在
     * @see #mightContain(String)
     * @see #putAll(Collection)
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
     * 清空布隆过滤器和计数（配置不变）
     * <p>
     * 删除 Redis 中的布隆过滤器和计数器，然后重新初始化。
     * 使用分布式锁保证多实例安全，避免并发清空导致的问题。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 清空布隆过滤器
     * bloomFilterService.clear();
     * log.info("布隆过滤器已清空，状态: {}", bloomFilterService.getStatus());
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取分布式锁</li>
     *     <li>删除布隆过滤器和计数器</li>
     *     <li>重新初始化布隆过滤器</li>
     *     <li>释放分布式锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用分布式锁保证多实例安全</li>
     *     <li>清空后所有元素都会被删除，无法恢复</li>
     *     <li>配置参数保持不变，会使用相同配置重新初始化</li>
     *     <li>如果获取锁失败，操作会被忽略</li>
     * </ul>
     *
     * @see #reset()
     * @see #delete()
     */
    @Override
    public void clear() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:clear");
        if (lock == null) {
            return;
        }
        try {
            redisService.deleteObject(Arrays.asList(BLOOM_NAME, BLOOM_COUNT_KEY));
            initBloomFilter();
            log.info("[RedisBloom] 已清空");
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 重置布隆过滤器（删除原有并重新初始化）
     * <p>
     * 删除 Redis 中的布隆过滤器和计数器，然后使用当前配置重新初始化。
     * 使用分布式锁保证多实例安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 重置布隆过滤器
     * bloomFilterService.reset();
     * log.info("布隆过滤器已重置，状态: {}", bloomFilterService.getStatus());
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取分布式锁</li>
     *     <li>删除布隆过滤器和计数器</li>
     *     <li>使用当前配置重新初始化</li>
     *     <li>释放分布式锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用分布式锁保证多实例安全</li>
     *     <li>重置后所有元素都会被删除，无法恢复</li>
     *     <li>会使用当前配置重新初始化（如果配置已更新，会使用新配置）</li>
     *     <li>如果获取锁失败，会记录警告日志</li>
     *     <li>通常在应用启动时自动调用（@PostConstruct）</li>
     * </ul>
     *
     * @see #clear()
     * @see #delete()
     */
    @Override
    public void reset() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:reset");
        if (lock == null) {
            log.warn("获取 reset 锁失败");
            return;
        }
        try {
            redisService.deleteObject(Arrays.asList(BLOOM_NAME, BLOOM_COUNT_KEY));
            initBloomFilter();
            log.info("[RedisBloom] 已重置");
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 扩容布隆过滤器（RedisBloom 自动扩容，无需手动操作）
     * <p>
     * RedisBloom 模块支持自动扩容，当元素数量超过预期值时，会自动扩容。
     * 因此此方法只需要记录日志，无需执行实际扩容操作。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // RedisBloom 会自动扩容，无需手动调用
     * bloomFilterService.expand(); // 只记录日志，不执行实际操作
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>RedisBloom 支持自动扩容，无需手动操作</li>
     *     <li>此方法只记录日志，不执行实际扩容</li>
     *     <li>如果需要重置布隆过滤器，使用 {@link #reset()} 方法</li>
     * </ul>
     *
     * @see #reset()
     * @see #calculateLoadFactor()
     */
    @Override
    public void expand() {
        log.info("[RedisBloom] 不需要手动扩容");
    }

    /**
     * 获取布隆过滤器状态信息
     * <p>
     * 返回布隆过滤器的详细状态信息，包括名称、元素计数、预期误判率等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取状态信息
     * String status = bloomFilterService.getStatus();
     * log.info("布隆过滤器状态: {}", status);
     * // 输出示例: RedisBloomFilter{name = frameworkjava:bloom, 元素计数 = 1000, 预期误判率 = 1.00%}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>状态信息包括名称、元素计数、预期误判率</li>
     *     <li>元素计数从 Redis 计数器获取</li>
     *     <li>误判率从配置中读取</li>
     *     <li>获取状态是只读操作，不会修改布隆过滤器</li>
     * </ul>
     *
     * @return 布隆过滤器的状态信息字符串
     * @see #exactElementCount()
     * @see #calculateLoadFactor()
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
     * 计算负载因子
     * <p>
     * 负载因子 = 已插入元素数 / 预期插入元素数。
     * 负载因子越高，误判率越高。当负载因子接近 1 时，建议扩容或重置。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查负载因子
     * double loadFactor = bloomFilterService.calculateLoadFactor();
     * if (loadFactor > 0.8) {
     *     log.warn("负载因子过高: {}%，建议重置", loadFactor * 100);
     *     bloomFilterService.reset();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>负载因子 = 已插入元素数 / 预期插入元素数</li>
     *     <li>负载因子范围：0.0 - 1.0+（可能超过 1.0）</li>
     *     <li>负载因子越高，误判率越高</li>
     *     <li>当负载因子 > 0.8 时，建议重置或扩容</li>
     *     <li>如果计算失败，返回 -1</li>
     * </ul>
     *
     * @return 负载因子（0.0 - 1.0+），如果计算失败返回 -1
     * @see #exactElementCount()
     * @see #reset()
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
     * <p>
     * 由于 RedisBloom 不提供近似元素数量接口，此方法直接返回精确元素数量。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>直接返回精确元素数量（与 exactElementCount 相同）</li>
     *     <li>元素数量从 Redis 计数器获取</li>
     * </ul>
     *
     * @return 近似元素数量（实际为精确值）
     * @see #exactElementCount()
     */
    @Override
    public long approximateElementCount() {
        return exactElementCount();
    }

    /**
     * 获取精确元素数量（从 Redis 计数器获取）
     * <p>
     * 从 Redis 中获取元素计数器的值，这个值是精确的（去重后的元素数量）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取精确元素数量
     * long count = bloomFilterService.exactElementCount();
     * log.info("布隆过滤器中精确有 {} 个元素", count);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>元素计数存储在 Redis 中，多实例共享</li>
     *     <li>计数是精确的（去重后的元素数量）</li>
     *     <li>如果计数器不存在，返回 0</li>
     *     <li>如果获取失败，返回 -1</li>
     * </ul>
     *
     * @return 精确元素数量，如果获取失败返回 -1
     * @see #approximateElementCount()
     * @see #actualElementCount()
     */
    @Override
    public long exactElementCount() {
        try {
            Long count = redisService.getCacheObject(BLOOM_COUNT_KEY, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("[RedisBloom] 获取元素计数失败", e);
            return -1;
        }
    }

    /**
     * 获取实际存储的元素数量（从 Redis 计数器获取）
     * <p>
     * 从 Redis 计数器获取实际元素数量，并转换为 int 类型。
     * 如果数量超过 Integer.MAX_VALUE，返回 Integer.MAX_VALUE。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>元素数量从 Redis 计数器获取</li>
     *     <li>如果数量超过 Integer.MAX_VALUE，返回 Integer.MAX_VALUE</li>
     *     <li>与 exactElementCount 功能相同，只是返回类型不同</li>
     * </ul>
     *
     * @return 实际存储的元素数量（int 类型）
     * @see #exactElementCount()
     */
    @Override
    public int actualElementCount() {
        long count = exactElementCount();
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    /**
     * 删除布隆过滤器（不重新创建）
     * <p>
     * 完全删除 Redis 中的布隆过滤器、计数器和配置信息。
     * 删除后不会自动重新创建，需要手动调用 {@link #reset()} 方法重新初始化。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除布隆过滤器
     * boolean deleted = bloomFilterService.delete();
     * if (deleted) {
     *     log.info("布隆过滤器已删除");
     *     // 如果需要，可以重新初始化
     *     bloomFilterService.reset();
     * }
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取分布式锁</li>
     *     <li>删除布隆过滤器、计数器和配置信息</li>
     *     <li>释放分布式锁</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用分布式锁保证多实例安全</li>
     *     <li>删除操作不可逆，所有数据都会丢失</li>
     *     <li>删除后不会自动重新创建</li>
     *     <li>如果需要继续使用，需要调用 reset() 重新初始化</li>
     *     <li>如果获取锁失败，返回 false</li>
     * </ul>
     *
     * @return true 表示删除成功，false 表示删除失败
     * @see #clear()
     * @see #reset()
     */
    @Override
    public boolean delete() {
        RLock lock = redissonLockService.acquire("lock:redis:bloom:delete");
        if (lock == null) {
            return false;
        }
        try {
            // 删除布隆过滤器、计数器、配置信息
            long result = redisService.deleteObject(Arrays.asList(BLOOM_NAME, BLOOM_COUNT_KEY, "{frameworkjava:bloom}:config"));

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