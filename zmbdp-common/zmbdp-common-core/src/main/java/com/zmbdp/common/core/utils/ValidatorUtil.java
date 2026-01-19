package com.zmbdp.common.core.utils;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Validator 校验框架工具类<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>基于 Jakarta Validation（Bean Validation）的数据校验工具</li>
 *     <li>使用 Spring 容器中的 Validator Bean 进行校验</li>
 *     <li>校验失败时抛出 ConstraintViolationException 异常</li>
 *     <li>支持分组校验</li>
 * </ul>
 *
 * <p>
 * 使用前准备：
 * <ol>
 *     <li>在实体类字段上添加校验注解（如 @NotNull、@NotBlank、@Size 等）</li>
 *     <li>确保 Spring 容器中已注册 Validator Bean</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * UserDTO user = new UserDTO();
 * ValidatorUtil.validate(user);  // 校验失败会抛出异常
 * </pre>
 *
 * <p>
 * 工具类说明：
 * <ul>
 *     <li>不允许实例化</li>
 *     <li>所有方法均为静态方法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidatorUtil {

    /**
     * 从 Spring 容器获取 Validator 实例
     */
    private static final Validator VALID = SpringUtil.getBean(Validator.class);

    /**
     * 校验对象
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>校验对象的字段是否符合校验规则</li>
     *     <li>支持分组校验（通过 groups 参数）</li>
     *     <li>校验失败时抛出 ConstraintViolationException 异常</li>
     *     <li>适用于参数校验、数据校验等场景</li>
     * </ul>
     *
     * @param object 需要校验的对象
     * @param groups 校验分组（可选，用于分组校验）
     * @param <T>    对象类型
     * @throws ConstraintViolationException 校验失败时抛出，包含所有校验错误信息
     */
    public static <T> void validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> validate = VALID.validate(object, groups);
        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("参数校验异常", validate);
        }
    }
}