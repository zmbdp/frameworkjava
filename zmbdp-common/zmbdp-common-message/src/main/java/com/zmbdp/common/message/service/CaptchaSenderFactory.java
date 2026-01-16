package com.zmbdp.common.message.service;

import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 验证码发送器工厂
 * 根据账号格式（手机号/邮箱）自动选择合适的发送器
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class CaptchaSenderFactory {

    /**
     * 短信发送器（用于手机号）
     */
    @Autowired
    @Qualifier("aliSmsService")
    private ICaptchaSender smsSender;

    /**
     * 邮件发送器（用于邮箱）
     */
    @Autowired
    @Qualifier("mailCodeService")
    private ICaptchaSender mailSender;

    /**
     * 根据账号格式获取对应的验证码发送器
     *
     * @param account 账号（手机号或邮箱）
     * @return 验证码发送器
     * @throws ServiceException 无法识别账号格式时抛出异常
     */
    public ICaptchaSender getSender(String account) {
        if (VerifyUtil.checkPhone(account)) {
            // 手机号：使用短信发送器
            log.debug("账号 {} 识别为手机号，使用短信发送器", account);
            return smsSender;
        } else if (VerifyUtil.checkEmail(account)) {
            // 邮箱：使用邮件发送器
            log.debug("账号 {} 识别为邮箱，使用邮件发送器", account);
            return mailSender;
        } else {
            throw new ServiceException("账号格式错误，请输入手机号或邮箱", ResultCode.INVALID_PARA.getCode());
        }
    }
}
