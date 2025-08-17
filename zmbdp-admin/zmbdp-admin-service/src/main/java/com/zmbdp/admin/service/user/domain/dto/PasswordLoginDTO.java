package com.zmbdp.admin.service.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号密码登录信息
 *
 * @author 稚名不带撇
 */
@Data
public class PasswordLoginDTO implements Serializable { // Serializable: 序列化接口, 可以进行序列化

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
