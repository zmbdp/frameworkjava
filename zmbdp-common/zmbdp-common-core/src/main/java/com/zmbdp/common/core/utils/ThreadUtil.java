package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程相关工具类<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>提供线程等待、休眠等基础操作</li>
 *     <li>提供线程池优雅关闭功能</li>
 *     <li>提供线程异常信息打印功能</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>直接调用静态方法进行线程相关操作</li>
 *     <li>适用于线程池管理、异常处理等场景</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * // 线程休眠
 * ThreadUtil.sleep(1000);  // 休眠1秒
 *
 * // 关闭线程池
 * ThreadUtil.shutdownAndAwaitTermination(executorService);
 * </pre>
 *
 * <p>
 * 工具类说明：
 * <ul>
 *     <li>不允许实例化</li>
 *     <li>所有方法均为静态方法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadUtil {

    /**
     * 线程休眠等待（单位为毫秒）- 静默处理中断
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>使当前线程暂停指定时间</li>
     *     <li>用于延迟执行、等待等场景</li>
     *     <li>被中断时会提前结束，中断状态会被恢复</li>
     * </ul>
     *
     * @param milliseconds 休眠的毫秒数
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * 线程休眠等待（单位为毫秒）- 抛出中断异常
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要明确处理中断异常的场景</li>
     *     <li>希望中断时抛出异常让调用者处理</li>
     * </ul>
     *
     * @param milliseconds 休眠的毫秒数
     * @throws RuntimeException 线程被中断时抛出
     */
    public static void sleepInterruptible(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断", e);
        }
    }

    /**
     * 优雅关闭线程池并等待任务完成
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>应用程序关闭时优雅地停止线程池</li>
     *     <li>先尝试正常关闭，如果超时则强制关闭</li>
     *     <li>确保所有任务都能得到处理或取消</li>
     * </ul>
     *
     * <p>关闭流程：</p>
     * <ol>
     *     <li>首先调用 shutdown()，停止接收新任务并尝试完成所有已存在任务</li>
     *     <li>等待最多 120 秒，如果超时则调用 shutdownNow()，取消待处理任务并中断阻塞线程</li>
     *     <li>再次等待最多 120 秒，如果仍然超时则强制退出</li>
     *     <li>对关闭过程中线程被中断的情况进行了处理</li>
     * </ol>
     *
     * @param pool 需要关闭的线程池
     */
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    if (!pool.awaitTermination(120, TimeUnit.SECONDS)) {
                        log.info("线程池未能正常终止");
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
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>处理线程执行过程中的异常</li>
     *     <li>如果传入的是 Future 对象，会尝试获取执行结果以捕获异常</li>
     *     <li>记录异常信息到日志</li>
     * </ul>
     *
     * @param runnable  可运行对象（可以是 Runnable 或 Future）
     * @param throwable 异常对象（如果为 null，会尝试从 Future 中获取）
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
