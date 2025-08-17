package com.zmbdp.admin.api.config.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典数据列表 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class DictionaryDataListReqDTO extends BasePageReqDTO {

    /**
     * 字典类型业务主键
     */
    @NotBlank(message = "字典类型业务主键不能为空")
    private String typeKey;

    /**
     * 字典数据值
     */
    private String value;
}