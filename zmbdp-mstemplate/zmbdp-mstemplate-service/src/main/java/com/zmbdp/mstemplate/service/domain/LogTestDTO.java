package com.zmbdp.mstemplate.service.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 日志注解测试用 DTO
 * <p>
 * 包含用于测试 recordParams、paramsExpression、desensitizeFields 等功能的字段。
 *
 * @author 稚名不带撇
 */
@Data
public class LogTestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 手机号（用于脱敏测试）
     */
    private String phone;

    /**
     * 密码（用于脱敏测试）
     */
    private String password;

    /**
     * 操作类型
     */
    private String actionType;

    /**
     * 备注
     */
    private String remark;
}