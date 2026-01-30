package com.zmbdp.common.log.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zmbdp.common.core.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * <p>
 * 对应数据库表 {@code operation_log}，用于存储业务操作日志。
 *
 * @author 稚名不带撇
 */
@Data
@TableName("operation_log")
@EqualsAndHashCode(callSuper = true)
public class OperationLog extends BaseDO {

    /**
     * 操作描述（必填）
     */
    private String operation;

    /**
     * 方法全限定名（类名#方法名）
     */
    private String method;

    /**
     * 请求路径（HTTP 请求时）
     */
    private String requestPath;

    /**
     * 请求方式（GET/POST/PUT/DELETE 等）
     */
    private String requestMethod;

    /**
     * 方法入参（JSON 格式）
     */
    private String params;

    /**
     * 方法返回值（JSON 格式）
     */
    private String result;

    /**
     * 异常信息（如果方法执行失败）
     */
    private String exception;

    /**
     * 异常堆栈（如果方法执行失败）
     */
    private String exceptionStack;

    /**
     * 操作者用户ID
     */
    private Long userId;

    /**
     * 操作者用户名
     */
    private String userName;

    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 请求来源（User-Agent）
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 方法执行耗时（毫秒）
     */
    private Long costTime;

    /**
     * 操作状态（SUCCESS/FAILED）
     */
    private String status;

    /**
     * 业务模块（可选，用于分类）
     */
    private String module;

    /**
     * 业务类型（可选，用于分类）
     */
    private String businessType;

    /**
     * 扩展字段（JSON 格式，用于存储额外信息）
     */
    private String extInfo;

    /**
     * 调用链追踪ID（traceId）
     */
    private String traceId;

    /**
     * 调用链跨度ID（spanId）
     */
    private String spanId;
}
