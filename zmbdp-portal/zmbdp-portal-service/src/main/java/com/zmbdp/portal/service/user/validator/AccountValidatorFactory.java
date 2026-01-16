package com.zmbdp.portal.service.user.validator;

import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 账号校验策略工厂
 * 根据账号格式（手机号/邮箱）自动选择合适的校验器
 *
 * @author 稚名不带撇
 */
@Component
public class AccountValidatorFactory {

    /**
     * 校验策略映射表（key: 类型，value: 校验器）
     */
    private final Map<String, AccountValidator> validatorMap;

    /**
     * 构造方法，自动注入所有校验策略
     *
     * @param validators 所有校验策略实现类
     */
    @Autowired
    public AccountValidatorFactory(List<AccountValidator> validators) {
        // 创建短信，邮件映射表 [sms, PhoneValidator] / [mail, EmailValidator]
        this.validatorMap = validators.stream()
                .collect(Collectors.toMap(AccountValidator::getType, Function.identity()));
    }

    /**
     * 根据账号格式获取对应的校验策略
     *
     * @param account 账号（手机号或邮箱）
     * @return 校验策略实例
     * @throws ServiceException 无法识别账号格式时抛出异常
     */
    public AccountValidator getValidator(String account) {
        if (VerifyUtil.checkPhone(account)) {
            // 手机号：使用手机号校验器
            return validatorMap.get("sms");
        } else if (VerifyUtil.checkEmail(account)) {
            // 邮箱：使用邮箱校验器
            return validatorMap.get("mail");
        } else {
            throw new ServiceException("账号格式错误，请输入手机号或邮箱", ResultCode.INVALID_PARA.getCode());
        }
    }
}
