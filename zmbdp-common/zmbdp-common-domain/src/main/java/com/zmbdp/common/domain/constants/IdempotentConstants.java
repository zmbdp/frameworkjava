package com.zmbdp.common.domain.constants;

/**
 * 幂等性相关常量
 *
 * @author 稚名不带撇
 */
public class IdempotentConstants {

    /**
     * 幂等性 Token 的 Redis Key 前缀
     */
    public static final String IDEMPOTENT_KEY_PREFIX = "idempotent:token:";

    /**
     * Nacos 幂等性 key 前缀配置名称
     */
    public static final String NACOS_IDEMPOTENT_KEY_PREFIX_PREFIX = "idempotent.key-prefix";

    /**
     * Nacos 上幂等性过期时间配置名称
     */
    public static final String NACOS_IDEMPOTENT_EXPIRE_TIME_PREFIX = "idempotent.expire-time";

    /**
     * Nacos 上是否启动强幂等配置名称
     */
    public static final String NACOS_IDEMPOTENT_RETURN_CACHED_RESULT_PREFIX = "idempotent.return-cached-result";

    /**
     * Nacos 上强幂等模式最大重试次数配置名称
     */
    public static final String NACOS_IDEMPOTENT_MAX_RETRY_COUNT_PREFIX = "idempotent.max-retry-count";

    /**
     * Nacos 上强幂等模式重试间隔时间配置名称（毫秒）
     */
    public static final String NACOS_IDEMPOTENT_RETRY_INTERVAL_MS_PREFIX = "idempotent.retry-interval-ms";
}