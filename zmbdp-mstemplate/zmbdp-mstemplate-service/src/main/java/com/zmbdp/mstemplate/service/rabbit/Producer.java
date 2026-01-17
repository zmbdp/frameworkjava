package com.zmbdp.mstemplate.service.rabbit;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.mstemplate.service.domain.MessageDTO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 测试消息发送者
 *
 * @author 稚名不带撇
 */
@Component
public class Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到 testQueue 队列
     * @param messageDTO 消息 DTO
     */
    public void produceMsg(MessageDTO messageDTO) {
        rabbitTemplate.convertAndSend("testQueue", messageDTO);
    }

    /**
     * 发送消息到 testQueueIdempotent 队列（幂等性测试队列）
     * @param messageDTO 消息 DTO
     */
    public void produceMsgIdempotent(MessageDTO messageDTO) {
        rabbitTemplate.convertAndSend("testQueueIdempotent", messageDTO);
    }

    /**
     * 发送消息到 testQueueIdempotentFailure 队列（幂等性失败测试队列）
     * @param messageDTO 消息 DTO
     */
    public void produceMsgIdempotentFailure(MessageDTO messageDTO) {
        rabbitTemplate.convertAndSend("testQueueIdempotentFailure", messageDTO);
    }

    /**
     * 发送消息到 testQueueIdempotentHeader 队列（消息头方式幂等性测试队列）
     * 在消息头中设置 Token
     * @param messageDTO 消息 DTO
     * @param idempotentToken 幂等性Token（放入消息头）
     */
    public void produceMsgIdempotentHeader(MessageDTO messageDTO, String idempotentToken) {
        // 创建消息属性
        MessageProperties properties = new MessageProperties();
        properties.setHeader("Idempotent-Token", idempotentToken);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        
        // 将对象转为字节数组
        byte[] body = JsonUtil.classToJson(messageDTO).getBytes();
        
        // 创建消息
        Message message = new Message(body, properties);
        
        // 发送消息
        rabbitTemplate.send("testQueueIdempotentHeader", message);
    }
}