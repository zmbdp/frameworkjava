package com.zmbdp.common.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置
 *
 * @author 稚名不带撇
 */
@RefreshScope
@Configuration
public class CaffeineConfig {

    /**
     * 初始容量
     */
    @Value("${caffeine.build.initial-capacity:100}")
    private Integer initialCapacity;

    /**
     * 最大容量
     */
    @Value("${caffeine.build.maximum-size:1000}")
    private Long maximumSize;

    /**
     * 过期时间 (秒)
     */
    @Value("${caffeine.build.expire:30}")
    private Long expire;


    /**
     * 构造本地缓存对象
     *
     * @return 本地缓存对象
     */
    @Bean
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder() // 创建缓存对象
                .initialCapacity(initialCapacity) // 设置一级缓存初始容量
                .maximumSize(maximumSize) //  设置一级缓存最大容量
                // 这里是配置过期策略，设置的是写入 30s 后过期
                // 其他过期策略:
                // .expireAfterAccess(30, TimeUnit.SECONDS) // 30s 未访问则过期
                // .expireAfter(new CacheExpiry()) // 自定义过期策略
                .expireAfterWrite(expire, TimeUnit.SECONDS) // 设置一级缓存过期时间, 固定 30s 后过期
                .build();
    }
}
