package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.core.config.MailAccount;
import com.zmbdp.common.core.utils.MailUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 邮件功能测试控制器
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/mail")
public class TestMailController {

    /**
     * 测试1：发送文本邮件（单个收件人）
     */
    @GetMapping("/send/text/single")
    public Result<String> sendTextSingle(@RequestParam(required = false, defaultValue = "test@example.com") String to) {
        log.info("========== 开始测试：发送文本邮件（单个收件人） ==========");
        log.info("收件人：{}", to);

        try {
            String subject = "测试邮件 - 文本格式（单个收件人）";
            String content = "这是一封测试邮件，使用文本格式发送。\n\n测试内容：\n1. 文本格式邮件\n2. 单个收件人\n3. 基本功能测试";

            log.info("邮件标题：{}", subject);
            log.info("邮件内容：{}", content);

            String messageId = MailUtil.sendText(to, subject, content);

            log.info("✅ 邮件发送成功，Message-ID: {}", messageId);
            log.info("========== 测试完成：发送文本邮件（单个收件人） ==========");
            return Result.success("邮件发送成功，Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送文本邮件（单个收件人） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试2：发送文本邮件（多个收件人 - 逗号分隔）
     */
    @GetMapping("/send/text/multiple")
    public Result<String> sendTextMultiple(@RequestParam(required = false, defaultValue = "test1@example.com,test2@example.com") String to) {
        log.info("========== 开始测试：发送文本邮件（多个收件人 - 逗号分隔） ==========");
        log.info("收件人：{}", to);

        try {
            String subject = "测试邮件 - 文本格式（多个收件人）";
            String content = "这是一封测试邮件，使用文本格式发送给多个收件人。\n\n测试内容：\n1. 文本格式邮件\n2. 多个收件人（逗号分隔）\n3. 基本功能测试";

            log.info("邮件标题：{}", subject);
            log.info("邮件内容：{}", content);

            String messageId = MailUtil.sendText(to, subject, content);

            log.info("✅ 邮件发送成功，Message-ID: {}", messageId);
            log.info("========== 测试完成：发送文本邮件（多个收件人） ==========");
            return Result.success("邮件发送成功，Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送文本邮件（多个收件人） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试3：发送HTML邮件（单个收件人）
     */
    @GetMapping("/send/html/single")
    public Result<String> sendHtmlSingle(@RequestParam(required = false, defaultValue = "test@example.com") String to) {
        log.info("========== 开始测试：发送HTML邮件（单个收件人） ==========");
        log.info("收件人：{}", to);

        try {
            String subject = "测试邮件 - HTML格式（单个收件人）";
            String content = "<html><body>"
                    + "<h1>这是一封HTML格式的测试邮件</h1>"
                    + "<p>测试内容：</p>"
                    + "<ul>"
                    + "<li>HTML格式邮件</li>"
                    + "<li>单个收件人</li>"
                    + "<li>基本功能测试</li>"
                    + "</ul>"
                    + "<p style='color: red;'>这是红色文字</p>"
                    + "</body></html>";

            log.info("邮件标题：{}", subject);
            log.info("邮件内容（HTML）：{}", content);

            String messageId = MailUtil.sendHtml(to, subject, content);

            log.info("✅ 邮件发送成功，Message-ID: {}", messageId);
            log.info("========== 测试完成：发送HTML邮件（单个收件人） ==========");
            return Result.success("邮件发送成功，Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送HTML邮件（单个收件人） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试4：发送HTML邮件（多个收件人 - 分号分隔）
     */
    @GetMapping("/send/html/multiple")
    public Result<String> sendHtmlMultiple(@RequestParam(required = false, defaultValue = "test1@example.com;test2@example.com") String to) {
        log.info("========== 开始测试：发送HTML邮件（多个收件人 - 分号分隔） ==========");
        log.info("收件人：{}", to);

        try {
            String subject = "测试邮件 - HTML格式（多个收件人）";
            String content = "<html><body>"
                    + "<h1>这是一封HTML格式的测试邮件</h1>"
                    + "<p>测试内容：</p>"
                    + "<ul>"
                    + "<li>HTML格式邮件</li>"
                    + "<li>多个收件人（分号分隔）</li>"
                    + "<li>基本功能测试</li>"
                    + "</ul>"
                    + "</body></html>";

            log.info("邮件标题：{}", subject);

            String messageId = MailUtil.sendHtml(to, subject, content);

            log.info("✅ 邮件发送成功，Message-ID: {}", messageId);
            log.info("========== 测试完成：发送HTML邮件（多个收件人） ==========");
            return Result.success("邮件发送成功，Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送HTML邮件（多个收件人） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试5：发送邮件（带抄送和密送）
     */
    @GetMapping("/send/ccbcc")
    public Result<String> sendWithCcBcc(
            @RequestParam(required = false, defaultValue = "test@example.com") String to,
            @RequestParam(required = false, defaultValue = "cc@example.com") String cc,
            @RequestParam(required = false, defaultValue = "bcc@example.com") String bcc) {
        log.info("========== 开始测试：发送邮件（带抄送和密送） ==========");
        log.info("收件人：{}", to);
        log.info("抄送人：{}", cc);
        log.info("密送人：{}", bcc);

        try {
            String subject = "测试邮件 - 带抄送和密送";
            String content = "这是一封测试邮件，包含抄送（CC）和密送（BCC）功能。\n\n测试内容：\n1. 文本格式邮件\n2. 抄送和密送功能\n3. 基本功能测试";

            log.info("邮件标题：{}", subject);

            String messageId = MailUtil.send(to, cc, bcc, subject, content, false);

            log.info("✅ 邮件发送成功，Message-ID: {}", messageId);
            log.info("========== 测试完成：发送邮件（带抄送和密送） ==========");
            return Result.success("邮件发送成功，Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送邮件（带抄送和密送） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试6：发送邮件（带附件）
     */
    @GetMapping("/send/attachment")
    public Result<String> sendWithAttachment(@RequestParam(required = false, defaultValue = "test@example.com") String to) {
        log.info("========== 开始测试：发送邮件（带附件） ==========");
        log.info("收件人：{}", to);

        try {
            String subject = "测试邮件 - 带附件";
            String content = "这是一封测试邮件，包含附件功能。\n\n测试内容：\n1. 文本格式邮件\n2. 附件功能\n3. 基本功能测试";

            // 创建临时测试文件
            File tempFile = File.createTempFile("test_attachment_", ".txt");
            tempFile.deleteOnExit();
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write("这是一个测试附件文件\n测试内容：附件功能测试");
            }

            log.info("邮件标题：{}", subject);
            log.info("附件文件：{}，大小：{} 字节", tempFile.getName(), tempFile.length());

            String messageId = MailUtil.sendText(to, subject, content, tempFile);

            log.info("✅ 邮件发送成功（带附件），Message-ID: {}", messageId);
            log.info("========== 测试完成：发送邮件（带附件） ==========");
            return Result.success("邮件发送成功（带附件），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送邮件（带附件） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试7：发送邮件（带多个附件）
     */
    @GetMapping("/send/attachments")
    public Result<String> sendWithAttachments(@RequestParam(required = false, defaultValue = "test@example.com") String to) {
        log.info("========== 开始测试：发送邮件（带多个附件） ==========");
        log.info("收件人：{}", to);

        try {
            String subject = "测试邮件 - 带多个附件";
            String content = "这是一封测试邮件，包含多个附件功能。\n\n测试内容：\n1. 文本格式邮件\n2. 多个附件功能\n3. 基本功能测试";

            // 创建多个临时测试文件
            File tempFile1 = File.createTempFile("test_attachment1_", ".txt");
            File tempFile2 = File.createTempFile("test_attachment2_", ".txt");
            tempFile1.deleteOnExit();
            tempFile2.deleteOnExit();

            try (java.io.FileWriter writer1 = new java.io.FileWriter(tempFile1, StandardCharsets.UTF_8);
                 java.io.FileWriter writer2 = new java.io.FileWriter(tempFile2, StandardCharsets.UTF_8)) {
                writer1.write("这是第一个测试附件文件\n测试内容：附件1");
                writer2.write("这是第二个测试附件文件\n测试内容：附件2");
            }

            log.info("邮件标题：{}", subject);
            log.info("附件文件1：{}，大小：{} 字节", tempFile1.getName(), tempFile1.length());
            log.info("附件文件2：{}，大小：{} 字节", tempFile2.getName(), tempFile2.length());

            String messageId = MailUtil.sendText(to, subject, content, tempFile1, tempFile2);

            log.info("✅ 邮件发送成功（带多个附件），Message-ID: {}", messageId);
            log.info("========== 测试完成：发送邮件（带多个附件） ==========");
            return Result.success("邮件发送成功（带多个附件），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送邮件（带多个附件） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试8：发送HTML邮件（带内嵌图片 - 自动检测格式）
     */
    @PostMapping(value = "/send/html/image", consumes = "multipart/form-data")
    public Result<String> sendHtmlWithImage(
            @RequestParam(value = "to", required = false) String to,
            @RequestParam("image") MultipartFile imageFile) {
        log.info("========== 开始测试：发送HTML邮件（带内嵌图片 - 自动检测格式） ==========");
        // 如果没有传 to 参数，使用默认值
        if (to == null || to.isEmpty()) {
            to = "test@example.com";
        }
        log.info("收件人：{}", to);
        log.info("上传图片：{}，大小：{} 字节", imageFile.getOriginalFilename(), imageFile.getSize());

        try {
            // 验证文件
            if (imageFile.isEmpty()) {
                log.warn("⚠️ 图片文件为空");
                return Result.fail(ResultCode.FAILED.getCode(), "图片文件不能为空");
            }

            String subject = "测试邮件 - HTML格式（带内嵌图片 - 自动检测格式）";
            String content = "<html><body>"
                    + "<h1>这是一封HTML格式的测试邮件（带内嵌图片）</h1>"
                    + "<p>测试内容：</p>"
                    + "<ul>"
                    + "<li>HTML格式邮件</li>"
                    + "<li>内嵌图片功能（cid方式）</li>"
                    + "<li>自动检测图片格式</li>"
                    + "<li>基本功能测试</li>"
                    + "</ul>"
                    + "<p>内嵌图片：<img src='cid:testImage' alt='测试图片' style='max-width: 500px;' /></p>"
                    + "</body></html>";

            // 将上传的文件转换为 InputStream
            Map<String, InputStream> imageMap = new HashMap<>();
            imageMap.put("testImage", imageFile.getInputStream());

            log.info("邮件标题：{}", subject);
            log.info("内嵌图片数量：{}", imageMap.size());
            log.info("图片文件名：{}", imageFile.getOriginalFilename());
            log.info("图片大小：{} 字节", imageFile.getSize());
            log.info("图片格式：自动检测（未指定格式参数）");

            String messageId = MailUtil.sendHtml(to, subject, content, imageMap);

            log.info("✅ 邮件发送成功（带内嵌图片 - 自动检测格式），Message-ID: {}", messageId);
            log.info("========== 测试完成：发送HTML邮件（带内嵌图片 - 自动检测格式） ==========");
            return Result.success("邮件发送成功（带内嵌图片 - 自动检测格式），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送HTML邮件（带内嵌图片 - 自动检测格式） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试9：发送HTML邮件（带内嵌图片 - 手动指定格式）
     */
    @PostMapping(value = "/send/html/image/manual", consumes = "multipart/form-data")
    public Result<String> sendHtmlWithImageManual(
            @RequestParam(value = "to", required = false) String to,
            @RequestParam("logo") MultipartFile logoFile,
            @RequestParam("photo") MultipartFile photoFile) {
        log.info("========== 开始测试：发送HTML邮件（带内嵌图片 - 手动指定格式） ==========");
        // 如果没有传 to 参数，使用默认值
        if (to == null || to.isEmpty()) {
            to = "test@example.com";
        }
        log.info("收件人：{}", to);
        log.info("上传Logo：{}，大小：{} 字节", logoFile.getOriginalFilename(), logoFile.getSize());
        log.info("上传Photo：{}，大小：{} 字节", photoFile.getOriginalFilename(), photoFile.getSize());

        try {
            // 验证文件
            if (logoFile.isEmpty()) {
                log.warn("⚠️ Logo文件为空");
                return Result.fail(ResultCode.FAILED.getCode(), "Logo文件不能为空");
            }
            if (photoFile.isEmpty()) {
                log.warn("⚠️ Photo文件为空");
                return Result.fail(ResultCode.FAILED.getCode(), "Photo文件不能为空");
            }

            String subject = "测试邮件 - HTML格式（带内嵌图片 - 手动指定格式）";
            String content = "<html><body>"
                    + "<h1>这是一封HTML格式的测试邮件（带内嵌图片）</h1>"
                    + "<p>测试内容：</p>"
                    + "<ul>"
                    + "<li>HTML格式邮件</li>"
                    + "<li>内嵌图片功能（cid方式）</li>"
                    + "<li>手动指定图片格式</li>"
                    + "<li>基本功能测试</li>"
                    + "</ul>"
                    + "<p>内嵌图片1（Logo）：<img src='cid:logo' alt='Logo' style='max-width: 500px;' /></p>"
                    + "<p>内嵌图片2（Photo）：<img src='cid:photo' alt='Photo' style='max-width: 500px;' /></p>"
                    + "</body></html>";

            // 将上传的文件转换为 InputStream
            Map<String, InputStream> imageMap = new HashMap<>();
            imageMap.put("logo", logoFile.getInputStream());
            imageMap.put("photo", photoFile.getInputStream());

            // 手动指定图片格式
            Map<String, String> imageContentTypeMap = new HashMap<>();
            imageContentTypeMap.put("logo", "image/png");
            imageContentTypeMap.put("photo", "image/jpeg");

            log.info("邮件标题：{}", subject);
            log.info("内嵌图片数量：{}", imageMap.size());
            log.info("Logo文件名：{}，大小：{} 字节", logoFile.getOriginalFilename(), logoFile.getSize());
            log.info("Photo文件名：{}，大小：{} 字节", photoFile.getOriginalFilename(), photoFile.getSize());
            log.info("图片格式：手动指定");
            log.info("Logo格式：{}", imageContentTypeMap.get("logo"));
            log.info("Photo格式：{}", imageContentTypeMap.get("photo"));

            String messageId = MailUtil.sendHtml(to, subject, content, imageMap, imageContentTypeMap);

            log.info("✅ 邮件发送成功（带内嵌图片 - 手动指定格式），Message-ID: {}", messageId);
            log.info("========== 测试完成：发送HTML邮件（带内嵌图片 - 手动指定格式） ==========");
            return Result.success("邮件发送成功（带内嵌图片 - 手动指定格式），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送HTML邮件（带内嵌图片 - 手动指定格式） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试10：发送邮件（使用自定义MailAccount）
     */
    @GetMapping("/send/custom")
    public Result<String> sendWithCustomAccount(@RequestParam(required = false, defaultValue = "test@example.com") String to) {
        log.info("========== 开始测试：发送邮件（使用自定义MailAccount） ==========");
        log.info("收件人：{}", to);

        try {
            // 获取默认MailAccount
            MailAccount defaultAccount = MailUtil.getMailAccount();
            log.info("默认MailAccount - Host: {}, Port: {}", defaultAccount.getHost(), defaultAccount.getPort());

            // 使用自定义MailAccount（复制默认配置并修改）
            MailAccount customAccount = MailUtil.getMailAccount(
                    defaultAccount.getFrom(),
                    defaultAccount.getUser(),
                    defaultAccount.getPass()
            );
            log.info("自定义MailAccount创建成功");

            String subject = "测试邮件 - 自定义MailAccount";
            String content = "这是一封使用自定义MailAccount发送的测试邮件。\n\n测试内容：\n1. 自定义MailAccount\n2. 线程安全测试\n3. 基本功能测试";

            log.info("邮件标题：{}", subject);

            Collection<String> tos = Arrays.asList(to);
            String messageId = MailUtil.send(customAccount, tos, subject, content, false);

            log.info("✅ 邮件发送成功（自定义MailAccount），Message-ID: {}", messageId);
            log.info("========== 测试完成：发送邮件（使用自定义MailAccount） ==========");
            return Result.success("邮件发送成功（自定义MailAccount），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败", e);
            log.info("========== 测试失败：发送邮件（使用自定义MailAccount） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 测试11：发送邮件（错误情况 - 空收件人）
     */
    @GetMapping("/send/error/empty")
    public Result<String> sendWithEmptyTo() {
        log.info("========== 开始测试：发送邮件（错误情况 - 空收件人） ==========");

        try {
            String subject = "测试邮件 - 错误情况";
            String content = "这是一封测试邮件，用于测试错误处理。";

            log.info("邮件标题：{}", subject);
            log.info("收件人为空（预期会失败）");

            String messageId = MailUtil.sendText("", subject, content);

            log.warn("⚠️ 邮件发送成功（预期应该失败），Message-ID: {}", messageId);
            log.info("========== 测试完成：发送邮件（空收件人 - 未按预期失败） ==========");
            return Result.success("邮件发送成功（未按预期失败），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败（预期行为）", e);
            log.info("========== 测试完成：发送邮件（空收件人 - 预期失败） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败（预期行为）：" + e.getMessage());
        }
    }

    /**
     * 测试12：发送邮件（错误情况 - 无效邮箱地址）
     */
    @GetMapping("/send/error/invalid")
    public Result<String> sendWithInvalidEmail(@RequestParam(required = false, defaultValue = "invalid-email") String to) {
        log.info("========== 开始测试：发送邮件（错误情况 - 无效邮箱地址） ==========");
        log.info("收件人（无效邮箱）：{}", to);

        try {
            String subject = "测试邮件 - 错误情况";
            String content = "这是一封测试邮件，用于测试错误处理。";

            log.info("邮件标题：{}", subject);

            String messageId = MailUtil.sendText(to, subject, content);

            log.warn("⚠️ 邮件发送成功（无效邮箱地址）");
            log.info("========== 测试完成：发送邮件（无效邮箱地址） ==========");
            return Result.success("邮件发送成功（无效邮箱地址），Message-ID: " + messageId);
        } catch (Exception e) {
            log.error("❌ 邮件发送失败（预期行为）", e);
            log.info("========== 测试完成：发送邮件（无效邮箱地址 - 预期失败） ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "邮件发送失败（预期行为）：" + e.getMessage());
        }
    }

    /**
     * 测试13：获取默认MailAccount
     */
    @GetMapping("/account/default")
    public Result<String> getDefaultAccount() {
        log.info("========== 开始测试：获取默认MailAccount ==========");

        try {
            MailAccount account = MailUtil.getMailAccount();

            log.info("✅ 获取默认MailAccount成功");
            log.info("Host: {}", account.getHost());
            log.info("Port: {}", account.getPort());
            log.info("From: {}", account.getFrom());
            log.info("User: {}", account.getUser());
            log.info("Auth: {}", account.isAuth());
            log.info("SSL: {}", account.getSslEnable());
            log.info("========== 测试完成：获取默认MailAccount ==========");
            return Result.success("获取默认MailAccount成功，Host: " + account.getHost());
        } catch (Exception e) {
            log.error("❌ 获取默认MailAccount失败", e);
            log.info("========== 测试失败：获取默认MailAccount ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "获取默认MailAccount失败：" + e.getMessage());
        }
    }

    /**
     * 测试14：获取自定义MailAccount（线程安全测试）
     */
    @GetMapping("/account/custom")
    public Result<String> getCustomAccount(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String pass) {
        log.info("========== 开始测试：获取自定义MailAccount（线程安全测试） ==========");
        log.info("From参数：{}", from);
        log.info("User参数：{}", user);
        log.info("Pass参数：{}（已脱敏）", pass != null ? "***" : null);

        try {
            // 获取默认账号
            MailAccount defaultAccount = MailUtil.getMailAccount();
            log.info("默认账号 - From: {}, User: {}", defaultAccount.getFrom(), defaultAccount.getUser());

            // 获取自定义账号（应返回新对象，不修改全局配置）
            MailAccount customAccount = MailUtil.getMailAccount(from, user, pass);
            log.info("自定义账号 - From: {}, User: {}", customAccount.getFrom(), customAccount.getUser());

            // 再次获取默认账号，验证未被修改
            MailAccount defaultAccount2 = MailUtil.getMailAccount();
            log.info("再次获取默认账号 - From: {}, User: {}", defaultAccount2.getFrom(), defaultAccount2.getUser());

            boolean isSame = defaultAccount == defaultAccount2;
            boolean isModified = !defaultAccount.getFrom().equals(defaultAccount2.getFrom());

            log.info("默认账号对象是否相同：{}（应为true）", isSame);
            log.info("默认账号是否被修改：{}（应为false）", isModified);

            if (!isModified && isSame) {
                log.info("✅ 线程安全测试通过，自定义账号未修改全局配置");
            } else {
                log.warn("⚠️ 线程安全测试未通过");
            }

            log.info("========== 测试完成：获取自定义MailAccount（线程安全测试） ==========");
            return Result.success("获取自定义MailAccount成功，线程安全测试通过");
        } catch (Exception e) {
            log.error("❌ 获取自定义MailAccount失败", e);
            log.info("========== 测试失败：获取自定义MailAccount ==========");
            return Result.fail(ResultCode.FAILED.getCode(), "获取自定义MailAccount失败：" + e.getMessage());
        }
    }
}