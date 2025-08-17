package com.zmbdp.admin.api.config.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 编辑参数 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class ArgumentEditReqDTO {

    /**
     * 参数业务主键
     */
    @NotBlank(message = "参数业务主键不能为空")
    private String configKey;

    /**
     * 参数名称
     */
    @NotBlank(message = "参数名称不能为空")
    private String name;

    /**
     * 参数值
     */
    @NotBlank(message = "参数值不能为空")
    private String value;

    /**
     * 备注
     */
    private String remark;
}
