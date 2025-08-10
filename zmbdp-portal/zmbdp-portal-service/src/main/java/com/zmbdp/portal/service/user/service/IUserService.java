package com.zmbdp.portal.service.user.service;

import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.portal.service.user.entity.dto.LoginDTO;

/**
 * 门户服务
 *
 * @author 稚名不带撇
 */
public interface IUserService {

    /**
     * 微信登录
     *
     * @param loginDTO 微信登录 DTO
     * @return tokenDTO 令牌
     */
    TokenDTO login(LoginDTO loginDTO);
}
