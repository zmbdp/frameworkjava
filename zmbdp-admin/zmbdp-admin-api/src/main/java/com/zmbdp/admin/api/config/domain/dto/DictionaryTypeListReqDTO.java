package com.zmbdp.admin.api.config.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import lombok.Data;

/**
 * 字典类型列表 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class DictionaryTypeListReqDTO extends BasePageReqDTO {

    /**
     * 字典类型业务主键
     */
    private String typeKey;

    /**
     * 字典类型值
     */
    private String value;
}