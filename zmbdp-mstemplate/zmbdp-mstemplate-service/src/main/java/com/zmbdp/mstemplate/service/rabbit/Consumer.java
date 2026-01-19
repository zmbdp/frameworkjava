package com.zmbdp.mstemplate.service.rabbit;

import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.idempotent.annotation.Idempotent;
import com.zmbdp.mstemplate.service.domain.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * MQ消息消费者
 * 用于测试MQ消费者的幂等性功能
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class Consumer {

    /**
     * 普通消费者（不使用幂等性）
     * 用于对比测试
     */
    @RabbitListener(queuesToDeclare = @Queue("testQueue"))
    public void listenerQueue(MessageDTO messageDTO) {
        log.info("收到消息为: {}", messageDTO);
    }

    /**
     * 幂等性消费者（从消息对象获取Token）
     * 使用SpEL表达式从MessageDTO.idempotentToken获取Token
     */
    @RabbitListener(queuesToDeclare = @Queue("testQueueIdempotent"))
    @Idempotent(
            tokenExpression = "#messageDTO.idempotentToken",
            expireTime = 300,
            message = "消息重复消费"
    )
    public void listenerQueueIdempotent(MessageDTO messageDTO) {
        log.info("=== MQ幂等性消费者 - 收到消息: {} ===", messageDTO);
        // 模拟业务处理
        log.info("业务处理完成：type={}, desc={}, token={}",
                messageDTO.getType(), messageDTO.getDesc(), messageDTO.getIdempotentToken());
    }

    /**
     * 幂等性消费者 - 测试业务失败场景
     * 业务失败时Token会被删除，允许重试
     */
    @RabbitListener(queuesToDeclare = @Queue("testQueueIdempotentFailure"))
    @Idempotent(
            tokenExpression = "#messageDTO.idempotentToken",
            expireTime = 300,
            message = "消息重复消费"
    )
    public void listenerQueueIdempotentFailure(MessageDTO messageDTO) {
        log.info("=== MQ幂等性消费者（失败测试） - 收到消息: {} ===", messageDTO);

        // 模拟业务失败：如果消息类型是"测试失败"，则抛出异常
        if ("测试失败".equals(messageDTO.getType())) {
            log.error("模拟业务失败，消息: {}", messageDTO);
            throw new ServiceException("模拟业务异常，Token会被删除，允许重试", ResultCode.ERROR.getCode());
        }

        log.info("业务处理成功：type={}, desc={}, token={}",
                messageDTO.getType(), messageDTO.getDesc(), messageDTO.getIdempotentToken());
    }

    /**
     * 幂等性消费者 - 从消息头获取Token（方式二）
     * 使用消息头方式传递Token
     * 注意：需要添加 org.springframework.amqp.core.Message 参数以便从消息头获取Token
     */
    @RabbitListener(queuesToDeclare = @Queue("testQueueIdempotentHeader"))
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "消息重复消费"
    )
    public void listenerQueueIdempotentHeader(MessageDTO messageDTO, org.springframework.amqp.core.Message message) {
        log.info("=== MQ幂等性消费者（消息头方式） - 收到消息: {} ===", messageDTO);
        // 模拟业务处理
        log.info("业务处理完成：type={}, desc={}", messageDTO.getType(), messageDTO.getDesc());
    }
}
