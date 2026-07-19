package com.zmbdp.admin.service.config.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zmbdp.common.domain.domain.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数表对应的实体类
 *
 * @author 稚名不带撇
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_argument")
public class SysArgument extends BaseEntity {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数业务主键
     */
    private String configKey;

    /**
     * 参数值
     */
    private String value;

    /**
     * 备注
     */
    private String remark;
}