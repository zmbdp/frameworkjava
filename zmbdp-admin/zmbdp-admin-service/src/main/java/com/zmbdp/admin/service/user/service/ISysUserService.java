package com.zmbdp.admin.service.user.service;

import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserListReqDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserLoginDTO;
import com.zmbdp.admin.service.user.domain.vo.SysUserLoginVO;
import com.zmbdp.common.security.domain.dto.TokenDTO;

import java.util.List;

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

    /**
     * 查询 B端用户
     * @param sysUserListReqDTO 用户查询 DTO
     * @return B端用户列表
     */
    List<SysUserDTO> getUserList(SysUserListReqDTO sysUserListReqDTO);

    /**
     * 获取 B端登录用户信息
     *
     * @return B端用户信息 DTO
     */
    SysUserLoginDTO getLoginUser();

    /**
     * 退出登录
     */
    void logout();
}
