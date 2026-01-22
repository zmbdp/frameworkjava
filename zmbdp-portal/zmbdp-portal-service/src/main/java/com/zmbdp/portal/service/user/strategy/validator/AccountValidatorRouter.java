package com.zmbdp.portal.service.user.strategy.validator;

import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账号校验策略路由器
 * <p>
 * 采用策略模式设计，作为策略上下文，负责：
 * <ul>
 *     <li>管理所有账号校验策略实现类</li>
 *     <li>根据账号格式自动选择合适的校验策略</li>
 *     <li>执行校验逻辑</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *     validatorFactory.validate(account);
 * </pre>
 * 系统会自动根据账号格式（手机号/邮箱）选择合适的校验策略并执行校验。
 * </p>
 *
 * @author 稚名不带撇
 */
@Component
public class AccountValidatorRouter {

    /**
     * 所有注册的校验策略实现类列表
     * <p>
     * Spring 会自动注入所有实现了 {@link IAccountValidatorStrategy} 接口的 Bean，
     * 如：PhoneValidatorStrategy、MailValidatorStrategy 等。
     * </p>
     */
    private final List<IAccountValidatorStrategy> accountValidatorStrategies;

    /**
     * 构造方法，自动注入所有校验策略实现类
     *
     * @param accountValidatorStrategies Spring 容器中所有 {@link IAccountValidatorStrategy} 的实现类
     */
    @Autowired
    public AccountValidatorRouter(List<IAccountValidatorStrategy> accountValidatorStrategies) {
        this.accountValidatorStrategies = accountValidatorStrategies;
    }

    /**
     * 根据账号格式自动选择合适的校验策略并执行校验
     * <p>
     * 工作流程：
     * <ol>
     *     <li>遍历所有注册的校验策略实现类</li>
     *     <li>通过 {@link IAccountValidatorStrategy#supports(String)} 方法筛选出支持该账号格式的策略</li>
     *     <li>选择第一个匹配的策略（通常只有一个匹配）</li>
     *     <li>调用策略的 {@link IAccountValidatorStrategy#validate(String)} 方法执行校验</li>
     * </ol>
     * </p>
     *
     * @param account 待校验的账号（手机号或邮箱等）
     * @throws ServiceException 当无法识别账号格式或账号格式不符合要求时抛出异常
     */
    public void validate(String account) {
        // 从所有策略中筛选出支持该账号格式的策略
        IAccountValidatorStrategy validator = accountValidatorStrategies.stream()
                // 遍历所有策略，调用 supports() 方法判断是否支持该账号格式
                // 如果 supports() 返回 true，说明该策略支持此账号格式，会被筛选出来
                .filter(v -> v.supports(account))
                // 获取第一个匹配的策略
                .findFirst()
                // 如果没有找到匹配的策略，说明账号格式无法识别，抛出异常
                .orElseThrow(() ->
                        new ServiceException("账号格式错误，请输入手机号或邮箱",
                                ResultCode.INVALID_PARA.getCode())
                );
        // 使用选中的策略执行实际的校验逻辑
        validator.validate(account);
    }
}