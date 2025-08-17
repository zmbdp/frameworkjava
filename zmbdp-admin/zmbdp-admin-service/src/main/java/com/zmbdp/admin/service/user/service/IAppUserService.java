package com.zmbdp.admin.service.user.service;

import com.zmbdp.admin.api.appuser.domain.dto.AppUserDTO;
import com.zmbdp.admin.api.appuser.domain.dto.AppUserListReqDTO;
import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.common.core.domain.dto.BasePageDTO;

import java.util.List;

/**
 * C端用户服务
 *
 * @author 稚名不带撇
 */
public interface IAppUserService {

    /*=============================================    内部调用    =============================================*/

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
    AppUserDTO findByOpenId(String openId);

    /**
     * 根据手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return C端用户 DTO
     */
    AppUserDTO findByPhone(String phoneNumber);

    /**
     * 根据手机号注册用户
     *
     * @param phoneNumber 手机号
     * @return C端用户 DTO
     */
    AppUserDTO registerByPhone(String phoneNumber);

    /**
     * 编辑 C端用户
     *
     * @param userEditReqDTO C端用户 DTO
     */
    void edit(UserEditReqDTO userEditReqDTO);

    /**
     * 根据用户 ID 获取用户信息
     *
     * @param userId 用户 ID
     * @return C端用户 VO
     */
    AppUserDTO findById(Long userId);

    /**
     * 根据用户 ID 列表获取用户列表信息
     *
     * @param userIds 用户 ID 列表
     * @return C端用户 DTO 列表
     */
    List<AppUserDTO> getUserList(List<Long> userIds);

    /*=============================================    前端调用    =============================================*/

    /**
     * 查询 C端用户
     *
     * @param appUserListReqDTO 查询 C端用户 DTO
     * @return C端用户分页结果
     */
    BasePageDTO<AppUserDTO> getUserList(AppUserListReqDTO appUserListReqDTO);
}
