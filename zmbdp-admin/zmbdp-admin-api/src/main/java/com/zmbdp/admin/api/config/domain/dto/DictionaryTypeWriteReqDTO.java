package com.zmbdp.admin.api.config.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典类型写操作 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class DictionaryTypeWriteReqDTO {

    /**
     * 字典类型键
     */
    @NotBlank(message = "字典类型键不能为空")
    private String typeKey;

    /**
     * 字典类型值
     */
    @NotBlank(message = "字典类型值不能为空")
    private String value;

    /**
     * 备注
     */
    private String remark;
}