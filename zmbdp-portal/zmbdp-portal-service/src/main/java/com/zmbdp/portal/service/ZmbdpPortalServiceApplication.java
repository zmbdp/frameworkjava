package com.zmbdp.portal.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = {"com.zmbdp.**.feign"})
public class ZmbdpPortalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZmbdpPortalServiceApplication.class, args);
    }
}
