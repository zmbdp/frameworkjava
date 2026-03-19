package com.zmbdp.admin.service.timedtask.bloom;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.zmbdp.admin.service.user.domain.entity.AppUser;
import com.zmbdp.admin.service.user.mapper.AppUserMapper;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.domain.constants.BloomFilterConstants;
import com.zmbdp.common.redis.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 布隆过滤器重置 XXL-JOB Handler
 *
 * <p>Handler 名称：{@code resetBloomFilterJobHandler}
 *
 * <p>职责：
 * <ul>
 *   <li>每日凌晨 4 点（默认）由 XXL-JOB 调度中心触发，支持在管控台实时调整 CRON；
 *   <li>使用 Redisson 分布式锁保证多实例下只有一个节点执行；
 *   <li>重置布隆过滤器后，将数据库全量用户数据重新预热写入。
 * </ul>
 *
 * <p>XXL-JOB 调度中心配置参考：
 * <pre>
 * executor.handler    : resetBloomFilterJobHandler
 * schedule_type       : CRON
 * schedule_conf       : 0 0 4 * * ?
 * executor_route      : FIRST
 * block_strategy      : SERIAL_EXECUTION
 * fail_retry_count    : 1
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class ResetBloomFilterJobHandler {

    /**
     * 用户前缀
     */
    private static final String APP_USER_PREFIX = BloomFilterConstants.APP_USER_PREFIX;

    /**
     * 用户手机号前缀
     */
    private static final String APP_USER_PHONE_NUMBER_PREFIX = BloomFilterConstants.APP_USER_PHONE_NUMBER_PREFIX;

    /**
     * 用户微信 ID 前缀
     */
    private static final String APP_USER_OPEN_ID_PREFIX = BloomFilterConstants.APP_USER_OPEN_ID_PREFIX;

    /**
     * 用户邮箱前缀
     */
    private static final String APP_USER_EMAIL_PREFIX = BloomFilterConstants.APP_USER_EMAIL_PREFIX;

    /**
     * 布隆过滤器锁 key
     */
    private static final String BLOOM_FILTER_TASK_LOCK = "bloom:filter:task:lock";

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
     * 重置布隆过滤器 XXL-JOB Handler
     *
     * <p>由调度中心按 CRON 触发，也可在管控台手动触发，支持失败自动重试。
     *
     * @throws Exception 执行异常会被 XXL-JOB 捕获并记录到执行日志
     */
    @XxlJob("resetBloomFilterJobHandler")
    public void resetBloomFilter() throws Exception {
        XxlJobHelper.log("[XXL-JOB] 开始执行布隆过滤器重置任务 =======================");
        log.info("[XXL-JOB] 开始执行布隆过滤器重置任务 =======================");

        // 获取分布式锁，防止多实例并发执行
        RLock lock = redissonLockService.acquire(BLOOM_FILTER_TASK_LOCK, 10, TimeUnit.SECONDS);
        if (lock == null) {
            String msg = "[XXL-JOB] 获取分布式锁失败，跳过本次执行（可能有其他节点正在执行）";
            XxlJobHelper.log(msg);
            log.warn(msg);
            // 锁竞争属于预期行为，告知调度中心成功，无需触发告警
            XxlJobHelper.handleSuccess(msg);
            return;
        }

        try {
            long beforeCount = bloomFilterService.approximateElementCount();
            XxlJobHelper.log("[XXL-JOB] 重置前元素数量: {}", beforeCount);
            log.info("[XXL-JOB] 重置前元素数量: {}", beforeCount);

            bloomFilterService.reset();

            XxlJobHelper.log("[XXL-JOB] 布隆过滤器重置完成，重新预热中...");
            log.info("[XXL-JOB] 布隆过滤器重置完成，重新预热中...");

            int loaded = refreshAppUserBloomFilter();

            String successMsg = String.format("[XXL-JOB] 任务执行成功，共预热 %d 条用户数据", loaded);
            XxlJobHelper.log(successMsg);
            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            String errMsg = "[XXL-JOB] 布隆过滤器重置任务执行失败";
            XxlJobHelper.log(errMsg + ": " + e.getMessage());
            log.error(errMsg, e);
            // 通知调度中心失败，触发配置的失败重试策略
            XxlJobHelper.handleFail(errMsg);
        } finally {
            redissonLockService.releaseLock(lock);
        }
    }

    /**
     * 全量加载 C 端用户数据到布隆过滤器
     *
     * @return 实际写入的元素数量
     */
    public int refreshAppUserBloomFilter() {
        XxlJobHelper.log("[XXL-JOB] 开始预热 C 端用户数据 -----------------------");
        log.info("[XXL-JOB] 开始预热 C 端用户数据 -----------------------");

        List<AppUser> appUsers = appUserMapper.selectList(null);
        XxlJobHelper.log("[XXL-JOB] 从数据库加载到 {} 个用户", appUsers.size());
        log.info("[XXL-JOB] 从数据库加载到 {} 个用户", appUsers.size());

        int count = 0;
        for (AppUser appUser : appUsers) {
            if (appUser.getPhoneNumber() != null && !appUser.getPhoneNumber().isEmpty()) {
                bloomFilterService.put(APP_USER_PHONE_NUMBER_PREFIX + appUser.getPhoneNumber());
                count++;
            }
            if (appUser.getOpenId() != null && !appUser.getOpenId().isEmpty()) {
                bloomFilterService.put(APP_USER_OPEN_ID_PREFIX + appUser.getOpenId());
                count++;
            }
            if (appUser.getEmail() != null && !appUser.getEmail().isEmpty()) {
                bloomFilterService.put(APP_USER_EMAIL_PREFIX + appUser.getEmail());
                count++;
            }
            bloomFilterService.put(APP_USER_PREFIX + appUser.getId());
        }

        XxlJobHelper.log("[XXL-JOB] 用户数据预热完成，共写入 {} 个元素 -----------------------", count);
        log.info("[XXL-JOB] 用户数据预热完成，共写入 {} 个元素 -----------------------", count);
        return count;
    }
}