package com.zmbdp.portal.service.user.strategy.account;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVO;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.portal.service.user.manager.AccountManager;
import com.zmbdp.portal.service.user.strategy.account.impl.EmailAccountStrategy;
import com.zmbdp.portal.service.user.strategy.account.impl.PhoneAccountStrategy;

/**
 * 账号策略接口
 * <p>
 * 采用策略模式设计，支持多种账号格式的处理（如手机号、邮箱等）。<br>
 * 每个具体策略实现类负责一种账号格式的校验、查询、注册等逻辑。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>账号格式校验：验证账号是否符合格式要求</li>
 *     <li>用户查询：根据账号类型查询用户信息</li>
 *     <li>用户注册：根据账号类型注册新用户</li>
 *     <li>查询或注册：查询用户，如果不存在则自动注册（一站式处理）</li>
 * </ul>
 * </p>
 * <p>
 * <b>实现类：</b>
 * <ul>
 *     <li>{@link PhoneAccountStrategy}：手机号处理策略</li>
 *     <li>{@link EmailAccountStrategy}：邮箱处理策略</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 通过 AccountManager 自动选择合适的策略
 * @Autowired
 * private AccountManager accountStrategyContext;
 * Result<AppUserVO> result = accountStrategyContext.validateAndFindUser(account);
 * }</pre>
 * </p>
 *
 * @author 稚名不带撇
 * @see AccountManager
 * @see PhoneAccountStrategy
 * @see EmailAccountStrategy
 */
public interface IAccountStrategy {

    /**
     * 判断当前策略是否支持该账号格式的处理
     * <p>
     * 此方法用于策略选择阶段，由策略上下文（{@link AccountManager}）调用。<br>
     * 用于从多个策略实现中筛选出合适的策略。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>策略上下文遍历所有策略实现类</li>
     *     <li>调用每个策略的 {@code supports()} 方法</li>
     *     <li>选择第一个返回 {@code true} 的策略</li>
     * </ol>
     * </p>
     *
     * @param account 待处理的账号（手机号或邮箱等），不能为 null
     * @return true 表示支持此账号格式的处理，false 表示不支持
     */
    boolean supports(String account);

    /**
     * 执行账号格式校验
     * <p>
     * 此方法用于实际校验阶段，对账号格式进行验证。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>调用此方法前，应确保 {@link #supports(String)} 返回 true</li>
     *     <li>如果账号格式不正确，会抛出 {@link ServiceException}</li>
     *     <li>此方法会进行二次校验，确保账号格式正确</li>
     * </ul>
     * </p>
     *
     * @param account 待校验的账号（手机号或邮箱等），不能为 null
     * @throws ServiceException 当账号格式不符合要求时抛出异常，错误码为 {@code ResultCode.INVALID_PARA}
     */
    void validate(String account);

    /**
     * 查询用户
     * <p>
     * 根据账号类型查询用户信息（手机号或邮箱）。<br>
     * 此方法会在校验账号格式后调用，确保账号格式正确。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果用户存在：返回 {@code Result.success(userVO)}</li>
     *     <li>如果用户不存在：返回 {@code Result.success()}（data 为 null）</li>
     *     <li>如果查询失败：返回 {@code Result.fail(...)}</li>
     * </ul>
     * </p>
     *
     * @param account 账号（手机号或邮箱等），不能为 null，且应已通过格式校验
     * @return 用户查询结果，如果用户不存在则返回成功结果但 data 为 null
     */
    Result<AppUserVO> findUser(String account);

    /**
     * 注册用户
     * <p>
     * 根据账号类型注册新用户（手机号或邮箱）。<br>
     * 此方法会在用户不存在时调用，用于自动注册功能。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>如果注册成功：返回 {@code Result.success(userVO)}</li>
     *     <li>如果注册失败：返回 {@code Result.fail(...)} 或抛出异常</li>
     * </ul>
     * </p>
     *
     * @param account 账号（手机号或邮箱等），不能为 null，且应已通过格式校验
     * @return 用户注册结果，如果注册失败则返回失败结果或抛出异常
     */
    Result<AppUserVO> registerUser(String account);

    /**
     * 查询用户，如果不存在则注册
     * <p>
     * 此方法会先查询用户，如果用户不存在则自动注册新用户。<br>
     * 常用于登录场景，实现"查询或注册"的一站式处理。
     * <p>
     * <b>工作流程：</b>
     * <ol>
     *     <li>调用 {@link #findUser(String)} 查询用户</li>
     *     <li>如果用户存在，直接返回查询结果</li>
     *     <li>如果用户不存在，调用 {@link #registerUser(String)} 注册用户</li>
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
     * @param account 账号（手机号或邮箱等），不能为 null，且应已通过格式校验
     * @return 用户信息结果，如果查询和注册都失败则返回失败结果或抛出异常
     */
    Result<AppUserVO> findOrRegisterUser(String account);
}