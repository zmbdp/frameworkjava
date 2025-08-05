package com.zmbdp.admin.api.config.domain.vo;

import lombok.Data;

/**
 * 字典类型 VO
 *
 * @author 稚名不带撇
 */
@Data
public class DictionaryTypeVO {

    /**
     * 字典类型 id
     */
    private Long id;

    /**
     * 字典类型键
     */
    private String typeKey;

    /**
     * 字典类型值
     */
    private String value;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态 1:正常 0:停用
     */
    private Integer status;
}
