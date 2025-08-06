package com.zmbdp.admin.api.config.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 编辑字典数据 DTO
 */
@Data
public class DictionaryDataEditReqDTO {

    /**
     * 字典数据业务主键
     */
    @NotBlank(message = "字典数据业务主键不能为空")
    private String dataKey;

    /**
     * 字典数据名称
     */
    @NotBlank(message = "字典数据名称不能为空")
    private String value;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序值
     */
    private Integer sort;
}