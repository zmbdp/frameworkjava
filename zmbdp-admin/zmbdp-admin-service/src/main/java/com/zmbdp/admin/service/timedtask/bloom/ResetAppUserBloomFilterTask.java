package com.zmbdp.admin.service.timedtask.bloom;

import com.zmbdp.admin.service.user.domain.entity.AppUser;
import com.zmbdp.admin.service.user.mapper.AppUserMapper;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.redis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 重置布隆过滤器
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RefreshScope
public class ResetAppUserBloomFilterTask {

    /**
     * 布隆过滤器锁 key
     */
    private static final String BLOOM_FILTER_LOCK = "bloom:filter:lock";

    /**
     * 布隆过滤器服务
     */
    @Autowired
    private BloomFilterService bloomFilterService;

    /**
     * C端用户表
     */
    @Autowired
    private AppUserMapper appUserMapper;

    /**
     * Redisson 分布式锁服务
     */
    @Autowired
    private RedissonLockService redissonLockService;

    /**
     * 是否启用布隆过滤器刷新任务
     */
    @Value("${bloom.filter.refresh.enabled:true}")
    private boolean enabled;

    /**
     * 执行布隆过滤器刷新任务
     * 清空当前布隆过滤器并将数据库中所有用户加密手机号和微信 ID 重新加载
     */
    @Scheduled(cron = "${bloom.filter.refresh.cron:0 0 4 * * ?}")
    public void refreshBloomFilter() {
        // 如果任务被禁用，则跳过执行
        if (!enabled) {
            log.info("布隆过滤器刷新任务已禁用，跳过执行");
            return;
        }

        // 获取锁
        RLock lock = redissonLockService.acquire(BLOOM_FILTER_LOCK, 3, TimeUnit.SECONDS);

        if (null == lock) {
            log.info("布隆过滤器刷新任务已获取锁失败，跳过执行");
            return;
        }
        try {
            log.info("开始执行布隆过滤器刷新任务");

            // 查询所有用户
            List<AppUser> appUsers = appUserMapper.selectList(null);

            log.info("从数据库加载到 {} 个用户", appUsers.size());

            // 重新初始化布隆过滤器
            // 打印一下数量
            log.info("布隆过滤器重置开始，当前数量为: {}", bloomFilterService.approximateElementCount());
            bloomFilterService.reset();
            log.info("布隆过滤器重置完成，当前数量为: {}", bloomFilterService.approximateElementCount());

            // 将所有用户加密手机号和微信 ID 添加到布隆过滤器中
            int count = 0;
            for (AppUser appUser : appUsers) {
                // 添加加密手机号（如果存在）
                if (appUser.getPhoneNumber() != null && !appUser.getPhoneNumber().isEmpty()) {
                    bloomFilterService.put(appUser.getPhoneNumber());
                    count++;
                }

                // 添加微信 ID（如果存在）
                if (appUser.getOpenId() != null && !appUser.getOpenId().isEmpty()) {
                    bloomFilterService.put(appUser.getOpenId());
                    count++;
                }
            }

            if (count != appUsers.size()) {
                log.warn("布隆过滤器刷新任务执行完成，但加载的用户数据数量( {} )与数据库用户数量( {} )不一致，请检查", count, appUsers.size());
            }
            log.info("布隆过滤器刷新任务执行完成，共加载 {} 个用户数据", count);
        } catch (Exception e) {
            log.error("布隆过滤器刷新任务执行失败", e);
        } finally {
            // 释放锁
            redissonLockService.releaseLock(lock);
        }
    }
}
