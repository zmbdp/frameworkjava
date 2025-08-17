package com.zmbdp.admin.api.config.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import lombok.Data;

/**
 * 查看参数 DTO
 */
@Data
public class ArgumentListReqDTO extends BasePageReqDTO {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数业务主键
     */
    private String configKey;
}
