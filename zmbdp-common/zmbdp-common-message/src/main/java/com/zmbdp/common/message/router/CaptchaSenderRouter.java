package com.zmbdp.common.message.router;

import com.zmbdp.common.message.strategy.ICaptchaSenderStrategy;
import com.zmbdp.common.message.strategy.impl.AliSmsServiceStrategy;
import com.zmbdp.common.message.strategy.impl.EmailCodeServiceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 验证码发送路由器
 * <p>
 * 采用策略模式设计，负责根据账号类型自动路由到合适的验证码发送策略并执行发送逻辑。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li><b>策略路由：</b>根据账号类型（手机号/邮箱）自动选择合适的发送策略</li>
 *     <li><b>策略调用：</b>调用选中策略的发送方法，直接返回策略的执行结果</li>
 * </ul>
 * </p>
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *     <li>接收账号和验证码</li>
 *     <li>遍历所有注册的验证码发送策略实现类</li>
 *     <li>调用每个策略的 {@link ICaptchaSenderStrategy#supports(String)} 方法判断是否支持</li>
 *     <li>找到第一个返回 true 的发送策略</li>
 *     <li>调用该策略的 {@link ICaptchaSenderStrategy#sendCode(String, String)} 方法发送验证码</li>
 *     <li>直接返回策略的执行结果（结果透传，不做额外处理）</li>
 *     <li>如果所有策略都不支持，抛出异常</li>
 * </ol>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private CaptchaSenderRouter captchaSenderRouter;
 *
 * // 手机号账号 - 自动使用 AliSmsServiceStrategy 发送短信
 * boolean result = captchaSenderRouter.sendCode("13800138000", "123456");
 *
 * // 邮箱账号 - 自动使用 EmailCodeServiceStrategy 发送邮件
 * boolean result = captchaSenderRouter.sendCode("user@example.com", "123456");
 * }</pre>
 * </p>
 * <p>
 * <b>设计说明：</b>
 * <ul>
 *     <li>采用策略模式，根据账号类型自动选择合适的发送策略实现</li>
 *     <li>路由器只负责路由和调用，不处理业务逻辑，结果直接透传</li>
 *     <li>策略选择基于 {@link ICaptchaSenderStrategy#supports(String)} 方法</li>
 * </ul>
 * </p>
 * <p>
 * <b>扩展性：</b>
 * <ul>
 *     <li>如需支持新的账号类型（如微信、QQ等），只需实现 {@link ICaptchaSenderStrategy} 接口</li>
 *     <li>Spring 会自动注入新的发送策略实现，无需修改路由器代码</li>
 *     <li>符合开闭原则：对扩展开放，对修改关闭</li>
 * </ul>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有发送策略实现类必须标注 {@code @Component} 注解，以便 Spring 自动注入</li>
 *     <li>如果账号格式无法识别（既不是手机号也不是邮箱），抛出 {@link IllegalArgumentException} 异常</li>
 *     <li>发送策略按注入顺序遍历，找到第一个支持的策略即使用</li>
 *     <li>发送失败不会抛出异常，只返回 false</li>
 * </ul>
 * </p>
 *
 * @author 稚名不带撇
 * @see ICaptchaSenderStrategy
 */
@Slf4j
@Component
public class CaptchaSenderRouter {

    /**
     * 验证码发送器列表
     * <p>
     * Spring 会自动注入所有实现了 {@link ICaptchaSenderStrategy} 接口的 Bean。<br>
     * 当前包含的发送器：
     * <ul>
     *     <li>{@link AliSmsServiceStrategy}：手机号发送器</li>
     *     <li>{@link EmailCodeServiceStrategy}：邮箱发送器</li>
     * </ul>
     */
    @Autowired
    private List<ICaptchaSenderStrategy> captchaSenderStrategies;

    /**
     * 发送验证码
     * <p>
     * 根据账号类型自动选择合适的发送策略并发送验证码，结果直接透传。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>遍历所有注册的验证码发送策略实现类</li>
     *     <li>调用每个策略的 {@link ICaptchaSenderStrategy#supports(String)} 方法判断是否支持</li>
     *     <li>找到第一个返回 true 的发送策略</li>
     *     <li>调用该策略的 {@link ICaptchaSenderStrategy#sendCode(String, String)} 方法发送验证码</li>
     *     <li>直接返回策略的执行结果（结果透传，不做额外处理）</li>
     *     <li>如果所有策略都不支持，抛出异常</li>
     * </ol>
     * </p>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 手机号账号 - 自动使用 AliSmsServiceStrategy 发送短信
     * boolean result = captchaSenderRouter.sendCode("13800138000", "123456");
     *
     * // 邮箱账号 - 自动使用 EmailCodeServiceStrategy 发送邮件
     * boolean result = captchaSenderRouter.sendCode("user@example.com", "123456");
     * }</pre>
     * </p>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 account 为 null 或空字符串，可能抛出异常</li>
     *     <li>如果账号格式无法识别（既不是手机号也不是邮箱），抛出 {@link IllegalArgumentException} 异常</li>
     *     <li>发送策略按注入顺序遍历，找到第一个支持的策略即使用</li>
     *     <li>发送失败不会抛出异常，只返回 false</li>
     * </ul>
     * </p>
     *
     * @param account 账号（手机号或邮箱），不能为 null 或空字符串
     * @param code    验证码，不能为 null 或空字符串
     * @return true 表示发送成功，false 表示发送失败
     * @throws IllegalArgumentException 如果账号格式无法识别（既不是手机号也不是邮箱）
     * @see ICaptchaSenderStrategy#supports(String)
     * @see ICaptchaSenderStrategy#sendCode(String, String)
     */
    public boolean sendCode(String account, String code) {
        for (ICaptchaSenderStrategy sender : captchaSenderStrategies) {
            if (sender.supports(account)) {
                return sender.sendCode(account, code);
            }
        }
        throw new IllegalArgumentException("不支持的账号类型: " + account);
    }
}