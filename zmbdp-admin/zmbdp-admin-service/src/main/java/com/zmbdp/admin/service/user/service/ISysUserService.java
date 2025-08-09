package com.zmbdp.admin.service.user.service;

import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;

/**
 * B端用户服务接口
 *
 * @author 稚名不带撇
 */
public interface ISysUserService {
    /**
     * B端用户登录
     *
     * @param passwordLoginDTO B端用户信息
     * @return token 信息
     */
    TokenDTO login(PasswordLoginDTO passwordLoginDTO);
}
