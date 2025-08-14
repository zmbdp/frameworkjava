package com.zmbdp.admin.service.timedtask.bloom;

import com.zmbdp.admin.service.user.domain.entity.AppUser;
import com.zmbdp.admin.service.user.mapper.AppUserMapper;
import com.zmbdp.common.redis.service.BloomFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 重置布隆过滤器
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class ResetAppUserBloomFilterTask {

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
     * 每天凌晨 4 点执行布隆过滤器刷新任务
     * 清空当前布隆过滤器并将数据库中所有用户加密手机号和微信 ID 重新加载
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void refreshBloomFilter() {
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
        }
    }
}
