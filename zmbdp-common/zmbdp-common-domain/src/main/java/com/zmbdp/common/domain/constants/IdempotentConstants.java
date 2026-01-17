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
}
