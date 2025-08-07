package com.zmbdp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
}) // 禁用数据源自动配置
public class ZmbdpGatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZmbdpGatewayServiceApplication.class, args);
    }

}