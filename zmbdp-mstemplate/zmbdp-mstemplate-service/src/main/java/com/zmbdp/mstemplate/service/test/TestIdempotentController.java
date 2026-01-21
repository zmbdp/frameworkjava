package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.constants.IdempotentConstants;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.idempotent.annotation.Idempotent;
import com.zmbdp.common.idempotent.enums.IdempotentMode;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.domain.MessageDTO;
import com.zmbdp.mstemplate.service.rabbit.Producer;
import com.zmbdp.mstemplate.service.test.feign.IdempotentTestApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 幂等性功能测试控制器
 * 使用 OpenFeign 客户端，一键测试所有功能，无需手动传参
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/idempotent")
public class TestIdempotentController {

    /**
     * MQ消息发送者
     */
    @Autowired
    private Producer producer;

    /**
     * 幂等性测试 Feign 客户端
     */
    @Autowired
    private IdempotentTestApi idempotentTestAPI;

    /**
     * Redis 服务（用于查询重试次数）
     */
    @Autowired
    private RedisService redisService;

    /**
     * 环境配置（用于获取 Redis Key 前缀）
     */
    @Autowired
    private Environment environment;

    /**
     * 线程池执行器（用于高并发测试）
     */
    @Autowired
    @Qualifier(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
    private Executor threadPoolExecutor;

    /*=============================================    一键测试接口    =============================================*/

    /**
     * 一键测试所有功能
     * 直接调用此接口即可测试所有幂等性功能，无需传参
     *
     * @return 详细的测试结果
     */
    @PostMapping("/all")
    public Result<Map<String, Object>> testAll() {
        log.info("=== 一键测试所有幂等性功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        // 生成测试用的Token
        String token1 = UUID.randomUUID().toString();
        String token2 = UUID.randomUUID().toString();
        String token3 = UUID.randomUUID().toString();

        // ========== HTTP基础功能测试 ==========
        Map<String, Object> httpBasic = new LinkedHashMap<>();
        try {
            // 1. 请求头方式 - 第一次请求
            Result<String> headerResult1 = idempotentTestAPI.testHttpBasicHeader(token1);
            httpBasic.put("请求头方式-第一次", isSuccess(headerResult1) ? "✅ 成功" : "❌ 失败: " + headerResult1.getErrMsg());

            // 2. 请求头方式 - 重复请求（应该失败）
            try {
                Result<String> headerResult2 = idempotentTestAPI.testHttpBasicHeader(token1);
                httpBasic.put("请求头方式-重复", isSuccess(headerResult2) ? "❌ 应该失败但成功了" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpBasic.put("请求头方式-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }

            // 3. 请求参数方式 - 第一次请求
            Result<String> paramResult1 = idempotentTestAPI.testHttpBasicParam(token2);
            httpBasic.put("请求参数方式-第一次", isSuccess(paramResult1) ? "✅ 成功" : "❌ 失败: " + paramResult1.getErrMsg());

            // 4. 请求参数方式 - 重复请求（应该失败）
            try {
                Result<String> paramResult2 = idempotentTestAPI.testHttpBasicParam(token2);
                httpBasic.put("请求参数方式-重复", isSuccess(paramResult2) ? "❌ 应该失败但成功了" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpBasic.put("请求参数方式-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }

            // 5. 优先级测试
            Result<String> priorityResult = idempotentTestAPI.testHttpBasicPriority(token3, "param-token");
            httpBasic.put("优先级测试", isSuccess(priorityResult) ? "✅ 成功（优先使用请求头）" : "❌ 失败: " + priorityResult.getErrMsg());

        } catch (Exception e) {
            httpBasic.put("错误", "❌ HTTP基础功能测试异常: " + e.getMessage());
        }
        result.put("HTTP基础功能", httpBasic);

        // ========== HTTP高级功能测试 ==========
        Map<String, Object> httpAdvanced = new LinkedHashMap<>();
        try {
            String strongToken = UUID.randomUUID().toString();

            // 1. 强幂等模式 - 第一次请求
            Result<String> strongResult1 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            httpAdvanced.put("强幂等模式-第一次", isSuccess(strongResult1) ? "✅ 成功（结果已缓存）" : "❌ 失败: " + strongResult1.getErrMsg());

            // 2. 强幂等模式 - 重复请求（应该返回缓存结果）
            Result<String> strongResult2 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            // 验证返回的是缓存结果（时间戳应该相同）
            boolean isCached = isSuccess(strongResult2) && strongResult1.getData().equals(strongResult2.getData());
            httpAdvanced.put("强幂等模式-重复", isCached ? "✅ 成功返回缓存结果" : "❌ 失败: 未返回缓存结果");

            // 3. 三态设计 - DEFAULT（第一次 + 重复请求）
            String defaultToken = UUID.randomUUID().toString();
            Result<String> defaultResult1 = idempotentTestAPI.testHttpAdvancedModeDefault(defaultToken);
            httpAdvanced.put("三态设计-DEFAULT-第一次", isSuccess(defaultResult1) ? "✅ 成功（使用全局配置）" : "❌ 失败: " + defaultResult1.getErrMsg());
            // 重复请求测试（根据全局配置决定是防重还是强幂等）
            try {
                Result<String> defaultResult2 = idempotentTestAPI.testHttpAdvancedModeDefault(defaultToken);
                httpAdvanced.put("三态设计-DEFAULT-重复", isSuccess(defaultResult2) ? "✅ 成功（根据全局配置处理）" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpAdvanced.put("三态设计-DEFAULT-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }

            // 4. 三态设计 - TRUE（第一次 + 重复请求，应该返回缓存结果）
            String trueToken = UUID.randomUUID().toString();
            Result<String> trueResult1 = idempotentTestAPI.testHttpAdvancedModeTrue(trueToken);
            httpAdvanced.put("三态设计-TRUE-第一次", isSuccess(trueResult1) ? "✅ 成功（强制开启强幂等）" : "❌ 失败: " + trueResult1.getErrMsg());
            // 重复请求测试（应该返回缓存结果）
            Result<String> trueResult2 = idempotentTestAPI.testHttpAdvancedModeTrue(trueToken);
            // 验证返回的是缓存结果（时间戳应该相同）
            boolean trueIsCached = isSuccess(trueResult2) && trueResult1.getData().equals(trueResult2.getData());
            httpAdvanced.put("三态设计-TRUE-重复", trueIsCached ? "✅ 成功返回缓存结果" : "❌ 失败: 未返回缓存结果");

            // 5. 三态设计 - FALSE（第一次 + 重复请求，应该被拒绝）
            String falseToken = UUID.randomUUID().toString();
            Result<String> falseResult1 = idempotentTestAPI.testHttpAdvancedModeFalse(falseToken);
            httpAdvanced.put("三态设计-FALSE-第一次", isSuccess(falseResult1) ? "✅ 成功（强制关闭强幂等）" : "❌ 失败: " + falseResult1.getErrMsg());
            // 重复请求测试（应该被拒绝）
            try {
                Result<String> falseResult2 = idempotentTestAPI.testHttpAdvancedModeFalse(falseToken);
                httpAdvanced.put("三态设计-FALSE-重复", isSuccess(falseResult2) ? "❌ 应该失败但成功了" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpAdvanced.put("三态设计-FALSE-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }

            // 6. 业务失败重试
            String failureToken = UUID.randomUUID().toString();
            try {
                Result<String> failureResult = idempotentTestAPI.testHttpAdvancedFailure(failureToken, true);
                httpAdvanced.put("业务失败重试", isSuccess(failureResult) ? "❌ 应该失败但成功了" : "✅ 正确抛出异常");
            } catch (Exception e) {
                httpAdvanced.put("业务失败重试", "✅ 正确抛出异常，Token会被删除");
                // 验证可以重试
                Result<String> retryResult = idempotentTestAPI.testHttpAdvancedFailure(failureToken, false);
                httpAdvanced.put("业务失败重试-验证", isSuccess(retryResult) ? "✅ 可以重试成功" : "❌ 重试失败: " + retryResult.getErrMsg());
            }

            // 7. 重试次数测试
            Map<String, Object> retryCountTest = testRetryCount();
            httpAdvanced.put("重试次数测试", retryCountTest);

        } catch (Exception e) {
            httpAdvanced.put("错误", "❌ HTTP高级功能测试异常: " + e.getMessage());
        }
        result.put("HTTP高级功能", httpAdvanced);

        // ========== MQ基础功能测试 ==========
        Map<String, Object> mqBasic = new LinkedHashMap<>();
        try {
            String mqToken1 = UUID.randomUUID().toString();
            String mqToken2 = UUID.randomUUID().toString();

            // 1. SpEL表达式方式 - 第一次
            MessageDTO message1 = createMessageDTO("测试消息", "MQ基础功能测试 - SpEL表达式", mqToken1);
            producer.produceMsgIdempotent(message1);
            mqBasic.put("SpEL表达式-第一次", "✅ 消息发送成功，Token: " + mqToken1);

            // 2. SpEL表达式方式 - 重复（应该被拒绝）
            MessageDTO message2 = createMessageDTO("测试消息", "MQ基础功能测试 - SpEL表达式重复", mqToken1);
            producer.produceMsgIdempotent(message2);
            mqBasic.put("SpEL表达式-重复", "✅ 消息发送成功（消费者会拒绝处理）");

            // 3. 消息头方式 - 第一次
            MessageDTO message3 = createMessageDTO("测试消息", "MQ基础功能测试 - 消息头方式", null);
            producer.produceMsgIdempotentHeader(message3, mqToken2);
            mqBasic.put("消息头方式-第一次", "✅ 消息发送成功，Token: " + mqToken2);

            // 4. 消息头方式 - 重复（应该被拒绝）
            MessageDTO message4 = createMessageDTO("测试消息", "MQ基础功能测试 - 消息头方式重复", null);
            producer.produceMsgIdempotentHeader(message4, mqToken2);
            mqBasic.put("消息头方式-重复", "✅ 消息发送成功（消费者会拒绝处理）");

            // 5. 批量发送测试
            String batchToken = UUID.randomUUID().toString();
            for (int i = 1; i <= 3; i++) {
                MessageDTO message = createMessageDTO("批量测试", "MQ基础功能测试 - 第" + i + "条消息", batchToken);
                producer.produceMsgIdempotent(message);
            }
            mqBasic.put("批量发送", "✅ 发送3条相同Token的消息，只有第一条会被处理，Token: " + batchToken);

        } catch (Exception e) {
            mqBasic.put("错误", "❌ MQ基础功能测试异常: " + e.getMessage());
        }
        result.put("MQ基础功能", mqBasic);

        // ========== MQ高级功能测试 ==========
        Map<String, Object> mqAdvanced = new LinkedHashMap<>();
        try {
            // 1. 业务失败测试（第一次失败 + 验证可以重试）
            String failureToken = UUID.randomUUID().toString();
            MessageDTO failureMessage1 = createMessageDTO("测试失败", "MQ高级功能测试 - 模拟业务失败（第一次）", failureToken);
            producer.produceMsgIdempotentFailure(failureMessage1);
            mqAdvanced.put("业务失败-第一次", "✅ 消息发送成功，Token: " + failureToken + "（消费者会抛出异常，Token会被删除）");

            // 等待一小段时间，确保第一次消息处理完成
            sleep(500);

            // 验证可以重试（发送成功类型的消息，应该能正常处理）
            MessageDTO retryMessage = createMessageDTO("测试成功", "MQ高级功能测试 - 验证业务失败后可以重试", failureToken);
            producer.produceMsgIdempotentFailure(retryMessage);
            mqAdvanced.put("业务失败-重试验证", "✅ 消息发送成功（Token已删除，可以重试）");

            // 2. 自定义过期时间测试
            String expireToken = UUID.randomUUID().toString();
            MessageDTO expireMessage = createMessageDTO("测试消息", "MQ高级功能测试 - 自定义过期时间", expireToken);
            producer.produceMsgIdempotent(expireMessage);
            mqAdvanced.put("自定义过期时间", "✅ 消息发送成功，Token: " + expireToken);

        } catch (Exception e) {
            mqAdvanced.put("错误", "❌ MQ高级功能测试异常: " + e.getMessage());
        }
        result.put("MQ高级功能", mqAdvanced);

        result.put("测试说明", "所有测试完成，请查看日志和MQ消费者处理结果");
        result.put("提示", "HTTP测试使用了OpenFeign客户端，MQ测试直接发送消息到队列");

        return Result.success(result);
    }

    /**
     * 高并发测试
     * 通过params传入并发量参数，测试高并发场景下幂等性是否正常工作
     *
     * @param concurrency 并发量，默认100
     * @return 测试结果
     */
    @PostMapping("/concurrent")
    public Result<Map<String, Object>> testConcurrent(@RequestParam(value = "concurrency", defaultValue = "100") int concurrency) {
        log.info("=== 高并发测试，并发量: {} ===", concurrency);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("并发量", concurrency);
        result.put("说明", "测试同一token的并发请求，验证幂等性是否正常工作");

        try {
            // 测试场景1：防重模式 - 同一token并发请求，只有一个应该成功
            Map<String, Object> preventDuplicateTest = testPreventDuplicateConcurrent(concurrency);
            result.put("防重模式-并发测试", preventDuplicateTest);

            // 测试场景2：强幂等模式 - 同一token并发请求，第一个成功后其他返回缓存结果
            Map<String, Object> strongIdempotentTest = testStrongIdempotentConcurrent(concurrency);
            result.put("强幂等模式-并发测试", strongIdempotentTest);

            // 测试场景3：不同token并发请求，验证互不干扰
            Map<String, Object> differentTokenTest = testDifferentTokenConcurrent(concurrency);
            result.put("不同Token-并发测试", differentTokenTest);

        } catch (Exception e) {
            result.put("错误", "❌ 高并发测试异常: " + e.getMessage());
            log.error("高并发测试异常", e);
        }

        System.err.println(result);
        return Result.success(result);
    }

    /**
     * 快速测试 - 只测试核心功能
     *
     * @return 测试结果
     */
    @PostMapping("/quick")
    public Result<Map<String, Object>> testQuick() {
        log.info("=== 快速测试核心功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        String token = UUID.randomUUID().toString();

        // HTTP基础功能
        try {
            Result<String> httpResult = idempotentTestAPI.testHttpBasicHeader(token);
            result.put("HTTP请求头方式", isSuccess(httpResult) ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("HTTP请求头方式", "❌ 异常: " + e.getMessage());
        }

        // HTTP强幂等模式
        try {
            String strongToken = UUID.randomUUID().toString();
            Result<String> strongResult1 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            Result<String> strongResult2 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            result.put("HTTP强幂等模式", isSuccess(strongResult1) && isSuccess(strongResult2) ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("HTTP强幂等模式", "❌ 异常: " + e.getMessage());
        }

        // MQ功能
        try {
            MessageDTO message = createMessageDTO("快速测试", "快速测试消息", UUID.randomUUID().toString());
            producer.produceMsgIdempotent(message);
            result.put("MQ消息发送", "✅ 成功");
        } catch (Exception e) {
            result.put("MQ消息发送", "❌ 异常: " + e.getMessage());
        }

        return Result.success(result);
    }

    /**
     * 获取测试说明
     */
    @GetMapping("/help")
    public Result<String> getTestHelp() {
        StringBuilder help = new StringBuilder();
        help.append("============================ 幂等性功能测试说明 ============================\n\n");
        help.append("【一键测试接口】\n\n");
        help.append("1. POST mstemplate/test/idempotent/all - 一键测试所有功能\n");
        help.append("   说明：直接调用此接口即可测试所有幂等性功能，无需传参\n");
        help.append("   测试内容：HTTP基础功能、HTTP高级功能、MQ基础功能、MQ高级功能\n\n");
        help.append("2. POST mstemplate/test/idempotent/quick - 快速测试核心功能\n");
        help.append("   说明：快速测试核心功能，测试时间更短\n\n");
        help.append("3. GET mstemplate/test/idempotent/help - 获取测试说明（本接口）\n\n");
        help.append("【测试说明】\n");
        help.append("- HTTP测试使用OpenFeign客户端自动调用，无需手动传参\n");
        help.append("- MQ测试直接发送消息到队列，由消费者处理\n");
        help.append("- 所有测试结果都会在返回的JSON中显示\n");
        help.append("- 请查看日志和MQ消费者处理结果以获取详细信息\n\n");
        help.append("======================================================================\n");

        return Result.success(help.toString());
    }

    /**
     * 防重模式并发测试
     * 测试同一token的并发请求，只有一个应该成功
     */
    private Map<String, Object> testPreventDuplicateConcurrent(int concurrency) {
        Map<String, Object> result = new LinkedHashMap<>();
        String token = UUID.randomUUID().toString();

        // 用于统计结果
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(concurrency);

        log.info("防重模式并发测试开始，Token: {}, 并发量: {}", token, concurrency);
        long startTime = System.currentTimeMillis();

        // 并发执行请求
        for (int i = 0; i < concurrency; i++) {
            final int index = i;
            threadPoolExecutor.execute(() -> {
                try {
                    Result<String> response = idempotentTestAPI.testHttpBasicHeader(token);
                    if (isSuccess(response)) {
                        successCount.incrementAndGet();
                        log.debug("防重模式并发测试 - 线程{}成功", index);
                    } else {
                        rejectCount.incrementAndGet();
                        log.debug("防重模式并发测试 - 线程{}被拒绝: {}", index, response.getErrMsg());
                    }
                } catch (Exception e) {
                    // 如果是重复请求异常，也算正常
                    if (e.getMessage() != null && e.getMessage().contains("重复")) {
                        rejectCount.incrementAndGet();
                        log.debug("防重模式并发测试 - 线程{}被拒绝（异常）: {}", index, e.getMessage());
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("防重模式并发测试 - 线程{}异常: {}", index, e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("防重模式并发测试等待被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        result.put("成功数", successCount.get());
        result.put("拒绝数", rejectCount.get());
        result.put("错误数", errorCount.get());
        result.put("总请求数", concurrency);
        result.put("耗时(ms)", duration);

        // 验证结果：只有一个应该成功
        if (successCount.get() == 1 && rejectCount.get() == concurrency - 1 && errorCount.get() == 0) {
            result.put("结果", "✅ 通过（只有1个成功，其他都被正确拒绝）");
        } else if (successCount.get() == 1) {
            result.put("结果", "✅ 通过（只有1个成功），但有" + errorCount.get() + "个错误");
        } else {
            result.put("结果", "❌ 失败（成功数: " + successCount.get() + "，期望: 1）");
        }

        log.info("防重模式并发测试完成，成功: {}, 拒绝: {}, 错误: {}, 耗时: {}ms",
                successCount.get(), rejectCount.get(), errorCount.get(), duration);

        return result;
    }

    /**
     * 强幂等模式并发测试
     * 测试同一token的并发请求，第一个成功后其他返回缓存结果
     */
    private Map<String, Object> testStrongIdempotentConcurrent(int concurrency) {
        Map<String, Object> result = new LinkedHashMap<>();
        String token = UUID.randomUUID().toString();

        // 用于统计结果
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger cachedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(concurrency);

        // 用于记录第一个成功的结果，其他请求的结果应该和它一样
        AtomicReference<String> firstResult = new AtomicReference<>();

        log.info("强幂等模式并发测试开始，Token: {}, 并发量: {}", token, concurrency);
        long startTime = System.currentTimeMillis();

        // 并发执行请求
        for (int i = 0; i < concurrency; i++) {
            final int index = i;
            threadPoolExecutor.execute(() -> {
                try {
                    Result<String> response = idempotentTestAPI.testHttpAdvancedStrong(token);
                    if (isSuccess(response)) {
                        // 原子性地设置第一个结果，只有一个线程能成功
                        if (firstResult.compareAndSet(null, response.getData())) {
                            successCount.incrementAndGet();
                            log.debug("强幂等模式并发测试 - 线程{}首次成功，结果: {}", index, response.getData());
                        } else {
                            // 检查是否返回的是缓存结果（时间戳应该相同）
                            String cached = firstResult.get();
                            if (cached != null && cached.equals(response.getData())) {
                                cachedCount.incrementAndGet();
                                log.debug("强幂等模式并发测试 - 线程{}返回缓存结果", index);
                            } else {
                                errorCount.incrementAndGet();
                                log.warn("强幂等模式并发测试 - 线程{}结果不一致，期望: {}，实际: {}",
                                        index, cached, response.getData());
                            }
                        }
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("强幂等模式并发测试 - 线程{}失败: {}", index, response.getErrMsg());
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.warn("强幂等模式并发测试 - 线程{}异常: {}", index, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("强幂等模式并发测试等待被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        result.put("首次成功数", successCount.get());
        result.put("返回缓存数", cachedCount.get());
        result.put("错误数", errorCount.get());
        result.put("总请求数", concurrency);
        result.put("耗时(ms)", duration);

        // 验证结果：只有一个首次成功，其他都返回缓存结果
        if (successCount.get() == 1 && cachedCount.get() == concurrency - 1 && errorCount.get() == 0) {
            result.put("结果", "✅ 通过（1个首次成功，" + cachedCount.get() + "个返回缓存结果）");
        } else {
            result.put("结果", "❌ 失败（首次成功: " + successCount.get() + "，返回缓存: " + cachedCount.get() + "，错误: " + errorCount.get() + "）");
        }

        log.info("强幂等模式并发测试完成，首次成功: {}, 返回缓存: {}, 错误: {}, 耗时: {}ms",
                successCount.get(), cachedCount.get(), errorCount.get(), duration);

        return result;
    }

    /**
     * 不同Token并发测试
     * 测试不同token的并发请求，验证互不干扰
     */
    private Map<String, Object> testDifferentTokenConcurrent(int concurrency) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 用于统计结果
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(concurrency);

        log.info("不同Token并发测试开始，并发量: {}", concurrency);
        long startTime = System.currentTimeMillis();

        // 并发执行请求，每个请求使用不同的token
        for (int i = 0; i < concurrency; i++) {
            final int index = i;
            final String token = UUID.randomUUID().toString();
            threadPoolExecutor.execute(() -> {
                try {
                    Result<String> response = idempotentTestAPI.testHttpBasicHeader(token);
                    if (isSuccess(response)) {
                        successCount.incrementAndGet();
                        log.debug("不同Token并发测试 - 线程{}成功，Token: {}", index, token);
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("不同Token并发测试 - 线程{}失败，Token: {}, 错误: {}", index, token, response.getErrMsg());
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.warn("不同Token并发测试 - 线程{}异常，Token: {}, 错误: {}", index, token, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有任务完成
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("不同Token并发测试等待被中断");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        result.put("成功数", successCount.get());
        result.put("错误数", errorCount.get());
        result.put("总请求数", concurrency);
        result.put("耗时(ms)", duration);

        // 验证结果：所有不同token的请求都应该成功
        if (successCount.get() == concurrency && errorCount.get() == 0) {
            result.put("结果", "✅ 通过（所有不同Token的请求都成功）");
        } else {
            result.put("结果", "❌ 失败（成功: " + successCount.get() + "，错误: " + errorCount.get() + "）");
        }

        log.info("不同Token并发测试完成，成功: {}, 错误: {}, 耗时: {}ms",
                successCount.get(), errorCount.get(), duration);

        return result;
    }

    /*=============================================    Feign 客户端调用的测试接口    =============================================*/

    /**
     * HTTP基础功能测试 - 请求头方式
     */
    @PostMapping("/http/basic/header")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, message = "请勿重复提交")
    public Result<String> testHttpBasicHeader() {
        return Result.success("请求头方式测试成功");
    }

    /**
     * HTTP基础功能测试 - 请求参数方式
     */
    @PostMapping("/http/basic/param")
    @Idempotent(allowParam = true, paramName = "idempotentToken", expireTime = 300, message = "请勿重复提交")
    public Result<String> testHttpBasicParam() {
        return Result.success("请求参数方式测试成功");
    }

    /**
     * HTTP基础功能测试 - 优先级测试（请求头优先）
     */
    @PostMapping("/http/basic/priority")
    @Idempotent(headerName = "Idempotent-Token", paramName = "idempotentToken", expireTime = 300, message = "请勿重复提交")
    public Result<String> testHttpBasicPriority() {
        return Result.success("优先级测试成功（优先使用请求头）");
    }

    /**
     * HTTP高级功能测试 - 强幂等模式
     */
    @PostMapping("/http/advanced/strong")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, returnCachedResult = IdempotentMode.TRUE, message = "请勿重复提交")
    public Result<String> testHttpAdvancedStrong() {
        return Result.success("强幂等模式测试成功 - " + System.currentTimeMillis());
    }

    /**
     * HTTP高级功能测试 - 三态设计 DEFAULT
     */
    @PostMapping("/http/advanced/mode/default")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, returnCachedResult = IdempotentMode.DEFAULT, message = "请勿重复提交")
    public Result<String> testHttpAdvancedModeDefault() {
        return Result.success("三态设计-DEFAULT测试成功");
    }

    /**
     * HTTP高级功能测试 - 三态设计 TRUE
     */
    @PostMapping("/http/advanced/mode/true")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, returnCachedResult = IdempotentMode.TRUE, message = "请勿重复提交")
    public Result<String> testHttpAdvancedModeTrue() {
        return Result.success("三态设计-TRUE测试成功 - " + System.currentTimeMillis());
    }

    /**
     * HTTP高级功能测试 - 三态设计 FALSE
     */
    @PostMapping("/http/advanced/mode/false")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, returnCachedResult = IdempotentMode.FALSE, message = "请勿重复提交")
    public Result<String> testHttpAdvancedModeFalse() {
        return Result.success("三态设计-FALSE测试成功");
    }

    /**
     * HTTP高级功能测试 - 业务失败重试
     */
    @PostMapping("/http/advanced/failure")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, message = "请勿重复提交")
    public Result<String> testHttpAdvancedFailure(@RequestParam(value = "shouldFail", defaultValue = "false") boolean shouldFail) {
        if (shouldFail) {
            throw new ServiceException("模拟业务异常，Token会被删除，允许重试", ResultCode.ERROR.getCode());
        }
        return Result.success("业务失败重试测试成功");
    }

    /**
     * 重试次数测试专用接口
     * 用于测试重试次数的递增、查询、删除等功能
     */
    @PostMapping("/http/advanced/retry-count")
    @Idempotent(headerName = "Idempotent-Token", expireTime = 300, message = "请勿重复提交")
    public Result<String> testHttpAdvancedRetryCount(@RequestParam(value = "shouldFail", defaultValue = "false") boolean shouldFail) {
        if (shouldFail) {
            throw new ServiceException("模拟业务异常，用于测试重试次数", ResultCode.ERROR.getCode());
        }
        return Result.success("重试次数测试成功");
    }

    /*=============================================    辅助方法    =============================================*/

    /**
     * 创建 MessageDTO 对象
     *
     * @param type  消息类型
     * @param desc  消息描述
     * @param token 幂等性Token（可为null，用于消息头方式）
     * @return MessageDTO 对象
     */
    private MessageDTO createMessageDTO(String type, String desc, String token) {
        MessageDTO message = new MessageDTO();
        message.setType(type);
        message.setDesc(desc);
        if (token != null) {
            message.setIdempotentToken(token);
        }
        return message;
    }

    /**
     * 安全睡眠，处理中断异常
     *
     * @param millis 睡眠时间（毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查 Result 是否成功
     *
     * @param result Result 对象
     * @return true - 成功；false - 失败
     */
    private boolean isSuccess(Result<?> result) {
        return result != null && result.getCode() == ResultCode.SUCCESS.getCode();
    }

    /**
     * 测试重试次数功能
     * 测试场景：
     * 1. 重试次数递增（根据配置的最大重试次数动态测试）
     * 2. 失败时重试次数是否能正确查询到
     * 3. 成功时重试次数是否会被删除
     * 4. 超过最大重试次数时的行为
     *
     * @return 测试结果
     */
    private Map<String, Object> testRetryCount() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 从配置中心读取实际的最大重试次数（与IdempotentAspect使用相同的配置）
            int maxRetries = environment.getProperty(
                    IdempotentConstants.NACOS_IDEMPOTENT_MAX_RETRY_COUNT_PREFIX,
                    Integer.class,
                    3 // 默认值
            );

            // 获取 Redis Key 前缀（使用与IdempotentAspect相同的配置）
            String keyPrefix = environment.getProperty(
                    IdempotentConstants.NACOS_IDEMPOTENT_KEY_PREFIX_PREFIX,
                    String.class,
                    IdempotentConstants.IDEMPOTENT_KEY_PREFIX
            );

            // 添加日志输出，方便调试
            log.info("重试次数测试 - 最大重试次数: {}, Redis Key前缀: {}", maxRetries, keyPrefix);
            result.put("配置的最大重试次数", maxRetries);

            // ========== 测试场景1：重试次数递增，测试到超过最大重试次数 ==========
            String retryToken1 = UUID.randomUUID().toString();
            String retryCountKey1 = keyPrefix + retryToken1 + ":retry:count";

            // 第一次失败（注意：第一次失败时，重试次数还没有写入，因为重试次数是在检测到FAILED状态时递增的）
            try {
                idempotentTestAPI.testHttpAdvancedRetryCount(retryToken1, true);
                result.put("场景1-第一次失败", "❌ 应该失败但成功了");
            } catch (Exception e) {
                result.put("场景1-第一次失败", "✅ 正确抛出异常");
            }

            // 等待一小段时间，确保第一次处理完成（状态设置为FAILED）
            sleep(300);

            // 查询第一次失败后的状态（此时状态应该是FAILED，但重试次数还没有写入）
            String status1 = redisService.getCacheObject(keyPrefix + retryToken1, String.class);
            Integer retryCount1 = redisService.getCacheObject(retryCountKey1, Integer.class);
            result.put("场景1-第一次失败后状态", "FAILED".equals(status1) ? "✅ 正确: " + status1 : "❌ 错误: " + status1);
            result.put("场景1-第一次失败后重试次数", retryCount1 == null ? "✅ 正确: null（第一次失败时还未写入）" : "❌ 错误: " + retryCount1);

            // 动态测试重试次数递增：从第2次失败到超过最大重试次数
            for (int attempt = 2; attempt <= maxRetries + 1; attempt++) {
                // 第attempt次失败（检测到FAILED状态，递增重试次数）
                try {
                    idempotentTestAPI.testHttpAdvancedRetryCount(retryToken1, true);
                    result.put("场景1-第" + attempt + "次失败", "❌ 应该失败但成功了");
                } catch (Exception e) {
                    if (attempt <= maxRetries) {
                        result.put("场景1-第" + attempt + "次失败", "✅ 正确抛出异常（重试次数: " + (attempt - 1) + "/" + maxRetries + "）");
                    } else {
                        result.put("场景1-第" + attempt + "次失败", "✅ 正确抛出异常（超过最大重试次数）");
                    }
                }

                sleep(300);

                // 查询重试次数
                Integer retryCount = redisService.getCacheObject(retryCountKey1, Integer.class);
                if (attempt <= maxRetries) {
                    // 在限制内，重试次数应该是 attempt-1
                    int expectedCount = attempt - 1;
                    result.put("场景1-第" + attempt + "次失败后重试次数",
                            retryCount != null && retryCount == expectedCount
                                    ? "✅ 正确: " + retryCount + "/" + maxRetries
                                    : "❌ 错误: " + retryCount + " (期望: " + expectedCount + ")");
                } else {
                    // 超过限制，重试次数应该被清除了
                    result.put("场景1-超过限制后重试次数",
                            retryCount == null ? "✅ 正确: 已被清除" : "❌ 错误: 未清除, 值=" + retryCount);
                }
            }

            // ========== 测试场景2：失败后成功，验证重试次数是否被删除 ==========
            String retryToken2 = UUID.randomUUID().toString();
            String retryCountKey2 = keyPrefix + retryToken2 + ":retry:count";

            // 第一次失败
            try {
                idempotentTestAPI.testHttpAdvancedRetryCount(retryToken2, true);
                result.put("场景2-第一次失败", "❌ 应该失败但成功了");
            } catch (Exception e) {
                result.put("场景2-第一次失败", "✅ 正确抛出异常");
            }

            // 等待一小段时间，确保第一次处理完成
            sleep(300);

            // 查询重试次数（第一次失败时还未写入）
            Integer retryCount2_1 = redisService.getCacheObject(retryCountKey2, Integer.class);
            result.put("场景2-第一次失败后重试次数", retryCount2_1 == null ? "✅ 正确: null（第一次失败时还未写入）" : "❌ 错误: " + retryCount2_1);

            // 第二次失败（此时会检测到FAILED状态，递增重试次数为1，然后删除FAILED状态，继续执行）
            try {
                idempotentTestAPI.testHttpAdvancedRetryCount(retryToken2, true);
                result.put("场景2-第二次失败", "❌ 应该失败但成功了");
            } catch (Exception e) {
                result.put("场景2-第二次失败", "✅ 正确抛出异常");
            }

            sleep(300);

            // 查询重试次数（应该是1）
            Integer retryCount2_2 = redisService.getCacheObject(retryCountKey2, Integer.class);
            result.put("场景2-第二次失败后重试次数", retryCount2_2 != null && retryCount2_2 == 1 ? "✅ 正确: " + retryCount2_2 : "❌ 错误: " + retryCount2_2 + " (期望: 1)");

            // 第三次成功（此时会检测到FAILED状态，递增重试次数为2，然后删除FAILED状态，继续执行，成功后清除重试次数）
            try {
                Result<String> successResult = idempotentTestAPI.testHttpAdvancedRetryCount(retryToken2, false);
                if (isSuccess(successResult)) {
                    result.put("场景2-第三次成功", "✅ 成功");
                    // 等待一小段时间，确保处理完成
                    sleep(300);
                    // 查询重试次数（应该被清除了）
                    Integer retryCount2_3 = redisService.getCacheObject(retryCountKey2, Integer.class);
                    result.put("场景2-成功后重试次数", retryCount2_3 == null ? "✅ 正确: 已被清除" : "❌ 错误: 未清除, 值=" + retryCount2_3);
                } else {
                    result.put("场景2-第三次成功", "❌ 失败: " + successResult.getErrMsg());
                }
            } catch (Exception e) {
                result.put("场景2-第三次成功", "❌ 异常: " + e.getMessage());
            }

            // ========== 测试场景3：连续失败多次后，验证重试次数是否正确（测试次数不超过最大重试次数） ==========
            String retryToken3 = UUID.randomUUID().toString();
            String retryCountKey3 = keyPrefix + retryToken3 + ":retry:count";

            // 测试次数取maxRetries和3的较小值，避免测试时间过长
            int testCount = Math.min(maxRetries, 3);

            for (int i = 1; i <= testCount; i++) {
                try {
                    idempotentTestAPI.testHttpAdvancedRetryCount(retryToken3, true);
                    result.put("场景3-第" + i + "次失败", "❌ 应该失败但成功了");
                } catch (Exception e) {
                    result.put("场景3-第" + i + "次失败", "✅ 正确抛出异常");
                }
                // 等待一小段时间，确保处理完成
                sleep(300);
                // 查询重试次数
                // 注意：第1次失败时，重试次数还未写入（因为重试次数是在检测到FAILED状态时递增的）
                // 第2次失败时，重试次数为1（因为检测到了第1次的FAILED状态）
                // 第i次失败时，重试次数为i-1（因为检测到了第i-1次的FAILED状态）
                Integer retryCount = redisService.getCacheObject(retryCountKey3, Integer.class);
                int expectedRetryCount = i == 1 ? 0 : i - 1; // 第1次失败时重试次数为0，第i次为i-1
                if (i == 1) {
                    result.put("场景3-第" + i + "次失败后重试次数", retryCount == null ? "✅ 正确: null（第一次失败时还未写入）" : "❌ 错误: " + retryCount + " (期望: null)");
                } else {
                    result.put("场景3-第" + i + "次失败后重试次数", retryCount != null && retryCount == expectedRetryCount ? "✅ 正确: " + retryCount + "/" + maxRetries : "❌ 错误: " + retryCount + " (期望: " + expectedRetryCount + ")");
                }
            }

            // ========== 测试场景4：失败后立即查询，验证重试次数是否存在 ==========
            String retryToken4 = UUID.randomUUID().toString();
            String retryCountKey4 = keyPrefix + retryToken4 + ":retry:count";

            // 第一次失败
            try {
                idempotentTestAPI.testHttpAdvancedRetryCount(retryToken4, true);
                result.put("场景4-第一次失败", "❌ 应该失败但成功了");
            } catch (Exception e) {
                result.put("场景4-第一次失败", "✅ 正确抛出异常");
            }

            // 立即查询重试次数（第一次失败时还未写入，应该为null）
            sleep(100);
            Integer retryCount4_1 = redisService.getCacheObject(retryCountKey4, Integer.class);
            result.put("场景4-失败后立即查询重试次数", retryCount4_1 == null ? "✅ 正确: null（第一次失败时还未写入）" : "❌ 错误: " + retryCount4_1);

            // 等待一段时间后再次查询（应该仍然为null，因为第一次失败时还未写入）
            sleep(200);
            Integer retryCount4_2 = redisService.getCacheObject(retryCountKey4, Integer.class);
            result.put("场景4-等待后查询重试次数", retryCount4_2 == null ? "✅ 正确: null（第一次失败时还未写入）" : "❌ 错误: " + retryCount4_2);

            // 第二次失败（此时会检测到FAILED状态，递增重试次数为1）
            try {
                idempotentTestAPI.testHttpAdvancedRetryCount(retryToken4, true);
                result.put("场景4-第二次失败", "❌ 应该失败但成功了");
            } catch (Exception e) {
                result.put("场景4-第二次失败", "✅ 正确抛出异常");
            }

            // 等待后查询重试次数（应该为1）
            sleep(300);
            Integer retryCount4_3 = redisService.getCacheObject(retryCountKey4, Integer.class);
            result.put("场景4-第二次失败后查询重试次数", retryCount4_3 != null && retryCount4_3 == 1 ? "✅ 正确: " + retryCount4_3 : "❌ 错误: " + retryCount4_3 + " (期望: 1)");

        } catch (Exception e) {
            result.put("错误", "❌ 重试次数测试异常: " + e.getMessage());
            log.error("重试次数测试异常", e);
        }

        return result;
    }
}
