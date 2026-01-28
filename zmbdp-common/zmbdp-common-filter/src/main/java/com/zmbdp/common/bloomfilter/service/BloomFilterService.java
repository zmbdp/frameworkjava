package com.zmbdp.common.bloomfilter.service;

import java.util.Collection;

/**
 * 布隆过滤器服务接口
 * <p>
 * 提供布隆过滤器的核心功能，包括元素添加、查询、清空、扩容等操作。
 * 布隆过滤器是一种概率型数据结构，用于快速判断元素是否可能存在。
 * <p>
 * <b>布隆过滤器特性：</b>
 * <ul>
 *     <li>空间效率高：使用位数组存储，占用内存小</li>
 *     <li>查询速度快：O(k) 时间复杂度，k 为哈希函数数量</li>
 *     <li>不会漏判：如果判断不存在，则一定不存在</li>
 *     <li>可能误判：如果判断存在，可能实际不存在（误判率可配置）</li>
 *     <li>不支持删除：传统布隆过滤器不支持元素删除（某些实现支持）</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>缓存穿透防护：快速判断 key 是否存在，避免无效的数据库查询</li>
 *     <li>URL 去重：判断 URL 是否已爬取</li>
 *     <li>垃圾邮件过滤：判断邮件是否可能是垃圾邮件</li>
 *     <li>推荐系统：判断用户是否已看过某内容</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 添加元素
 * bloomFilterService.put("user:123");
 *
 * // 查询元素是否存在
 * if (bloomFilterService.mightContain("user:123")) {
 *     // 可能存在，继续查询数据库
 *     User user = userService.findById(123L);
 * } else {
 *     // 一定不存在，直接返回
 *     return null;
 * }
 *
 * // 批量添加
 * Collection<String> keys = Arrays.asList("user:1", "user:2", "user:3");
 * bloomFilterService.putAll(keys);
 *
 * // 获取状态
 * String status = bloomFilterService.getStatus();
 * log.info("布隆过滤器状态: {}", status);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>布隆过滤器可能出现误判，但不会漏判</li>
 *     <li>误判率与预期插入数量和位数组大小相关</li>
 *     <li>当元素数量接近预期值时，建议扩容或重置</li>
 *     <li>不同实现类有不同的特性（Redis、线程安全、快速版本）</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.bloomfilter.service.impl.RedisBloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.SafeBloomFilterService
 * @see com.zmbdp.common.bloomfilter.service.impl.FastBloomFilterService
 */
public interface BloomFilterService {

    /**
     * 添加元素到布隆过滤器
     * <p>
     * 将元素添加到布隆过滤器中。如果元素已存在，操作会被忽略（不会重复添加）。
     * 添加后，该元素在后续查询中会被判断为"可能存在"。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 添加单个元素
     * bloomFilterService.put("user:123");
     *
     * // 在缓存写入时同步添加到布隆过滤器
     * User user = userService.findById(123L);
     * redisService.setCacheObject("user:123", user, 3600, TimeUnit.SECONDS);
     * bloomFilterService.put("user:123");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null 或空字符串，操作会被忽略</li>
     *     <li>如果元素已存在，操作会被静默忽略（不会抛出异常）</li>
     *     <li>添加操作是线程安全的（取决于具体实现）</li>
     *     <li>添加后元素计数会增加</li>
     * </ul>
     *
     * @param key 要添加的元素键，不能为 null 或空字符串
     * @see #putAll(Collection)
     * @see #mightContain(String)
     */
    void put(String key);

    /**
     * 批量添加元素到布隆过滤器
     * <p>
     * 将多个元素批量添加到布隆过滤器中。如果某个元素已存在，会被忽略。
     * 批量添加比逐个添加效率更高，适合大量数据的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量添加用户ID
     * Collection<String> userIds = Arrays.asList("user:1", "user:2", "user:3");
     * bloomFilterService.putAll(userIds);
     *
     * // 批量添加从数据库查询到的ID
     * List<Long> ids = userService.findAllIds();
     * Collection<String> keys = ids.stream()
     *     .map(id -> "user:" + id)
     *     .collect(Collectors.toList());
     * bloomFilterService.putAll(keys);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 keys 为 null 或空集合，操作会被忽略</li>
     *     <li>集合中的 null 或空字符串会被忽略</li>
     *     <li>批量添加可能分批处理（取决于具体实现）</li>
     *     <li>批量添加比逐个调用 put 方法效率更高</li>
     * </ul>
     *
     * @param keys 要添加的元素键集合，不能为 null
     * @see #put(String)
     * @see #mightContainAny(Collection)
     */
    void putAll(Collection<String> keys);

    /**
     * 判断元素是否可能存在
     * <p>
     * 检查元素是否在布隆过滤器中。由于布隆过滤器的特性：
     * <ul>
     *     <li>如果返回 false，则元素一定不存在（不会漏判）</li>
     *     <li>如果返回 true，则元素可能存在（可能误判）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询前先检查布隆过滤器
     * if (!bloomFilterService.mightContain("user:123")) {
     *     // 一定不存在，直接返回，避免无效的数据库查询
     *     return null;
     * }
     * // 可能存在，继续查询数据库
     * User user = userService.findById(123L);
     *
     * // 在缓存查询中使用
     * if (bloomFilterService.mightContain(cacheKey)) {
     *     Object value = redisService.getCacheObject(cacheKey, User.class);
     *     if (value == null) {
     *         // 误判，实际不存在，查询数据库
     *         value = userService.findById(userId);
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null 或空字符串，返回 false</li>
     *     <li>返回 false 表示一定不存在（不会漏判）</li>
     *     <li>返回 true 表示可能存在（可能误判，需要进一步验证）</li>
     *     <li>查询操作是线程安全的（取决于具体实现）</li>
     *     <li>时间复杂度为 O(k)，k 为哈希函数数量</li>
     * </ul>
     *
     * @param key 要查询的元素键，不能为 null 或空字符串
     * @return true 表示可能存在，false 表示一定不存在
     * @see #mightContainAny(Collection)
     * @see #put(String)
     */
    boolean mightContain(String key);

    /**
     * 判断集合中是否有任意元素可能存在
     * <p>
     * 检查集合中是否至少有一个元素在布隆过滤器中。
     * 只要有一个元素返回 true，就立即返回 true（短路操作）。
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
     *     <li>返回 true 表示至少有一个可能存在（需要进一步验证）</li>
     *     <li>批量查询比逐个调用 mightContain 效率更高</li>
     * </ul>
     *
     * @param keys 要查询的元素键集合，不能为 null
     * @return true 表示至少有一个可能存在，false 表示所有都不存在
     * @see #mightContain(String)
     * @see #putAll(Collection)
     */
    boolean mightContainAny(Collection<String> keys);

    /**
     * 清空布隆过滤器和计数（配置不变）
     * <p>
     * 清空布隆过滤器中的所有元素，但保留配置参数（预期插入数量、误判率等）。
     * 清空后，布隆过滤器会重新初始化，元素计数归零。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 清空布隆过滤器（保留配置）
     * bloomFilterService.clear();
     * log.info("布隆过滤器已清空，状态: {}", bloomFilterService.getStatus());
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>清空后所有元素都会被删除，无法恢复</li>
     *     <li>配置参数（预期插入数量、误判率）保持不变</li>
     *     <li>清空后需要重新添加元素</li>
     *     <li>清空操作是线程安全的（取决于具体实现）</li>
     *     <li>如果需要删除整个布隆过滤器，使用 {@link #delete()} 方法</li>
     * </ul>
     *
     * @see #reset()
     * @see #delete()
     */
    void clear();

    /**
     * 重置布隆过滤器
     * <p>
     * 重置布隆过滤器，清空所有元素并重新初始化。
     * 与 {@link #clear()} 方法功能相同，但语义上更强调"重置"操作。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 重置布隆过滤器
     * bloomFilterService.reset();
     * log.info("布隆过滤器已重置，状态: {}", bloomFilterService.getStatus());
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>重置后所有元素都会被删除，无法恢复</li>
     *     <li>配置参数保持不变</li>
     *     <li>重置后需要重新添加元素</li>
     *     <li>通常在应用启动时自动调用（@PostConstruct）</li>
     *     <li>与 {@link #clear()} 方法功能相同</li>
     * </ul>
     *
     * @see #clear()
     * @see #delete()
     */
    void reset();

    /**
     * 扩容布隆过滤器
     * <p>
     * 当布隆过滤器负载率过高时，可以手动扩容。
     * 扩容会创建新的布隆过滤器，并将现有元素迁移到新过滤器中。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查负载率
     * double loadFactor = bloomFilterService.calculateLoadFactor();
     * if (loadFactor > 0.8) {
     *     log.warn("负载率过高: {}%，开始扩容", loadFactor * 100);
     *     bloomFilterService.expand();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>扩容会创建新的布隆过滤器，并将现有元素迁移</li>
     *     <li>扩容操作可能较慢，取决于元素数量</li>
     *     <li>某些实现（如 Redis）可能不支持手动扩容或自动扩容</li>
     *     <li>扩容后元素计数会保持不变</li>
     *     <li>建议在负载率超过阈值时进行扩容</li>
     * </ul>
     *
     * @see #calculateLoadFactor()
     * @see #reset()
     */
    void expand();

    /**
     * 获取布隆过滤器状态信息
     * <p>
     * 返回布隆过滤器的详细状态信息，包括元素数量、负载率、误判率等。
     * 适用于监控和调试场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取状态信息
     * String status = bloomFilterService.getStatus();
     * log.info("布隆过滤器状态: {}", status);
     *
     * // 定期输出状态用于监控
     * @Scheduled(fixedRate = 60000)
     * public void logBloomFilterStatus() {
     *     log.info(bloomFilterService.getStatus());
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的状态信息格式取决于具体实现</li>
     *     <li>状态信息包括元素数量、负载率、误判率等</li>
     *     <li>获取状态是只读操作，不会修改布隆过滤器</li>
     *     <li>适用于监控、调试和日志记录</li>
     * </ul>
     *
     * @return 布隆过滤器的状态信息字符串
     * @see #calculateLoadFactor()
     * @see #exactElementCount()
     */
    String getStatus();

    /**
     * 计算负载因子
     * <p>
     * 负载因子 = 已插入元素数量 / 预期插入数量。
     * 负载因子越高，误判率越高。当负载因子接近 1 时，建议扩容或重置。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查负载因子
     * double loadFactor = bloomFilterService.calculateLoadFactor();
     * if (loadFactor > 0.8) {
     *     log.warn("负载因子过高: {}%，建议扩容", loadFactor * 100);
     *     bloomFilterService.expand();
     * }
     *
     * // 监控负载因子
     * double loadFactor = bloomFilterService.calculateLoadFactor();
     * metrics.recordLoadFactor(loadFactor);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>负载因子范围：0.0 - 1.0（可能超过 1.0，表示已超过预期）</li>
     *     <li>负载因子越高，误判率越高</li>
     *     <li>当负载因子 > 0.8 时，建议扩容或重置</li>
     *     <li>负载因子 = 1.0 表示已达到预期插入数量</li>
     *     <li>如果预期插入数量为 0，返回 0 或 -1（取决于实现）</li>
     * </ul>
     *
     * @return 负载因子（0.0 - 1.0+），如果计算失败返回 -1
     * @see #exactElementCount()
     * @see #expand()
     */
    double calculateLoadFactor();

    /**
     * 获取近似元素数量
     * <p>
     * 返回布隆过滤器中元素的近似数量。<br>
     * 由于布隆过滤器的特性，这个值是估算值，可能不够精确，但计算速度快。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取近似元素数量
     * long approximateCount = bloomFilterService.approximateElementCount();
     * log.info("布隆过滤器中大约有 {} 个元素", approximateCount);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是近似值，可能不够精确</li>
     *     <li>由于误判的存在，近似值可能偏大</li>
     *     <li>如果需要精确值，使用 {@link #exactElementCount()} 方法</li>
     *     <li>某些实现可能直接返回精确值</li>
     * </ul>
     *
     * @return 近似元素数量
     * @see #exactElementCount()
     * @see #actualElementCount()
     */
    long approximateElementCount();

    /**
     * 获取精确元素数量
     * <p>
     * 返回布隆过滤器中元素的精确数量。这个值通过计数器维护，比近似值更准确。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取精确元素数量
     * long exactCount = bloomFilterService.exactElementCount();
     * log.info("布隆过滤器中精确有 {} 个元素", exactCount);
     *
     * // 检查是否接近预期值
     * long exactCount = bloomFilterService.exactElementCount();
     * if (exactCount >= bloomFilterConfig.getExpectedInsertions() * 0.8) {
     *     log.warn("元素数量接近预期值，建议扩容");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是精确值（通过计数器维护）</li>
     *     <li>精确值可能小于实际添加的元素数量（如果存在重复添加）</li>
     *     <li>某些实现可能返回近似值（如果未维护计数器）</li>
     *     <li>如果计数器丢失，可能返回 0 或 -1</li>
     * </ul>
     *
     * @return 精确元素数量，如果获取失败返回 -1
     * @see #approximateElementCount()
     * @see #actualElementCount()
     */
    long exactElementCount();

    /**
     * 获取实际存储的元素数量
     * <p>
     * 返回实际存储的元素数量（去重后的数量）。<br>
     * 某些实现会维护一个 Set 来存储实际元素，用于扩容时的数据迁移。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取实际元素数量
     * int actualCount = bloomFilterService.actualElementCount();
     * log.info("布隆过滤器中实际存储了 {} 个元素", actualCount);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是实际存储的元素数量（去重后）</li>
     *     <li>某些实现可能不维护实际元素集合，返回 0 或与精确值相同</li>
     *     <li>实际元素数量用于扩容时的数据迁移</li>
     *     <li>如果元素数量很大，维护实际元素集合会占用额外内存</li>
     * </ul>
     *
     * @return 实际存储的元素数量
     * @see #exactElementCount()
     * @see #approximateElementCount()
     */
    int actualElementCount();

    /**
     * 删除布隆过滤器
     * <p>
     * 完全删除布隆过滤器，包括所有元素、计数器和相关数据。
     * 删除后需要重新初始化才能使用。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除布隆过滤器
     * boolean deleted = bloomFilterService.delete();
     * if (deleted) {
     *     log.info("布隆过滤器已删除");
     *     // 重新初始化
     *     bloomFilterService.reset();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>删除操作不可逆，所有数据都会丢失</li>
     *     <li>删除后需要调用 {@link #reset()} 重新初始化</li>
     *     <li>删除操作是线程安全的（取决于具体实现）</li>
     *     <li>某些实现（如 Redis）会删除 Redis 中的相关数据</li>
     *     <li>与 {@link #clear()} 不同，delete 会完全删除，clear 会保留配置</li>
     * </ul>
     *
     * @return true 表示删除成功，false 表示删除失败
     * @see #clear()
     * @see #reset()
     */
    boolean delete();
}