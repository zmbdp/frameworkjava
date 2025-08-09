package com.zmbdp.admin.service.user.service;

import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserDTO;
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

    /**
     * 新增或编辑用户
     *
     * @param sysUserDTO B端用户信息
     * @return 用户 ID
     */
    Long addOrEdit(SysUserDTO sysUserDTO);
}
