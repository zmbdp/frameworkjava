package com.zmbdp.common.core.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用校验工具类
 *
 * @author 稚名不带撇
 */
public class VerifyUtil {

    /**
     * 手机号的正则校验
     */
    public static final Pattern PHONE_PATTERN = Pattern.compile("^1[2|3|4|5|6|7|8|9][0-9]\\d{8}$");

    /**
     * 纯数字验证码
     */
    public static final String NUMBER_VERIFY_CODES = "1234567890";
    /**
     * 数字 + 字母 验证码
     */
    public static final String ALPHABET_VERIFY_CODES = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    /**
     * 字母 + 数字 + 特殊字符 验证码
     */
    public static final String COMPLEX_VERIFY_CODES = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*?";

    /**
     * 手机号校验
     *
     * @param phone 手机号 11 位，以 1 开头  第二位是 2-9
     * @return 成功: true; 失败: false
     */
    public static boolean checkPhone(String phone) {
        Matcher m = PHONE_PATTERN.matcher(phone);
        return m.matches();
    }

    /**
     * 随机生成验证码
     *
     * @param size 验证码长度
     * @param type 验证码类型 (1: 纯数字; 2: 字母 + 数字; 3: 字符 + 字母 + 数字)
     * @return 验证码
     */
    public static String generateVerifyCode(int size, int type) {
        // 选择验证码种类
        String sources = switch (type) {
            case 1 -> NUMBER_VERIFY_CODES;
            case 2 -> ALPHABET_VERIFY_CODES;
            default -> COMPLEX_VERIFY_CODES;
        };
        // 获取一个线程本地的随机数生成器实例
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        StringBuilder verifyCode = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            verifyCode.append(sources.charAt(rand.nextInt(sources.length())));
        }
        return verifyCode.toString();
    }
}
