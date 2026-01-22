package com.zmbdp.portal.service.user.strategy.login.impl;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.message.service.CaptchaService;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.portal.service.user.domain.dto.CodeLoginDTO;
import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import com.zmbdp.portal.service.user.strategy.login.ILoginStrategy;
import com.zmbdp.portal.service.user.strategy.login.LoginRouter;
import com.zmbdp.portal.service.user.strategy.validator.AccountValidatorRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 验证码登录策略
 * <p>
 * 实现 {@link ILoginStrategy} 接口，提供基于验证码的登录功能。
 * 支持手机号和邮箱两种账号类型的验证码登录。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>验证码登录：通过手机号/邮箱和验证码登录</li>
 *     <li>账号格式校验：自动判断是手机号还是邮箱</li>
 *     <li>自动注册：如果用户不存在则自动注册</li>
 *     <li>验证码校验：校验用户输入的验证码是否正确</li>
 *     <li>用户信息填充：将用户信息填充到 LoginUserDTO</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 通过路由器自动选择策略（推荐）
 * @Autowired
 * private LoginRouter loginStrategyRouter;
 * LoginUserDTO loginUserDTO = loginStrategyRouter.login(codeLoginDTO);
 *
 * // 直接注入指定实现类
 * @Autowired
 * @Qualifier("codeLoginStrategy")
 * private ILoginStrategy codeStrategy;
 * LoginUserDTO loginUserDTO = codeStrategy.login(codeLoginDTO);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>支持配置热更新（@RefreshScope）</li>
 *     <li>如果用户不存在，会自动调用注册接口</li>
 *     <li>验证码校验失败会抛出异常</li>
 *     <li>登录成功会删除验证码缓存</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILoginStrategy
 * @see LoginRouter
 */
@Slf4j
@Component
public class CodeLoginStrategy implements ILoginStrategy {

    /**
     * C端用户服务
     */
    @Autowired
    private AppUserApi appUserApi;

    /**
     * 验证码服务
     */
    @Autowired
    private CaptchaService captchaService;

    /**
     * 账号校验策略工厂
     */
    @Autowired
    private AccountValidatorRouter validatorFactory;

    /**
     * 是否支持当前登录类型
     * <p>
     * 判断当前策略是否支持验证码登录类型。
     * 通过 instanceof 判断是否为 CodeLoginDTO。
     *
     * @param loginDTO 登录 DTO，不能为 null
     * @return true 表示是验证码登录类型，false 表示不是
     */
    @Override
    public boolean supports(LoginDTO loginDTO) {
        return loginDTO instanceof CodeLoginDTO;
    }

    /**
     * 执行验证码登录逻辑
     * <p>
     * 通过手机号/邮箱和验证码登录，如果用户不存在则自动注册，然后填充用户信息到 LoginUserDTO。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>将 loginDTO 转换为 CodeLoginDTO</li>
     *     <li>校验账号格式（手机号或邮箱）</li>
     *     <li>根据账号类型查询用户（手机号或邮箱）</li>
     *     <li>如果用户不存在，调用注册接口</li>
     *     <li>校验验证码</li>
     *     <li>删除验证码缓存</li>
     *     <li>将用户信息填充到 LoginUserDTO</li>
     *     <li>返回 LoginUserDTO</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 通过注入的 Bean 执行登录
     * @Autowired
     * @Qualifier("codeLoginStrategy")
     * private ILoginStrategy codeStrategy;
     * LoginUserDTO loginUserDTO = codeStrategy.login(codeLoginDTO);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 loginDTO 不是 CodeLoginDTO 类型，会抛出 ClassCastException</li>
     *     <li>如果账号格式错误，会抛出 ServiceException</li>
     *     <li>如果验证码错误，会抛出 ServiceException（ResultCode.ERROR_CODE）</li>
     *     <li>如果查询和注册都失败，返回的 LoginUserDTO 可能没有用户信息</li>
     *     <li>登录成功会设置 userName 为用户的 nickName</li>
     * </ul>
     *
     * @param loginDTO 登录 DTO，必须是 CodeLoginDTO 类型
     * @return 登录用户信息 DTO，不能为 null
     * @throws ServiceException 如果账号格式错误或验证码错误
     */
    @Override
    public LoginUserDTO login(LoginDTO loginDTO) {
        CodeLoginDTO codeLoginDTO = (CodeLoginDTO) loginDTO;
        LoginUserDTO loginUserDTO = new LoginUserDTO();

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

        return loginUserDTO;
    }

    /**
     * 注册用户（支持手机号/邮箱）
     * <p>
     * 根据账号类型（手机号或邮箱）调用相应的注册接口。
     *
     * @param codeLoginDTO 验证码登录 DTO，不能为 null
     * @return 用户 VO，如果注册失败返回 null
     */
    private AppUserVO register(CodeLoginDTO codeLoginDTO) {
        String account = codeLoginDTO.getAccount();
        Result<AppUserVO> result = null;

        // 使用策略模式判断是手机号还是邮箱，然后调用相应的注册方法
        if (VerifyUtil.checkPhone(account)) {
            // 手机号注册
            result = appUserApi.registerByPhone(account);
        } else if (VerifyUtil.checkEmail(account)) {
            // 邮箱注册
            result = appUserApi.registerByEmail(account);
        } else {
            log.error("账号格式错误，无法注册! {}", account);
            return null;
        }
        // 如果说返回的值是 null 或者 状态码不是 200 或者 数据为 null，就注册失败
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            log.error("用户注册失败! {}", account);
            return null;
        }
        return result.getData();
    }
}