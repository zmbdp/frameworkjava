package com.zmbdp.mstemplate.service.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.zmbdp.common.cache.utils.CacheUtil;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.service.IClothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 服装服务实现类
 *
 * @author 稚名不带撇
 */
@Service
public class ClothServiceImpl implements IClothService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private Cache<String, Object> caffeineCache;

    /**
     * 获取服装价格
     * 使用二级缓存（Redis + Caffeine）
     *
     * @param proId 产品ID
     * @return 价格
     */
    @Override
    public Integer clothPriceGet(Long proId) {
        String key = "c:" + proId;
        // 先从二级缓存获取
        Integer price = CacheUtil.getL2Cache(redisService, key, new TypeReference<Integer>() {}, caffeineCache);
        if (price != null) {
            return price;
        }
        // 缓存未命中，从数据库查询
        price = getPriceFromDB(proId);
        return price;
    }

    /**
     * 从数据库查询价格并更新缓存
     *
     * @param proId 产品ID
     * @return 价格
     */
    private Integer getPriceFromDB(Long proId) {
        // 模拟从数据库查询商品一年内的平均售卖价格
        Integer price = 100;
        String key = "c:" + proId;
        // 更新二级缓存
        CacheUtil.setL2Cache(redisService, key, price, caffeineCache, 600L, TimeUnit.SECONDS);
        return price;
    }
}