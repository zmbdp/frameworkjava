package com.zmbdp.mstemplate.service.test;

import com.zmbdp.mstemplate.service.service.ThreadPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("/test/thread")
public class TestThreadPoolController {

    @Autowired
    private ThreadPoolService threadPoolService;

    @GetMapping("/info")
    public void info() {
        log.info("TestThreadPoolController thread name: {}", Thread.currentThread().getName());
        threadPoolService.info();
    }
}
