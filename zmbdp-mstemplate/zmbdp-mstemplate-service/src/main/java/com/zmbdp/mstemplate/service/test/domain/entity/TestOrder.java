package com.zmbdp.mstemplate.service.test.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zmbdp.common.domain.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 测试订单表 test_order
 *
 * @author 稚名不带撇
 */
@Data
@TableName("test_order")
@EqualsAndHashCode(callSuper = true)
public class TestOrder extends BaseDO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID（创建人）
     */
    private Long userId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 状态（1待支付 2已支付 3已取消）
     */
    private String status;

    /**
     * 租户ID（多租户场景）
     */
    private Long tenantId;
}

