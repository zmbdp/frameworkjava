package com.zmbdp.admin.service.config.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zmbdp.common.domain.domain.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据表
 *
 * @author 稚名不带撇
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dictionary_data")
public class SysDictionaryData extends BaseEntity {

    /**
     * 字典类型主键
     */
    private String typeKey;

    /**
     * 字典数据主键
     */
    private String dataKey;

    /**
     * 字典数据名称
     */
    private String value;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态 1正常 0停用
     */
    private Integer status;
}