package com.zmbdp.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * admin 服务启动类
 *
 * @author 稚名不带撇
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class ZmbdpAdminServiceApplication {

    /**
     * 启动方法
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ZmbdpAdminServiceApplication.class, args);
        log.info("admin 服务启动成功......");
    }
}
