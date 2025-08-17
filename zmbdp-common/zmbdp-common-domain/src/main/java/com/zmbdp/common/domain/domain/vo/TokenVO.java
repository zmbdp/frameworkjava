package com.zmbdp.common.domain.domain.vo;

import lombok.Data;

/**
 * 登录响应 VO
 *
 * @author 稚名不带撇
 */
@Data
public class TokenVO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 过期时间
     */
    private Long expires;
}
