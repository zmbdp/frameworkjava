package com.zmbdp.common.security.utils;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.SecurityConstants;
import com.zmbdp.common.domain.constants.TokenConstants;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;

/**
 * 安全工具类
 * <p>
 * 提供 Token 提取和处理相关的工具方法。
 * 主要用于从 HTTP 请求中提取 Token，并处理 Token 前缀。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>提取 Token</b>：从 HTTP 请求头中提取 Token</li>
 *     <li><b>处理前缀</b>：自动移除 Token 前缀（如 "Bearer "）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 从当前请求中获取 Token
 * String token = SecurityUtil.getToken();
 * // 从请求头 Authorization 中提取 Token
 *
 * // 2. 从指定请求中获取 Token
 * HttpServletRequest request = ...;
 * String token = SecurityUtil.getToken(request);
 *
 * // 3. 处理 Token 前缀
 * String tokenWithPrefix = "Bearer eyJhbGciOiJIUzUxMiJ9...";
 * String token = SecurityUtil.replaceTokenPrefix(tokenWithPrefix);
 * // 返回：eyJhbGciOiJIUzUxMiJ9...（移除了 "Bearer " 前缀）
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>Token 从请求头 {@code Authorization} 中获取</li>
 *     <li>支持自动移除 Token 前缀（如 "Bearer "）</li>
 *     <li>如果请求头中没有 Token，返回 null</li>
 *     <li>Token 前缀配置在 {@code TokenConstants.PREFIX} 中</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.domain.constants.SecurityConstants#AUTHENTICATION
 * @see com.zmbdp.common.domain.constants.TokenConstants#PREFIX
 */
public class SecurityUtil {

    /**
     * 从当前请求中获取 Token
     * <p>
     * 从当前线程的 HTTP 请求中提取 Token。
     * 内部调用 {@link com.zmbdp.common.core.utils.ServletUtil#getRequest()} 获取当前请求。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在控制器或服务中获取当前请求的 Token
     * String token = SecurityUtil.getToken();
     * if (token != null) {
     *     // Token 存在，可以使用
     *     LoginUserDTO user = tokenService.getLoginUser(token, "your-secret-key");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 HTTP 请求上下文中调用（不能在非 Web 环境中使用）</li>
     *     <li>如果当前线程没有 HTTP 请求，可能抛出异常</li>
     *     <li>Token 从请求头 {@code Authorization} 中获取</li>
     *     <li>会自动处理 Token 前缀（如 "Bearer "）</li>
     * </ul>
     *
     * @return Token 字符串，如果请求头中没有 Token 返回 null
     * @see #getToken(HttpServletRequest)
     * @see com.zmbdp.common.core.utils.ServletUtil#getRequest()
     */
    public static String getToken() {
        return getToken(Objects.requireNonNull(ServletUtil.getRequest()));
    }

    /**
     * 从 HTTP 请求中获取 Token
     * <p>
     * 从请求头 {@code Authorization} 中提取 Token，并自动处理 Token 前缀。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在拦截器中获取 Token
     * @Override
     * public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
     *     String token = SecurityUtil.getToken(request);
     *     if (token != null) {
     *         // Token 存在，验证 Token
     *         LoginUserDTO user = tokenService.getLoginUser(token, "your-secret-key");
     *         if (user != null) {
     *             // Token 有效，继续处理请求
     *             return true;
     *         }
     *     }
     *     // Token 无效，返回未授权错误
     *     response.setStatus(401);
     *     return false;
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Token 从请求头 {@code Authorization} 中获取</li>
     *     <li>请求头名称配置在 {@code SecurityConstants.AUTHENTICATION} 中</li>
     *     <li>会自动调用 {@link #replaceTokenPrefix(String)} 处理 Token 前缀</li>
     *     <li>如果请求头中没有 Token，返回 null</li>
     * </ul>
     *
     * @param request HTTP 请求对象，不能为 null
     * @return Token 字符串，如果请求头中没有 Token 返回 null
     * @see #replaceTokenPrefix(String)
     * @see com.zmbdp.common.domain.constants.SecurityConstants#AUTHENTICATION
     */
    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(SecurityConstants.AUTHENTICATION);
        return replaceTokenPrefix(token);
    }

    /**
     * 移除 Token 前缀
     * <p>
     * 如果 Token 以配置的前缀开头（如 "Bearer "），则移除该前缀。
     * 用于处理前端可能添加的 Token 前缀。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // Token 带前缀
     * String tokenWithPrefix = "Bearer eyJhbGciOiJIUzUxMiJ9...";
     * String token = SecurityUtil.replaceTokenPrefix(tokenWithPrefix);
     * // 返回：eyJhbGciOiJIUzUxMiJ9...（移除了 "Bearer " 前缀）
     *
     * // Token 不带前缀
     * String tokenWithoutPrefix = "eyJhbGciOiJIUzUxMiJ9...";
     * String token = SecurityUtil.replaceTokenPrefix(tokenWithoutPrefix);
     * // 返回：eyJhbGciOiJIUzUxMiJ9...（保持不变）
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Token 前缀配置在 {@code TokenConstants.PREFIX} 中（如 "Bearer "）</li>
     *     <li>只移除第一个匹配的前缀</li>
     *     <li>如果 Token 为空或 null，返回原值</li>
     *     <li>如果 Token 不以配置的前缀开头，返回原值</li>
     * </ul>
     *
     * @param token Token 字符串，可能包含前缀（如 "Bearer xxx"），可以为 null 或空字符串
     * @return 移除前缀后的 Token 字符串，如果 Token 为空或没有前缀则返回原值
     * @see com.zmbdp.common.domain.constants.TokenConstants#PREFIX
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