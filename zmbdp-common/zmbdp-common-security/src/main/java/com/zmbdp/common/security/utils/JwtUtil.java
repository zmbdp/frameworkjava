package com.zmbdp.common.security.utils;

import com.zmbdp.common.domain.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * JWT（JSON Web Token）工具类
 * <p>
 * 提供 JWT Token 的创建、解析、信息提取等功能。
 * 基于 {@code io.jsonwebtoken} 库实现，使用 HS512 算法进行签名。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>创建 Token</b>：从数据声明（Claims）生成 JWT Token</li>
 *     <li><b>解析 Token</b>：从 JWT Token 中解析数据声明</li>
 *     <li><b>提取信息</b>：从 Token 或 Claims 中提取用户标识、用户ID、用户名、用户来源等信息</li>
 * </ul>
 * <p>
 * <b>JWT Token 结构：</b>
 * <ul>
 *     <li><b>Header</b>：算法类型（HS512）</li>
 *     <li><b>Payload</b>：数据声明（Claims），包含用户标识、用户ID、用户名、用户来源等</li>
 *     <li><b>Signature</b>：使用密钥对 Header 和 Payload 进行签名</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 创建 Token
 * Map<String, Object> claims = new HashMap<>();
 * claims.put("user_key", "uuid-123");
 * claims.put("user_id", "123");
 * claims.put("username", "admin");
 * String token = JwtUtil.createToken(claims, "your-secret-key");
 *
 * // 2. 解析 Token
 * Claims parsedClaims = JwtUtil.parseToken(token, "your-secret-key");
 * String userId = parsedClaims.get("user_id", String.class);
 *
 * // 3. 提取用户标识
 * String userKey = JwtUtil.getUserKey(token, "your-secret-key");
 *
 * // 4. 提取用户ID
 * String userId = JwtUtil.getUserId(token, "your-secret-key");
 *
 * // 5. 提取用户名
 * String username = JwtUtil.getUserName(token, "your-secret-key");
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>使用 HS512 算法进行签名，密钥长度建议至少 64 字符</li>
 *     <li>密钥必须与创建和解析 Token 时使用的密钥一致</li>
 *     <li>Token 中包含的信息是明文的（Base64 编码），不要存储敏感信息</li>
 *     <li>Token 的有效期由调用方控制（通常存储在 Redis 中）</li>
 *     <li>如果 Token 签名验证失败，解析时会抛出异常</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see io.jsonwebtoken.Jwts
 * @see io.jsonwebtoken.Claims
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtil {

    /**
     * 从数据声明（Claims）生成 JWT Token
     * <p>
     * 使用 HS512 算法对数据声明进行签名，生成 JWT Token 字符串。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 构建数据声明
     * Map<String, Object> claims = new HashMap<>();
     * claims.put(SecurityConstants.USER_KEY, "uuid-123-456");
     * claims.put(SecurityConstants.USER_ID, "123");
     * claims.put(SecurityConstants.USERNAME, "admin");
     * claims.put(SecurityConstants.USER_FROM, "admin");
     *
     * // 生成 Token
     * String token = JwtUtil.createToken(claims, "your-secret-key-min-64-chars");
     * // 返回：eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2tleSI6InV1aWQtMTIzLTQ1NiIsInVzZXJfaWQiOiIxMjMifQ...
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 HS512 算法进行签名</li>
     *     <li>密钥长度建议至少 64 字符，以确保安全性</li>
     *     <li>密钥必须与解析 Token 时使用的密钥一致</li>
     *     <li>Claims 中的数据会以 Base64 编码存储在 Token 中（明文可见）</li>
     *     <li>不要存储敏感信息（如密码）到 Claims 中</li>
     * </ul>
     *
     * @param claims 数据声明（Claims），不能为 null，包含要存储在 Token 中的数据
     * @param secret 签名密钥，不能为 null 或空字符串，建议长度至少 64 字符
     * @return JWT Token 字符串（格式：Header.Payload.Signature）
     * @see io.jsonwebtoken.Jwts#builder()
     * @see io.jsonwebtoken.SignatureAlgorithm#HS512
     */
    public static String createToken(Map<String, Object> claims, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Jwts.builder().setClaims(claims).signWith(key, SignatureAlgorithm.HS512).compact();
    }

    /**
     * 解析 JWT Token 获取数据声明（Claims）
     * <p>
     * 验证 Token 的签名，如果验证通过则返回 Token 中的数据声明。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 解析 Token
     * String token = "eyJhbGciOiJIUzUxMiJ9...";
     * Claims claims = JwtUtil.parseToken(token, "your-secret-key");
     *
     * // 从 Claims 中获取数据
     * String userKey = claims.get("user_key", String.class);
     * String userId = claims.get("user_id", String.class);
     * String username = claims.get("username", String.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 Token 签名验证失败，会抛出异常</li>
     *     <li>如果 Token 格式不正确，会抛出异常</li>
     *     <li>密钥必须与创建 Token 时使用的密钥一致</li>
     *     <li>返回的 Claims 对象包含 Token 中的所有数据声明</li>
     * </ul>
     *
     * @param token  JWT Token 字符串，不能为 null 或空字符串
     * @param secret 签名密钥，不能为 null 或空字符串，必须与创建 Token 时使用的密钥一致
     * @return 数据声明（Claims）对象，包含 Token 中的所有数据
     * @throws io.jsonwebtoken.JwtException 如果 Token 签名验证失败或格式不正确
     * @see io.jsonwebtoken.Jwts#parser()
     * @see io.jsonwebtoken.Claims
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
    }

    /**
     * 从 Token 中获取用户标识
     * <p>
     * 解析 Token 并提取用户标识（UUID），用于在 Redis 中查找用户信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 从 Token 中获取用户标识
     * String token = "eyJhbGciOiJIUzUxMiJ9...";
     * String userKey = JwtUtil.getUserKey(token, "your-secret-key");
     * // 返回：uuid-123-456
     *
     * // 使用用户标识从 Redis 中获取用户信息
     * String redisKey = "login_tokens:" + userKey;
     * LoginUserDTO user = redisService.getCacheObject(redisKey, LoginUserDTO.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>用户标识存储在 Claims 的 {@code SecurityConstants.USER_KEY} 键中</li>
     *     <li>如果 Token 解析失败，会抛出异常</li>
     *     <li>如果用户标识不存在，返回空字符串</li>
     *     <li>密钥必须与创建 Token 时使用的密钥一致</li>
     * </ul>
     *
     * @param token  JWT Token 字符串，不能为 null 或空字符串
     * @param secret 签名密钥，不能为 null 或空字符串
     * @return 用户标识（UUID），如果不存在返回空字符串
     * @see #parseToken(String, String)
     * @see #getValue(Claims, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USER_KEY
     */
    public static String getUserKey(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 从数据声明（Claims）中获取用户标识
     * <p>
     * 从已解析的 Claims 对象中提取用户标识，避免重复解析 Token。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 先解析 Token 获取 Claims
     * Claims claims = JwtUtil.parseToken(token, "your-secret-key");
     *
     * // 从 Claims 中获取用户标识（避免重复解析）
     * String userKey = JwtUtil.getUserKey(claims);
     * String userId = JwtUtil.getUserId(claims);
     * String username = JwtUtil.getUserName(claims);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果用户标识不存在，返回空字符串</li>
     *     <li>适用于需要从 Claims 中提取多个信息的场景，避免重复解析 Token</li>
     * </ul>
     *
     * @param claims 数据声明（Claims）对象，不能为 null
     * @return 用户标识（UUID），如果不存在返回空字符串
     * @see #getValue(Claims, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USER_KEY
     */
    public static String getUserKey(Claims claims) {
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 从 Token 中获取用户ID
     * <p>
     * 解析 Token 并提取用户ID。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 从 Token 中获取用户ID
     * String token = "eyJhbGciOiJIUzUxMiJ9...";
     * String userId = JwtUtil.getUserId(token, "your-secret-key");
     * // 返回：123
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>用户ID存储在 Claims 的 {@code SecurityConstants.USER_ID} 键中</li>
     *     <li>如果用户ID不存在，返回空字符串</li>
     *     <li>返回的是字符串类型，需要时自行转换为 Long</li>
     * </ul>
     *
     * @param token  JWT Token 字符串，不能为 null 或空字符串
     * @param secret 签名密钥，不能为 null 或空字符串
     * @return 用户ID（字符串），如果不存在返回空字符串
     * @see #parseToken(String, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USER_ID
     */
    public static String getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USER_ID);
    }

    /**
     * 从数据声明（Claims）中获取用户ID
     * <p>
     * 从已解析的 Claims 对象中提取用户ID。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Claims claims = JwtUtil.parseToken(token, "your-secret-key");
     * String userId = JwtUtil.getUserId(claims);
     * }</pre>
     *
     * @param claims 数据声明（Claims）对象，不能为 null
     * @return 用户ID（字符串），如果不存在返回空字符串
     * @see #getValue(Claims, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USER_ID
     */
    public static String getUserId(Claims claims) {
        return getValue(claims, SecurityConstants.USER_ID);
    }

    /**
     * 从 Token 中获取用户名
     * <p>
     * 解析 Token 并提取用户名。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * String token = "eyJhbGciOiJIUzUxMiJ9...";
     * String username = JwtUtil.getUserName(token, "your-secret-key");
     * // 返回：admin
     * }</pre>
     *
     * @param token  JWT Token 字符串，不能为 null 或空字符串
     * @param secret 签名密钥，不能为 null 或空字符串
     * @return 用户名，如果不存在返回空字符串
     * @see #parseToken(String, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USERNAME
     */
    public static String getUserName(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USERNAME);
    }

    /**
     * 从数据声明（Claims）中获取用户名
     *
     * @param claims 数据声明（Claims）对象，不能为 null
     * @return 用户名，如果不存在返回空字符串
     * @see #getValue(Claims, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USERNAME
     */
    public static String getUserName(Claims claims) {
        return getValue(claims, SecurityConstants.USERNAME);
    }

    /**
     * 从 Token 中获取用户来源
     * <p>
     * 解析 Token 并提取用户来源（如 "admin"、"portal" 等）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * String token = "eyJhbGciOiJIUzUxMiJ9...";
     * String userFrom = JwtUtil.getUserFrom(token, "your-secret-key");
     * // 返回：admin
     * }</pre>
     *
     * @param token  JWT Token 字符串，不能为 null 或空字符串
     * @param secret 签名密钥，不能为 null 或空字符串
     * @return 用户来源，如果不存在返回空字符串
     * @see #parseToken(String, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USER_FROM
     */
    public static String getUserFrom(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USER_FROM);
    }

    /**
     * 从数据声明（Claims）中获取用户来源
     *
     * @param claims 数据声明（Claims）对象，不能为 null
     * @return 用户来源，如果不存在返回空字符串
     * @see #getValue(Claims, String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#USER_FROM
     */
    public static String getUserFrom(Claims claims) {
        return getValue(claims, SecurityConstants.USER_FROM);
    }

    /**
     * 从数据声明（Claims）中获取指定键的值
     * <p>
     * 从 Claims 对象中提取指定键的值，并转换为字符串。
     * 如果值为 null，返回空字符串。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Claims claims = JwtUtil.parseToken(token, "your-secret-key");
     * String userKey = getValue(claims, SecurityConstants.USER_KEY);
     * String userId = getValue(claims, SecurityConstants.USER_ID);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果键不存在或值为 null，返回空字符串</li>
     *     <li>值会被转换为字符串（调用 {@code toString()}）</li>
     *     <li>适用于提取 Claims 中的各种字段</li>
     * </ul>
     *
     * @param claims 数据声明（Claims）对象，不能为 null
     * @param key    要获取的键，不能为 null
     * @return 键对应的值（字符串），如果不存在或为 null 返回空字符串
     */
    private static String getValue(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}