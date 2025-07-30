package com.zmbdp.common.domain.domain;

import lombok.Data;

/**
 * 响应报文封装
 *
 * @param <T> 响应数据
 * @author 稚名不带撇
 */
@Data
public class Result<T> {

    /**
     * 响应码
     */
    private int code;

    /**
     * 消息
     */
    private String errMsg;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param <T> 数据类型
     * @return 响应报文
     */
    public static <T> Result<T> success() {
        return restResult(null, ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getErrMsg());
    }

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 响应报文
     */
    public static <T> Result<T> success(T data) {
        return restResult(data, ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getErrMsg());
    }

    /**
     * 成功响应
     *
     * @param data   响应数据
     * @param errMsg 响应消息
     * @param <T>    数据类型
     * @return 响应报文
     */
    public static <T> Result<T> success(T data, String errMsg) {
        return restResult(data, ResultCode.SUCCESS.getCode(), errMsg);
    }

    /*=============================================    失败状态码    =============================================*/

    /**
     * 失败响应
     *
     * @param <T> 数据类型
     * @return 响应报文
     */
    public static <T> Result<T> fail() {
        return restResult(null, ResultCode.ERROR.getCode(), ResultCode.ERROR.getErrMsg());
    }

    /**
     * 失败响应
     *
     * @param errMsg 响应消息
     * @param <T>    数据类型
     * @return 响应报文
     */
    public static <T> Result<T> fail(String errMsg) {
        return restResult(null, ResultCode.ERROR.getCode(), errMsg);
    }

    /**
     * 失败响应
     *
     * @param code   响应码
     * @param errMsg 响应消息
     * @param <T>    数据类型
     * @return 响应报文
     */
    public static <T> Result<T> fail(Integer code, String errMsg) {
        return restResult(null, code, errMsg);
    }

    /**
     * 失败响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 响应报文
     */
    public static <T> Result<T> fail(T data) {
        return restResult(data, ResultCode.ERROR.getCode(), ResultCode.ERROR.getErrMsg());
    }

    /**
     * 失败响应
     *
     * @param data   响应数据
     * @param errMsg 响应消息
     * @param <T>    数据类型
     * @return 响应报文
     */
    public static <T> Result<T> fail(T data, String errMsg) {
        return restResult(data, ResultCode.ERROR.getCode(), errMsg);
    }

    /**
     * 失败响应
     *
     * @param code   响应编码
     * @param errMsg 响应消息
     * @param <T>    数据类型
     * @return 响应报文
     */
    public static <T> Result<T> fail(int code, String errMsg) {
        return restResult(null, code, errMsg);
    }

    /**
     * 响应结果
     *
     * @param data   响应数据
     * @param code   响应编码
     * @param errMsg 响应消息
     * @param <T>    数据类型
     * @return 响应报文
     */
    private static <T> Result<T> restResult(T data, int code, String errMsg) {
        Result<T> apiResult = new Result<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setErrMsg(errMsg);
        return apiResult;
    }
}
