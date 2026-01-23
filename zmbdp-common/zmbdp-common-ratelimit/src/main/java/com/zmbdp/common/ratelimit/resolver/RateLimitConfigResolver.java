package com.zmbdp.common.ratelimit.resolver;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.RateLimitConstants;
import com.zmbdp.common.ratelimit.annotation.RateLimit;
import com.zmbdp.common.ratelimit.config.RateLimitConfig;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 限流配置解析器
 * <p>
 * 负责从注解和 Nacos 配置中解析限流参数，遵循优先级：注解 > Nacos > 默认值。
 *
 * @author 稚名不带撇
 */
@Component
public class RateLimitConfigResolver {

    /**
     * Spring 环境对象
     */
    private final Environment environment;

    /**
     * 构造函数
     *
     * @param environment Spring 环境对象
     */
    public RateLimitConfigResolver(Environment environment) {
        this.environment = environment;
    }

    /**
     * 解析限流配置
     * <p>
     * 优先级：注解参数 > Nacos 全局配置 > 代码默认值
     *
     * @param rateLimit 限流注解实例
     * @return 限流配置对象
     */
    public RateLimitConfig resolve(RateLimit rateLimit) {
        // 读取全局配置
        // Redis Key 前缀
        String keyPrefix = environment.getProperty(RateLimitConstants.NACOS_KEY_PREFIX, String.class, RateLimitConstants.KEY_PREFIX_DEFAULT);
        // 默认限流阈值
        int globalLimit = environment.getProperty(RateLimitConstants.NACOS_DEFAULT_LIMIT, Integer.class, RateLimitConstants.DEFAULT_LIMIT);
        // 默认窗口大小（秒）
        long globalWindowSec = environment.getProperty(RateLimitConstants.NACOS_DEFAULT_WINDOW_SEC, Long.class, RateLimitConstants.DEFAULT_WINDOW_SEC);
        // 默认提示信息
        String globalMessage = environment.getProperty(RateLimitConstants.NACOS_DEFAULT_MESSAGE, String.class, RateLimitConstants.DEFAULT_MESSAGE);
        // 是否开启失败快速返回
        Boolean globalFailOpen = environment.getProperty(RateLimitConstants.NACOS_FAIL_OPEN, Boolean.class, false);
        // IP 请求头名称
        String globalIpHeaderName = environment.getProperty(RateLimitConstants.NACOS_IP_HEADER_NAME, String.class, RateLimitConstants.DEFAULT_IP_HEADER_NAME);
        // 是否允许从请求参数获取 IP
        Boolean globalAllowIpParam = environment.getProperty(RateLimitConstants.NACOS_IP_ALLOW_PARAM, Boolean.class, RateLimitConstants.DEFAULT_IP_ALLOW_PARAM);
        // IP 请求参数名称
        String globalIpParamName = environment.getProperty(RateLimitConstants.NACOS_IP_PARAM_NAME, String.class, RateLimitConstants.DEFAULT_IP_PARAM_NAME);

        // 确定最终使用的配置值（注解参数优先，未配置则使用全局配置）
        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : globalLimit; // 如果注解参数有值，则使用注解参数
        long windowSec = rateLimit.windowSec() > 0 ? rateLimit.windowSec() : globalWindowSec;
        String message = StringUtil.isNotEmpty(rateLimit.message())
                ? rateLimit.message().trim()
                : globalMessage;
        boolean failOpen = globalFailOpen != null && globalFailOpen;

        // IP 获取配置（优先级：注解 > Nacos > 默认值）
        String ipHeaderName = StringUtil.isNotEmpty(rateLimit.ipHeaderName())
                ? rateLimit.ipHeaderName().trim()
                : globalIpHeaderName;
        boolean allowIpParam = rateLimit.allowIpParam() || (globalAllowIpParam != null && globalAllowIpParam);
        String ipParamName = StringUtil.isNotEmpty(rateLimit.ipParamName())
                ? rateLimit.ipParamName().trim()
                : globalIpParamName;

        // 返回最终的配置对象
        return new RateLimitConfig(
                keyPrefix,
                limit,
                windowSec,
                windowSec * 1000L, // 转换为毫秒
                message,
                failOpen,
                ipHeaderName,
                allowIpParam,
                ipParamName
        );
    }

    /**
     * 检查限流是否启用
     *
     * @return true 启用，false 禁用
     */
    public boolean isEnabled() {
        Boolean enabled = environment.getProperty(RateLimitConstants.NACOS_ENABLED, Boolean.class, true);
        return enabled != null && enabled;
    }
}