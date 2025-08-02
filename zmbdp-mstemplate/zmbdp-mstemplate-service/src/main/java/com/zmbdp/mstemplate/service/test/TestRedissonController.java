package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.common.redis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
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

    @Autowired
    private RedissonLockService redissonLockService;

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

    @PostMapping("/delStock/redisson")
    public String delStockRedisson() {
        String proKey = "proKey";
        // 获取锁, 让所有线程竞争同一把锁，去掉 uuid
        RLock locked = redissonLockService.acquire(proKey, 3, TimeUnit.SECONDS); // waitTime: 最大等待时间， leaseTime: 锁的过期时间，-1表示开启看门狗
        try {
            if (locked == null) {
                return "unlock";  // 未获取到锁
            }
            // 获取库存
            String stockKey = "stock";
            Integer stock = redisService.getCacheObject(stockKey, Integer.class);
            if (stock <= 0) {
                return "error";  // 秒杀失败
            }
            stock--;
            redisService.setCacheObject(stockKey, stock);
        } finally {
            // 确保释放锁, 避免死锁
            redissonLockService.releaseLock(locked);
        }
        return "success";   // 秒杀成功
    }
}
