package com.zmbdp.portal.service.user.strategy.account;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.portal.service.user.strategy.account.impl.EmailAccountStrategy;
import com.zmbdp.portal.service.user.strategy.account.impl.PhoneAccountStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账号策略上下文
 * <p>
 * 采用策略模式设计，作为策略上下文（Context），负责：
 * <ul>
 *     <li>管理所有账号策略实现类</li>
 *     <li>根据账号格式自动选择合适的处理策略</li>
 *     <li>执行校验、查询、注册等逻辑</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 方式1：仅校验账号格式
 * accountStrategyContext.validate(account);
 *
 * // 方式2：校验账号格式并查询用户（推荐，避免重复判断）
 * Result<AppUserVO> result = accountStrategyContext.validateAndFindUser(account);
 *
 * // 方式3：注册用户
 * Result<AppUserVO> registerResult = accountStrategyContext.registerUser(account);
 *
 * // 方式4：校验账号格式并查询用户，如果不存在则注册（推荐，一站式处理）
 * Result<AppUserVO> result = accountStrategyContext.validateAndFindOrRegisterUser(account);
 * }</pre>
 * </p>
 * <p>
 * 系统会自动根据账号格式（手机号/邮箱）选择合适的处理策略并执行相应操作。
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法都会先校验账号格式，格式不正确会抛出异常</li>
 *     <li>推荐使用 {@link #validateAndFindOrRegisterUser(String)} 方法，实现查询或注册的一站式处理</li>
 *     <li>策略选择基于 {@link IAccountStrategy#supports(String)} 方法</li>
 * </ul>
 * </p>
 *
 * @author 稚名不带撇
 * @see IAccountStrategy
 * @see PhoneAccountStrategy
 * @see EmailAccountStrategy
 */
@Slf4j
@Component
public class AccountStrategyContext {

    /**
     * 所有注册的账号策略实现类列表
     * <p>
     * Spring 会自动注入所有实现了 {@link IAccountStrategy} 接口的 Bean，
     * 如：{@link PhoneAccountStrategy}、{@link EmailAccountStrategy} 等。
     * </p>
     */
    private final List<IAccountStrategy> accountStrategies;

    /**
     * 构造方法，自动注入所有账号策略实现类
     * <p>
     * Spring 会通过构造函数注入所有实现了 {@link IAccountStrategy} 接口的 Bean。
     * </p>
     *
     * @param accountStrategies Spring 容器中所有 {@link IAccountStrategy} 的实现类，不能为 null
     */
    @Autowired
    public AccountStrategyContext(List<IAccountStrategy> accountStrategies) {
        this.accountStrategies = accountStrategies;
    }

    /**
     * 根据账号格式自动选择合适的策略并执行校验
     * <p>
     * 此方法会根据账号格式自动选择合适的策略，并执行账号格式校验。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>遍历所有注册的策略实现类</li>
     *     <li>通过 {@link IAccountStrategy#supports(String)} 方法筛选出支持该账号格式的策略</li>
     *     <li>选择第一个匹配的策略（通常只有一个匹配）</li>
     *     <li>调用策略的 {@link IAccountStrategy#validate(String)} 方法执行校验</li>
     * </ol>
     * </p>
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>仅需要校验账号格式，不需要查询用户</li>
     *     <li>发送验证码前的格式校验</li>
     * </ul>
     * </p>
     *
     * @param account 待校验的账号（手机号或邮箱等），不能为 null
     * @throws ServiceException 当无法识别账号格式或账号格式不符合要求时抛出异常，错误码为 {@code ResultCode.INVALID_PARA}
     */
    public void validate(String account) {
        IAccountStrategy handler = findHandler(account);
        handler.validate(account);
    }

    /**
     * 校验账号格式并查询用户
     * <p>
     * 此方法会先校验账号格式，然后根据账号类型查询用户信息，
     * 避免后续代码重复判断账号类型和重复查询。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>根据账号格式选择合适的策略</li>
     *     <li>校验账号格式</li>
     *     <li>调用策略的查询用户方法</li>
     * </ol>
     * </p>
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果用户存在：返回 {@code Result.success(userVO)}</li>
     *     <li>如果用户不存在：返回 {@code Result.success()}（data 为 null）</li>
     *     <li>如果查询失败：返回 {@code Result.fail(...)}</li>
     * </ul>
     * </p>
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>登录时查询用户信息</li>
     *     <li>需要根据账号查询用户，避免重复判断账号类型</li>
     * </ul>
     * </p>
     *
     * @param account 待处理的账号（手机号或邮箱等），不能为 null
     * @return 用户查询结果，如果用户不存在则返回成功结果但 data 为 null
     * @throws ServiceException 当无法识别账号格式或账号格式不符合要求时抛出异常，错误码为 {@code ResultCode.INVALID_PARA}
     */
    public AppUserVO validateAndFindUser(String account) {
        IAccountStrategy strategy = findHandler(account);
        strategy.validate(account);
        Result<AppUserVO> result = strategy.findUser(account);
        // 如果说返回回来的整个 result 都是空 或者 压根不等于 SUCCESS，说明系统内部出问题了
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new ServiceException(ResultCode.INVALID_PARA);
        }
        // 说明此时是 SUCCESS，但是 data 为 null
        if (result.getData() == null) {
            log.warn("用户不存在，请先注册: {}", account);
            return null;
        }
        return result.getData();
    }

    /**
     * 注册用户
     * <p>
     * 根据账号格式自动选择合适的策略并注册用户。<br>
     * 此方法会先校验账号格式，然后调用相应的注册接口。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>根据账号格式选择合适的策略</li>
     *     <li>校验账号格式</li>
     *     <li>调用策略的注册用户方法</li>
     * </ol>
     * </p>
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果注册成功：返回 {@code Result.success(userVO)}</li>
     *     <li>如果注册失败：返回 {@code Result.fail(...)} 或抛出异常</li>
     * </ul>
     * </p>
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>用户不存在时自动注册</li>
     *     <li>验证码登录时的自动注册功能</li>
     * </ul>
     * </p>
     *
     * @param account 待注册的账号（手机号或邮箱等），不能为 null
     * @return 用户注册结果，如果注册失败则返回失败结果或抛出异常
     * @throws ServiceException 当无法识别账号格式或账号格式不符合要求时抛出异常，错误码为 {@code ResultCode.INVALID_PARA}
     */
    public AppUserVO registerUser(String account) {
        IAccountStrategy strategy = findHandler(account);
        strategy.validate(account);
        Result<AppUserVO> result = strategy.registerUser(account);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            throw new ServiceException(ResultCode.INVALID_PARA);
        }
        return result.getData();
    }

    /**
     * 校验账号格式并查询用户，如果不存在则注册
     * <p>
     * 此方法会先校验账号格式，然后查询用户，如果用户不存在则自动注册。<br>
     * 实现"查询或注册"的一站式处理，避免业务代码重复判断。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>根据账号格式选择合适的策略</li>
     *     <li>校验账号格式</li>
     *     <li>调用策略的查询或注册方法（策略内部会先查询，不存在则注册）</li>
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
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>验证码登录（推荐使用此方法，实现查询或注册的一站式处理）</li>
     *     <li>需要确保用户存在，不存在则自动注册的场景</li>
     * </ul>
     * </p>
     *
     * @param account 待处理的账号（手机号或邮箱等），不能为 null
     * @return 用户信息结果，如果查询和注册都失败则返回失败结果或抛出异常
     * @throws ServiceException 当无法识别账号格式或账号格式不符合要求时抛出异常，错误码为 {@code ResultCode.INVALID_PARA}
     */
    public AppUserVO validateAndFindOrRegisterUser(String account) {
        IAccountStrategy strategy = findHandler(account);
        strategy.validate(account);
        Result<AppUserVO> result = strategy.findOrRegisterUser(account);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            log.error("用户查询或注册失败! 账号: {}, 结果: {}", account, result);
            throw new ServiceException("用户查询或注册失败");
        }
        return result.getData();
    }

    /**
     * 查找支持该账号格式的策略
     * <p>
     * 从所有注册的策略实现类中，查找支持指定账号格式的策略。<br>
     * 如果找不到匹配的策略，会抛出异常。
     * </p>
     *
     * @param account 账号（手机号或邮箱等），不能为 null
     * @return 账号策略，不会为 null
     * @throws ServiceException 如果没有找到匹配的策略，错误码为 {@code ResultCode.INVALID_PARA}，错误信息为"账号格式错误，请输入手机号或邮箱"
     */
    private IAccountStrategy findHandler(String account) {
        return accountStrategies.stream()
                .filter(h -> h.supports(account))
                .findFirst()
                .orElseThrow(() -> {
                            log.error("账号格式错误，请输入手机号或邮箱: {}", account);
                            return new ServiceException("账号格式错误，请输入手机号或邮箱", ResultCode.INVALID_PARA.getCode());
                        }
                );
    }
}