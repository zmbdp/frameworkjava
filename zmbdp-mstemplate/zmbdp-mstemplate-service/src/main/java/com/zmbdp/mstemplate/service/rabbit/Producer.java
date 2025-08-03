package com.zmbdp.mstemplate.service.rabbit;

import com.zmbdp.mstemplate.service.domain.MessageDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class Producer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 生产者发送到默认交换机
    public void produceMsg(MessageDTO messageDTO) {
        rabbitTemplate.convertAndSend("testQueue", messageDTO);
    }
}