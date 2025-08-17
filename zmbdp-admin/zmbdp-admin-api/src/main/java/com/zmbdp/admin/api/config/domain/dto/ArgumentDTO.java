package com.zmbdp.admin.api.config.domain.dto;

import lombok.Data;

/**
 * 远程调用参数对象 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class ArgumentDTO {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数业务主键
     */
    private String configKey;

    /**
     * 参数值
     */
    private String value;

    /**
     * 备注
     */
    private String remark;
}
