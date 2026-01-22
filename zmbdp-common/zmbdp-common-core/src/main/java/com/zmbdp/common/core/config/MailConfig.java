package com.zmbdp.common.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件配置，使用 Spring JavaMailSender<br>
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
     * 注册 JavaMailSender Bean
     *
     * @return JavaMailSender 邮件发送器实例
     */
    @Bean
    @ConditionalOnProperty(value = "mail.isEnabled", havingValue = "true")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(user);
        mailSender.setPassword(pass);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (sslEnable != null && sslEnable) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "jakarta.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
        }
        props.put("mail.mime.charset", "UTF-8");
        return mailSender;
    }

    /**
     * 注册 MailAccount Bean（用于兼容性）
     *
     * @return MailAccount 邮箱配置实例
     */
    @Bean
    @ConditionalOnProperty(value = "mail.isEnabled", havingValue = "true")
    public MailAccount mailAccount() {
        MailAccount account = new MailAccount();
        account.setFrom(from);
        account.setUser(user);
        account.setPass(pass);
        account.setHost(host);
        account.setPort(port);
        account.setSslEnable(sslEnable);
        return account;
    }
}