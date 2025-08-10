package com.zmbdp.portal.service.user.controller;


import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.TokenVO;
import com.zmbdp.portal.service.user.entity.dto.WechatLoginDTO;
import com.zmbdp.portal.service.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
