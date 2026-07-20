package com.zmbdp.mstemplate.service.feign;

import com.zmbdp.common.domain.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 幂等性测试 Feign 客户端
 * 用于调用需要请求头、参数的测试接口
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "idempotentTestApi", name = "zmbdp-mstemplate-service", path = "/test/idempotent")
public interface IdempotentTestApi {

    /**
     * 测试请求头方式
     *
     * @param token 幂等性 Token（通过请求头传入）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/basic/header")
    Result<String> testHttpBasicHeader(@RequestHeader("Idempotent-Token") String token);

    /**
     * 测试请求参数方式
     *
     * @param token 幂等性 Token（通过请求参数传入）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/basic/param")
    Result<String> testHttpBasicParam(@RequestParam("idempotentToken") String token);

    /**
     * 测试优先级（请求头优先）
     *
     * @param headerToken 通过请求头传入的 Token
     * @param paramToken  通过请求参数传入的 Token
     * @return 幂等性测试结果
     */
    @PostMapping("/http/basic/priority")
    Result<String> testHttpBasicPriority(
            @RequestHeader("Idempotent-Token") String headerToken,
            @RequestParam("idempotentToken") String paramToken);

    /**
     * 测试强幂等模式
     *
     * @param token 幂等性 Token（通过请求头传入）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/advanced/strong")
    Result<String> testHttpAdvancedStrong(@RequestHeader("Idempotent-Token") String token);

    /**
     * 测试三态设计 - DEFAULT
     *
     * @param token 幂等性 Token（通过请求头传入）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/advanced/mode/default")
    Result<String> testHttpAdvancedModeDefault(@RequestHeader("Idempotent-Token") String token);

    /**
     * 测试三态设计 - TRUE
     *
     * @param token 幂等性 Token（通过请求头传入）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/advanced/mode/true")
    Result<String> testHttpAdvancedModeTrue(@RequestHeader("Idempotent-Token") String token);

    /**
     * 测试三态设计 - FALSE
     *
     * @param token 幂等性 Token（通过请求头传入）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/advanced/mode/false")
    Result<String> testHttpAdvancedModeFalse(@RequestHeader("Idempotent-Token") String token);

    /**
     * 测试业务失败重试
     *
     * @param token      幂等性 Token（通过请求头传入）
     * @param shouldFail 是否模拟业务失败（用于测试幂等性在业务失败后的重试行为）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/advanced/failure")
    Result<String> testHttpAdvancedFailure(
            @RequestHeader("Idempotent-Token") String token,
            @RequestParam(value = "shouldFail", defaultValue = "false") boolean shouldFail);

    /**
     * 测试重试次数功能
     *
     * @param token      幂等性 Token（通过请求头传入）
     * @param shouldFail 是否模拟业务失败（用于测试重试次数限制）
     * @return 幂等性测试结果
     */
    @PostMapping("/http/advanced/retry-count")
    Result<String> testHttpAdvancedRetryCount(
            @RequestHeader("Idempotent-Token") String token,
            @RequestParam(value = "shouldFail", defaultValue = "false") boolean shouldFail);
}
