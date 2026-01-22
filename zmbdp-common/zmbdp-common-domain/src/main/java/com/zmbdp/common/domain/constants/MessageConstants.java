package com.zmbdp.common.domain.constants;

/**
 * 发送验证码的常量
 *
 * @author 稚名不带撇
 */
public class MessageConstants {

    /**
     * 发送成功的响应码
     */
    public static final String CAPTCHA_MSG_OK = "OK";

    /**
     * 验证码发送次数的 key
     */
    public static final String CAPTCHA_CODE_TIMES_KEY = "captcha:times:";

    /**
     * 验证码频繁发送的 key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha:code:";

    /**
     * 默认验证码的长度
     */
    public static final int DEFAULT_CAPTCHA_LENGTH = 6;

    /**
     * 默认验证码
     */
    public static final String DEFAULT_CAPTCHA_CODE = "123456";
}