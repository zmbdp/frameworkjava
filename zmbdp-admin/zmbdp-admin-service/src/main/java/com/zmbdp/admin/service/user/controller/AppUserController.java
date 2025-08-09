package com.zmbdp.admin.service.user.controller;

import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVo;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.admin.service.user.service.IAppUserService;
import com.zmbdp.common.domain.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端用户服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/app_user")
public class AppUserController implements AppUserApi {

    /**
     * 用户服务 service
     */
    @Autowired
    private IAppUserService appUserService;

    /**
     * 根据微信 ID 注册用户
     *
     * @param openId 用户微信 ID
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVo> registerByOpenId(String openId) {
        return Result.success(appUserService.registerByOpenId(openId).convertToVO());
    }

    /**
     * 根据 openId 查询用户信息
     *
     * @param openId 用户微信 ID
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVo> findByOpenId(String openId) {
        return null;
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVo> findByPhone(String phoneNumber) {
        return null;
    }

    /**
     * 根据手机号注册用户
     *
     * @param phoneNumber 手机号
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVo> registerByPhone(String phoneNumber) {
        return null;
    }

    /**
     * 编辑 C端用户
     *
     * @param userEditReqDTO C端用户 DTO
     * @return void
     */
    @Override
    public Result<Void> edit(UserEditReqDTO userEditReqDTO) {
        return null;
    }

    /**
     * 根据用户 ID 获取用户信息
     *
     * @param userId 用户 ID
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVo> findById(Long userId) {
        return null;
    }

    /**
     * 根据用户 ID 列表获取用户列表信息
     *
     * @param userIds 用户 ID 列表
     * @return C端用户 VO 列表
     */
    @Override
    public Result<List<AppUserVo>> list(List<Long> userIds) {
        return null;
    }
}
