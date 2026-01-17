package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.idempotent.annotation.Idempotent;
import com.zmbdp.mstemplate.service.domain.MessageDTO;
import com.zmbdp.mstemplate.service.rabbit.Producer;
import com.zmbdp.mstemplate.service.service.IdempotentTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 幂等性功能测试控制器
 * 全面测试HTTP接口和MQ消费者的幂等性功能
 *
 * @author 稚名不带撇
 */
@RestController
@Slf4j
@RequestMapping("/test/idempotent")
public class TestIdempotentController {

    /**
     * 幂等性测试服务
     */
    @Autowired
    private IdempotentTestService idempotentTestService;

    /**
     * MQ消息发送者
     */
    @Autowired
    private Producer producer;

    /*=============================================    HTTP接口测试    =============================================*/

    /**
     * 测试1：从请求头获取Token - 第一次请求（成功）
     * 请求头：Idempotent-Token: test-token-001
     */
    @PostMapping("/http/header/first")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpHeaderFirst() {
        log.info("=== 测试1：HTTP请求头 - 第一次请求 ===");
        return Result.success("第一次请求成功");
    }

    /**
     * 测试2：从请求头获取Token - 重复请求（失败）
     * 请求头：Idempotent-Token: test-token-002
     * 连续调用两次，第二次应该返回"请勿重复提交"
     */
    @PostMapping("/http/header/repeat")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpHeaderRepeat() {
        log.info("=== 测试2：HTTP请求头 - 重复请求 ===");
        return Result.success("请求成功（如果看到这个，说明是第一次请求）");
    }

    /**
     * 测试3：从请求头获取Token - Token为空（失败）
     * 不传请求头或传空值
     */
    @PostMapping("/http/header/empty")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpHeaderEmpty() {
        log.info("=== 测试3：HTTP请求头 - Token为空 ===");
        return Result.success("不应该执行到这里");
    }

    /**
     * 测试4：从请求参数获取Token - 第一次请求（成功）
     * 请求参数：idempotentToken=test-token-003
     */
    @PostMapping("/http/param/first")
    @Idempotent(
            allowParam = true,
            paramName = "idempotentToken",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpParamFirst(@RequestParam(value = "idempotentToken", required = false) String token) {
        log.info("=== 测试4：HTTP请求参数 - 第一次请求，Token: {} ===", token);
        return Result.success("第一次请求成功，Token: " + token);
    }

    /**
     * 测试5：从请求参数获取Token - 重复请求（失败）
     * 请求参数：idempotentToken=test-token-004
     */
    @PostMapping("/http/param/repeat") // TODO: 明天从儿开始测试
    @Idempotent(
            allowParam = true,
            paramName = "idempotentToken",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpParamRepeat(@RequestParam(value = "idempotentToken", required = false) String token) {
        log.info("=== 测试5：HTTP请求参数 - 重复请求，Token: {} ===", token);
        return Result.success("请求成功（如果看到这个，说明是第一次请求）");
    }

    /**
     * 测试6：从请求参数获取Token - Token为空（失败）
     */
    @PostMapping("/http/param/empty")
    @Idempotent(
            allowParam = true,
            paramName = "idempotentToken",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpParamEmpty() {
        log.info("=== 测试6：HTTP请求参数 - Token为空 ===");
        return Result.success("不应该执行到这里");
    }

    /**
     * 测试7：优先级测试 - 请求头和参数都有Token，优先使用请求头
     * 请求头：Idempotent-Token: header-token-001
     * 请求参数：idempotentToken=param-token-001
     * 应该使用请求头的Token
     */
    @PostMapping("/http/priority/header")
    @Idempotent(
            headerName = "Idempotent-Token",
            allowParam = true,
            paramName = "idempotentToken",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpPriorityHeader(@RequestParam(value = "idempotentToken", required = false) String paramToken) {
        log.info("=== 测试7：HTTP优先级 - 请求头和参数都有Token ===");
        return Result.success("应该使用请求头的Token");
    }

    /**
     * 测试8：业务方法执行失败，Token被删除，允许重试
     * 第一次请求会抛出异常，Token会被删除，可以立即重试
     */
    @PostMapping("/http/failure/retry")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpFailureRetry(@RequestParam("shouldFail") boolean shouldFail) {
        log.info("=== 测试8：HTTP业务失败 - Token被删除允许重试，shouldFail: {} ===", shouldFail);
        if (shouldFail) {
            throw new ServiceException("模拟业务异常，Token会被删除，允许重试");
        }
        return Result.success("业务执行成功");
    }

    /**
     * 测试9：自定义过期时间测试
     * 设置10秒过期时间，10秒后可以重新请求
     */
    @PostMapping("/http/custom/expire")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 10,
            message = "10秒内请勿重复提交"
    )
    public Result<String> testHttpCustomExpire() {
        log.info("=== 测试9：HTTP自定义过期时间 - 10秒 ===");
        return Result.success("请求成功，10秒后可以重新请求");
    }

    /**
     * 测试10：自定义错误提示信息
     */
    @PostMapping("/http/custom/message")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "自定义错误提示：操作过于频繁，请稍后再试"
    )
    public Result<String> testHttpCustomMessage() {
        log.info("=== 测试10：HTTP自定义错误提示信息 ===");
        return Result.success("第一次请求成功");
    }

    /**
     * 测试11：自定义请求头名称
     * 使用自定义请求头：X-Idempotency-Key
     */
    @PostMapping("/http/custom/header")
    @Idempotent(
            headerName = "X-Idempotency-Key",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testHttpCustomHeader() {
        log.info("=== 测试11：HTTP自定义请求头名称 ===");
        return Result.success("第一次请求成功");
    }

    /**
     * 测试12：测试Service层方法的幂等性
     */
    @PostMapping("/http/service/method")
    public Result<String> testHttpServiceMethod(@RequestHeader(value = "Idempotent-Token", required = false) String token) {
        log.info("=== 测试12：HTTP Service层方法幂等性，Token: {} ===", token);
        return idempotentTestService.testIdempotentMethod();
    }

    /**
     * 测试13：强幂等模式 - 第一次请求（成功，返回结果并缓存）
     * 设置 returnCachedResult = true，开启强幂等模式
     */
    @PostMapping("/http/strong/first")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交",
            returnCachedResult = true
    )
    public Result<String> testHttpStrongIdempotentFirst() {
        log.info("=== 测试13：HTTP强幂等模式 - 第一次请求 ===");
        return Result.success("第一次请求成功（结果已缓存）");
    }

    /**
     * 测试14：强幂等模式 - 重复请求（成功，返回第一次的结果）
     * 第二次请求应该返回第一次的结果，而不是报错
     */
    @PostMapping("/http/strong/repeat")
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交",
            returnCachedResult = true
    )
    public Result<String> testHttpStrongIdempotentRepeat() {
        log.info("=== 测试14：HTTP强幂等模式 - 重复请求 ===");
        return Result.success("第一次请求成功（如果看到这个，说明是第一次请求）");
    }

    /*=============================================    MQ消费者测试    =============================================*/

    /**
     * 测试15：发送MQ消息 - 从消息对象获取Token（第一次，成功）
     * 使用SpEL表达式从MessageDTO.idempotentToken获取Token
     */
    @PostMapping("/mq/spel/first")
    public Result<String> testMqSpelFirst() {
        log.info("=== 测试15：MQ SpEL表达式 - 第一次发送消息 ===");
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - SpEL表达式");
        messageDTO.setIdempotentToken(UUID.randomUUID().toString());
        // 发送消息，进行幂等性测试
        producer.produceMsgIdempotent(messageDTO);
        // 把当前生成的幂等性 Token 返回给前端，方便后续重复发送消息
        return Result.success("消息发送成功，Token: " + messageDTO.getIdempotentToken());
    }

    /**
     * 测试16：发送MQ消息 - 从消息对象获取Token（重复，失败）
     * 发送相同Token的消息，消费者会拒绝处理
     */
    @PostMapping("/mq/spel/repeat")
    public Result<String> testMqSpelRepeat(@RequestParam(value = "token", required = false) String token) {
        log.info("=== 测试16：MQ SpEL表达式 - 重复发送消息，Token: {} ===", token);
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - SpEL表达式重复消息");
        // 如果传入了 token，使用传入的 token，否则生成新的
        messageDTO.setIdempotentToken(token != null ? token : UUID.randomUUID().toString());
        
        producer.produceMsgIdempotent(messageDTO);
        return Result.success("消息发送成功（如果Token重复，消费者会拒绝处理），Token: " + messageDTO.getIdempotentToken());
    }

    /**
     * 测试17：发送MQ消息 - Token为空（失败）
     * 不设置idempotentToken字段
     */
    @PostMapping("/mq/spel/empty")
    public Result<String> testMqSpelEmpty() {
        log.info("=== 测试17：MQ SpEL表达式 - Token为空 ===");
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - SpEL表达式Token为空");
        // 不设置 idempotentToken
        
        producer.produceMsgIdempotent(messageDTO);
        return Result.success("消息发送成功（消费者会因为Token为空而拒绝处理）");
    }

    /**
     * 测试18：发送MQ消息 - 业务执行失败，Token被删除允许重试
     */
    @PostMapping("/mq/spel/failure")
    public Result<String> testMqSpelFailure() {
        log.info("=== 测试18：MQ SpEL表达式 - 业务失败允许重试 ===");
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试失败");
        messageDTO.setDesc("幂等性测试 - 模拟业务失败");
        messageDTO.setIdempotentToken(UUID.randomUUID().toString());
        
        producer.produceMsgIdempotentFailure(messageDTO);
        return Result.success("消息发送成功，Token: " + messageDTO.getIdempotentToken() + "（消费者会抛出异常，Token会被删除）");
    }

    /**
     * 测试19：发送MQ消息 - 自定义过期时间
     */
    @PostMapping("/mq/spel/expire")
    public Result<String> testMqSpelExpire() {
        log.info("=== 测试19：MQ SpEL表达式 - 自定义过期时间 ===");
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - 自定义过期时间");
        messageDTO.setIdempotentToken(UUID.randomUUID().toString());
        
        producer.produceMsgIdempotent(messageDTO);
        return Result.success("消息发送成功，Token: " + messageDTO.getIdempotentToken());
    }

    /**
     * 测试20：批量发送相同Token的消息（验证幂等性）
     * 连续发送3条相同Token的消息，只有第一条会被处理
     */
    @PostMapping("/mq/spel/batch")
    public Result<String> testMqSpelBatch() {
        log.info("=== 测试20：MQ SpEL表达式 - 批量发送相同Token ===");
        String token = UUID.randomUUID().toString();
        
        for (int i = 1; i <= 3; i++) {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setType("批量测试");
            messageDTO.setDesc("幂等性测试 - 第" + i + "条消息");
            messageDTO.setIdempotentToken(token);
            
            producer.produceMsgIdempotent(messageDTO);
            log.info("发送第 {} 条消息，Token: {}", i, token);
        }
        
        return Result.success("批量发送完成，Token: " + token + "（只有第一条会被处理）");
    }

    /**
     * 测试21：发送MQ消息 - 从消息头获取Token（第一次，成功）
     * 方式二：在消息头中设置Token
     */
    @PostMapping("/mq/header/first")
    public Result<String> testMqHeaderFirst() {
        log.info("=== 测试21：MQ消息头方式 - 第一次发送消息 ===");
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - 消息头方式");
        String token = UUID.randomUUID().toString();
        
        producer.produceMsgIdempotentHeader(messageDTO, token);
        return Result.success("消息发送成功，Token: " + token);
    }

    /**
     * 测试22：发送MQ消息 - 从消息头获取Token（重复，失败）
     * 发送相同Token的消息，消费者会拒绝处理
     */
    @PostMapping("/mq/header/repeat")
    public Result<String> testMqHeaderRepeat(@RequestParam(value = "token", required = false) String token) {
        log.info("=== 测试22：MQ消息头方式 - 重复发送消息，Token: {} ===", token);
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - 消息头方式重复消息");
        // 如果传入了token，使用传入的token，否则生成新的
        String finalToken = token != null ? token : UUID.randomUUID().toString();
        
        producer.produceMsgIdempotentHeader(messageDTO, finalToken);
        return Result.success("消息发送成功（如果Token重复，消费者会拒绝处理），Token: " + finalToken);
    }

    /**
     * 测试23：发送MQ消息 - 消息头Token为空（失败）
     * 不设置消息头中的Token
     */
    @PostMapping("/mq/header/empty")
    public Result<String> testMqHeaderEmpty() {
        log.info("=== 测试23：MQ消息头方式 - Token为空 ===");
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("测试消息");
        messageDTO.setDesc("幂等性测试 - 消息头方式Token为空");
        
        // 发送消息但不设置Token（传入null或空字符串）
        producer.produceMsgIdempotentHeader(messageDTO, null);
        return Result.success("消息发送成功（消费者会因为Token为空而拒绝处理）");
    }

    /**
     * 测试24：批量发送相同Token的消息（消息头方式，验证幂等性）
     * 连续发送3条相同Token的消息，只有第一条会被处理
     */
    @PostMapping("/mq/header/batch")
    public Result<String> testMqHeaderBatch() {
        log.info("=== 测试24：MQ消息头方式 - 批量发送相同Token ===");
        String token = UUID.randomUUID().toString();
        
        for (int i = 1; i <= 3; i++) {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setType("批量测试");
            messageDTO.setDesc("幂等性测试 - 第" + i + "条消息（消息头方式）");
            
            producer.produceMsgIdempotentHeader(messageDTO, token);
            log.info("发送第 {} 条消息，Token: {}", i, token);
        }
        
        return Result.success("批量发送完成，Token: " + token + "（只有第一条会被处理）");
    }

    /*=============================================    综合测试    =============================================*/

    /**
     * 测试25：综合测试 - 测试各种场景
     */
    @PostMapping("/comprehensive")
    public Result<String> testComprehensive() {
        log.info("=== 测试25：综合测试 - 各种场景 ===");
        StringBuilder result = new StringBuilder();
        result.append("综合测试结果：\n");
        
        // 1. 生成不同场景的Token
        String token1 = UUID.randomUUID().toString();
        String token2 = UUID.randomUUID().toString();
        
        result.append("1. Token生成测试：\n");
        result.append("   Token1: ").append(token1).append("\n");
        result.append("   Token2: ").append(token2).append("\n\n");
        
        // 2. 测试MQ消息
        result.append("2. MQ消息测试：\n");
        MessageDTO message1 = new MessageDTO();
        message1.setType("综合测试");
        message1.setDesc("第一次消息");
        message1.setIdempotentToken(token1);
        producer.produceMsgIdempotent(message1);
        result.append("   发送消息1，Token: ").append(token1).append("\n");
        
        // 等待一下，然后发送相同Token的消息
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        MessageDTO message2 = new MessageDTO();
        message2.setType("综合测试");
        message2.setDesc("重复消息（相同Token）");
        message2.setIdempotentToken(token1);
        producer.produceMsgIdempotent(message2);
        result.append("   发送消息2（相同Token），应该被拒绝\n");
        
        MessageDTO message3 = new MessageDTO();
        message3.setType("综合测试");
        message3.setDesc("新消息（不同Token）");
        message3.setIdempotentToken(token2);
        producer.produceMsgIdempotent(message3);
        result.append("   发送消息3（不同Token），应该被处理\n");
        
        result.append("\n综合测试完成，请查看日志和消费者处理结果\n");
        
        return Result.success(result.toString());
    }

    /**
     * 测试26：获取当前测试说明
     */
    @GetMapping("/help")
    public Result<String> getTestHelp() {
        StringBuilder help = new StringBuilder();
        help.append("============================ 幂等性功能测试说明 ============================\n\n");
        help.append("【HTTP接口测试】\n");
        help.append("1. POST /test/idempotent/http/header/first - 请求头第一次请求（成功）\n");
        help.append("   请求头：Idempotent-Token: test-token-001\n\n");
        help.append("2. POST /test/idempotent/http/header/repeat - 请求头重复请求（失败）\n");
        help.append("   请求头：Idempotent-Token: test-token-002（连续调用两次）\n\n");
        help.append("3. POST /test/idempotent/http/header/empty - 请求头Token为空（失败）\n");
        help.append("   不传请求头或传空值\n\n");
        help.append("4. POST /test/idempotent/http/param/first?idempotentToken=xxx - 请求参数第一次（成功）\n\n");
        help.append("5. POST /test/idempotent/http/param/repeat?idempotentToken=xxx - 请求参数重复（失败）\n\n");
        help.append("6. POST /test/idempotent/http/param/empty - 请求参数Token为空（失败）\n\n");
        help.append("7. POST /test/idempotent/http/priority/header - 优先级测试（请求头优先）\n");
        help.append("   请求头：Idempotent-Token: header-token-001\n");
        help.append("   请求参数：idempotentToken=param-token-001\n\n");
        help.append("8. POST /test/idempotent/http/failure/retry?shouldFail=true - 业务失败允许重试\n\n");
        help.append("9. POST /test/idempotent/http/custom/expire - 自定义过期时间（10秒）\n\n");
        help.append("10. POST /test/idempotent/http/custom/message - 自定义错误提示\n\n");
        help.append("11. POST /test/idempotent/http/custom/header - 自定义请求头名称\n");
        help.append("    请求头：X-Idempotency-Key: custom-token-001\n\n");
        help.append("12. POST /test/idempotent/http/service/method - Service层方法幂等性测试\n\n");
        help.append("13. POST /test/idempotent/http/strong/first - HTTP强幂等模式第一次请求（成功，结果缓存）\n");
        help.append("    请求头：Idempotent-Token: strong-token-001\n");
        help.append("    开启 returnCachedResult=true，重复请求会返回第一次的结果\n\n");
        help.append("14. POST /test/idempotent/http/strong/repeat - HTTP强幂等模式重复请求（成功，返回缓存结果）\n");
        help.append("    请求头：Idempotent-Token: strong-token-002（连续调用两次）\n");
        help.append("    第二次请求应该返回第一次的结果，而不是报错\n\n");
        help.append("【MQ消费者测试】\n");
        help.append("15. POST /test/idempotent/mq/spel/first - MQ第一次发送（成功）\n\n");
        help.append("16. POST /test/idempotent/mq/spel/repeat?token=xxx - MQ重复发送（失败）\n\n");
        help.append("17. POST /test/idempotent/mq/spel/empty - MQ Token为空（失败）\n\n");
        help.append("18. POST /test/idempotent/mq/spel/failure - MQ业务失败允许重试\n\n");
        help.append("19. POST /test/idempotent/mq/spel/expire - MQ自定义过期时间\n\n");
        help.append("20. POST /test/idempotent/mq/spel/batch - MQ批量发送相同Token（SpEL表达式）\n\n");
        help.append("21. POST /test/idempotent/mq/header/first - MQ消息头方式第一次发送（成功）\n\n");
        help.append("22. POST /test/idempotent/mq/header/repeat?token=xxx - MQ消息头方式重复发送（失败）\n\n");
        help.append("23. POST /test/idempotent/mq/header/empty - MQ消息头方式Token为空（失败）\n\n");
        help.append("24. POST /test/idempotent/mq/header/batch - MQ消息头方式批量发送相同Token\n\n");
        help.append("【综合测试】\n");
        help.append("25. POST /test/idempotent/comprehensive - 综合测试各种场景\n\n");
        help.append("26. GET /test/idempotent/help - 获取测试说明（本接口）\n\n");
        help.append("======================================================================\n");
        
        return Result.success(help.toString());
    }
}
