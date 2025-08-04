package com.zmbdp.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class ZmbdpAdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZmbdpAdminServiceApplication.class, args);
    }
}
