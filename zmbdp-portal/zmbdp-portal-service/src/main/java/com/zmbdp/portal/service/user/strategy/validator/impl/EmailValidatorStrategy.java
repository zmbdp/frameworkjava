package com.zmbdp.portal.service.user.strategy.validator.impl;

import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.portal.service.user.strategy.validator.AccountValidatorRouter;
import com.zmbdp.portal.service.user.strategy.validator.IAccountValidatorStrategy;
import org.springframework.stereotype.Component;

/**
 * 邮箱校验策略
 * <ul>
 *     <li>负责邮箱格式的校验逻辑，实现 {@link IAccountValidatorStrategy} 接口。</li>
 *     <li>当账号格式为邮箱时，此策略会被 {@link AccountValidatorRouter} 选中并执行校验。</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Component
public class EmailValidatorStrategy implements IAccountValidatorStrategy {

    /**
     * 判断是否支持邮箱格式的校验
     * <p>
     * 通过调用工具类验证账号是否为邮箱格式。
     * </p>
     *
     * @param account 待判断的账号
     * @return true-账号为邮箱格式，支持校验；false-不是邮箱格式，不支持
     */
    @Override
    public boolean supports(String account) {
        return VerifyUtil.checkEmail(account);
    }

    /**
     * 执行邮箱格式校验
     * <p>
     * 对账号进行邮箱格式验证，如果格式不正确则抛出异常。
     * 注意：此方法被调用时，通常 {@link #supports(String)} 已返回 true，
     * 但为了防御性编程和业务完整性，此处仍进行二次校验。
     * </p>
     *
     * @param account 待校验的邮箱地址
     * @throws ServiceException 当邮箱格式不正确时抛出异常，异常信息为"邮箱格式错误"
     */
    @Override
    public void validate(String account) {
        if (!VerifyUtil.checkEmail(account)) {
            throw new ServiceException("邮箱格式错误", ResultCode.INVALID_PARA.getCode());
        }
    }
}