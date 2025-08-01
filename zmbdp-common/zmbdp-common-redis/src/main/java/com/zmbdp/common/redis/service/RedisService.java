package com.zmbdp.common.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis 相关操作工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class RedisService {

    @Autowired
    private RedisTemplate redisTemplate;

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
            if (o == null) {
                return null;
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
        if (!StringUtils.hasText(key)) {
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
        if (!StringUtils.hasText(key)) {
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
            log.warn("RedisService.removeForList remove left redis error: {}", e.getMessage());
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
            log.warn("RedisService.removeForList remove k left redis error: {}", e.getMessage());
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
            log.warn("RedisService.removeForList remove right redis error: {}", e.getMessage());
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
            log.warn("RedisService.removeForList remove k right redis error: {}", e.getMessage());
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
     * @param key           key信息
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
    public Long addMember(final String key, Object... member) {
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
    public Long deleteMember(final String key, Object... member) {
        try {
            Long remove = redisTemplate.opsForSet().remove(key, member);
            return remove == null ? 0L : remove;
        } catch (Exception e) {
            log.warn("RedisService.deleteMember delete member error: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 检查元素是否存在
     *
     * @param key    缓存 key
     * @param member 缓存元素
     * @return 是否存在
     */
    public Boolean isMember(final String key, Object member) {
        try {
            return redisTemplate.opsForSet().isMember(key, member);
        } catch (Exception e) {
            log.warn("RedisService.isMember check member error: {}", e.getMessage());
            return false;
        }
    }


    /**
     * 获取 Set 中所有元素
     *
     * @param key   键值
     * @param clazz 数据类型
     * @param <T>   泛型
     * @return Set<T>
     */
    public <T> Set<T> getCacheSet(final String key, Class<T> clazz) {
        try {
            // 需要适当的转换逻辑
            Set<T> members = (Set<T>) redisTemplate.opsForSet().members(key);
            return members;
        } catch (Exception e) {
            log.warn("RedisService.getCacheSet get set data error: {}", e.getMessage());
            return null;
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
     * 获取两个集合的交集
     *
     * @param setKey1  第一个集合的 key
     * @param setKey2  第二个集合的 key
     * @param clazz 元素类型
     * @param <T>   泛型类型
     * @return 交集结果
     */
    public <T> Set<T> intersectToCacheSet(final String setKey1, final String setKey2, Class<T> clazz) {
        try {
            return (Set<T>) redisTemplate.opsForSet().intersect(setKey1, setKey2);
        } catch (Exception e) {
            log.warn("RedisService.intersect set intersect error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取两个集合的并集
     *
     * @param setKey1  第一个集合的key
     * @param setKey2  第二个集合的key
     * @param clazz 元素类型
     * @param <T>   泛型类型
     * @return 并集结果
     */
    public <T> Set<T> unionToCacheSet(final String setKey1, final String setKey2, Class<T> clazz) {
        try {
            return (Set<T>) redisTemplate.opsForSet().union(setKey1, setKey2);
        } catch (Exception e) {
            log.warn("RedisService.union set union error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取两个集合的差集 (key1 - key2)
     *
     * @param setKey1  第一个集合的key
     * @param setKey2  第二个集合的key
     * @param clazz 元素类型
     * @param <T>   泛型类型
     * @return 差集结果
     */
    public <T> Set<T> differenceToCacheSet(final String setKey1, final String setKey2, Class<T> clazz) {
        try {
            return (Set<T>) redisTemplate.opsForSet().difference(setKey1, setKey2);
        } catch (Exception e) {
            log.warn("RedisService.difference set difference error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将元素从 sourceKey 集合移动到 destinationKey 集合
     *
     * @param sourceKey      源集合key
     * @param destinationKey 目标集合key
     * @param member         要移动的元素
     * @return 移动成功返回true，否则返回false
     */
    public Boolean moveMemberCacheSet(final String sourceKey, final String destinationKey, Object member) {
        try {
            return redisTemplate.opsForSet().move(sourceKey, member, destinationKey);
        } catch (Exception e) {
            log.warn("RedisService.moveMember move member error: {}", e.getMessage());
            return false;
        }
    }
}
