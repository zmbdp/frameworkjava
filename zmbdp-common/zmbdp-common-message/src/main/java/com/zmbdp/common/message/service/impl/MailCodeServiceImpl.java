package com.zmbdp.common.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.zmbdp.common.core.utils.MailUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.message.config.MailCodeProperties;
import com.zmbdp.common.message.service.ICaptchaSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 邮件验证码服务实现
 * <p>
 * 实现 {@link ICaptchaSender} 接口，提供基于邮件的验证码发送功能。
 * 支持通过邮件发送验证码，支持随机选择邮件标题和内容模板。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>发送邮件验证码：通过邮件发送验证码到用户邮箱</li>
 *     <li>随机模板选择：从配置的标题和内容模板列表中随机选择，提高多样性</li>
 *     <li>发送开关控制：支持通过配置控制是否实际发送邮件（开发/测试环境可关闭）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 方式1：通过路由器自动选择发送器（推荐）
 * @Autowired
 * private CaptchaSenderRouter captchaSenderRouter;
 * boolean result = captchaSenderRouter.send("user@example.com", "123456");
 *
 * // 方式2：直接注入指定实现类
 * @Autowired
 * @Qualifier("mailCodeService")
 * private ICaptchaSender mailSender;
 * boolean result = mailSender.sendCode("user@example.com", "123456");
 * }</pre>
 * <p>
 * <b>配置说明：</b>
 * <ul>
 *     <li>mail.code.subject：邮件标题列表（YAML 列表格式，可选）</li>
 *     <li>mail.code.content：邮件内容模板列表（支持 {code} 占位符，YAML 列表格式，可选）</li>
 *     <li>captcha.send-message：是否发送邮件（默认 false，开发环境建议关闭）</li>
 * </ul>
 * <p>
 * <b>邮件模板配置示例：</b>
 * <pre>{@code
 * mail:
 *   code:
 *     subject:
 *       - "验证码通知"
 *       - "您的验证码"
 *     content:
 *       - "您的验证码是：{code}，请勿泄露给他人。"
 *       - "验证码：{code}，有效期 5 分钟。"
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>支持配置热更新（@RefreshScope）</li>
 *     <li>如果 sendMessage 为 false，不会实际发送邮件，直接返回 false</li>
 *     <li>邮件标题和内容模板支持配置多个，会随机选择</li>
 *     <li>内容模板中的 {code} 占位符会被实际验证码替换</li>
 *     <li>如果未配置标题或内容模板，使用默认值</li>
 *     <li>发送失败会记录错误日志，但不会抛出异常</li>
 *     <li>需要配置邮件服务器信息（SMTP 服务器、用户名、授权码等）</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.message.service.ICaptchaSender
 * @see com.zmbdp.common.message.config.MailCodeProperties
 * @see com.zmbdp.common.core.utils.MailUtil
 */
@Slf4j
@RefreshScope
@Component("mailCodeService")
public class MailCodeServiceImpl implements ICaptchaSender {

    /**
     * 随机数生成器
     * <p>
     * 用于从邮件标题和内容模板列表中随机选择元素。
     * 使用静态实例，避免频繁创建 Random 对象。
     */
    private static final Random RANDOM = new Random();

    /**
     * 邮件验证码配置属性
     * <p>
     * 包含邮件标题列表和内容模板列表。
     * 支持从配置文件中读取多个标题和内容模板，发送时会随机选择。
     */
    @Autowired
    private MailCodeProperties mailCodeProperties;

    /**
     * 是否发送邮件
     * <p>
     * 配置项：captcha.send-message，默认值为 false。
     * <ul>
     *     <li>true：实际发送邮件到用户邮箱（生产环境）</li>
     *     <li>false：不发送邮件，直接返回 false（开发/测试环境）</li>
     * </ul>
     * 此配置用于控制是否实际调用邮件发送服务。
     * <p>
     * <b>注意：</b>此配置项与 {@link com.zmbdp.common.message.service.CaptchaService} 中的 sendMessage 配置项名称相同，
     * 但作用域不同。此配置仅控制邮件发送，不影响短信发送。
     */
    @Value("${captcha.send-message:false}")
    private boolean sendMessage;

    /**
     * 是否支持当前账号类型
     * <p>
     * 判断当前发送器是否支持邮箱账号类型。
     * 使用 {@link com.zmbdp.common.core.utils.VerifyUtil#checkEmail(String)} 验证邮箱格式。
     *
     * @param account 账号，不能为 null
     * @return true 表示账号是邮箱格式，false 表示不是
     */
    @Override
    public boolean supports(String account) {
        return VerifyUtil.checkEmail(account);
    }

    /**
     * 发送验证码
     * <p>
     * 通过邮件发送验证码到指定邮箱地址。
     * 从配置的标题和内容模板列表中随机选择，替换验证码占位符后发送。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 通过注入的 Bean 发送验证码
     * @Autowired
     * @Qualifier("mailCodeService")
     * private ICaptchaSender mailSender;
     * boolean result = mailSender.sendCode("user@example.com", "123456");
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>检查 sendMessage 配置，如果为 false 则直接返回 false</li>
     *     <li>从配置的标题列表中随机选择一个标题（如果未配置，使用默认值）</li>
     *     <li>从配置的内容模板列表中随机选择一个模板（如果未配置，使用默认值）</li>
     *     <li>将内容模板中的 {code} 占位符替换为实际验证码</li>
     *     <li>调用 {@link com.zmbdp.common.core.utils.MailUtil#sendHtml(String, String, String, File...)} 发送 HTML 邮件</li>
     *     <li>返回发送结果</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 email 为 null 或空字符串，可能抛出异常</li>
     *     <li>如果 code 为 null 或空字符串，可能发送失败</li>
     *     <li>如果 sendMessage 为 false，不会实际发送邮件，直接返回 false</li>
     *     <li>发送失败不会抛出异常，只返回 false 并记录错误日志</li>
     *     <li>邮件标题和内容模板支持配置多个，会随机选择以提高多样性</li>
     *     <li>内容模板中的 {code} 占位符会被实际验证码替换</li>
     *     <li>如果未配置标题或内容模板，使用默认值（"验证码" 和 "您的验证码是：{code}，请勿泄露给他人。"）</li>
     * </ul>
     *
     * @param email 邮箱地址，不能为 null 或空字符串
     * @param code  验证码，不能为 null 或空字符串
     * @return true 表示发送成功，false 表示发送失败
     * @see #getRandomItem(List, String)
     * @see com.zmbdp.common.core.utils.MailUtil#sendHtml(String, String, String, File...)
     */
    @Override
    public boolean sendCode(String email, String code) {
        // 把是否发送邮件交给 nacos 管理
        if (!sendMessage) {
            log.error("邮件发送通道关闭, {}", email);
            return false;
        }
        // 从列表中随机选择一个标题
        String subject = getRandomItem(mailCodeProperties.getSubject(), "验证码");
        // 从列表中随机选择一个内容模板
        String contentTemplate = getRandomItem(mailCodeProperties.getContent(), "您的验证码是：{code}，请勿泄露给他人。");
        // 构建邮件内容（替换占位符）
        String content = contentTemplate.replace("{code}", code);
        // 发送邮件
        try {
            MailUtil.sendHtml(email, subject, content);
            return true;
        } catch (Exception e) {
            Map<String, Object> logInfo = new HashMap<>();
            logInfo.put("to", email);
            logInfo.put("subject", subject);
            log.error("邮件: {} 发送失败, 失败原因: {}...", new Gson().toJson(logInfo), e.getMessage());
            return false;
        }
    }

    /**
     * 从列表中随机选择一个元素
     * <p>
     * 从配置的标题或内容模板列表中随机选择一个元素。
     * 如果列表为空或无效，返回默认值。
     * <p>
     * <b>处理逻辑：</b>
     * <ol>
     *     <li>如果列表为 null 或空，返回默认值</li>
     *     <li>对列表进行处理：去除空白字符，过滤空字符串</li>
     *     <li>如果处理后的列表为空，返回默认值</li>
     *     <li>如果列表只有一个元素，直接返回该元素</li>
     *     <li>如果列表有多个元素，随机选择一个索引并返回对应元素</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 从标题列表中随机选择
     * List<String> subjects = Arrays.asList("验证码通知", "您的验证码");
     * String subject = getRandomItem(subjects, "验证码");
     * // 可能返回 "验证码通知" 或 "您的验证码"
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>列表中的空白字符会被自动去除</li>
     *     <li>空字符串会被过滤掉</li>
     *     <li>如果所有元素都被过滤掉，返回默认值</li>
     *     <li>随机选择使用 {@link Random#nextInt(int)} 方法</li>
     *     <li>如果列表只有一个有效元素，直接返回，不进行随机选择</li>
     * </ul>
     *
     * @param list         字符串列表，可以为 null 或空列表
     * @param defaultValue 默认值，当列表为空或无效时使用
     * @return 随机选择的元素，如果列表为空或无效则返回默认值
     * @see java.util.Random#nextInt(int)
     */
    private String getRandomItem(List<String> list, String defaultValue) {
        if (list == null || list.isEmpty()) {
            return defaultValue;
        }
        // 处理列表：去除空白字符，过滤空字符串
        List<String> processedList = list.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .toList();

        if (processedList.isEmpty()) {
            return defaultValue;
        }
        // 如果列表只有一个元素，直接返回
        if (processedList.size() == 1) {
            return processedList.get(0);
        }
        // 随机选择一个索引
        int index = RANDOM.nextInt(processedList.size());
        return processedList.get(index);
    }
}