package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.domain.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RedisBloom 测试控制器
 * <p>
 * 用于验证：<p>
 * 1. 初始化与重置是否生效<p>
 * 2. 元素添加与查询是否正确<p>
 * 3. 批量操作、清空等是否线程与分布式安全
 *
 * @author 稚名
 */
@Slf4j
@RestController
@RequestMapping("/test/bloom")
public class RedisBloomFilterController {

    @Autowired
    private BloomFilterService bloomFilterService;

    /**
     * 1 一键全流程测试（基础测试）<p>
     * 包含：重置 -> 批量插入 -> 校验存在/不存在 -> 打印状态与计数
     */
    @PostMapping("/fullCheck")
    public Result<Void> fullCheck() {
        log.info("🧩========== RedisBloomFilter 全流程测试开始 ==========");

        try {
            // 1 重置布隆过滤器
            log.info("[Step 1] 重置布隆过滤器...");
            bloomFilterService.reset();
            bloomFilterService.clear();

            // 2 添加一批测试数据
            List<String> testData = Arrays.asList("user_1", "user_2", "user_3", "admin", "guest");
            log.info("[Step 2] 批量添加测试数据: {}", testData);
            bloomFilterService.putAll(testData);

            // 3 校验存在的键
            log.info("[Step 3] 校验已存在的键...");
            List<String> existKeys = Arrays.asList("admin", "user_1", "guest");
            for (String key : existKeys) {
                boolean exists = bloomFilterService.mightContain(key);
                log.info("✔️ 键 '{}' 存在检测结果: {}", key, exists);
            }

            // 4 校验不存在的键
            log.info("[Step 4] 校验不存在的键...");
            List<String> nonExistKeys = Arrays.asList("nonexist", "xxx", "test_999");
            for (String key : nonExistKeys) {
                boolean exists = bloomFilterService.mightContain(key);
                log.info("❌ 键 '{}' 存在检测结果: {}", key, exists);
            }

            // 5 校验集合中是否至少有一个存在
            log.info("[Step 5] 校验集合存在性...");
            boolean anyExist = bloomFilterService.mightContainAny(Arrays.asList("foo", "bar", "admin"));
            log.info("集合 [foo, bar, admin] 是否至少有一个存在: {}", anyExist);

            // 6 打印状态与计数
            log.info("[Step 6] 获取状态与计数...");
            String status = bloomFilterService.getStatus();
            long count = bloomFilterService.exactElementCount();
            log.info("📊 当前状态: {}", status);
            log.info("📈 当前精确计数: {}", count);

            log.info("✅========== RedisBloomFilter 全流程测试完成 ==========");
            bloomFilterService.delete();
            return Result.success();
        } catch (Exception e) {
            log.error("❌ RedisBloomFilter 全流程测试失败", e);
            return Result.fail("测试执行异常: " + e.getMessage());
        }
    }

    /**
     * 2 高并发写入测试
     *
     * @param totalOps 总操作数
     * @param threads  线程数
     */
    @PostMapping("/concurrentWriteTest")
    public Result<Void> concurrentWriteTest(@RequestParam(defaultValue = "1000") int totalOps,
                                            @RequestParam(defaultValue = "10") int threads
    ) throws InterruptedException {
        log.info("=========== 高并发写入测试开始 ===========");
        bloomFilterService.reset();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(totalOps);

        long start = System.currentTimeMillis();
        for (int i = 0; i < totalOps; i++) {
            final String key = "key_" + i;
            executor.submit(() -> {
                bloomFilterService.put(key);
                latch.countDown();
            });
        }

        latch.await();
        long end = System.currentTimeMillis();
        executor.shutdown();

        long count = bloomFilterService.exactElementCount();
        double totalSeconds = (end - start) / 1000.0;
        double opsPerSecond = count / totalSeconds;

        log.info("高并发写入完成: {} 条数据, {} 线程, 耗时 {} ms", totalOps, threads, (end - start));
        log.info("当前精确计数: {}", count);
        log.info("吞吐量: {} ops/s", String.format("%.2f", opsPerSecond));

        // 打印状态和误判率
        String status = bloomFilterService.getStatus();
        double loadFactor = bloomFilterService.calculateLoadFactor();
        log.info("布隆过滤器状态: {}", status);
        if (loadFactor >= 0) {
            log.info("负载率: {}%", String.format("%.2f", loadFactor * 100));
        } else {
            log.info("负载率: RedisBloom 无法计算负载");
        }

        log.info("=========== 高并发写入测试结束 ===========");
        bloomFilterService.delete();
        return Result.success();
    }

    /**
     * 3 清空验证
     */
    @PostMapping("/clearTest")
    public Result<Void> clearTest() {
        log.info("=========== 清空验证开始 ===========");
        bloomFilterService.clear();
        log.info("清空后精确计数: {}", bloomFilterService.exactElementCount());
        boolean existCheck = bloomFilterService.mightContain("user_1");
        log.info("清空后 'user_1' 是否存在: {}", existCheck);
        log.info("=========== 清空验证结束 ===========");
        return Result.success();
    }

    /**
     * 4 性能统计测试
     *
     * @param ops     操作数
     * @param threads 线程数
     */
    @PostMapping("/performanceTest")
    public Result<Void> performanceTest(@RequestParam(defaultValue = "10000") int ops,
                                        @RequestParam(defaultValue = "10") int threads
    ) throws InterruptedException {
        log.info("=========== 性能测试开始 ===========");
        bloomFilterService.reset();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(ops);

        long start = System.currentTimeMillis();
        for (int i = 0; i < ops; i++) {
            final String key = "perf_" + i;
            executor.submit(() -> {
                bloomFilterService.put(key);
                latch.countDown();
            });
        }

        latch.await();
        long end = System.currentTimeMillis();
        executor.shutdown();

        double totalSeconds = (end - start) / 1000.0;
        double opsPerSecond = ops / totalSeconds;

        log.info("性能测试完成: {} 条数据, {} 线程, 耗时 {} ms", ops, threads, (end - start));
        log.info("吞吐量: {} ops/s", opsPerSecond);
        log.info("当前精确计数: {}", bloomFilterService.exactElementCount());
        log.info("=========== 性能测试结束 ===========");
        bloomFilterService.delete();
        return Result.success();
    }

    /**
     * 5 RedisBloom 单个 VS lua脚本 前后对比测试
     *
     * @param ops     操作数
     * @param threads 线程数
     */
    @PostMapping("/compareRedisBloomPutAndQuery")
    public Result<Void> compareRedisBloomPutAndQuery(@RequestParam(defaultValue = "10000") int ops,
                                                     @RequestParam(defaultValue = "10") int threads
    ) throws InterruptedException {
        log.info("=========== RedisBloom 单个 VS lua脚本 前后对比测试开始 ===========");
        bloomFilterService.reset();

        // 准备测试数据
        List<String> keys = new ArrayList<>(ops);
        for (int i = 0; i < ops; i++) {
            keys.add("perf_key_" + i);
        }

        // -------------------
        // 1 单条 put
        // -------------------
        log.info("Step 1: 单条 put 测试开始");
        bloomFilterService.reset();
        ExecutorService executor1 = Executors.newFixedThreadPool(threads);
        CountDownLatch latch1 = new CountDownLatch(ops);

        long start1 = System.currentTimeMillis();
        for (String key : keys) {
            executor1.submit(() -> {
                bloomFilterService.put(key);
                latch1.countDown();
            });
        }
        latch1.await();
        long end1 = System.currentTimeMillis();
        executor1.shutdown();
        double seconds1 = (end1 - start1) / 1000.0;
        double ops1 = ops / seconds1;
        log.info("单条 put 完成: {} 条数据, 耗时 {} ms, 吞吐量 {} ops/s", ops, (end1 - start1), String.format("%.2f", ops1));

        // -------------------
        // 2 Lua 批量 put
        // -------------------
        log.info("Step 2: Lua 批量 put 测试开始");
        bloomFilterService.reset();
        long start2 = System.currentTimeMillis();
        bloomFilterService.putAll(keys); // Lua 批量添加
        long end2 = System.currentTimeMillis();
        double seconds2 = (end2 - start2) / 1000.0;
        double ops2 = ops / seconds2;
        log.info("Lua 批量 put 完成: {} 条数据, 耗时 {} ms, 吞吐量 {} ops/s", ops, (end2 - start2), String.format("%.2f", ops2));

        // -------------------
        // 3 单条查询 mightContain
        // -------------------
        log.info("Step 3: 单条查询 mightContain 测试开始");
        long start3 = System.currentTimeMillis();
        for (String key : keys) {
            bloomFilterService.mightContain(key);
        }
        long end3 = System.currentTimeMillis();
        double seconds3 = (end3 - start3) / 1000.0;
        double ops3 = ops / seconds3;
        log.info("单条查询完成: {} 条数据, 耗时 {} ms, 吞吐量 {} ops/s", ops, (end3 - start3), String.format("%.2f", ops3));

        // -------------------
        // 4 Lua 批量查询 mightContainAny
        // -------------------
        log.info("Step 4: Lua 批量查询 mightContainAny 测试开始");
        long start4 = System.currentTimeMillis();
        boolean anyExist = bloomFilterService.mightContainAny(keys); // Lua 批量查询
        long end4 = System.currentTimeMillis();
        double seconds4 = (end4 - start4) / 1000.0;
        double ops4 = ops / seconds4;
        log.info("Lua 批量查询完成: {} 条数据, 耗时 {} ms, 吞吐量 {} ops/s, 查询结果 anyExist = {}", ops, (end4 - start4), String.format("%.2f", ops4), anyExist);

        log.info("=========== RedisBloom 单个 VS lua脚本 前后对比测试结束 ===========");
        bloomFilterService.delete();
        return Result.success();
    }

    /**
     * 6 全面测试（包含边界情况、并发安全、误判率等）
     */
    @PostMapping("/comprehensiveTest")
    public Result<Void> comprehensiveTest() {
        log.info("=========== RedisBloom 全面测试开始 ===========");

        try {
            // ------------------------------
            // 1 初始化与清空
            // ------------------------------
            log.info("--- 初始化与清空测试 ---");
            bloomFilterService.clear();
            log.info("布隆过滤器已清空");

            // ------------------------------
            // 2 基础功能测试
            // ------------------------------
            log.info("--- 基础功能测试 ---");
            List<String> basicData = Arrays.asList("test1", "test2", "test3");
            bloomFilterService.putAll(basicData);
            log.info("批量添加完成: {}", basicData);

            basicData.forEach(key -> log.info("键 '{}' 存在: {}", key, bloomFilterService.mightContain(key)));

            List<String> nonExistKeys = Arrays.asList("nonexist1", "nonexist2");
            nonExistKeys.forEach(key -> log.info("键 '{}' 存在: {} (应为 false 或误判)",
                    key, bloomFilterService.mightContain(key)));

            // ------------------------------
            // 3 边界值测试
            // ------------------------------
            log.info("--- 边界值测试 ---");
            bloomFilterService.put(null);
            bloomFilterService.put("");
            log.info("null键存在: {}, 空字符串存在: {}",
                    bloomFilterService.mightContain(null),
                    bloomFilterService.mightContain("")
            );

            bloomFilterService.put("test1");
            log.info("重复插入 'test1' 后负载因子: {}", String.format("%.2f", bloomFilterService.calculateLoadFactor()));

            // ------------------------------
            // 4 大量数据测试（控制在10000以内）
            // ------------------------------
            log.info("--- 大量数据测试 ---");
            int largeDataCount = 9000; // 控制在 10000 以内
            List<String> largeData = new ArrayList<>();
            for (int i = 0; i < largeDataCount; i++) {
                largeData.add("large_" + i);
            }

            // 分批插入，避免 Lua unpack 超量
            int batchSize = 1000;
            long start = System.currentTimeMillis();
            for (int i = 0; i < largeData.size(); i += batchSize) {
                List<String> batch = largeData.subList(i, Math.min(i + batchSize, largeData.size()));
                bloomFilterService.putAll(batch);
            }
            long end = System.currentTimeMillis();
            log.info("批量添加 {} 个元素耗时: {} ms", largeDataCount, (end - start));

            // ------------------------------
            // 5 误判率测试
            // ------------------------------
            log.info("--- 误判率测试 ---");
            int testCount = 10000;
            int falsePositiveCount = 0;
            for (int i = 0; i < testCount; i++) {
                String key = "false_positive_test_" + (i + 10000);
                if (bloomFilterService.mightContain(key)) {
                    falsePositiveCount++;
                }
            }
            double falsePositiveRate = (double) falsePositiveCount / testCount * 100;
            log.info("测试 {} 个不存在的元素，误判 {} 个，误判率: {}%",
                    testCount, falsePositiveCount, String.format("%.2f", falsePositiveRate));

            // ------------------------------
            // 6 并发测试（小规模）
            // ------------------------------
            log.info("--- 并发测试 ---");
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 500; i++) { // 控制数量
                final int idx = i;
                tasks.add(() -> {
                    bloomFilterService.put("concurrent_" + idx);
                    return null;
                });
            }
            executor.invokeAll(tasks);
            executor.shutdown();
            log.info("并发插入完成，精确计数: {}", bloomFilterService.exactElementCount());

            // ------------------------------
            // 7 清空与重置测试
            // ------------------------------
            log.info("负载因子: {}", String.format("%.2f", bloomFilterService.calculateLoadFactor()));
            log.info("--- 清空与重置测试 ---");
            bloomFilterService.clear();
            log.info("清空后精确计数: {}", bloomFilterService.exactElementCount());

            bloomFilterService.reset();
            log.info("重置后精确计数: {}", bloomFilterService.exactElementCount());

            // ------------------------------
            // 8 状态检查
            // ------------------------------
            log.info("--- 状态检查 ---");
            String status = bloomFilterService.getStatus();
            long exactCount = bloomFilterService.exactElementCount();
            long approxCount = bloomFilterService.approximateElementCount();
            int actualCount = bloomFilterService.actualElementCount();
            log.info("状态: {}", status);
            log.info("精确计数: {}, 近似计数: {}, 实际计数: {}", exactCount, approxCount, actualCount);
            log.info("负载因子: {}", String.format("%.2f", bloomFilterService.calculateLoadFactor()));

            log.info("=========== RedisBloom 全面测试结束 ===========");
            bloomFilterService.delete();
            return Result.success();

        } catch (Exception e) {
            log.error("全面测试过程中发生异常", e);
            bloomFilterService.delete();
            return Result.fail("测试失败: " + e.getMessage());
        }
    }
}