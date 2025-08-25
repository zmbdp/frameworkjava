package com.zmbdp.common.security.utils;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.SecurityConstants;
import com.zmbdp.common.domain.constants.TokenConstants;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

/**
 * 安全工具类
 *
 * @author 稚名不带撇
 */
public class SecurityUtil {

    /**
     * 获取请求 token
     * @return token 信息
     */
    public static String getToken() {
        return getToken(Objects.requireNonNull(ServletUtil.getRequest()));
    }

    /**
     * 根据 request 获取请求 token
     * @param request 请求
     * @return token信息
     */
    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHENTICATION);
        return replaceTokenPrefix(token);
    }

    /**
     * 裁剪 token 前缀
     * @param token 前端可能设置了令牌的前缀
     * @return token 信息
     */
    public static String replaceTokenPrefix(String token) {
        // 假如前端设置了令牌的前缀，需要替换裁剪
        if (StringUtil.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
            // 把前缀换成空串
            token = token.replaceFirst(TokenConstants.PREFIX, "");
        }
        return token;
    }
}