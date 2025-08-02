package com.zmbdp.common.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * redisson 分布式锁相关操作工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonLockService {
    /**
     * redis操作客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 获取锁（毫秒级）
     *
     * @param lockKey 锁的 key，唯一标识，建议模块名 + 唯一键
     * @param expire  超时时间，单位毫秒，传入 -1 自动续期
     * @return 获取到的 RLock 实例，为 null 则获取失败
     */
    public RLock acquire(String lockKey, long expire) {
        try {
            final RLock lockInstance = redissonClient.getLock(lockKey);

            // 注意：如果 tryLock 指定了 leaseTime > 0 就不会续期。参考 RedissonLock 类的 tryAcquireAsync 方法的实现
            lockInstance.lock(expire, TimeUnit.MILLISECONDS);
            return lockInstance;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取锁（自定义时间单位）
     *
     * @param lockKey 锁的 key，唯一标识，建议模块名 + 唯一键
     * @param expire  超时时间，单位毫秒，传入 -1 自动续期
     * @param timeUnit 时间单位
     * @return 获取到的 RLock 实例，为 null 则获取失败
     */
    public RLock acquire(String lockKey, long expire, TimeUnit timeUnit) {
        try {
            final RLock lockInstance = redissonClient.getLock(lockKey);

            // 注意：如果 tryLock 指定了 expire > 0 就不会续期。参考 RedissonLock 类的 tryAcquireAsync 方法的实现
            lockInstance.lock(expire, timeUnit);
            return lockInstance;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 释放锁。注意：必须和获取锁在一个线程中
     *
     * @param lockInstance 锁的实例，acquire 返回的
     * @return 释放成功返回 true，否则返回 false
     */
    public boolean releaseLock(RLock lockInstance) {
        if (lockInstance.isHeldByCurrentThread()) {
            lockInstance.unlock();
            return true;
        }
        return false;
    }
}