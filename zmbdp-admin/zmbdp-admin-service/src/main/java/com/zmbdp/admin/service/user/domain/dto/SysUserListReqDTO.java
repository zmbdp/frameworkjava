package com.zmbdp.admin.service.user.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * B端用户查询 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class SysUserListReqDTO implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 身份
     */
    private String identity;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}