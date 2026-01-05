package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程相关工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 禁止实例化，只能通过静态方法获取实例
public class ThreadUtil {

    /**
     * sleep 等待，单位为毫秒
     *
     * @param milliseconds 毫秒数
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            return;
        }
    }

    /**
     * 停止线程池<br>
     * 先使用 shutdown，停止接收新任务并尝试完成所有已存在任务<br>
     * 如果超时，则调用 shutdownNow，取消在 workQueue 中 Pending 的任务，并中断所有阻塞函数<br>
     * 如果仍然超时，则强制退出<br>
     * 另对在 shutdown 时线程本身被调用中断做了处理<br>
     *
     * @param pool 线程池
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                        log.info("Pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 打印线程异常信息
     *
     * @param runnable 线程
     * @param throwable 异常
     */
    public static void printException(Runnable runnable, Throwable throwable) {
        if (throwable == null && runnable instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) runnable;
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                throwable = ce;
            } catch (ExecutionException ee) {
                throwable = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (throwable != null) {
            log.error(throwable.getMessage(), throwable);
        }
    }
}
