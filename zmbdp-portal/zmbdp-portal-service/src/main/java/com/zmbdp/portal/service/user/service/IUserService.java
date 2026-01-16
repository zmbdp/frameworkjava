package com.zmbdp.portal.service.user.service;

import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import com.zmbdp.portal.service.user.domain.dto.UserDTO;

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
     * 发送验证码（支持手机号或邮箱，根据配置自动判断）
     *
     * @param account 手机号或邮箱地址
     * @return 验证码
     */
    String sendCode(String account);

    /**
     * 编辑 C端用户信息
     *
     * @param userEditReqDTO C端用户编辑 DTO
     */
    void edit(UserEditReqDTO userEditReqDTO);

    /**
     * 获取用户登录信息
     *
     * @return 用户信息 DTO
     */
    UserDTO getLoginUser();

    /**
     * 退出登录
     */
    void logout();
}
