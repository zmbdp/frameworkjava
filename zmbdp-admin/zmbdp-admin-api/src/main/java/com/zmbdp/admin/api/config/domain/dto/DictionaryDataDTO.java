package com.zmbdp.admin.api.config.domain.dto;

import lombok.Data;

/**
 * 字典数据 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class DictionaryDataDTO {

    /**
     * 字典数据 ID
     */
    private Long id;

    /**
     * 字典类型业务主键
     */
    private String typeKey;

    /**
     * 字典数据业务主键
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
     * 排序值
     */
    private Integer sort;

    /**
     * 字典数据状态 1正常 0停用
     */
    private Integer status;
}
