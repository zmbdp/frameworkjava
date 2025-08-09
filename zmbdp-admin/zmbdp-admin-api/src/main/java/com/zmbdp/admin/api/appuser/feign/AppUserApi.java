package com.zmbdp.admin.api.appuser.feign;

import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVo;
import com.zmbdp.common.domain.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * C端用户服务远程调用 Api
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "appUserApi", name = "zmbdp-admin-service", path = "/app_user")
public interface AppUserApi {

    /**
     * 根据微信 ID 注册用户
     *
     * @param openId 用户微信 ID
     * @return C端用户 VO
     */
    @GetMapping("/register/openid")
    Result<AppUserVo> registerByOpenId(@RequestParam String openId);

    /**
     * 根据 openId 查询用户信息
     *
     * @param openId 用户微信 ID
     * @return C端用户 VO
     */
    @GetMapping("/open_id_find")
    Result<AppUserVo> findByOpenId(@RequestParam String openId);

    /**
     * 根据手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return C端用户 VO
     */
    @GetMapping("/phone_find")
    Result<AppUserVo> findByPhone(@RequestParam String phoneNumber);

    /**
     * 根据手机号注册用户
     *
     * @param phoneNumber 手机号
     * @return C端用户 VO
     */
    @GetMapping("/register/phone")
    Result<AppUserVo> registerByPhone(@RequestParam String phoneNumber);

    /**
     * 编辑 C端用户
     *
     * @param userEditReqDTO C端用户 DTO
     * @return void
     */
    @PostMapping("/edit")
    Result<Void> edit(@RequestBody @Validated UserEditReqDTO userEditReqDTO);

    /**
     * 根据用户 ID 获取用户信息
     *
     * @param userId 用户 ID
     * @return C端用户 VO
     */
    @GetMapping("/id_find")
    Result<AppUserVo> findById(@RequestParam Long userId);

    /**
     * 根据用户 ID 列表获取用户列表信息
     *
     * @param userIds 用户 ID 列表
     * @return C端用户 VO 列表
     */
    @PostMapping("/list")
    Result<List<AppUserVo>> list(@RequestBody List<Long> userIds);
}
