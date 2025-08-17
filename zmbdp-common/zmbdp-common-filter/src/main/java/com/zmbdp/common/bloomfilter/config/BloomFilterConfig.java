package com.zmbdp.common.bloomfilter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 *
 * @author 稚名不带撇
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "bloom.filter")
public class BloomFilterConfig {

    /**
     * 预期插入的元素数量
     */
    private int expectedInsertions = 1000;

    /**
     * 误判率
     */
    private double falseProbability = 0.01;

    /**
     * 布隆过滤器阈值
     */
    private double warningThreshold = 0.7;
}
