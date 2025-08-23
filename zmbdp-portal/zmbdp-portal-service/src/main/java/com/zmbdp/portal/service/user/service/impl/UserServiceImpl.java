package com.zmbdp.portal.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVo;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.constants.UserConstants;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.message.service.CaptchaService;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.common.security.service.TokenService;
import com.zmbdp.common.security.utils.JwtUtil;
import com.zmbdp.common.security.utils.SecurityUtil;
import com.zmbdp.portal.service.user.domain.dto.CodeLoginDTO;
import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import com.zmbdp.portal.service.user.domain.dto.UserDTO;
import com.zmbdp.portal.service.user.domain.dto.WechatLoginDTO;
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

    /**
     * 令牌密钥
     */
    @Value("${jwt.token.secret}")
    private String secret;

    /**
     * 验证码服务
     */
    @Autowired
    private CaptchaService captchaService;

    /**
     * 用户 登录/注册
     *
     * @param loginDTO 用户信息 DTO
     * @return tokenDTO 令牌
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        // 针对入参进行分发，看看到底是微信登录还是手机登录
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 微信登录
            loginByWechat(wechatLoginDTO, loginUserDTO);
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            // 手机登录
            loginByCode(codeLoginDTO, loginUserDTO);
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
            loginUserDTO.setUserName(appUserVo.getNickName());
        }
    }

    /**
     * 手机号登录处理逻辑
     *
     * @param codeLoginDTO 验证码登录 DTO
     * @param loginUserDTO 用户信息上下文 DTO
     */
    private void loginByCode(CodeLoginDTO codeLoginDTO, LoginUserDTO loginUserDTO) {
        // 校验手机号
        if (!VerifyUtil.checkPhone(codeLoginDTO.getPhone())) {
            throw new ServiceException("手机号格式错误", ResultCode.INVALID_PARA.getCode());
        }
        AppUserVo appUserVo;
        // 查询是否存在
        Result<AppUserVo> result = appUserApi.findByPhone(codeLoginDTO.getPhone());
        // 查不到就注册，查得到就赋值
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            appUserVo = register(codeLoginDTO);
        } else {
            appUserVo = result.getData();
        }
        // 然后从缓存中获取验证码
        String cacheCode = captchaService.getCode(codeLoginDTO.getPhone());
        // 再校验验证码
        if (cacheCode == null) {
            throw new ServiceException("验证码无效", ResultCode.INVALID_PARA.getCode());
        }
        if (!cacheCode.equals(codeLoginDTO.getCode())) {
            throw new ServiceException("验证码错误", ResultCode.INVALID_PARA.getCode());
        }
        // 走到这里表示通过了，从缓存中删除
        captchaService.deleteCode(codeLoginDTO.getPhone());
        // 设置登录信息
        if (appUserVo != null) {
            BeanCopyUtil.copyProperties(appUserVo, loginUserDTO);
            loginUserDTO.setUserName(appUserVo.getNickName());
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
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            // 3 处理手机号注册逻辑
            result = appUserApi.registerByPhone(codeLoginDTO.getPhone());
            if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
                log.error("用户注册失败! {}", codeLoginDTO.getPhone());
            }
        }
        return result == null ? null : result.getData();
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    @Override
    public String sendCode(String phone) {
        if (!VerifyUtil.checkPhone(phone)) {
            throw new ServiceException("手机号格式错误", ResultCode.INVALID_PARA.getCode());
        }
        return captchaService.sendCode(phone);
    }

    /**
     * 编辑 C端用户信息
     *
     * @param userEditReqDTO C端用户编辑 DTO
     */
    @Override
    public void edit(UserEditReqDTO userEditReqDTO) {
        Result<Void> result = appUserApi.edit(userEditReqDTO);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new ServiceException("修改用户失败");
        }
    }

    /**
     * 获取用户登录信息
     *
     * @return 用户信息 DTO
     */
    @Override
    public UserDTO getLoginUser() {
        // 获取当前登录的用户
        LoginUserDTO loginUserDTO = tokenService.getLoginUser(secret);
        // 判断令牌是否正确
        if (loginUserDTO == null) {
            throw new ServiceException("用户令牌有误", ResultCode.INVALID_PARA.getCode());
        }
        // 然后再查出数据库的看看能不能查询出来
        Result<AppUserVo> result = appUserApi.findById(loginUserDTO.getUserId());
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            throw new ServiceException("查询用户失败", ResultCode.INVALID_PARA.getCode());
        }
        // 拼接对象，返回结果
        UserDTO userDTO = new UserDTO();
        // 拼接 jwt 的结果
        BeanCopyUtil.copyProperties(loginUserDTO, userDTO);
        // 拼接 数据库的结果
        BeanCopyUtil.copyProperties(result.getData(), userDTO);
        userDTO.setUserName(result.getData().getNickName());
        return userDTO;
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        // 解析令牌, 拿出用户信息做个日志
        // 拿的是 JWT
        String Jwt = SecurityUtil.getToken();
        if (StringUtil.isEmpty(Jwt)) {
            return;
        }
        String userName = JwtUtil.getUserName(Jwt, secret);
        String userId = JwtUtil.getUserId(Jwt, secret);
        log.info("[{}] 退出了系统, 用户ID: {}", userName, userId);
        // 根据 jwt 删除用户缓存记录
        tokenService.delLoginUser(Jwt, secret);
    }
}
