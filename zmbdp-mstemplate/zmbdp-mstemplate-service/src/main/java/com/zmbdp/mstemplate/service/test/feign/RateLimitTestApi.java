package com.zmbdp.mstemplate.service.test.feign;

import com.zmbdp.common.domain.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 限流测试 Feign 客户端
 * 用于调用需要请求头、参数的测试接口
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "rateLimitTestApi", name = "zmbdp-mstemplate-service", path = "/test/ratelimit")
public interface RateLimitTestApi {

    /**
     * 测试基础限流 - IP维度
     */
    @PostMapping("/basic/ip")
    Result<String> testBasicIp();

    /**
     * 测试基础限流 - 账号维度（有userId）
     */
    @PostMapping("/basic/account")
    Result<String> testBasicAccount(@RequestHeader(value = "userId", required = false) String userId);

    /**
     * 测试基础限流 - 账号维度（无userId，退化为IP）
     */
    @PostMapping("/basic/account-no-user")
    Result<String> testBasicAccountNoUser();

    /**
     * 测试基础限流 - 双维度
     */
    @PostMapping("/basic/both")
    Result<String> testBasicBoth(@RequestHeader(value = "userId", required = false) String userId);

    /**
     * 测试自定义limit和windowSec
     */
    @PostMapping("/custom/limit-window")
    Result<String> testCustomLimitWindow();

    /**
     * 测试自定义message
     */
    @PostMapping("/custom/message")
    Result<String> testCustomMessage();

    /**
     * 测试自定义keySuffix
     */
    @PostMapping("/custom/key-suffix")
    Result<String> testCustomKeySuffix();

    /**
     * 测试IP请求头方式
     */
    @PostMapping("/ip/header")
    Result<String> testIpHeader(@RequestHeader(value = "X-Real-IP", required = false) String ip);

    /**
     * 测试IP请求参数方式
     */
    @GetMapping("/ip/param")
    Result<String> testIpParam(@RequestParam(value = "clientIp", required = false) String clientIp);

    /**
     * 测试IP请求头和参数优先级
     */
    @PostMapping("/ip/priority")
    Result<String> testIpPriority(
            @RequestHeader(value = "X-Client-IP", required = false) String headerIp,
            @RequestParam(value = "ip", required = false) String paramIp);

    /**
     * 测试使用全局配置（limit=0, windowSec=0）
     */
    @PostMapping("/global/config")
    Result<String> testGlobalConfig();

    /**
     * 测试异常情况 - 负数limit
     */
    @PostMapping("/exception/negative-limit")
    Result<String> testExceptionNegativeLimit();

    /**
     * 测试异常情况 - 负数windowSec
     */
    @PostMapping("/exception/negative-window")
    Result<String> testExceptionNegativeWindow();

    /**
     * 测试并发场景
     */
    @PostMapping("/concurrent/test")
    Result<String> testConcurrent();
}
