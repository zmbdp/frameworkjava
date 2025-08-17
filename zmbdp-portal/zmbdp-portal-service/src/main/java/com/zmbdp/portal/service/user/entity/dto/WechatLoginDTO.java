package com.zmbdp.portal.service.user.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 微信登录信息
 *
 * @author 稚名不带撇
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WechatLoginDTO extends LoginDTO {

    /**
     * 微信 openId
     */
    @NotBlank(message = "微信openId不能为空")
    private String openId;
}
