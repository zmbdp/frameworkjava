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
     * 获取有看门狗的锁
     *
     * @param lockKey  锁的 key
     * @param waitTime 最大等待时间
     * @param unit     时间单位
     * @return RLock 实例（获取失败返回 null）
     */
    public RLock acquire(String lockKey, long waitTime, TimeUnit unit) {
        try {
            // 获取分布式锁实例（注意：此时并未实际加锁，只是创建锁对象）
            // lockKey 建议格式：业务前缀: 唯一标识（如 "order:pay:123456"）
            RLock lock = redissonClient.getLock(lockKey);
            // 尝试获取锁，支持看门狗自动续期
            // waitTime: 最大等待时间（单位由 unit 指定），waitTime 为 0 表示不等待立即返回
            // -1: leaseTime参数，表示启用看门狗机制（默认 30 秒锁有效期，每 10 秒自动续期）
            // 返回true表示成功获取锁，false表示超时未获取
            boolean acquired = lock.tryLock(waitTime, -1, unit);
            return acquired ? lock : null;
        } catch (InterruptedException e) {
            log.warn("RedissonLockService.acquire 获取看门狗的锁失败：{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 获取锁（可配置过期时间, 如果不是 -1 就说明不启动看门狗模式）
     *
     * @param lockKey   锁的 key
     * @param waitTime  最大等待时间
     * @param leaseTime 锁持有时间（-1 表示启用看门狗）
     * @param unit      时间单位
     * @return RLock 实例（获取失败返回null）
     */
    public RLock acquire(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            // 获取分布式锁实例（注意：此时并未实际加锁，只是创建锁对象）
            // lockKey 建议格式：业务前缀: 唯一标识（如 "order:pay:123456"）
            RLock lock = redissonClient.getLock(lockKey);
            // 尝试获取锁，支持看门狗自动续期
            // waitTime: 最大等待时间（单位由 unit 指定），waitTime 为 0 表示不等待立即返回
            // -1: leaseTime参数，表示启用看门狗机制（默认 30 秒锁有效期，每 10 秒自动续期）
            // 返回true表示成功获取锁，false表示超时未获取
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            return acquired ? lock : null;
        } catch (InterruptedException e) {
            log.warn("RedissonLockService.acquire 获取正常锁失败：{}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 安全释放锁
     *
     * @param lock 锁实例
     * @return 是否释放成功, true - 释放成功, false - 释放失败
     */
    public boolean releaseLock(RLock lock) {
        if (lock == null) {
            return true;
        }
        try {
            // 直接调用 unlock()，依赖 Redisson 的内部校验
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("RedissonLockService.releaseLock Lock released: {}", lock.getName());
                return true;
            }
        } catch (IllegalMonitorStateException e) {
            log.warn("RedissonLockService.releaseLock 锁释放失败：{}", e.getMessage(), e);
        }
        return false;
    }
}