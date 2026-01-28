package com.zmbdp.common.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁服务类
 * <p>
 * 基于 Redisson 实现的分布式锁工具类，提供线程安全的分布式锁获取和释放功能。<br>
 * 支持看门狗（Watchdog）机制自动续期，防止业务执行时间超过锁有效期导致锁被误释放。
 * </p>
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>看门狗机制</b>：自动续期，防止业务执行时间过长导致锁过期</li>
 *     <li><b>阻塞获取</b>：支持阻塞等待获取锁，直到获取成功或超时</li>
 *     <li><b>非阻塞获取</b>：支持立即返回，获取失败不等待</li>
 *     <li><b>安全释放</b>：自动校验锁的持有者，防止误释放其他线程的锁</li>
 *     <li><b>可重入锁</b>：同一线程可以多次获取同一把锁</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 获取带看门狗的锁（阻塞等待，自动续期）
 * RLock lock = redissonLockService.acquire("order:pay:123456");
 * try {
 *     // 执行业务逻辑
 *     processOrder();
 * } finally {
 *     // 释放锁
 *     redissonLockService.releaseLock(lock);
 * }
 *
 * // 2. 获取带过期时间的锁（阻塞等待，不自动续期）
 * RLock lock = redissonLockService.acquire("order:pay:123456", 30);
 * try {
 *     processOrder();
 * } finally {
 *     redissonLockService.releaseLock(lock);
 * }
 *
 * // 3. 尝试获取锁（带超时，启用看门狗）
 * RLock lock = redissonLockService.acquire("order:pay:123456", 5, TimeUnit.SECONDS);
 * if (lock != null) {
 *     try {
 *         processOrder();
 *     } finally {
 *         redissonLockService.releaseLock(lock);
 *     }
 * } else {
 *     log.warn("获取锁失败");
 * }
 *
 * // 4. 尝试获取锁（带超时和过期时间，不启用看门狗）
 * RLock lock = redissonLockService.acquire("order:pay:123456", 5, 30, TimeUnit.SECONDS);
 * if (lock != null) {
 *     try {
 *         processOrder();
 *     } finally {
 *         redissonLockService.releaseLock(lock);
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>锁的 key 建议使用业务前缀，格式如：{@code "业务模块:操作:唯一标识"}</li>
 *     <li>必须在 finally 块中释放锁，确保锁一定会被释放</li>
 *     <li>看门狗机制默认锁有效期为 30 秒，每 10 秒自动续期一次</li>
 *     <li>如果业务执行时间确定且较短，建议使用固定过期时间的锁，避免看门狗续期开销</li>
 *     <li>释放锁时会自动校验锁的持有者，只有持有锁的线程才能释放</li>
 *     <li>如果线程被中断，获取锁的操作会失败并恢复中断状态</li>
 * </ul>
 * </p>
 *
 * @author 稚名不带撇
 */
@Slf4j
@RequiredArgsConstructor // 创建实例时，会自动注入 RedissonClient 实例
public class RedissonLockService {

    /**
     * Redisson 客户端实例
     * <p>
     * 用于创建和管理分布式锁。通过构造函数自动注入（使用 {@code @RequiredArgsConstructor} 注解）。
     * <p>
     * <b>使用说明：</b>
     * <ul>
     *     <li>通过 {@code redissonClient.getLock(lockKey)} 创建锁实例</li>
     *     <li>支持可重入锁（RLock），同一线程可以多次获取同一把锁</li>
     *     <li>支持看门狗机制自动续期</li>
     * </ul>
     */
    private final RedissonClient redissonClient;

    /**
     * 获取带看门狗机制的分布式锁（阻塞等待）
     * <p>
     * 如果锁已被其他线程持有，当前线程会一直阻塞等待，直到获取到锁。<br>
     * 看门狗机制会自动续期，默认锁有效期为 30 秒，每 10 秒自动续期一次。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取锁（阻塞等待，自动续期）
     * RLock lock = redissonLockService.acquire("order:pay:123456");
     * try {
     *     // 执行业务逻辑（执行时间不确定）
     *     processOrder();
     * } finally {
     *     // 确保锁一定会被释放
     *     redissonLockService.releaseLock(lock);
     * }
     *
     * // 处理用户订单（业务执行时间可能较长）
     * RLock lock = redissonLockService.acquire("user:order:123");
     * try {
     *     // 复杂的业务逻辑，执行时间不确定
     *     validateOrder();
     *     processPayment();
     *     updateInventory();
     * } finally {
     *     redissonLockService.releaseLock(lock);
     * }
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>业务执行时间不确定，需要自动续期的场景</li>
     *     <li>业务逻辑复杂，执行时间可能较长的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此方法会一直阻塞，如果锁一直被其他线程持有，当前线程会一直等待</li>
     *     <li>建议在业务执行时间不确定的场景使用</li>
     *     <li>如果希望限制等待时间，使用带超时的 {@link #acquire(String, long, TimeUnit)} 方法</li>
     *     <li>看门狗机制默认锁有效期为 30 秒，每 10 秒自动续期一次</li>
     *     <li>必须在 finally 块中释放锁，确保锁一定会被释放</li>
     * </ul>
     *
     * @param lockKey 锁的 key，建议格式：业务前缀:唯一标识（如 "order:pay:123456"），不能为 null
     * @return RLock 锁实例，如果获取失败（发生异常）返回 null
     * @see #acquire (String, long, TimeUnit)
     * @see #releaseLock(RLock)
     */
    public RLock acquire(String lockKey) {
        try {
            final RLock lock = redissonClient.getLock(lockKey);
            lock.lock(-1, TimeUnit.SECONDS);
            return lock;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取带固定过期时间的分布式锁（阻塞等待）
     * <p>
     * 如果锁已被其他线程持有，当前线程会一直阻塞等待，直到获取到锁。<br>
     * 锁的有效期为指定的过期时间，不会自动续期。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取锁（阻塞等待，30秒后自动过期）
     * RLock lock = redissonLockService.acquire("order:pay:123456", 30);
     * try {
     *     // 执行业务逻辑（执行时间确定且较短）
     *     processOrder();
     * } finally {
     *     // 确保锁一定会被释放
     *     redissonLockService.releaseLock(lock);
     * }
     *
     * // 快速操作（业务执行时间很短，使用短过期时间）
     * RLock lock = redissonLockService.acquire("cache:update:123", 5);
     * try {
     *     // 快速更新缓存
     *     updateCache();
     * } finally {
     *     redissonLockService.releaseLock(lock);
     * }
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>业务执行时间确定且较短的场景</li>
     *     <li>避免看门狗续期开销的场景</li>
     *     <li>业务逻辑简单，执行时间可预估的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此方法会一直阻塞，如果锁一直被其他线程持有，当前线程会一直等待</li>
     *     <li>如果业务执行时间超过过期时间，锁会被自动释放，可能导致并发问题</li>
     *     <li>过期时间单位为秒，不是毫秒</li>
     *     <li>建议设置的过期时间要大于业务执行时间，留出安全余量</li>
     *     <li>必须在 finally 块中释放锁，确保锁一定会被释放</li>
     * </ul>
     *
     * @param lockKey 锁的 key，建议格式：业务前缀:唯一标识（如 "order:pay:123456"），不能为 null
     * @param expire  锁的有效期（秒），必须大于 0
     * @return RLock 锁实例，如果获取失败（发生异常）返回 null
     * @see #acquire(String)
     * @see #acquire(String, long, long, TimeUnit)
     * @see #releaseLock(RLock)
     */
    public RLock acquire(String lockKey, long expire) {
        try {
            final RLock lock = redissonClient.getLock(lockKey);
            lock.lock(expire, TimeUnit.SECONDS);
            return lock;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试获取带看门狗机制的分布式锁（带超时）
     * <p>
     * 在指定的等待时间内尝试获取锁，如果获取成功则启用看门狗机制自动续期。<br>
     * 如果等待时间内未获取到锁，返回 null。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 尝试获取锁（等待最多5秒，启用看门狗）
     * RLock lock = redissonLockService.acquire("order:pay:123456", 5, TimeUnit.SECONDS);
     * if (lock != null) {
     *     try {
     *         // 执行业务逻辑（执行时间不确定）
     *         processOrder();
     *     } finally {
     *         // 确保锁一定会被释放
     *         redissonLockService.releaseLock(lock);
     *     }
     * } else {
     *     log.warn("获取锁失败，可能被其他线程占用");
     * }
     *
     * // 非阻塞获取（waitTime = 0，立即返回）
     * RLock lock = redissonLockService.acquire("resource:123", 0, TimeUnit.SECONDS);
     * if (lock != null) {
     *     try {
     *         useResource();
     *     } finally {
     *         redissonLockService.releaseLock(lock);
     *     }
     * } else {
     *     log.warn("资源被占用，稍后重试");
     * }
     * }</pre>
     * <p>
     * <b>看门狗机制说明：</b>
     * <ul>
     *     <li>默认锁有效期为 30 秒</li>
     *     <li>每 10 秒自动续期一次</li>
     *     <li>业务执行完成后，看门狗会自动停止续期</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>业务执行时间不确定，需要自动续期的场景</li>
     *     <li>不希望无限等待，需要限制等待时间的场景</li>
     *     <li>需要非阻塞获取锁的场景（waitTime = 0）</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 waitTime 为 0，表示不等待立即返回（非阻塞）</li>
     *     <li>如果等待时间内未获取到锁，返回 null</li>
     *     <li>如果线程在等待过程中被中断，会恢复中断状态并返回 null</li>
     *     <li>必须在 finally 块中释放锁，确保锁一定会被释放</li>
     * </ul>
     *
     * @param lockKey  锁的 key，建议格式：业务前缀:唯一标识（如 "order:pay:123456"），不能为 null
     * @param waitTime 最大等待时间，如果为 0 表示不等待立即返回
     * @param unit     时间单位，不能为 null
     * @return RLock 锁实例，如果获取成功返回锁实例，如果超时未获取或发生异常返回 null
     * @throws InterruptedException 如果线程在等待过程中被中断，会恢复中断状态并返回 null
     * @see #acquire(String)
     * @see #acquire(String, long, long, TimeUnit)
     * @see #releaseLock(RLock)
     */
    public RLock acquire(String lockKey, long waitTime, TimeUnit unit) {
        try {
            // 获取分布式锁实例（注意：此时并未实际加锁，只是创建锁对象）
            // lockKey 建议格式：业务前缀: 唯一标识（如 "order:pay:123456"）
            final RLock lock = redissonClient.getLock(lockKey);
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
     * 尝试获取分布式锁（可配置过期时间和看门狗）
     * <p>
     * 在指定的等待时间内尝试获取锁，可以灵活配置锁的过期时间和是否启用看门狗机制。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 场景1：启用看门狗机制（leaseTime = -1）
     * RLock lock = redissonLockService.acquire("order:pay:123456", 5, -1, TimeUnit.SECONDS);
     * if (lock != null) {
     *     try {
     *         // 执行业务逻辑（执行时间不确定，需要自动续期）
     *         processOrder();
     *     } finally {
     *         redissonLockService.releaseLock(lock);
     *     }
     * }
     *
     * // 场景2：固定过期时间（leaseTime > 0，不启用看门狗）
     * RLock lock = redissonLockService.acquire("cache:update:123", 3, 10, TimeUnit.SECONDS);
     * if (lock != null) {
     *     try {
     *         // 执行业务逻辑（执行时间确定，10秒后自动过期）
     *         updateCache();
     *     } finally {
     *         redissonLockService.releaseLock(lock);
     *     }
     * }
     *
     * // 场景3：非阻塞获取（waitTime = 0）
     * RLock lock = redissonLockService.acquire("resource:123", 0, 30, TimeUnit.SECONDS);
     * if (lock != null) {
     *     try {
     *         useResource();
     *     } finally {
     *         redissonLockService.releaseLock(lock);
     *     }
     * } else {
     *     log.warn("资源被占用");
     * }
     * }</pre>
     * <p>
     * <b>leaseTime 参数说明：</b>
     * <ul>
     *     <li>{@code -1}：启用看门狗机制，默认锁有效期为 30 秒，每 10 秒自动续期</li>
     *     <li>{@code > 0}：锁的有效期，单位为 unit 指定的时间单位，不会自动续期</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>需要精确控制锁的过期时间，且业务执行时间确定的场景（leaseTime > 0）</li>
     *     <li>业务执行时间不确定，需要自动续期的场景（leaseTime = -1）</li>
     *     <li>需要限制等待时间，且需要灵活配置锁过期时间的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 waitTime 为 0，表示不等待立即返回（非阻塞）</li>
     *     <li>如果 leaseTime > 0，锁不会自动续期，业务执行时间不能超过 leaseTime</li>
     *     <li>如果 leaseTime = -1，启用看门狗机制，锁会自动续期</li>
     *     <li>如果等待时间内未获取到锁，返回 null</li>
     *     <li>如果线程在等待过程中被中断，会恢复中断状态并返回 null</li>
     *     <li>必须在 finally 块中释放锁，确保锁一定会被释放</li>
     * </ul>
     *
     * @param lockKey   锁的 key，建议格式：业务前缀:唯一标识（如 "order:pay:123456"），不能为 null
     * @param waitTime  最大等待时间，如果为 0 表示不等待立即返回
     * @param leaseTime 锁持有时间，-1 表示启用看门狗机制，> 0 表示锁的有效期（单位由 unit 指定）
     * @param unit      时间单位，不能为 null
     * @return RLock 锁实例，如果获取成功返回锁实例，如果超时未获取或发生异常返回 null
     * @throws InterruptedException 如果线程在等待过程中被中断，会恢复中断状态并返回 null
     * @see #acquire(String)
     * @see #acquire(String, long, TimeUnit)
     * @see #releaseLock(RLock)
     */
    public RLock acquire(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            // 获取分布式锁实例（注意：此时并未实际加锁，只是创建锁对象）
            // lockKey 建议格式：业务前缀: 唯一标识（如 "order:pay:123456"）
            final RLock lock = redissonClient.getLock(lockKey);
            // 尝试获取锁，支持看门狗自动续期
            // waitTime: 最大等待时间（单位由 unit 指定），waitTime 为 0 表示不等待立即返回
            // leaseTime: 锁持有时间，-1 表示启用看门狗机制，> 0 表示锁的有效期
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
     * 安全释放分布式锁
     * <p>
     * 释放锁前会进行安全检查：
     * <ul>
     *     <li>检查锁是否为 null（null 视为释放成功）</li>
     *     <li>检查锁是否被当前线程持有（防止误释放其他线程的锁）</li>
     *     <li>检查锁是否仍然有效（防止释放已过期的锁）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 标准用法：在 finally 块中释放锁
     * RLock lock = redissonLockService.acquire("order:pay:123456");
     * try {
     *     // 执行业务逻辑
     *     processOrder();
     * } finally {
     *     // 确保锁一定会被释放
     *     boolean released = redissonLockService.releaseLock(lock);
     *     if (!released) {
     *         log.warn("锁释放失败，可能已被其他线程释放或过期");
     *     }
     * }
     *
     * // 带超时的锁释放
     * RLock lock = redissonLockService.acquire("resource:123", 5, TimeUnit.SECONDS);
     * if (lock != null) {
     *     try {
     *         useResource();
     *     } finally {
     *         redissonLockService.releaseLock(lock);
     *     }
     * }
     *
     * // 处理 null 锁的情况
     * RLock lock = redissonLockService.acquire("lock:key");
     * if (lock != null) {
     *     try {
     *         doSomething();
     *     } finally {
     *         // releaseLock 方法会处理 null 的情况，但这里 lock 不为 null
     *         redissonLockService.releaseLock(lock);
     *     }
     * }
     * }</pre>
     * <p>
     * <b>安全检查机制：</b>
     * <ul>
     *     <li>{@code lock.isLocked()}：检查锁是否仍然有效（未过期）</li>
     *     <li>{@code lock.isHeldByCurrentThread()}：检查锁是否被当前线程持有</li>
     *     <li>只有两个条件都满足时，才会释放锁</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 finally 块中调用此方法，确保锁一定会被释放</li>
     *     <li>只有持有锁的线程才能释放锁，其他线程调用会失败</li>
     *     <li>如果锁已经过期或被其他线程释放，释放操作会失败但不抛异常</li>
     *     <li>如果 lock 为 null，视为释放成功，返回 true</li>
     *     <li>释放失败时会记录警告日志，但不会抛出异常</li>
     * </ul>
     *
     * @param lock 要释放的锁实例，如果为 null 则视为释放成功，返回 true
     * @return true - 释放成功；false - 释放失败（锁未被当前线程持有、锁已过期或发生异常）
     * @see #acquire(String)
     * @see #acquire(String, long)
     * @see #acquire(String, long, TimeUnit)
     * @see #acquire(String, long, long, TimeUnit)
     */
    public boolean releaseLock(RLock lock) {
        if (lock == null) {
            return true;
        }
        try {
            // 安全检查：只有当前线程持有且锁仍然有效时才释放
            // isLocked(): 检查锁是否仍然有效（未过期）
            // isHeldByCurrentThread(): 检查锁是否被当前线程持有
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("RedissonLockService.releaseLock Lock released: {}", lock.getName());
                return true;
            }
        } catch (IllegalMonitorStateException e) {
            log.warn("RedissonLockService.releaseLock 锁释放失败：{}", e.getMessage(), e);
        }
        return false;
    }
}