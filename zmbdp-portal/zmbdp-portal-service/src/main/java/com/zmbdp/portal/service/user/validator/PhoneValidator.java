package com.zmbdp.portal.service.user.validator;

import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import org.springframework.stereotype.Component;

/**
 * 手机号校验策略
 *
 * @author 稚名不带撇
 */
@Component
public class PhoneValidator implements AccountValidator {

    /**
     * 校验手机号格式
     *
     * @param account 手机号
     * @throws ServiceException 格式错误时抛出异常
     */
    @Override
    public void validate(String account) {
        if (!VerifyUtil.checkPhone(account)) {
            throw new ServiceException("手机号格式错误", ResultCode.INVALID_PARA.getCode());
        }
    }

    /**
     * 获取支持的校验类型
     *
     * @return sms
     */
    @Override
    public String getType() {
        return "sms";
    }
}
