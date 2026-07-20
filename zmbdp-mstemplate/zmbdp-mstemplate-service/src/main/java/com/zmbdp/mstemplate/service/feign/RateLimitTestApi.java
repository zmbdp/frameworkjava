package com.zmbdp.mstemplate.service.feign;

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
     *
     * @return 限流测试结果
     */
    @PostMapping("/basic/ip")
    Result<String> testBasicIp();

    /**
     * 测试基础限流 - 账号维度（有userId）
     *
     * @param userId 用户ID（请求头传入）
     * @return 限流测试结果
     */
    @PostMapping("/basic/account")
    Result<String> testBasicAccount(@RequestHeader(value = "userId", required = false) String userId);

    /**
     * 测试基础限流 - 账号维度（无userId，退化为IP）
     *
     * @return 限流测试结果
     */
    @PostMapping("/basic/account-no-user")
    Result<String> testBasicAccountNoUser();

    /**
     * 测试基础限流 - 双维度
     *
     * @param userId 用户ID（请求头传入）
     * @return 限流测试结果
     */
    @PostMapping("/basic/both")
    Result<String> testBasicBoth(@RequestHeader(value = "userId", required = false) String userId);

    /**
     * 测试自定义limit和windowSec
     *
     * @return 限流测试结果
     */
    @PostMapping("/custom/limit-window")
    Result<String> testCustomLimitWindow();

    /**
     * 测试自定义message
     *
     * @return 限流测试结果
     */
    @PostMapping("/custom/message")
    Result<String> testCustomMessage();

    /**
     * 测试自定义keySuffix
     *
     * @return 限流测试结果
     */
    @PostMapping("/custom/key-suffix")
    Result<String> testCustomKeySuffix();

    /**
     * 测试IP请求头方式
     *
     * @param ip 客户端 IP（通过 X-Real-IP 请求头传入）
     * @return 限流测试结果
     */
    @PostMapping("/ip/header")
    Result<String> testIpHeader(@RequestHeader(value = "X-Real-IP", required = false) String ip);

    /**
     * 测试IP请求参数方式
     *
     * @param clientIp 客户端 IP（通过请求参数传入）
     * @return 限流测试结果
     */
    @GetMapping("/ip/param")
    Result<String> testIpParam(@RequestParam(value = "clientIp", required = false) String clientIp);

    /**
     * 测试IP请求头和参数优先级
     *
     * @param headerIp 通过请求头传入的 IP
     * @param paramIp  通过请求参数传入的 IP
     * @return 限流测试结果
     */
    @PostMapping("/ip/priority")
    Result<String> testIpPriority(
            @RequestHeader(value = "X-Client-IP", required = false) String headerIp,
            @RequestParam(value = "ip", required = false) String paramIp);

    /**
     * 测试使用全局配置（limit=0, windowSec=0）
     *
     * @return 限流测试结果
     */
    @PostMapping("/global/config")
    Result<String> testGlobalConfig();

    /**
     * 测试异常情况 - 负数limit
     *
     * @return 限流测试结果
     */
    @PostMapping("/exception/negative-limit")
    Result<String> testExceptionNegativeLimit();

    /**
     * 测试异常情况 - 负数windowSec
     *
     * @return 限流测试结果
     */
    @PostMapping("/exception/negative-window")
    Result<String> testExceptionNegativeWindow();

    /**
     * 测试并发场景
     *
     * @return 限流测试结果
     */
    @PostMapping("/concurrent/test")
    Result<String> testConcurrent();
}
