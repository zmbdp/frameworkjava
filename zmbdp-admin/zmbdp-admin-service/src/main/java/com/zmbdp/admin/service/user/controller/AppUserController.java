package com.zmbdp.admin.service.user.controller;

import com.zmbdp.admin.api.appuser.domain.dto.AppUserDTO;
import com.zmbdp.admin.api.appuser.domain.dto.AppUserListReqDTO;
import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.admin.service.user.service.IAppUserService;
import com.zmbdp.common.core.domain.dto.BasePageDTO;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import com.zmbdp.common.domain.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * C端用户服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/app_user")
public class AppUserController implements AppUserApi {

    /*=============================================    内部调用    =============================================*/

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
    public Result<AppUserVO> registerByOpenId(String openId) {
        AppUserDTO appUserDTO = appUserService.registerByOpenId(openId);
        if (appUserDTO == null) {
            throw new ServiceException("注册失败");
        }
        return Result.success(appUserDTO.convertToVO());
    }

    /**
     * 根据 openId 查询用户信息
     *
     * @param openId 用户微信 ID
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVO> findByOpenId(String openId) {
        AppUserDTO appUserDTO = appUserService.findByOpenId(openId);
        if (appUserDTO == null) {
            return Result.success();
        }
        return Result.success(appUserDTO.convertToVO());
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVO> findByPhone(String phoneNumber) {
        AppUserDTO appUserDTO = appUserService.findByPhone(phoneNumber);
        if (appUserDTO == null) {
            return Result.success();
        }
        return Result.success(appUserDTO.convertToVO());
    }

    /**
     * 根据手机号注册用户
     *
     * @param phoneNumber 手机号
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVO> registerByPhone(String phoneNumber) {
        AppUserDTO appUserDTO = appUserService.registerByPhone(phoneNumber);
        if (appUserDTO == null) {
            throw new ServiceException("注册失败");
        }
        return Result.success(appUserDTO.convertToVO());
    }

    /**
     * 编辑 C端用户
     *
     * @param userEditReqDTO C端用户 DTO
     * @return void
     */
    @Override
    public Result<Void> edit(@Validated UserEditReqDTO userEditReqDTO) {
        appUserService.edit(userEditReqDTO);
        return Result.success();
    }

    /**
     * 根据用户 ID 获取用户信息
     *
     * @param userId 用户 ID
     * @return C端用户 VO
     */
    @Override
    public Result<AppUserVO> findById(Long userId) {
        AppUserDTO appUserDTO = appUserService.findById(userId);
        if (appUserDTO == null) {
            return Result.success();
        }
        return Result.success(appUserDTO.convertToVO());
    }

    /**
     * 根据用户 ID 列表获取用户列表信息
     *
     * @param userIds 用户 ID 列表
     * @return C端用户 VO 列表
     */
    @Override
    public Result<List<AppUserVO>> list(List<Long> userIds) {
        List<AppUserDTO> appUserDTOList = appUserService.getUserList(userIds);
        return Result.success(appUserDTOList.stream()
                .filter(Objects::nonNull) // 过滤空对象
                .map(AppUserDTO::convertToVO) // 转换为 VO
                .collect(Collectors.toList()) // 转换成列表
        );
    }

    /*=============================================    前端调用    =============================================*/

    /**
     * 查询 C端用户
     *
     * @param appUserListReqDTO 查询 C端用户 DTO
     * @return C端用户分页结果
     */
    @PostMapping("/list/search")
    public Result<BasePageVO<AppUserVO>> list(@RequestBody AppUserListReqDTO appUserListReqDTO) {
        BasePageDTO<AppUserDTO> appUserDTOList = appUserService.getUserList(appUserListReqDTO);
        BasePageVO<AppUserVO> result = new BasePageVO<>();
        BeanCopyUtil.copyProperties(appUserDTOList, result);
        return Result.success(result);
    }
}
