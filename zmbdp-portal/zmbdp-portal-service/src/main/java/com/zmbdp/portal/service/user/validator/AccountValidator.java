package com.zmbdp.portal.service.user.validator;

import com.zmbdp.common.domain.exception.ServiceException;

/**
 * 账号校验策略接口
 *
 * @author 稚名不带撇
 */
public interface AccountValidator {

    /**
     * 校验账号格式
     *
     * @param account 账号（手机号或邮箱）
     * @throws ServiceException 格式错误时抛出异常
     */
    void validate(String account);

    /**
     * 获取支持的校验类型
     *
     * @return 校验类型（sms-短信，mail-邮件）
     */
    String getType();
}
