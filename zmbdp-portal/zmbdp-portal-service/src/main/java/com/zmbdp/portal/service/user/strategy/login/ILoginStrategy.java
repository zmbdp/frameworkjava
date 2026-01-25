package com.zmbdp.portal.service.user.strategy.login;

import com.zmbdp.portal.service.user.domain.dto.LoginDTO;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;

/**
 * 登录策略接口
 * <p>
 * 定义不同登录方式的统一规范，支持多种登录方式（微信、验证码等）。
 * 每个实现类通过 {@link #supports(LoginDTO)} 方法判断是否支持当前登录类型。
 * <p>
 * <b>设计目的：</b>
 * <ul>
 *     <li>统一登录接口，屏蔽不同登录方式的实现细节</li>
 *     <li>支持多种登录方式（微信、验证码等），易于扩展</li>
 *     <li>每个实现类自己判断是否支持当前登录类型</li>
 * </ul>
 * <p>
 * <b>实现类：</b>
 * <ul>
 *     <li>{@link com.zmbdp.portal.service.user.strategy.login.impl.WechatLoginStrategy}：微信登录策略</li>
 *     <li>{@link com.zmbdp.portal.service.user.strategy.login.impl.CodeLoginStrategy}：验证码登录策略（支持手机号/邮箱）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <ul>
 *     <li>通过路由器获取：使用 {@link LoginRouter#login(LoginDTO)} 根据登录类型自动选择</li>
 *     <li>直接注入实现类：通过 {@code @Qualifier} 指定实现类</li>
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
 *     <li>每个实现类需要实现 supports 方法来判断是否支持当前登录类型</li>
 *     <li>登录失败会抛出异常，成功返回 LoginUserDTO</li>
 *     <li>实现类需要标注 {@code @Component} 注解，并指定 Bean 名称</li>
 *     <li>建议通过 {@link LoginRouter} 使用策略</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see LoginRouter
 */
public interface ILoginStrategy {

    /**
     * 是否支持当前登录类型
     * <p>
     * 判断当前策略是否支持指定的登录类型（如微信登录、验证码登录等）。
     * 每个实现类根据自己的业务逻辑判断是否支持该登录类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 判断是否支持微信登录
     * boolean supports = wechatLoginStrategy.supports(wechatLoginDTO); // true
     * boolean supports = codeLoginStrategy.supports(wechatLoginDTO); // false
     *
     * // 判断是否支持验证码登录
     * boolean supports = codeLoginStrategy.supports(codeLoginDTO); // true
     * boolean supports = wechatLoginStrategy.supports(codeLoginDTO); // false
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 loginDTO 为 null，应返回 false</li>
     *     <li>实现类应该通过 instanceof 判断登录类型</li>
     *     <li>此方法用于路由器自动选择合适的策略</li>
     * </ul>
     *
     * @param loginDTO 登录 DTO，不能为 null
     * @return true 表示支持该登录类型，false 表示不支持
     */
    boolean supports(LoginDTO loginDTO);

    /**
     * 执行登录逻辑
     * <p>
     * 根据不同的登录类型执行相应的登录逻辑，包括用户查询、注册、验证等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 微信登录
     * LoginUserDTO loginUserDTO = wechatLoginStrategy.login(wechatLoginDTO);
     *
     * // 验证码登录
     * LoginUserDTO loginUserDTO = codeLoginStrategy.login(codeLoginDTO);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 loginDTO 为 null 或类型不匹配，可能抛出异常</li>
     *     <li>登录失败会抛出 ServiceException 异常</li>
     *     <li>登录成功返回填充好的 LoginUserDTO</li>
     *     <li>不同实现类有不同的验证逻辑（如验证码登录需要校验验证码）</li>
     * </ul>
     *
     * @param loginDTO 登录 DTO，不能为 null
     * @return 登录用户信息 DTO，不能为 null
     * @throws com.zmbdp.common.domain.exception.ServiceException 如果登录失败
     */
    LoginUserDTO login(LoginDTO loginDTO);
}