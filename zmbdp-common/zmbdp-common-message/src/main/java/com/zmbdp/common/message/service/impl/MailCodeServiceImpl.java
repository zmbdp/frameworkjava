package com.zmbdp.common.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.zmbdp.common.core.utils.MailUtil;
import com.zmbdp.common.message.config.MailCodeProperties;
import com.zmbdp.common.message.service.ICaptchaSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 邮件验证码服务
 *
 * @author 稚名不带撇
 */
@Slf4j
@RefreshScope
@Component("mailCodeService")
public class MailCodeServiceImpl implements ICaptchaSender {

    /**
     * 随机数生成器
     */
    private static final Random RANDOM = new Random();

    /**
     * 邮件验证码配置
     */
    @Autowired
    private MailCodeProperties mailCodeProperties;

    /**
     * 是否发送邮件
     */
    @Value("${captcha.send-message:false}")
    private boolean sendMessage;

    /**
     * 发送邮件验证码
     *
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否发送成功
     */
    @Override
    public boolean sendMobileCode(String email, String code) {
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
     *
     * @param list         列表
     * @param defaultValue 默认值（列表为空时使用）
     * @return 随机选择的元素或默认值
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
