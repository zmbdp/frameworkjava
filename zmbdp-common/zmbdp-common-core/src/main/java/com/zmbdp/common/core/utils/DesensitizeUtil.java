package com.zmbdp.common.core.utils;

import com.zmbdp.common.domain.constants.LogConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 * <p>
 * 提供敏感数据的脱敏处理功能，支持手机号、身份证号、邮箱、密码、银行卡号等常见敏感字段。
 * <p>
 * <b>支持的脱敏类型：</b>
 * <ul>
 *     <li><b>手机号</b>：保留前3位和后4位，中间用*替换（如：138****5678）</li>
 *     <li><b>身份证号</b>：保留前6位和后4位，中间用*替换（如：110101********1234）</li>
 *     <li><b>邮箱</b>：保留@前3位和@后全部，中间用*替换（如：abc***@example.com）</li>
 *     <li><b>密码</b>：全部替换为*（如：********）</li>
 *     <li><b>银行卡号</b>：保留前4位和后4位，中间用*替换（如：6222****1234）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DesensitizeUtil {

    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 身份证号正则表达式（18位或15位）
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}|\\d{17}[\\dXx]$");

    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 银行卡号正则表达式（16-19位数字）
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");

    /**
     * 脱敏手机号
     * <p>
     * 保留前3位和后4位，中间用*替换。
     * <p>
     * <b>示例：</b>
     * <pre>
     * 13812345678 -> 138****5678
     * </pre>
     *
     * @param phone 手机号
     * @return 脱敏后的手机号，如果输入为空或格式不正确，返回原值
     */
    public static String desensitizePhone(String phone) {
        if (StringUtil.isEmpty(phone)) {
            return phone;
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone; // 格式不正确，不脱敏
        }
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return phone;
    }

    /**
     * 脱敏身份证号
     * <p>
     * 保留前6位和后4位，中间用*替换。
     * <p>
     * <b>示例：</b>
     * <pre>
     * 110101199001011234 -> 110101********1234
     * </pre>
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号，如果输入为空或格式不正确，返回原值
     */
    public static String desensitizeIdCard(String idCard) {
        if (StringUtil.isEmpty(idCard)) {
            return idCard;
        }
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard; // 格式不正确，不脱敏
        }
        int length = idCard.length();
        if (length == 15) {
            return idCard.substring(0, 6) + "*****" + idCard.substring(11);
        } else if (length == 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        }
        return idCard;
    }

    /**
     * 脱敏邮箱
     * <p>
     * 保留@前3位和@后全部，中间用*替换。
     * <p>
     * <b>示例：</b>
     * <pre>
     * abcdef@example.com -> abc***@example.com
     * </pre>
     *
     * @param email 邮箱地址
     * @return 脱敏后的邮箱，如果输入为空或格式不正确，返回原值
     */
    public static String desensitizeEmail(String email) {
        if (StringUtil.isEmpty(email)) {
            return email;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email; // 格式不正确，不脱敏
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        String prefix = email.substring(0, Math.min(3, atIndex));
        String suffix = email.substring(atIndex);
        return prefix + "***" + suffix;
    }

    /**
     * 脱敏密码
     * <p>
     * 全部替换为*，长度保持原密码长度。
     * <p>
     * <b>示例：</b>
     * <pre>
     * password123 -> ***********
     * </pre>
     *
     * @param password 密码
     * @return 脱敏后的密码（全部为*），如果输入为空，返回空字符串
     */
    public static String desensitizePassword(String password) {
        if (StringUtil.isEmpty(password)) {
            return "";
        }
        return "*".repeat(password.length());
    }

    /**
     * 脱敏银行卡号
     * <p>
     * 保留前4位和后4位，中间用*替换。
     * <p>
     * <b>示例：</b>
     * <pre>
     * 6222021234567890123 -> 6222****0123
     * </pre>
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号，如果输入为空或格式不正确，返回原值
     */
    public static String desensitizeBankCard(String bankCard) {
        if (StringUtil.isEmpty(bankCard)) {
            return bankCard;
        }
        if (!BANK_CARD_PATTERN.matcher(bankCard).matches()) {
            return bankCard; // 格式不正确，不脱敏
        }
        int length = bankCard.length();
        if (length >= 8) {
            return bankCard.substring(0, 4) + "****" + bankCard.substring(length - 4);
        }
        return bankCard;
    }

    /**
     * 根据脱敏类型脱敏数据
     * <p>
     * 根据指定的脱敏类型（如 {@code phone}、{@code idCard} 等）对数据进行脱敏处理。
     *
     * @param value 原始数据
     * @param type  脱敏类型（{@link LogConstants} 中定义的常量）
     * @return 脱敏后的数据
     */
    public static String desensitize(String value, String type) {
        if (StringUtil.isEmpty(value) || StringUtil.isEmpty(type)) {
            return value;
        }
        return switch (type.toLowerCase()) {
            case LogConstants.DESENSITIZE_PHONE -> desensitizePhone(value);
            case LogConstants.DESENSITIZE_ID_CARD -> desensitizeIdCard(value);
            case LogConstants.DESENSITIZE_EMAIL -> desensitizeEmail(value);
            case LogConstants.DESENSITIZE_PASSWORD -> desensitizePassword(value);
            case LogConstants.DESENSITIZE_BANK_CARD -> desensitizeBankCard(value);
            default -> value; // 未知类型，不脱敏
        };
    }
}
