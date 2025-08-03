package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.mstemplate.service.domain.MessageDTO;
import com.zmbdp.mstemplate.service.rabbit.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/test/rabbit")
public class TestRabbitController {

    @Autowired
    private Producer producer;

    @PostMapping("/send")
    public Result<Void> send() {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setType("系统");
        messageDTO.setDesc("请您尽快完成系统升级");
        producer.produceMsg(messageDTO);
        return Result.success();
    }


}
