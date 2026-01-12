package com.zmbdp.common.core.config;

import lombok.Data;

import java.util.Properties;

/**
 * 邮件账号配置类（兼容 Hutool MailAccount API）<br>
 * 用于配置 SMTP 服务器、用户名、授权码等信息
 *
 * @author 稚名不带撇
 */
@Data
public class MailAccount {
    /**
     * 发件人邮箱地址
     */
    private String from;

    /**
     * 登录用户名（一般和 from 一样）
     */
    private String user;

    /**
     * 邮箱授权码 / 密码
     */
    private String pass;

    /**
     * SMTP 服务器地址
     */
    private String host;

    /**
     * SMTP 端口（465 一般是 SSL）
     */
    private Integer port;

    /**
     * 是否开启 SSL
     */
    private Boolean sslEnable;

    /**
     * 是否需要认证
     */
    private Boolean auth = true;

    /**
     * 判断是否需要认证
     *
     * @return true 表示需要认证，false 表示不需要
     */
    public boolean isAuth() {
        return auth != null && auth;
    }

    /**
     * 获取 SMTP 配置属性
     *
     * @return Properties SMTP 配置属性
     */
    public Properties getSmtpProps() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(auth));
        if (sslEnable != null && sslEnable) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "jakarta.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
        }
        props.put("mail.mime.charset", "UTF-8");
        return props;
    }
}
