package com.zmbdp.portal.service.user.controller;


import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.TokenVO;
import com.zmbdp.portal.service.user.domain.dto.CodeLoginDTO;
import com.zmbdp.portal.service.user.domain.dto.WechatLoginDTO;
import com.zmbdp.portal.service.user.domain.vo.UserVo;
import com.zmbdp.portal.service.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 门户服务
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 门户服务 service
     */
    @Autowired
    private IUserService userService;

    /**
     * 微信登录 / 注册
     *
     * @param wechatLoginDTO 微信登录 DTO
     * @return token令牌
     */
    @PostMapping("/login/wechat")
    public Result<TokenVO> login(@Validated @RequestBody WechatLoginDTO wechatLoginDTO) {
        return Result.success(userService.login(wechatLoginDTO).convertToVo());
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    @GetMapping("/send_code")
    public Result<String> sendCode(@Validated @RequestParam("phone") String phone) {
        return Result.success(userService.sendCode(phone));
    }

    /**
     * 手机号登录 / 注册
     *
     * @param codeLoginDTO 验证码登录信息
     * @return token 信息 VO
     */
    @PostMapping("/login/code")
    public Result<TokenVO> login(@Validated @RequestBody CodeLoginDTO codeLoginDTO) {
        return Result.success(userService.login(codeLoginDTO).convertToVo());
    }

    /**
     * 编辑 C端用户信息
     *
     * @param userEditReqDTO C端用户编辑 DTO
     * @return void
     */
    @PostMapping("/edit")
    public Result<Void> edit(@Validated @RequestBody UserEditReqDTO userEditReqDTO) {
        userService.edit(userEditReqDTO);
        return Result.success();
    }

    /**
     * 获取用户登录信息
     *
     * @return 用户信息 VO
     */
    @GetMapping("/login_info/get")
    public Result<UserVo> getLoginUser() {
        return Result.success(userService.getLoginUser().convertToVO());
    }

    /**
     * 退出登录
     *
     * @return void
     */
    @DeleteMapping("/logout")
    Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
}
