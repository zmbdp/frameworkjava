package com.zmbdp.common.domain.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 *
 * @author 稚名不带撇
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    //---------------------------2xx

    /**
     * 操作成功
     */
    SUCCESS(200000, "操作成功"),

    //------------------------4xx
    //400

    /**
     * 无效的参数
     */
    INVALID_PARA(400000, "无效的参数"),
    /**
     * 无效的验证码
     */
    INVALID_CODE(400001, "无效的验证码"),

    /**
     * 错误的验证码
     */
    ERROR_CODE(400002, "错误的验证码"),

    /**
     * 手机号格式错误
     */
    ERROR_PHONE_FORMAT(400003, "手机号格式错误"),

    /**
     * 超过每日发送次数限制
     */
    SEND_MSG_OVERLIMIT(400004, "超过每日发送次数限制"),

    /**
     * 无效的区划
     */
    INVALID_REGION(400005, "无效的区划"),

    /**
     * 参数类型不匹配
     */
    PARA_TYPE_MISMATCH(400006, "参数类型不匹配"),

    /**
     * 账号已停用，登录失败
     */
    USER_DISABLE(400007, "账号已停用，登录失败"),

    //401

    /**
     * 令牌不能为空
     */
    TOKEN_EMPTY(401000, "令牌不能为空"),

    /**
     * 令牌已过期或验证不正确！
     */
    TOKEN_INVALID(401001, "令牌已过期或验证不正确！"),

    /**
     * 令牌已过期！
     */
    TOKEN_OVERTIME(401002, "令牌已过期！"),

    /**
     * 登录状态已过期！
     */
    LOGIN_STATUS_OVERTIME(401003, "登录状态已过期！"),

    /**
     * 令牌验证失败！
     */
    TOKEN_CHECK_FAILED(401004, "令牌验证失败！"),


    //404

    /**
     * 服务未找到！
     */
    SERVICE_NOT_FOUND(404000, "服务未找到"),

    /**
     * url 未找到
     */
    URL_NOT_FOUND(404001, "url 未找到"),

    //405

    /**
     * 请求方法不支持！
     */
    REQUEST_METNHOD_NOT_SUPPORTED(405000, "请求方法不支持"),

    //---------------------5xx

    /**
     * 服务繁忙请稍后重试！
     */
    ERROR(500000, "服务繁忙请稍后重试"),

    /**
     * 操作失败
     */
    FAILED(500001, "操作失败"),

    /**
     * 短信发送失败
     */
    SEND_MSG_FAILED(500002, "短信发送失败"),


    /**
     * 获取直传地址失败
     */
    PRE_SIGN_URL_FAILED(500003, "获取直传地址失败"),

    /**
     * 上传oss异常，请稍后重试
     */
    OSS_UPLOAD_FAILED(500004, "上传 oss 异常，请稍后重试"),

    /**
     * 获取地图数据失败，请稍后重试
     */
    QQMAP_QUERY_FAILED(500005, "获取地图数据失败，请稍后重试"),

    /**
     * 城市信息获取失败
     */
    QQMAP_CITY_UNKNOW(500006, "城市信息获取失败"),

    /**
     * 根据位置获取城市失败
     */
    QQMAP_LOCATE_FAILED(500007, "根据位置获取城市失败"),

    /**
     * 地图特性未开启
     */
    MAP_NOT_ENABLED(500008, "地图特性未开启,开启方式参考使用手册"),

    /**
     * 地图区划特性未开启
     */
    MAP_REGION_NOT_ENABLED(500009, "地图区划特性未开启,开启方式参考使用手册"),


    //---------------------枚举占位
    /**
     * 占位专用
     */
    RESERVED(99999999, "占位专用");

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String errMsg;
}
