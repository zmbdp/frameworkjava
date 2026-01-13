package com.zmbdp.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zmbdp.common.core.config.MailAccount;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件工具类（基于 Spring JavaMailSender + Jakarta Mail）<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>基于 Spring 容器中的 {@link MailAccount} 和 {@link JavaMailSender} 发送邮件</li>
 *     <li>支持文本 / HTML 邮件</li>
 *     <li>支持抄送（CC）、密送（BCC）</li>
 *     <li>支持附件、内嵌图片（cid）</li>
 *     <li>支持自定义 MailAccount 或使用全局配置</li>
 * </ul>
 *
 * <p>
 * 使用前准备：
 * <ol>
 *     <li>在 Spring 容器中注册 {@link MailAccount} 和 {@link JavaMailSender} Bean</li>
 *     <li>引入 zmbdp-common-core 相关依赖</li>
 * </ol>
 * <p>
 * 示例：
 * <pre>
 * MailUtil.sendHtml("a@b.com", "标题", "&lt;h1&gt;内容&lt;/h1&gt;");
 * </pre>
 * <p>
 * 工具类说明：
 * <ul>
 *     <li>不允许实例化</li>
 *     <li>所有方法均为静态方法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MailUtil {

    /**
     * 从 Spring 容器获取默认的 JavaMailSender
     */
    private static JavaMailSender getDefaultJavaMailSender() {
        return SpringUtil.getBean(JavaMailSender.class);
    }

    /**
     * 根据 MailAccount 创建 JavaMailSender
     *
     * @param mailAccount 邮件账号配置
     * @return JavaMailSender 邮件发送器
     */
    private static JavaMailSender createJavaMailSender(MailAccount mailAccount) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailAccount.getHost());
        mailSender.setPort(mailAccount.getPort());
        mailSender.setUsername(mailAccount.getUser());
        mailSender.setPassword(mailAccount.getPass());

        Properties props = mailAccount.getSmtpProps();
        mailSender.setJavaMailProperties(props);

        return mailSender;
    }

    /**
     * 获取默认邮件账号
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要获取 Spring 容器中配置的默认邮件账号</li>
     *     <li>需要查看当前使用的邮件账号配置信息</li>
     *     <li>需要在其他方法中复用默认账号配置</li>
     * </ul>
     *
     * @return MailAccount 邮件账号配置对象，包含 SMTP 服务器、用户名、授权码等信息
     */
    public static MailAccount getMailAccount() {
        return SpringUtil.getBean(MailAccount.class);
    }

    /**
     * 在默认 MailAccount 的基础上，创建新的 MailAccount 并覆盖发件人信息
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要临时修改发件人显示名称、账号或授权码</li>
     *     <li>需要在保持其他配置不变的情况下更换发件人</li>
     *     <li>参数为空时会保留原有配置值</li>
     *     <li>需要线程安全的邮件账号配置（返回新对象，不修改全局配置）</li>
     * </ul>
     *
     * @param from 发件人显示名称，为空时保留原值
     * @param user 邮箱账号，为空时保留原值
     * @param pass 邮箱授权码，为空时保留原值
     * @return MailAccount 新的邮件账号配置对象（不修改全局配置）
     */
    public static MailAccount getMailAccount(String from, String user, String pass) {
        MailAccount account = new MailAccount();
        MailAccount defaultAccount = getMailAccount();
        // 使用 BeanCopyUtil 复制属性
        BeanCopyUtil.copyProperties(defaultAccount, account);

        // 覆盖新值（如果参数为空，使用已复制的 account 中的值）
        account.setFrom(StrUtil.blankToDefault(from, account.getFrom()));
        account.setUser(StrUtil.blankToDefault(user, account.getUser()));
        account.setPass(StrUtil.blankToDefault(pass, account.getPass()));
        return account;
    }


    /* ========================= 基于默认 MailAccount 的快捷方法 ========================= */

    /**
     * 发送普通文本邮件（支持附件）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送纯文本格式的邮件</li>
     *     <li>收件人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to      收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject 邮件标题
     * @param content 邮件正文内容（纯文本格式）
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendText(String to, String subject, String content, File... files) {
        return send(to, subject, content, false, files);
    }

    /**
     * 发送 HTML 邮件（支持附件）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送 HTML 格式的邮件（支持富文本、样式等）</li>
     *     <li>收件人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to      收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject 邮件标题
     * @param content HTML 格式的邮件正文内容
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendHtml(String to, String subject, String content, File... files) {
        return send(to, subject, content, true, files);
    }

    /**
     * 发送邮件（指定是否为 HTML 格式）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>收件人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to      收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject 邮件标题
     * @param content 邮件正文内容（文本或 HTML）
     * @param isHtml  是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(String to, String subject, String content, boolean isHtml, File... files) {
        return send(splitAddress(to), subject, content, isHtml, files);
    }

    /**
     * 发送邮件（支持抄送、密送）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要设置抄送（CC）和密送（BCC）收件人</li>
     *     <li>收件人、抄送人、密送人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to      收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param cc      抄送人邮箱地址，多个抄送人可使用 "," 或 ";" 分隔，可为空
     * @param bcc     密送人邮箱地址，多个密送人可使用 "," 或 ";" 分隔，可为空
     * @param subject 邮件标题
     * @param content 邮件正文内容（文本或 HTML）
     * @param isHtml  是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(String to, String cc, String bcc, String subject, String content, boolean isHtml, File... files) {
        return send(splitAddress(to), splitAddress(cc), splitAddress(bcc), subject, content, isHtml, files);
    }

    /**
     * 发送文本邮件（多收件人）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送纯文本格式的邮件</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos     收件人邮箱地址集合
     * @param subject 邮件标题
     * @param content 邮件正文内容（纯文本格式）
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendText(Collection<String> tos, String subject, String content, File... files) {
        return send(tos, subject, content, false, files);
    }

    /**
     * 发送 HTML 邮件（多收件人）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送 HTML 格式的邮件（支持富文本、样式等）</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos     收件人邮箱地址集合
     * @param subject 邮件标题
     * @param content HTML 格式的邮件正文内容
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendHtml(Collection<String> tos, String subject, String content, File... files) {
        return send(tos, subject, content, true, files);
    }

    /**
     * 发送邮件（无抄送、密送）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>不需要设置抄送和密送</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos     收件人邮箱地址集合
     * @param subject 邮件标题
     * @param content 邮件正文内容（文本或 HTML）
     * @param isHtml  是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(Collection<String> tos, String subject, String content, boolean isHtml, File... files) {
        return send(tos, null, null, subject, content, isHtml, files);
    }

    /**
     * 发送邮件（完整参数，使用默认 MailAccount）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要设置完整的邮件信息（收件人、抄送、密送）</li>
     *     <li>收件人、抄送人、密送人已经以集合形式准备好</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos     收件人邮箱地址集合
     * @param ccs     抄送人邮箱地址集合，可以为 null 或空
     * @param bccs    密送人邮箱地址集合，可以为 null 或空
     * @param subject 邮件标题
     * @param content 邮件正文内容（文本或 HTML）
     * @param isHtml  是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files   邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            Collection<String> tos, Collection<String> ccs, Collection<String> bccs,
            String subject, String content, boolean isHtml, File... files
    ) {
        return send(getMailAccount(), true, tos, ccs, bccs, subject, content, null, null, isHtml, files);
    }


    /* ========================= 带图片（cid）的快捷方法 ========================= */

    /**
     * 发送 HTML 邮件（支持内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送 HTML 格式的邮件，正文中包含内嵌图片</li>
     *     <li>收件人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>图片通过 cid 方式内嵌到邮件正文中</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to       收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject  邮件标题
     * @param content  HTML 格式的邮件正文内容，图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap 内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param files    邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendHtml(
            String to, String subject, String content,
            Map<String, InputStream> imageMap, File... files
    ) {
        return send(to, subject, content, imageMap, true, files);
    }

    /**
     * 发送 HTML 邮件（支持内嵌图片，可指定图片格式）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送 HTML 格式的邮件，正文中包含内嵌图片</li>
     *     <li>需要手动指定图片的 MIME 类型（如 image/png、image/jpeg 等）</li>
     *     <li>收件人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>图片通过 cid 方式内嵌到邮件正文中</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to                 收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject            邮件标题
     * @param content            HTML 格式的邮件正文内容，图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap           内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param imageContentTypeMap 图片格式映射，key 为 cid，value 为 MIME 类型（如 "image/png"、"image/jpeg"），如果为 null 或某个 cid 不存在，则自动检测格式
     * @param files              邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendHtml(
            String to, String subject, String content,
            Map<String, InputStream> imageMap, Map<String, String> imageContentTypeMap, File... files
    ) {
        return send(getMailAccount(), true, splitAddress(to), null, null, subject, content, imageMap, imageContentTypeMap, true, files);
    }

    /**
     * 发送邮件（支持内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>邮件正文中包含内嵌图片（cid 方式）</li>
     *     <li>收件人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to       收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject  邮件标题
     * @param content  邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap 内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml   是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files    邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            String to, String subject, String content,
            Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(getMailAccount(), true, splitAddress(to), null, null, subject, content, imageMap, null, isHtml, files);
    }

    /**
     * 发送邮件（支持抄送、密送、内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要设置抄送（CC）和密送（BCC）收件人</li>
     *     <li>邮件正文中包含内嵌图片（cid 方式）</li>
     *     <li>收件人、抄送人、密送人可以是单个或多个（使用逗号或分号分隔）</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param to       收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param cc       抄送人邮箱地址，多个抄送人可使用 "," 或 ";" 分隔，可为空
     * @param bcc      密送人邮箱地址，多个密送人可使用 "," 或 ";" 分隔，可为空
     * @param subject  邮件标题
     * @param content  邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap 内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml   是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files    邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            String to, String cc, String bcc, String subject, String content,
            Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(getMailAccount(), true, splitAddress(to), splitAddress(cc), splitAddress(bcc), subject, content, imageMap, null, isHtml, files);
    }

    /**
     * 发送 HTML 邮件（多收件人 + 内嵌图片，自动检测图片格式）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送 HTML 格式的邮件，正文中包含内嵌图片</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>图片通过 cid 方式内嵌到邮件正文中</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos      收件人邮箱地址集合
     * @param subject  邮件标题
     * @param content  HTML 格式的邮件正文内容，图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap 内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param files    邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendHtml(
            Collection<String> tos, String subject, String content,
            Map<String, InputStream> imageMap, File... files
    ) {
        return send(getMailAccount(), true, tos, null, null, subject, content, imageMap, null, true, files);
    }

    /**
     * 发送 HTML 邮件（多收件人 + 内嵌图片，手动指定图片格式）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>发送 HTML 格式的邮件，正文中包含内嵌图片</li>
     *     <li>需要手动指定图片的 MIME 类型（如 image/png、image/jpeg 等）</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>图片通过 cid 方式内嵌到邮件正文中</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos                收件人邮箱地址集合
     * @param subject           邮件标题
     * @param content           HTML 格式的邮件正文内容，图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap          内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param imageContentTypeMap 图片格式映射，key 为 cid，value 为 MIME 类型（如 "image/png"、"image/jpeg"），如果为 null 或某个 cid 不存在，则自动检测格式
     * @param files             邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String sendHtml(
            Collection<String> tos, String subject, String content,
            Map<String, InputStream> imageMap, Map<String, String> imageContentTypeMap, File... files
    ) {
        return send(getMailAccount(), true, tos, null, null, subject, content, imageMap, imageContentTypeMap, true, files);
    }

    /**
     * 发送邮件（多收件人 + 内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>邮件正文中包含内嵌图片（cid 方式）</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>不需要设置抄送和密送</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos      收件人邮箱地址集合
     * @param subject  邮件标题
     * @param content  邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap 内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml   是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files    邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            Collection<String> tos, String subject, String content,
            Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(getMailAccount(), true, tos, null, null, subject, content, imageMap, null, isHtml, files);
    }

    /**
     * 发送邮件（完整参数 + 内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要设置完整的邮件信息（收件人、抄送、密送）</li>
     *     <li>邮件正文中包含内嵌图片（cid 方式）</li>
     *     <li>收件人、抄送人、密送人已经以集合形式准备好</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     *     <li>使用 Spring 容器中配置的默认邮件账号</li>
     * </ul>
     *
     * @param tos      收件人邮箱地址集合
     * @param ccs      抄送人邮箱地址集合，可以为 null 或空
     * @param bccs     密送人邮箱地址集合，可以为 null 或空
     * @param subject  邮件标题
     * @param content  邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap 内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml   是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files    邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            Collection<String> tos, Collection<String> ccs, Collection<String> bccs, String subject,
            String content, Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(getMailAccount(), true, tos, ccs, bccs, subject, content, imageMap, null, isHtml, files);
    }

    /* ========================= 传入自定义 MailAccount 的方法 ========================= */

    /**
     * 使用指定 MailAccount 发送邮件
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要临时指定邮件账号（非 Spring 容器中的默认账号）</li>
     *     <li>收件人以字符串形式传入（支持逗号或分号分隔）</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（包含 SMTP、用户名、授权码等信息）
     * @param to          收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject     邮件标题
     * @param content     邮件正文内容（文本或 HTML）
     * @param isHtml      是否为 HTML 邮件，true 表示 HTML，false 表示纯文本
     * @param files       邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            MailAccount mailAccount, String to, String subject,
            String content, boolean isHtml, File... files
    ) {
        return send(mailAccount, splitAddress(to), subject, content, isHtml, files);
    }

    /**
     * 使用指定 MailAccount 发送邮件（多收件人）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要临时指定邮件账号（非 Spring 容器中的默认账号）</li>
     *     <li>收件人已经以集合形式准备好</li>
     *     <li>不需要设置抄送和密送</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（包含 SMTP、用户名、授权码等信息）
     * @param tos         收件人邮箱地址集合
     * @param subject     邮件标题
     * @param content     邮件正文内容（文本或 HTML）
     * @param isHtml      是否为 HTML 邮件，true 表示 HTML，false 表示纯文本
     * @param files       邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            MailAccount mailAccount, Collection<String> tos, String subject,
            String content, boolean isHtml, File... files
    ) {
        return send(mailAccount, tos, null, null, subject, content, isHtml, files);
    }

    /**
     * 使用指定 MailAccount 发送邮件（完整参数）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要临时指定邮件账号（非 Spring 容器中的默认账号）</li>
     *     <li>需要设置完整的邮件信息（收件人、抄送、密送）</li>
     *     <li>收件人、抄送人、密送人已经以集合形式准备好</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（包含 SMTP、用户名、授权码等信息）
     * @param tos         收件人邮箱地址集合
     * @param ccs         抄送人邮箱地址集合，可以为 null 或空
     * @param bccs        密送人邮箱地址集合，可以为 null 或空
     * @param subject     邮件标题
     * @param content     邮件正文内容（文本或 HTML）
     * @param isHtml      是否为 HTML 邮件，true 表示 HTML，false 表示纯文本
     * @param files       邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            MailAccount mailAccount, Collection<String> tos, Collection<String> ccs,
            Collection<String> bccs, String subject, String content, boolean isHtml, File... files
    ) {
        return send(mailAccount, false, tos, ccs, bccs, subject, content, null, null, isHtml, files);
    }

    /**
     * 使用指定的 MailAccount 发送邮件（支持内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要临时指定邮件账号（非 Spring 容器中的默认账号）</li>
     *     <li>收件人以字符串形式传入（支持逗号或分号分隔）</li>
     *     <li>邮件内容中包含内嵌图片（cid 方式）</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（包含 SMTP、用户名、授权码等信息）
     * @param to          收件人邮箱地址，多个收件人可使用 "," 或 ";" 分隔
     * @param subject     邮件标题
     * @param content     邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap    内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml      是否为 HTML 邮件，true 表示 HTML，false 表示纯文本
     * @param files       邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            MailAccount mailAccount, String to, String subject, String content,
            Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(mailAccount, false, splitAddress(to), null, null, subject, content, imageMap, null, isHtml, files);
    }

    /**
     * 使用指定的 MailAccount 发送邮件（支持内嵌图片，不包含抄送与密送）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要使用自定义 {@link MailAccount} 发送邮件</li>
     *     <li>收件人以集合形式传入</li>
     *     <li>邮件正文中包含内嵌图片（cid 方式）</li>
     *     <li>不需要设置抄送（CC）和密送（BCC）</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（SMTP 信息、用户名、授权码等）
     * @param tos         收件人邮箱地址集合
     * @param subject     邮件标题
     * @param content     邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap    内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml      是否为 HTML 邮件，true 表示 HTML，false 表示纯文本
     * @param files       邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            MailAccount mailAccount, Collection<String> tos, String subject, String content,
            Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(mailAccount, false, tos, null, null, subject, content, imageMap, null, isHtml, files);
    }

    /**
     * 使用指定的 MailAccount 发送邮件（完整参数 + 内嵌图片）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要使用自定义 {@link MailAccount} 发送邮件</li>
     *     <li>需要设置完整的邮件信息（收件人、抄送、密送）</li>
     *     <li>邮件正文中包含内嵌图片（cid 方式）</li>
     *     <li>收件人、抄送人、密送人已经以集合形式准备好</li>
     *     <li>需要根据业务逻辑动态决定发送文本或 HTML 邮件</li>
     *     <li>需要附带文件附件</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（SMTP 信息、用户名、授权码等）
     * @param tos         收件人邮箱地址集合
     * @param ccs         抄送人邮箱地址集合，可以为 null 或空
     * @param bccs        密送人邮箱地址集合，可以为 null 或空
     * @param subject     邮件标题
     * @param content     邮件正文内容（文本或 HTML），图片占位符格式为 cid:$IMAGE_PLACEHOLDER
     * @param imageMap    内嵌图片映射，key 为 cid（对应 content 中的占位符），value 为图片输入流
     * @param isHtml      是否为 HTML 邮件，true 表示 HTML，false 表示纯文本
     * @param files       邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    public static String send(
            MailAccount mailAccount, Collection<String> tos, Collection<String> ccs, Collection<String> bccs,
            String subject, String content, Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        return send(mailAccount, false, tos, ccs, bccs, subject, content, imageMap, null, isHtml, files);
    }


    /* ========================= Session 获取 ========================= */

    /**
     * 根据 MailAccount 获取 JavaMail Session
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要获取 JavaMail Session 对象进行底层邮件操作</li>
     *     <li>需要自定义邮件发送流程</li>
     *     <li>需要复用 Session 以提高性能（isSingleton 为 true 时）</li>
     *     <li>需要为每个请求创建独立的 Session（isSingleton 为 false 时）</li>
     * </ul>
     *
     * @param mailAccount 邮件账号配置（包含 SMTP、用户名、授权码等信息）
     * @param isSingleton 是否使用单例 Session，true 表示使用全局默认 Session，false 表示创建新 Session
     * @return Session JavaMail Session 对象，用于邮件发送操作
     */
    public static Session getSession(MailAccount mailAccount, boolean isSingleton) {
        Authenticator authenticator = null;

        if (mailAccount.isAuth()) {
            final String user = mailAccount.getUser();
            final String pass = mailAccount.getPass();
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            };
        }

        return isSingleton ?
                Session.getDefaultInstance(mailAccount.getSmtpProps(), authenticator) :
                Session.getInstance(mailAccount.getSmtpProps(), authenticator);
    }


    /* ========================= 私有底层发送逻辑 ========================= */

    /**
     * 私有底层发送逻辑（内部方法）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>作为所有公共发送方法的底层实现</li>
     *     <li>统一处理邮件发送的核心逻辑</li>
     *     <li>支持全局 Session 复用或独立 Session 创建</li>
     *     <li>自动处理内嵌图片的添加和流关闭</li>
     * </ul>
     *
     * @param mailAccount      邮件账号配置（包含 SMTP、用户名、授权码等信息）
     * @param useGlobalSession 是否使用全局 Session，true 表示复用全局 Session，false 表示创建新 Session
     * @param tos              收件人邮箱地址集合
     * @param ccs              抄送人邮箱地址集合，可以为 null 或空
     * @param bccs             密送人邮箱地址集合，可以为 null 或空
     * @param subject          邮件标题
     * @param content          邮件正文内容（文本或 HTML）
     * @param imageMap         内嵌图片映射，key 为 cid，value 为图片输入流（会自动关闭）
     * @param isHtml           是否为 HTML 格式，true 表示 HTML，false 表示纯文本
     * @param files            邮件附件（可选，可传入多个文件）
     * @return message-id 邮件发送成功后返回的消息 ID
     */
    private static String send(
            MailAccount mailAccount, boolean useGlobalSession, Collection<String> tos,
            Collection<String> ccs, Collection<String> bccs, String subject,
            String content, Map<String, InputStream> imageMap, Map<String, String> imageContentTypeMap,
            boolean isHtml, File... files
    ) {
        // 检查收件人列表是否为空
        if (CollUtil.isEmpty(tos)) {
            throw new IllegalArgumentException("收件人不能为空");
        }

        try {
            // 创建或获取 JavaMailSender
            JavaMailSender mailSender;
            if (useGlobalSession && mailAccount == getMailAccount()) {
                // 使用默认的 JavaMailSender（复用全局 Session）
                mailSender = getDefaultJavaMailSender();
            } else {
                // 创建新的 JavaMailSender（使用指定的 MailAccount）
                mailSender = createJavaMailSender(mailAccount);
            }
            
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置发件人
            helper.setFrom(mailAccount.getFrom());

            // 设置收件人
            helper.setTo(tos.toArray(new String[0]));

            // 设置抄送人
            if (CollUtil.isNotEmpty(ccs)) {
                helper.setCc(ccs.toArray(new String[0]));
            }

            // 设置密送人
            if (CollUtil.isNotEmpty(bccs)) {
                helper.setBcc(bccs.toArray(new String[0]));
            }

            // 设置主题
            helper.setSubject(subject);

            // 设置内容
            helper.setText(content, isHtml);

            // 添加附件
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file != null && file.exists()) {
                        helper.addAttachment(file.getName(), file);
                    }
                }
            }

            // 添加内嵌图片
            if (MapUtil.isNotEmpty(imageMap)) {
                for (Map.Entry<String, InputStream> entry : imageMap.entrySet()) {
                    String cid = entry.getKey();
                    InputStream inputStream = entry.getValue();
                    try {
                        // 将 InputStream 读取为字节数组，使用 ByteArrayResource 支持多次读取
                        byte[] imageBytes = IoUtil.readBytes(inputStream);
                        // 如果指定了格式，使用指定的格式；否则自动检测
                        String contentType;
                        if (imageContentTypeMap != null && imageContentTypeMap.containsKey(cid)) {
                            contentType = imageContentTypeMap.get(cid);
                        } else {
                            contentType = detectImageContentType(imageBytes);
                        }
                        helper.addInline(cid, new ByteArrayResource(imageBytes), contentType);
                    } finally {
                        IoUtil.close(inputStream);
                    }
                }
            }

            // 发送邮件
            mailSender.send(message);

            // 返回消息 ID
            return message.getMessageID();
        } catch (MessagingException e) {
            throw new ServiceException(ResultCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * 检测图片格式（通过文件头 Magic Number）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>通过读取图片文件的文件头（Magic Number）自动检测图片格式</li>
     *     <li>支持 PNG、JPEG、GIF、WebP、BMP 等常见图片格式</li>
     *     <li>如果无法识别格式，默认返回 image/png</li>
     * </ul>
     *
     * @param imageBytes 图片字节数组
     * @return String 图片的 MIME 类型（如 image/png、image/jpeg 等）
     */
    private static String detectImageContentType(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return "image/png"; // 默认返回 PNG
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (imageBytes.length >= 8
                && imageBytes[0] == (byte) 0x89
                && imageBytes[1] == 0x50
                && imageBytes[2] == 0x4E
                && imageBytes[3] == 0x47
                && imageBytes[4] == 0x0D
                && imageBytes[5] == 0x0A
                && imageBytes[6] == 0x1A
                && imageBytes[7] == 0x0A) {
            return "image/png";
        }

        // JPEG: FF D8 FF
        if (imageBytes.length >= 3
                && imageBytes[0] == (byte) 0xFF
                && imageBytes[1] == (byte) 0xD8
                && imageBytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }

        // GIF: 47 49 46 38 (GIF8)
        if (imageBytes.length >= 6
                && imageBytes[0] == 0x47
                && imageBytes[1] == 0x49
                && imageBytes[2] == 0x46
                && imageBytes[3] == 0x38
                && (imageBytes[4] == 0x37 || imageBytes[4] == 0x39) // 7 or 9
                && imageBytes[5] == 0x61) { // a
            return "image/gif";
        }

        // WebP: 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
        if (imageBytes.length >= 12
                && imageBytes[0] == 0x52
                && imageBytes[1] == 0x49
                && imageBytes[2] == 0x46
                && imageBytes[3] == 0x46
                && imageBytes[8] == 0x57
                && imageBytes[9] == 0x45
                && imageBytes[10] == 0x42
                && imageBytes[11] == 0x50) {
            return "image/webp";
        }

        // BMP: 42 4D (BM)
        if (imageBytes.length >= 2
                && imageBytes[0] == 0x42
                && imageBytes[1] == 0x4D) {
            return "image/bmp";
        }

        // 默认返回 PNG
        return "image/png";
    }

    /**
     * 将多个联系人字符串拆成列表，支持逗号和分号
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>解析以字符串形式传入的多个邮箱地址</li>
     *     <li>支持逗号（,）和分号（;）两种分隔符</li>
     *     <li>自动去除每个地址前后的空白字符</li>
     *     <li>作为内部工具方法，供其他发送方法调用</li>
     * </ul>
     *
     * @param addresses 邮箱地址字符串，多个地址可使用 "," 或 ";" 分隔，可为空
     * @return List<String> 解析后的邮箱地址列表，如果输入为空则返回空列表
     */
    private static List<String> splitAddress(String addresses) {
        if (StrUtil.isBlank(addresses)) {
            return CollUtil.newArrayList();
        }

        List<String> result;
        if (StrUtil.contains(addresses, CommonConstants.COMMA_SEPARATOR)) {
            result = StrUtil.splitTrim(addresses, CommonConstants.COMMA_SEPARATOR);
        } else if (StrUtil.contains(addresses, CommonConstants.DEFAULT_DELIMITER)) {
            result = StrUtil.splitTrim(addresses, CommonConstants.DEFAULT_DELIMITER);
        } else {
            result = CollUtil.newArrayList(addresses);
        }
        return result;
    }
}
