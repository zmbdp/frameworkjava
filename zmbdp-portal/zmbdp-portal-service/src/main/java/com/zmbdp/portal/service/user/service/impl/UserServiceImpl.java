package com.zmbdp.portal.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVo;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.constants.UserConstants;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.common.security.service.TokenService;
import com.zmbdp.portal.service.user.entity.dto.LoginDTO;
import com.zmbdp.portal.service.user.entity.dto.WechatLoginDTO;
import com.zmbdp.portal.service.user.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * 门户服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
@RefreshScope
public class UserServiceImpl implements IUserService {

    /**
     * C端用户服务
     */
    @Autowired
    private AppUserApi appUserApi;

    /**
     * 令牌服务
     */
    @Autowired
    private TokenService tokenService;

    @Value("${jwt.token.secret}")
    private String secret;

//    @Autowired
//    private CaptchaService captchaService;

    /**
     * 微信登录
     *
     * @param loginDTO 微信登录 DTO
     * @return tokenDTO 令牌
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        // 针对入参进行分发，看看到底是微信登录还是手机登录
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 微信登录
            loginByWechat(wechatLoginDTO, loginUserDTO);
        }
        // 这时候数据表里面肯定有数据的，直接设置缓存，返回给前端就可以了
        loginUserDTO.setUserFrom(UserConstants.USER_FROM_TU_C);
        return tokenService.createToken(loginUserDTO, secret);
    }

    /**
     * 处理微信登录逻辑
     *
     * @param wechatLoginDTO 微信登录 DTO
     * @param loginUserDTO   用户生命周期对象
     */
    private void loginByWechat(WechatLoginDTO wechatLoginDTO, LoginUserDTO loginUserDTO) {
        AppUserVo appUserVo;
        // 先进行查询是否存在
        Result<AppUserVo> result = appUserApi.findByOpenId(wechatLoginDTO.getOpenId());
        // 对查询结果进行判断
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            // 没查到，需要进行注册
            appUserVo = register(wechatLoginDTO);
        } else {
            // 说明查到了，直接拼装结果
            appUserVo = result.getData();
        }
        // 设置登录信息
        if (appUserVo != null) {
          BeanCopyUtil.copyProperties(appUserVo, loginUserDTO);
        }
    }

    /**
     * 根据入参来注册
     *
     * @param loginDTO 用户生命周期信息
     * @return 用户 VO
     */
    private AppUserVo register(LoginDTO loginDTO) {
        Result<AppUserVo> result = null;
        // 判断一下是微信还是手机号
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 如果是微信的，就直接找微信登录的 api 就行了
            result = appUserApi.registerByOpenId(wechatLoginDTO.getOpenId());
            // 判断结果
            if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
                log.error("用户注册失败! {}", wechatLoginDTO.getOpenId());
            }
        }
        return result == null ? null : result.getData();
    }
}
