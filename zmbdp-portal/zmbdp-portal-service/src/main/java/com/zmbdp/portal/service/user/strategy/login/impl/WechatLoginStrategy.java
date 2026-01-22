package com.zmbdp.portal.service.user.strategy.login.impl;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.admin.api.appuser.feign.AppUserApi;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import com.zmbdp.portal.service.user.domain.dto.WechatLoginDTO;
import com.zmbdp.portal.service.user.strategy.login.ILoginStrategy;
import com.zmbdp.portal.service.user.strategy.login.LoginRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 微信登录策略
 * <p>
 * 实现 {@link ILoginStrategy} 接口，提供基于微信 OpenId 的登录功能。
 * 支持通过微信 OpenId 查询用户，如果用户不存在则自动注册。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>微信登录：通过 OpenId 查询用户</li>
 *     <li>自动注册：如果用户不存在则自动注册</li>
 *     <li>用户信息填充：将用户信息填充到 LoginUserDTO</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 通过路由器自动选择策略（推荐）
 * @Autowired
 * private LoginRouter loginStrategyRouter;
 * LoginUserDTO loginUserDTO = loginStrategyRouter.login(wechatLoginDTO);
 *
 * // 直接注入指定实现类
 * @Autowired
 * @Qualifier("wechatLoginStrategy")
 * private ILoginStrategy wechatStrategy;
 * LoginUserDTO loginUserDTO = wechatStrategy.login(wechatLoginDTO);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>支持配置热更新（@RefreshScope）</li>
 *     <li>如果用户不存在，会自动调用注册接口</li>
 *     <li>登录成功会填充 LoginUserDTO 的用户信息</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILoginStrategy
 * @see LoginRouter
 */
@Slf4j
@Component
public class WechatLoginStrategy implements ILoginStrategy {

    /**
     * C端用户服务
     */
    @Autowired
    private AppUserApi appUserApi;

    /**
     * 是否支持当前登录类型
     * <p>
     * 判断当前策略是否支持微信登录类型。
     * 通过 instanceof 判断是否为 WechatLoginDTO。
     *
     * @param loginDTO 登录 DTO，不能为 null
     * @return true 表示是微信登录类型，false 表示不是
     */
    @Override
    public boolean supports(LoginDTO loginDTO) {
        return loginDTO instanceof WechatLoginDTO;
    }

    /**
     * 执行微信登录逻辑
     * <p>
     * 通过微信 OpenId 查询用户，如果用户不存在则自动注册，然后填充用户信息到 LoginUserDTO。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>将 loginDTO 转换为 WechatLoginDTO</li>
     *     <li>通过 OpenId 查询用户</li>
     *     <li>如果用户不存在，调用注册接口</li>
     *     <li>将用户信息填充到 LoginUserDTO</li>
     *     <li>返回 LoginUserDTO</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 通过注入的 Bean 执行登录
     * @Autowired
     * @Qualifier("wechatLoginStrategy")
     * private ILoginStrategy wechatStrategy;
     * LoginUserDTO loginUserDTO = wechatStrategy.login(wechatLoginDTO);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 loginDTO 不是 WechatLoginDTO 类型，会抛出 ClassCastException</li>
     *     <li>如果查询和注册都失败，返回的 LoginUserDTO 可能没有用户信息</li>
     *     <li>登录成功会设置 userName 为用户的 nickName</li>
     * </ul>
     *
     * @param loginDTO 登录 DTO，必须是 WechatLoginDTO 类型
     * @return 登录用户信息 DTO，不能为 null
     */
    @Override
    public LoginUserDTO login(LoginDTO loginDTO) {
        WechatLoginDTO wechatLoginDTO = (WechatLoginDTO) loginDTO;
        LoginUserDTO loginUserDTO = new LoginUserDTO();

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

        return loginUserDTO;
    }

    /**
     * 注册微信用户
     * <p>
     * 通过微信 OpenId 注册新用户。
     *
     * @param wechatLoginDTO 微信登录 DTO，不能为 null
     * @return 用户 VO，如果注册失败返回 null
     */
    private AppUserVO register(WechatLoginDTO wechatLoginDTO) {
        Result<AppUserVO> result = appUserApi.registerByOpenId(wechatLoginDTO.getOpenId());
        // 判断结果
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            log.error("用户注册失败! {}", wechatLoginDTO.getOpenId());
            return null;
        }
        return result.getData();
    }
}