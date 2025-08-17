package com.zmbdp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 白名单配置
 *
 * @author 稚名不带撇
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties {

    /**
     * 放行白名单
     */
    private List<String> whites;
}