package com.zmbdp.common.security.handler;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
 * 全局异常处理工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 设置 http 响应码
     *
     * @param response 响应信息
     * @param errCode  响应码
     */
    private void setResponseCode(HttpServletResponse response, Integer errCode) {
        // 把前面三个拿出来返回给前端，设置 http 响应码
        int httpCode = Integer.parseInt(String.valueOf(errCode).substring(0, 3));
        response.setStatus(httpCode);
    }

    /**
     * 请求方式不支持
     *
     * @param e        异常信息
     * @param request  请求
     * @param response 响应
     * @return 异常结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String requestURI = request.getRequestURI();
        log.error("请求地址 '{}', 不支持 '{}' 请求", requestURI, e.getMethod());
        setResponseCode(response, ResultCode.REQUEST_METNHOD_NOT_SUPPORTED.getCode());
        return Result.fail(ResultCode.REQUEST_METNHOD_NOT_SUPPORTED.getCode(), ResultCode.REQUEST_METNHOD_NOT_SUPPORTED.getErrMsg());
    }


    /**
     * 类型不匹配异常
     *
     * @param e        异常信息
     * @param response 响应
     * @return 不匹配结果
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
     * url未找到异常
     *
     * @param e        异常信息
     * @param response 响应
     * @return 异常结果
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
     * 业务异常
     *
     * @param e        异常信息
     * @param request  请求
     * @param response 响应
     * @return 业务异常结果
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e, HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址: '{}',发生业务异常", requestURI, e);
        setResponseCode(response, e.getCode());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     *
     * @param e        异常信息
     * @param request  请求
     * @param response 响应
     * @return 异常报文
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
     * 参数校验异常
     *
     * @param e 异常信息
     * @return 参数校验异常信息
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
     * 参数校验异常
     *
     * @param e        异常信息
     * @param request  请求
     * @param response 响应
     * @return 异常报文
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
     * 拦截运行时异常
     *
     * @param e        异常信息
     * @param request  请求信息
     * @param response 响应信息
     * @return 响应结果
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
     * 系统异常
     *
     * @param e        异常信息
     * @param request  请求
     * @param response 响应
     * @return 响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求地址: '{}', 发生异常. ", requestURI, e);
        setResponseCode(response, ResultCode.ERROR.getCode());
        return Result.fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getErrMsg());
    }

}