package com.zmbdp.common.cache.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.redis.service.RedisService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 缓存工具类
 * <p>
 * 提供二级缓存（本地缓存 + Redis）和布隆过滤器的统一操作接口。
 * 实现多级缓存架构，提高缓存命中率和系统性能。
 * <p>
 * <b>缓存架构：</b>
 * <ul>
 *     <li>一级缓存：本地缓存（Caffeine），速度快，容量小</li>
 *     <li>二级缓存：Redis 缓存，分布式，容量大</li>
 *     <li>布隆过滤器：快速判断 key 是否存在，避免无效的 Redis 查询</li>
 * </ul>
 * <p>
 * <b>缓存策略：</b>
 * <ul>
 *     <li>读取：先查一级缓存 → 再查二级缓存 → 如果查到，回填一级缓存</li>
 *     <li>写入：同时写入一级和二级缓存（可选布隆过滤器）</li>
 *     <li>删除：同时删除一级和二级缓存</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 获取缓存（二级缓存）
 * UserDTO user = CacheUtil.getL2Cache(redisService, "user:123", UserDTO.class, caffeineCache);
 * if (user == null) {
 *     // 缓存未命中，查询数据库
 *     user = userService.findById(123L);
 *     // 写入缓存
 *     CacheUtil.setL2Cache(redisService, "user:123", user, caffeineCache, 3600L, TimeUnit.SECONDS);
 * }
 *
 * // 使用布隆过滤器（提高性能）
 * UserDTO user2 = CacheUtil.getL2Cache(redisService, bloomFilterService,
 *     "user:456", UserDTO.class, caffeineCache);
 *
 * // 删除缓存
 * CacheUtil.delL2Cache("user:123", caffeineCache, redisService);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法</li>
 *     <li>一级缓存使用 Caffeine，二级缓存使用 Redis</li>
 *     <li>布隆过滤器可以快速过滤不存在的 key，减少 Redis 查询</li>
 *     <li>从二级缓存查到的数据会自动回填到一级缓存</li>
 *     <li>删除操作会同时删除一级和二级缓存</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.github.benmanes.caffeine.cache.Cache
 * @see com.zmbdp.common.redis.service.RedisService
 * @see com.zmbdp.common.bloomfilter.service.BloomFilterService
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheUtil {

    /**
     * 从二级缓存中获取数据（先查一级缓存，再查二级缓存）
     * <p>
     * 实现二级缓存查询逻辑：先查询本地缓存（Caffeine），如果未命中则查询 Redis 缓存。
     * 如果从 Redis 中查到数据，会自动回填到本地缓存，提高后续查询速度。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户缓存
     * UserDTO user = CacheUtil.getL2Cache(redisService, "user:123", UserDTO.class, caffeineCache);
     * if (user == null) {
     *     // 缓存未命中，查询数据库
     *     user = userService.findById(123L);
     *     // 写入缓存
     *     CacheUtil.setL2Cache(redisService, "user:123", user, caffeineCache, 3600L, TimeUnit.SECONDS);
     * }
     *
     * // 获取配置缓存
     * ConfigDTO config = CacheUtil.getL2Cache(redisService, "config:system", ConfigDTO.class, caffeineCache);
     * }</pre>
     * <p>
     * <b>查询流程：</b>
     * <ol>
     *     <li>先从一级缓存（Caffeine）中查询，如果命中则直接返回</li>
     *     <li>如果一级缓存未命中，从二级缓存（Redis）中查询</li>
     *     <li>如果二级缓存命中，将数据回填到一级缓存，然后返回</li>
     *     <li>如果两级缓存都未命中，返回 null</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null，可能抛出异常</li>
     *     <li>如果缓存未命中，返回 null，需要调用者查询数据库</li>
     *     <li>从二级缓存查到的数据会自动回填到一级缓存</li>
     *     <li>不支持复杂泛型嵌套，如需支持请使用 {@link #getL2Cache(RedisService, String, TypeReference, Cache)}</li>
     *     <li>如果需要使用布隆过滤器，使用带 BloomFilterService 参数的重载方法</li>
     * </ul>
     *
     * @param redisService  Redis 缓存服务，不能为 null
     * @param key           缓存的键，不能为 null
     * @param clazz         值类型（Class），不能为 null
     * @param caffeineCache 本地缓存（Caffeine），不能为 null
     * @param <T>           缓存值的类型
     * @return 缓存值，如果未命中则返回 null
     * @see #getL2Cache(RedisService, String, TypeReference, Cache)
     * @see #getL2Cache(RedisService, BloomFilterService, String, Class, Cache)
     */
    public static <T> T getL2Cache(RedisService redisService, String key, Class<T> clazz, Cache<String, Object> caffeineCache) {
        // 先从一级缓存中拿取数据, 如果有就返回
        T ifPresent = (T) caffeineCache.getIfPresent(key);
        if (ifPresent != null) {
            return ifPresent;
        }
        // 如果没查到，就查二级缓存
        ifPresent = redisService.getCacheObject(key, clazz);
        if (ifPresent != null) {
            // 如果查到了，就存储到一级缓存中再返回
            setL2Cache(key, ifPresent, caffeineCache);
            return ifPresent;
        }
        // 如果还没查到，就返回空，让用户去查询数据库
        return null;
    }

    /**
     * 从二级缓存中获取数据（支持复杂泛型嵌套）
     * <p>
     * 与 {@link #getL2Cache(RedisService, String, Class, Cache)} 功能相同，但支持复杂泛型类型的缓存值。
     * 使用 TypeReference 来处理复杂泛型嵌套，如 List&lt;Map&lt;String, UserDTO&gt;&gt; 等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取复杂泛型缓存
     * List<Map<String, UserDTO>> complexData = CacheUtil.getL2Cache(redisService,
     *     "complex:data",
     *     new TypeReference<List<Map<String, UserDTO>>>() {},
     *     caffeineCache);
     *
     * // 获取 Map 类型缓存
     * Map<String, List<UserDTO>> mapData = CacheUtil.getL2Cache(redisService,
     *     "map:data",
     *     new TypeReference<Map<String, List<UserDTO>>>() {},
     *     caffeineCache);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果只需要简单类型，使用 {@link #getL2Cache(RedisService, String, Class, Cache)}</li>
     *     <li>valueTypeRef 需要使用匿名内部类创建 TypeReference 实例</li>
     *     <li>其他注意事项同 {@link #getL2Cache(RedisService, String, Class, Cache)}</li>
     * </ul>
     *
     * @param redisService  Redis 缓存服务，不能为 null
     * @param key           缓存的键，不能为 null
     * @param valueTypeRef  值类型引用（TypeReference），不能为 null
     * @param caffeineCache 本地缓存（Caffeine），不能为 null
     * @param <T>           缓存值的类型
     * @return 缓存值，如果未命中则返回 null
     * @see #getL2Cache(RedisService, String, Class, Cache)
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T getL2Cache(RedisService redisService, String key, TypeReference<T> valueTypeRef, Cache<String, Object> caffeineCache) {
        // 先从一级缓存中拿取数据, 如果有就返回
        T ifPresent = (T) caffeineCache.getIfPresent(key);
        if (ifPresent != null) {
            return ifPresent;
        }
        // 如果没查到，就查二级缓存
        ifPresent = redisService.getCacheObject(key, valueTypeRef);
        if (ifPresent != null) {
            // 如果查到了，就存储到一级缓存中再返回
            setL2Cache(key, ifPresent, caffeineCache);
            return ifPresent;
        }
        // 如果还没查到，就返回空，让用户去查询数据库
        return null;
    }

    /**
     * 从二级缓存中获取数据（使用布隆过滤器优化）
     * <p>
     * 在查询缓存前先使用布隆过滤器判断 key 是否存在，如果布隆过滤器确定不存在，直接返回 null，
     * 避免无效的 Redis 查询，提高性能。适用于大量 key 查询的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用布隆过滤器优化查询
     * UserDTO user = CacheUtil.getL2Cache(redisService, bloomFilterService,
     *     "user:123", UserDTO.class, caffeineCache);
     * if (user == null) {
     *     // 缓存未命中，查询数据库
     *     user = userService.findById(123L);
     *     // 写入缓存（同时写入布隆过滤器）
     *     CacheUtil.setL2Cache(redisService, bloomFilterService,
     *         "user:123", user, caffeineCache, 3600L, TimeUnit.SECONDS);
     * }
     * }</pre>
     * <p>
     * <b>查询流程：</b>
     * <ol>
     *     <li>先检查布隆过滤器，如果确定不存在则直接返回 null（避免 Redis 查询）</li>
     *     <li>如果布隆过滤器判断可能存在，执行正常的二级缓存查询流程</li>
     *     <li>查询流程：一级缓存 → 二级缓存 → 回填一级缓存</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>布隆过滤器可能出现误判（判断存在但实际不存在），但不会漏判（判断不存在则一定不存在）</li>
     *     <li>使用布隆过滤器可以显著减少无效的 Redis 查询，提高性能</li>
     *     <li>如果布隆过滤器判断不存在，直接返回 null，不会查询 Redis</li>
     *     <li>写入缓存时需要使用带布隆过滤器的方法，确保布隆过滤器同步更新</li>
     *     <li>其他注意事项同 {@link #getL2Cache(RedisService, String, Class, Cache)}</li>
     * </ul>
     *
     * @param redisService       Redis 缓存服务，不能为 null
     * @param bloomFilterService 布隆过滤器服务，不能为 null
     * @param key                缓存的键，不能为 null
     * @param clazz              值类型（Class），不能为 null
     * @param caffeineCache      本地缓存（Caffeine），不能为 null
     * @param <T>                缓存值的类型
     * @return 缓存值，如果未命中或布隆过滤器判断不存在则返回 null
     * @see #getL2Cache(RedisService, String, Class, Cache)
     * @see #setL2Cache(RedisService, BloomFilterService, String, Object, Cache, Long, TimeUnit)
     */
    public static <T> T getL2Cache(RedisService redisService, BloomFilterService bloomFilterService,
                                   String key, Class<T> clazz, Cache<String, Object> caffeineCache) {
        // 先检查布隆过滤器，如果确定不存在则直接返回null
        if (!bloomFilterService.mightContain(key)) {
            return null;
        }
        // 再调用上面的逻辑从一级和二级缓存中获取数据
        return getL2Cache(redisService, key, clazz, caffeineCache);
    }

    /**
     * 从二级缓存中获取数据（使用布隆过滤器优化，支持复杂泛型嵌套）
     * <p>
     * 结合布隆过滤器和复杂泛型支持，先通过布隆过滤器判断 key 是否存在，再执行二级缓存查询。
     * 适用于需要复杂泛型类型且需要性能优化的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用布隆过滤器获取复杂泛型缓存
     * List<Map<String, UserDTO>> complexData = CacheUtil.getL2Cache(redisService, bloomFilterService,
     *     "complex:data",
     *     new TypeReference<List<Map<String, UserDTO>>>() {},
     *     caffeineCache);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>结合了布隆过滤器优化和复杂泛型支持</li>
     *     <li>其他注意事项同 {@link #getL2Cache(RedisService, BloomFilterService, String, Class, Cache)} 和
     *         {@link #getL2Cache(RedisService, String, TypeReference, Cache)}</li>
     * </ul>
     *
     * @param redisService       Redis 缓存服务，不能为 null
     * @param bloomFilterService 布隆过滤器服务，不能为 null
     * @param key                缓存的键，不能为 null
     * @param valueTypeRef       值类型引用（TypeReference），不能为 null
     * @param caffeineCache      本地缓存（Caffeine），不能为 null
     * @param <T>                缓存值的类型
     * @return 缓存值，如果未命中或布隆过滤器判断不存在则返回 null
     * @see #getL2Cache(RedisService, BloomFilterService, String, Class, Cache)
     * @see #getL2Cache(RedisService, String, TypeReference, Cache)
     */
    public static <T> T getL2Cache(RedisService redisService, BloomFilterService bloomFilterService,
                                   String key, TypeReference<T> valueTypeRef, Cache<String, Object> caffeineCache) {
        // 先检查布隆过滤器，如果确定不存在则直接返回null
        if (!bloomFilterService.mightContain(key)) {
            return null;
        }
        // 再调用上面的逻辑从一级和二级缓存中获取数据
        return getL2Cache(redisService, key, valueTypeRef, caffeineCache);
    }

    /**
     * 存储到一级缓存（本地缓存）中
     * <p>
     * 将数据存储到本地缓存（Caffeine）中，不设置过期时间，使用 Caffeine 的默认过期策略。
     * 通常用于将从二级缓存查到的数据回填到一级缓存。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储到本地缓存
     * CacheUtil.setL2Cache("user:123", user, caffeineCache);
     *
     * // 通常用于回填（从 Redis 查到后回填到本地缓存）
     * UserDTO user = redisService.getCacheObject("user:123", UserDTO.class);
     * if (user != null) {
     *     CacheUtil.setL2Cache("user:123", user, caffeineCache);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只写入一级缓存（本地缓存），不写入二级缓存（Redis）</li>
     *     <li>不设置过期时间，使用 Caffeine 的默认过期策略</li>
     *     <li>如果 key 或 value 为 null，可能抛出异常</li>
     *     <li>如果需要同时写入二级缓存，使用 {@link #setL2Cache(RedisService, String, Object, Cache, Long, TimeUnit)}</li>
     * </ul>
     *
     * @param key           缓存的键，不能为 null
     * @param value         缓存的值，可以为 null
     * @param caffeineCache 本地缓存（Caffeine），不能为 null
     * @param <T>           缓存值的类型
     * @see #setL2Cache(RedisService, String, Object, Cache, Long, TimeUnit)
     */
    public static <T> void setL2Cache(String key, T value, Cache<String, Object> caffeineCache) {
        caffeineCache.put(key, value);
    }

    /**
     * 存储到二级缓存（同时写入一级和二级缓存）
     * <p>
     * 将数据同时存储到 Redis 缓存（二级缓存）和本地缓存（一级缓存）中。
     * Redis 缓存会设置过期时间，本地缓存使用 Caffeine 的默认过期策略。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储用户数据到二级缓存（1小时过期）
     * UserDTO user = userService.findById(123L);
     * CacheUtil.setL2Cache(redisService, "user:123", user, caffeineCache,
     *     3600L, TimeUnit.SECONDS);
     *
     * // 存储配置数据（30分钟过期）
     * ConfigDTO config = new ConfigDTO();
     * CacheUtil.setL2Cache(redisService, "config:system", config, caffeineCache,
     *     1800L, TimeUnit.SECONDS);
     * }</pre>
     * <p>
     * <b>存储流程：</b>
     * <ol>
     *     <li>先存储到二级缓存（Redis），设置过期时间</li>
     *     <li>再存储到一级缓存（本地缓存），使用默认过期策略</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>同时写入一级和二级缓存，确保数据一致性</li>
     *     <li>Redis 缓存会设置过期时间，本地缓存使用默认策略</li>
     *     <li>如果 key 或 value 为 null，可能抛出异常</li>
     *     <li>timeout 和 timeUnit 用于设置 Redis 缓存的过期时间</li>
     *     <li>如果需要使用布隆过滤器，使用带 BloomFilterService 参数的重载方法</li>
     * </ul>
     *
     * @param redisService  Redis 缓存服务，不能为 null
     * @param key           缓存的键，不能为 null
     * @param value         缓存的值，可以为 null
     * @param caffeineCache 本地缓存（Caffeine），不能为 null
     * @param timeout       缓存的过期时间（用于 Redis），不能为 null
     * @param timeUnit      时间单位（用于 Redis），不能为 null
     * @param <T>           缓存值的类型
     * @see #setL2Cache(RedisService, BloomFilterService, String, Object, Cache, Long, TimeUnit)
     */
    public static <T> void setL2Cache(
            RedisService redisService, String key, T value, Cache<String, Object> caffeineCache,
            Long timeout, TimeUnit timeUnit
    ) {
        redisService.setCacheObject(key, value, timeout, timeUnit);
        setL2Cache(key, value, caffeineCache);
    }

    /**
     * 存储到二级缓存（同时写入 Redis、本地缓存和布隆过滤器）
     * <p>
     * 将数据存储到 Redis 缓存、本地缓存和布隆过滤器中。
     * 这是最完整的存储方法，适用于需要布隆过滤器优化的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储用户数据（同时写入三级存储）
     * UserDTO user = userService.findById(123L);
     * CacheUtil.setL2Cache(redisService, bloomFilterService,
     *     "user:123", user, caffeineCache, 3600L, TimeUnit.SECONDS);
     *
     * // 存储列表数据
     * List<UserDTO> userList = userService.findAll();
     * CacheUtil.setL2Cache(redisService, bloomFilterService,
     *     "user:list", userList, caffeineCache, 1800L, TimeUnit.SECONDS);
     * }</pre>
     * <p>
     * <b>存储流程：</b>
     * <ol>
     *     <li>存储到二级缓存（Redis），设置过期时间</li>
     *     <li>存储到一级缓存（本地缓存），使用默认过期策略</li>
     *     <li>添加到布隆过滤器，用于快速判断 key 是否存在</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>同时写入 Redis、本地缓存和布隆过滤器，确保数据一致性</li>
     *     <li>布隆过滤器用于优化查询性能，避免无效的 Redis 查询</li>
     *     <li>写入布隆过滤器后，查询时可以使用带布隆过滤器的 getL2Cache 方法</li>
     *     <li>如果 key 或 value 为 null，可能抛出异常</li>
     *     <li>timeout 和 timeUnit 用于设置 Redis 缓存的过期时间</li>
     *     <li>如果不需要布隆过滤器，使用 {@link #setL2Cache(RedisService, String, Object, Cache, Long, TimeUnit)}</li>
     * </ul>
     *
     * @param redisService       Redis 缓存服务，不能为 null
     * @param bloomFilterService 布隆过滤器服务，不能为 null
     * @param key                缓存的键，不能为 null
     * @param value              缓存的值，可以为 null
     * @param caffeineCache      本地缓存（Caffeine），不能为 null
     * @param timeout            缓存的过期时间（用于 Redis），不能为 null
     * @param timeUnit           时间单位（用于 Redis），不能为 null
     * @param <T>                缓存值的类型
     * @see #setL2Cache(RedisService, String, Object, Cache, Long, TimeUnit)
     * @see #getL2Cache(RedisService, BloomFilterService, String, Class, Cache)
     */
    public static <T> void setL2Cache(
            RedisService redisService, BloomFilterService bloomFilterService,
            String key, T value, Cache<String, Object> caffeineCache,
            Long timeout, TimeUnit timeUnit
    ) {
        // 存储到 Redis 和本地缓存
        redisService.setCacheObject(key, value, timeout, timeUnit);
        setL2Cache(key, value, caffeineCache);

        // 添加到布隆过滤器
        bloomFilterService.put(key);
    }

    /**
     * 从一级缓存（本地缓存）中删除数据
     * <p>
     * 从本地缓存（Caffeine）中删除指定 key 的数据。
     * 只删除一级缓存，不影响二级缓存（Redis）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除本地缓存
     * CacheUtil.delL1Cache("user:123", caffeineCache);
     *
     * // 通常用于缓存失效场景
     * userService.updateUser(user);
     * CacheUtil.delL1Cache("user:" + user.getId(), caffeineCache);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只删除一级缓存（本地缓存），不删除二级缓存（Redis）</li>
     *     <li>如果 key 为 null，可能抛出异常</li>
     *     <li>如果 key 不存在，操作会静默成功（不会抛出异常）</li>
     *     <li>如果需要同时删除一级和二级缓存，使用 {@link #delL2Cache(String, Cache, RedisService)}</li>
     * </ul>
     *
     * @param key           缓存的键，不能为 null
     * @param caffeineCache 本地缓存（Caffeine），不能为 null
     * @see #delL2Cache(String, Cache, RedisService)
     */
    public static void delL1Cache(String key, Cache<String, Object> caffeineCache) {
        caffeineCache.invalidate(key);
    }

    /**
     * 从二级缓存（Redis缓存）中删除数据
     * <p>
     * 从 Redis 缓存中删除指定 key 的数据。
     * 只删除二级缓存，不影响一级缓存（本地缓存）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除 Redis 缓存
     * CacheUtil.delL2Cache("user:123", redisService);
     *
     * // 通常用于缓存失效场景
     * userService.deleteUser(123L);
     * CacheUtil.delL2Cache("user:123", redisService);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只删除二级缓存（Redis），不删除一级缓存（本地缓存）</li>
     *     <li>如果 key 为 null，可能抛出异常</li>
     *     <li>如果 key 不存在，操作会静默成功（不会抛出异常）</li>
     *     <li>布隆过滤器不会自动删除，需要手动处理（如果需要）</li>
     *     <li>如果需要同时删除一级和二级缓存，使用 {@link #delL2Cache(String, Cache, RedisService)}</li>
     * </ul>
     *
     * @param key          缓存的键，不能为 null
     * @param redisService Redis 缓存服务，不能为 null
     * @see #delL2Cache(String, Cache, RedisService)
     */
    public static void delL2Cache(String key, RedisService redisService) {
        redisService.deleteObject(key);
    }

    /**
     * 从二级缓存中删除数据（同时删除一级和二级缓存）
     * <p>
     * 同时从本地缓存（Caffeine）和 Redis 缓存中删除指定 key 的数据。
     * 确保两级缓存的数据一致性，适用于数据更新或删除的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除二级缓存（同时删除一级和二级）
     * CacheUtil.delL2Cache("user:123", caffeineCache, redisService);
     *
     * // 在数据更新时删除缓存
     * userService.updateUser(user);
     * CacheUtil.delL2Cache("user:" + user.getId(), caffeineCache, redisService);
     *
     * // 在数据删除时删除缓存
     * userService.deleteUser(123L);
     * CacheUtil.delL2Cache("user:123", caffeineCache, redisService);
     * }</pre>
     * <p>
     * <b>删除流程：</b>
     * <ol>
     *     <li>先从一级缓存（本地缓存）中删除</li>
     *     <li>再从二级缓存（Redis）中删除</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>同时删除一级和二级缓存，确保数据一致性</li>
     *     <li>如果 key 为 null，可能抛出异常</li>
     *     <li>如果 key 不存在，操作会静默成功（不会抛出异常）</li>
     *     <li>布隆过滤器不会自动删除，如果需要可以手动处理</li>
     *     <li>如果只需要删除一级缓存，使用 {@link #delL1Cache(String, Cache)}</li>
     *     <li>如果只需要删除二级缓存，使用 {@link #delL2Cache(String, RedisService)}</li>
     * </ul>
     *
     * @param key           缓存的键，不能为 null
     * @param caffeineCache 本地缓存（Caffeine），不能为 null
     * @param redisService  Redis 缓存服务，不能为 null
     * @see #delL1Cache(String, Cache)
     * @see #delL2Cache(String, RedisService)
     */
    public static void delL2Cache(String key, Cache<String, Object> caffeineCache, RedisService redisService) {
        // 从一级缓存中删除
        delL1Cache(key, caffeineCache);
        // 从二级缓存中删除
        delL2Cache(key, redisService);
    }
}