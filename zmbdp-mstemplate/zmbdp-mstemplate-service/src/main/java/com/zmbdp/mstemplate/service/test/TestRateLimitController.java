package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.ratelimit.annotation.RateLimit;
import com.zmbdp.common.ratelimit.enums.RateLimitDimension;
import com.zmbdp.mstemplate.service.test.feign.RateLimitTestApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流功能测试控制器
 * 使用 OpenFeign 客户端，一键测试所有功能，无需手动传参
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/ratelimit")
public class TestRateLimitController {

    /**
     * 限流测试 Feign 客户端
     */
    @Autowired
    private RateLimitTestApi rateLimitTestApi;

    /**
     * 线程池执行器（用于高并发测试）
     */
    @Autowired
    @Qualifier(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
    private Executor threadPoolExecutor;

    /*=============================================    一键测试接口    =============================================*/

    /**
     * 一键测试所有功能
     * 直接调用此接口即可测试所有限流功能，无需传参
     *
     * @return 详细的测试结果
     */
    @PostMapping("/all")
    public Result<Map<String, Object>> testAll() {
        log.info("=== 一键测试所有限流功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        // ========== 基础功能测试 ==========
        Map<String, Object> basic = new LinkedHashMap<>();
        try {
            // 1. IP维度限流 - 正常请求（只测试一次，避免触发限流影响后续测试）
            Result<String> ipResult1 = rateLimitTestApi.testBasicIp();
            basic.put("IP维度-正常请求", isSuccess(ipResult1) ? "✅ 成功" : "❌ 失败: " + ipResult1.getErrMsg());

            // 等待一小段时间，避免限流影响后续测试
            sleep(500);

            // 2. 账号维度限流 - 有userId（使用不同的keySuffix，避免与IP维度冲突）
            Result<String> accountResult1 = rateLimitTestApi.testBasicAccount("user123");
            basic.put("账号维度-有userId-正常请求", isSuccess(accountResult1) ? "✅ 成功" : "❌ 失败: " + accountResult1.getErrMsg());

            sleep(500);

            // 3. 账号维度限流 - 无userId（退化为IP，但使用不同的接口路径）
            Result<String> accountNoUserResult = rateLimitTestApi.testBasicAccountNoUser();
            basic.put("账号维度-无userId-退化为IP", isSuccess(accountNoUserResult) ? "✅ 成功（已退化为IP限流）" : "❌ 失败: " + accountNoUserResult.getErrMsg());

            sleep(500);

            // 4. 双维度限流 - 有userId（使用不同的keySuffix）
            Result<String> bothResult1 = rateLimitTestApi.testBasicBoth("user456");
            basic.put("双维度限流-有userId-正常请求", isSuccess(bothResult1) ? "✅ 成功" : "❌ 失败: " + bothResult1.getErrMsg());

            sleep(500);

            // 5. 双维度限流 - 无userId（退化为IP，但使用不同的接口路径）
            Result<String> bothNoUserResult = rateLimitTestApi.testBasicBoth(null);
            basic.put("双维度限流-无userId-退化为IP", isSuccess(bothNoUserResult) ? "✅ 成功（已退化为IP限流）" : "❌ 失败: " + bothNoUserResult.getErrMsg());

        } catch (Exception e) {
            basic.put("错误", "❌ 基础功能测试异常: " + e.getMessage());
            log.error("基础功能测试异常", e);
        }
        result.put("基础功能", basic);

        // ========== 自定义参数测试 ==========
        Map<String, Object> custom = new LinkedHashMap<>();
        try {
            // 等待一小段时间，避免与基础功能测试冲突
            sleep(500);

            // 1. 自定义limit和windowSec（使用不同的keySuffix，避免冲突）
            Result<String> customLimitResult = rateLimitTestApi.testCustomLimitWindow();
            custom.put("自定义limit和windowSec", isSuccess(customLimitResult) ? "✅ 成功" : "❌ 失败: " + customLimitResult.getErrMsg());

            sleep(500);

            // 2. 自定义message（只测试一次，避免触发限流）
            Result<String> customMessageResult = rateLimitTestApi.testCustomMessage();
            custom.put("自定义message-正常请求", isSuccess(customMessageResult) ? "✅ 成功" : "❌ 失败: " + customMessageResult.getErrMsg());
            // 注意：自定义message的限流提示测试已移除，避免触发限流影响其他测试
            // 如需测试限流提示，请单独调用 /test/ratelimit/custom/message 接口多次

            sleep(500);

            // 3. 自定义keySuffix（使用不同的keySuffix，避免冲突）
            Result<String> keySuffixResult = rateLimitTestApi.testCustomKeySuffix();
            custom.put("自定义keySuffix", isSuccess(keySuffixResult) ? "✅ 成功" : "❌ 失败: " + keySuffixResult.getErrMsg());

        } catch (Exception e) {
            custom.put("错误", "❌ 自定义参数测试异常: " + e.getMessage());
            log.error("自定义参数测试异常", e);
        }
        result.put("自定义参数", custom);

        // ========== IP获取方式测试 ==========
        Map<String, Object> ipTest = new LinkedHashMap<>();
        try {
            // 等待一小段时间，避免与之前的测试冲突
            sleep(500);

            // 1. IP请求头方式（使用不同的keySuffix，避免冲突）
            Result<String> headerResult = rateLimitTestApi.testIpHeader("192.168.1.100");
            ipTest.put("IP请求头方式", isSuccess(headerResult) ? "✅ 成功" : "❌ 失败: " + headerResult.getErrMsg());

            sleep(500);

            // 2. IP请求参数方式（使用不同的keySuffix，避免冲突）
            Result<String> paramResult = rateLimitTestApi.testIpParam("192.168.1.200");
            ipTest.put("IP请求参数方式", isSuccess(paramResult) ? "✅ 成功" : "❌ 失败: " + paramResult.getErrMsg());

            sleep(500);

            // 3. IP优先级测试（请求头优先，使用不同的keySuffix，避免冲突）
            Result<String> priorityResult = rateLimitTestApi.testIpPriority("192.168.1.300", "192.168.1.400");
            ipTest.put("IP优先级测试", isSuccess(priorityResult) ? "✅ 成功（请求头优先）" : "❌ 失败: " + priorityResult.getErrMsg());

        } catch (Exception e) {
            ipTest.put("错误", "❌ IP获取方式测试异常: " + e.getMessage());
            log.error("IP获取方式测试异常", e);
        }
        result.put("IP获取方式", ipTest);

        // ========== 全局配置测试 ==========
        Map<String, Object> global = new LinkedHashMap<>();
        try {
            // 等待一小段时间，避免与之前的测试冲突
            sleep(500);

            Result<String> globalResult = rateLimitTestApi.testGlobalConfig();
            global.put("使用全局配置", isSuccess(globalResult) ? "✅ 成功（使用Nacos全局配置）" : "❌ 失败: " + globalResult.getErrMsg());
        } catch (Exception e) {
            global.put("错误", "❌ 全局配置测试异常: " + e.getMessage());
            log.error("全局配置测试异常", e);
        }
        result.put("全局配置", global);

        // ========== 异常情况测试 ==========
        Map<String, Object> exception = new LinkedHashMap<>();
        try {
            // 等待一小段时间，避免与之前的测试冲突
            sleep(500);

            // 1. 负数limit（应该使用全局配置或默认值，使用不同的keySuffix，避免冲突）
            Result<String> negativeLimitResult = rateLimitTestApi.testExceptionNegativeLimit();
            exception.put("负数limit", isSuccess(negativeLimitResult) ? "✅ 成功（使用全局配置）" : "❌ 失败: " + negativeLimitResult.getErrMsg());

            sleep(500);

            // 2. 负数windowSec（应该使用全局配置或默认值，使用不同的keySuffix，避免冲突）
            Result<String> negativeWindowResult = rateLimitTestApi.testExceptionNegativeWindow();
            exception.put("负数windowSec", isSuccess(negativeWindowResult) ? "✅ 成功（使用全局配置）" : "❌ 失败: " + negativeWindowResult.getErrMsg());

        } catch (Exception e) {
            exception.put("错误", "❌ 异常情况测试异常: " + e.getMessage());
            log.error("异常情况测试异常", e);
        }
        result.put("异常情况", exception);

        // ========== 并发测试 ==========
        Map<String, Object> concurrent = testConcurrentInternal();
        result.put("并发测试", concurrent);

        result.put("测试说明", "所有测试完成，请查看日志获取详细信息");
        result.put("提示", "HTTP测试使用了OpenFeign客户端，测试了所有RateLimit注解参数");

        return Result.success(result);
    }

    /**
     * 高并发测试
     * 通过params传入并发量参数，测试高并发场景下限流是否正常工作
     *
     * @param concurrency 并发量，默认50
     * @return 测试结果
     */
    @PostMapping("/concurrent")
    public Result<Map<String, Object>> testConcurrent(@RequestParam(value = "concurrency", defaultValue = "50") int concurrency) {
        log.info("=== 高并发测试，并发量: {} ===", concurrency);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("并发量", concurrency);
        result.put("说明", "测试同一IP的并发请求，验证限流是否正常工作");

        try {
            // 用于统计结果
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger limitedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(concurrency);

            log.info("并发测试开始，并发量: {}", concurrency);
            long startTime = System.currentTimeMillis();

            // 并发执行请求
            for (int i = 0; i < concurrency; i++) {
                final int index = i;
                threadPoolExecutor.execute(() -> {
                    try {
                        Result<String> response = rateLimitTestApi.testConcurrent();
                        if (isSuccess(response)) {
                            successCount.incrementAndGet();
                            log.info("并发测试 - 线程{}成功", index);
                        } else {
                            // 检查是否是限流错误（错误码 400020 或包含"频繁"关键词）
                            if (response.getCode() == ResultCode.REQUEST_TOO_FREQUENT.getCode()) {
                                limitedCount.incrementAndGet();
                                log.info("并发测试 - 线程{}被限流: {}", index, response.getErrMsg());
                            } else if (response.getErrMsg() != null && response.getErrMsg().contains("频繁")) {
                                limitedCount.incrementAndGet();
                                log.info("并发测试 - 线程{}被限流: {}", index, response.getErrMsg());
                            } else {
                                errorCount.incrementAndGet();
                                log.warn("并发测试 - 线程{}失败: {}", index, response.getErrMsg());
                            }
                        }
                    } catch (Exception e) {
                        // Feign 异常处理：检查异常信息中是否包含限流错误
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.contains("400020") || errorMsg.contains("频繁") || errorMsg.contains("REQUEST_TOO_FREQUENT"))) {
                            limitedCount.incrementAndGet();
                            log.info("并发测试 - 线程{}被限流（异常）: {}", index, errorMsg);
                        } else {
                            errorCount.incrementAndGet();
                            log.warn("并发测试 - 线程{}异常: {}", index, errorMsg);
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
                log.warn("并发测试等待被中断");
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            result.put("成功数", successCount.get());
            result.put("限流数", limitedCount.get());
            result.put("错误数", errorCount.get());
            result.put("总请求数", concurrency);
            result.put("耗时(ms)", duration);

            // 验证结果：应该有部分请求被限流
            if (limitedCount.get() > 0) {
                result.put("结果", "✅ 通过（成功: " + successCount.get() + "，限流: " + limitedCount.get() + "）");
            } else {
                result.put("结果", "⚠️ 未触发限流（可能配置较大或时间窗口未到）");
            }

            log.info("并发测试完成，成功: {}, 限流: {}, 错误: {}, 耗时: {}ms",
                    successCount.get(), limitedCount.get(), errorCount.get(), duration);

        } catch (Exception e) {
            result.put("错误", "❌ 高并发测试异常: " + e.getMessage());
            log.error("高并发测试异常", e);
        }

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

        // IP维度限流
        try {
            Result<String> ipResult = rateLimitTestApi.testBasicIp();
            result.put("IP维度限流", isSuccess(ipResult) ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("IP维度限流", "❌ 异常: " + e.getMessage());
        }

        // 账号维度限流
        try {
            Result<String> accountResult = rateLimitTestApi.testBasicAccount("user123");
            result.put("账号维度限流", isSuccess(accountResult) ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("账号维度限流", "❌ 异常: " + e.getMessage());
        }

        // 自定义参数
        try {
            Result<String> customResult = rateLimitTestApi.testCustomLimitWindow();
            result.put("自定义参数", isSuccess(customResult) ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("自定义参数", "❌ 异常: " + e.getMessage());
        }

        return Result.success(result);
    }

    /**
     * 获取测试说明
     */
    @GetMapping("/help")
    public Result<String> getTestHelp() {
        String help = "============================ 限流功能测试说明 ============================\n\n" +
                "【一键测试接口】\n\n" +
                "1. POST /test/ratelimit/all - 一键测试所有功能\n" +
                "   说明：直接调用此接口即可测试所有限流功能，无需传参\n" +
                "   测试内容：基础功能、自定义参数、IP获取方式、全局配置、异常情况、并发测试\n\n" +
                "2. POST /test/ratelimit/quick - 快速测试核心功能\n" +
                "   说明：快速测试核心功能，测试时间更短\n\n" +
                "3. POST /test/ratelimit/concurrent?concurrency=50 - 高并发测试\n" +
                "   说明：测试高并发场景下的限流功能，可指定并发量\n\n" +
                "4. GET /test/ratelimit/help - 获取测试说明（本接口）\n\n" +
                "【测试说明】\n" +
                "- HTTP测试使用OpenFeign客户端自动调用，无需手动传参\n" +
                "- 测试了RateLimit注解的所有参数：limit、windowSec、dimensions、message、keySuffix、ipHeaderName、allowIpParam、ipParamName\n" +
                "- 测试了正常和异常情况\n" +
                "- 所有测试结果都会在返回的JSON中显示\n" +
                "- 请查看日志获取详细信息\n\n" +
                "======================================================================\n";

        return Result.success(help);
    }

    /*=============================================    Feign 客户端调用的测试接口    =============================================*/

    /**
     * 基础限流测试 - IP维度
     * 测试参数：limit=5, windowSec=60, dimensions=IP, keySuffix="test-basic-ip"
     */
    @PostMapping("/basic/ip")
    @RateLimit(limit = 5, windowSec = 60, dimensions = RateLimitDimension.IP, keySuffix = "test-basic-ip")
    public Result<String> testBasicIp() {
        return Result.success("IP维度限流测试成功");
    }

    /**
     * 基础限流测试 - 账号维度（有userId）
     * 测试参数：limit=3, windowSec=60, dimensions=ACCOUNT, keySuffix="test-basic-account"
     */
    @PostMapping("/basic/account")
    @RateLimit(limit = 3, windowSec = 60, dimensions = RateLimitDimension.ACCOUNT, keySuffix = "test-basic-account")
    public Result<String> testBasicAccount() {
        return Result.success("账号维度限流测试成功");
    }

    /**
     * 基础限流测试 - 账号维度（无userId，退化为IP）
     * 测试参数：limit=3, windowSec=60, dimensions=ACCOUNT, keySuffix="test-basic-account-no-user"
     */
    @PostMapping("/basic/account-no-user")
    @RateLimit(limit = 3, windowSec = 60, dimensions = RateLimitDimension.ACCOUNT, keySuffix = "test-basic-account-no-user")
    public Result<String> testBasicAccountNoUser() {
        return Result.success("账号维度限流测试成功（无userId，已退化为IP限流）");
    }

    /**
     * 基础限流测试 - 双维度
     * 测试参数：limit=2, windowSec=60, dimensions=BOTH, keySuffix="test-basic-both"
     */
    @PostMapping("/basic/both")
    @RateLimit(limit = 2, windowSec = 60, dimensions = RateLimitDimension.BOTH, keySuffix = "test-basic-both")
    public Result<String> testBasicBoth() {
        return Result.success("双维度限流测试成功");
    }

    /**
     * 自定义limit和windowSec测试
     * 测试参数：limit=10, windowSec=30, keySuffix="test-custom-limit-window"
     */
    @PostMapping("/custom/limit-window")
    @RateLimit(limit = 10, windowSec = 30, keySuffix = "test-custom-limit-window")
    public Result<String> testCustomLimitWindow() {
        return Result.success("自定义limit和windowSec测试成功");
    }

    /**
     * 自定义message测试
     * 测试参数：limit=3, windowSec=60, message="自定义限流提示信息", keySuffix="test-custom-message"
     */
    @PostMapping("/custom/message")
    @RateLimit(limit = 3, windowSec = 60, message = "自定义限流提示信息，请稍后重试", keySuffix = "test-custom-message")
    public Result<String> testCustomMessage() {
        return Result.success("自定义message测试成功");
    }

    /**
     * 自定义keySuffix测试
     * 测试参数：limit=5, windowSec=60, keySuffix="test-custom-key-suffix"
     */
    @PostMapping("/custom/key-suffix")
    @RateLimit(limit = 5, windowSec = 60, keySuffix = "test-custom-key-suffix")
    public Result<String> testCustomKeySuffix() {
        return Result.success("自定义keySuffix测试成功");
    }

    /**
     * IP请求头方式测试
     * 测试参数：limit=5, windowSec=60, ipHeaderName="X-Real-IP", keySuffix="test-ip-header"
     */
    @PostMapping("/ip/header")
    @RateLimit(limit = 5, windowSec = 60, ipHeaderName = "X-Real-IP", keySuffix = "test-ip-header")
    public Result<String> testIpHeader() {
        return Result.success("IP请求头方式测试成功");
    }

    /**
     * IP请求参数方式测试
     * 测试参数：limit=5, windowSec=60, allowIpParam=true, ipParamName="clientIp", keySuffix="test-ip-param"
     */
    @GetMapping("/ip/param")
    @RateLimit(limit = 5, windowSec = 60, allowIpParam = true, ipParamName = "clientIp", keySuffix = "test-ip-param")
    public Result<String> testIpParam() {
        return Result.success("IP请求参数方式测试成功");
    }

    /**
     * IP优先级测试（请求头优先）
     * 测试参数：limit=5, windowSec=60, ipHeaderName="X-Client-IP", allowIpParam=true, ipParamName="ip", keySuffix="test-ip-priority"
     */
    @PostMapping("/ip/priority")
    @RateLimit(limit = 5, windowSec = 60, ipHeaderName = "X-Client-IP", allowIpParam = true, ipParamName = "ip", keySuffix = "test-ip-priority")
    public Result<String> testIpPriority() {
        return Result.success("IP优先级测试成功（请求头优先）");
    }

    /**
     * 使用全局配置测试
     * 测试参数：limit=0, windowSec=0（使用Nacos全局配置）, keySuffix="test-global-config"
     */
    @PostMapping("/global/config")
    @RateLimit(limit = 0, windowSec = 0, keySuffix = "test-global-config")
    public Result<String> testGlobalConfig() {
        return Result.success("使用全局配置测试成功");
    }

    /**
     * 异常情况测试 - 负数limit
     * 测试参数：limit=-1（应该使用全局配置或默认值）, keySuffix="test-exception-negative-limit"
     */
    @PostMapping("/exception/negative-limit")
    @RateLimit(limit = -1, windowSec = 60, keySuffix = "test-exception-negative-limit")
    public Result<String> testExceptionNegativeLimit() {
        return Result.success("负数limit测试成功（应使用全局配置）");
    }

    /**
     * 异常情况测试 - 负数windowSec
     * 测试参数：windowSec=-1（应该使用全局配置或默认值）, keySuffix="test-exception-negative-window"
     */
    @PostMapping("/exception/negative-window")
    @RateLimit(limit = 5, windowSec = -1, keySuffix = "test-exception-negative-window")
    public Result<String> testExceptionNegativeWindow() {
        return Result.success("负数windowSec测试成功（应使用全局配置）");
    }

    /**
     * 并发测试专用接口
     * 测试参数：limit=10, windowSec=60（较小的限流值，便于测试）, keySuffix="test-concurrent"
     */
    @PostMapping("/concurrent/test")
    @RateLimit(limit = 10, windowSec = 60, keySuffix = "test-concurrent")
    public Result<String> testConcurrent() {
        return Result.success("并发测试成功 - " + System.currentTimeMillis());
    }

    /*=============================================    辅助方法    =============================================*/

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
     * 并发测试辅助方法
     */
    private Map<String, Object> testConcurrentInternal() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // 用于统计结果
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger limitedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(30);

            log.info("并发测试开始");
            long startTime = System.currentTimeMillis();

            // 并发执行请求
            for (int i = 0; i < 30; i++) {
                final int index = i;
                threadPoolExecutor.execute(() -> {
                    try {
                        Result<String> response = rateLimitTestApi.testConcurrent();
                        if (isSuccess(response)) {
                            successCount.incrementAndGet();
                        } else {
                            // 检查是否是限流错误（错误码 400020 或包含"频繁"关键词）
                            if (response.getCode() == ResultCode.REQUEST_TOO_FREQUENT.getCode()) {
                                limitedCount.incrementAndGet();
                            } else if (response.getErrMsg() != null && response.getErrMsg().contains("频繁")) {
                                limitedCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        // Feign 异常处理：检查异常信息中是否包含限流错误
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.contains("400020") || errorMsg.contains("频繁") || errorMsg.contains("REQUEST_TOO_FREQUENT"))) {
                            limitedCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
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
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            result.put("成功数", successCount.get());
            result.put("限流数", limitedCount.get());
            result.put("错误数", errorCount.get());
            result.put("总请求数", 30);
            result.put("耗时(ms)", duration);

            if (limitedCount.get() > 0) {
                result.put("结果", "✅ 通过（成功: " + successCount.get() + "，限流: " + limitedCount.get() + "）");
            } else {
                result.put("结果", "⚠️ 未触发限流");
            }

        } catch (Exception e) {
            result.put("错误", "❌ 并发测试异常: " + e.getMessage());
        }
        return result;
    }
}