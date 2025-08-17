package com.zmbdp.admin.service.user.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * B端用户查询VO
 *
 * @author 稚名不带撇
 */
@Data
public class SysUserVo implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户身份
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
