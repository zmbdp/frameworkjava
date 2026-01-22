package com.zmbdp.portal.service.user.validator;

import com.zmbdp.common.domain.exception.ServiceException;

/**
 * 账号校验策略接口
 * <p>
 * 采用策略模式设计，支持多种账号格式的校验（如手机号、邮箱等）。
 * 每个具体策略实现类负责一种账号格式的校验逻辑。
 * </p>
 *
 * @author 稚名不带撇
 */
public interface AccountValidator {

    /**
     * 判断当前策略是否支持该账号格式的校验
     * <p>
     * 此方法用于策略选择阶段，由策略上下文（AccountValidatorFactory）调用，
     * 用于从多个策略实现中筛选出合适的策略。
     * </p>
     *
     * @param account 待校验的账号（手机号或邮箱等）
     * @return true-支持此账号格式的校验，false-不支持
     */
    boolean supports(String account);

    /**
     * 执行账号格式校验
     * <p>
     * 此方法用于实际校验阶段，对账号格式进行验证。
     * 注意：调用此方法前，应确保 {@link #supports(String)} 返回 true，
     * 否则可能抛出异常。
     * </p>
     *
     * @param account 待校验的账号（手机号或邮箱等）
     * @throws ServiceException 当账号格式不符合要求时抛出异常
     */
    void validate(String account);
}