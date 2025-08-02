package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/test/redisson")
public class TestRedissonController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/delStock")
    public String delStock() {
        String proKey = "proKey";
        String uuid = UUID.randomUUID().toString();  // 唯一  作为身份标识
        Boolean save = redisService.setCacheObjectIfAbsent(proKey, uuid, 30, TimeUnit.SECONDS); // 加锁
        if (!save) {
            return "unlock";  // 未获取到锁
        }
        try {
            // 获取库存
            String stockKey = "stock";
            Integer stock = redisService.getCacheObject(stockKey, Integer.class);
            if (stock <= 0) {
                return "error";  // 秒杀失败
            }
            stock--;
            redisService.setCacheObject(stockKey, stock);
        } finally {
            redisService.compareAndDelete(proKey, uuid);
        }
        return "success";   // 秒杀成功
    }
}
