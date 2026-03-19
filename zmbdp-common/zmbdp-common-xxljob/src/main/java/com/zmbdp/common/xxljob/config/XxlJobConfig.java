package com.zmbdp.common.xxljob.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器自动配置
 * <p>
 * 通过 Nacos 统一下发 xxl.job.* 配置，各服务无需重复编写执行器初始化代码。
 * 只需在 pom.xml 中引入 zmbdp-common-xxljob 依赖，并在 Nacos 共享配置中
 * 加载 share-xxljob-{env}.yaml 即可完成执行器注册。
 * </p>
 *
 * <p>配置项说明：</p>
 * <pre>
 * xxl:
 *   job:
 *     admin:
 *       addresses: http://xxl-job-admin:8080/xxl-job-admin
 *     executor:
 *       appname: ${spring.application.name}-executor
 *       address:
 *       ip:
 *       port: -1
 *       accessToken: frameworkjava_dev
 *       logpath: /data/applogs/xxl-job/executor
 *       logretentiondays: 30
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "xxl.job", name = "admin.addresses")
public class XxlJobConfig {

    /**
     * 调度中心地址（多地址逗号分隔，支持集群）
     */
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    /**
     * 执行器通讯 Token（与调度中心保持一致）
     */
    @Value("${xxl.job.executor.accessToken:}")
    private String accessToken;

    /**
     * 执行器 AppName（唯一标识，调度中心按此匹配执行器）
     */
    @Value("${xxl.job.executor.appname:${spring.application.name}-executor}")
    private String appname;

    /**
     * 执行器注册地址（优先级高于 IP + Port，为空则自动注册）
     */
    @Value("${xxl.job.executor.address:}")
    private String address;

    /**
     * 执行器 IP（为空则自动获取）
     */
    @Value("${xxl.job.executor.ip:}")
    private String ip;

    /**
     * 执行器端口（-1 则随机可用端口）
     */
    @Value("${xxl.job.executor.port:-1}")
    private int port;

    /**
     * 执行器运行日志文件存储路径
     */
    @Value("${xxl.job.executor.logpath:/data/applogs/xxl-job/executor}")
    private String logPath;

    /**
     * 执行器日志保留天数（-1 永不清理）
     */
    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    /**
     * 注册 XXL-Job Spring 执行器 Bean
     * <p>
     * {@link ConditionalOnMissingBean} 保证业务服务可覆盖此默认配置。
     * </p>
     *
     * @return XxlJobSpringExecutor
     */
    @Bean
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        log.info("[XXL-Job] 执行器初始化：appname = {}, adminAddresses = {}, port = {}", appname, adminAddresses, port);
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return xxlJobSpringExecutor;
    }
}
