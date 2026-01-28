package com.zmbdp.common.message.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件验证码配置属性
 *
 * @author 稚名不带撇
 */
@Data
@Slf4j
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mail.code")
public class MailCodeProperties {

    /**
     * 邮件标题列表（支持 YAML 列表格式）
     */
    private List<String> subject = new ArrayList<>();

    /**
     * 邮件内容模板列表（支持 {code} 占位符，支持 YAML 列表格式）
     */
    private List<String> content = new ArrayList<>();

    /**
     * 初始化后检查配置是否加载成功
     */
    @PostConstruct
    public void init() {
        log.info("邮件验证码配置加载 - subject数量: {}, content数量: {}",
                subject != null ? subject.size() : 0,
                content != null ? content.size() : 0);
        if (subject != null && !subject.isEmpty()) {
            log.info("邮件标题列表: {}", subject);
        }
        if (content != null && !content.isEmpty()) {
            log.info("邮件内容模板数量: {}", content.size());
        }
    }
}