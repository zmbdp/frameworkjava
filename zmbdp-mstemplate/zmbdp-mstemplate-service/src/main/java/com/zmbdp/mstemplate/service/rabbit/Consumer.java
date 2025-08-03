package com.zmbdp.mstemplate.service.rabbit;

import com.zmbdp.mstemplate.service.domain.MessageDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    @RabbitListener(queues = "testQueue")
    public void listenerQueue(MessageDTO messageDTO){
        System.out.println("收到消息为: " + messageDTO);
        System.out.println(MessageDTO.class);
    }
}