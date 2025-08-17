package com.zmbdp.admin.service.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * B端用户信息
 *
 * @author 稚名不带撇
 */
@Data
public class SysUserDTO {

    /**
     * B端人员用户 ID
     */
    private Long userId;

    /**
     * 身份
     */
    private String identity;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phoneNumber;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    /**
     * 状态
     */
    @NotBlank(message = "状态不能为空")
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 校验密码是否合理
     *
     * @return 合理: true; 否则: false
     */
    public boolean checkPassword() {
        if (StringUtils.isEmpty(this.password)) {
            return false;
        }
        if (this.password.length() > 20) {
            return false;
        }
        // 允许字母、数字和 !@#$%^&* 等特殊字符
        return this.password.matches("^[a-zA-Z0-9!@#$%^&*]+$");
    }
}