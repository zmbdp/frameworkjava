package com.zmbdp.common.core.config;

import com.zmbdp.common.core.enums.RejectType;
import com.zmbdp.common.core.utils.ThreadUtil;
import com.zmbdp.common.domain.constants.CommonConstants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * 线程池配置
 *
 * @author 稚名不带撇
 */
@Slf4j
@EnableAsync // 启动类上添加该注解，则该类下的方法会异步执行
@Configuration
public class ThreadPoolConfig {

    /**
     * 核心线程数，如果说 nacos 上没有，就默认是 cpu 数 + 1
     */
    @Value("${thread.pool-executor.corePoolSize:0}")
    private Integer corePoolSize;

    /**
     * 最大线程数，如果 nacos 上没有，默认就是 (核心线程数 + 1) * 2
     */
    @Value("${thread.pool-executor.maxPoolSize:0}")
    private Integer maxPoolSize;

    /**
     * 阻塞队列大小
     */
    @Value("${thread.pool-executor.queueCapacity:100}")
    private Integer queueCapacity;

    /**
     * 空闲存活时间
     */
    @Value("${thread.pool-executor.keepAliveSeconds:60}")
    private Integer keepAliveSeconds;

    /**
     * 线程名称前缀
     */
    @Value("${thread.pool-executor.prefixName:zmbdp-thread-}")
    private String prefixName;

    /**
     * 拒绝策略选择器, 看用户的选择是哪种
     */
    @Value("${thread.pool-executor.rejectHandler:2}")
    private Integer rejectHandler;

    /**
     * 初始化线程池参数
     */
    @PostConstruct
    public void initThreadPoolParameters() {
        // 获取 cpu 核数 + 1
        int cpuCore = Runtime.getRuntime().availableProcessors() + 1;

        if (corePoolSize == null || corePoolSize <= 0) {
            corePoolSize = cpuCore;
        }

        if (maxPoolSize == null || maxPoolSize <= 0) {
            maxPoolSize = cpuCore * 2;
        }

        log.info("初始化线程池参数：corePoolSize: {}, maxPoolSize: {}, queueCapacity: {}, keepAliveSeconds: {}, prefixName: {}, rejectHandler: {}",
                                corePoolSize, maxPoolSize, queueCapacity, keepAliveSeconds, prefixName, rejectHandler);
    }

    /**
     * 注册和配置线程池执行器
     *
     * @return 线程池执行器
     */
    @Bean(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
    public Executor getThreadExecutor() {
        // 创建一个异步线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // 设置核心线程数
        executor.setMaxPoolSize(maxPoolSize); // 设置最大线程数
        executor.setQueueCapacity(queueCapacity); // 设置阻塞队列大小
        executor.setKeepAliveSeconds(keepAliveSeconds); // 设置空闲存活时间
        executor.setThreadNamePrefix(prefixName); // 设置线程名称前缀
        // 设置 当线程池关闭的时候 等待所有的任务完成后再继续销毁其他的 bean
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(getRejectHandler());
        return executor;
    }

    /**
     * 根据用户的选择, 配置拒绝策略
     *
     * @return 拒绝策略处理器
     */
    public RejectedExecutionHandler getRejectHandler() {
        // 如果是 AbortPolicy 策略, 不允许任务丢失, 抛出 RejectedExecutionException 异常
        if (RejectType.AbortPolicy.getValue().equals(rejectHandler)) {
            return new ThreadPoolExecutor.AbortPolicy();
        } else if (RejectType.CallerRunsPolicy.getValue().equals(rejectHandler)) {
            // 如果是 CallerRunsPolicy 策略, 让提交任务的线程运行被拒绝的任务
            return new ThreadPoolExecutor.CallerRunsPolicy();
        } else if (RejectType.DiscardOldestPolicy.getValue().equals(rejectHandler)) {
            // 如果是 DiscardOldestPolicy 策略, 丢弃最老任务，尝试执行新任务, 无反馈
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        } else {
            // 如果是 DiscardPolicy 策略, 丢弃请求, 无反馈
            return new ThreadPoolExecutor.DiscardPolicy();
        }
    }

    /**
     * 执行周期性或定时任务
     *
     * @return 定时任务执行器
     */
    @Bean(CommonConstants.SCHEDULED_THREADS_BEAN_NAME)
    public ScheduledExecutorService scheduledExecutorService() {
        // ScheduledThreadPoolExecutor：专门用于执行需要定时或重复执行的任务
        return new ScheduledThreadPoolExecutor(
                corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        ) {
            @Override
            protected void afterExecute(Runnable runnable, Throwable throwable) {
                super.afterExecute(runnable, throwable);
                ThreadUtil.printException(runnable, throwable);
            }
        };
    }
}
