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
 * <b>特性说明：</b>
 * <ul>
 *     <li>自动序列化/反序列化：使用 GenericJackson2JsonRedisSerializer 自动处理对象序列化</li>
 *     <li>支持泛型嵌套：通过 TypeReference 支持复杂的泛型类型（如 List&lt;Map&lt;String, User&gt;&gt;）</li>
 *     <li>异常安全：所有方法都包含异常处理，不会抛出异常，失败时返回默认值或 false</li>
 *     <li>事务支持：提供 Redis 事务执行方法，保证操作的原子性</li>
 *     <li>Lua 脚本：支持通过 Lua 脚本实现复杂的原子操作</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 基本对象存储
 * redisService.setCacheObject("user:1", user, 3600, TimeUnit.SECONDS);
 * User cachedUser = redisService.getCacheObject("user:1", User.class);
 *
 * // 2. 复杂泛型类型存储
 * TypeReference<List<User>> typeRef = new TypeReference<List<User>>() {};
 * redisService.setCacheList("users", userList);
 * List<User> users = redisService.getCacheList("users", typeRef);
 *
 * // 3. 原子操作
 * Long count = redisService.incr("counter", 1);
 * Boolean success = redisService.setCacheObjectIfAbsent("lock:key", "value", 60, TimeUnit.SECONDS);
 *
 * // 4. Hash 操作
 * redisService.setCacheMapValue("user:1:info", "name", "张三");
 * String name = redisService.getCacheMapValue("user:1:info", "name");
 *
 * // 5. List 操作
 * redisService.rightPushForList("queue:task", task);
 * Task task = redisService.leftPopForList("queue:task");
 *
 * // 6. Set 操作
 * redisService.addMember("set:tags", "java", "python");
 * boolean exists = redisService.isMember("set:tags", "java");
 *
 * // 7. ZSet 操作（排行榜）
 * redisService.addMemberZSet("zset:ranking", user, 100.0);
 * Set<User> top10 = redisService.getZSetRangeDesc("zset:ranking", 0, 9, typeRef);
 * }</pre>
 * </p>
 * <p>
 * <b>注意事项：</b>
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
     * Redis 模板对象
     * <p>
     * Spring Data Redis 提供的 RedisTemplate，用于执行 Redis 操作。
     * 支持所有 Redis 数据结构类型的操作。
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /*=============================================    通用方法    =============================================*/

    /**
     * 为指定的键设置过期时间（秒）
     * <p>
     * 如果键不存在，操作会失败。如果键已经有过期时间，会更新为新的过期时间。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 设置键的过期时间为 60 秒
     * Boolean success = redisService.expire("user:123", 60);
     * if (success) {
     *     log.info("过期时间设置成功");
     * }
     *
     * // 为缓存设置过期时间
     * redisService.setCacheObject("cache:key", data);
     * redisService.expire("cache:key", 3600); // 1 小时后过期
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 false</li>
     *     <li>如果键已经有过期时间，会更新为新的过期时间</li>
     *     <li>timeout 必须大于 0</li>
     *     <li>设置成功后，键会在指定秒数后自动删除</li>
     * </ul>
     *
     * @param key     Redis 键，不能为 null
     * @param timeout 过期时间（秒），必须大于 0
     * @return true - 设置成功；false - 设置失败（键不存在或其他错误）
     * @see #expire(String, long, TimeUnit)
     * @see #getExpire(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 设置键的过期时间为 30 分钟
     * Boolean success = redisService.expire("user:123", 30, TimeUnit.MINUTES);
     *
     * // 设置键的过期时间为 1 小时
     * Boolean success = redisService.expire("cache:key", 1, TimeUnit.HOURS);
     *
     * // 设置键的过期时间为 7 天
     * Boolean success = redisService.expire("session:token", 7, TimeUnit.DAYS);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 false</li>
     *     <li>如果键已经有过期时间，会更新为新的过期时间</li>
     *     <li>timeout 必须大于 0</li>
     *     <li>支持的时间单位：SECONDS、MINUTES、HOURS、DAYS、MILLISECONDS、MICROSECONDS、NANOSECONDS</li>
     * </ul>
     *
     * @param key      Redis 键，不能为 null
     * @param timeout  过期时间，必须大于 0
     * @param timeUnit 时间单位，不能为 null
     * @return true - 设置成功；false - 设置失败（键不存在或其他错误）
     * @see #expire(String, long)
     * @see #getExpire(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取键的剩余过期时间
     * Long ttl = redisService.getExpire("user:123");
     * if (ttl > 0) {
     *     log.info("键将在 {} 秒后过期", ttl);
     * } else if (ttl == -1) {
     *     log.info("键永久有效");
     * } else {
     *     log.info("键不存在");
     * }
     *
     * // 检查缓存是否即将过期（剩余时间少于 60 秒）
     * Long ttl = redisService.getExpire("cache:key");
     * if (ttl > 0 && ttl < 60) {
     *     // 刷新缓存
     *     refreshCache("cache:key");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回 -1 表示键存在但没有设置过期时间（永久有效）</li>
     *     <li>返回 -2 表示键不存在</li>
     *     <li>返回正数表示剩余过期时间（秒）</li>
     * </ul>
     *
     * @param key Redis 键，不能为 null
     * @return 剩余过期时间（秒），-1 表示永久有效，-2 表示键不存在
     * @see #expire(String, long)
     * @see #expire(String, long, TimeUnit)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查键是否存在
     * if (redisService.hasKey("user:123")) {
     *     log.info("键存在");
     *     User user = redisService.getCacheObject("user:123", User.class);
     * } else {
     *     log.info("键不存在，需要从数据库加载");
     *     User user = loadUserFromDatabase(123L);
     *     redisService.setCacheObject("user:123", user, 3600, TimeUnit.SECONDS);
     * }
     *
     * // 检查缓存是否存在，决定是否刷新
     * if (!redisService.hasKey("cache:key")) {
     *     refreshCache("cache:key");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键已过期但还未被删除，可能仍返回 true（取决于 Redis 版本）</li>
     *     <li>如果键不存在，返回 false</li>
     *     <li>时间复杂度为 O(1)</li>
     * </ul>
     *
     * @param key Redis 键，不能为 null
     * @return true - 键存在；false - 键不存在或发生错误
     * @see #getCacheObject(String, Class)
     * @see #deleteObject(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查找所有以 user: 开头的键
     * Collection<String> userKeys = redisService.keys("user:*");
     * log.info("找到 {} 个用户键", userKeys.size());
     *
     * // 查找所有 session 键
     * Collection<String> sessionKeys = redisService.keys("session:*");
     * for (String key : sessionKeys) {
     *     redisService.deleteObject(key);
     * }
     *
     * // 查找特定模式的键
     * Collection<String> keys = redisService.keys("cache:user:[123]");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>在生产环境中慎用，如果键数量很大可能会阻塞 Redis 服务器</li>
     *     <li>建议使用 SCAN 命令的迭代方式（本方法未实现）</li>
     *     <li>如果键数量很大，此操作可能较慢</li>
     *     <li>返回的集合可能包含大量键，注意内存使用</li>
     * </ul>
     *
     * @param pattern 键的模式，支持通配符，不能为 null
     * @return 匹配的键集合，如果没有匹配的键或发生错误，返回空集合
     * @see #deleteObject(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 重命名键
     * redisService.renameKey("old:key", "new:key");
     *
     * // 迁移数据时重命名
     * redisService.renameKey("user:temp:123", "user:123");
     *
     * // 备份键（先复制再重命名）
     * User user = redisService.getCacheObject("user:123", User.class);
     * if (user != null) {
     *     redisService.setCacheObject("user:123:backup", user);
     *     redisService.renameKey("user:123", "user:123:old");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果新键名已存在，会被覆盖（原数据丢失）</li>
     *     <li>如果旧键不存在，操作会失败（但不会抛出异常）</li>
     *     <li>这是一个原子操作</li>
     *     <li>重命名后，旧键名不再存在</li>
     * </ul>
     *
     * @param oldKey 原键名，不能为 null
     * @param newKey 新键名，不能为 null，如果已存在会被覆盖
     * @see #deleteObject(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除单个键
     * Boolean deleted = redisService.deleteObject("user:123");
     * if (deleted) {
     *     log.info("键删除成功");
     * }
     *
     * // 删除缓存
     * redisService.deleteObject("cache:key");
     *
     * // 删除会话
     * redisService.deleteObject("session:" + sessionId);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 false（不会抛出异常）</li>
     *     <li>删除操作是原子性的</li>
     *     <li>删除后，键及其关联的值都会被移除</li>
     *     <li>适用于所有数据类型（String、List、Set、ZSet、Hash）</li>
     * </ul>
     *
     * @param key 要删除的 Redis 键，不能为 null
     * @return true - 删除成功；false - 删除失败（键不存在或其他错误）
     * @see #deleteObject(Collection)
     * @see #hasKey(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量删除多个键
     * List<String> keys = Arrays.asList("user:1", "user:2", "user:3");
     * Long deleted = redisService.deleteObject(keys);
     * log.info("删除了 {} 个键", deleted);
     *
     * // 删除匹配模式的所有键
     * Collection<String> userKeys = redisService.keys("user:*");
     * if (!userKeys.isEmpty()) {
     *     Long deleted = redisService.deleteObject(userKeys);
     *     log.info("删除了 {} 个用户键", deleted);
     * }
     *
     * // 清理过期缓存
     * Collection<String> cacheKeys = redisService.keys("cache:*");
     * redisService.deleteObject(cacheKeys);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果某个键不存在，会被忽略（不会抛出异常）</li>
     *     <li>返回实际删除的键数量</li>
     *     <li>如果所有键都不存在，返回 0</li>
     *     <li>批量删除是原子性的，要么全部成功，要么全部失败</li>
     *     <li>如果键数量很大，此操作可能较慢</li>
     *     <li>如果发生错误返回 null（注意：与返回 0 不同）</li>
     * </ul>
     *
     * @param collection 要删除的键集合，不能为 null
     * @return 实际删除的键数量，如果发生错误返回 null（注意：不是 0）
     * @see #deleteObject(String)
     * @see #keys(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在事务中执行多个操作
     * List<Object> results = redisService.executeInTransaction(operations -> {
     *     operations.opsForValue().set("key1", "value1");
     *     operations.opsForValue().set("key2", "value2");
     *     operations.opsForList().leftPush("list:key", "item");
     * });
     *
     * // 原子性地更新多个字段
     * List<Object> results = redisService.executeInTransaction(operations -> {
     *     HashOperations<String, String, Object> hashOps = operations.opsForHash();
     *     hashOps.put("user:123:info", "name", "张三");
     *     hashOps.put("user:123:info", "age", 25);
     *     hashOps.put("user:123:info", "email", "zhangsan@example.com");
     * });
     *
     * // 事务失败时会抛出异常
     * try {
     *     redisService.executeInTransaction(operations -> {
     *         operations.opsForValue().set("key1", "value1");
     *         // 如果这里发生异常，事务会回滚
     *         throw new RuntimeException("操作失败");
     *     });
     * } catch (RuntimeException e) {
     *     log.error("事务执行失败", e);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>所有操作在一个事务中执行，保证原子性</li>
     *     <li>如果事务执行失败，会抛出 RuntimeException</li>
     *     <li>事务执行流程：MULTI → 执行操作 → EXEC 或 DISCARD（发生异常时）</li>
     *     <li>如果命令类型错误或队列为空，exec() 会返回 null，抛出异常</li>
     *     <li>事务中的命令不会立即执行，而是在 EXEC 时批量执行</li>
     * </ul>
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储简单对象
     * User user = new User();
     * user.setId(1L);
     * user.setName("张三");
     * redisService.setCacheObject("user:1", user);
     *
     * // 存储字符串
     * redisService.setCacheObject("config:app_name", "MyApp");
     *
     * // 存储数字
     * redisService.setCacheObject("counter:total", 100);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>对象会被自动序列化为 JSON 格式</li>
     *     <li>如果键已存在，会被覆盖</li>
     *     <li>存储的对象会永久有效，直到手动删除</li>
     *     <li>如果需要设置过期时间，使用 {@link #setCacheObject(String, Object, long, TimeUnit)}</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 缓存值，可以是任意类型，会自动序列化
     * @param <T>   值的类型
     * @see #setCacheObject(String, Object, long, TimeUnit)
     * @see #getCacheObject(String, Class)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储用户信息，1 小时后过期
     * User user = new User();
     * user.setId(1L);
     * user.setName("张三");
     * redisService.setCacheObject("user:1", user, 1, TimeUnit.HOURS);
     *
     * // 存储验证码，5 分钟后过期
     * redisService.setCacheObject("captcha:13800138000", "123456", 5, TimeUnit.MINUTES);
     *
     * // 存储会话信息，30 分钟后过期
     * Session session = new Session();
     * redisService.setCacheObject("session:token123", session, 30, TimeUnit.MINUTES);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>对象会被自动序列化为 JSON 格式</li>
     *     <li>如果键已存在，会被覆盖</li>
     *     <li>到达过期时间后，键会自动从 Redis 中删除</li>
     *     <li>timeout 必须大于 0</li>
     *     <li>支持的时间单位：SECONDS、MINUTES、HOURS、DAYS 等</li>
     * </ul>
     *
     * @param key      缓存键，不能为 null
     * @param value    缓存值，可以是任意类型，会自动序列化
     * @param timeout  过期时间，必须大于 0
     * @param timeUnit 时间单位，不能为 null（如 TimeUnit.SECONDS、TimeUnit.MINUTES 等）
     * @param <T>      值的类型
     * @see #setCacheObject(String, Object)
     * @see #getCacheObject(String, Class)
     * @see #expire(String, long, TimeUnit)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实现分布式锁（不设置过期时间）
     * String lockKey = "lock:resource:123";
     * String lockValue = UUID.randomUUID().toString();
     * if (redisService.setCacheObjectIfAbsent(lockKey, lockValue)) {
     *     try {
     *         // 获取锁成功，执行业务逻辑
     *         doSomething();
     *     } finally {
     *         // 释放锁
     *         if (lockValue.equals(redisService.getCacheObject(lockKey, String.class))) {
     *             redisService.deleteObject(lockKey);
     *         }
     *     }
     * } else {
     *     log.warn("获取锁失败，资源被占用");
     * }
     *
     * // 初始化配置（只初始化一次）
     * if (redisService.setCacheObjectIfAbsent("config:initialized", true)) {
     *     initializeConfig();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果键已存在，操作会失败，不会覆盖原有值</li>
     *     <li>常用于实现分布式锁</li>
     *     <li>如果需要设置过期时间，使用 {@link #setCacheObjectIfAbsent(String, Object, long, TimeUnit)}</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 缓存值，可以是任意类型，会自动序列化
     * @param <T>   值的类型
     * @return true - 设置成功（键不存在）；false - 设置失败（键已存在或发生错误）
     * @see #setCacheObjectIfAbsent(String, Object, long, TimeUnit)
     * @see #compareAndDelete(String, String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实现带过期时间的分布式锁
     * String lockKey = "lock:resource:123";
     * String lockValue = UUID.randomUUID().toString();
     * if (redisService.setCacheObjectIfAbsent(lockKey, lockValue, 60, TimeUnit.SECONDS)) {
     *     try {
     *         // 获取锁成功，执行业务逻辑
     *         doSomething();
     *     } finally {
     *         // 释放锁（使用 compareAndDelete 确保只释放自己的锁）
     *         redisService.compareAndDelete(lockKey, lockValue);
     *     }
     * } else {
     *     log.warn("获取锁失败，资源被占用");
     * }
     *
     * // 初始化缓存（只初始化一次，30 分钟后过期）
     * if (redisService.setCacheObjectIfAbsent("cache:key", data, 30, TimeUnit.MINUTES)) {
     *     log.info("缓存初始化成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果键已存在，操作会失败，不会覆盖原有值</li>
     *     <li>常用于实现带过期时间的分布式锁</li>
     *     <li>过期时间可以防止死锁（锁自动释放）</li>
     *     <li>建议使用 {@link #compareAndDelete(String, String)} 释放锁，确保只释放自己的锁</li>
     * </ul>
     *
     * @param key      缓存键，不能为 null
     * @param value    缓存值，可以是任意类型，会自动序列化
     * @param timeout  过期时间，必须大于 0
     * @param timeUnit 时间单位，不能为 null
     * @param <T>      值的类型
     * @return true - 设置成功（键不存在）；false - 设置失败（键已存在或发生错误）
     * @see #setCacheObjectIfAbsent(String, Object)
     * @see #compareAndDelete(String, String)
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
     * <p>
     * 对于复杂泛型类型（如 List&lt;User&gt;），请使用 {@link #getCacheObject(String, TypeReference)} 方法。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户对象
     * User user = redisService.getCacheObject("user:123", User.class);
     * if (user != null) {
     *     log.info("用户姓名: {}", user.getName());
     * } else {
     *     log.info("用户不存在，从数据库加载");
     *     user = loadUserFromDatabase(123L);
     *     redisService.setCacheObject("user:123", user, 3600, TimeUnit.SECONDS);
     * }
     *
     * // 获取字符串
     * String appName = redisService.getCacheObject("config:app_name", String.class);
     *
     * // 获取数字
     * Integer count = redisService.getCacheObject("counter:total", Integer.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 null</li>
     *     <li>如果对象类型匹配，直接返回；否则会进行 JSON 转换</li>
     *     <li>适用于简单的对象类型（非泛型嵌套）</li>
     *     <li>对于复杂泛型类型，使用 {@link #getCacheObject(String, TypeReference)}</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param clazz 缓存对象的类型，不能为 null
     * @param <T>   对象的类型
     * @return 缓存的对象，如果键不存在或发生错误，返回 null
     * @see #getCacheObject(String, TypeReference)
     * @see #setCacheObject(String, Object)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取 List<User> 类型
     * TypeReference<List<User>> typeRef = new TypeReference<List<User>>() {};
     * List<User> users = redisService.getCacheObject("users", typeRef);
     *
     * // 获取 Map<String, User> 类型
     * TypeReference<Map<String, User>> typeRef = new TypeReference<Map<String, User>>() {};
     * Map<String, User> userMap = redisService.getCacheObject("user:map", typeRef);
     *
     * // 获取复杂嵌套类型 List<Map<String, Order>>
     * TypeReference<List<Map<String, Order>>> typeRef = new TypeReference<List<Map<String, Order>>>() {};
     * List<Map<String, Order>> complexData = redisService.getCacheObject("complex:data", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 null</li>
     *     <li>使用 TypeReference 可以正确处理泛型类型</li>
     *     <li>适用于复杂泛型嵌套类型</li>
     *     <li>简单类型可以使用 {@link #getCacheObject(String, Class)}</li>
     * </ul>
     *
     * @param key          缓存键，不能为 null
     * @param valueTypeRef 类型引用，用于指定泛型类型，不能为 null
     * @param <T>          对象的类型
     * @return 缓存的对象，如果键不存在或发生错误，返回 null
     * @see #getCacheObject(String, Class)
     * @see #setCacheObject(String, Object)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 递增计数器
     * Long count = redisService.incr("counter:page_view");
     * log.info("页面访问次数: {}", count);
     *
     * // 实现点赞功能
     * Long likes = redisService.incr("post:123:likes");
     * log.info("点赞数: {}", likes);
     *
     * // 实现访问计数
     * Long visits = redisService.incr("user:123:visits");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果键不存在，会先初始化为 0 再递增</li>
     *     <li>键的值必须是数字类型（整数），否则会失败</li>
     *     <li>如果失败返回 -1</li>
     *     <li>适用于计数器、点赞数、访问量等场景</li>
     * </ul>
     *
     * @param key Redis 键，不能为 null 或空字符串
     * @return 递增后的值，如果失败返回 -1
     * @see #incr(String, long)
     * @see #decr(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 增加积分（增加 10 分）
     * Long newScore = redisService.incr("user:123:score", 10);
     * log.info("新积分: {}", newScore);
     *
     * // 减少积分（使用负数）
     * Long newScore = redisService.incr("user:123:score", -5);
     *
     * // 批量增加访问量
     * Long totalVisits = redisService.incr("page:home:visits", 100);
     *
     * // 实现库存扣减（减少库存）
     * Long remaining = redisService.incr("product:123:stock", -1);
     * if (remaining >= 0) {
     *     log.info("库存扣减成功，剩余: {}", remaining);
     * } else {
     *     log.warn("库存不足");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果键不存在，会先初始化为 0 再递增</li>
     *     <li>键的值必须是数字类型（整数），否则会失败</li>
     *     <li>delta 可以为负数（相当于递减）</li>
     *     <li>如果失败返回 -1</li>
     * </ul>
     *
     * @param key   Redis 键，不能为 null 或空字符串
     * @param delta 增加的数值，可以为负数（相当于递减）
     * @return 递增后的值，如果失败返回 -1
     * @see #incr(String)
     * @see #decr(String, long)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 递减计数器
     * Long count = redisService.decr("counter:remaining");
     * log.info("剩余次数: {}", count);
     *
     * // 扣减库存
     * Long stock = redisService.decr("product:123:stock");
     * if (stock >= 0) {
     *     log.info("库存扣减成功，剩余: {}", stock);
     * } else {
     *     log.warn("库存不足");
     * }
     *
     * // 减少可用配额
     * Long quota = redisService.decr("user:123:quota");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果键不存在，会先初始化为 0 再递减（结果为 -1）</li>
     *     <li>键的值必须是数字类型（整数），否则会失败</li>
     *     <li>如果失败返回 -1</li>
     *     <li>适用于库存扣减、配额减少等场景</li>
     * </ul>
     *
     * @param key Redis 键，不能为 null 或空字符串
     * @return 递减后的值，如果失败返回 -1
     * @see #decr(String, long)
     * @see #incr(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量扣减库存（扣减 5 个）
     * Long remaining = redisService.decr("product:123:stock", 5);
     * if (remaining >= 0) {
     *     log.info("库存扣减成功，剩余: {}", remaining);
     * } else {
     *     log.warn("库存不足");
     * }
     *
     * // 减少积分（扣减 10 分）
     * Long newScore = redisService.decr("user:123:score", 10);
     *
     * // 批量减少配额
     * Long quota = redisService.decr("user:123:quota", 3);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果键不存在，会先初始化为 0 再递减</li>
     *     <li>键的值必须是数字类型（整数），否则会失败</li>
     *     <li>delta 必须大于 0</li>
     *     <li>如果失败返回 -1</li>
     *     <li>适用于批量库存扣减、配额减少等场景</li>
     * </ul>
     *
     * @param key   Redis 键，不能为 null 或空字符串
     * @param delta 减少的数值，必须大于 0
     * @return 递减后的值，如果失败返回 -1
     * @see #decr(String)
     * @see #incr(String, long)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储用户ID列表
     * List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
     * Long length = redisService.setCacheList("user:ids", userIds);
     * log.info("列表长度: {}", length);
     *
     * // 存储任务队列
     * List<Task> tasks = taskService.getPendingTasks();
     * redisService.setCacheList("task:queue", tasks);
     *
     * // 追加数据到现有列表
     * List<String> newItems = Arrays.asList("item1", "item2");
     * redisService.setCacheList("list:items", newItems); // 追加到列表末尾
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，会创建新的列表</li>
     *     <li>如果键已存在，会将新元素追加到列表末尾</li>
     *     <li>列表保持插入顺序，支持重复元素</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>如果列表很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key      缓存键，不能为 null
     * @param dataList 要存储的列表数据，不能为 null
     * @param <T>      元素的类型
     * @return 保存成功返回列表长度，失败返回 -1
     * @see #getCacheList(String, Class)
     * @see #rightPushForList(String, Object)
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
     * 适用于实现队列（FIFO）或栈（LIFO）数据结构。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实现队列：左侧插入，右侧取出
     * redisService.leftPushForList("queue:task", task);
     * Task task = redisService.rightPopForList("queue:task");
     *
     * // 实现栈：左侧插入，左侧取出
     * redisService.leftPushForList("stack:data", data);
     * Data data = redisService.leftPopForList("stack:data");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null，可能抛出异常</li>
     *     <li>如果列表不存在，会创建新的列表</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>支持重复元素</li>
     * </ul>
     *
     * @param key  缓存键，不能为 null
     * @param data 要插入的元素，不能为 null
     * @param <T>  元素的类型
     * @return 插入成功返回列表长度，失败返回 -1
     * @see #rightPushForList(String, Object)
     * @see #leftPopForList(String)
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
     * 适用于实现队列（FIFO）数据结构。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实现队列：右侧插入，左侧取出
     * redisService.rightPushForList("queue:task", task);
     * Task task = redisService.leftPopForList("queue:task");
     *
     * // 追加元素到列表末尾
     * redisService.rightPushForList("list:items", item);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 为 null，可能抛出异常</li>
     *     <li>如果列表不存在，会创建新的列表</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>支持重复元素</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要插入的元素，不能为 null
     * @param <T>   元素的类型
     * @return 插入成功返回列表长度，失败返回 -1
     * @see #leftPushForList(String, Object)
     * @see #rightPopForList(String)
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
     * 删除并返回列表左侧第一个元素（头删）
     * <p>
     * 从列表左侧（头部）删除并返回第一个元素。如果列表为空，返回 null。
     * 适用于实现队列（FIFO）或栈（LIFO）数据结构。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实现队列：左侧取出
     * Task task = redisService.leftPopForList("queue:task");
     * if (task != null) {
     *     // 处理任务
     * }
     *
     * // 实现栈：左侧取出
     * Data data = redisService.leftPopForList("stack:data");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果列表为空，返回 null（不会抛出异常）</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>这是一个阻塞操作，如果列表为空会立即返回 null</li>
     *     <li>如果需要阻塞等待，使用带超时参数的方法</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @see #rightPopForList(String)
     * @see #leftPopForList(String, long)
     */
    public void leftPopForList(final String key) {
        try {
            redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.warn("RedisService.leftPopForList left pop redis error: {}", e.getMessage());
        }
    }

    /**
     * 删除并返回列表左侧 k 个元素（头删）
     * <p>
     * 从列表左侧（头部）删除并返回前 k 个元素。如果列表元素少于 k 个，返回所有元素。
     * 适用于批量处理列表头部的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量取出列表头部的元素（最多5个）
     * List<Data> dataList = redisService.leftPopForList("list:data", 5);
     * if (dataList != null && !dataList.isEmpty()) {
     *     // 处理数据
     *     processData(dataList);
     * }
     *
     * // 批量处理任务队列（每次处理10个任务）
     * List<Task> tasks = redisService.leftPopForList("queue:task", 10);
     * for (Task task : tasks) {
     *     processTask(task);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果列表为空，返回空列表（不会抛出异常）</li>
     *     <li>如果列表元素少于 k 个，返回所有元素</li>
     *     <li>k 必须大于 0</li>
     *     <li>返回的元素顺序与列表中的顺序一致（从左到右）</li>
     *     <li>注意：此方法返回 void，实际返回类型需要根据 RedisTemplate 的实现确认</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @param k   删除元素的数量，必须大于 0
     * @see #leftPopForList(String)
     * @see #rightPopForList(String, long)
     */
    public void leftPopForList(final String key, final long k) {
        try {
            redisTemplate.opsForList().leftPop(key, k);
        } catch (Exception e) {
            log.warn("RedisService.leftPopForList left pop k redis error: {}", e.getMessage());
        }
    }

    /**
     * 删除并返回列表右侧第一个元素（尾删）
     * <p>
     * 从列表右侧（尾部）删除并返回最后一个元素。如果列表为空，返回 null。
     * 适用于实现队列（FIFO）数据结构。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实现队列：右侧取出（后进先出）
     * Task task = redisService.rightPopForList("queue:task");
     * if (task != null) {
     *     // 处理任务
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果列表为空，返回 null（不会抛出异常）</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>这是一个阻塞操作，如果列表为空会立即返回 null</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @see #leftPopForList(String)
     * @see #rightPopForList(String, long)
     */
    public void rightPopForList(final String key) {
        try {
            redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            log.warn("RedisService.rightPopForList right pop redis error: {}", e.getMessage());
        }
    }

    /**
     * 删除并返回列表右侧 k 个元素（尾删）
     * <p>
     * 从列表右侧（尾部）删除并返回后 k 个元素。如果列表元素少于 k 个，返回所有元素。
     * 适用于批量处理列表末尾的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量取出列表末尾的元素（最多5个）
     * // 注意：此方法返回 void，实际返回类型需要根据 RedisTemplate 的实现确认
     * redisService.rightPopForList("list:data", 5);
     *
     * // 批量处理日志列表（每次处理最后10条）
     * redisService.rightPopForList("log:list", 10);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果列表为空，操作会静默成功（不会抛出异常）</li>
     *     <li>如果列表元素少于 k 个，删除所有元素</li>
     *     <li>k 必须大于 0</li>
     *     <li>注意：此方法返回 void，实际返回类型需要根据 RedisTemplate 的实现确认</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @param k   删除元素的数量，必须大于 0
     * @see #rightPopForList(String)
     * @see #leftPopForList(String, long)
     */
    public void rightPopForList(final String key, final long k) {
        try {
            redisTemplate.opsForList().rightPop(key, k);
        } catch (Exception e) {
            log.warn("RedisService.rightPopForList right pop k redis error: {}", e.getMessage());
        }
    }

    /**
     * 移除列表中第一个匹配的元素（从左到右）
     * <p>
     * 从列表左侧开始查找，移除第一个与指定值相等的元素。
     * 适用于删除列表中的特定元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 移除列表中第一个匹配的任务
     * Long removed = redisService.removeLeftForList("queue:task", task);
     * if (removed > 0) {
     *     log.info("任务已移除");
     * }
     *
     * // 移除列表中第一个匹配的用户ID
     * Long removed = redisService.removeLeftForList("user:ids", 123L);
     *
     * // 移除列表中第一个匹配的字符串
     * Long removed = redisService.removeLeftForList("list:items", "item1");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只移除第一个匹配的元素（从左侧开始）</li>
     *     <li>如果列表中有多个相同的元素，只移除第一个</li>
     *     <li>返回 0 表示没有找到匹配的元素</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要移除的元素值，不能为 null
     * @param <T>   值类型
     * @return 移除的元素数量，0 表示未找到，-1 表示操作失败
     * @see #removeLeftForList(String, Object, long)
     * @see #removeRightForList(String, Object)
     * @see #removeAllForList(String, Object)
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
     * 移除列表中前 k 个匹配的元素（从左到右）
     * <p>
     * 从列表左侧开始查找，移除前 k 个与指定值相等的元素。
     * 适用于批量删除列表中的特定元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 移除列表中前3个匹配的任务
     * Long removed = redisService.removeLeftForList("queue:task", task, 3);
     * log.info("移除了 {} 个任务", removed);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>从左侧开始查找，最多移除 k 个匹配的元素</li>
     *     <li>如果匹配的元素少于 k 个，移除所有匹配的元素</li>
     *     <li>返回实际移除的元素数量</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>k 必须大于 0</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要移除的元素值，不能为 null
     * @param k     删除元素的数量，必须大于 0
     * @param <T>   值类型
     * @return 移除的元素数量，-1 表示操作失败
     * @see #removeLeftForList(String, Object)
     * @see #removeRightForList(String, Object, long)
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
     * 移除列表中第一个匹配的元素（从右到左）
     * <p>
     * 从列表右侧开始查找，移除第一个与指定值相等的元素。
     * 适用于删除列表末尾的特定元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 移除列表中最后一个匹配的任务
     * Long removed = redisService.removeRightForList("queue:task", task);
     * if (removed > 0) {
     *     log.info("成功移除任务");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只移除第一个匹配的元素（从右到左）</li>
     *     <li>如果列表中有多个相同的元素，只移除最后一个</li>
     *     <li>返回 0 表示没有找到匹配的元素</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要移除的元素值，不能为 null
     * @param <T>   值类型
     * @return 移除的元素数量，0 表示未找到，-1 表示操作失败
     * @see #removeLeftForList(String, Object)
     * @see #removeAllForList(String, Object)
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
     * 移除列表中前 k 个匹配的元素（从右到左）
     * <p>
     * 从列表右侧开始查找，移除前 k 个与指定值相等的元素。
     * 适用于批量删除列表末尾的特定元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 移除列表中后3个匹配的任务
     * Long removed = redisService.removeRightForList("queue:task", task, 3);
     * log.info("移除了 {} 个任务", removed);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>从右侧开始查找，最多移除 k 个匹配的元素</li>
     *     <li>如果匹配的元素少于 k 个，移除所有匹配的元素</li>
     *     <li>返回实际移除的元素数量</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>k 必须大于 0</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要移除的元素值，不能为 null
     * @param k     移除的元素数量，必须大于 0
     * @param <T>   值类型
     * @return 移除的元素数量，-1 表示操作失败
     * @see #removeRightForList(String, Object)
     * @see #removeLeftForList(String, Object, long)
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
     * 移除列表中所有匹配的元素
     * <p>
     * 从列表中删除所有与指定值相等的元素。适用于批量删除列表中的特定元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 移除列表中所有匹配的任务
     * Long removed = redisService.removeAllForList("queue:task", task);
     * log.info("移除了 {} 个任务", removed);
     *
     * // 移除列表中所有匹配的用户ID
     * Long removed = redisService.removeAllForList("user:ids", 123L);
     *
     * // 清理列表中的重复项
     * Long removed = redisService.removeAllForList("list:items", "duplicate_item");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>移除列表中所有匹配的元素</li>
     *     <li>返回实际移除的元素数量</li>
     *     <li>返回 0 表示没有找到匹配的元素</li>
     *     <li>返回 -1 表示操作失败</li>
     *     <li>元素比较使用 equals 方法</li>
     *     <li>如果列表很大且匹配元素很多，此操作可能较慢</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要移除的元素值，不能为 null
     * @param <T>   值类型
     * @return 移除的元素数量，-1 表示操作失败
     * @see #removeLeftForList(String, Object)
     * @see #removeRightForList(String, Object)
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
     * <p>
     * 清空列表，删除所有元素。列表键仍然存在，但为空列表。
     * 适用于清空列表数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 清空任务队列
     * redisService.removeForAllList("queue:task");
     *
     * // 清空日志列表
     * redisService.removeForAllList("log:list");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>清空列表中的所有元素</li>
     *     <li>列表键仍然存在，但为空列表</li>
     *     <li>如果 key 不存在，操作会静默成功（不会抛出异常）</li>
     *     <li>如果需要删除整个键，使用 {@link #deleteObject(String)}</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @see #deleteObject(String)
     * @see #retainListRange(String, long, long)
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
     * 保留指定范围内的元素（裁剪列表）
     * <p>
     * 只保留列表中指定范围内的元素，删除范围外的所有元素。
     * 适用于截取列表的某一部分。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 只保留前10个元素（索引0-9）
     * redisService.retainListRange("list:data", 0, 9);
     *
     * // 只保留最后5个元素
     * redisService.retainListRange("list:data", -5, -1);
     *
     * // 保留中间的元素（索引5-14）
     * redisService.retainListRange("list:data", 5, 14);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只保留指定范围内的元素，删除范围外的所有元素</li>
     *     <li>start 和 end 都包含在范围内</li>
     *     <li>如果 start > end，会删除所有元素（等同于 {@link #removeForAllList(String)}）</li>
     *     <li>如果 start 或 end 超出列表范围，会保留有效的部分</li>
     *     <li>负数索引从列表末尾开始计算（-1 表示最后一个元素）</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param start 开始下标（包含），0 表示第一个元素，-1 表示最后一个元素
     * @param end   结束下标（包含），-1 表示最后一个元素
     * @see #removeForAllList(String)
     * @see #getCacheListByRange(String, long, long, Class)
     */
    public void retainListRange(final String key, final long start, final long end) {
        try {
            redisTemplate.opsForList().trim(key, start, end);
        } catch (Exception e) {
            log.warn("RedisService.retainListRange redis error: {}", e.getMessage());
        }
    }

    /**
     * 修改列表中指定索引位置的元素
     * <p>
     * 将列表中指定索引位置的元素替换为新值。如果索引超出范围，操作会失败。
     * 适用于更新列表中的特定元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 修改索引为0的元素（第一个元素）
     * redisService.setElementAtIndex("list:data", 0, newData);
     *
     * // 修改最后一个元素
     * long size = redisService.getCacheListSize("list:data");
     * if (size > 0) {
     *     redisService.setElementAtIndex("list:data", (int)(size - 1), newData);
     * }
     *
     * // 更新任务列表中的某个任务
     * redisService.setElementAtIndex("task:list", 2, updatedTask);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>索引从 0 开始，0 表示第一个元素</li>
     *     <li>如果索引超出范围（负数或大于等于列表长度），操作会失败（不会抛出异常）</li>
     *     <li>如果 key 不存在，操作会失败</li>
     *     <li>负数索引不支持，需要使用正数索引</li>
     *     <li>操作是原子性的</li>
     * </ul>
     *
     * @param key      缓存键，不能为 null
     * @param index    下标（索引），从 0 开始，必须大于等于 0
     * @param newValue 修改后的新值，不能为 null
     * @param <T>      值类型
     * @see #getCacheListByRange(String, long, long, Class)
     * @see #getCacheListSize(String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取所有用户列表
     * List<User> users = redisService.getCacheList("list:users", User.class);
     *
     * // 获取所有任务列表
     * List<Task> tasks = redisService.getCacheList("list:tasks", Task.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>返回的列表保持原有顺序</li>
     *     <li>适用于简单类型，复杂泛型请使用 {@link #getCacheList(String, TypeReference)}</li>
     *     <li>如果列表很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param clazz 元素的类型，不能为 null
     * @param <T>   元素的类型
     * @return 列表数据，如果键不存在或发生错误，返回 null
     * @see #getCacheList(String, TypeReference)
     * @see #getCacheListByRange(String, long, long, Class)
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
     * 返回列表中所有元素，保持原有顺序。使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取复杂泛型列表
     * TypeReference<List<Map<String, User>>> typeRef = new TypeReference<List<Map<String, User>>>() {};
     * List<Map<String, User>> complexList = redisService.getCacheList("list:complex", typeRef);
     *
     * // 获取简单泛型列表
     * TypeReference<List<User>> typeRef = new TypeReference<List<User>>() {};
     * List<User> users = redisService.getCacheList("list:users", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>返回的列表保持原有顺序</li>
     *     <li>适用于复杂泛型类型，简单类型可以使用 {@link #getCacheList(String, Class)}</li>
     *     <li>如果列表很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           元素的类型
     * @return 列表数据，如果键不存在或发生错误，返回 null
     * @see #getCacheList(String, Class)
     * @see #getCacheListByRange(String, long, long, TypeReference)
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
     * 根据范围获取列表数据
     * <p>
     * 获取列表中指定范围内的元素，保持原有顺序。适用于简单的对象类型（非泛型嵌套）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取前10个元素（索引0-9）
     * List<User> users = redisService.getCacheListByRange("list:users", 0, 9, User.class);
     *
     * // 获取最后5个元素（索引-5到-1）
     * List<User> lastUsers = redisService.getCacheListByRange("list:users", -5, -1, User.class);
     *
     * // 获取中间的元素（索引5-14）
     * List<Task> tasks = redisService.getCacheListByRange("list:tasks", 5, 14, Task.class);
     *
     * // 获取所有元素（索引0到-1）
     * List<String> allItems = redisService.getCacheListByRange("list:items", 0, -1, String.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>start 和 end 都包含在范围内</li>
     *     <li>如果 start > end，返回 null</li>
     *     <li>负数索引从列表末尾开始计算（-1 表示最后一个元素）</li>
     *     <li>如果范围超出列表，返回有效的部分</li>
     *     <li>适用于简单类型，复杂泛型请使用 {@link #getCacheListByRange(String, long, long, TypeReference)}</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param start 开始下标（包含），0 表示第一个元素，-1 表示最后一个元素
     * @param end   结束下标（包含），-1 表示最后一个元素
     * @param clazz 元素的类型，不能为 null
     * @param <T>   类型
     * @return List 列表，如果 start > end 或发生错误则返回 null
     * @see #getCacheListByRange(String, long, long, TypeReference)
     * @see #getCacheList(String, Class)
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
     * 根据范围获取列表数据（支持复杂泛型嵌套）
     * <p>
     * 获取列表中指定范围内的元素，保持原有顺序。使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取前10个元素（索引0-9）
     * TypeReference<List<User>> typeRef = new TypeReference<List<User>>() {};
     * List<User> users = redisService.getCacheListByRange("list:users", 0, 9, typeRef);
     *
     * // 获取最后5个元素
     * List<User> lastUsers = redisService.getCacheListByRange("list:users", -5, -1, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>start 和 end 都包含在范围内</li>
     *     <li>如果 start > end，返回 null</li>
     *     <li>负数索引从列表末尾开始计算（-1 表示最后一个元素）</li>
     *     <li>如果范围超出列表，返回有效的部分</li>
     *     <li>适用于复杂泛型类型，简单类型可以使用 {@link #getCacheListByRange(String, long, long, Class)}</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param start         开始下标（包含），0 表示第一个元素，-1 表示最后一个元素
     * @param end           结束下标（包含），-1 表示最后一个元素
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           类型信息
     * @return List 列表，如果 start > end 或发生错误则返回 null
     * @see #getCacheListByRange(String, long, long, Class)
     * @see #getCacheList(String, TypeReference)
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
     * 获取指定列表的长度
     * <p>
     * 返回列表中元素的数量。如果键不存在，返回 0。
     * 适用于检查列表是否为空或获取列表大小。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取列表长度
     * long size = redisService.getCacheListSize("list:data");
     * if (size > 0) {
     *     log.info("列表中有 {} 个元素", size);
     * }
     *
     * // 检查列表是否为空
     * if (redisService.getCacheListSize("queue:task") == 0) {
     *     log.info("任务队列为空");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 0</li>
     *     <li>如果 key 不是列表类型，可能返回 0 或抛出异常</li>
     *     <li>返回 0 表示列表为空或键不存在</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @return 列表长度，如果键不存在或发生错误返回 0
     * @see #getCacheList(String, Class)
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
     * 向 Set 集合中添加元素（支持批量添加）
     * <p>
     * 向集合中添加一个或多个元素。如果元素已存在，会被忽略（不会重复添加）。
     * Set 集合中的元素是无序的且不重复的。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 添加单个元素
     * Long added = redisService.addMember("set:tags", "java");
     *
     * // 批量添加元素
     * Long added = redisService.addMember("set:tags", "java", "python", "go");
     *
     * // 添加对象元素
     * Long added = redisService.addMember("set:users", user1, user2);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果元素已存在，不会被重复添加，但不会抛出异常</li>
     *     <li>返回实际添加的元素数量（不包括已存在的元素）</li>
     *     <li>如果所有元素都已存在，返回 0</li>
     *     <li>如果 key 不存在，会创建新的集合</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param key    缓存键，不能为 null
     * @param member 要添加的元素，可以是多个，不能为 null
     * @return 实际添加的元素个数（不包括已存在的），如果发生错误返回 0
     * @see #deleteMember(String, Object...)
     * @see #isMember(String, Object)
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
     * 从 Set 中删除元素
     * <p>
     * 从集合中删除一个或多个元素。如果元素不存在，会被忽略。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除单个元素
     * Long deleted = redisService.deleteMember("set:tags", "java");
     *
     * // 批量删除元素
     * Long deleted = redisService.deleteMember("set:tags", "java", "python");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果元素不存在，会被忽略（不会抛出异常）</li>
     *     <li>返回实际删除的元素数量</li>
     *     <li>如果所有元素都不存在，返回 0</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param key    缓存键，不能为 null
     * @param member 要删除的元素，可以是多个，不能为 null
     * @return 实际删除的元素个数，如果发生错误返回 0
     * @see #addMember(String, Object...)
     * @see #isMember(String, Object)
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
     * 检查 Set 集合中的某个元素是否存在
     * <p>
     * 判断指定元素是否存在于集合中。适用于检查元素是否已添加。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 检查标签是否存在
     * if (redisService.isMember("set:tags", "java")) {
     *     log.info("标签已存在");
     * }
     *
     * // 检查用户是否在集合中
     * if (redisService.isMember("set:users", user)) {
     *     log.info("用户已在集合中");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 false</li>
     *     <li>如果元素不存在，返回 false</li>
     *     <li>元素比较使用 equals 方法</li>
     *     <li>时间复杂度为 O(1)</li>
     * </ul>
     *
     * @param key    缓存键，不能为 null
     * @param member 要检查的元素，不能为 null
     * @return true 表示元素存在，false 表示元素不存在或发生错误
     * @see #addMember(String, Object...)
     * @see #deleteMember(String, Object...)
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
     * 获取 Set 中所有元素（支持复杂泛型嵌套）
     * <p>
     * 返回集合中的所有元素。使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取简单类型集合
     * TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
     * Set<String> tags = redisService.getCacheSet("set:tags", typeRef);
     *
     * // 获取复杂类型集合
     * TypeReference<Set<User>> typeRef = new TypeReference<Set<User>>() {};
     * Set<User> users = redisService.getCacheSet("set:users", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>集合中的元素是无序的</li>
     *     <li>返回的集合可能为空集合（如果集合中没有元素）</li>
     *     <li>如果集合很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           类型信息
     * @return Set 数据，如果键不存在或发生错误返回 null
     * @see #getCacheSetSize(String)
     * @see #isMember(String, Object)
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
     * 获取 Set 集合中元素的数量
     * <p>
     * 返回集合中元素的数量。如果键不存在，返回 0。
     * 适用于检查集合是否为空或获取集合大小。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取集合大小
     * Long size = redisService.getCacheSetSize("set:tags");
     * log.info("集合中有 {} 个元素", size);
     *
     * // 检查集合是否为空
     * if (redisService.getCacheSetSize("set:tags") == 0) {
     *     log.info("集合为空");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 0</li>
     *     <li>如果 key 不是集合类型，可能返回 0 或抛出异常</li>
     *     <li>时间复杂度为 O(1)</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @return 集合中元素的数量，如果键不存在或发生错误返回 0
     * @see #getCacheSet(String, TypeReference)
     * @see #addMember(String, Object...)
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
     * <p>
     * 返回两个集合中都存在的元素。交集是指同时属于两个集合的元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取两个标签集合的交集
     * TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
     * Set<String> commonTags = redisService.intersectToCacheSet("set:tags1", "set:tags2", typeRef);
     *
     * // 获取两个用户集合的交集
     * TypeReference<Set<User>> typeRef = new TypeReference<Set<User>>() {};
     * Set<User> commonUsers = redisService.intersectToCacheSet("set:users1", "set:users2", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果任一集合不存在，返回空集合</li>
     *     <li>如果两个集合没有共同元素，返回空集合</li>
     *     <li>时间复杂度为 O(N*M)，其中 N 和 M 是两个集合的大小</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param setKey1       第一个集合的键，不能为 null
     * @param setKey2       第二个集合的键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           泛型类型
     * @return 交集结果，如果发生错误返回 null
     * @see #unionToCacheSet(String, String, TypeReference)
     * @see #differenceToCacheSet(String, String, TypeReference)
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
     * <p>
     * 返回两个集合中所有不重复的元素。并集是指属于任一集合的元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取两个标签集合的并集
     * TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
     * Set<String> allTags = redisService.unionToCacheSet("set:tags1", "set:tags2", typeRef);
     *
     * // 合并两个用户集合
     * TypeReference<Set<User>> typeRef = new TypeReference<Set<User>>() {};
     * Set<User> allUsers = redisService.unionToCacheSet("set:users1", "set:users2", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果两个集合都不存在，返回空集合</li>
     *     <li>如果只有一个集合存在，返回该集合的所有元素</li>
     *     <li>结果中不会包含重复元素</li>
     *     <li>时间复杂度为 O(N+M)，其中 N 和 M 是两个集合的大小</li>
     * </ul>
     *
     * @param setKey1       第一个集合的键，不能为 null
     * @param setKey2       第二个集合的键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           泛型类型
     * @return 并集结果，如果发生错误返回 null
     * @see #intersectToCacheSet(String, String, TypeReference)
     * @see #differenceToCacheSet(String, String, TypeReference)
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
     * <p>
     * 返回属于第一个集合但不属于第二个集合的元素。差集是指只属于第一个集合的元素。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取第一个集合相对于第二个集合的差集
     * TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
     * Set<String> diffTags = redisService.differenceToCacheSet("set:tags1", "set:tags2", typeRef);
     *
     * // 找出只在第一个集合中的用户
     * TypeReference<Set<User>> typeRef = new TypeReference<Set<User>>() {};
     * Set<User> diffUsers = redisService.differenceToCacheSet("set:users1", "set:users2", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果第一个集合不存在，返回空集合</li>
     *     <li>如果第二个集合不存在，返回第一个集合的所有元素</li>
     *     <li>时间复杂度为 O(N*M)，其中 N 和 M 是两个集合的大小</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param setKey1       第一个集合的键，不能为 null
     * @param setKey2       第二个集合的键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           泛型类型
     * @return 差集结果（第一个集合 - 第二个集合），如果发生错误返回 null
     * @see #intersectToCacheSet(String, String, TypeReference)
     * @see #unionToCacheSet(String, String, TypeReference)
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
     * 将元素从源集合移动到目标集合
     * <p>
     * 将元素从源集合中删除，并添加到目标集合中。这是一个原子操作。
     * 如果元素在源集合中不存在，操作会失败。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将用户从待处理集合移动到已完成集合
     * Boolean moved = redisService.moveMemberCacheSet("set:pending", "set:completed", user);
     * if (moved) {
     *     log.info("用户已移动到已完成集合");
     * }
     *
     * // 将任务从一个队列移动到另一个队列
     * Boolean moved = redisService.moveMemberCacheSet("set:queue1", "set:queue2", task);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，要么全部成功，要么全部失败</li>
     *     <li>如果元素在源集合中不存在，返回 false</li>
     *     <li>如果目标集合不存在，会创建新的集合</li>
     *     <li>如果源集合和目标集合相同，操作会成功（元素仍然存在）</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param sourceKey      源集合的键，不能为 null
     * @param destinationKey 目标集合的键，不能为 null
     * @param member         要移动的元素，不能为 null
     * @return true 表示移动成功，false 表示元素不存在或发生错误
     * @see #addMember(String, Object...)
     * @see #deleteMember(String, Object...)
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
     * 向有序集合（ZSet）中添加元素
     * <p>
     * 向有序集合中添加元素，并指定分数。如果元素已存在，会更新其分数。
     * 有序集合中的元素按分数排序，分数可以相同。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 添加用户及其积分
     * redisService.addMemberZSet("zset:ranking", user, 100.0);
     *
     * // 添加任务及其优先级
     * redisService.addMemberZSet("zset:tasks", task, priority);
     *
     * // 更新元素分数（如果元素已存在）
     * redisService.addMemberZSet("zset:ranking", user, 150.0);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果元素已存在，会更新其分数</li>
     *     <li>分数可以是整数或浮点数</li>
     *     <li>元素按分数从小到大排序</li>
     *     <li>如果 key 不存在，会创建新的有序集合</li>
     *     <li>返回 true 表示添加成功，false 表示操作失败</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 元素值，不能为 null
     * @param seqNo 分数（排序值），可以是整数或浮点数
     * @return true 表示添加成功，false 表示操作失败
     * @see #delMemberZSet(String, Object)
     * @see #incrementZSetScore(String, Object, double)
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
     * 从有序集合（ZSet）中删除元素
     * <p>
     * 从有序集合中删除指定的元素。如果元素不存在，操作会失败。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除排行榜中的用户
     * Long deleted = redisService.delMemberZSet("zset:ranking", user);
     * if (deleted > 0) {
     *     log.info("用户已从排行榜中删除");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果元素不存在，返回 0</li>
     *     <li>返回实际删除的元素数量（通常为 0 或 1）</li>
     *     <li>元素比较使用 equals 方法</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 要删除的元素值，不能为 null
     * @return 删除的元素数量，如果发生错误返回 0
     * @see #addMemberZSet(String, Object, double)
     * @see #removeZSetByScore(String, double, double)
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
     * 按索引范围获取有序集合元素（升序，支持复杂泛型嵌套）
     * <p>
     * 根据索引范围获取有序集合中的元素，按分数从小到大排序。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取排行榜前10名（索引0-9）
     * TypeReference<LinkedHashSet<User>> typeRef = new TypeReference<LinkedHashSet<User>>() {};
     * Set<User> top10 = redisService.getZSetRange("zset:ranking", 0, 9, typeRef);
     *
     * // 获取所有元素
     * Set<User> all = redisService.getZSetRange("zset:ranking", 0, -1, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>元素按分数从小到大排序（升序）</li>
     *     <li>start 和 end 都包含在范围内</li>
     *     <li>-1 表示最后一个元素</li>
     *     <li>如果 start > end，返回空集合</li>
     *     <li>返回 LinkedHashSet 保持顺序</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param start         起始索引（包含），0 表示第一个元素，-1 表示最后一个元素
     * @param end           结束索引（包含），-1 表示最后一个元素
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 缓存对象集合（按分数升序），如果发生错误返回 null
     * @see #getZSetRangeDesc(String, long, long, TypeReference)
     * @see #getCacheZSet(String, TypeReference)
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
     * 获取所有有序集合数据（升序，支持复杂泛型嵌套）
     * <p>
     * 返回有序集合中的所有元素，按分数从小到大排序。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取排行榜所有用户（按分数升序，分数低的在前）
     * TypeReference<LinkedHashSet<User>> typeRef = new TypeReference<LinkedHashSet<User>>() {};
     * Set<User> allUsers = redisService.getCacheZSet("zset:ranking", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>元素按分数从小到大排序（升序）</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>返回 LinkedHashSet 保持顺序</li>
     *     <li>如果集合很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 有序集合（按分数升序），如果键不存在或发生错误返回 null
     * @see #getCacheZSetDesc(String, TypeReference)
     * @see #getZSetRange(String, long, long, TypeReference)
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
     * 按索引范围获取有序集合元素（降序，支持复杂泛型嵌套）
     * <p>
     * 根据索引范围获取有序集合中的元素，按分数从大到小排序。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取排行榜前10名（按分数降序，分数高的在前）
     * TypeReference<LinkedHashSet<User>> typeRef = new TypeReference<LinkedHashSet<User>>() {};
     * Set<User> top10 = redisService.getZSetRangeDesc("zset:ranking", 0, 9, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>元素按分数从大到小排序（降序）</li>
     *     <li>start 和 end 都包含在范围内</li>
     *     <li>-1 表示最后一个元素</li>
     *     <li>如果 start > end，返回空集合</li>
     *     <li>返回 LinkedHashSet 保持顺序</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param start         起始索引（包含），0 表示第一个元素（分数最高的）
     * @param end           结束索引（包含），-1 表示最后一个元素
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 缓存对象集合（按分数降序），如果发生错误返回 null
     * @see #getZSetRange(String, long, long, TypeReference)
     * @see #getCacheZSetDesc(String, TypeReference)
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
     * 获取所有有序集合数据（降序，支持复杂泛型嵌套）
     * <p>
     * 返回有序集合中的所有元素，按分数从大到小排序。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取排行榜所有用户（按分数降序，分数高的在前）
     * TypeReference<LinkedHashSet<User>> typeRef = new TypeReference<LinkedHashSet<User>>() {};
     * Set<User> allUsers = redisService.getCacheZSetDesc("zset:ranking", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>元素按分数从大到小排序（降序）</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>返回 LinkedHashSet 保持顺序</li>
     *     <li>如果集合很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 降序的有序集合（按分数降序），如果键不存在或发生错误返回 null
     * @see #getCacheZSet(String, TypeReference)
     * @see #getZSetRangeDesc(String, long, long, TypeReference)
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
     * 获取有序集合（ZSet）的大小
     * <p>
     * 返回有序集合中元素的数量。如果键不存在，返回 0。
     * 适用于检查有序集合是否为空或获取集合大小。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取排行榜大小
     * Long size = redisService.getZSetSize("zset:ranking");
     * log.info("排行榜中有 {} 个用户", size);
     *
     * // 检查排行榜是否为空
     * if (redisService.getZSetSize("zset:ranking") == 0) {
     *     log.info("排行榜为空");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 0</li>
     *     <li>如果 key 不是有序集合类型，可能返回 0 或抛出异常</li>
     *     <li>时间复杂度为 O(1)</li>
     * </ul>
     *
     * @param key 缓存键，不能为 null
     * @return 有序集合中元素的数量，如果键不存在或发生错误返回 0
     * @see #getCacheZSet(String, TypeReference)
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
     * 增加有序集合中元素的分数
     * <p>
     * 为有序集合中的元素增加指定分数。如果元素不存在，会先添加元素并设置分数。
     * 如果元素已存在，会在原有分数基础上增加。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 增加用户积分
     * Double newScore = redisService.incrementZSetScore("zset:ranking", user, 10.0);
     * log.info("用户新分数: {}", newScore);
     *
     * // 减少用户积分（使用负数）
     * Double newScore = redisService.incrementZSetScore("zset:ranking", user, -5.0);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果元素不存在，会先添加元素并设置分数为 delta</li>
     *     <li>如果元素已存在，会在原有分数基础上增加 delta</li>
     *     <li>delta 可以为负数（相当于减少分数）</li>
     *     <li>返回增加后的新分数</li>
     *     <li>这是一个原子操作，线程安全</li>
     * </ul>
     *
     * @param key    缓存键，不能为 null
     * @param member 元素值，不能为 null
     * @param delta  增加的分数，可以为负数（相当于减少）
     * @return 增加后的新分数，如果发生错误返回 null
     * @see #addMemberZSet(String, Object, double)
     * @see #getZSetScore(String, Object)
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
     * 获取有序集合中元素的分数
     * <p>
     * 返回有序集合中指定元素的分数。如果元素不存在，返回 null。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户积分
     * Double score = redisService.getZSetScore("zset:ranking", user);
     * if (score != null) {
     *     log.info("用户积分: {}", score);
     * } else {
     *     log.info("用户不在排行榜中");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果元素不存在，返回 null</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>元素比较使用 equals 方法</li>
     *     <li>时间复杂度为 O(1)</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null
     * @param value 元素值，不能为 null
     * @return 元素的分数，如果元素不存在或发生错误返回 null
     * @see #incrementZSetScore(String, Object, double)
     * @see #getZSetRank(String, Object)
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
     * 获取有序集合中元素的排名（升序）
     * <p>
     * 返回元素在有序集合中的排名（索引），按分数从小到大排序。
     * 排名从 0 开始，分数最小的元素排名为 0。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户在排行榜中的排名（升序，分数低的在前）
     * Long rank = redisService.getZSetRank("zset:ranking", user);
     * if (rank != null) {
     *     log.info("用户排名: {}", rank + 1); // 显示排名（从1开始）
     * } else {
     *     log.info("用户不在排行榜中");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>排名从 0 开始，分数最小的元素排名为 0</li>
     *     <li>如果元素不存在，返回 null</li>
     *     <li>如果多个元素分数相同，排名按插入顺序或元素值排序</li>
     *     <li>时间复杂度为 O(log(N))</li>
     * </ul>
     *
     * @param key    缓存键，不能为 null
     * @param member 元素值，不能为 null
     * @return 排名（从 0 开始），如果元素不存在返回 null
     * @see #getZSetReverseRank(String, Object)
     * @see #getZSetScore(String, Object)
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
     * 获取有序集合中元素的排名（降序）
     * <p>
     * 返回元素在有序集合中的排名（索引），按分数从大到小排序。
     * 排名从 0 开始，分数最高的元素排名为 0。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户在排行榜中的排名（降序，分数高的在前）
     * Long rank = redisService.getZSetReverseRank("zset:ranking", user);
     * if (rank != null) {
     *     log.info("用户排名: {}", rank + 1); // 显示排名（从1开始）
     * } else {
     *     log.info("用户不在排行榜中");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>排名从 0 开始，分数最高的元素排名为 0</li>
     *     <li>如果元素不存在，返回 null</li>
     *     <li>如果多个元素分数相同，排名按插入顺序或元素值排序</li>
     *     <li>时间复杂度为 O(log(N))</li>
     *     <li>适用于排行榜场景（分数高的排名靠前）</li>
     * </ul>
     *
     * @param key    缓存键，不能为 null
     * @param member 元素值，不能为 null
     * @return 排名（从 0 开始），如果元素不存在返回 null
     * @see #getZSetRank(String, Object)
     * @see #getZSetScore(String, Object)
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
     * 按分数范围获取有序集合元素（升序，支持复杂泛型）
     * <p>
     * 获取分数在指定范围内的元素，按分数从小到大排序。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取积分在 100-200 之间的用户（按分数升序）
     * TypeReference<LinkedHashSet<User>> typeRef = new TypeReference<LinkedHashSet<User>>() {};
     * Set<User> users = redisService.getZSetRangeByScore("zset:ranking", 100.0, 200.0, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>minScore 和 maxScore 都包含在范围内</li>
     *     <li>元素按分数从小到大排序（升序）</li>
     *     <li>如果范围内没有元素，返回空集合</li>
     *     <li>返回 LinkedHashSet 保持顺序</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param minScore      最小分数（包含），不能为 null
     * @param maxScore      最大分数（包含），不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           泛型类型
     * @return 元素集合（按分数升序），如果发生错误返回 null
     * @see #getZSetReverseRangeByScore(String, double, double, TypeReference)
     * @see #getZSetRange(String, long, long, TypeReference)
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
     * 按分数范围获取有序集合元素（降序，支持复杂泛型嵌套）
     * <p>
     * 获取分数在指定范围内的元素，按分数从大到小排序。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取积分在 100-200 之间的用户（按分数降序，分数高的在前）
     * TypeReference<LinkedHashSet<User>> typeRef = new TypeReference<LinkedHashSet<User>>() {};
     * Set<User> users = redisService.getZSetReverseRangeByScore("zset:ranking", 100.0, 200.0, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>minScore 和 maxScore 都包含在范围内</li>
     *     <li>元素按分数从大到小排序（降序）</li>
     *     <li>如果范围内没有元素，返回空集合</li>
     *     <li>返回 LinkedHashSet 保持顺序</li>
     * </ul>
     *
     * @param key           缓存键，不能为 null
     * @param minScore      最小分数（包含），不能为 null
     * @param maxScore      最大分数（包含），不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           泛型类型
     * @return 元素集合（按分数降序），如果发生错误返回 null
     * @see #getZSetRangeByScore(String, double, double, TypeReference)
     * @see #getZSetRangeDesc(String, long, long, TypeReference)
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
     * 根据分数范围删除有序集合中的元素
     * <p>
     * 删除分数在指定范围内的所有元素。适用于清理特定分数区间的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除积分在 0-50 之间的用户（清理低分用户）
     * Long deleted = redisService.removeZSetByScore("zset:ranking", 0.0, 50.0);
     * log.info("删除了 {} 个低分用户", deleted);
     *
     * // 删除所有负分用户
     * Long deleted = redisService.removeZSetByScore("zset:ranking", Double.NEGATIVE_INFINITY, -1.0);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>minScore 和 maxScore 都包含在删除范围内</li>
     *     <li>返回实际删除的元素数量</li>
     *     <li>如果范围内没有元素，返回 0</li>
     *     <li>这是一个批量删除操作，如果范围很大可能较慢</li>
     * </ul>
     *
     * @param key      缓存键，不能为 null
     * @param minScore 最小分数（包含），不能为 null
     * @param maxScore 最大分数（包含），不能为 null
     * @return 删除的元素个数，如果发生错误返回 0
     * @see #delMemberZSet(String, Object)
     * @see #getZSetRangeByScore(String, double, double, TypeReference)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 存储用户信息（多个字段）
     * Map<String, Object> userInfo = new HashMap<>();
     * userInfo.put("name", "张三");
     * userInfo.put("age", 25);
     * userInfo.put("email", "zhangsan@example.com");
     * redisService.setCacheMap("user:123:info", userInfo);
     *
     * // 存储配置信息
     * Map<String, String> config = new HashMap<>();
     * config.put("app_name", "MyApp");
     * config.put("version", "1.0.0");
     * redisService.setCacheMap("config:app", config);
     *
     * // 批量更新用户信息
     * Map<String, Object> updates = new HashMap<>();
     * updates.put("name", "李四");
     * updates.put("phone", "13800138000");
     * redisService.setCacheMap("user:123:info", updates); // 会覆盖所有字段
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，会创建新的 Hash</li>
     *     <li>如果键已存在，会覆盖所有字段（原有字段会被删除）</li>
     *     <li>Hash 结构适合存储对象的多个属性</li>
     *     <li>如果需要只更新部分字段，使用 {@link #setCacheMapValue(String, String, Object)}</li>
     * </ul>
     *
     * @param key     Redis 键，不能为 null
     * @param dataMap 要存储的 Map 数据，key 为字段名，value 为字段值，不能为 null
     * @param <T>     值的类型
     * @see #setCacheMapValue(String, String, Object)
     * @see #getCacheMap(String, Class)
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
     * 适用于更新 Hash 中的单个字段，不影响其他字段。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 更新用户信息中的单个字段
     * redisService.setCacheMapValue("user:123:info", "name", "张三");
     * redisService.setCacheMapValue("user:123:info", "age", 25);
     * redisService.setCacheMapValue("user:123:info", "email", "zhangsan@example.com");
     *
     * // 更新配置项
     * redisService.setCacheMapValue("config:app", "version", "2.0.0");
     *
     * // 存储对象字段
     * redisService.setCacheMapValue("user:123:info", "address", address);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 Hash 键不存在，会创建新的 Hash</li>
     *     <li>如果字段已存在，会被覆盖</li>
     *     <li>只更新指定字段，不影响其他字段</li>
     *     <li>适用于部分更新场景</li>
     *     <li>如果需要批量更新，使用 {@link #setCacheMap(String, Map)}</li>
     * </ul>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKey  Hash 字段名，不能为 null
     * @param value 字段值，可以是任意类型，会自动序列化
     * @param <T>   值的类型
     * @see #setCacheMap(String, Map)
     * @see #getCacheMapValue(String, String)
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        try {
            redisTemplate.opsForHash().put(key, hKey, value);
        } catch (Exception e) {
            log.warn("RedisService.setCacheMapValue set cache error: {}", e.getMessage());
        }
    }

    /**
     * 删除 Hash 中的单个字段
     * <p>
     * 从 Hash 中删除指定的字段。如果字段不存在，操作会失败。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 删除用户信息中的某个字段
     * boolean deleted = redisService.deleteCacheMapValue("user:123:info", "email");
     * if (deleted) {
     *     log.info("字段删除成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果字段不存在，返回 false</li>
     *     <li>如果 key 不存在，返回 false</li>
     *     <li>只删除指定的字段，不影响其他字段</li>
     * </ul>
     *
     * @param key  Redis 键（Hash 的键），不能为 null
     * @param hKey Hash 字段名，不能为 null
     * @return true 表示删除成功，false 表示字段不存在或发生错误
     * @see #deleteCacheMapValues(String, Object...)
     * @see #setCacheMapValue(String, String, Object)
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
     * 删除 Hash 中的多个字段（批量删除）
     * <p>
     * 从 Hash 中删除指定的多个字段。如果某个字段不存在，会被忽略。
     * 适用于批量清理 Hash 中的字段。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量删除用户信息中的多个字段
     * Long deleted = redisService.deleteCacheMapValues("user:123:info", "email", "phone", "address");
     * log.info("删除了 {} 个字段", deleted);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果字段不存在，会被忽略（不会抛出异常）</li>
     *     <li>返回实际删除的字段数量</li>
     *     <li>如果所有字段都不存在，返回 0</li>
     *     <li>如果 key 不存在，返回 0</li>
     * </ul>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKeys Hash 字段名集合，可以是多个，不能为 null
     * @return 实际删除的字段数量，如果发生错误返回 0
     * @see #deleteCacheMapValue(String, String)
     * @see #setCacheMapValue(String, String, Object)
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
     * <p>
     * 对于复杂泛型类型（如 Map&lt;String, List&lt;User&gt;&gt;），请使用 {@link #getCacheMap(String, TypeReference)} 方法。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户信息的所有字段
     * Map<String, Object> userInfo = redisService.getCacheMap("user:123:info", Object.class);
     * if (userInfo != null) {
     *     String name = (String) userInfo.get("name");
     *     Integer age = (Integer) userInfo.get("age");
     *     log.info("用户: {}, 年龄: {}", name, age);
     * }
     *
     * // 获取配置信息
     * Map<String, String> config = redisService.getCacheMap("config:app", String.class);
     *
     * // 获取所有字段（值类型为 String）
     * Map<String, String> data = redisService.getCacheMap("hash:data", String.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 null</li>
     *     <li>适用于简单的对象类型（非泛型嵌套）</li>
     *     <li>对于复杂泛型类型，使用 {@link #getCacheMap(String, TypeReference)}</li>
     *     <li>返回的 Map 包含 Hash 中所有字段</li>
     * </ul>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param clazz 值的类型，不能为 null
     * @param <T>   值的类型
     * @return Hash 对应的 Map，如果键不存在或发生错误，返回 null
     * @see #getCacheMap(String, TypeReference)
     * @see #getCacheMapValue(String, String)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取复杂类型 Hash（值为 User 对象）
     * TypeReference<Map<String, User>> typeRef = new TypeReference<Map<String, User>>() {};
     * Map<String, User> userMap = redisService.getCacheMap("hash:users", typeRef);
     *
     * // 获取嵌套类型 Hash（值为 List<Order>）
     * TypeReference<Map<String, List<Order>>> typeRef = new TypeReference<Map<String, List<Order>>>() {};
     * Map<String, List<Order>> orderMap = redisService.getCacheMap("hash:orders", typeRef);
     *
     * // 获取简单类型 Hash（值为 String）
     * TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
     * Map<String, String> config = redisService.getCacheMap("config:app", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在，返回 null</li>
     *     <li>使用 TypeReference 可以正确处理泛型类型</li>
     *     <li>适用于复杂泛型嵌套类型</li>
     *     <li>简单类型可以使用 {@link #getCacheMap(String, Class)}</li>
     * </ul>
     *
     * @param key           Redis 键（Hash 的键），不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           值的类型
     * @return Hash 对应的 Map，如果键不存在或发生错误，返回 null
     * @see #getCacheMap(String, Class)
     * @see #getCacheMapValue(String, String, TypeReference)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户姓名
     * String name = redisService.getCacheMapValue("user:123:info", "name");
     *
     * // 获取用户年龄
     * Integer age = redisService.getCacheMapValue("user:123:info", "age");
     *
     * // 获取配置项
     * String version = redisService.getCacheMapValue("config:app", "version");
     *
     * // 获取对象字段
     * Address address = redisService.getCacheMapValue("user:123:info", "address", Address.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果字段不存在，返回 null</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>适用于简单的对象类型（非泛型嵌套）</li>
     *     <li>对于复杂泛型类型，使用 {@link #getCacheMapValue(String, String, TypeReference)}</li>
     * </ul>
     *
     * @param key  Redis 键（Hash 的键），不能为 null
     * @param hKey Hash 字段名，不能为 null
     * @param <T>  值的类型
     * @return 字段值，如果字段不存在或发生错误，返回 null
     * @see #getCacheMapValue(String, String, TypeReference)
     * @see #setCacheMapValue(String, String, Object)
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
     * 获取 Hash 中指定字段的值（支持复杂泛型嵌套）
     * <p>
     * 从 Hash 中获取指定字段的值。使用 TypeReference 可以正确处理泛型类型。
     * 如果字段不存在，返回 null。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取复杂类型字段
     * TypeReference<List<User>> typeRef = new TypeReference<List<User>>() {};
     * List<User> users = redisService.getCacheMapValue("user:123:info", "friends", typeRef);
     *
     * // 获取 Map 类型字段
     * TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
     * Map<String, String> metadata = redisService.getCacheMapValue("user:123:info", "metadata", typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果字段不存在，返回 null</li>
     *     <li>如果 key 不存在，返回 null</li>
     *     <li>适用于复杂泛型类型，简单类型可以使用 {@link #getCacheMapValue(String, String)}</li>
     * </ul>
     *
     * @param key           Redis 键（Hash 的键），不能为 null
     * @param hKey          Hash 字段名，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return Hash 中的对象，如果字段不存在或发生错误返回 null
     * @see #getCacheMapValue(String, String)
     * @see #setCacheMapValue(String, String, Object)
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
     * 获取 Hash 中的多个字段值（返回 List，支持简单类型）
     * <p>
     * 从 Hash 中批量获取多个字段的值，返回 List 集合。
     * 适用于简单的对象类型（非泛型嵌套）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量获取用户信息（返回 List）
     * Collection<String> fields = Arrays.asList("name", "age", "email");
     * List<Object> values = redisService.getMultiCacheMapValue("user:123:info", fields, Object.class);
     * if (values != null && values.size() == 3) {
     *     String name = (String) values.get(0);
     *     Integer age = (Integer) values.get(1);
     *     String email = (String) values.get(2);
     * }
     *
     * // 批量获取配置项（返回 List<String>）
     * Collection<String> configKeys = Arrays.asList("app_name", "version", "author");
     * List<String> configs = redisService.getMultiCacheMapValue("config:app", configKeys, String.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的 List 顺序与 hKeys 的顺序一致</li>
     *     <li>如果某个字段不存在，对应位置为 null</li>
     *     <li>适用于简单类型，复杂泛型请使用 {@link #getMultiCacheListValue(String, Collection, TypeReference)}</li>
     *     <li>如果需要返回 Map 格式，使用 {@link #getMultiCacheMapValue(String, Collection, TypeReference)}</li>
     * </ul>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKeys Hash 字段名集合，不能为 null
     * @param clazz 值的类型，不能为 null
     * @param <T>   对象类型
     * @return 获取的多个数据的 List 集合，如果发生错误返回 null
     * @see #getMultiCacheListValue(String, Collection, TypeReference)
     * @see #getMultiCacheMapValue(String, Collection, TypeReference)
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
     * 获取 Hash 中的多个字段值（返回 Map，支持复杂泛型嵌套）
     * <p>
     * 从 Hash 中批量获取多个字段的值，返回 Map 集合（key 为字段名，value 为字段值）。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量获取用户信息（返回 Map）
     * Collection<String> fields = Arrays.asList("name", "email", "friends");
     * TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
     * Map<String, Object> values = redisService.getMultiCacheMapValue("user:123:info", fields, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的 Map 只包含存在的字段（不存在的字段不会出现在 Map 中）</li>
     *     <li>Map 的 key 为字段名，value 为字段值</li>
     *     <li>适用于需要以 Map 形式返回的场景</li>
     * </ul>
     *
     * @param key           Redis 键（Hash 的键），不能为 null
     * @param hKeys         Hash 字段名集合，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 获取的多个数据的 Map 集合（key 为字段名），如果发生错误返回 null
     * @see #getMultiCacheListValue(String, Collection, TypeReference)
     * @see #getMultiCacheSetValue(String, Collection, TypeReference)
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
     * 获取 Hash 中的多个字段值（返回 List，支持复杂泛型嵌套）
     * <p>
     * 从 Hash 中批量获取多个字段的值，返回 List 集合。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量获取用户列表
     * Collection<String> fields = Arrays.asList("user1", "user2", "user3");
     * TypeReference<List<User>> typeRef = new TypeReference<List<User>>() {};
     * List<User> users = redisService.getMultiCacheListValue("hash:users", fields, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的 List 顺序与 hKeys 的顺序一致</li>
     *     <li>如果某个字段不存在，对应位置为 null</li>
     *     <li>适用于需要保持顺序的场景</li>
     * </ul>
     *
     * @param key           Redis 键（Hash 的键），不能为 null
     * @param hKeys         Hash 字段名集合，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 获取的多个数据的 List 集合，如果发生错误返回 null
     * @see #getMultiCacheMapValue(String, Collection, TypeReference)
     * @see #getMultiCacheSetValue(String, Collection, TypeReference)
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
     * 获取 Hash 中的多个字段值（返回 Set，支持复杂泛型嵌套）
     * <p>
     * 从 Hash 中批量获取多个字段的值，返回 Set 集合（去重）。
     * 使用 TypeReference 可以正确处理泛型类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量获取标签集合（去重）
     * Collection<String> fields = Arrays.asList("tag1", "tag2", "tag3");
     * TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
     * Set<String> tags = redisService.getMultiCacheSetValue("hash:tags", fields, typeRef);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的 Set 会自动去重</li>
     *     <li>不存在的字段不会出现在 Set 中</li>
     *     <li>适用于需要去重的场景</li>
     * </ul>
     *
     * @param key           Redis 键（Hash 的键），不能为 null
     * @param hKeys         Hash 字段名集合，不能为 null
     * @param typeReference 类型引用，用于指定泛型类型，不能为 null
     * @param <T>           对象类型
     * @return 获取的多个数据的 Set 集合（去重），如果发生错误返回 null
     * @see #getMultiCacheListValue(String, Collection, TypeReference)
     * @see #getMultiCacheMapValue(String, Collection, TypeReference)
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
     * <p>
     * 返回 Hash 中字段的数量。如果键不存在，返回 0。
     * 适用于检查 Hash 是否为空或获取 Hash 大小。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户信息字段数量
     * Long size = redisService.getCacheMapSize("user:123:info");
     * log.info("用户信息有 {} 个字段", size);
     *
     * // 检查 Hash 是否为空
     * if (redisService.getCacheMapSize("user:123:info") == 0) {
     *     log.info("用户信息为空");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回 0</li>
     *     <li>如果 key 不是 Hash 类型，可能返回 0 或抛出异常</li>
     *     <li>时间复杂度为 O(1)</li>
     * </ul>
     *
     * @param key Redis 键（Hash 的键），不能为 null
     * @return 字段数量，如果键不存在或发生错误返回 0
     * @see #getCacheMap(String, Class)
     * @see #getCacheMapKeys(String)
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
     * 获取 Hash 中的所有字段名
     * <p>
     * 返回 Hash 中所有字段的名称集合。如果键不存在，返回空集合。
     * 适用于获取 Hash 的所有字段名。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取用户信息的所有字段名
     * Set<String> fields = redisService.getCacheMapKeys("user:123:info");
     * log.info("用户信息字段: {}", fields);
     *
     * // 检查 Hash 是否有字段
     * if (redisService.getCacheMapKeys("user:123:info").isEmpty()) {
     *     log.info("Hash 为空");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 key 不存在，返回空集合（不会返回 null）</li>
     *     <li>如果 key 不是 Hash 类型，可能返回空集合或抛出异常</li>
     *     <li>如果 Hash 很大，此操作可能较慢</li>
     * </ul>
     *
     * @param key Redis 键（Hash 的键），不能为 null
     * @return Hash 中的所有字段名集合，如果键不存在或发生错误返回空集合
     * @see #getCacheMapSize(String)
     * @see #getCacheMap(String, Class)
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 增加用户积分
     * Long newScore = redisService.incrementCacheMapValue("user:123:info", "score", 10);
     * log.info("新积分: {}", newScore);
     *
     * // 减少用户积分（使用负数）
     * Long newScore = redisService.incrementCacheMapValue("user:123:info", "score", -5);
     *
     * // 增加访问次数
     * Long visits = redisService.incrementCacheMapValue("user:123:info", "visits", 1);
     *
     * // 增加库存数量
     * Long stock = redisService.incrementCacheMapValue("product:123:info", "stock", 100);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果字段不存在，会先初始化为 0 再增加</li>
     *     <li>如果值不是数字类型，会尝试转换后计算</li>
     *     <li>delta 可以为负数（相当于减少）</li>
     *     <li>如果发生错误返回 0</li>
     * </ul>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKey  Hash 字段名，不能为 null
     * @param delta 增加的数值，可以为负数（相当于减少）
     * @return 增加后的数值，如果发生错误返回 0
     * @see #incrementCacheMapValue(String, String, double)
     * @see #getCacheMapValue(String, String)
     */
    public Long incrementCacheMapValue(final String key, final String hKey, final long delta) {
        try {
            return redisTemplate.opsForHash().increment(key, hKey, delta);
        } catch (Exception e) {
            // 如果直接 increment 失败，尝试获取当前值并手动转换后计算
            try {
                Object currentValue = redisTemplate.opsForHash().get(key, hKey);
                if (currentValue == null) {
                    // 如果字段不存在，直接设置 delta 值
                    redisTemplate.opsForHash().put(key, hKey, String.valueOf(delta));
                    return delta;
                }

                // 尝试将当前值转换为 Long
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
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 增加用户余额（浮点数）
     * Double newBalance = redisService.incrementCacheMapValue("user:123:info", "balance", 100.50);
     * log.info("新余额: {}", newBalance);
     *
     * // 减少用户余额（使用负数）
     * Double newBalance = redisService.incrementCacheMapValue("user:123:info", "balance", -50.25);
     *
     * // 增加评分（支持小数）
     * Double newRating = redisService.incrementCacheMapValue("product:123:info", "rating", 0.5);
     *
     * // 增加权重
     * Double newWeight = redisService.incrementCacheMapValue("item:123:info", "weight", 1.5);
     *
     * // 处理字符串类型的数值（会自动转换）
     * redisService.setCacheMapValue("user:123:info", "score", "100.5");
     * Double newScore = redisService.incrementCacheMapValue("user:123:info", "score", 10.5);
     * // 结果：111.0
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，线程安全</li>
     *     <li>如果字段不存在，会先初始化为 0.0 再增加</li>
     *     <li>如果值不是数字类型，会尝试转换后计算（支持 String 和 Number 类型）</li>
     *     <li>delta 可以为负数（相当于减少）</li>
     *     <li>如果发生错误返回 0.0</li>
     *     <li>支持浮点数计算，适用于金额、评分等场景</li>
     *     <li>如果值无法转换为数字类型，会抛出异常并返回 0.0</li>
     * </ul>
     *
     * @param key   Redis 键（Hash 的键），不能为 null
     * @param hKey  Hash 字段名，不能为 null
     * @param delta 增加的数值，可以为负数（相当于减少）
     * @return 增加后的数值，如果发生错误返回 0.0
     * @see #incrementCacheMapValue(String, String, long)
     * @see #getCacheMapValue(String, String)
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
     * <p>
     * 注意：key 和 value 中不能包含空格，否则操作会失败。
     * <p>
     * <b>使用示例（分布式锁释放）：</b>
     * <pre>{@code
     * // 实现分布式锁的释放
     * String lockKey = "lock:resource:123";
     * String lockValue = UUID.randomUUID().toString();
     * if (redisService.setCacheObjectIfAbsent(lockKey, lockValue, 60, TimeUnit.SECONDS)) {
     *     try {
     *         // 执行业务逻辑
     *         doSomething();
     *     } finally {
     *         // 只有锁的值匹配时才释放（防止释放其他线程的锁）
     *         boolean released = redisService.compareAndDelete(lockKey, lockValue);
     *         if (released) {
     *             log.info("锁释放成功");
     *         } else {
     *             log.warn("锁释放失败，可能已被其他线程释放或过期");
     *         }
     *     }
     * }
     *
     * // 安全地删除缓存（只有值匹配时才删除）
     * String expectedValue = "expected_value";
     * boolean deleted = redisService.compareAndDelete("cache:key", expectedValue);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>这是一个原子操作，通过 Lua 脚本实现</li>
     *     <li>只有当键的值等于期望值时，才会删除该键</li>
     *     <li>key 和 value 中不能包含空格，否则操作会失败</li>
     *     <li>常用于实现分布式锁的释放，确保只有持有锁的线程才能释放锁</li>
     *     <li>如果值不匹配，返回 false（不会抛出异常）</li>
     * </ul>
     *
     * @param key   缓存键，不能为 null，不能包含空格
     * @param value 期望的值，只有当键的值等于此值时才会删除，不能为 null，不能包含空格
     * @return true - 删除成功（值匹配）；false - 删除失败（值不匹配、键不存在或包含空格）
     * @see #setCacheObjectIfAbsent(String, Object, long, TimeUnit)
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