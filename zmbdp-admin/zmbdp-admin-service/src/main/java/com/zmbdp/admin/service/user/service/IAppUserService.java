package com.zmbdp.admin.service.user.service;

import com.zmbdp.admin.api.appuser.domain.dto.AppUserDTO;

/**
 * C端用户服务
 *
 * @author zmbdp
 */
public interface IAppUserService {

    /**
     * 根据微信 ID 注册用户
     *
     * @param openId 用户微信 ID
     * @return C端用户 DTO
     */
    AppUserDTO registerByOpenId(String openId);

    /**
     * 根据 openId 查询用户信息
     *
     * @param openId 用户微信 ID
     * @return C端用户 DTO
     */
    AppUserDTO findByPhone(String openId);
}
