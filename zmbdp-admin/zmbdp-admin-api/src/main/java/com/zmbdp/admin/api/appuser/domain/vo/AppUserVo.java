package com.zmbdp.admin.api.appuser.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * C端用户 VO
 *
 * @author 稚名不带撇
 */
@Data
public class AppUserVo implements Serializable {

    /**
     * C端用户 ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 微信 ID
     */
    private String openId;

    /**
     * 用户头像
     */
    private String avatar;
}
