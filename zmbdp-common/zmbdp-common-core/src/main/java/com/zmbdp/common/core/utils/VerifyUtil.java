package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用校验工具类
 * <p>
 * 提供常用的数据校验功能，包括手机号校验、邮箱校验、验证码生成等。<br>
 * 使用正则表达式进行格式校验，使用线程安全的随机数生成器生成验证码。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>手机号格式校验（中国大陆手机号）</li>
 *     <li>邮箱格式校验</li>
 *     <li>验证码生成（纯数字、字母+数字、复杂字符）</li>
 *     <li>线程安全的随机数生成</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 手机号校验
 * boolean valid = VerifyUtil.checkPhone("13800138000"); // true
 * boolean invalid = VerifyUtil.checkPhone("12345678901"); // false
 *
 * // 邮箱校验
 * boolean validEmail = VerifyUtil.checkEmail("user@example.com"); // true
 * boolean invalidEmail = VerifyUtil.checkEmail("invalid-email"); // false
 *
 * // 生成验证码
 * String code1 = VerifyUtil.generateVerifyCode(6, 1); // 6位纯数字
 * String code2 = VerifyUtil.generateVerifyCode(6, 2); // 6位字母+数字
 * String code3 = VerifyUtil.generateVerifyCode(8, 3); // 8位复杂字符
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>手机号校验规则：11 位，以 1 开头，第二位是 2-9</li>
 *     <li>邮箱校验使用标准邮箱格式正则</li>
 *     <li>验证码生成使用 ThreadLocalRandom，线程安全</li>
 *     <li>验证码字符集排除了容易混淆的字符（如 0、O、I、l 等）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class VerifyUtil {

    /**
     * 手机号的正则校验 Pattern
     * <p>
     * 用于校验中国大陆手机号格式。<br>
     * 规则：11 位数字，以 1 开头，第二位是 2-9，后面 9 位是 0-9。
     * <p>
     * <b>匹配规则：</b>
     * <ul>
     *     <li>^1：以 1 开头</li>
     *     <li>[2|3|4|5|6|7|8|9]：第二位是 2-9 中的任意一个</li>
     *     <li>[0-9]\\d{8}：后面 9 位是 0-9 的数字</li>
     * </ul>
     * <p>
     * <b>支持的号段：</b>
     * <ul>
     *     <li>13x、14x、15x、16x、17x、18x、19x（x 为 0-9）</li>
     * </ul>
     */
    public static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 邮箱的正则校验 Pattern
     * <p>
     * 用于校验邮箱地址格式。<br>
     * 规则：用户名部分 + @ + 域名部分 + . + 顶级域名（至少 2 个字符）。
     * <p>
     * <b>匹配规则：</b>
     * <ul>
     *     <li>[a-zA-Z0-9._%+-]+：用户名部分，支持字母、数字、点、下划线、百分号、加号、减号</li>
     *     <li>@：@ 符号</li>
     *     <li>[a-zA-Z0-9.-]+：域名部分，支持字母、数字、点、减号</li>
     *     <li>\\.[a-zA-Z]{2,}：顶级域名，至少 2 个字母</li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <ul>
     *     <li>user@example.com ✓</li>
     *     <li>user.name@example.co.uk ✓</li>
     *     <li>user+tag@example.com ✓</li>
     * </ul>
     */
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /**
     * 纯数字验证码字符集
     * <p>
     * 包含 0-9 的数字，用于生成纯数字验证码。
     */
    public static final String NUMBER_VERIFY_CODES = "1234567890";

    /**
     * 数字 + 字母验证码字符集
     * <p>
     * 包含字母和数字，排除了容易混淆的字符（如 0、O、I、l 等）。
     * 用于生成字母+数字混合验证码。
     */
    public static final String ALPHABET_VERIFY_CODES = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 字母 + 数字 + 特殊字符验证码字符集
     * <p>
     * 包含字母、数字和特殊字符，排除了容易混淆的字符。
     * 用于生成复杂验证码（包含特殊字符）。
     */
    public static final String COMPLEX_VERIFY_CODES = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*?";

    /**
     * 手机号格式校验
     * <p>
     * 校验手机号是否符合中国大陆手机号格式。
     * 规则：11 位数字，以 1 开头，第二位是 2-9。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 校验手机号
     * boolean valid = VerifyUtil.checkPhone("13800138000"); // true
     * boolean invalid1 = VerifyUtil.checkPhone("12345678901"); // false（第二位不是2-9）
     * boolean invalid2 = VerifyUtil.checkPhone("1380013800"); // false（长度不对）
     * boolean invalid3 = VerifyUtil.checkPhone(null); // false
     * boolean invalid4 = VerifyUtil.checkPhone(""); // false
     * }</pre>
     * <p>
     * <b>支持的号段：</b>
     * <ul>
     *     <li>13x、14x、15x、16x、17x、18x、19x（x 为 0-9）</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 phone 为 null 或空字符串，返回 false</li>
     *     <li>只校验格式，不校验号段是否真实存在</li>
     *     <li>使用预编译的 Pattern，性能较好</li>
     *     <li>匹配规则：^1[2|3|4|5|6|7|8|9][0-9]\\d{8}$</li>
     * </ul>
     *
     * @param phone 需要校验的手机号，11 位，以 1 开头，第二位是 2-9
     * @return 如果格式正确返回 true，否则返回 false
     * @see #PHONE_PATTERN
     */
    public static boolean checkPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        Matcher m = PHONE_PATTERN.matcher(phone);
        return m.matches();
    }

    /**
     * 邮箱格式校验
     * <p>
     * 校验邮箱地址是否符合标准邮箱格式。
     * 使用正则表达式进行格式校验。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 校验邮箱
     * boolean valid = VerifyUtil.checkEmail("user@example.com"); // true
     * boolean valid2 = VerifyUtil.checkEmail("user.name@example.co.uk"); // true
     * boolean invalid1 = VerifyUtil.checkEmail("invalid-email"); // false
     * boolean invalid2 = VerifyUtil.checkEmail("@example.com"); // false
     * boolean invalid3 = VerifyUtil.checkEmail(null); // false
     * boolean invalid4 = VerifyUtil.checkEmail(""); // false
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 email 为 null 或空字符串，返回 false</li>
     *     <li>只校验格式，不校验邮箱是否真实存在</li>
     *     <li>使用预编译的 Pattern，性能较好</li>
     *     <li>匹配规则：^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$</li>
     *     <li>支持常见的邮箱格式，但不支持所有特殊格式</li>
     * </ul>
     *
     * @param email 需要校验的邮箱地址
     * @return 如果格式正确返回 true，否则返回 false
     * @see #EMAIL_PATTERN
     */
    public static boolean checkEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        Matcher m = EMAIL_PATTERN.matcher(email);
        return m.matches();
    }

    /**
     * 随机生成验证码
     * <p>
     * 根据指定的长度和类型生成随机验证码。
     * 使用线程安全的 ThreadLocalRandom 生成随机数，确保并发安全。
     * <p>
     * <b>验证码类型：</b>
     * <ul>
     *     <li>type = 1：纯数字验证码（0-9）</li>
     *     <li>type = 2：字母 + 数字验证码（排除了容易混淆的字符）</li>
     *     <li>type = 3 或其他：字母 + 数字 + 特殊字符验证码</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 生成6位纯数字验证码
     * String code1 = VerifyUtil.generateVerifyCode(6, 1);
     * // 结果：类似 "123456"
     *
     * // 生成6位字母+数字验证码
     * String code2 = VerifyUtil.generateVerifyCode(6, 2);
     * // 结果：类似 "aB3dE9"
     *
     * // 生成8位复杂验证码（包含特殊字符）
     * String code3 = VerifyUtil.generateVerifyCode(8, 3);
     * // 结果：类似 "aB3dE9!@"
     *
     * // 用于短信验证码
     * String smsCode = VerifyUtil.generateVerifyCode(6, 1);
     * smsService.sendCode(phone, smsCode);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 ThreadLocalRandom，线程安全，性能好</li>
     *     <li>type = 2 和 3 的字符集排除了容易混淆的字符（0、O、I、l 等）</li>
     *     <li>size 必须大于 0，否则会抛出异常</li>
     *     <li>每次生成的验证码都是随机的，不保证唯一性</li>
     *     <li>验证码字符从对应的字符集中随机选择</li>
     * </ul>
     *
     * @param size 验证码长度，必须大于 0
     * @param type 验证码类型：1=纯数字，2=字母+数字，3或其他=字母+数字+特殊字符
     * @return 生成的验证码字符串
     * @throws IllegalArgumentException 当 size <= 0 时可能抛出异常
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