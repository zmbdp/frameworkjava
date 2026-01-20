package com.zmbdp.common.message.service;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.constants.MessageConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 *
 * @author 稚名不带撇
 */
@Service
@RefreshScope
public class CaptchaService {

    /**
     * redis服务类
     */
    @Autowired
    private RedisService redisService;

    /**
     * 单个手机号，每日发送短信次数的限制
     */
    @Value("${captcha.send-limit:50}")
    private Integer sendLimit;

    /**
     * 验证码的有效期，单位是分钟
     */
    @Value("${captcha.code-expiration:5}")
    private Long phoneCodeExpiration;

    /**
     * 用来判断是否发送随机验证码
     */
    @Value("${captcha.send-message:true}")
    private boolean sendMessage;

    /**
     * 判断生成什么难度的验证码
     */
    @Value("${captcha.type:2}")
    private Integer captChaType;

    /**
     * 短信服务是否发送线上短信（AliSmsServiceImpl 的 sendMessage 配置）
     */
    @Value("${sms.send-message:false}")
    private boolean smsSendMessage;

    /**
     * 邮件服务是否发送邮件（MailCodeServiceImpl 的 sendMessage 配置）
     */
    @Value("${mail.send-message:false}")
    private boolean mailSendMessage;

    /**
     * 验证码发送器工厂（根据账号格式自动选择短信或邮件发送器）
     */
    @Autowired
    private CaptchaSenderFactory captchaSenderFactory;

    /**
     * 发送验证码
     *
     * @param account 手机号 / 邮箱
     * @return 验证码
     */
    public String sendCode(String account) {
        // 先校验是否超过每日的发送限制（针对每个手机号）
        String limitCacheKey = MessageConstants.CAPTCHA_CODE_TIMES_KEY + account;
        Integer times = redisService.getCacheObject(limitCacheKey, Integer.class);
        times = times == null ? 0 : times;
        if (times >= sendLimit) {
            throw new ServiceException(ResultCode.SEND_MSG_FAILED);
        }

        // 然后判断是否在 1 分钟内频繁发送
        String codeKey = MessageConstants.CAPTCHA_CODE_KEY + account;
        String cacheValue = redisService.getCacheObject(codeKey, String.class);
        long expireTime = redisService.getExpire(codeKey);
        if (!StringUtil.isEmpty(cacheValue) && expireTime > phoneCodeExpiration * 60 - 60) {
            long time = expireTime - phoneCodeExpiration * 60 + 60;
            throw new ServiceException("操作频繁, 请在 " + time + " 秒之后重试", ResultCode.INVALID_PARA.getCode());
        }

        // 生成验证码（根据配置决定使用固定验证码还是随机验证码）
        String verifyCode = shouldUseFixedCode(account)
                ? MessageConstants.DEFAULT_CAPTCHA_CODE
                : VerifyUtil.generateVerifyCode(MessageConstants.DEFAULT_CAPTCHA_LENGTH, captChaType);

        // 发送线上短信/邮件（根据账号格式自动选择发送器）
        if (sendMessage) {
            ICaptchaSender sender = captchaSenderFactory.getSender(account);
            boolean result = sender.sendMobileCode(account, verifyCode);
            // 发送失败时，如果使用了固定验证码，则不抛异常（允许失败）
            if (!result && !shouldIgnoreSendFailure(account)) {
                throw new ServiceException(ResultCode.SEND_MSG_FAILED);
            }
        }
        // 设置验证码的缓存
        redisService.setCacheObject(codeKey, verifyCode, phoneCodeExpiration, TimeUnit.MINUTES);
        // 设置发送次数限制的缓存 （无法预先设置缓存，只能先读后写）
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        redisService.setCacheObject(limitCacheKey, times + 1, seconds, TimeUnit.SECONDS);
        return verifyCode;
    }

    /**
     * 从缓存中获取手机号的验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    public String getCode(String phone) {
        String cacheKey = MessageConstants.CAPTCHA_CODE_KEY + phone;
        return redisService.getCacheObject(cacheKey, String.class);
    }

    /**
     * 从缓存中删除手机号的验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    public boolean deleteCode(String phone) {
        String cacheKey = MessageConstants.CAPTCHA_CODE_KEY + phone;
        return redisService.deleteObject(cacheKey);
    }

    /**
     * 校验手机号与验证码是否匹配
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 布尔类型
     */
    public boolean checkCode(String phone, String code) {
        if (getCode(phone) == null || StringUtil.isEmpty(getCode(phone))) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        return getCode(phone).equals(code);
    }

    /**
     * 判断是否应该使用固定验证码
     * 使用固定验证码的情况：
     * 1. sendMessage 为 false（总开关关闭）
     * 2. sendMessage 为 true，但手机号的 sms.send-message 为 false（短信通道关闭）
     * 3. sendMessage 为 true，但邮箱的 mail.send-message 为 false（邮件通道关闭）
     *
     * @param account 账号（手机号或邮箱）
     * @return true 表示使用固定验证码，false 表示生成随机验证码
     */
    private boolean shouldUseFixedCode(String account) {
        // 总开关关闭，使用固定验证码
        if (!sendMessage) {
            return true;
        }
        // 用户输入的是手机号且短信通道关闭，使用固定验证码
        if (VerifyUtil.checkPhone(account) && !smsSendMessage) {
            return true;
        }
        // 用户输入的是邮箱且邮件通道关闭，使用固定验证码
        return VerifyUtil.checkEmail(account) && !mailSendMessage;
    }

    /**
     * 判断发送失败时是否应该忽略异常
     * 如果使用了固定验证码，则发送失败时应该忽略异常（因为验证码已经固定，用户可以直接使用）
     *
     * @param account 账号（手机号或邮箱）
     * @return true 表示忽略发送失败，false 表示抛出异常
     */
    private boolean shouldIgnoreSendFailure(String account) {
        return shouldUseFixedCode(account);
    }
}