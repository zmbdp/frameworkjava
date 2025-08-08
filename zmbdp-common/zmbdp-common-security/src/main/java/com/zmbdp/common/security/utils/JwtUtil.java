package com.zmbdp.common.security.utils;

import com.zmbdp.common.domain.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Map;

/**
 * Jwt工具类
 */
public class JwtUtil {

    /**
     * 从原始数据声明生成令牌
     *
     * @param claims 数据声明
     * @param secret 密钥
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims, String secret) {
        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /**
     * 根据令牌获取数据声明
     *
     * @param token  令牌
     * @param secret 密钥
     * @return 数据声明
     */
    public static Claims parseToken(String token, String secret) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 根据令牌获取用户标识
     *
     * @param token  令牌
     * @param secret 密钥
     * @return 用户标识
     */
    public static String getUserKey(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据数据声明获取用户标识
     *
     * @param claims 数据声明
     * @return 用户标识
     */
    public static String getUserKey(Claims claims) {
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据令牌获取用户 ID
     *
     * @param token  令牌
     * @param secret 密钥
     * @return 用户ID
     */
    public static String getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USER_ID);
    }

    /**
     * 根据数据声明获取用户ID
     *
     * @param claims 数据声明
     * @return 用户ID
     */
    public static String getUserId(Claims claims) {
        return getValue(claims, SecurityConstants.USER_ID);
    }

    /**
     * 根据令牌获取用户名称
     *
     * @param token  令牌
     * @param secret 密钥
     * @return 用户名称
     */
    public static String getUserName(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USERNAME);
    }

    /**
     * 根据数据声明获取用户名称
     *
     * @param claims 数据声明
     * @return 用户名称
     */
    public static String getUserName(Claims claims) {
        return getValue(claims, SecurityConstants.USERNAME);
    }

    /**
     * 根据令牌获取用户来源
     *
     * @param token 令牌
     * @return 用户来源
     */
    public static String getUserFrom(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return getValue(claims, SecurityConstants.USER_FROM);
    }

    /**
     * 根据数据声明获取用户来源
     *
     * @param claims 数据声明
     * @return 用户来源
     */
    public static String getUserFrom(Claims claims) {
        return getValue(claims, SecurityConstants.USER_FROM);
    }

    public static String getValue(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
