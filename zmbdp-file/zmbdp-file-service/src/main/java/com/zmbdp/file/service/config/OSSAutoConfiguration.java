package com.zmbdp.file.service.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyuncs.exceptions.ClientException;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 存储配置类
 *
 * @author 稚名不带撇
 */
@Configuration
@ConditionalOnProperty(value = "storage.type", havingValue = "oss")
// 当括号中条件成立时，微服务启动的时候就会加载当前类。
public class OSSAutoConfiguration {

    /**
     * oss客户端
     */
    public OSSClient ossClient;

    /**
     * 初始化客户端
     *
     * @param prop oss配置
     * @return OSSClient 实例
     * @throws ClientException 客户端异常
     */
    @Bean
    public OSSClient ossClient(OSSProperties prop) throws ClientException {
        // 用阿里云提供的 CredentialsProviderFactory 创建认证凭证
        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(
                prop.getAccessKeyId(), prop.getAccessKeySecret()); // 读取配置文件中的 accessKeyId 和 accessKeySecret

        // 创建 ClientBuilderConfiguration, 配置 OSS 客户端的网络和签名参数
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V4);

        // 根据用户在 nacos 上的选择，看看是内网上传还是外网
        if (prop.getInternal()) {
            ossClient = (OSSClient) OSSClientBuilder.create()
                    .endpoint(prop.getIntEndpoint())
                    .region(prop.getRegion())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(conf)
                    .build();
        } else {
            ossClient = (OSSClient) OSSClientBuilder.create()
                    .endpoint(prop.getEndpoint())
                    .region(prop.getRegion())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(conf)
                    .build();
        }
        // 返回 OSSClient 实例
        return ossClient;
    }

    /**
     * 关闭客户端
     *
     * <p>@PreDestroy: 表示下面的方法 spring 容器销毁时执行
     */
    @PreDestroy
    public void closeOSSClient() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
