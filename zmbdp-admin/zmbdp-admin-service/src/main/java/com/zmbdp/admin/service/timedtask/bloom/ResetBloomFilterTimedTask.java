package com.zmbdp.admin.service.timedtask.bloom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器重置兜底任务（Spring Scheduled 降级方案）
 *
 * <p><b>架构说明：</b>
 * 正常情况下，布隆过滤器重置由 XXL-Job 调度中心统一管理（参见 {@link ResetBloomFilterJobHandler}）。
 * 本类作为 <b>降级兜底</b>：当 XXL-Job 调度中心不可用时，
 * 通过将 {@code bloom.filter.fallback.enabled} 设为 {@code true} 启用本地定时任务，
 * 保证布隆过滤器在任何情况下都能得到重置，不影响系统可用性。
 *
 * <p><b>默认行为：</b>禁用（由 XXL-Job 接管调度）。
 *
 * <p><b>开启方式：</b>在 Nacos 配置中心动态下发以下配置：
 * <pre>
 * bloom:
 *   filter:
 *     fallback:
 *       enabled: true
 *       cron: 0 0 4 * * ?
 * </pre>
 *
 * @author 稚名不带撇
 * @see ResetBloomFilterJobHandler XXL-Job 主调度 Handler
 */
@Slf4j
@Component
@RefreshScope
public class ResetBloomFilterTimedTask {

    /**
     * XXL-Job Handler（降级时委托给它执行，避免重复代码）
     */
    @Autowired
    private ResetBloomFilterJobHandler resetBloomFilterJobHandler;

    /**
     * 是否启用降级本地定时任务（默认 false，由 XXL-Job 接管）
     */
    @Value("${bloom.filter.fallback.enabled:false}")
    private boolean fallbackEnabled;

    /**
     * 降级兜底定时任务
     *
     * <p>仅在 {@code bloom.filter.fallback.enabled=true} 时实际执行。
     * CRON 支持通过 Nacos 动态刷新（@RefreshScope）。
     */
    @Scheduled(cron = "${bloom.filter.fallback.cron:0 0 4 * * ?}")
    public void fallbackRefreshBloomFilter() {
        if (!fallbackEnabled) {
            return;
        }
        log.warn("[Fallback] XXL-Job 降级模式激活：通过本地 Spring Scheduled 执行布隆过滤器重置");
        try {
            resetBloomFilterJobHandler.resetBloomFilter();
        } catch (Exception e) {
            log.error("[Fallback] 布隆过滤器降级重置任务执行失败", e);
        }
    }
}
