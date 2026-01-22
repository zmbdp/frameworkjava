package com.zmbdp.portal.service.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 验证码登录
 *
 * @author 稚名不带撇
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeLoginDTO extends LoginDTO {

    /**
     * 手机号/邮箱
     */
    @NotBlank(message = "手机号/邮箱不能为空")
    private String account;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}