package com.zmbdp.portal.service.user.validator;

import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import org.springframework.stereotype.Component;

/**
 * 邮箱校验策略
 *
 * @author 稚名不带撇
 */
@Component
public class EmailValidator implements AccountValidator {

    /**
     * 校验邮箱格式
     *
     * @param account 邮箱地址
     * @throws ServiceException 格式错误时抛出异常
     */
    @Override
    public void validate(String account) {
        if (!VerifyUtil.checkEmail(account)) {
            throw new ServiceException("邮箱格式错误", ResultCode.INVALID_PARA.getCode());
        }
    }

    /**
     * 获取支持的校验类型
     *
     * @return mail
     */
    @Override
    public String getType() {
        return "mail";
    }
}
