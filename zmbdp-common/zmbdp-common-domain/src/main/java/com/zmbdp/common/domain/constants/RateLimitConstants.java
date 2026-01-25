package com.zmbdp.common.domain.constants;

/**
 * 频控相关常量
 *
 * @author 稚名不带撇
 */
public class RateLimitConstants {

    /**
     * Redis Key 前缀默认值
     */
    public static final String KEY_PREFIX_DEFAULT = "ratelimit:";

    /**
     * Nacos 配置：key 前缀
     */
    public static final String NACOS_KEY_PREFIX = "ratelimit.key-prefix";

    /**
     * Nacos 配置：全局默认限流阈值
     */
    public static final String NACOS_DEFAULT_LIMIT = "ratelimit.default-limit";

    /**
     * Nacos 配置：全局默认时间窗口（秒）
     */
    public static final String NACOS_DEFAULT_WINDOW_SEC = "ratelimit.default-window-sec";

    /**
     * Nacos 配置：全局默认提示信息
     */
    public static final String NACOS_DEFAULT_MESSAGE = "ratelimit.default-message";

    /**
     * Nacos 配置：全局开关，false 时跳过限流
     */
    public static final String NACOS_ENABLED = "ratelimit.enabled";

    /**
     * Nacos 配置：Redis 异常时的降级策略
     * true = 失败放行（降级），false = 失败拒绝（保证安全）
     */
    public static final String NACOS_FAIL_OPEN = "ratelimit.fail-open";

    /**
     * 默认限流阈值（仅当 Nacos 也未配置时使用）
     */
    public static final int DEFAULT_LIMIT = 60;

    /**
     * 默认时间窗口秒数
     */
    public static final long DEFAULT_WINDOW_SEC = 60L;

    /**
     * 默认提示信息
     */
    public static final String DEFAULT_MESSAGE = "请求过于频繁，请稍后重试";

    /**
     * 请求头：网关下发的用户 ID（与 {@link com.zmbdp.common.domain.constants.SecurityConstants#USER_ID} 一致）
     */
    public static final String HEADER_USER_ID = "userId";

    /**
     * 账号
     */
    public static final String HEADER_ACCOUNT = "account";

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

    /**
     * 无效 IP 标识：unknown
     * <p>
     * 某些代理服务器在无法获取真实 IP 时会设置此值，需要过滤掉。
     */
    public static final String UNKNOWN = "unknown";

    /**
     * Nacos 配置：IP 请求头名称
     * <p>
     * 用于指定从 HTTP 请求头中获取客户端 IP 的字段名称。
     * 优先级：注解参数 > Nacos 配置 > 默认值（X-Forwarded-For）
     */
    public static final String NACOS_IP_HEADER_NAME = "ratelimit.ip-header-name";

    /**
     * Nacos 配置：是否允许从请求参数获取 IP
     * <p>
     * 当无法在请求头中传递 IP 时，可以启用此选项从请求参数中获取 IP。
     * 优先级：注解参数 > Nacos 配置 > 默认值（false）
     */
    public static final String NACOS_IP_ALLOW_PARAM = "ratelimit.ip-allow-param";

    /**
     * Nacos 配置：IP 请求参数名称
     * <p>
     * 指定从请求参数中获取 IP 的参数名称。仅当 {@code ip-allow-param = true} 时生效。
     * 优先级：注解参数 > Nacos 配置 > 默认值（"clientIp"）
     */
    public static final String NACOS_IP_PARAM_NAME = "ratelimit.ip-param-name";

    /**
     * 默认 IP 请求头名称
     * <p>
     * 默认使用 X-Forwarded-For，支持多层代理场景。
     */
    public static final String DEFAULT_IP_HEADER_NAME = HEADER_X_FORWARDED_FOR;

    /**
     * 默认是否允许从请求参数获取 IP
     */
    public static final boolean DEFAULT_IP_ALLOW_PARAM = false;

    /**
     * 默认 IP 请求参数名称
     */
    public static final String DEFAULT_IP_PARAM_NAME = "clientIp";
}