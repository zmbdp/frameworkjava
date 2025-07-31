package com.zmbdp.common.core.enums;

import lombok.Getter;

/**
 * 拒绝策略枚举
 *
 * @author 稚名不带撇
 */
@Getter
public enum RejectType {

    /**
     * AbortPolicy策略 枚举值：1
     */
    AbortPolicy(1),

    /**
     * CallerRunsPolicy策略 枚举值：2
     */
    CallerRunsPolicy(2),

    /**
     * DiscardOldestPolicy策略 枚举值：3
     */
    DiscardOldestPolicy(3),

    /**
     * DiscardPolicy策略 枚举值：4
     */
    DiscardPolicy(4);


    private Integer value;

    RejectType(Integer value) {
        this.value = value;
    }
}
