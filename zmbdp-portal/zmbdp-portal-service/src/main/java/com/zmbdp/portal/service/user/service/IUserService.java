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
     * 用户 登录/注册
     *
     * @param loginDTO 用户信息 DTO
     * @return tokenDTO 令牌
     */
    TokenDTO login(LoginDTO loginDTO);

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    String sendCode(String phone);
}
