package com.zmbdp.common.core.utils;

import cn.hutool.crypto.SecureUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * AES 加密工具类
 * <p>
 * 提供基于 AES（Advanced Encryption Standard）算法的加密和解密功能。<br>
 * 使用固定的密钥进行加密解密，适用于前后端数据传输加密场景。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>前后端数据传输加密</li>
 *   <li>敏感信息加密存储</li>
 *   <li>API 接口参数加密</li>
 *   <li>密码等敏感字段加密</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>使用固定的密钥（KEYS），前后端必须保持一致</li>
 *   <li>加密结果以十六进制字符串形式返回</li>
 *   <li>如果输入数据为空或 null，返回 null（不会抛出异常）</li>
 *   <li>加密和解密必须使用相同的密钥</li>
 *   <li>密钥长度固定为 16 字节（128 位）</li>
 * </ul>
 * <p>
 * <b>安全建议：</b>
 * <ul>
 *   <li>生产环境建议将密钥配置到配置文件中，不要硬编码</li>
 *   <li>定期更换密钥以提高安全性</li>
 *   <li>对于高敏感数据，建议使用更复杂的加密方案</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class AESUtil {

    /**
     * AES 加密密钥
     * <p>
     * 固定密钥，长度为 16 字节（128 位），用于 AES 加密和解密。<br>
     * 前后端必须使用相同的密钥才能正确加解密。
     * <p>
     * <b>注意：</b>生产环境建议将密钥配置到配置文件中，避免硬编码。
     */
    private static final byte[] KEYS = "12345678abcdefgh".getBytes(StandardCharsets.UTF_8);

    /**
     * AES 加密方法
     * <p>
     * 使用 AES 算法对原始数据进行加密，返回十六进制字符串格式的加密结果。<br>
     * 加密使用固定的密钥（KEYS），确保前后端加解密一致。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 加密用户密码
     * String password = "myPassword123";
     * String encrypted = AESUtil.encryptHex(password);
     * // 结果：返回加密后的十六进制字符串，例如 "a1b2c3d4e5f6..."
     *
     * // 加密 API 参数
     * String param = "userId=123&token=abc";
     * String encryptedParam = AESUtil.encryptHex(param);
     *
     * // 如果输入为空或 null，返回 null
     * String result = AESUtil.encryptHex(null); // 返回 null
     * String result2 = AESUtil.encryptHex("");  // 返回 null
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 data 为 null 或空字符串，返回 null（不会抛出异常）</li>
     *   <li>加密结果以十六进制字符串形式返回，长度是原数据的 2 倍</li>
     *   <li>相同的输入每次加密结果相同（使用固定密钥）</li>
     *   <li>加密后的数据需要使用 {@link #decryptHex(String)} 方法解密</li>
     *   <li>加密结果可以安全地传输和存储</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *   <li>密码加密存储</li>
     *   <li>敏感数据传输</li>
     *   <li>API 参数加密</li>
     *   <li>配置文件敏感信息加密</li>
     * </ul>
     *
     * @param data 待加密的原始数据，可以为 null 或空字符串
     * @return 加密后的十六进制字符串，如果 data 为 null 或空字符串则返回 null
     * @see #decryptHex(String)
     */
    public static String encryptHex(String data) {
        if (StringUtil.isNotEmpty(data)) {
            return SecureUtil.aes(KEYS).encryptHex(data);
        }
        return null;
    }

    /**
     * AES 解密方法
     * <p>
     * 使用 AES 算法对加密后的十六进制字符串进行解密，返回原始数据。<br>
     * 解密必须使用与加密时相同的密钥（KEYS）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 解密之前加密的数据
     * String encrypted = "a1b2c3d4e5f6..."; // 这是之前加密的结果
     * String decrypted = AESUtil.decryptHex(encrypted);
     * // 结果：返回原始数据，例如 "myPassword123"
     *
     * // 解密 API 返回的加密数据
     * String encryptedResponse = apiService.getEncryptedData();
     * String originalData = AESUtil.decryptHex(encryptedResponse);
     *
     * // 如果输入为空或 null，返回 null
     * String result = AESUtil.decryptHex(null); // 返回 null
     * String result2 = AESUtil.decryptHex("");  // 返回 null
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 data 为 null 或空字符串，返回 null（不会抛出异常）</li>
     *   <li>data 必须是有效的十六进制加密字符串，否则解密会失败</li>
     *   <li>必须使用与加密时相同的密钥才能正确解密</li>
     *   <li>如果 data 不是由 {@link #encryptHex(String)} 加密的，解密可能失败或返回乱码</li>
     *   <li>解密失败时可能会抛出异常（由底层 Hutool 库处理）</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *   <li>解密存储的加密密码</li>
     *   <li>解密接收到的加密数据</li>
     *   <li>解密 API 返回的加密参数</li>
     *   <li>解密配置文件中的加密信息</li>
     * </ul>
     * <p>
     * <b>加解密配对使用：</b>
     * <pre>{@code
     * // 加密
     * String original = "敏感数据";
     * String encrypted = AESUtil.encryptHex(original);
     *
     * // 解密（必须使用相同的密钥）
     * String decrypted = AESUtil.decryptHex(encrypted);
     * // decrypted 应该等于 original
     * }</pre>
     *
     * @param data 待解密的十六进制加密字符串，必须是由 {@link #encryptHex(String)} 加密的结果，可以为 null 或空字符串
     * @return 解密后的原始数据，如果 data 为 null 或空字符串则返回 null
     * @throws RuntimeException 当 data 不是有效的加密字符串或密钥不匹配时可能抛出异常
     * @see #encryptHex(String)
     */
    public static String decryptHex(String data) {
        if (StringUtil.isNotEmpty(data)) {
            return SecureUtil.aes(KEYS).decryptStr(data);
        }
        return null;
    }
}