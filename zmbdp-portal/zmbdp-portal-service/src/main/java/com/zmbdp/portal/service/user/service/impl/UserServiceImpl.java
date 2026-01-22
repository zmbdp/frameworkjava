package com.zmbdp.portal.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
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
import com.zmbdp.portal.service.user.validator.AccountValidatorFactory;
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
     * 账号校验策略工厂
     */
    @Autowired
    private AccountValidatorFactory validatorFactory;

    /**
     * 用户 登录/注册
     *
     * @param loginDTO 用户信息 DTO
     * @return tokenDTO 令牌
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        // 针对入参进行分发，看看到底是微信登录还是手机/邮箱登录
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 微信登录
            loginByWechat(wechatLoginDTO, loginUserDTO);
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            // 手机/邮箱 登录
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
        AppUserVO appUserVO;
        // 先进行查询是否存在
        Result<AppUserVO> result = appUserApi.findByOpenId(wechatLoginDTO.getOpenId());
        // 对查询结果进行判断
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            // 没查到，需要进行注册
            appUserVO = register(wechatLoginDTO);
        } else {
            // 说明查到了，直接拼装结果
            appUserVO = result.getData();
        }
        // 设置登录信息
        if (appUserVO != null) {
            BeanCopyUtil.copyProperties(appUserVO, loginUserDTO);
            loginUserDTO.setUserName(appUserVO.getNickName());
        }
    }

    /**
     * 验证码登录处理逻辑（支持手机号/邮箱）
     *
     * @param codeLoginDTO 验证码登录 DTO
     * @param loginUserDTO 用户信息上下文 DTO
     */
    private void loginByCode(CodeLoginDTO codeLoginDTO, LoginUserDTO loginUserDTO) {
        String account = codeLoginDTO.getAccount();
        // 使用策略模式进行账号格式校验（根据输入格式自动选择校验器）
        validatorFactory.validate(account);

        AppUserVO appUserVO;
        Result<AppUserVO> result;

        // 根据账号类型查询用户（使用策略模式判断是手机号还是邮箱）
        if (VerifyUtil.checkPhone(account)) {
            // 手机号：查询手机号用户
            result = appUserApi.findByPhone(account);
        } else if (VerifyUtil.checkEmail(account)) {
            // 邮箱：查询邮箱用户
            result = appUserApi.findByEmail(account);
        } else {
            throw new ServiceException("账号格式错误，请输入手机号或邮箱", ResultCode.INVALID_PARA.getCode());
        }

        // 查不到就注册，查得到就赋值
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            appUserVO = register(codeLoginDTO);
        } else {
            appUserVO = result.getData();
        }

        // 再校验验证码
        if (!captchaService.checkCode(account, codeLoginDTO.getCode())) {
            throw new ServiceException(ResultCode.ERROR_CODE);
        }
        // 走到这里表示通过了，从缓存中删除
        if (!captchaService.deleteCode(account)) {
            log.warn("验证码删除失败！手机号/邮箱: {}", account);
        }
        // 设置登录信息
        if (appUserVO != null) {
            BeanCopyUtil.copyProperties(appUserVO, loginUserDTO);
            loginUserDTO.setUserName(appUserVO.getNickName());
        }
    }

    /**
     * 根据入参来注册（支持微信/手机号/邮箱）
     *
     * @param loginDTO 用户生命周期信息
     * @return 用户 VO
     */
    private AppUserVO register(LoginDTO loginDTO) {
        Result<AppUserVO> result = null;
        // 判断一下是微信还是手机号/邮箱
        if (loginDTO instanceof WechatLoginDTO wechatLoginDTO) {
            // 如果是微信的，就直接找微信登录的 api 就行了
            result = appUserApi.registerByOpenId(wechatLoginDTO.getOpenId());
            // 判断结果
            if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
                log.error("用户注册失败! {}", wechatLoginDTO.getOpenId());
            }
        } else if (loginDTO instanceof CodeLoginDTO codeLoginDTO) {
            // 使用策略模式判断是手机号还是邮箱，然后调用相应的注册方法
            String account = codeLoginDTO.getAccount();
            if (VerifyUtil.checkPhone(account)) {
                // 手机号注册
                result = appUserApi.registerByPhone(account);
            } else if (VerifyUtil.checkEmail(account)) {
                // 邮箱注册
                result = appUserApi.registerByEmail(account);
            } else {
                log.error("账号格式错误，无法注册! {}", account);
            }
            if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
                log.error("用户注册失败! {}", account);
            }
        }
        return result == null ? null : result.getData();
    }

    /**
     * 发送验证码（支持手机号或邮箱，根据输入格式自动判断）
     *
     * @param account 手机号或邮箱地址
     * @return 验证码
     */
    @Override
    public String sendCode(String account) {
        // 使用策略模式进行账号格式校验（根据输入格式自动选择校验器）
        validatorFactory.validate(account);
        return captchaService.sendCode(account);
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
        Result<AppUserVO> result = appUserApi.findById(loginUserDTO.getUserId());
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