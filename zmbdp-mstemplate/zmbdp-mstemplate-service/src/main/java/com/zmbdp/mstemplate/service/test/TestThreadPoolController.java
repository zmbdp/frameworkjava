package com.zmbdp.mstemplate.service.test;

import com.zmbdp.mstemplate.service.service.ThreadPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 线程池测试控制器
 * 测试异步线程池功能
 *
 * @author 稚名不带撇
 */
@Slf4j
@RefreshScope
@RestController
@RequestMapping("/test/thread")
public class TestThreadPoolController {

    @Autowired
    private ThreadPoolService threadPoolService;

    /**
     * 测试异步线程池
     */
    @GetMapping("/info")
    public void info() {
        log.info("TestThreadPoolController thread name: {}", Thread.currentThread().getName());
        threadPoolService.info();
    }
}