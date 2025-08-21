package com.zmbdp.common.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.core.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * redis 相关操作工具类
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
     * 设置有效时间
     *
     * @param key     Redis 键
     * @param timeout 有效时间 (秒)
     * @return true - 设置成功; false - 设置失败
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
     * 设置有效时间 (并指定时间单位)
     *
     * @param key      Redis 键
     * @param timeout  有效时间
     * @param timeUnit 时间单位
     * @return true - 设置成功; false - 设置失败
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
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间, -2 - 不存在
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
     * 判断 key是否存在
     *
     * @param key 键
     * @return true - 存在; false - 不存在
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
     * 根据提供的键模式查找 Redis 中匹配的键
     *
     * @param pattern 要查找的键的模式
     * @return 键列表
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
     * 重命名key
     *
     * @param oldKey 原来 key
     * @param newKey 新 key
     */
    public void renameKey(String oldKey, String newKey) {
        try {
            redisTemplate.rename(oldKey, newKey);
        } catch (Exception e) {
            log.warn("RedisService.renameKey error: {}", e.getMessage());
        }
    }

    /**
     * 删除单个数据
     *
     * @param key 缓存的键值
     * @return 是否成功  true - 删除成功; false - 删除失败
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
     * 删除多个数据
     *
     * @param collection 多个数据对应的缓存的键值
     * @return 是否删除了对象 true - 删除成功; false - 删除失败
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

    /*=============================================    String    =============================================*/

    /**
     * 存入 redis
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   泛型类型
     */
    public <T> void setCacheObject(final String key, final T value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("RedisService.setCacheObject set cache error: {}", e.getMessage());
        }
    }


    /**
     * 存入 redis，设置过期时间
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  缓存超时时间
     * @param timeUnit 时间单位
     * @param <T>      泛型类型
     */
    public <T> void setCacheObject(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.warn("RedisService.setCacheObject set cache and ttl error: {}", e.getMessage());
        }
    }

    /**
     * 存入 redis，如果键不存在则设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   泛型类型
     * @return 设置成功返回 true，否则返回 false
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
     * 存入 redis 并设置过期时间，如果键不存在则设置缓存
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 缓存超时时间
     * @param timeUnit 时间单位
     * @param <T>     泛型类型
     * @return 设置成功返回 true，否则返回 false
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
     * 获取缓存对象
     *
     * @param key   缓存键
     * @param clazz 缓存对象类型
     * @param <T>   泛型类型
     * @return 缓存对象
     */
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        try {
            Object o = redisTemplate.opsForValue().get(key);
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
     * 获取复杂的泛型嵌套缓存对象
     *
     * @param key          缓存键值
     * @param valueTypeRef 缓存对象类型
     * @param <T>          泛型类型
     * @return 缓存对象
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
     * 对 key 对应的 value 进行原子递增（+1）
     *
     * @param key Redis 键, value 必须为数字类型
     * @return 递增后的值（失败返回 -1）
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
     * 对 key 对应的 value 进行原子递减（-1）
     *
     * @param key Redis 键, value 必须为数字类型
     * @return 递减后的值（失败返回 -1）
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

    /*=============================================    List    =============================================*/


    /**
     * 将 List 数据保持原有顺序存入缓存
     *
     * @param key      缓存键
     * @param dataList 缓存数据
     * @param <T>      泛型类型
     * @return 保存成功返回保存的条数，失败返回 -1
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
     * 从 List 结构左侧插入数据（头插、插入单个数据）
     *
     * @param key  缓存键
     * @param data 缓存数据
     * @param <T>  泛型类型
     * @return 插入成功返回插入的条数，失败返回 -1
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
     * 从 List 结构右侧插入数据（尾插、插入单个数据）
     *
     * @param key   key
     * @param value 缓存的对象
     * @param <T>   值类型
     * @return 插入成功返回插入的条数，失败返回 -1
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
     * 获得缓存的 List 对象
     *
     * @param key   key 缓存的键值
     * @param clazz 对象的类
     * @param <T>   对象类型
     * @return 列表
     */
    //有序性
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
     * 获得缓存的 List 对象 （支持复杂的泛型嵌套）
     *
     * @param key           key 信息
     * @param typeReference 类型模板
     * @param <T>           对象类型
     * @return List 对象
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
     * 缓存 Map 数据
     *
     * @param key     key
     * @param dataMap map
     * @param <T>     对象类型
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
     * 往 Hash 中存入单个数据（如果key不存在，则创建key添加map中的数据）
     *
     * @param key   Redis 键
     * @param hKey  Hash 键
     * @param value 值
     * @param <T>   对象类型
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
     * 获取缓存的 map 数据
     *
     * @param key   key
     * @param clazz 值的类型
     * @param <T>   对象类型
     * @return hash 对应的 map
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
     * 获取缓存的 map 数据（支持复杂的泛型嵌套）
     *
     * @param key           key
     * @param typeReference 类型模板
     * @param <T>           对象类型
     * @return hash 对应的 map
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
     * 获取 Hash 中的单个数据
     *
     * @param key  Redis 键
     * @param hKey Hash 键
     * @param <T>  对象类型
     * @return Hash中的对象
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
     * 为 Hash 中的值增加指定数值（整型）
     *
     * @param key   Redis 键
     * @param hKey  Hash 键
     * @param delta 增加的数值
     * @return 增加后的数值
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
     * 为 Hash 中的值增加指定数值（浮点型）
     *
     * @param key   Redis 键
     * @param hKey  Hash 键
     * @param delta 增加的数值
     * @return 增加后的数值
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
     * 删除指定值对应的 Redis 中的键值（Compare And Delete）
     * 先对 value 进行比较，成功了再进行删除（所有的操作通过 lua 脚本原子性实现）
     * @param key   缓存 key
     * @param value 期望的值
     * @return 是否完成了删除操作，true - 表示删除成功; false - 表示删除失败
     */
    public boolean compareAndDelete(String key, String value) {
        // 验证 key 和 value 中不能包含空格
        if (key.contains(StringUtil.SPACE) || value.contains(StringUtil.SPACE)) {
            return false;
        }

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        // 通过 lua 脚本原子验证令牌和删除令牌
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key), // KEYS[1]
                value); // ARGV[1]
        // 如果返回结果为 0 行, 则说明删除失败
        return !Objects.equals(result, 0L);
    }
}
