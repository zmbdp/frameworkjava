package com.zmbdp.common.security.handler;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理应用程序中抛出的各种异常，将异常信息转换为标准的响应格式返回给客户端。<br>
 * 使用 {@code @RestControllerAdvice} 注解，自动拦截所有控制器抛出的异常。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>统一异常处理</b>：集中处理所有异常，避免在每个控制器中重复处理</li>
 *     <li><b>标准响应格式</b>：所有异常都转换为 {@link com.zmbdp.common.domain.domain.Result} 格式</li>
 *     <li><b>HTTP 状态码映射</b>：根据错误码自动设置对应的 HTTP 状态码</li>
 *     <li><b>异常日志记录</b>：记录异常信息，便于问题排查</li>
 * </ul>
 * <p>
 * <b>支持的异常类型：</b>
 * <ul>
 *     <li>{@link HttpRequestMethodNotSupportedException}：请求方式不支持</li>
 *     <li>{@link MethodArgumentTypeMismatchException}：参数类型不匹配</li>
 *     <li>{@link NoResourceFoundException}：URL 未找到</li>
 *     <li>{@link ServiceException}：业务异常</li>
 *     <li>{@link MethodArgumentNotValidException}：参数校验异常（@Valid）</li>
 *     <li>{@link ConstraintViolationException}：参数校验异常（@Validated）</li>
 *     <li>{@link RuntimeException}：运行时异常</li>
 *     <li>{@link Exception}：系统异常（兜底处理）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 控制器中直接抛出异常，全局异常处理器会自动处理
 * @RestController
 * public class UserController {
 *     @GetMapping("/user/{id}")
 *     public Result<User> getUser(@PathVariable Long id) {
 *         if (id == null) {
 *             throw new ServiceException(ResultCode.INVALID_PARA, "用户ID不能为空");
 *         }
 *         return Result.success(userService.getById(id));
 *     }
 * }
 *
 * // 参数校验异常会自动处理
 * @PostMapping("/user")
 * public Result<String> createUser(@Valid @RequestBody UserDTO userDTO) {
 *     // 如果 userDTO 校验失败，会自动返回校验错误信息
 *     return Result.success("创建成功");
 * }
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>HTTP 状态码从错误码的前三位提取（如 400001 -> 400）</li>
 *     <li>参数校验异常会合并所有错误信息，用分号分隔</li>
 *     <li>所有异常都会记录日志，包含请求地址和异常信息</li>
 *     <li>业务异常使用自定义错误码和错误信息</li>
 *     <li>系统异常统一返回通用错误码和错误信息</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.domain.domain.Result
 * @see com.zmbdp.common.domain.domain.ResultCode
 * @see com.zmbdp.common.domain.exception.ServiceException
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    /**
     * 设置 HTTP 响应状态码
     * <p>
     * 从错误码中提取前三位数字作为 HTTP 状态码。<br>
     * 例如：错误码 400001 会设置 HTTP 状态码为 400。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 设置 HTTP 状态码为 400
     * setResponseCode(response, 400001);
     * // HTTP 响应状态码会被设置为 400
     *
     * // 设置 HTTP 状态码为 500
     * setResponseCode(response, 500001);
     * // HTTP 响应状态码会被设置为 500
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>错误码必须是至少 3 位数字</li>
     *     <li>HTTP 状态码从错误码的前三位提取</li>
     *     <li>如果错误码格式不正确，可能抛出异常</li>
     * </ul>
     *
     * @param response HTTP 响应对象，不能为 null
     * @param errCode  错误码，必须是至少 3 位数字（如 400001）
     */
    private void setResponseCode(HttpServletResponse response, Integer errCode) {
        // 把前面三个拿出来返回给前端，设置 http 响应码
        int httpCode = Integer.parseInt(String.valueOf(errCode).substring(0, 3));
        response.setStatus(httpCode);
    }

    /**
     * 处理请求方式不支持异常
     * <p>
     * 当客户端使用了不支持的 HTTP 请求方式（如 GET、POST、PUT、DELETE 等）时触发。<br>
     * 例如：接口只支持 POST 请求，但客户端发送了 GET 请求。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 接口只支持 POST 请求
     * @PostMapping("/user")
     * public Result<String> createUser() {
     *     return Result.success("创建成功");
     * }
     *
     * // 如果客户端发送 GET /user 请求，会触发此异常处理
     * // 返回：Result.fail(405001, "请求方式不支持")
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 405（Method Not Allowed）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#REQUEST_METHOD_NOT_SUPPORTED}</li>
     *     <li>会记录请求地址和不支持的请求方式</li>
     * </ul>
     *
     * @param e        请求方式不支持异常，不能为 null
     * @param request  HTTP 请求对象，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含错误码和错误信息
     * @see org.springframework.web.HttpRequestMethodNotSupportedException
     * @see com.zmbdp.common.domain.domain.ResultCode#REQUEST_METHOD_NOT_SUPPORTED
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String requestURI = request.getRequestURI();
        log.error("请求地址 '{}', 不支持 '{}' 请求", requestURI, e.getMethod());
        setResponseCode(response, ResultCode.REQUEST_METHOD_NOT_SUPPORTED.getCode());
        return Result.fail(ResultCode.REQUEST_METHOD_NOT_SUPPORTED.getCode(), ResultCode.REQUEST_METHOD_NOT_SUPPORTED.getErrMsg());
    }

    /**
     * 处理参数类型不匹配异常
     * <p>
     * 当方法参数类型与请求参数类型不匹配时触发。<br>
     * 例如：接口期望 Long 类型参数，但客户端传递了字符串 "abc"。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 接口期望 Long 类型参数
     * @GetMapping("/user/{id}")
     * public Result<User> getUser(@PathVariable Long id) {
     *     return Result.success(userService.getById(id));
     * }
     *
     * // 如果客户端请求 GET /user/abc（id 不是数字），会触发此异常处理
     * // 返回：Result.fail(400002, "参数类型不匹配")
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 400（Bad Request）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#PARA_TYPE_MISMATCH}</li>
     *     <li>常见场景：路径变量类型转换失败、请求参数类型转换失败</li>
     * </ul>
     *
     * @param e        参数类型不匹配异常，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含错误码和错误信息
     * @see org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
     * @see com.zmbdp.common.domain.domain.ResultCode#PARA_TYPE_MISMATCH
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public Result<?> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletResponse response
    ) {
        log.error("类型不匹配异常", e);
        setResponseCode(response, ResultCode.PARA_TYPE_MISMATCH.getCode());
        return Result.fail(ResultCode.PARA_TYPE_MISMATCH.getCode(), ResultCode.PARA_TYPE_MISMATCH.getErrMsg());
    }

    /**
     * 处理 URL 未找到异常
     * <p>
     * 当请求的 URL 路径不存在时触发。<br>
     * 例如：客户端请求了不存在的接口路径。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 如果客户端请求不存在的路径
     * // GET /api/nonexistent
     * // 会触发此异常处理
     * // 返回：Result.fail(404001, "URL未找到")
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 404（Not Found）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#URL_NOT_FOUND}</li>
     *     <li>常见场景：接口路径拼写错误、接口不存在</li>
     * </ul>
     *
     * @param e        URL 未找到异常，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含错误码和错误信息
     * @see org.springframework.web.servlet.resource.NoResourceFoundException
     * @see com.zmbdp.common.domain.domain.ResultCode#URL_NOT_FOUND
     */
    @ExceptionHandler({NoResourceFoundException.class})
    public Result<?> handleMethodNoResourceFoundException(
            NoResourceFoundException e,
            HttpServletResponse response
    ) {
        log.error("url 未找到异常", e);
        setResponseCode(response, ResultCode.URL_NOT_FOUND.getCode());
        return Result.fail(ResultCode.URL_NOT_FOUND.getCode(), ResultCode.URL_NOT_FOUND.getErrMsg());
    }

    /**
     * 处理业务异常
     * <p>
     * 处理应用程序中抛出的业务异常，返回自定义的错误码和错误信息。<br>
     * 业务异常通常用于处理业务逻辑错误，如数据不存在、操作不允许等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在业务代码中抛出业务异常
     * @Service
     * public class UserService {
     *     public User getUserById(Long id) {
     *         User user = userMapper.selectById(id);
     *         if (user == null) {
     *             throw new ServiceException(ResultCode.DATA_NOT_FOUND, "用户不存在");
     *         }
     *         return user;
     *     }
     * }
     *
     * // 全局异常处理器会自动捕获并返回
     * // Result.fail(404002, "用户不存在")
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码从异常的错误码前三位提取</li>
     *     <li>返回异常中自定义的错误码和错误信息</li>
     *     <li>会记录请求地址和异常信息</li>
     *     <li>适用于业务逻辑错误，不适用于系统异常</li>
     * </ul>
     *
     * @param e        业务异常，不能为 null
     * @param request  HTTP 请求对象，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含异常中的错误码和错误信息
     * @see com.zmbdp.common.domain.exception.ServiceException
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e, HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址: '{}',发生业务异常", requestURI, e);
        setResponseCode(response, e.getCode());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid 注解）
     * <p>
     * 处理使用 {@code @Valid} 注解进行参数校验时抛出的异常。<br>
     * 当请求参数不符合校验规则时触发，会合并所有校验错误信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // DTO 类定义校验规则
     * public class UserDTO {
     *     @NotBlank(message = "用户名不能为空")
     *     private String username;
     *
     *     @Email(message = "邮箱格式不正确")
     *     private String email;
     * }
     *
     * // 控制器中使用 @Valid 注解
     * @PostMapping("/user")
     * public Result<String> createUser(@Valid @RequestBody UserDTO userDTO) {
     *     // 如果 userDTO 校验失败，会触发此异常处理
     *     // 返回：Result.fail(400003, "用户名不能为空;邮箱格式不正确")
     *     return Result.success("创建成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 400（Bad Request）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#INVALID_PARA}</li>
     *     <li>多个校验错误会用分号（;）分隔合并</li>
     *     <li>适用于 {@code @Valid} 注解的校验</li>
     * </ul>
     *
     * @param e        参数校验异常，不能为 null
     * @param request  HTTP 请求对象，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含错误码和合并后的错误信息
     * @see org.springframework.web.bind.MethodArgumentNotValidException
     * @see com.zmbdp.common.domain.domain.ResultCode#INVALID_PARA
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生参数校验异常", requestURI, e);
        // 设置 http 响应码
        setResponseCode(response, ResultCode.INVALID_PARA.getCode());
        String message = joinMessage(e);
        return Result.fail(ResultCode.INVALID_PARA.getCode(), message);
    }

    /**
     * 合并参数校验异常信息
     * <p>
     * 将 {@code MethodArgumentNotValidException} 中的所有校验错误信息合并为一个字符串。<br>
     * 多个错误信息用分号（{@code ;}）分隔。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 如果有多个校验错误：
     * // 1. "用户名不能为空"
     * // 2. "邮箱格式不正确"
     * // 3. "年龄必须大于0"
     *
     * // 合并后的结果：
     * // "用户名不能为空;邮箱格式不正确;年龄必须大于0"
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果异常中没有错误信息，返回空字符串</li>
     *     <li>多个错误信息用分号（{@code ;}）分隔</li>
     *     <li>使用流处理提取所有错误信息</li>
     * </ul>
     *
     * @param e 参数校验异常，不能为 null
     * @return 合并后的错误信息字符串，多个错误用分号分隔；如果没有错误返回空字符串
     */
    private String joinMessage(MethodArgumentNotValidException e) {
        // 先获取所有异常信息的列表
        List<ObjectError> allErrors = e.getAllErrors();
        if (CollectionUtils.isEmpty(allErrors)) {
            return CommonConstants.EMPTY_STR;
        }
        // 流处理获取异常信息
        return allErrors
                .stream() // 获取所有错误信息
                .map(ObjectError::getDefaultMessage) // 获取所有错误信息
                .collect(Collectors.joining(CommonConstants.DEFAULT_DELIMITER)); // 转换成字符串, 用分号隔开
    }

    /**
     * 处理参数校验异常（@Validated 注解）
     * <p>
     * 处理使用 {@code @Validated} 注解进行参数校验时抛出的异常。<br>
     * 当方法参数或路径变量不符合校验规则时触发。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 控制器方法参数校验
     * @GetMapping("/user/{id}")
     * public Result<User> getUser(
     *         @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
     *     // 如果 id <= 0，会触发此异常处理
     *     // 返回：Result.fail(400003, "用户ID必须大于0")
     *     return Result.success(userService.getById(id));
     * }
     *
     * // 类级别校验
     * @RestController
     * @Validated
     * public class UserController {
     *     @GetMapping("/user")
     *     public Result<User> getUser(@RequestParam @NotBlank String username) {
     *         // 如果 username 为空，会触发此异常处理
     *         return Result.success(userService.getByUsername(username));
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 400（Bad Request）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#INVALID_PARA}</li>
     *     <li>适用于 {@code @Validated} 注解的校验</li>
     *     <li>返回异常中的错误信息</li>
     * </ul>
     *
     * @param e        参数校验异常，不能为 null
     * @param request  HTTP 请求对象，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含错误码和错误信息
     * @see jakarta.validation.ConstraintViolationException
     * @see com.zmbdp.common.domain.domain.ResultCode#INVALID_PARA
     */
    @ExceptionHandler({ConstraintViolationException.class})
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String requestURI = request.getRequestURI();
        log.error("请求地址: '{}', 发生参数校验异常", requestURI, e);
        // 设置 http 响应码
        setResponseCode(response, ResultCode.INVALID_PARA.getCode());
        String message = e.getMessage();
        return Result.fail(ResultCode.INVALID_PARA.getCode(), message);
    }

    /**
     * 处理运行时异常
     * <p>
     * 处理所有 {@code RuntimeException} 及其子类异常（不包括 {@code ServiceException}）。<br>
     * 作为系统异常的兜底处理，返回通用错误信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 代码中抛出运行时异常
     * @Service
     * public class UserService {
     *     public void deleteUser(Long id) {
     *         if (id == null) {
     *             throw new IllegalArgumentException("用户ID不能为空");
     *         }
     *         // 如果发生空指针异常等运行时异常
     *         // 会触发此异常处理
     *         // 返回：Result.fail(500001, "系统异常")
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 500（Internal Server Error）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#ERROR}</li>
     *     <li>返回通用错误信息，不暴露具体异常信息</li>
     *     <li>会记录详细的异常堆栈信息到日志</li>
     *     <li>不包括 {@code ServiceException}（由专门的方法处理）</li>
     * </ul>
     *
     * @param e        运行时异常，不能为 null
     * @param request  HTTP 请求对象，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含通用错误码和错误信息
     * @see java.lang.RuntimeException
     * @see com.zmbdp.common.domain.domain.ResultCode#ERROR
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(
            RuntimeException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String requestURI = request.getRequestURI();
        log.error("请求地址: '{}', 发生运行时异常. ", requestURI, e);
        setResponseCode(response, ResultCode.ERROR.getCode());
        return Result.fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getErrMsg());
    }

    /**
     * 处理系统异常（兜底处理）
     * <p>
     * 处理所有未被其他异常处理器捕获的异常，作为最后的兜底处理。<br>
     * 确保所有异常都能被正确处理，不会直接暴露给客户端。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 任何未被其他异常处理器捕获的异常都会触发此方法
     * // 例如：IOException、SQLException 等检查异常
     * // 返回：Result.fail(500001, "系统异常")
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码设置为 500（Internal Server Error）</li>
     *     <li>错误码：{@link com.zmbdp.common.domain.domain.ResultCode#ERROR}</li>
     *     <li>返回通用错误信息，不暴露具体异常信息</li>
     *     <li>会记录详细的异常堆栈信息到日志</li>
     *     <li>此方法优先级最低，只有在其他异常处理器都无法处理时才会调用</li>
     * </ul>
     *
     * @param e        系统异常，不能为 null
     * @param request  HTTP 请求对象，不能为 null
     * @param response HTTP 响应对象，不能为 null
     * @return 异常响应结果，包含通用错误码和错误信息
     * @see java.lang.Exception
     * @see com.zmbdp.common.domain.domain.ResultCode#ERROR
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址: '{}', 发生异常. ", requestURI, e);
        setResponseCode(response, ResultCode.ERROR.getCode());
        return Result.fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getErrMsg());
    }
}