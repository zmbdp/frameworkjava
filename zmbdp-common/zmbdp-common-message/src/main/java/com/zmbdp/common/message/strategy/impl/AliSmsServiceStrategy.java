package com.zmbdp.common.message.strategy.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.google.gson.Gson;
import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.constants.MessageConstants;
import com.zmbdp.common.message.strategy.ICaptchaSenderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云短信发送策略
 * <p>
 * 实现 {@link ICaptchaSenderStrategy} 接口，提供基于阿里云短信服务的验证码发送功能。<br>
 * 支持通过阿里云短信服务 API 发送验证码短信。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>发送短信验证码：通过阿里云短信服务发送验证码</li>
 *     <li>模板短信发送：支持使用配置的短信模板发送短信</li>
 *     <li>发送开关控制：支持通过配置控制是否实际发送短信（开发/测试环境可关闭）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 方式1：通过路由器自动选择发送器（推荐）
 * @Autowired
 * private CaptchaSenderRouter captchaSenderRouter;
 * boolean result = captchaSenderRouter.sendCode("13800138000", "123456");
 *
 * // 方式2：直接注入指定实现类
 * @Autowired
 * @Qualifier("aliSmsService")
 * private ICaptchaSenderStrategy smsSender;
 * boolean result = smsSender.sendCode("13800138000", "123456");
 * }</pre>
 * <p>
 * <b>配置说明：</b>
 * <ul>
 *     <li>sms.aliyun.templateCode：短信模板代码（必填）</li>
 *     <li>sms.sign-name：短信签名（必填）</li>
 *     <li>sms.send-message：是否发送短信（默认 false，开发环境建议关闭）</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>支持配置热更新（@RefreshScope）</li>
 *     <li>如果 sendMessage 为 false，不会实际发送短信，直接返回 false</li>
 *     <li>需要配置阿里云短信服务的 AccessKey 和 SecretKey</li>
 *     <li>短信模板需要在阿里云控制台预先创建</li>
 *     <li>发送失败会记录错误日志，但不会抛出异常</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ICaptchaSenderStrategy
 * @see com.aliyun.dysmsapi20170525.Client
 */
@Slf4j
@Component
@RefreshScope
public class AliSmsServiceStrategy implements ICaptchaSenderStrategy {

    /**
     * 阿里云短信服务客户端
     * <p>
     * 用于调用阿里云短信服务 API 发送短信。<br>
     * 需要在 Spring 容器中配置该 Bean。
     */
    @Autowired
    private Client client;

    /**
     * 短信模板代码
     * <p>
     * 配置项：sms.aliyun.templateCode。<br>
     * 在阿里云短信服务控制台创建的短信模板代码，用于发送验证码短信。<br>
     * 模板中需要包含 {code} 占位符，用于替换验证码。
     */
    @Value("${sms.aliyun.templateCode:}")
    private String templateCode;

    /**
     * 短信签名
     * <p>
     * 配置项：sms.sign-name。<br>
     * 在阿里云短信服务控制台申请的短信签名，用于标识发送方。<br>
     * 签名需要审核通过后才能使用。
     */
    @Value("${sms.sign-name:}")
    private String signName;

    /**
     * 是否发送线上短信
     * <p>
     * 配置项：sms.send-message，默认值为 false。
     * <ul>
     *     <li>true：实际发送短信到用户手机（生产环境）</li>
     *     <li>false：不发送短信，直接返回 false（开发/测试环境）</li>
     * </ul>
     * 此配置用于控制是否实际调用阿里云短信服务 API。
     */
    @Value("${sms.send-message:false}")
    private boolean sendMessage;

    /**
     * 是否支持当前账号类型
     * <p>
     * 判断当前发送器是否支持手机号账号类型。<br>
     * 使用 {@link com.zmbdp.common.core.utils.VerifyUtil#checkPhone(String)} 验证手机号格式。
     *
     * @param account 账号，不能为 null
     * @return true 表示账号是手机号格式，false 表示不是
     */
    @Override
    public boolean supports(String account) {
        return VerifyUtil.checkPhone(account);
    }

    /**
     * 发送验证码
     * <p>
     * 通过阿里云短信服务发送验证码到指定手机号。<br>
     * 使用配置的短信模板和签名发送短信。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 通过注入的 Bean 发送验证码
     * @Autowired
     * @Qualifier("aliSmsService")
     * private ICaptchaSenderStrategy smsSender;
     * boolean result = smsSender.sendCode("13800138000", "123456");
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>构建短信参数（包含验证码）</li>
     *     <li>调用 {@link #sendTemMessage(String, String, Map)} 发送模板短信</li>
     *     <li>返回发送结果</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 phone 为 null 或空字符串，可能抛出异常</li>
     *     <li>如果 code 为 null 或空字符串，可能发送失败</li>
     *     <li>如果 sendMessage 为 false，不会实际发送短信，直接返回 false</li>
     *     <li>发送失败不会抛出异常，只返回 false 并记录错误日志</li>
     *     <li>需要确保模板代码和签名已正确配置</li>
     * </ul>
     *
     * @param phone 手机号，不能为 null 或空字符串
     * @param code  验证码，不能为 null 或空字符串
     * @return true 表示发送成功，false 表示发送失败
     * @see #sendTemMessage(String, String, Map)
     */
    @Override
    public boolean sendCode(String phone, String code) {
        log.info("开始发送短信验证码, 账号：{}", phone);
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        return sendTemMessage(phone, templateCode, params);
    }

    /**
     * 发送模板短信
     * <p>
     * 通过阿里云短信服务 API 发送模板短信。<br>
     * 根据模板代码和参数构建短信内容并发送。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>检查 sendMessage 配置，如果为 false 则直接返回 false</li>
     *     <li>创建阿里云短信发送请求（SendSmsRequest）</li>
     *     <li>设置手机号、签名、模板代码、模板参数</li>
     *     <li>调用阿里云客户端发送短信</li>
     *     <li>解析响应结果，判断是否发送成功</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 sendMessage 为 false，不会实际发送短信，直接返回 false</li>
     *     <li>如果 phone 为 null 或空字符串，可能抛出异常</li>
     *     <li>如果 templateCode 为 null 或空字符串，可能发送失败</li>
     *     <li>params 中的参数会转换为 JSON 格式传递给模板</li>
     *     <li>发送失败会记录详细的错误日志（包含请求信息和错误原因）</li>
     *     <li>异常会被捕获，不会向上抛出，只返回 false</li>
     *     <li>成功响应码为 "OK"（MessageConstants.CAPTCHA_MSG_OK）</li>
     * </ul>
     *
     * @param phone        手机号，不能为 null 或空字符串
     * @param templateCode 模板代码，不能为 null 或空字符串
     * @param params       模板参数（Map 格式），会转换为 JSON 传递给模板
     * @return true 表示发送成功，false 表示发送失败
     * @see com.aliyun.dysmsapi20170525.models.SendSmsRequest
     * @see com.aliyun.dysmsapi20170525.models.SendSmsResponse
     */
    private boolean sendTemMessage(String phone, String templateCode, Map<String, String> params) {
        // 把是否发送线上短信交给 nacos 管理
        if (!sendMessage) {
            log.error("短信发送通道关闭, {}", phone);
            return false;
        }
        // 先创建阿里云发送短信的请求
        SendSmsRequest sendSmsRequest = new SendSmsRequest();
        sendSmsRequest.setPhoneNumbers(phone);
        sendSmsRequest.setSignName(signName);
        sendSmsRequest.setTemplateCode(templateCode);
        sendSmsRequest.setTemplateParam(JsonUtil.classToJson(params));
        // 然后发送请求，根据结果判断是否发送成功
        try {
            SendSmsResponse sendSmsResponse = client.sendSms(sendSmsRequest);
            SendSmsResponseBody responseBody = sendSmsResponse.getBody();
            if (responseBody.getCode().equals(MessageConstants.CAPTCHA_MSG_OK)) {
                return true;
            }
            log.error("短信: {} 发送失败, 失败原因: {}...", new Gson().toJson(sendSmsRequest), responseBody.getMessage());
            return false;
        } catch (Exception e) {
            log.error("短信: {} 发送失败, 失败原因: {}...", new Gson().toJson(sendSmsRequest), e.getMessage());
            return false;
        }
    }
}