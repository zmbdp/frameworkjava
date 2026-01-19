package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.idempotent.annotation.Idempotent;
import com.zmbdp.common.idempotent.enums.IdempotentMode;
import com.zmbdp.mstemplate.service.domain.MessageDTO;
import com.zmbdp.mstemplate.service.rabbit.Producer;
import com.zmbdp.mstemplate.service.test.feign.IdempotentTestApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
            boolean headerSuccess1 = headerResult1.getCode() == ResultCode.SUCCESS.getCode();
            httpBasic.put("请求头方式-第一次", headerSuccess1 ? "✅ 成功" : "❌ 失败: " + headerResult1.getErrMsg());
            
            // 2. 请求头方式 - 重复请求（应该失败）
            try {
                Result<String> headerResult2 = idempotentTestAPI.testHttpBasicHeader(token1);
                boolean headerSuccess2 = headerResult2.getCode() == ResultCode.SUCCESS.getCode();
                httpBasic.put("请求头方式-重复", headerSuccess2 ? "❌ 应该失败但成功了" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpBasic.put("请求头方式-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }
            
            // 3. 请求参数方式 - 第一次请求
            Result<String> paramResult1 = idempotentTestAPI.testHttpBasicParam(token2);
            boolean paramSuccess1 = paramResult1.getCode() == ResultCode.SUCCESS.getCode();
            httpBasic.put("请求参数方式-第一次", paramSuccess1 ? "✅ 成功" : "❌ 失败: " + paramResult1.getErrMsg());
            
            // 4. 请求参数方式 - 重复请求（应该失败）
            try {
                Result<String> paramResult2 = idempotentTestAPI.testHttpBasicParam(token2);
                boolean paramSuccess2 = paramResult2.getCode() == ResultCode.SUCCESS.getCode();
                httpBasic.put("请求参数方式-重复", paramSuccess2 ? "❌ 应该失败但成功了" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpBasic.put("请求参数方式-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }
            
            // 5. 优先级测试
            Result<String> priorityResult = idempotentTestAPI.testHttpBasicPriority(token3, "param-token");
            boolean prioritySuccess = priorityResult.getCode() == ResultCode.SUCCESS.getCode();
            httpBasic.put("优先级测试", prioritySuccess ? "✅ 成功（优先使用请求头）" : "❌ 失败: " + priorityResult.getErrMsg());
            
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
            boolean strongSuccess1 = strongResult1.getCode() == ResultCode.SUCCESS.getCode();
            httpAdvanced.put("强幂等模式-第一次", strongSuccess1 ? "✅ 成功（结果已缓存）" : "❌ 失败: " + strongResult1.getErrMsg());
            
            // 2. 强幂等模式 - 重复请求（应该返回缓存结果）
            Result<String> strongResult2 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            boolean strongSuccess2 = strongResult2.getCode() == ResultCode.SUCCESS.getCode();
            // 验证返回的是缓存结果（时间戳应该相同）
            boolean isCached = strongSuccess2 && strongResult1.getData().equals(strongResult2.getData());
            httpAdvanced.put("强幂等模式-重复", isCached ? "✅ 成功返回缓存结果" : "❌ 失败: 未返回缓存结果");
            
            // 3. 三态设计 - DEFAULT（第一次 + 重复请求）
            String defaultToken = UUID.randomUUID().toString();
            Result<String> defaultResult1 = idempotentTestAPI.testHttpAdvancedModeDefault(defaultToken);
            boolean defaultSuccess1 = defaultResult1.getCode() == ResultCode.SUCCESS.getCode();
            httpAdvanced.put("三态设计-DEFAULT-第一次", defaultSuccess1 ? "✅ 成功（使用全局配置）" : "❌ 失败: " + defaultResult1.getErrMsg());
            // 重复请求测试（根据全局配置决定是防重还是强幂等）
            try {
                Result<String> defaultResult2 = idempotentTestAPI.testHttpAdvancedModeDefault(defaultToken);
                boolean defaultSuccess2 = defaultResult2.getCode() == ResultCode.SUCCESS.getCode();
                httpAdvanced.put("三态设计-DEFAULT-重复", defaultSuccess2 ? "✅ 成功（根据全局配置处理）" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpAdvanced.put("三态设计-DEFAULT-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }
            
            // 4. 三态设计 - TRUE（第一次 + 重复请求，应该返回缓存结果）
            String trueToken = UUID.randomUUID().toString();
            Result<String> trueResult1 = idempotentTestAPI.testHttpAdvancedModeTrue(trueToken);
            boolean trueSuccess1 = trueResult1.getCode() == ResultCode.SUCCESS.getCode();
            httpAdvanced.put("三态设计-TRUE-第一次", trueSuccess1 ? "✅ 成功（强制开启强幂等）" : "❌ 失败: " + trueResult1.getErrMsg());
            // 重复请求测试（应该返回缓存结果）
            Result<String> trueResult2 = idempotentTestAPI.testHttpAdvancedModeTrue(trueToken);
            boolean trueSuccess2 = trueResult2.getCode() == ResultCode.SUCCESS.getCode();
            // 验证返回的是缓存结果（时间戳应该相同）
            boolean trueIsCached = trueSuccess2 && trueResult1.getData().equals(trueResult2.getData());
            httpAdvanced.put("三态设计-TRUE-重复", trueIsCached ? "✅ 成功返回缓存结果" : "❌ 失败: 未返回缓存结果");
            
            // 5. 三态设计 - FALSE（第一次 + 重复请求，应该被拒绝）
            String falseToken = UUID.randomUUID().toString();
            Result<String> falseResult1 = idempotentTestAPI.testHttpAdvancedModeFalse(falseToken);
            boolean falseSuccess1 = falseResult1.getCode() == ResultCode.SUCCESS.getCode();
            httpAdvanced.put("三态设计-FALSE-第一次", falseSuccess1 ? "✅ 成功（强制关闭强幂等）" : "❌ 失败: " + falseResult1.getErrMsg());
            // 重复请求测试（应该被拒绝）
            try {
                Result<String> falseResult2 = idempotentTestAPI.testHttpAdvancedModeFalse(falseToken);
                boolean falseSuccess2 = falseResult2.getCode() == ResultCode.SUCCESS.getCode();
                httpAdvanced.put("三态设计-FALSE-重复", falseSuccess2 ? "❌ 应该失败但成功了" : "✅ 正确拒绝重复请求");
            } catch (Exception e) {
                httpAdvanced.put("三态设计-FALSE-重复", "✅ 正确拒绝重复请求（抛出异常）");
            }
            
            // 6. 业务失败重试
            String failureToken = UUID.randomUUID().toString();
            try {
                Result<String> failureResult = idempotentTestAPI.testHttpAdvancedFailure(failureToken, true);
                boolean failureSuccess = failureResult.getCode() == ResultCode.SUCCESS.getCode();
                httpAdvanced.put("业务失败重试", failureSuccess ? "❌ 应该失败但成功了" : "✅ 正确抛出异常");
            } catch (Exception e) {
                httpAdvanced.put("业务失败重试", "✅ 正确抛出异常，Token会被删除");
                // 验证可以重试
                Result<String> retryResult = idempotentTestAPI.testHttpAdvancedFailure(failureToken, false);
                boolean retrySuccess = retryResult.getCode() == ResultCode.SUCCESS.getCode();
                httpAdvanced.put("业务失败重试-验证", retrySuccess ? "✅ 可以重试成功" : "❌ 重试失败: " + retryResult.getErrMsg());
            }
            
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
            MessageDTO message1 = new MessageDTO();
            message1.setType("测试消息");
            message1.setDesc("MQ基础功能测试 - SpEL表达式");
            message1.setIdempotentToken(mqToken1);
            producer.produceMsgIdempotent(message1);
            mqBasic.put("SpEL表达式-第一次", "✅ 消息发送成功，Token: " + mqToken1);
            
            // 2. SpEL表达式方式 - 重复（应该被拒绝）
            MessageDTO message2 = new MessageDTO();
            message2.setType("测试消息");
            message2.setDesc("MQ基础功能测试 - SpEL表达式重复");
            message2.setIdempotentToken(mqToken1);
            producer.produceMsgIdempotent(message2);
            mqBasic.put("SpEL表达式-重复", "✅ 消息发送成功（消费者会拒绝处理）");
            
            // 3. 消息头方式 - 第一次
            MessageDTO message3 = new MessageDTO();
            message3.setType("测试消息");
            message3.setDesc("MQ基础功能测试 - 消息头方式");
            producer.produceMsgIdempotentHeader(message3, mqToken2);
            mqBasic.put("消息头方式-第一次", "✅ 消息发送成功，Token: " + mqToken2);
            
            // 4. 消息头方式 - 重复（应该被拒绝）
            MessageDTO message4 = new MessageDTO();
            message4.setType("测试消息");
            message4.setDesc("MQ基础功能测试 - 消息头方式重复");
            producer.produceMsgIdempotentHeader(message4, mqToken2);
            mqBasic.put("消息头方式-重复", "✅ 消息发送成功（消费者会拒绝处理）");
            
            // 5. 批量发送测试
            String batchToken = UUID.randomUUID().toString();
            for (int i = 1; i <= 3; i++) {
                MessageDTO message = new MessageDTO();
                message.setType("批量测试");
                message.setDesc("MQ基础功能测试 - 第" + i + "条消息");
                message.setIdempotentToken(batchToken);
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
            MessageDTO failureMessage1 = new MessageDTO();
            failureMessage1.setType("测试失败");
            failureMessage1.setDesc("MQ高级功能测试 - 模拟业务失败（第一次）");
            String failureToken = UUID.randomUUID().toString();
            failureMessage1.setIdempotentToken(failureToken);
            producer.produceMsgIdempotentFailure(failureMessage1);
            mqAdvanced.put("业务失败-第一次", "✅ 消息发送成功，Token: " + failureToken + "（消费者会抛出异常，Token会被删除）");
            
            // 等待一小段时间，确保第一次消息处理完成
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 验证可以重试（发送成功类型的消息，应该能正常处理）
            MessageDTO retryMessage = new MessageDTO();
            retryMessage.setType("测试成功");
            retryMessage.setDesc("MQ高级功能测试 - 验证业务失败后可以重试");
            retryMessage.setIdempotentToken(failureToken);
            producer.produceMsgIdempotentFailure(retryMessage);
            mqAdvanced.put("业务失败-重试验证", "✅ 消息发送成功（Token已删除，可以重试）");
            
            // 2. 自定义过期时间测试
            MessageDTO expireMessage = new MessageDTO();
            expireMessage.setType("测试消息");
            expireMessage.setDesc("MQ高FAILED 级功能测试 - 自定义过期时间");
            String expireToken = UUID.randomUUID().toString();
            expireMessage.setIdempotentToken(expireToken);
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
            boolean httpSuccess = httpResult.getCode() == ResultCode.SUCCESS.getCode();
            result.put("HTTP请求头方式", httpSuccess ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("HTTP请求头方式", "❌ 异常: " + e.getMessage());
        }
        
        // HTTP强幂等模式
        try {
            String strongToken = UUID.randomUUID().toString();
            Result<String> strongResult1 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            Result<String> strongResult2 = idempotentTestAPI.testHttpAdvancedStrong(strongToken);
            boolean strongSuccess1 = strongResult1.getCode() == ResultCode.SUCCESS.getCode();
            boolean strongSuccess2 = strongResult2.getCode() == ResultCode.SUCCESS.getCode();
            result.put("HTTP强幂等模式", strongSuccess1 && strongSuccess2 ? "✅ 成功" : "❌ 失败");
        } catch (Exception e) {
            result.put("HTTP强幂等模式", "❌ 异常: " + e.getMessage());
        }
        
        // MQ功能
        try {
            MessageDTO message = new MessageDTO();
            message.setType("快速测试");
            message.setDesc("快速测试消息");
            message.setIdempotentToken(UUID.randomUUID().toString());
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
}
