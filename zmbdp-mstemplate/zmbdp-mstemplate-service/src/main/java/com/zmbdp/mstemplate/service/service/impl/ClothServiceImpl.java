package com.zmbdp.mstemplate.service.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.zmbdp.common.cache.utils.CacheUtil;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.service.IClothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ClothServiceImpl implements IClothService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private Cache<String, Object> caffeineCache;


    // proId 商品的主键 Id
    @Override
    public Integer clothPriceGet(Long proId) {
//        String key = CacheConstants.CLOTH_KEY + proId;
        String key = "c:"+ proId;
        Integer price = CacheUtil.getL2Cache(redisService, key, new TypeReference<Integer>() {}, caffeineCache);
        if (price != null) {
            return price;
        }
        price = getPriceFromDB(proId);
        return price;
    }

    //通过 sql 从数据库中查出指定商品在一年之间的平均售卖价格,并且要将查出来的数据分别放入二级缓存和一级缓存
    private Integer getPriceFromDB(Long proId) {
        Integer price = 100;  // 通过 sql 从数据库中查出指定商品在一年之间的平均售卖价格
        //String key = CacheConstants.CLOTH_KEY + proId;
        String key = "c:"+ proId;
        CacheUtil.setL2Cache(redisService, key, price, caffeineCache, 600L, TimeUnit.SECONDS);
        return price;
    }
}
