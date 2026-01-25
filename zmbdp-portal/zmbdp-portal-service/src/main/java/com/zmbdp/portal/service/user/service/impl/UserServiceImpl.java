package com.zmbdp.portal.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.core.utils.StringUtil;
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
import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import com.zmbdp.portal.service.user.domain.dto.UserDTO;
import com.zmbdp.portal.service.user.service.IUserService;
import com.zmbdp.portal.service.user.strategy.account.AccountStrategyContext;
import com.zmbdp.portal.service.user.strategy.login.LoginRouter;
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
     * 账号处理策略路由器
     */
    @Autowired
    private AccountStrategyContext accountStrategyContext;

    /**
     * 登录策略路由器
     * <p>
     * 根据登录类型（微信登录、验证码登录等）自动选择对应的登录策略。
     */
    @Autowired
    private LoginRouter loginRouter;

    /**
     * 用户 登录/注册
     * <p>
     * 通过登录策略路由器自动选择合适的登录策略执行登录逻辑。
     *
     * @param loginDTO 用户信息 DTO
     * @return tokenDTO 令牌
     */
    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        // 通过策略路由器自动选择合适的登录策略执行登录
        LoginUserDTO loginUserDTO = loginRouter.login(loginDTO);
        // 这时候数据表里面肯定有数据的，直接设置缓存，返回给前端就可以了
        loginUserDTO.setUserFrom(UserConstants.USER_FROM_TU_C);
        return tokenService.createToken(loginUserDTO, secret);
    }

    /**
     * 发送验证码（支持手机号或邮箱，根据输入格式自动判断）
     *
     * @param account 手机号或邮箱地址
     * @return 验证码
     */
    @Override
    public String sendCode(String account) {
        // 使用策略模式进行账号格式校验（根据输入格式自动选择处理策略）
        accountStrategyContext.validate(account);
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