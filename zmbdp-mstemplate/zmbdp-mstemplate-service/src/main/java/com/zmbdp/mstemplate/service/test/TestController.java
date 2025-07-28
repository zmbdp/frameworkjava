package com.zmbdp.mstemplate.service.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {
    // /test/info
    @GetMapping("/info")
    public void info() {
        log.info("接口调用测试");
    }
}