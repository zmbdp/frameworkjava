package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程相关工具类
 * <p>
 * 提供线程操作相关的工具方法，包括线程休眠、线程池优雅关闭、异常处理等功能。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>线程休眠（静默处理中断或抛出异常）</li>
 *     <li>线程池优雅关闭（等待任务完成）</li>
 *     <li>线程异常信息打印（支持 Future 异常提取）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 线程休眠（静默处理中断）
 * ThreadUtil.sleep(1000);  // 休眠1秒
 *
 * // 线程休眠（抛出中断异常）
 * try {
 *     ThreadUtil.sleepInterruptible(1000);
 * } catch (RuntimeException e) {
 *     // 处理中断
 * }
 *
 * // 优雅关闭线程池
 * ExecutorService executor = Executors.newFixedThreadPool(10);
 * // ... 使用线程池
 * ThreadUtil.shutdownAndAwaitTermination(executor);
 *
 * // 打印线程异常
 * ThreadUtil.printException(runnable, throwable);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>sleep 方法会静默处理中断，不会抛出异常</li>
 *     <li>sleepInterruptible 方法会抛出异常，需要调用者处理</li>
 *     <li>线程池关闭会等待最多 240 秒（120 + 120）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadUtil {

    /**
     * 线程休眠（静默处理中断）
     * <p>
     * 使当前线程暂停指定时间，如果线程被中断，会静默处理并恢复中断状态，不会抛出异常。
     * 适用于不需要处理中断异常的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 休眠1秒
     * ThreadUtil.sleep(1000);
     *
     * // 在循环中使用
     * while (condition) {
     *     // 执行任务
     *     doSomething();
     *     // 休眠5秒后继续
     *     ThreadUtil.sleep(5000);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果线程被中断，会提前结束休眠，并恢复中断状态</li>
     *     <li>不会抛出异常，适合在不需要处理中断的场景使用</li>
     *     <li>如果需要在中断时抛出异常，使用 {@link #sleepInterruptible(long)}</li>
     *     <li>milliseconds 可以为 0 或负数（不会休眠）</li>
     * </ul>
     *
     * @param milliseconds 休眠的毫秒数，可以为 0 或负数
     * @see #sleepInterruptible(long)
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
     * 线程休眠（抛出中断异常）
     * <p>
     * 使当前线程暂停指定时间，如果线程被中断，会抛出 RuntimeException 让调用者处理。
     * 适用于需要明确处理中断异常的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 休眠并处理中断
     * try {
     *     ThreadUtil.sleepInterruptible(1000);
     * } catch (RuntimeException e) {
     *     if (e.getCause() instanceof InterruptedException) {
     *         // 处理中断
     *         log.warn("线程被中断");
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果线程被中断，会抛出 RuntimeException（包装 InterruptedException）</li>
     *     <li>中断状态会被恢复</li>
     *     <li>需要调用者捕获并处理异常</li>
     *     <li>如果不需要处理中断异常，使用 {@link #sleep(long)}</li>
     *     <li>milliseconds 可以为 0 或负数（不会休眠）</li>
     * </ul>
     *
     * @param milliseconds 休眠的毫秒数，可以为 0 或负数
     * @throws RuntimeException 当线程被中断时抛出（包装 InterruptedException）
     * @see #sleep(long)
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
     * <p>
     * 优雅地关闭线程池，先尝试正常关闭，等待任务完成；如果超时则强制关闭。
     * 确保所有任务都能得到处理或取消，适合在应用程序关闭时使用。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在应用关闭时关闭线程池
     * @PreDestroy
     * public void destroy() {
     *     ThreadUtil.shutdownAndAwaitTermination(executorService);
     * }
     *
     * // 在 Spring Bean 销毁时关闭
     * @Override
     * public void destroy() {
     *     ThreadUtil.shutdownAndAwaitTermination(customExecutor);
     * }
     * }</pre>
     * <p>
     * <b>关闭流程：</b>
     * <ol>
     *     <li>首先调用 shutdown()，停止接收新任务并尝试完成所有已存在任务</li>
     *     <li>等待最多 120 秒，如果超时则调用 shutdownNow()，取消待处理任务并中断阻塞线程</li>
     *     <li>再次等待最多 120 秒，如果仍然超时则记录日志并强制退出</li>
     *     <li>对关闭过程中线程被中断的情况进行了处理</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 pool 为 null 或已经关闭，直接返回（不会抛出异常）</li>
     *     <li>总共最多等待 240 秒（120 + 120）</li>
     *     <li>如果超时，会强制关闭，未完成的任务会被取消</li>
     *     <li>在关闭过程中如果当前线程被中断，会立即强制关闭线程池</li>
     *     <li>适合在应用关闭钩子或 Bean 销毁方法中使用</li>
     * </ul>
     *
     * @param pool 需要关闭的线程池，可以为 null 或已关闭的线程池
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
     * <p>
     * 打印线程执行过程中的异常信息到日志。如果传入的是 Future 对象，会尝试获取执行结果以捕获异常。
     * 适用于线程池异常处理和日志记录。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 直接传入异常
     * ThreadUtil.printException(runnable, exception);
     *
     * // 从 Future 中提取异常
     * Future<?> future = executorService.submit(task);
     * ThreadUtil.printException(future, null);
     *
     * // 在异常处理器中使用
     * executorService.submit(() -> {
     *     try {
     *         doSomething();
     *     } catch (Exception e) {
     *         ThreadUtil.printException(null, e);
     *     }
     * });
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 throwable 为 null 且 runnable 是 Future，会尝试调用 future.get() 获取异常</li>
     *     <li>如果 future 未完成，不会等待，直接返回</li>
     *     <li>如果 future 被取消，会记录 CancellationException</li>
     *     <li>如果 future 执行异常，会提取并记录原始异常（ExecutionException.getCause()）</li>
     *     <li>如果当前线程在获取 future 结果时被中断，会恢复中断状态</li>
     *     <li>异常信息会记录到 ERROR 级别日志</li>
     * </ul>
     *
     * @param runnable  可运行对象（可以是 Runnable 或 Future），可以为 null
     * @param throwable 异常对象，如果为 null 且 runnable 是 Future，会尝试从 Future 中获取
     */
    public static void printException(Runnable runnable, Throwable throwable) {
        if (throwable == null && runnable instanceof Future<?> future) {
            try {
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
