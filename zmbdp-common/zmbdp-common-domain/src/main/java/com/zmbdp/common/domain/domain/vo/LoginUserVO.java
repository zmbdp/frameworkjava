package com.zmbdp.common.domain.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录信息
 *
 * @author 稚名不带撇
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户标识
     */
    private String token;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;
}
