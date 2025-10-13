package com.zmbdp.mstemplate.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zmbdp.common.bloomfilter.config.BloomFilterConfig;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.cache.utils.CacheUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.domain.User;
import com.zmbdp.mstemplate.service.service.IClothService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/test/cache")
public class TestCacheController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private Cache<String, Object> caffeineCache;

    @Autowired
    private IClothService clothService;

    @Autowired
    private BloomFilterService bloomFilterService;


    /**
     * 测试布隆过滤器配置功能
     */
    @Autowired
    private BloomFilterConfig bloomFilterConfig;


    @GetMapping("/get")
    public Result<Void> get() {
        String key = "testKey";
        CacheUtil.getL2Cache(redisService, key, new TypeReference<List<Map<String, RegionTest>>>() {
        }, caffeineCache);
        return Result.success();
    }

    @GetMapping("/cloth/get")
    public Result<Integer> clothGet(Long proId) {
        return Result.success(clothService.clothPriceGet(proId));
    }

    /**
     * 测试布隆过滤器功能
     */
    @PostMapping("/bloom/filter")
    public Result<Void> testBloomFilter() {
        try {
            log.info("============================   测试布隆过滤器功能   ============================");

            log.info("布隆过滤器重置开始，当前数量为: {}", bloomFilterService.approximateElementCount());
            bloomFilterService.reset();
            log.info("布隆过滤器重置完成，当前数量为: {}", bloomFilterService.approximateElementCount());

            // 1. 测试添加元素到布隆过滤器
            log.info("--- 测试添加元素到布隆过滤器 ---");
            String key1 = "bloom:1001";
            String key2 = "bloom:1002";
            String key3 = "product:2001";

            bloomFilterService.put(key1);
            bloomFilterService.put(key2);
            bloomFilterService.put(key3);

            log.info("成功添加3个元素到布隆过滤器: {}, {}, {}", key1, key2, key3);

            // 2. 测试存在的元素
            log.info("--- 测试存在的元素 ---");
            boolean containsKey1 = bloomFilterService.mightContain(key1);
            boolean containsKey2 = bloomFilterService.mightContain(key2);
            boolean containsKey3 = bloomFilterService.mightContain(key3);

            log.info("检查元素 '{}' 是否存在: {}", key1, containsKey1);
            log.info("检查元素 '{}' 是否存在: {}", key2, containsKey2);
            log.info("检查元素 '{}' 是否存在: {}", key3, containsKey3);

            // 3. 测试不存在的元素
            log.info("--- 测试不存在的元素 ---");
            String nonExistentKey1 = "bloom:9999";
            String nonExistentKey2 = "product:8888";

            boolean containsNonExistent1 = bloomFilterService.mightContain(nonExistentKey1);
            boolean containsNonExistent2 = bloomFilterService.mightContain(nonExistentKey2);

            log.info("检查不存在的元素 '{}' 是否存在: {}", nonExistentKey1, containsNonExistent1);
            log.info("检查不存在的元素 '{}' 是否存在: {}", nonExistentKey2, containsNonExistent2);

            // 4. 测试与缓存结合使用
            log.info("--- 测试与缓存结合使用 ---");

            // 先添加数据到缓存中
            User user1 = new User();
            user1.setName("张三");
            user1.setAge(25);

            User user2 = new User();
            user2.setName("李四");
            user2.setAge(30);

            // 使用带布隆过滤器的缓存方法存储数据
            String userKey1 = "bloom:cache:1001";
            String userKey2 = "bloom:cache:1002";

            // 模拟存储到缓存并更新布隆过滤器
            redisService.setCacheObject(userKey1, user1);
            redisService.setCacheObject(userKey2, user2);
            bloomFilterService.put(userKey1);
            bloomFilterService.put(userKey2);

            log.info("将用户数据存储到缓存并添加到布隆过滤器: {}, {}", userKey1, userKey2);

            // 使用布隆过滤器优化的缓存获取方法
            log.info("--- 测试布隆过滤器优化的缓存获取 ---");

            // 测试存在的键
            if (bloomFilterService.mightContain(userKey1)) {
                User cachedUser1 = redisService.getCacheObject(userKey1, User.class);
                log.info("通过布隆过滤器检查后获取用户数据: {}", cachedUser1);
            } else {
                log.info("布隆过滤器显示键 {} 不存在，跳过Redis查询", userKey1);
            }

            // 测试不存在的键
            String nonExistentUserKey = "bloom:cache:9999";
            if (bloomFilterService.mightContain(nonExistentUserKey)) {
                User cachedUser = redisService.getCacheObject(nonExistentUserKey, User.class);
                log.info("通过布隆过滤器检查后获取不存在的用户数据: {}", cachedUser);
            } else {
                log.info("布隆过滤器显示键 {} 不存在，跳过Redis查询，有效减少缓存穿透", nonExistentUserKey);
            }

            // 5. 测试大量数据的误判率
            log.info("--- 测试大量数据的误判率 ---");
            int testCount = 10000;
            int existCount = 0;
            int nonExistCount = 0;

            // 添加一部分数据到布隆过滤器
            for (int i = 0; i < testCount / 2; i++) {
                bloomFilterService.put("test:data:" + i);
            }

            // 检查存在的数据
            for (int i = 0; i < testCount / 2; i++) {
                if (bloomFilterService.mightContain("test:data:" + i)) {
                    existCount++;
                }
            }

            // 检查不存在的数据
            for (int i = testCount / 2; i < testCount; i++) {
                if (bloomFilterService.mightContain("test:data:" + i)) {
                    nonExistCount++;
                }
            }

            double existRate = (double) existCount / (testCount / 2) * 100;
            double falsePositiveRate = (double) nonExistCount / (testCount / 2) * 100;

            log.info("测试数据总量: {}", testCount);
            log.info("存在的数据数量: {}, 命中率: {}%", testCount / 2, existRate);
            log.info("不存在的数据中被误判数量: {}, 误判率: {}%", nonExistCount, falsePositiveRate);

            log.info("=== 布隆过滤器功能测试完成 ===");
            return Result.success();

        } catch (Exception e) {
            log.error("测试布隆过滤器功能过程中发生异常", e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试 CacheUtil 中集成布隆过滤器的方法
     */
    @PostMapping("/util/bloom")
    public Result<Void> testCacheUtilWithBloom() {
        try {
            log.info("============================   测试CacheUtil集成布隆过滤器   ============================");

            log.info("布隆过滤器重置开始，当前数量为: {}", bloomFilterService.approximateElementCount());
            bloomFilterService.reset();
            log.info("布隆过滤器重置完成，当前数量为: {}", bloomFilterService.approximateElementCount());

            // 创建测试用的本地缓存
            Cache<String, Object> caffeineCache =
                    Caffeine.newBuilder()
                            .maximumSize(100)
                            .build();

            // 1. 测试不使用布隆过滤器的缓存方法
            log.info("--- 测试不使用布隆过滤器的缓存方法 ---");
            String key1 = "test:cache:1";
            String value1 = "测试值1";

            // 存储数据（不使用布隆过滤器）
            CacheUtil.setL2Cache(redisService, key1, value1, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("使用不带布隆过滤器的方法存储数据: {} = {}", key1, value1);

            // 获取数据（不使用布隆过滤器）
            String result1 = CacheUtil.getL2Cache(redisService, key1, String.class, caffeineCache);
            log.info("使用不带布隆过滤器的方法获取数据: {}", result1);

            // 2. 测试使用布隆过滤器的缓存方法
            log.info("--- 测试使用布隆过滤器的缓存方法 ---");
            String key2 = "test:cache:2";
            String value2 = "测试值2";

            // 存储数据（使用布隆过滤器）
            CacheUtil.setL2Cache(redisService, bloomFilterService, key2, value2, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("使用带布隆过滤器的方法存储数据: {} = {}", key2, value2);

            // 获取数据（使用布隆过滤器）
            String result2 = CacheUtil.getL2Cache(redisService, bloomFilterService, key2, String.class, caffeineCache);
            log.info("使用带布隆过滤器的方法获取数据: {}", result2);

            // 3. 测试布隆过滤器防止缓存穿透
            log.info("--- 测试布隆过滤器防止缓存穿透 ---");
            String nonExistentKey = "test:cache:nonexistent";

            // 先确保这个键不在布隆过滤器中
            boolean mightContain = bloomFilterService.mightContain(nonExistentKey);
            log.info("键 {} 是否可能存在于布隆过滤器中: {}", nonExistentKey, mightContain);

            if (!mightContain) {
                // 使用带布隆过滤器的方法获取不存在的数据
                String result3 = CacheUtil.getL2Cache(redisService, bloomFilterService, nonExistentKey, new TypeReference<String>() {
                }, caffeineCache);
                log.info("通过布隆过滤器检查不存在的键，结果: {}", result3);
                log.info("由于布隆过滤器判断键不存在，避免了Redis查询");
            }

            // 4. 测试复杂对象的缓存
            log.info("--- 测试复杂对象的缓存 ---");
            String userKey = "test:cache:bloom:1";
            User testUser = new User();
            testUser.setName("测试用户");
            testUser.setAge(28);

            // 存储复杂对象（使用布隆过滤器）
            CacheUtil.setL2Cache(redisService, bloomFilterService, userKey, testUser, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("使用带布隆过滤器的方法存储复杂对象: {}", testUser);

            // 获取复杂对象（使用布隆过滤器）
            User cachedUser = CacheUtil.getL2Cache(redisService, bloomFilterService, userKey, new TypeReference<User>() {
            }, caffeineCache);
            log.info("使用带布隆过滤器的方法获取复杂对象: {}", cachedUser);

            // 5. 测试性能对比
            log.info("--- 测试性能对比 ---");
            int testIterations = 2000;

            // 清空一下布隆过滤器
            bloomFilterService.reset();
            // 测试不使用布隆过滤器的性能
            long start1 = System.currentTimeMillis();
            for (int i = 0; i < testIterations; i++) {
                CacheUtil.getL2Cache(redisService, "nonexistent:key:" + i, new TypeReference<String>() {
                }, caffeineCache);
            }
            long end1 = System.currentTimeMillis();

            // 测试使用布隆过滤器的性能
            long start2 = System.currentTimeMillis();
            for (int i = 0; i < testIterations; i++) {
                CacheUtil.getL2Cache(redisService, bloomFilterService, "nonexistent:key:" + i, new TypeReference<String>() {
                }, caffeineCache);
            }
            long end2 = System.currentTimeMillis();

            log.info("不使用布隆过滤器查询 {} 次耗时: {} ms", testIterations, (end1 - start1));
            log.info("使用布隆过滤器查询 {} 次耗时: {} ms", testIterations, (end2 - start2));
            log.info("性能提升: {} ms", (end1 - start1) - (end2 - start2));

            log.info("=== CacheUtil集成布隆过滤器测试完成 ===");
            return Result.success();

        } catch (Exception e) {
            log.error("测试CacheUtil集成布隆过滤器过程中发生异常", e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }

    @PostMapping("/bloom/config")
    public Result<Void> testBloomFilterConfig() {
        try {
            log.info("============================   测试布隆过滤器配置   ============================");

            log.info("布隆过滤器重置开始，当前数量为: {}", bloomFilterService.approximateElementCount());
            bloomFilterService.reset();
            log.info("布隆过滤器重置完成，当前数量为: {}", bloomFilterService.approximateElementCount());

            // 1. 测试配置参数获取
            log.info("--- 测试配置参数获取 ---");
            int expectedInsertions = bloomFilterConfig.getExpectedInsertions();
            double falseProbability = bloomFilterConfig.getFalseProbability();

            log.info("布隆过滤器配置 - 预期插入元素数量: {}", expectedInsertions);
            log.info("布隆过滤器配置 - 误判率: {}", falseProbability);

            // 2. 测试动态配置生效
            log.info("--- 测试动态配置生效 ---");
            // 重新初始化布隆过滤器以应用配置
            // 注意：在实际应用中，这种重新初始化可能需要更复杂的处理

            log.info("布隆过滤器配置已加载，预期插入元素数量: {}, 误判率: {}",
                    bloomFilterConfig.getExpectedInsertions(),
                    bloomFilterConfig.getFalseProbability());

            // 3. 测试配置参数的合理性
            log.info("--- 测试配置参数的合理性 ---");
            if (expectedInsertions > 0) {
                log.info("预期插入元素数量配置合理: {}", expectedInsertions);
            } else {
                log.warn("预期插入元素数量配置不合理: {}", expectedInsertions);
            }

            if (falseProbability > 0 && falseProbability < 1) {
                log.info("误判率配置合理: {}", falseProbability);
            } else {
                log.warn("误判率配置不合理: {}", falseProbability);
            }

            // 4. 测试配置对布隆过滤器的影响
            log.info("--- 测试配置对布隆过滤器的影响 ---");

            // 添加一些测试数据到布隆过滤器
            int insertCount = Math.min(expectedInsertions / 10, 100);
            for (int i = 0; i < insertCount; i++) {
                bloomFilterService.put("config:test:" + i);
            }
            log.info("向布隆过滤器中添加了 {} 个元素", insertCount);

            // 测试存在的元素命中率（应该接近100%）
            int existHitCount = 0;
            for (int i = 0; i < insertCount; i++) {
                if (bloomFilterService.mightContain("config:test:" + i)) {
                    existHitCount++;
                }
            }
            double existHitRate = insertCount > 0 ? (double) existHitCount / insertCount * 100 : 0;
            log.info("存在元素测试数量: {}, 命中数量: {}, 命中率: {}%", insertCount, existHitCount, existHitRate);

            // 测试不存在的元素误判率
            int nonExistTestCount = 1000;
            int falsePositiveCount = 0;
            for (int i = insertCount; i < insertCount + nonExistTestCount; i++) {
                if (bloomFilterService.mightContain("config:test:" + i)) {
                    falsePositiveCount++;
                }
            }
            double falsePositiveRate = nonExistTestCount > 0 ? (double) falsePositiveCount / nonExistTestCount * 100 : 0;
            log.info("不存在元素测试数量: {}, 误判数量: {}, 误判率: {}%", nonExistTestCount, falsePositiveCount, falsePositiveRate);

            log.info("配置的误判率: {}，实际测试误判率: {}", falseProbability, falsePositiveRate);

            log.info("=== 布隆过滤器配置测试完成 ===");
            return Result.success();

        } catch (Exception e) {
            log.error("测试布隆过滤器配置过程中发生异常", e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }

    /**
     * 检查布隆过滤器配置
     *
     * @return 布隆过滤器配置信息
     */
    @PostMapping("/bloom/config/check")
    public Result<Map<String, Object>> checkBloomFilterConfig() {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取配置值
            result.put("expectedInsertions", bloomFilterConfig.getExpectedInsertions());
            result.put("falseProbability", bloomFilterConfig.getFalseProbability());
            result.put("warningThreshold", bloomFilterConfig.getWarningThreshold());
            result.put("bloomFilterStatus", bloomFilterService.getStatus());

            log.info("布隆过滤器配置检查: {}", result);

            return Result.success(result);
        } catch (Exception e) {
            log.error("检查布隆过滤器配置时发生异常", e);
            return Result.fail("检查失败: " + e.getMessage());
        }
    }

    /**
     * 测试布隆过滤器扩容功能
     */
    @PostMapping("/bloom/expand")
    public Result<Void> testBloomFilterExpansion() {
        try {
            log.info("============================   测试布隆过滤器扩容功能   ============================");

            // 1. 重置布隆过滤器到小容量
            log.info("--- 重置布隆过滤器到小容量 ---");
            log.info("扩容前状态: {}", bloomFilterService.getStatus());
            bloomFilterService.reset();
            log.info("重置后状态: {}", bloomFilterService.getStatus());

            // 2. 添加超过容量的元素以触发警告
            log.info("--- 添加元素测试负载警告 ---");
            for (int i = 0; i < 8; i++) {
                bloomFilterService.put("test:small:" + i);
                log.info("添加元素 test:small:{} 后状态: {}", i, bloomFilterService.getStatus());
            }

            // 3. 验证元素存在性
            log.info("--- 验证元素存在性 ---");
            for (int i = 0; i < 8; i++) {
                boolean exists = bloomFilterService.mightContain("test:small:" + i);
                log.info("元素 test:small:{} 存在: {}", i, exists);
            }

            // 4. 测试不存在的元素
            log.info("--- 测试不存在的元素 ---");
            boolean notExists = bloomFilterService.mightContain("test:small:999");
            log.info("不存在的元素 test:small:999 检查结果: {}", notExists);

            // 5. 手动扩容测试
            log.info("--- 手动扩容测试 ---");
            log.info("扩容前实际元素数量: {}", bloomFilterService.actualElementCount());

            // 执行扩容（模拟用户在Nacos上修改配置后调用）
            bloomFilterService.expand();

            log.info("扩容后状态: {}", bloomFilterService.getStatus());
            log.info("扩容后实际元素数量: {}", bloomFilterService.actualElementCount());

            // 6. 验证扩容后元素仍然存在
            log.info("--- 验证扩容后元素仍然存在 ---");
            for (int i = 0; i < 8; i++) {
                boolean exists = bloomFilterService.mightContain("test:small:" + i);
                log.info("扩容后元素 test:small:{} 存在: {}", i, exists);
            }

            // 7. 添加更多元素测试扩容后容量
            log.info("--- 添加更多元素测试扩容后容量 ---");
            for (int i = 8; i < 200; i++) {
                bloomFilterService.put("test:small:" + i);
            }

            // 验证新添加的元素存在
            for (int i = 8; i < 200; i++) {
                boolean exists = bloomFilterService.mightContain("test:small:" + i);
                log.info("新添加元素 test:small:{} 存在: {}", i, exists);
            }

            log.info("扩容后最终状态: {}", bloomFilterService.getStatus());

            // 8. 边界情况测试
            log.info("--- 边界情况测试 ---");

            // 测试空值处理
            bloomFilterService.put(null);
            log.info("尝试添加null值后状态: {}", bloomFilterService.getStatus());

            bloomFilterService.put("");
            log.info("尝试添加空字符串后状态: {}", bloomFilterService.getStatus());

            // 测试null和空字符串检查
            boolean nullCheck = bloomFilterService.mightContain(null);
            boolean emptyCheck = bloomFilterService.mightContain("");
            log.info("null值检查结果: {}", nullCheck);
            log.info("空字符串检查结果: {}", emptyCheck);

            // 9. 性能测试
            log.info("--- 性能测试 ---");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                bloomFilterService.put("perf:test:" + i);
            }
            long endTime = System.currentTimeMillis();
            log.info("添加1000个元素耗时: {} ms", endTime - startTime);

            startTime = System.currentTimeMillis();
            int mightContainCount = 0;
            for (int i = 0; i < 1000; i++) {
                if (bloomFilterService.mightContain("perf:test:" + i)) {
                    mightContainCount++;
                }
            }
            endTime = System.currentTimeMillis();
            log.info("查询1000个元素耗时: {} ms, 命中数量: {}", endTime - startTime, mightContainCount);

            log.info("=== 布隆过滤器扩容功能测试完成 ===");
            return Result.success();

        } catch (Exception e) {
            log.error("测试布隆过滤器扩容功能过程中发生异常", e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }

    @PostMapping("/threads/bloom")
    public Result<Void> testBloomFilterThreads() {
        log.info("开始布隆过滤器线程安全测试");

        // 保存初始状态
        long initialCount = bloomFilterService.approximateElementCount();
        String initialStatus = bloomFilterService.getStatus();
        log.info("测试前状态 - 元素数量: {}, 状态: {}", initialCount, initialStatus);

        // 重置布隆过滤器确保干净的测试环境
        bloomFilterService.reset();
        log.info("重置后状态: {}", bloomFilterService.getStatus());

        // 准备测试数据
        int threadCount = 10;
        int itemsPerThread = 100;
        List<Thread> threads = new ArrayList<>();
        List<String> addedKeys = Collections.synchronizedList(new ArrayList<>());

        // 创建多个线程并发添加元素
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < itemsPerThread; i++) {
                    String key = "thread:" + threadId + ":item:" + i;
                    bloomFilterService.put(key);
                    addedKeys.add(key);
                }
                log.info("线程 {} 完成添加 {} 个元素", threadId, itemsPerThread);
            });
            threads.add(thread);
        }

        // 启动所有线程
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程执行完成
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            log.error("线程执行中断", e);
            Thread.currentThread().interrupt();
            return Result.fail("测试被中断");
        }

        long endTime = System.currentTimeMillis();

        // 测试结果分析
        long expectedElements = (long) threadCount * itemsPerThread;
        long actualElements = bloomFilterService.approximateElementCount();
        String finalStatus = bloomFilterService.getStatus();

        log.info("测试完成 - 预期元素数量: {}, 实际元素数量: {}, 耗时: {}ms",
                expectedElements, actualElements, endTime - startTime);
        log.info("最终状态: {}", finalStatus);

        // 验证1: 元素数量是否大致正确（布隆过滤器的approximateElementCount可能有轻微误差）
        double countDeviation = Math.abs((double) (actualElements - expectedElements) / expectedElements);
        log.info("元素数量偏差率: {}%", String.format("%.2f", countDeviation * 100));

        // 验证2: 所有添加的元素是否都能被正确识别
        AtomicInteger foundCount = new AtomicInteger(0);
        AtomicInteger notFoundCount = new AtomicInteger(0);

        // 并发查询测试
        List<Thread> queryThreads = new ArrayList<>();
        for (int t = 0; t < 5; t++) {
            final int threadId = t;
            Thread queryThread = new Thread(() -> {
                int localFound = 0;
                int localNotFound = 0;
                for (int i = threadId; i < addedKeys.size(); i += 5) {
                    String key = addedKeys.get(i);
                    if (bloomFilterService.mightContain(key)) {
                        localFound++;
                    } else {
                        localNotFound++;
                        log.warn("元素未找到: {}", key);
                    }
                }
                foundCount.addAndGet(localFound);
                notFoundCount.addAndGet(localNotFound);
            });
            queryThreads.add(queryThread);
        }

        // 启动查询线程
        for (Thread thread : queryThreads) {
            thread.start();
        }

        // 等待查询完成
        try {
            for (Thread thread : queryThreads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            log.error("查询线程执行中断", e);
            Thread.currentThread().interrupt();
            return Result.fail("测试被中断");
        }

        log.info("查询结果 - 找到: {}, 未找到: {}", foundCount.get(), notFoundCount.get());

        // 验证3: 状态信息的一致性
        String status1 = bloomFilterService.getStatus();
        String status2 = bloomFilterService.getStatus();
        boolean statusConsistent = status1.equals(status2);
        log.info("状态信息一致性: {}", statusConsistent ? "一致" : "不一致");

        // 验证4: 精确计数与近似计数的比较（如果有提供）
        try {
            long exactCount = bloomFilterService.exactElementCount();
            long approxCount = bloomFilterService.approximateElementCount();
            log.info("精确计数: {}, 近似计数: {}, 差异: {}", exactCount, approxCount, Math.abs(exactCount - approxCount));
        } catch (Exception e) {
            log.warn("无法获取精确计数: {}", e.getMessage());
        }

        // 测试结论
        boolean testPassed = true;
        StringBuilder resultMessage = new StringBuilder();

        if (countDeviation > 0.1) { // 允许10%的偏差
            testPassed = false;
            resultMessage.append("元素计数偏差过大; ");
        }

        if (notFoundCount.get() > 0) {
            testPassed = false;
            resultMessage.append("存在元素丢失; ");
        }

        if (!statusConsistent) {
            resultMessage.append("状态信息不一致; ");
        }

        if (testPassed) {
            log.info("并发测试成功：布隆过滤器在高并发环境下表现稳定" + "并发测试成功，处理了" + expectedElements + "个元素，耗时" + (endTime - startTime) + "ms");
            return Result.success();
        } else {
            log.error("并发测试发现问题：{}", resultMessage.toString());
            return Result.fail("并发测试失败: " + resultMessage.toString());
        }
    }

    @PostMapping("/threads/bloom/stress")
    public Result<Void> stressTestBloomFilter() {
        log.info("🧪 开始布隆过滤器压力测试");

        // 重置布隆过滤器
        bloomFilterService.reset();
        log.info("初始状态: {}", bloomFilterService.getStatus());

        // 高并发压力测试参数
        int threadCount = 20;
        int operationsPerThread = 500;
        List<Thread> threads = new ArrayList<>();

        // 性能计数器
        AtomicInteger putSuccessCount = new AtomicInteger(0);
        AtomicInteger querySuccessCount = new AtomicInteger(0);
        AtomicInteger queryFalseCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 创建混合操作线程（添加和查询）
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    if (Math.random() < 0.7) {
                        String key = "stress:" + threadId + ":" + i;
                        bloomFilterService.put(key);
                        putSuccessCount.incrementAndGet();
                    } else {
                        String key = "stress:" + threadId + ":" + (i + 10000);
                        boolean result = bloomFilterService.mightContain(key);
                        if (result) {
                            querySuccessCount.incrementAndGet();
                        } else {
                            queryFalseCount.incrementAndGet();
                        }
                    }
                }
                log.info("线程 {} 完成", threadId);
            });
            threads.add(thread);
        }

        // 启动线程
        threads.forEach(Thread::start);

        // 等待线程完成
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            log.error("压力测试线程执行中断", e);
            Thread.currentThread().interrupt();
            return Result.fail("测试被中断");
        }

        long endTime = System.currentTimeMillis();
        long totalOps = putSuccessCount.get() + querySuccessCount.get() + queryFalseCount.get();
        double durationSec = (endTime - startTime) / 1000.0;
        double throughput = totalOps / durationSec;

        // 输出结果
        log.info("💥 压力测试完成，耗时: {}ms ({}秒)", endTime - startTime, durationSec);
        log.info("添加操作成功次数: {}", putSuccessCount.get());
        log.info("查询操作成功次数: {}，查询失败次数: {}", querySuccessCount.get(), queryFalseCount.get());
        log.info("最终状态: {}", bloomFilterService.getStatus());
        log.info("总操作数: {}, 吞吐量: {} ops/s", totalOps, String.format("%.2f", throughput));

        // 验证状态一致性
        String status1 = bloomFilterService.getStatus();
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String status2 = bloomFilterService.getStatus();

        if (status1.equals(status2)) {
            log.info("压力测试后状态信息一致 ✅");
        } else {
            log.warn("压力测试后状态信息不一致 ⚠️");
            log.warn("状态1: {}", status1);
            log.warn("状态2: {}", status2);
        }
        bloomFilterService.clear();
        return Result.success();
    }

    /**
     * 全面测试 CacheUtil 所有功能
     */
    @GetMapping("/comprehensive/test")
    public Result<Map<String, Object>> comprehensiveTest() {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            log.info("============================   全面测试 CacheUtil 功能   ============================");

            // 1. 测试基本的本地缓存功能
            log.info("--- 测试基本本地缓存功能 ---");
            String localKey = "comprehensive:test:local";
            String localValue = "Local Cache Value";

            // 存储到本地缓存
            CacheUtil.setL2Cache(localKey, localValue, caffeineCache);
            log.info("已存储到本地缓存: {} = {}", localKey, localValue);

            // 从本地缓存获取
            String localResult = CacheUtil.getL2Cache(redisService, localKey, String.class, caffeineCache);
            resultMap.put("localCacheResult", localResult);
            log.info("从本地缓存获取结果: {}", localResult);

            // 删除本地缓存
            CacheUtil.delL1Cache(localKey, caffeineCache);
            String localResultAfterDelete = CacheUtil.getL2Cache(redisService, localKey, String.class, caffeineCache);
            log.info("删除本地缓存后获取结果: {}", localResultAfterDelete);

            // 2. 测试Redis缓存功能
            log.info("--- 测试Redis缓存功能 ---");
            String redisKey = "comprehensive:test:redis";
            String redisValue = "Redis Cache Value";

            // 存储到Redis缓存
            CacheUtil.setL2Cache(redisService, redisKey, redisValue, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("已存储到Redis缓存: {} = {}", redisKey, redisValue);

            // 清空本地缓存以确保从Redis获取
            caffeineCache.invalidate(redisKey);

            // 从Redis缓存获取
            String redisResult = CacheUtil.getL2Cache(redisService, redisKey, String.class, caffeineCache);
            resultMap.put("redisCacheResult", redisResult);
            log.info("从Redis缓存获取结果: {}", redisResult);

            // 删除Redis缓存
            CacheUtil.delL2Cache(redisKey, redisService);
            caffeineCache.invalidate(redisKey); // 同时清除本地缓存
            String redisResultAfterDelete = CacheUtil.getL2Cache(redisService, redisKey, String.class, caffeineCache);
            resultMap.put("redisCacheAfterDelete", redisResultAfterDelete);
            log.info("删除Redis缓存后获取结果: {}", redisResultAfterDelete);

            // 3. 测试复杂对象缓存功能
            log.info("--- 测试复杂对象缓存功能 ---");
            String objectKey = "comprehensive:test:object";
            User testUser = new User();
            testUser.setName("综合测试用户");
            testUser.setAge(30);

            // 存储复杂对象到缓存
            CacheUtil.setL2Cache(redisService, objectKey, testUser, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("已存储复杂对象到缓存: {}", testUser);

            // 清空本地缓存以确保从Redis获取
            caffeineCache.invalidate(objectKey);

            // 从缓存获取复杂对象
            User cachedUser = CacheUtil.getL2Cache(redisService, objectKey, new TypeReference<User>() {
            }, caffeineCache);
            resultMap.put("objectCacheResult", cachedUser != null ? cachedUser.getName() + "(" + cachedUser.getAge() + ")" : null);
            log.info("从缓存获取复杂对象: {}", cachedUser);

            // 4. 测试布隆过滤器功能
            log.info("--- 测试布隆过滤器功能 ---");
            String bloomKey = "comprehensive:test:bloom";
            String bloomValue = "Bloom Filter Value";

            // 存储数据并更新布隆过滤器
            CacheUtil.setL2Cache(redisService, bloomFilterService, bloomKey, bloomValue, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("已存储数据并更新布隆过滤器: {} = {}", bloomKey, bloomValue);

            // 检查存在的键
            boolean mightContainExist = bloomFilterService.mightContain(bloomKey);
            resultMap.put("bloomFilterExistKey", mightContainExist);
            log.info("布隆过滤器检查存在的键 {}: {}", bloomKey, mightContainExist);

            // 从缓存获取数据（通过布隆过滤器）
            String bloomResult = CacheUtil.getL2Cache(redisService, bloomFilterService, bloomKey, String.class, caffeineCache);
            resultMap.put("bloomFilterCacheResult", bloomResult);
            log.info("通过布隆过滤器从缓存获取数据: {}", bloomResult);

            // 检查不存在的键
            String nonExistKey = "comprehensive:test:nonexist";
            boolean mightContainNonExist = bloomFilterService.mightContain(nonExistKey);
            resultMap.put("bloomFilterNonExistKey", mightContainNonExist);
            log.info("布隆过滤器检查不存在的键 {}: {}", nonExistKey, mightContainNonExist);

            // 尝试获取不存在的数据（应该被布隆过滤器拦截）
            String nonExistResult = CacheUtil.getL2Cache(redisService, bloomFilterService, nonExistKey, String.class, caffeineCache);
            resultMap.put("bloomFilterNonExistResult", nonExistResult);
            log.info("通过布隆过滤器获取不存在的数据结果: {}", nonExistResult);

            // 5. 测试同时删除本地和Redis缓存
            log.info("--- 测试同时删除本地和Redis缓存 ---");
            String bothKey = "comprehensive:test:both";
            String bothValue = "Both Cache Value";

            // 存储数据
            CacheUtil.setL2Cache(redisService, bothKey, bothValue, caffeineCache, 300L, TimeUnit.SECONDS);
            log.info("已存储数据到两级缓存: {} = {}", bothKey, bothValue);

            // 同时删除两级缓存
            CacheUtil.delL2Cache(bothKey, caffeineCache, redisService);
            String bothResultAfterDelete = CacheUtil.getL2Cache(redisService, bothKey, String.class, caffeineCache);
            resultMap.put("bothCacheAfterDelete", bothResultAfterDelete);
            log.info("同时删除两级缓存后获取结果: {}", bothResultAfterDelete);

            // 6. 测试缓存穿透防护
            log.info("--- 测试缓存穿透防护 ---");
            String penetrationKey = "comprehensive:test:penetration";

            // 确保这个键不在布隆过滤器中
            boolean inBloom = bloomFilterService.mightContain(penetrationKey);
            if (!inBloom) {
                // 尝试获取不存在的数据
                String penetrationResult = CacheUtil.getL2Cache(redisService, bloomFilterService, penetrationKey, new TypeReference<String>() {
                }, caffeineCache);
                resultMap.put("penetrationTestResult", penetrationResult);
                log.info("缓存穿透防护测试结果: {}", penetrationResult);
                log.info("由于布隆过滤器判断键不存在，避免了Redis查询");
            }

            // 7. 测试性能对比
            log.info("--- 测试性能对比 ---");
            int testIterations = 10000;

            // 测试不使用布隆过滤器的性能
            long start1 = System.currentTimeMillis();
            for (int i = 0; i < testIterations; i++) {
                CacheUtil.getL2Cache(redisService, "nonexistent:key:" + i, new TypeReference<String>() {
                }, caffeineCache);
            }
            long end1 = System.currentTimeMillis();

            // 测试使用布隆过滤器的性能
            long start2 = System.currentTimeMillis();
            for (int i = 0; i < testIterations; i++) {
                CacheUtil.getL2Cache(redisService, bloomFilterService, "nonexistent:key:" + i, new TypeReference<String>() {
                }, caffeineCache);
            }
            long end2 = System.currentTimeMillis();

            long timeWithoutBloom = end1 - start1;
            long timeWithBloom = end2 - start2;
            resultMap.put("performanceWithoutBloom", timeWithoutBloom + "ms");
            resultMap.put("performanceWithBloom", timeWithBloom + "ms");
            resultMap.put("performanceImprovement", (timeWithoutBloom - timeWithBloom) + "ms");

            log.info("不使用布隆过滤器查询 {} 次耗时: {} ms", testIterations, timeWithoutBloom);
            log.info("使用布隆过滤器查询 {} 次耗时: {} ms", testIterations, timeWithBloom);
            log.info("性能提升: {} ms", timeWithoutBloom - timeWithBloom);

            // 清理测试数据
            redisService.deleteObject(localKey);
            redisService.deleteObject(redisKey);
            redisService.deleteObject(objectKey);
            redisService.deleteObject(bloomKey);
            redisService.deleteObject(bothKey);
            caffeineCache.invalidateAll();

            log.info("=== CacheUtil全面功能测试完成 ===");
            resultMap.put("status", "测试完成");
            return Result.success(resultMap);

        } catch (Exception e) {
            log.error("全面测试CacheUtil功能过程中发生异常", e);
            resultMap.put("error", e.getMessage());
            return Result.fail("测试失败: " + e.getMessage());
        }
    }
}
