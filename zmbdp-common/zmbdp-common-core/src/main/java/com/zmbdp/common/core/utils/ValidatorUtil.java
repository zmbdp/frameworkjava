package com.zmbdp.common.core.utils;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Validator 校验框架工具类
 * <p>
 * 基于 Jakarta Validation（Bean Validation）提供数据校验功能。
 * 使用 Spring 容器中的 Validator Bean 进行校验，支持分组校验。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>基于 Jakarta Validation（JSR-303/JSR-380）标准</li>
 *     <li>使用 Spring 容器中的 Validator Bean</li>
 *     <li>支持分组校验（通过 groups 参数）</li>
 *     <li>校验失败时抛出 ConstraintViolationException 异常</li>
 *     <li>异常中包含所有校验错误信息</li>
 * </ul>
 * <p>
 * <b>使用前准备：</b>
 * <ol>
 *     <li>在实体类字段上添加校验注解（如 @NotNull、@NotBlank、@Size、@Email 等）</li>
 *     <li>确保 Spring 容器中已注册 Validator Bean（通常 Spring Boot 会自动配置）</li>
 *     <li>引入 Jakarta Validation 相关依赖</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 实体类示例
 * public class UserDTO {
 *     @NotBlank(message = "用户名不能为空")
 *     private String username;
 *
 *     @Email(message = "邮箱格式不正确")
 *     private String email;
 *
 *     @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
 *     private String password;
 * }
 *
 * // 基本校验
 * UserDTO user = new UserDTO();
 * ValidatorUtil.validate(user);  // 校验失败会抛出 ConstraintViolationException
 *
 * // 分组校验
 * public interface CreateGroup {}
 * public interface UpdateGroup {}
 *
 * @NotNull(groups = CreateGroup.class)
 * private Long id;
 *
 * ValidatorUtil.validate(user, CreateGroup.class);  // 只校验 CreateGroup 分组的注解
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>Validator 从 Spring 容器中获取，如果容器中没有会抛出异常</li>
 *     <li>校验失败会抛出 ConstraintViolationException，包含所有校验错误</li>
 *     <li>可以通过异常获取详细的校验错误信息</li>
 *     <li>支持分组校验，可以只校验特定分组的注解</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see jakarta.validation.Validator
 * @see jakarta.validation.ConstraintViolationException
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidatorUtil {

    /**
     * 从 Spring 容器获取 Validator 实例
     */
    private static final Validator VALID = SpringUtil.getBean(Validator.class);

    /**
     * 校验对象
     * <p>
     * 使用 Jakarta Validation 对对象进行校验，检查对象字段上的校验注解。
     * 如果校验失败，会抛出 ConstraintViolationException 异常，包含所有校验错误信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 基本校验
     * UserDTO user = new UserDTO();
     * user.setUsername("");  // 空字符串
     * try {
     *     ValidatorUtil.validate(user);
     * } catch (ConstraintViolationException e) {
     *     // 获取所有校验错误
     *     Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
     *     for (ConstraintViolation<?> violation : violations) {
     *         System.out.println(violation.getMessage());
     *     }
     * }
     *
     * // 分组校验
     * public interface CreateGroup {}
     * public interface UpdateGroup {}
     *
     * @NotNull(groups = CreateGroup.class)
     * private Long id;
     *
     * // 只校验 CreateGroup 分组的注解
     * ValidatorUtil.validate(user, CreateGroup.class);
     *
     * // 校验多个分组
     * ValidatorUtil.validate(user, CreateGroup.class, UpdateGroup.class);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>参数校验（Controller 层）</li>
     *     <li>业务数据校验（Service 层）</li>
     *     <li>DTO 对象校验</li>
     *     <li>需要分组校验的场景（如创建和更新使用不同的校验规则）</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 object 为 null，不会抛出异常（null 校验需要 @NotNull 注解）</li>
     *     <li>如果校验通过，方法正常返回（不抛出异常）</li>
     *     <li>如果校验失败，抛出 ConstraintViolationException，包含所有校验错误</li>
     *     <li>groups 参数用于分组校验，只校验指定分组的注解</li>
     *     <li>如果不传 groups，校验所有注解（包括没有分组的注解）</li>
     *     <li>异常中的 ConstraintViolation 包含字段路径、错误消息等信息</li>
     * </ul>
     *
     * @param object 需要校验的对象，可以为 null（null 校验需要 @NotNull 注解）
     * @param groups 校验分组（可选），只校验指定分组的注解；如果不传，校验所有注解
     * @param <T>    对象类型
     * @throws ConstraintViolationException 当校验失败时抛出，包含所有校验错误信息
     * @see jakarta.validation.constraints.NotNull
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    public static <T> void validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> validate = VALID.validate(object, groups);
        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("参数校验异常", validate);
        }
    }
}