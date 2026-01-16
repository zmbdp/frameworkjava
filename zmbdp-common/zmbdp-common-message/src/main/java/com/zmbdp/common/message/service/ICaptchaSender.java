package com.zmbdp.common.message.service;

/**
 * 验证码发送服务
 *
 * @author 稚名不带撇
 */
public interface ICaptchaSender {

    /**
     * 发送手机验证码
     *
     * @param account 手机号 / 邮箱
     * @param code    验证码
     * @return 是否发送成功
     */
    boolean sendMobileCode(String account, String code);
}
