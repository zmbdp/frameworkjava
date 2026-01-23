package com.zmbdp.common.message.strategy;

import com.zmbdp.common.message.strategy.impl.AliSmsServiceStrategy;
import com.zmbdp.common.message.strategy.impl.EmailCodeServiceStrategy;

/**
 * 验证码发送服务接口
 * <p>
 * 定义验证码发送的统一规范，支持多种发送方式（短信、邮件等）。
 * 每个实现类通过 {@link #supports(String)} 方法判断是否支持当前账号类型，
 * 路由器会根据账号类型自动选择合适的发送器实现。
 * <p>
 * <b>设计目的：</b>
 * <ul>
 *     <li>统一验证码发送接口，屏蔽不同发送方式的实现细节</li>
 *     <li>支持多种发送方式（短信、邮件等），易于扩展</li>
 *     <li>每个实现类自己判断是否支持当前账号类型</li>
 * </ul>
 * <p>
 * <b>实现类：</b>
 * <ul>
 *     <li>{@link AliSmsServiceStrategy}：阿里云短信服务实现（用于手机号）</li>
 *     <li>{@link EmailCodeServiceStrategy}：邮件验证码服务实现（用于邮箱）</li>
 * </ul>
 * <p>
 * <b>使用方式：</b>
 * <ul>
 *     <li>通过路由器获取：使用 {@link CaptchaSenderRouter#send(String, String)} 根据账号类型自动选择</li>
 *     <li>直接注入实现类：通过 {@code @Qualifier} 指定实现类</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 通过路由器自动选择发送器
 * boolean result = captchaSenderRouter.send("13800138000", "123456");
 *
 * // 直接注入指定实现类
 * @Autowired
 * @Qualifier("aliSmsService")
 * private ICaptchaSenderStrategy smsSender;
 * boolean result = smsSender.sendCode("13800138000", "123456");
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>每个实现类需要实现 supports 方法来判断是否支持当前账号类型</li>
 *     <li>发送失败不会抛出异常，只返回 false</li>
 *     <li>实现类需要标注 {@code @Component} 注解，并指定 Bean 名称</li>
 *     <li>建议通过 {@link CaptchaSenderRouter} 获取发送器</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see CaptchaSenderRouter
 * @see AliSmsServiceStrategy
 * @see EmailCodeServiceStrategy
 */
public interface ICaptchaSenderStrategy {

    /**
     * 是否支持当前账号类型
     * <p>
     * 判断当前发送器是否支持指定的账号类型（如手机号、邮箱等）。
     * 每个实现类根据自己的业务逻辑判断是否支持该账号。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 判断是否支持手机号
     * boolean supports = aliSmsService.supports("13800138000"); // true
     * boolean supports = mailCodeService.supports("13800138000"); // false
     *
     * // 判断是否支持邮箱
     * boolean supports = mailCodeService.supports("user@example.com"); // true
     * boolean supports = aliSmsService.supports("user@example.com"); // false
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 account 为 null 或空字符串，应返回 false</li>
     *     <li>实现类应该使用 {@link com.zmbdp.common.core.utils.VerifyUtil} 进行格式验证</li>
     *     <li>此方法用于路由器自动选择合适的发送器</li>
     * </ul>
     *
     * @param account 账号（手机号或邮箱等），不能为 null
     * @return true 表示支持该账号类型，false 表示不支持
     */
    boolean supports(String account);

    /**
     * 发送验证码
     * <p>
     * 向指定账号（手机号或邮箱）发送验证码。
     * 根据实现类的不同，支持短信和邮件两种发送方式。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 发送短信验证码（手机号）
     * boolean result = aliSmsService.sendCode("13800138000", "123456");
     *
     * // 发送邮件验证码（邮箱）
     * boolean result = mailCodeService.sendCode("user@example.com", "123456");
     * }</pre>
     * <p>
     * <b>实现类说明：</b>
     * <ul>
     *     <li>{@link AliSmsServiceStrategy}：account 应为手机号</li>
     *     <li>{@link EmailCodeServiceStrategy}：account 应为邮箱地址</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 account 为 null 或空字符串，可能抛出异常或返回 false</li>
     *     <li>如果 code 为 null 或空字符串，可能发送失败</li>
     *     <li>发送失败不会抛出异常，只返回 false</li>
     *     <li>不同实现类可能有不同的配置要求（如短信需要模板代码、邮件需要 SMTP 配置等）</li>
     *     <li>某些实现类可能支持发送开关配置（开发环境可关闭实际发送）</li>
     * </ul>
     *
     * @param account 账号（手机号或邮箱），不能为 null 或空字符串
     * @param code    验证码，不能为 null 或空字符串
     * @return true 表示发送成功，false 表示发送失败
     */
    boolean sendCode(String account, String code);
}