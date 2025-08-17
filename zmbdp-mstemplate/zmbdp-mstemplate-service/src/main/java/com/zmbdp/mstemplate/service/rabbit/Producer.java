package com.zmbdp.mstemplate.service.rabbit;

import com.zmbdp.mstemplate.service.domain.MessageDTO;
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
}