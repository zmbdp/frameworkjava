package com.zmbdp.common.message.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信服务配置参数
 *
 * @author 稚名不带撇
 */
@RefreshScope
@Configuration
public class AliSmsConfig {

    /**
     * accessKeyId
     */
    @Value("${sms.aliyun.accessKeyId:}")
    private String accessKeyId;

    /**
     * accessKeySecret
     */
    @Value("${sms.aliyun.accessKeySecret:}")
    private String accessKeySecret;

    /**
     * 服务器地址
     */
    @Value("${sms.aliyun.endpoint:}")
    private String endpoint;

    /**
     * 注册阿里云短信服务的客户端
     *
     * @return 发送短信请求的客户端
     * @throws Exception 创建客户端异常
     */
    @Bean("aliClient")
    public Client client() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }
}
