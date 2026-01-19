package com.zmbdp.common.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Redis 操作服务类
 * <p>
 * 提供 Redis 的完整操作封装，支持 Redis 的所有数据结构类型：
 * <ul>
 *     <li><b>String</b>：字符串类型，支持对象序列化存储</li>
 *     <li><b>List</b>：列表类型，支持有序列表操作</li>
 *     <li><b>Set</b>：集合类型，支持无序集合操作</li>
 *     <li><b>ZSet</b>：有序集合类型，支持按分数排序</li>
 *     <li><b>Hash</b>：哈希类型，支持字段-值映射</li>
 * </ul>
 * </p>
 * <p>
 * 特性说明：
 * <ul>
 *     <li>自动序列化/反序列化：使用 GenericJackson2JsonRedisSerializer 自动处理对象序列化</li>
 *     <li>支持泛型嵌套：通过 TypeReference 支持复杂的泛型类型（如 List&lt;Map&lt;String, User&gt;&gt;）</li>
 *     <li>异常安全：所有方法都包含异常处理，不会抛出异常，失败时返回默认值或 false</li>
 *     <li>事务支持：提供 Redis 事务执行方法，保证操作的原子性</li>
 *     <li>Lua 脚本：支持通过 Lua 脚本实现复杂的原子操作</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * // 1. 基本对象存储
 * redisService.setCacheObject("user:1", user, 3600, TimeUnit.SECONDS);
 * User cachedUser = redisService.getCacheObject("user:1", User.class);
 *
 * // 2. 复杂泛型类型存储
 * TypeReference&lt;List&lt;User&gt;&gt; typeRef = new TypeReference&lt;List&lt;User&gt;&gt;() {};
 * redisService.setCacheList("users", userList);
 * List&lt;User&gt; users = redisService.getCacheList("users", typeRef);
 *
 * // 3. 原子操作
 * Long count = redisService.incr("counter", 1);
 * Boolean success = redisService.setCacheObjectIfAbsent("lock:key", "value", 60, TimeUnit.SECONDS);
 *
 * // 4. Hash 操作
 * redisService.setCacheMapValue("user:1:info", "name", "张三");
 * String name = redisService.getCacheMapValue("user:1:info", "name");
 * </pre>
 * </p>
 * <p>
 * 注意事项：
 * <ul>
 *     <li>所有方法都是线程安全的，可以在多线程环境下使用</li>
 *     <li>存储对象时会自动序列化为 JSON，读取时会自动反序列化</li>
 *     <li>对于复杂泛型类型，建议使用 TypeReference 而不是 Class</li>
 *     <li>keys() 方法在生产环境慎用，可能阻塞 Redis 服务器</li>
 *     <li>事务操作失败时会抛出异常，需要调用方处理</li>
 * </ul>
 * </p>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class RedisService {

    /**
     * RedisTemplate
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /*=============================================    通用方法    =============================================*/

    /**
     * 为指定的键设置过期时间（秒）
     * <p>
     * 如果键不存在，操作会失败。如果键已经有过期时间，会更新为新的过期时间。
     * </p>
     *
     * @param key     Redis 键，不能为 null
     * @param timeout 过期时间（秒），必须大于 0
     * @return true - 设置成功；false - 设置失败（键不存在或其他错误）
     */
    public Boolean expire(final String key, final long timeout) {
        try {
            return redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("RedisService.expire set ttl error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 为指定的键设置过期时间（支持自定义时间单位）
     * <p>
     * 如果键不存在，操作会失败。如果键已经有过期时间，会更新为新的过期时间。
     * 支持的时间单位：SECONDS（秒）、MINUTES（分钟）、HOURS（小时）、DAYS（天）等。
     * </p>
     *
     * @param key      Redis 键，不能为 null
     * @param timeout  过期时间，必须大于 0
     * @param timeUnit 时间单位，不能为 null
     * @return true - 设置成功；false - 设置失败（键不存在或其他错误）
     */
    public Boolean expire(final String key, final long timeout, final TimeUnit timeUnit) {
        try {
            return redisTemplate.expire(key, timeout, timeUnit);
        } catch (Exception e) {
            log.warn("RedisService.expire set custom ttl error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定键的剩余过期时间（秒）
     * <p>
     * 返回值说明：
     * <ul>
     *     <li>正数：键的剩余过期时间（秒）</li>
     *     <li>-1：键存在但没有设置过期时间（永久有效）</li>
     *     <li>-2：键不存在</li>
     * </ul>
     * </p>
     *
     * @param key Redis 键，不能为 null
     * @return 剩余过期时间（秒），-1 表示永久有效，-2 表示键不存在
     */
    public Long getExpire(final String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.warn("RedisService.getExpire get ttl error: {}", e.getMessage());
            return -2L;
        }
    }

    /**
     * 判断指定的键是否存在
     * <p>
     * 该方法会检查 Redis 中是否存在指定的键，无论键是否设置了过期时间。
     * </p>
     *
     * @param key Redis 键，不能为 null
     * @return true - 键存在；false - 键不存在或发生错误
     */
    public Boolean hasKey(final String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("RedisService.hasKey error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据模式匹配查找 Redis 中所有匹配的键
     * <p>
     * 支持的模式：
     * <ul>
     *     <li>{@code *}：匹配任意多个字符，如 {@code user:*}</li>
     *     <li>{@code ?}：匹配单个字符，如 {@code user:?}</li>
     *     <li>{@code [abc]}：匹配指定字符中的一个，如 {@code user:[123]}</li>
     * </ul>
     * <b>注意：</b>在生产环境中慎用此方法，如果键数量很大可能会阻塞 Redis 服务器。
     * 建议使用 SCAN 命令的迭代方式（本方法未实现）。
     * </p>
     *
     * @param pattern 键的模式，支持通配符，不能为 null
     * @return 匹配的键集合，如果没有匹配的键或发生错误，返回空集合
     */
    public Collection<String> keys(final String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.warn("RedisService.keys error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 重命名 Redis 键
     * <p>
     * 如果新键名已存在，会被覆盖。如果旧键不存在，操作会失败（但不会抛出异常）。
     * </p>
     *
     * @param oldKey 原键名，不能为 null
     * @param newKey 新键名，不能为 null，如果已存在会被覆盖
     */
    public void renameKey(String oldKey, String newKey) {
        try {
            redisTemplate.rename(oldKey, newKey);
        } catch (Exception e) {
            log.warn("RedisService.renameKey error: {}", e.getMessage());
        }
    }

    /**
     * 删除指定的键及其关联的值
     * <p>
     * 如果键不存在，返回 false。删除操作是原子性的。
     * </p>
     *
     * @param key 要删除的 Redis 键，不能为 null
     * @return true - 删除成功；false - 删除失败（键不存在或其他错误）
     */
    public Boolean deleteObject(final String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("RedisService.deleteObject error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 批量删除多个键
     * <p>
     * 删除集合中所有指定的键。如果某个键不存在，会被忽略。
     * 返回实际删除的键数量。
     * </p>
     *
     * @param collection 要删除的键集合，不能为 null
     * @return 实际删除的键数量，如果发生错误返回 0
     */
    public Long deleteObject(final Collection collection) {
        try {
            Long delete = redisTemplate.delete(collection);
            return delete == null ? 0 : delete;
        } catch (Exception e) {
            log.warn("RedisService.deleteObject multiple error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 在 Redis 事务中执行多个操作
     * <p>
     * 所有操作会在一个事务中执行，保证原子性。如果事务执行失败，会抛出异常。
     * 事务执行流程：MULTI → 执行操作 → EXEC 或 DISCARD（发生异常时）。
     * </p>
     * <p>
     * 使用示例：
     * <pre>
     * List&lt;Object&gt; results = redisService.executeInTransaction(operations -&gt; {
     *     operations.opsForValue().set("key1", "value1");
     *     operations.opsForValue().set("key2", "value2");
     * });
     * </pre>
     * </p>
     *
     * @param action 事务操作的回调函数，通过 RedisOperations 执行 Redis 命令，不能为 null
     * @return 事务中每个命令的执行结果列表
     * @throws RuntimeException 如果事务执行失败（命令类型错误或队列为空）
     */
    @SuppressWarnings({"rawtypes", "unchecked"}) // 忽略警告
    public List<Object> executeInTransaction(Consumer<RedisOperations> action) {
        try {
            return (List<Object>) redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi(); // 开启事务
                    try {
                        ((Consumer) action).accept(operations);
                    } catch (Exception e) {
                        operations.discard(); // 回滚事务
                        throw e;
                    }
                    // exec() 提交事务，若命令队列中有类型错误或执行失败，exec() 会返回 null
                    List<Object> results = operations.exec();
                    if (results == null) {
                        throw new RuntimeException("Redis事务执行失败，可能命令类型错误或队列为空");
                    }
                    return results;
                }
            });
        } catch (Exception e) {
            log.error("Redis 事务执行失败", e);
            throw e;
        }
    }

    /*=============================================    String    =============================================*/

    /**
     * 将对象存储到 Redis（不设置过期时间）
     * <p>
     * 对象会被自动序列化为 JSON 格式存储。如果键已存在，会被覆盖。
     * 存储的对象会永久有效，直到手动删除或 Redis 重启（取决于持久化配置）。
     * </p>
     *
     * @param key   缓存键，不能为 null
     * @param value 缓存值，可以是任意类型，会自动序列化
     * @param <T>   值的类型
     */
    public <T> void setCacheObject(final String key, final T value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("RedisService.setCacheObject set cache error: {}", e.getMessage());
        }
    }

    /**
     * 将对象存储到 Redis 并设置过期时间
     * <p>
     * 对象会被自动序列化为 JSON 格式存储。如果键已存在，会被覆盖。
     * 到达过期时间后，键会自动从 Redis 中删除。
     * </p>
     *
     * @param key      缓存键，不能为 null
     * @param value    缓存值，可以是任意类型，会自动序列化
     * @param timeout  过期时间，必须大于 0
     * @param timeUnit 时间单位，不能为 null（如 TimeUnit.SECONDS、TimeUnit.MINUTES 等）
     * @param <T>      值的类型
     */
    public <T> void setCacheObject(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.warn("RedisService.setCacheObject set cache and ttl error: {}", e.getMessage());
        }
    }

    /**
     * 仅在键不存在时设置缓存（不设置过期时间）
     * <p>
     * 这是一个原子操作，相当于 Redis 的 SETNX 命令。常用于实现分布式锁。
     * 如果键已存在，操作会失败，不会覆盖原有值。
     * </p>
     *
     * @param key   缓存键，不能为 null
     * @param value 缓存值，可以是任意类型，会自动序列化
     * @param <T>   值的类型
     * @return true - 设置成功（键不存在）；false - 设置失败（键已存在或发生错误）
     */
    public <T> Boolean setCacheObjectIfAbsent(final String key, final T value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e) {
            log.warn("RedisService.setCacheObjectIfAbsent error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 仅在键不存在时设置缓存并指定过期时间
     * <p>
     * 这是一个原子操作，相当于 Redis 的 SETNX 命令加上过期时间。
     * 常用于实现带过期时间的分布式锁。如果键已存在，操作会失败。
     * </p>
     *
     * @param key      缓存键，不能为 null
     * @param value    缓存值，可以是任意类型，会自动序列化
     * @param timeout  过期时间，必须大于 0
     * @param timeUnit 时间单位，不能为 null
     * @param <T>      值的类型
     * @return true - 设置成功（键不存在）；false - 设置失败（键已存在或发生错误）
     */
    public <T> Boolean setCacheObjectIfAbsent(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.warn("RedisService.setCacheObjectIfAbsent set cache and ttl error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Redis 获取缓存对象
     * <p>
     * 如果键不存在，返回 null。如果对象类型匹配，直接返回；否则会进行 JSON 转换。
     * 适用于简单的对象类型（非泛型嵌套）。
     * </p>
     * <p>
     * 对于复杂泛型类型（如 List&lt;User&gt;），请使用 {@link #getCacheObject(String, TypeReference)} 方法。
     * </p>
     *
     * @param key   缓存键，不能为 null
     * @param clazz 缓存对象的类型，不能为 null
     * @param <T>   对象的类型
     * @return 缓存的对象，如果键不存在或发生错误，返回 null
     */
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        try {
            Object o = redisTemplate.opsForValue().get(key);
            if (o == null) {
                return null;
            }

            // 如果对象本身就是目标类型，直接返回
            if (clazz.isInstance(o)) {
                return clazz.cast(o);
            }

            // 缓存对象先转换成 json
            String jsonStr = JsonUtil.classToJson(o);
            // 再转换成对象
            return JsonUtil.jsonToClass(jsonStr, clazz);
        } catch (Exception e) {
            log.warn("RedisService.getCacheObject get Class error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Redis 获取复杂泛型嵌套的缓存对象
     * <p>
     * 使用 TypeReference 可以正确处理泛型类型，如 List&lt;User&gt;、Map&lt;String, List&lt;Order&gt;&gt; 等。
     * 如果键不存在，返回 null。
     * </p>
     * <p>
     * 使用示例：
     * <pre>
     * TypeReference&lt;List&lt;User&gt;&gt; typeRef = new TypeReference&lt;List&lt;User&gt;&gt;() {};
     * List&lt;User&gt; users = redisService.getCacheObject("users", typeRef);
     * </pre>
     * </p>
     *
     * @param key          缓存键，不能为 null
     * @param valueTypeRef 类型引用，用于指定泛型类型，不能为 null
     * @param <T>          对象的类型
     * @return 缓存的对象，如果键不存在或发生错误，返回 null
     */
    public <T> T getCacheObject(final String key, TypeReference<T> valueTypeRef) {
        try {
            Object o = redisTemplate.opsForValue().get(key);
            if (o == null) {
                return null;
            }
            // 缓存对象先转换成 json
            String jsonStr = JsonUtil.classToJson(o);
            // 再转换成对象
            return JsonUtil.jsonToClass(jsonStr, valueTypeRef);
        } catch (Exception e) {
            log.warn("RedisService.getCacheObject get complex Class error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 对指定键的值进行原子递增（+1）
     * <p>
     * 这是一个原子操作，线程安全。如果键不存在，会先初始化为 0 再递增。
     * 键的值必须是数字类型（整数），否则会失败。
     * </p>
     *
     * @param key Redis 键，不能为 null 或空字符串
     * @return 递增后的值，如果失败返回 -1
     */
    public Long incr(final String key) {
        if (StringUtil.isEmpty(key)) {
            return -1L;
        }
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("RedisService.incr error: key={}", key, e);
            return -1L;
        }
    }

    /**
     * 对指定键的值进行原子递增（指定增量）
     * <p>
     * 这是一个原子操作，线程安全。如果键不存在，会先初始化为 0 再递增。
     * 键的值必须是数字类型（整数），否则会失败。
     * </p>
     *
     * @param key   Redis 键，不能为 null 或空字符串
     * @param delta 增加的数值，可以为负数（相当于递减）
     * @return 递增后的值，如果失败返回 -1
     */
    public Long incr(final String key, final long delta) {
        if (StringUtil.isEmpty(key)) {
            return -1L;
        }
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.warn("RedisService.incr error: key={}", key, e);
            return -1L;
        }
    }

    /**
     * 对指定键的值进行原子递减（-1）
     * <p>
     * 这是一个原子操作，线程安全。如果键不存在，会先初始化为 0 再递减。
     * 键的值必须是数字类型（整数），否则会失败。
     * </p>
     *
     * @param key Redis 键，不能为 null 或空字符串
     * @return 递减后的值，如果失败返回 -1
     */
    public Long decr(final String key) {
        if (StringUtil.isEmpty(key)) {
            return -1L;
        }
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.warn("RedisService.decr error: {}", key, e);
            return -1L;
        }
    }

    /**
     * 对指定键的值进行原子递减（指定减量）
     * <p>
     * 这是一个原子操作，线程安全。如果键不存在，会先初始化为 0 再递减。
     * 键的值必须是数字类型（整数），否则会失败。
     * </p>
     *
     * @param key   Redis 键，不能为 null 或空字符串
     * @param delta 减少的数值，必须大于 0
     * @return 递减后的值，如果失败返回 -1
     */
    public Long decr(final String key, final long delta) {
        if (StringUtil.isEmpty(key)) {
            return -1L;
        }
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.warn("RedisService.decr error: {}", key, e);
            return -1L;
        }
    }

    /*=============================================    List    =============================================*/

    /**
     * 将列表数据按原有顺序存入 Redis（从右侧批量插入）
     * <p>
     * 如果键不存在，会创建新的列表。如果键已存在，会将新元素追加到列表末尾。
     * 列表保持插入顺序，支持重复元素。
     * </p>
     *
     * @param key      缓存键，不能为 null
     * @param dataList 要存储的列表数据，不能为 null
     * @param <T>      元素的类型
     * @return 保存成功返回列表长度，失败返回 -1
     */
    public <T> Long setCacheList(final String key, final List<T> dataList) {
        try {
            Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
            return count == null ? -1 : count;
        } catch (Exception e) {
            log.warn("RedisService.setCacheList set List error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 从列表左侧插入单个元素（头插）
     * <p>
     * 元素会被插入到列表的最前面（索引 0）。如果键不存在，会创建新的列表。
     * </p>
     *
     * @param key  缓存键，不能为 null
     * @param data 要插入的元素，不能为 null
     * @param <T>  元素的类型
     * @return 插入成功返回列表长度，失败返回 -1
     */
    public <T> Long leftPushForList(final String key, final T data) {
        try {
            Long count = redisTemplate.opsForList().leftPush(key, data);
            return count == null ? -1 : count;
        } catch (Exception e) {
            log.warn("RedisService.leftPushForList left push redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 从列表右侧插入单个元素（尾插）
     * <p>
     * 元素会被插入到列表的最后面。如果键不存在，会创建新的列表。
     * </p>
     *
     * @param key   缓存键，不能为 null
     * @param value 要插入的元素，不能为 null
     * @param <T>   元素的类型
     * @return 插入成功返回列表长度，失败返回 -1
     */
    public <T> Long rightPushForList(final String key, final T value) {
        try {
            Long count = redisTemplate.opsForList().rightPush(key, value);
            return count == null ? -1 : count;
        } catch (Exception e) {
            log.warn("RedisService.rightPushForList right push redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 删除左侧第一个数据 （头删）
     *
     * @param key key
     */
    public void leftPopForList(final String key) {
        try {
            redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.warn("RedisService.leftPopForList left pop redis error: {}", e.getMessage());
        }
    }

    /**
     * 删除左侧 k 个数据 （头删）
     *
     * @param key key
     * @param k   删除元素的数量
     */
    public void leftPopForList(final String key, final long k) {
        try {
            redisTemplate.opsForList().leftPop(key, k);
        } catch (Exception e) {
            log.warn("RedisService.leftPopForList left pop k redis error: {}", e.getMessage());
        }
    }

    /**
     * 删除右侧第一个数据 （尾删）
     *
     * @param key key
     */
    public void rightPopForList(final String key) {
        try {
            redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            log.warn("RedisService.rightPopForList right pop redis error: {}", e.getMessage());
        }
    }

    /**
     * 删除右侧 k 个数据 （尾删）
     *
     * @param key key
     * @param k   删除元素的数量
     */
    public void rightPopForList(final String key, final long k) {
        try {
            redisTemplate.opsForList().rightPop(key, k);
        } catch (Exception e) {
            log.warn("RedisService.rightPopForList right pop k redis error: {}", e.getMessage());
        }
    }

    /**
     * 移除 List 第一个匹配的元素（从左到右）
     *
     * @param key   key
     * @param value 值
     * @param <T>   值类型
     * @return 移除的元素数量
     */
    public <T> Long removeLeftForList(final String key, final T value) {
        try {
            // count: 等于正数的时候从左往右删
            Long remove = redisTemplate.opsForList().remove(key, 1L, value);
            return remove == null ? -1 : remove;
        } catch (Exception e) {
            log.warn("RedisService.removeLeftForList remove left redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 移除 List 前 k 个匹配的元素（从左到右）
     *
     * @param key   key
     * @param value 值
     * @param k     删除元素的数量
     * @param <T>   值类型
     * @return 移除的元素数量
     */
    public <T> Long removeLeftForList(final String key, final T value, final long k) {
        try {
            // count: 等于正数的时候从左往右删
            Long remove = redisTemplate.opsForList().remove(key, k, value);
            return remove == null ? -1 : remove;
        } catch (Exception e) {
            log.warn("RedisService.removeLeftForList remove k left redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 移除 List 第一个匹配的元素（从右到左）
     *
     * @param key   key
     * @param value 值
     * @param <T>   值类型
     * @return 移除的元素数量
     */
    public <T> Long removeRightForList(final String key, final T value) {
        try {
            // count: 等于负数的时候从右往左删
            Long remove = redisTemplate.opsForList().remove(key, -1L, value);
            return remove == null ? -1 : remove;
        } catch (Exception e) {
            log.warn("RedisService.removeRightForList remove right redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 移除 List 前 k 个匹配的元素（从右到左）
     *
     * @param key   key
     * @param value 值
     * @param k     移除的元素数量
     * @param <T>   值类型
     * @return 移除的元素数量
     */
    public <T> Long removeRightForList(final String key, final T value, final long k) {
        try {
            // count: 等于负数的时候从右往左删
            Long remove = redisTemplate.opsForList().remove(key, -k, value);
            return remove == null ? -1 : remove;
        } catch (Exception e) {
            log.warn("RedisService.removeRightForList remove k right redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 移除 List 中所有匹配的元素
     *
     * @param key   key
     * @param value 值
     * @param <T>   值类型
     * @return 移除的元素数量
     */
    public <T> Long removeAllForList(final String key, final T value) {
        try {
            // count: 等于 0 的时候全部删除
            Long remove = redisTemplate.opsForList().remove(key, 0, value);
            return remove == null ? -1 : remove;
        } catch (Exception e) {
            log.warn("RedisService.removeAllForList remove redis error: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 移除列表中的所有元素
     *
     * @param key key
     */
    public void removeForAllList(final String key) {
        try {
            // 当 start(下标) > end(下标) 时, 删除所有元素
            redisTemplate.opsForList().trim(key, -1, 0);
        } catch (Exception e) {
            log.warn("RedisService.removeForAllList remove redis error: {}", e.getMessage());
        }
    }

    /**
     * 保留指定范围内的元素
     *
     * @param key   key
     * @param start 开始下标 (0 表示第一个下标, 1 表示第二个下标, 2 表示第三个下标, 以此类推)
     * @param end   结束下标 (特殊的: -1 表示最后一个下标，-2 表示倒数第二个下标，以此类推)
     */
    public void retainListRange(final String key, final long start, final long end) {
        try {
            redisTemplate.opsForList().trim(key, start, end);
        } catch (Exception e) {
            log.warn("RedisService.retainListRange redis error: {}", e.getMessage());
        }
    }

    /**
     * 修改指定下标数据
     *
     * @param key      key
     * @param index    下标
     * @param newValue 修改后新值
     * @param <T>      值类型
     */
    public <T> void setElementAtIndex(final String key, int index, T newValue) {
        try {
            redisTemplate.opsForList().set(key, index, newValue);
        } catch (Exception e) {
            log.warn("RedisService.setElementAtIndex set element error: {}", e.getMessage());
        }
    }

    /**
     * 获取完整的列表数据
     * <p>
     * 返回列表中所有元素，保持原有顺序。适用于简单的对象类型（非泛型嵌套）。
     * </p>
     * <p>
     * 对于复杂泛型类型（如 List&lt;Map&lt;String, User&gt;&gt;），请使用 {@link #getCacheList(String, TypeReference)} 方法。
     * </p>
     *
     * @param key   缓存键，不能为 null
     * @param clazz 元素的类型，不能为 null
     * @param <T>   元素的类型
     * @return 列表数据，如果键不存在或发生错误，返回 null
     */
    public <T> List<T> getCacheList(final String key, Class<T> clazz) {
        try {
            List list = redisTemplate.opsForList().range(key, 0, -1);
            return JsonUtil.jsonToList(JsonUtil.classToJson(list), clazz);
        } catch (Exception e) {
            log.warn("RedisService.getCacheList get list error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取完整的列表数据（支持复杂泛型嵌套）
     * <p>
     * 使用 TypeReference 可以正确处理泛型类型，如 List&lt;User&gt;、List&lt;Map&lt;String, Order&gt;&gt; 等。
     * </p>
     * <p>
     * 使用示例：
     * <pre>
     * TypeReference&lt;List&lt;User&gt;&gt; typeRef = new TypeReference&lt;List&lt;User&gt;&gt;() {};
     * List&lt;User&gt; users = redisService.getCacheList("users", typeRef);
     * </pre>
     * </p>
     *
     * @param key           缓存键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           元素的类型
     * @return 列表数据，如果键不存在或发生错误，返回 null
     */
    public <T> List<T> getCacheList(final String key, TypeReference<List<T>> typeReference) {
        try {
            List list = redisTemplate.opsForList().range(key, 0, -1);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(list), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheList get complex list error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据范围获取 List
     *
     * @param key   key
     * @param start 开始下标 (0 表示第一个下标, 1 表示第二个下标, 2 表示第三个下标, 以此类推)
     * @param end   结束下标 (特殊的: -1 表示最后一个下标, -2 倒数第二个下标, -3 倒数第三个下标, 以此类推)
     * @param clazz 类信息
     * @param <T>   类型
     * @return List 列表 (如果 start(下标) > end(下标) 则返回null)
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end, Class<T> clazz) {
        try {
            List range = redisTemplate.opsForList().range(key, start, end);
            return JsonUtil.jsonToList(JsonUtil.classToJson(range), clazz);
        } catch (Exception e) {
            log.warn("RedisService.getCacheListByRange get list by range error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据范围获取 List（支持复杂的泛型嵌套）
     *
     * @param key           key
     * @param start         开始下标 (0 表示第一个下标, 1 表示第二个下标, 2 表示第三个下标, 以此类推)
     * @param end           结束下标 (特殊的: -1 表示最后一个下标, -2 倒数第二个下标, -3 倒数第三个下标, 以此类推)
     * @param typeReference 类型模板
     * @param <T>           类型信息
     * @return List 列表
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end, TypeReference<List<T>> typeReference) {
        try {
            List range = redisTemplate.opsForList().range(key, start, end);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(range), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheListByRange get complex list by range error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取指定列表长度
     *
     * @param key key 信息
     * @return 列表长度
     */
    public long getCacheListSize(final String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.warn("RedisService.getCacheListSize get list size error: {}", e.getMessage());
            return 0L;
        }
    }

    /*=============================================    Set    =============================================*/

    /**
     * Set 添加元素 (批量添加或添加单个元素)
     *
     * @param key    key
     * @param member 元素信息
     * @return 添加的元素个数
     */
    public Long addMember(final String key, final Object... member) {
        try {
            Long add = redisTemplate.opsForSet().add(key, member);
            return add == null ? 0L : add;
        } catch (Exception e) {
            log.warn("RedisService.addMember add member error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 删除元素
     *
     * @param key    key
     * @param member 元素信息
     * @return 删除的元素个数
     */
    public Long deleteMember(final String key, final Object... member) {
        try {
            Long remove = redisTemplate.opsForSet().remove(key, member);
            return remove == null ? 0L : remove;
        } catch (Exception e) {
            log.warn("RedisService.deleteMember delete member error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 检查 Set 中的某个元素是否存在
     *
     * @param key    缓存 key
     * @param member 缓存元素
     * @return 是否存在
     */
    public boolean isMember(final String key, final Object member) {
        try {
            Boolean flag = redisTemplate.opsForSet().isMember(key, member);
            return flag != null && flag;
        } catch (Exception e) {
            log.warn("RedisService.isMember check member error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Set 中所有元素（支持复杂的泛型嵌套）
     *
     * @param key           key
     * @param typeReference 类型模板
     * @param <T>           类型信息
     * @return set数据
     */
    public <T> Set<T> getCacheSet(final String key, TypeReference<Set<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForSet().members(key);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheSet get complex set data error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Set 缓存元素个数
     *
     * @param key 缓存 key
     * @return 缓存元素个数
     */
    public Long getCacheSetSize(final String key) {
        try {
            Long size = redisTemplate.opsForSet().size(key);
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.warn("RedisService.getCacheSetSize get set size error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 获取两个集合的交集（支持复杂泛型嵌套）
     *
     * @param setKey1       第一个集合的 key
     * @param setKey2       第二个集合的 key
     * @param typeReference 类型模板
     * @param <T>           泛型类型
     * @return 交集结果
     */
    public <T> Set<T> intersectToCacheSet(final String setKey1, final String setKey2, TypeReference<Set<T>> typeReference) {
        try {
            Set<?> intersectSet = redisTemplate.opsForSet().intersect(setKey1, setKey2);
            if (intersectSet == null) {
                return Collections.emptySet();
            }
            return JsonUtil.jsonToClass(JsonUtil.classToJson(intersectSet), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.intersect set intersect error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取两个集合的并集（支持复杂泛型嵌套）
     *
     * @param setKey1       第一个集合的 key
     * @param setKey2       第二个集合的 key
     * @param typeReference 类型模板
     * @param <T>           泛型类型
     * @return 并集结果
     */
    public <T> Set<T> unionToCacheSet(final String setKey1, final String setKey2, TypeReference<Set<T>> typeReference) {
        try {
            Set<?> unionSet = redisTemplate.opsForSet().union(setKey1, setKey2);
            if (unionSet == null) {
                return Collections.emptySet();
            }
            return JsonUtil.jsonToClass(JsonUtil.classToJson(unionSet), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.union set union error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取两个集合的差集（支持复杂泛型嵌套）
     *
     * @param setKey1       第一个集合的 key
     * @param setKey2       第二个集合的 key
     * @param typeReference 类型模板
     * @param <T>           泛型类型
     * @return 差集结果
     */
    public <T> Set<T> differenceToCacheSet(final String setKey1, final String setKey2, TypeReference<Set<T>> typeReference) {
        try {
            Set<?> differenceSet = redisTemplate.opsForSet().difference(setKey1, setKey2);
            if (differenceSet == null) {
                return Collections.emptySet();
            }
            return JsonUtil.jsonToClass(JsonUtil.classToJson(differenceSet), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.difference set difference error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将元素从 sourceKey 集合移动到 destinationKey 集合
     *
     * @param sourceKey      源集合 key
     * @param destinationKey 目标集合 key
     * @param member         要移动的元素
     * @return 移动成功返回true，否则返回 false
     */
    public Boolean moveMemberCacheSet(final String sourceKey, final String destinationKey, Object member) {
        try {
            return redisTemplate.opsForSet().move(sourceKey, member, destinationKey);
        } catch (Exception e) {
            log.warn("RedisService.moveMember move member error: {}", e.getMessage());
            return false;
        }
    }

    /*=============================================    ZSet    =============================================*/

    /**
     * 添加元素
     *
     * @param key   key
     * @param value 值
     * @param seqNo 分数
     * @return 添加成功返回 true，否则返回 false
     */
    public Boolean addMemberZSet(final String key, final Object value, final double seqNo) {
        try {
            return redisTemplate.opsForZSet().add(key, value, seqNo);
        } catch (Exception e) {
            log.warn("RedisService.addMemberZSet add member error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除元素
     *
     * @param key   key
     * @param value 值
     * @return 删除数量
     */
    public Long delMemberZSet(final String key, final Object value) {
        try {
            Long remove = redisTemplate.opsForZSet().remove(key, value);
            return remove == null ? 0L : remove;
        } catch (Exception e) {
            log.warn("RedisService.delMemberZSet del member error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 按范围获取元素（升序，支持复杂的泛型嵌套）
     *
     * @param key           缓存 key
     * @param start         起始索引
     * @param end           结束索引
     * @param typeReference 类型模板
     * @param <T>           对象类型
     * @return 缓存对象集合
     */
    public <T> Set<T> getZSetRange(final String key, final long start, final long end, TypeReference<LinkedHashSet<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForZSet().range(key, start, end);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getZSetRange complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有有序集合数据（升序，支持复杂的泛型嵌套）
     *
     * @param key           key 信息
     * @param typeReference 类型模板
     * @param <T>           对象类型
     * @return 有序集合
     */
    public <T> Set<T> getCacheZSet(final String key, TypeReference<LinkedHashSet<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForZSet().range(key, 0, -1);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheZSet error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 按范围获取元素（降序，支持复杂的泛型嵌套）
     *
     * @param key           缓存 key
     * @param start         起始索引
     * @param end           结束索引
     * @param typeReference 类型模板
     * @param <T>           对象类型
     * @return 缓存对象集合
     */
    public <T> Set<T> getZSetRangeDesc(final String key, final long start, final long end, TypeReference<LinkedHashSet<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForZSet().reverseRange(key, start, end);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getZSetRangeDesc complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有有序集合（降序，支持复杂的泛型嵌套）
     *
     * @param key           key 信息
     * @param typeReference 类型模板
     * @param <T>           对象类型信息
     * @return 降序的有序集合
     */
    public <T> Set<T> getCacheZSetDesc(final String key, TypeReference<LinkedHashSet<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheZSetDesc complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取集合大小
     *
     * @param key 键值
     * @return 集合大小
     */
    public Long getZSetSize(final String key) {
        try {
            Long size = redisTemplate.opsForZSet().size(key);
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.warn("RedisService.getZSetSize error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 增加元素的分数，如果元素存在则添加分数，如果不存在就添加元素设置分数
     *
     * @param key    key
     * @param member 元素值
     * @param delta  增加的分数
     * @return 新的分数
     */
    public Double incrementZSetScore(final String key, final Object member, final double delta) {
        try {
            return redisTemplate.opsForZSet().incrementScore(key, member, delta);
        } catch (Exception e) {
            log.warn("RedisService.incrementZSetScore error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取元素的分数
     *
     * @param key   key
     * @param value 元素值
     * @return 分数
     */
    public Double getZSetScore(final String key, final Object value) {
        try {
            return redisTemplate.opsForZSet().score(key, value);
        } catch (Exception e) {
            log.warn("RedisService.getZSetScore error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取元素的排名（升序）
     *
     * @param key    key
     * @param member 元素值
     * @return 排名（从0开始），如果元素不存在返回null
     */
    public Long getZSetRank(final String key, final Object member) {
        try {
            return redisTemplate.opsForZSet().rank(key, member);
        } catch (Exception e) {
            log.warn("RedisService.getZSetRank error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取元素的排名（降序）
     *
     * @param key    key
     * @param member 元素值
     * @return 排名（从0开始），如果元素不存在返回null
     */
    public Long getZSetReverseRank(final String key, final Object member) {
        try {
            return redisTemplate.opsForZSet().reverseRank(key, member);
        } catch (Exception e) {
            log.warn("RedisService.getZSetReverseRank error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 按分数范围获取元素（升序，支持复杂泛型）
     *
     * @param key           key
     * @param minScore      最小分数
     * @param maxScore      最大分数
     * @param typeReference 类型模板
     * @param <T>           泛型类型
     * @return 元素集合
     */
    public <T> Set<T> getZSetRangeByScore(final String key, final double minScore, final double maxScore, TypeReference<LinkedHashSet<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForZSet().rangeByScore(key, minScore, maxScore);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getZSetRangeByScore complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 按分数范围获取元素（降序，支持复杂泛型）
     *
     * @param key           key
     * @param minScore      最小分数
     * @param maxScore      最大分数
     * @param typeReference 类型模板
     * @param <T>           泛型类型
     * @return 元素集合
     */
    public <T> Set<T> getZSetReverseRangeByScore(final String key, final double minScore, final double maxScore, TypeReference<LinkedHashSet<T>> typeReference) {
        try {
            Set data = redisTemplate.opsForZSet().reverseRangeByScore(key, minScore, maxScore);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getZSetReverseRangeByScore complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据排序分值删除
     *
     * @param key      key
     * @param minScore 最小分
     * @param maxScore 最大分
     * @return 删除的元素个数
     */
    public Long removeZSetByScore(final String key, final double minScore, final double maxScore) {
        try {
            Long l = redisTemplate.opsForZSet().removeRangeByScore(key, minScore, maxScore);
            return l == null ? 0L : l;
        } catch (Exception e) {
            log.warn("RedisService.removeZSetByScore del member error: {}", e.getMessage());
            return 0L;
        }
    }

    /*=============================================    Hash    =============================================*/

    /**
     * 将 Map 数据批量存入 Redis Hash
     * <p>
     * 如果键不存在，会创建新的 Hash。如果键已存在，会覆盖所有字段。
     * Hash 结构适合存储对象的多个属性。
     * </p>
     *
     * @param key     Redis 键，不能为 null
     * @param dataMap 要存储的 Map 数据，key 为字段名，value 为字段值，不能为 null
     * @param <T>     值的类型
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            try {
                redisTemplate.opsForHash().putAll(key, dataMap);
            } catch (Exception e) {
                log.warn("RedisService.setCacheMap set cache error: {}", e.getMessage());
            }
        }
    }

    /**
     * 往 Hash 中存入单个字段
     * <p>
     * 如果 Hash 键不存在，会创建新的 Hash。如果字段已存在，会被覆盖。
     * </p>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKey  Hash 字段名，不能为 null
     * @param value 字段值，可以是任意类型，会自动序列化
     * @param <T>   值的类型
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        try {
            redisTemplate.opsForHash().put(key, hKey, value);
        } catch (Exception e) {
            log.warn("RedisService.setCacheMapValue set cache error: {}", e.getMessage());
        }
    }

    /**
     * 删除 Hash 中的某条数据
     *
     * @param key  Redis 键
     * @param hKey Hash 键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey) {
        try {
            return redisTemplate.opsForHash().delete(key, hKey) > 0;
        } catch (Exception e) {
            log.warn("RedisService.deleteCacheMapValue delete cache error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除 Hash 中的多个字段
     *
     * @param key   Redis 键
     * @param hKeys Hash 键集合
     * @return 删除的字段数量
     */
    public Long deleteCacheMapValues(final String key, final Object... hKeys) {
        try {
            Long deleted = redisTemplate.opsForHash().delete(key, hKeys);
            return deleted == null ? 0L : deleted;
        } catch (Exception e) {
            log.warn("RedisService.deleteCacheMapValues delete cache error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 获取 Hash 中的所有字段和值
     * <p>
     * 返回 Hash 中所有字段的 Map。适用于简单的对象类型（非泛型嵌套）。
     * </p>
     * <p>
     * 对于复杂泛型类型（如 Map&lt;String, List&lt;User&gt;&gt;），请使用 {@link #getCacheMap(String, TypeReference)} 方法。
     * </p>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param clazz 值的类型，不能为 null
     * @param <T>   值的类型
     * @return Hash 对应的 Map，如果键不存在或发生错误，返回 null
     */
    public <T> Map<String, T> getCacheMap(final String key, Class<T> clazz) {
        try {
            Map data = redisTemplate.opsForHash().entries(key);
            return JsonUtil.jsonToMap(JsonUtil.classToJson(data), clazz);
        } catch (Exception e) {
            log.warn("RedisService.getCacheMap error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中的所有字段和值（支持复杂泛型嵌套）
     * <p>
     * 使用 TypeReference 可以正确处理泛型类型，如 Map&lt;String, User&gt;、Map&lt;String, List&lt;Order&gt;&gt; 等。
     * </p>
     *
     * @param key           Redis 键（Hash 的键），不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           值的类型
     * @return Hash 对应的 Map，如果键不存在或发生错误，返回 null
     */
    public <T> Map<String, T> getCacheMap(final String key, TypeReference<Map<String, T>> typeReference) {
        try {
            Map data = redisTemplate.opsForHash().entries(key);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheMap complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中指定字段的值
     * <p>
     * 如果字段不存在，返回 null。适用于简单的对象类型（非泛型嵌套）。
     * </p>
     *
     * @param key  Redis 键（Hash 的键），不能为 null
     * @param hKey Hash 字段名，不能为 null
     * @param <T>  值的类型
     * @return 字段值，如果字段不存在或发生错误，返回 null
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        try {
            HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
            return opsForHash.get(key, hKey);
        } catch (Exception e) {
            log.warn("RedisService.getCacheMapValue error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中的单个数据（支持复杂的泛型嵌套）
     *
     * @param key           Redis 键
     * @param hKey          Hash 键
     * @param typeReference 对象模板
     * @param <T>           对象类型
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey, TypeReference<T> typeReference) {
        try {
            Object value = redisTemplate.opsForHash().get(key, hKey);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(value), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getCacheMapValue complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中的多个数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @param clazz 值的类型
     * @param <T>   对象类型
     * @return 获取的多个数据的集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<String> hKeys, Class<T> clazz) {
        try {
            List list = redisTemplate.opsForHash().multiGet(key, hKeys);
            return JsonUtil.jsonToList(JsonUtil.classToJson(list), clazz);
        } catch (Exception e) {
            log.warn("RedisService.getMultiCacheMapValue error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中的多个数据 Map 版（支持复杂泛型嵌套）
     *
     * @param key           Redis键
     * @param hKeys         Hash键集合
     * @param typeReference 对象模板
     * @param <T>           对象类型
     * @return 获取的多个数据的集合
     */
    public <T> Map<String, T> getMultiCacheMapValue(final String key, final Collection<String> hKeys, TypeReference<Map<String, T>> typeReference) {
        try {
            List<Object> data = redisTemplate.opsForHash().multiGet(key, hKeys);
            Map<String, Object> resultMap = new HashMap<>();
            Iterator<String> keyIterator = hKeys.iterator();
            for (Object value : data) {
                if (keyIterator.hasNext()) {
                    resultMap.put(keyIterator.next(), value);
                }
            }
            return JsonUtil.jsonToClass(JsonUtil.classToJson(resultMap), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getMultiCacheMapValue complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中的多个数据 List 版（支持复杂泛型嵌套）
     *
     * @param key           Redis键
     * @param hKeys         Hash键集合
     * @param typeReference 对象模板
     * @param <T>           对象类型
     * @return 获取的多个数据的集合
     */
    public <T> List<T> getMultiCacheListValue(final String key, final Collection<String> hKeys, TypeReference<List<T>> typeReference) {
        try {
            List data = redisTemplate.opsForHash().multiGet(key, hKeys);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getMultiCacheListValue complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中的多个数据 List 版（支持复杂泛型嵌套）
     *
     * @param key           Redis键
     * @param hKeys         Hash键集合
     * @param typeReference 对象模板
     * @param <T>           对象类型
     * @return 获取的多个数据的集合
     */
    public <T> Set<T> getMultiCacheSetValue(final String key, final Collection<String> hKeys, TypeReference<Set<T>> typeReference) {
        try {
            List data = redisTemplate.opsForHash().multiGet(key, hKeys);
            return JsonUtil.jsonToClass(JsonUtil.classToJson(data), typeReference);
        } catch (Exception e) {
            log.warn("RedisService.getMultiCacheSetValue complex error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Hash 中字段的数量
     *
     * @param key Redis 键
     * @return 字段数量
     */
    public Long getCacheMapSize(final String key) {
        try {
            Long size = redisTemplate.opsForHash().size(key);
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.warn("RedisService.getCacheMapSize error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 获取 Hash 中的所有 key
     *
     * @param key Redis 键
     * @return Hash 中的所有 key 集合
     */
    public Set<String> getCacheMapKeys(final String key) {
        try {
            Set<String> keys = redisTemplate.opsForHash().keys(key);
            return keys == null ? Collections.emptySet() : keys;
        } catch (Exception e) {
            log.warn("RedisService.getCacheMapKeys error: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 为 Hash 中指定字段的值增加指定数值（整型）
     * <p>
     * 这是一个原子操作，线程安全。如果字段不存在，会先初始化为 0 再增加。
     * 如果直接 increment 失败（值不是数字类型），会尝试获取当前值并手动转换后计算。
     * </p>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKey  Hash 字段名，不能为 null
     * @param delta 增加的数值，可以为负数（相当于减少）
     * @return 增加后的数值，如果发生错误返回 0
     */
    public Long incrementCacheMapValue(final String key, final String hKey, final long delta) {
        try {
            return redisTemplate.opsForHash().increment(key, hKey, delta);
        } catch (Exception e) {
            // 如果直接increment失败，尝试获取当前值并手动转换后计算
            try {
                Object currentValue = redisTemplate.opsForHash().get(key, hKey);
                if (currentValue == null) {
                    // 如果字段不存在，直接设置delta值
                    redisTemplate.opsForHash().put(key, hKey, String.valueOf(delta));
                    return delta;
                }

                // 尝试将当前值转换为Long
                Long currentLongValue;
                if (currentValue instanceof String) {
                    currentLongValue = Long.parseLong((String) currentValue);
                } else if (currentValue instanceof Number) {
                    currentLongValue = ((Number) currentValue).longValue();
                } else {
                    throw new IllegalArgumentException("Cannot convert " + currentValue.getClass() + " to Long");
                }

                // 计算新值并存储
                Long newValue = currentLongValue + delta;
                redisTemplate.opsForHash().put(key, hKey, String.valueOf(newValue));
                return newValue;
            } catch (Exception parseException) {
                log.warn("RedisService.incrementCacheMapValue Integer error: {}", parseException.getMessage());
                return 0L;
            }
        }
    }

    /**
     * 为 Hash 中指定字段的值增加指定数值（浮点型）
     * <p>
     * 这是一个原子操作，线程安全。如果字段不存在，会先初始化为 0.0 再增加。
     * 如果直接 increment 失败（值不是数字类型），会尝试获取当前值并手动转换后计算。
     * </p>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKey  Hash 字段名，不能为 null
     * @param delta 增加的数值，可以为负数（相当于减少）
     * @return 增加后的数值，如果发生错误返回 0.0
     */
    public Double incrementCacheMapValue(final String key, final String hKey, final double delta) {
        try {
            return redisTemplate.opsForHash().increment(key, hKey, delta);
        } catch (Exception e) {
            // 如果直接 increment 失败，尝试获取当前值并手动转换后计算
            try {
                Object currentValue = redisTemplate.opsForHash().get(key, hKey);
                if (currentValue == null) {
                    // 如果字段不存在，直接设置delta值
                    redisTemplate.opsForHash().put(key, hKey, String.valueOf(delta));
                    return delta;
                }

                // 尝试将当前值转换为 Double
                Double currentDoubleValue;
                if (currentValue instanceof String) {
                    currentDoubleValue = Double.parseDouble((String) currentValue);
                } else if (currentValue instanceof Number) {
                    currentDoubleValue = ((Number) currentValue).doubleValue();
                } else {
                    throw new IllegalArgumentException("Cannot convert " + currentValue.getClass() + " to Double");
                }

                // 计算新值并存储
                Double newValue = currentDoubleValue + delta;
                redisTemplate.opsForHash().put(key, hKey, String.valueOf(newValue));
                return newValue;
            } catch (Exception parseException) {
                log.warn("RedisService.incrementCacheMapValue Double error: {}", parseException.getMessage());
                return 0.0;
            }
        }
    }

    /*=============================================    LUA脚本    =============================================*/

    /**
     * 比较并删除（Compare And Delete，CAD）
     * <p>
     * 这是一个原子操作，通过 Lua 脚本实现。只有当键的值等于期望值时，才会删除该键。
     * 常用于实现分布式锁的释放，确保只有持有锁的线程才能释放锁。
     * </p>
     * <p>
     * 注意：key 和 value 中不能包含空格，否则操作会失败。
     * </p>
     * <p>
     * 使用示例（分布式锁释放）：
     * <pre>
     * String lockValue = UUID.randomUUID().toString();
     * if (redisService.setCacheObjectIfAbsent("lock:key", lockValue, 60, TimeUnit.SECONDS)) {
     *     try {
     *         // 执行业务逻辑
     *     } finally {
     *         // 只有锁的值匹配时才释放
     *         redisService.compareAndDelete("lock:key", lockValue);
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param key   缓存键，不能为 null，不能包含空格
     * @param value 期望的值，只有当键的值等于此值时才会删除，不能为 null，不能包含空格
     * @return true - 删除成功（值匹配）；false - 删除失败（值不匹配、键不存在或包含空格）
     */
    public boolean compareAndDelete(String key, String value) {
        // 验证 key 和 value 中不能包含空格
        if (key.contains(StringUtil.SPACE) || value.contains(StringUtil.SPACE)) {
            return false;
        }

        String script = """
                -- 如果 key 对应的值等于传入值，则删除 key
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;

        // 通过 lua 脚本原子验证令牌和删除令牌
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key), // KEYS[1]
                value); // ARGV[1]
        // 如果返回结果为 0 行, 则说明删除失败
        return !Objects.equals(result, 0L);
    }
}
