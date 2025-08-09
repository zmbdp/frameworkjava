package com.zmbdp.file.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        RabbitAutoConfiguration.class
}) // 禁用数据源自动配置
public class ZmbdpFileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZmbdpFileServiceApplication.class, args);
    }
}
