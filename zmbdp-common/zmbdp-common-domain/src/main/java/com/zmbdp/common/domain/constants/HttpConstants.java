package com.zmbdp.common.domain.constants;

/**
 * Http 常量
 *
 * @author 稚名不带撇
 */
public class HttpConstants {

    /**
     * 系统管理员路径
     */
    public static final String SYS_USER_PATH = "sys_user";

    /**
     * 普通用户路径
     */
    public static final String APP_USER_PATH = "app_user";

    /*=============================================    HTTP 请求头常量    =============================================*/

    /**
     * HTTP 请求头：User-Agent
     */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /**
     * HTTP 请求头：X-Forwarded-For
     * <p>
     * 用于标识通过代理或负载均衡器转发的原始客户端 IP。
     * <p>
     * 格式：{@code 客户端IP, 代理1IP, 代理2IP, ...}
     * 我们只取第一个 IP（最原始的客户端 IP）。
     */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * HTTP 请求头：X-Real-IP
     * <p>
     * Nginx 等反向代理服务器设置的客户端真实 IP。
     * <p>
     * 通常只有一个 IP 地址。
     */
    public static final String HEADER_X_REAL_IP = "X-Real-IP";
}