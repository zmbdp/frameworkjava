package com.zmbdp.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.Mail;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 邮件工具类（基于 Hutool Mail + Jakarta Mail）<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>基于 Spring 容器中的 {@link MailAccount} 发送邮件</li>
 *     <li>支持文本 / HTML 邮件</li>
 *     <li>支持抄送（CC）、密送（BCC）</li>
 *     <li>支持附件、内嵌图片（cid）</li>
 *     <li>支持自定义 MailAccount 或使用全局配置</li>
 * </ul>
 *
 * <p>
 * 使用前准备：
 * <ol>
 *     <li>在 Spring 容器中注册 {@link MailAccount} Bean</li>
 *     <li>引入 zmbdp-common-core 相关依赖</li>
 * </ol>
 * <p>
 * 示例：
 * <pre>
 * MailUtils.sendHtml("a@b.com", "标题", "&lt;h1&gt;内容&lt;/h1&gt;");
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
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class MailUtils {

    /**
     * 从 Spring 容器拿到公共的 MailAccount
     */
    private static final MailAccount ACCOUNT = SpringUtil.getBean(MailAccount.class);

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
        return ACCOUNT;
    }

    /**
     * 在默认 MailAccount 的基础上，临时覆盖发件人信息
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要临时修改发件人显示名称、账号或授权码</li>
     *     <li>需要在保持其他配置不变的情况下更换发件人</li>
     *     <li>参数为空时会保留原有配置值</li>
     * </ul>
     *
     * <p>注意：该方法会修改全局 MailAccount，不适合并发场景频繁调用</p>
     *
     * @param from 发件人显示名称，为空时保留原值
     * @param user 邮箱账号，为空时保留原值
     * @param pass 邮箱授权码，为空时保留原值
     * @return MailAccount 修改后的邮件账号配置
     */
    public static MailAccount getMailAccount(String from, String user, String pass) {
        ACCOUNT.setFrom(StrUtil.blankToDefault(from, ACCOUNT.getFrom()));
        ACCOUNT.setUser(StrUtil.blankToDefault(user, ACCOUNT.getUser()));
        ACCOUNT.setPass(StrUtil.blankToDefault(pass, ACCOUNT.getPass()));
        return ACCOUNT;
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
        return send(getMailAccount(), true, tos, ccs, bccs, subject, content, null, isHtml, files);
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
        return send(splitAddress(to), subject, content, imageMap, isHtml, files);
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
        return send(splitAddress(to), splitAddress(cc), splitAddress(bcc), subject, content, imageMap, isHtml, files);
    }

    /**
     * 发送 HTML 邮件（多收件人 + 内嵌图片）
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
        return send(tos, subject, content, imageMap, true, files);
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
        return send(tos, null, null, subject, content, imageMap, isHtml, files);
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
        return send(getMailAccount(), true, tos, ccs, bccs, subject, content, imageMap, isHtml, files);
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
        return send(mailAccount, false, tos, ccs, bccs, subject, content, null, isHtml, files);
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
        return send(mailAccount, splitAddress(to), subject, content, imageMap, isHtml, files);
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
        return send(mailAccount, tos, null, null, subject, content, imageMap, isHtml, files);
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
        return send(mailAccount, false, tos, ccs, bccs, subject, content, imageMap, isHtml, files);
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
        // 创建一个认证器对象，初始值为 null
        Authenticator authenticator = null;

        // 检查邮件账号是否需要认证
        if (mailAccount.isAuth()) {
            // 获取用户名和密码
            final String user = mailAccount.getUser();
            final String pass = mailAccount.getPass();
            // 创建认证器，用于 SMTP 认证
            authenticator = new Authenticator() {
                // 重写获取密码认证的方法
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // 返回用户名和密码的认证对象
                    return new PasswordAuthentication(user, pass);
                }
            };
        }
        // 根据 isSingleton 参数决定使用单例 Session 还是创建新 Session
        return isSingleton ?
                // 使用单例模式，获取默认 Session 实例
                Session.getDefaultInstance(mailAccount.getSmtpProps(), authenticator) :
                // 创建新的 Session 实例
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
            String content, Map<String, InputStream> imageMap, boolean isHtml, File... files
    ) {
        // 创建邮件对象并设置是否使用全局会话
        final Mail mail = Mail.create(mailAccount).setUseGlobalSession(useGlobalSession);

        // 再检查抄送人列表和密送列表是否为空，不为空就得填写
        if (CollUtil.isNotEmpty(ccs)) { // 抄送人列表
            mail.setCcs(ccs.toArray(new String[0]));
        }
        if (CollUtil.isNotEmpty(bccs)) { // 密送人列表
            mail.setBccs(bccs.toArray(new String[0]));
        }
        // 设置邮件相关信息
        mail.setTos(tos.toArray(new String[0])); // 收件人列表
        mail.setTitle(subject); // 标题
        mail.setContent(content); // 内容
        mail.setHtml(isHtml); // 是否为 HTML 格式
        mail.setFiles(files); // 附件

        // 检查内嵌图片映射是否不为空，不为空就得添加
        if (MapUtil.isNotEmpty(imageMap)) {
            // 遍历图片映射
            for (Map.Entry<String, InputStream> entry : imageMap.entrySet()) {
                // 添加图片到邮件中
                mail.addImage(entry.getKey(), entry.getValue());
                // 关闭输入流
                IoUtil.close(entry.getValue());
            }
        }

        // 发送邮件并返回消息 ID
        return mail.send();
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
     * @return List<String> 解析后的邮箱地址列表，如果输入为空则返回 null
     */
    private static List<String> splitAddress(String addresses) {
        // 判空，如果是空就直接返回
        if (StrUtil.isBlank(addresses)) {
            return null;
        }

        List<String> result;
        // 判断字符串中是否包含逗号或分号
        if (StrUtil.contains(addresses, CharUtil.COMMA)) {
            // 如果包含逗号，则使用逗号进行分割
            result = StrUtil.splitTrim(addresses, CharUtil.COMMA);
        } else if (StrUtil.contains(addresses, ';')) {
            // 如果包含分号，则使用分号进行分割
            result = StrUtil.splitTrim(addresses, ';');
        } else {
            // 否则，将字符串放入列表中
            result = CollUtil.newArrayList(addresses);
        }
        return result;
    }
}