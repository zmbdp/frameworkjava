package com.zmbdp.common.core.config;

import cn.hutool.extra.mail.MailAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置，用 Hutool 的 MailAccount<br>
 * 配置项放 nacos 或 yml：<br>
 * mail.from, mail.user, mail.pass, mail.host, mail.port, mail.ssl-enable<br>
 *
 * @author 稚名不带撇
 */
@RefreshScope
@Configuration
public class MailConfig {

    /**
     * 发件人邮箱（from）
     */
    @Value("${mail.from:}")
    private String from;

    /**
     * 登录用户名（一般和 from 一样）
     */
    @Value("${mail.user:}")
    private String user;

    /**
     * 邮箱授权码 / 密码
     */
    @Value("${mail.pass:}")
    private String pass;

    /**
     * SMTP 服务器地址
     */
    @Value("${mail.host:smtp.163.com}")
    private String host;

    /**
     * SMTP 端口（465 一般是 SSL）
     */
    @Value("${mail.port:465}")
    private Integer port;

    /**
     * 是否开启 SSL
     */
    @Value("${mail.ssl-enable:true}")
    private Boolean sslEnable;

    /**
     * 注册 Hutool 的 MailAccount Bean
     *
     * @return MailAccount 邮箱实例 Bean
     */
    @Bean
    @ConditionalOnProperty(value = "mail.isEnabled", havingValue = "true")
    public MailAccount mailAccount() {
        MailAccount account = new MailAccount();
        account.setFrom(from); // 发件人邮箱
        account.setUser(user); // 登录用户名
        account.setPass(pass); // 授权码
        account.setHost(host); // SMTP 服务器地址
        account.setPort(port); // SMTP 端口
        account.setSslEnable(sslEnable);
        return account;
    }
}