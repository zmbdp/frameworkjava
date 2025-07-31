package com.zmbdp.common.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
     * @param key 缓存键
     * @param clazz 缓存对象类型
     * @return 缓存对象
     * @param <T> 泛型类型
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
     * @param key 缓存键值
     * @param valueTypeRef 缓存对象类型
     * @return 缓存对象
     * @param <T> 泛型类型
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
     * @
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
}
