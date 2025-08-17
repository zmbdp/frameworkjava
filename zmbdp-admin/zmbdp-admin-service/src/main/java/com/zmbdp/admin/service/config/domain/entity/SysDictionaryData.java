package com.zmbdp.admin.service.config.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典数据表
 *
 * @author 稚名不带撇
 */
@Getter
@Setter
@TableName("sys_dictionary_data")
public class SysDictionaryData {

    /**
     * 自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
