package com.zmbdp.file.service.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置信息，从 nacos 读取
 *
 * @author 稚名不带撇
 */
@Slf4j
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "oss")
@ConditionalOnProperty(value = "storage.type", havingValue = "oss")
// 当括号中条件成立时，微服务启动的时候就会加载当前类。
public class OSSProperties  {

    /**
     * oss是否内网上传
     */
    private Boolean internal;

    /**
     * oss 的 endpoint
     */
    private String endpoint;

    /**
     * oss 的 endpoint 的内部地址
     */
    private String intEndpoint;

    /**
     * 地域的代码
     */
    private String region;

    /**
     * ak
     */
    private String accessKeyId;

    /**
     * sk
     */
    private String accessKeySecret;

    /**
     *存储桶
     */
    private String bucketName;

    /**
     * 路径前缀，加在 endPoint 之后
     */
    private String pathPrefix;

    /**
     * 签名过期时间，单位秒
     */
    private Integer expre;

    /**
     * 文件名最小长度
     */
    private Integer minLen;

    /**
     * 文件名最大长度
     */
    private Integer maxLen;


    /**
     * 获取外网访问的 URL
     *
     * @return 外网访问的 URL 信息
     */
    public String getBaseUrl() {
        return "https://" + bucketName + "." + endpoint + "/";
    }

    /**
     * 获取内网访问的 URL
     *
     * @return 内网访问的 URL 信息
     */
    public String getInternalBaseUrl() {
        return "http://" + bucketName + "." + intEndpoint + "/";
    }

}
