package com.zmbdp.common.message.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 验证码发送器路由器
 * <p>
 * 根据账号类型自动选择合适的验证码发送器。
 * 遍历所有实现了 {@link ICaptchaSender} 接口的发送器，找到支持当前账号类型的发送器并调用。
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *     <li>接收账号和验证码</li>
 *     <li>遍历所有发送器，找到支持该账号类型的发送器</li>
 *     <li>调用发送器的 sendCode 方法发送验证码</li>
 *     <li>如果所有发送器都不支持，抛出异常</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 自动识别账号类型并发送验证码
 * boolean result = captchaSenderRouter.send("13800138000", "123456");
 *
 * // 邮箱账号
 * boolean result = captchaSenderRouter.send("user@example.com", "123456");
 * }</pre>
 * <p>
 * <b>扩展性：</b>
 * <ul>
 *     <li>如需支持新的账号类型（如微信、QQ等），只需实现 {@link ICaptchaSender} 接口</li>
 *     <li>Spring 会自动注入新的发送器实现，无需修改路由器代码</li>
 *     <li>符合开闭原则：对扩展开放，对修改关闭</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有发送器实现类必须标注 {@code @Component} 注解，以便 Spring 自动注入</li>
 *     <li>如果账号格式无法识别，抛出 IllegalArgumentException 异常</li>
 *     <li>发送器按注入顺序遍历，找到第一个支持的发送器即使用</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ICaptchaSender
 */
@Slf4j
@Component
public class CaptchaSenderRouter {

    /**
     * 验证码发送器列表
     * <p>
     * Spring 会自动注入所有实现了 {@link ICaptchaSender} 接口的 Bean。
     * 当前包含的发送器：
     * <ul>
     *     <li>{@link com.zmbdp.common.message.service.impl.AliSmsServiceImpl}：手机号发送器</li>
     *     <li>{@link com.zmbdp.common.message.service.impl.MailCodeServiceImpl}：邮箱发送器</li>
     * </ul>
     */
    @Autowired
    private List<ICaptchaSender> senders;

    /**
     * 发送验证码
     * <p>
     * 根据账号类型自动选择合适的发送器并发送验证码。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>遍历所有发送器实现类</li>
     *     <li>调用每个发送器的 {@link ICaptchaSender#supports(String)} 方法</li>
     *     <li>找到第一个返回 true 的发送器</li>
     *     <li>调用该发送器的 {@link ICaptchaSender#sendCode(String, String)} 方法发送验证码</li>
     *     <li>如果所有发送器都不支持，抛出异常</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 手机号账号
     * boolean result = captchaSenderRouter.send("13800138000", "123456");
     * // 自动使用 AliSmsServiceImpl 发送短信
     *
     * // 邮箱账号
     * boolean result = captchaSenderRouter.send("user@example.com", "123456");
     * // 自动使用 MailCodeServiceImpl 发送邮件
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 account 为 null 或空字符串，可能抛出异常</li>
     *     <li>如果账号格式无法识别（既不是手机号也不是邮箱），抛出 IllegalArgumentException</li>
     *     <li>发送器按注入顺序遍历，找到第一个支持的发送器即使用</li>
     *     <li>发送失败不会抛出异常，只返回 false</li>
     * </ul>
     *
     * @param account 账号（手机号或邮箱），不能为 null 或空字符串
     * @param code    验证码，不能为 null 或空字符串
     * @return true 表示发送成功，false 表示发送失败
     * @throws IllegalArgumentException 如果账号格式无法识别（既不是手机号也不是邮箱）
     * @see ICaptchaSender#supports(String)
     * @see ICaptchaSender#sendCode(String, String)
     */
    public boolean send(String account, String code) {
        for (ICaptchaSender sender : senders) {
            if (sender.supports(account)) {
                return sender.sendCode(account, code);
            }
        }
        throw new IllegalArgumentException("不支持的账号类型: " + account);
    }
}