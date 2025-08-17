package com.zmbdp.common.domain.exception;

import com.zmbdp.common.domain.domain.ResultCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 自定义异常
 *
 * @author 稚名不带撇
 */
@Getter
@Setter
public class ServiceException extends RuntimeException {

    /**
     * 错误码
     */
    private int code;

    /**
     * 错误提示
     */
    private String message;

    //推荐的使用方法

    /**
     * 响应构造异常
     *
     * @param resultCode 响应信息
     */
    public ServiceException(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.message = resultCode.getErrMsg();
    }

    /**
     * 消息构造异常
     *
     * @param message 异常消息
     */
    public ServiceException(String message) {
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    /**
     * 消息和响应码定制异常
     *
     * @param message 消息
     * @param code    响应码
     */
    public ServiceException(String message, int code) {
        this.code = code;
        this.message = message;
    }
}
