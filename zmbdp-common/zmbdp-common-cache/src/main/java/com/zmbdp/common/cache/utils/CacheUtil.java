package com.zmbdp.common.cache.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 缓存工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
public class CacheUtil {

    /**
     * 从一级缓存（本地缓存）中获取数据，如果没查到就从二级缓存（Redis缓存）中获取数据
     * @param redisService  Redis 缓存服务
     * @param key           缓存的键
     * @param valueTypeRef  值类型引用
     * @param caffeineCache 本地缓存信息
     * @return 缓存值
     * @param <T> 缓存值的类型
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
     * 从一级缓存（本地缓存）中获取数据，如果没查到就从二级缓存（Redis缓存）中获取数据（布隆过滤器先查）
     * @param redisService  Redis 缓存服务
     * @param bloomFilterService 布隆过滤器服务
     * @param key           缓存的键
     * @param valueTypeRef  值类型引用
     * @param caffeineCache 本地缓存信息
     * @return 缓存值
     * @param <T> 缓存值的类型
     */
    public static <T> T getL2Cache(RedisService redisService, BloomFilterService bloomFilterService,
                                   String key, TypeReference<T> valueTypeRef, Cache<String, Object> caffeineCache) {
        // 先检查布隆过滤器，如果确定不存在则直接返回null
        if (!bloomFilterService.mightContain(key)) {
            return null;
        }

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
     * 存储到一级缓存（本地缓存）中
     *
     * @param key           缓存的键
     * @param value         缓存的值
     * @param caffeineCache 本地缓存信息
     * @param <T>           缓存的值的类型
     */
    public static <T> void setL2Cache(String key, T value, Cache<String, Object> caffeineCache) {
        caffeineCache.put(key, value);
    }

    /**
     * 存储到二级缓存（Redis缓存）和一级缓存（本地缓存）中（不使用布隆过滤器）
     *
     * @param redisService  Redis 缓存服务
     * @param key           缓存的键
     * @param value         缓存的值
     * @param caffeineCache 本地缓存信息
     * @param timeout       缓存的过期时间
     * @param timeUnit      时间单位
     * @param <T>           缓存的值的类型
     */
    public static <T> void setL2Cache(
            RedisService redisService, String key, T value, Cache<String, Object> caffeineCache,
            Long timeout, TimeUnit timeUnit
    ) {
        redisService.setCacheObject(key, value, timeout, timeUnit);
        setL2Cache(key, value, caffeineCache);
    }

    /**
     * 存储到二级缓存（Redis缓存）、一级缓存（本地缓存）和布隆过滤器中
     *
     * @param redisService  Redis 缓存服务
     * @param bloomFilterService 布隆过滤器服务
     * @param key           缓存的键
     * @param value         缓存的值
     * @param caffeineCache 本地缓存信息
     * @param timeout       缓存的过期时间
     * @param timeUnit      时间单位
     * @param <T>           缓存的值的类型
     */
    public static <T> void setL2Cache(
            RedisService redisService, BloomFilterService bloomFilterService,
            String key, T value, Cache<String, Object> caffeineCache,
            Long timeout, TimeUnit timeUnit
    ) {
        // 存储到Redis和本地缓存
        redisService.setCacheObject(key, value, timeout, timeUnit);
        setL2Cache(key, value, caffeineCache);

        // 添加到布隆过滤器
        bloomFilterService.put(key);
    }
}
