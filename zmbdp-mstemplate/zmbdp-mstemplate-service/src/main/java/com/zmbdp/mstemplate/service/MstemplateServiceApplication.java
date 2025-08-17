package com.zmbdp.mstemplate.service;


import com.zmbdp.admin.api.config.frign.ArgumentServiceApi;
import com.zmbdp.admin.api.config.frign.DictionaryServiceApi;
import com.zmbdp.admin.api.map.feign.MapServiceApi;
import com.zmbdp.file.api.feign.FileServiceApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ，模板微服务启动类
 *
 * @author 稚名不带撇
 */
@Slf4j
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class
}) // 禁用数据源自动配置
@EnableFeignClients(clients = {
        FileServiceApi.class, MapServiceApi.class,
        DictionaryServiceApi.class, ArgumentServiceApi.class
}) // 告诉 SpringCloud 这个类需要调用 FileServiceApi 服务
public class MstemplateServiceApplication {
    /**
     * 启动方法
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MstemplateServiceApplication.class, args);
    }
}
