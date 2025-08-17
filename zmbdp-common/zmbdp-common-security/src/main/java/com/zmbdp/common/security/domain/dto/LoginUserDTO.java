package com.zmbdp.common.security.domain.dto;

import lombok.Data;

/**
 * 用户信息上下文
 *
 * @author 稚名不带撇
 */
@Data
public class LoginUserDTO {

    /**
     * 用户标识
     */
    private String token;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户来源
     */
    private String userFrom;

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
