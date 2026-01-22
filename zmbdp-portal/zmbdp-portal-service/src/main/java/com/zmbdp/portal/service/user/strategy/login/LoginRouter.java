package com.zmbdp.portal.service.user.strategy.login;

import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 登录策略路由器
 * <p>
 * 根据登录类型自动选择合适的登录策略。
 * 遍历所有实现了 {@link ILoginStrategy} 接口的策略，找到支持当前登录类型的策略并调用。
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *     <li>接收登录 DTO</li>
 *     <li>遍历所有策略，找到支持该登录类型的策略</li>
 *     <li>调用策略的 login 方法执行登录逻辑</li>
 *     <li>如果所有策略都不支持，抛出异常</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 自动识别登录类型并执行登录
 * @Autowired
 * private LoginRouter loginStrategyRouter;
 * LoginUserDTO loginUserDTO = loginStrategyRouter.login(wechatLoginDTO);
 *
 * // 验证码登录
 * LoginUserDTO loginUserDTO = loginStrategyRouter.login(codeLoginDTO);
 * }</pre>
 * <p>
 * <b>扩展性：</b>
 * <ul>
 *     <li>如需支持新的登录类型（如QQ登录、支付宝登录等），只需实现 {@link ILoginStrategy} 接口</li>
 *     <li>Spring 会自动注入新的策略实现，无需修改路由器代码</li>
 *     <li>符合开闭原则：对扩展开放，对修改关闭</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有策略实现类必须标注 {@code @Component} 注解，以便 Spring 自动注入</li>
 *     <li>如果登录类型无法识别，抛出 IllegalArgumentException 异常</li>
 *     <li>策略按注入顺序遍历，找到第一个支持的策略即使用</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILoginStrategy
 */
@Slf4j
@Component
public class LoginRouter {

    /**
     * 登录策略列表
     * <p>
     * Spring 会自动注入所有实现了 {@link ILoginStrategy} 接口的 Bean。
     * 当前包含的策略：
     * <ul>
     *     <li>{@link com.zmbdp.portal.service.user.strategy.login.impl.WechatLoginStrategy}：微信登录策略</li>
     *     <li>{@link com.zmbdp.portal.service.user.strategy.login.impl.CodeLoginStrategy}：验证码登录策略</li>
     * </ul>
     */
    @Autowired
    private List<ILoginStrategy> loginStrategies;

    /**
     * 执行登录
     * <p>
     * 根据登录类型自动选择合适的策略并执行登录逻辑。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>遍历所有策略实现类</li>
     *     <li>调用每个策略的 {@link ILoginStrategy#supports(LoginDTO)} 方法</li>
     *     <li>找到第一个返回 true 的策略</li>
     *     <li>调用该策略的 {@link ILoginStrategy#login(LoginDTO)} 方法执行登录</li>
     *     <li>如果所有策略都不支持，抛出异常</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 微信登录
     * LoginUserDTO loginUserDTO = loginStrategyRouter.login(wechatLoginDTO);
     * // 自动使用 WechatLoginStrategy 执行登录
     *
     * // 验证码登录
     * LoginUserDTO loginUserDTO = loginStrategyRouter.login(codeLoginDTO);
     * // 自动使用 CodeLoginStrategy 执行登录
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 loginDTO 为 null，可能抛出异常</li>
     *     <li>如果登录类型无法识别（所有策略都不支持），抛出 IllegalArgumentException</li>
     *     <li>策略按注入顺序遍历，找到第一个支持的策略即使用</li>
     *     <li>登录失败会抛出 ServiceException 异常</li>
     * </ul>
     *
     * @param loginDTO 登录 DTO，不能为 null
     * @return 登录用户信息 DTO，不能为 null
     * @throws IllegalArgumentException                           如果登录类型无法识别（所有策略都不支持）
     * @throws com.zmbdp.common.domain.exception.ServiceException 如果登录失败
     * @see ILoginStrategy#supports(LoginDTO)
     * @see ILoginStrategy#login(LoginDTO)
     */
    public LoginUserDTO login(LoginDTO loginDTO) {
        for (ILoginStrategy strategy : loginStrategies) {
            if (strategy.supports(loginDTO)) {
                return strategy.login(loginDTO);
            }
        }
        throw new IllegalArgumentException("不支持的登录类型: " + loginDTO.getClass().getSimpleName());
    }
}