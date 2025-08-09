package com.zmbdp.common.core.utils;

import cn.hutool.crypto.SecureUtil;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * AES 加密工具类
 *
 * @author 稚名不带撇
 */
public class AESUtil {

    /**
     * 前后端规定的秘钥
     */
    private static final byte[] KEYS = "12345678abcdefgh".getBytes(StandardCharsets.UTF_8);

    /**
     * aes 加密
     *
     * @param data 原始数据
     * @return 加密后的数据
     */
    public static String encryptHex(String data) {
        if (StringUtils.isNotEmpty(data)) {
            return SecureUtil.aes(KEYS).encryptHex(data);
        }
        return null;
    }

    /**
     * aes 解密
     *
     * @param data 加密后的数据
     * @return 原始数据
     */
    public static String decryptHex(String data) {
        if (StringUtils.isNotEmpty(data)) {
            return SecureUtil.aes(KEYS).decryptStr(data);
        }
        return null;
    }
}