package com.zmbdp.portal.service.user.strategy.account.impl;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.portal.service.user.manager.AccountManager;
import com.zmbdp.portal.service.user.strategy.account.IAccountStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 邮箱账号策略
 * <p>
 * 负责邮箱格式的校验、查询、注册等逻辑，实现 {@link IAccountStrategy} 接口。<br>
 * 当账号格式为邮箱时，此策略会被 {@link AccountManager} 选中并执行处理。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>邮箱格式校验：验证账号是否为有效的邮箱格式</li>
 *     <li>用户查询：根据邮箱查询用户信息</li>
 *     <li>用户注册：根据邮箱注册新用户</li>
 *     <li>查询或注册：查询用户，如果不存在则自动注册</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>验证码登录（邮箱）</li>
 *     <li>发送验证码（邮箱）</li>
 *     <li>用户注册（邮箱）</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 方式1：通过 AccountManager 使用（推荐）
 * @Autowired
 * private AccountManager accountStrategyContext;
 * AppUserVO user = accountStrategyContext.validateAndFindOrRegisterUser("user@example.com");
 *
 * // 方式2：直接注入使用（不推荐，通常不需要）
 * @Autowired
 * private EmailAccountStrategy emailAccountStrategy;
 * if (emailAccountStrategy.supports("user@example.com")) {
 *     emailAccountStrategy.validate("user@example.com");
 *     Result<AppUserVO> result = emailAccountStrategy.findUser("user@example.com");
 * }
 * }</pre>
 * </p>
 *
 * @author 稚名不带撇
 * @see IAccountStrategy
 * @see AccountManager
 */
@Slf4j
@Component
public class EmailAccountStrategy implements IAccountStrategy {

    /**
     * C端用户服务
     * <p>
     * 用于查询和注册用户信息。
     * </p>
     */
    @Autowired
    private AppUserApi appUserApi;

    /**
     * 判断是否支持邮箱格式的处理
     * <p>
     * 通过调用工具类 {@link VerifyUtil#checkEmail(String)} 验证账号是否为邮箱格式。
     * <p>
     * <b>邮箱格式规则：</b>
     * <ul>
     *     <li>包含 @ 符号</li>
     *     <li>@ 前有用户名部分</li>
     *     <li>@ 后有域名部分</li>
     *     <li>域名部分包含有效的顶级域名</li>
     * </ul>
     * </p>
     *
     * @param account 待判断的账号，不能为 null
     * @return true 表示账号为邮箱格式，支持处理；false 表示不是邮箱格式，不支持
     */
    @Override
    public boolean supports(String account) {
        return VerifyUtil.checkEmail(account);
    }

    /**
     * 执行邮箱格式校验
     * <p>
     * 对账号进行邮箱格式验证，如果格式不正确则抛出异常。<br>
     * 此方法会进行二次校验，确保账号格式正确。
     * </p>
     *
     * @param account 待校验的邮箱地址，不能为 null
     * @throws ServiceException 当邮箱格式不正确时抛出异常，异常信息为"邮箱格式错误"，错误码为 {@code ResultCode.INVALID_PARA}
     */
    @Override
    public void validate(String account) {
        if (!VerifyUtil.checkEmail(account)) {
            throw new ServiceException("邮箱格式错误", ResultCode.INVALID_PARA.getCode());
        }
    }

    /**
     * 查询用户
     * <p>
     * 根据邮箱查询用户信息。<br>
     * 调用 {@link AppUserApi#findByEmail(String)} 方法查询用户。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果用户存在：返回 {@code Result.success(userVO)}</li>
     *     <li>如果用户不存在：返回 {@code Result.success()}（data 为 null）</li>
     *     <li>如果查询失败：返回 {@code Result.fail(...)}</li>
     * </ul>
     * </p>
     *
     * @param account 邮箱，不能为 null，且应已通过格式校验
     * @return 用户查询结果，如果用户不存在则返回成功结果但 data 为 null
     */
    @Override
    public Result<AppUserVO> findUser(String account) {
        return appUserApi.findByEmail(account);
    }

    /**
     * 注册用户
     * <p>
     * 根据邮箱注册新用户。<br>
     * 调用 {@link AppUserApi#registerByEmail(String)} 方法注册用户。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果注册成功：返回 {@code Result.success(userVO)}</li>
     *     <li>如果注册失败：返回 {@code Result.fail(...)} 或抛出异常</li>
     * </ul>
     * </p>
     *
     * @param account 邮箱，不能为 null，且应已通过格式校验
     * @return 用户注册结果，如果注册失败则返回失败结果或抛出异常
     */
    @Override
    public Result<AppUserVO> registerUser(String account) {
        return appUserApi.registerByEmail(account);
    }

    /**
     * 查询用户，如果不存在则注册
     * <p>
     * 先根据邮箱查询用户，如果用户不存在则自动注册新用户。<br>
     * 实现"查询或注册"的一站式处理。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>调用 {@link #findUser(String)} 查询用户</li>
     *     <li>如果用户存在（data 不为 null），直接返回查询结果</li>
     *     <li>如果用户不存在（data 为 null），调用 {@link #registerUser(String)} 注册用户</li>
     *     <li>返回注册结果</li>
     * </ol>
     * </p>
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果用户存在：返回查询到的用户信息</li>
     *     <li>如果用户不存在但注册成功：返回注册后的用户信息</li>
     *     <li>如果用户不存在且注册失败：返回失败结果或抛出异常</li>
     * </ul>
     * </p>
     *
     * @param account 邮箱，不能为 null，且应已通过格式校验
     * @return 用户信息结果，如果查询和注册都失败则返回失败结果或抛出异常
     */
    @Override
    public Result<AppUserVO> findOrRegisterUser(String account) {
        // 先查询用户
        Result<AppUserVO> result = findUser(account);
        // 如果用户存在，直接返回
        if (result != null && result.getCode() == ResultCode.SUCCESS.getCode() && result.getData() != null) {
            return result;
        }
        // 用户不存在，注册新用户
        return registerUser(account);
    }
}