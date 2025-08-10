package com.zmbdp.portal.service.user.controller;


import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.TokenVO;
import com.zmbdp.portal.service.user.entity.dto.CodeLoginDTO;
import com.zmbdp.portal.service.user.entity.dto.WechatLoginDTO;
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
     * 微信登录
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
    public Result<String> sendCode(@Validated @RequestParam String phone) {
        return Result.success(userService.sendCode(phone));
    }

    /**
     * 手机号登录
     * @param codeLoginDTO 验证码登录信息
     * @return token信息VO
     */
    @PostMapping("/login/code")
    public Result<TokenVO> login(@Validated @RequestBody CodeLoginDTO codeLoginDTO) {
        return Result.success(userService.login(codeLoginDTO).convertToVo());
    }
}
