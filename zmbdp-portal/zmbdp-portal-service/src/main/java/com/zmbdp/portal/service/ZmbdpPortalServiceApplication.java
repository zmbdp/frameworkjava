package com.zmbdp.portal.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 门户服务启动类
 *
 * @author 稚名不带撇
 */
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.zmbdp.**.feign"})
public class ZmbdpPortalServiceApplication {

    /**
     * 启动方法
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ZmbdpPortalServiceApplication.class, args);
    }
}
