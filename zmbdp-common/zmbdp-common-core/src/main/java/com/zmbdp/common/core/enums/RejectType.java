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
     * AbortPolicy 策略 枚举值：1
     */
    AbortPolicy(1),

    /**
     * CallerRunsPolicy 策略 枚举值：2
     */
    CallerRunsPolicy(2),

    /**
     * DiscardOldestPolicy 策略 枚举值：3
     */
    DiscardOldestPolicy(3),

    /**
     * DiscardPolicy 策略 枚举值：4
     */
    DiscardPolicy(4);

    /**
     * 枚举值
     */
    private Integer value;

    /**
     * 构造函数
     *
     * @param value 枚举值
     */
    RejectType(Integer value) {
        this.value = value;
    }
}
