package com.zmbdp.admin.api.config.domain.vo;

import lombok.Data;

/**
 * 字典数据列表 VO
 *
 * @author 稚名不带撇
 */
@Data
public class DictionaryDataVo {

    /**
     * 字典数据主键
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
     * 排序
     */
    private Integer sort;

    /**
     * 字典数据状态
     */
    private Integer status;
}
