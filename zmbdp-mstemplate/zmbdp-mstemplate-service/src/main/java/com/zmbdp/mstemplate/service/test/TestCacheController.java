package com.zmbdp.mstemplate.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.zmbdp.common.cache.utils.CacheUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.service.IClothService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test/cache")
public class TestCacheController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private Cache<String, Object> caffeineCache;

    @Autowired
    private IClothService clothService;


    @GetMapping("/get")
    public Result<Void> get() {
        String key = "testKey";
        CacheUtil.getL2Cache(redisService, key, new TypeReference<List<Map<String, RegionTest>>>() {
        }, caffeineCache);
        return Result.success();
    }

    @GetMapping("/cloth/get")
    public Result<Integer> clothGet(Long proId) {
        return Result.success(clothService.clothPriceGet(proId));
    }
}
