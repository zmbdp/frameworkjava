package com.zmbdp.common.domain.constants;

/**
 * 日志相关常量
 *
 * @author 稚名不带撇
 */
public class LogConstants {

    /**
     * 日志存储的默认 Key 前缀（Redis 或数据库表名）
     */
    public static final String LOG_KEY_PREFIX = "log:operation:";

    /**
     * Nacos 上日志功能是否启用配置名称
     */
    public static final String NACOS_LOG_ENABLED_PREFIX = "log.enabled";

    /**
     * Nacos 上日志异步处理配置名称
     */
    public static final String NACOS_LOG_ASYNC_ENABLED_PREFIX = "log.async-enabled";

    /**
     * Nacos 上日志存储类型配置名称（console/database/file/redis/mq）
     */
    public static final String NACOS_LOG_STORAGE_TYPE_PREFIX = "log.storage-type";

    /**
     * Nacos 上全局默认是否记录参数配置名称
     */
    public static final String NACOS_LOG_DEFAULT_RECORD_PARAMS_PREFIX = "log.default.record-params";

    /**
     * Nacos 上全局默认是否记录返回值配置名称
     */
    public static final String NACOS_LOG_DEFAULT_RECORD_RESULT_PREFIX = "log.default.record-result";

    /**
     * Nacos 上全局默认是否记录异常配置名称
     */
    public static final String NACOS_LOG_DEFAULT_RECORD_EXCEPTION_PREFIX = "log.default.record-exception";

    /**
     * Nacos 上全局默认异常时是否抛出异常配置名称
     */
    public static final String NACOS_LOG_DEFAULT_THROW_EXCEPTION_PREFIX = "log.default.throw-exception";

    /**
     * Nacos 上是否启用全局默认记录配置名称（即使没有注解也记录基本信息）
     */
    public static final String NACOS_LOG_GLOBAL_RECORD_ENABLED_PREFIX = "log.global-record-enabled";

    /**
     * 日志功能默认启用状态
     */
    public static final boolean LOG_ENABLED_DEFAULT = true;

    /**
     * 日志异步处理默认启用状态
     */
    public static final boolean LOG_ASYNC_ENABLED_DEFAULT = true;

    /**
     * 全局默认是否记录参数
     */
    public static final boolean DEFAULT_RECORD_PARAMS = false;

    /**
     * 全局默认是否记录返回值
     */
    public static final boolean DEFAULT_RECORD_RESULT = false;

    /**
     * 全局默认是否记录异常
     */
    public static final boolean DEFAULT_RECORD_EXCEPTION = true;

    /**
     * 全局默认异常时是否抛出异常
     */
    public static final boolean DEFAULT_THROW_EXCEPTION = true;

    /**
     * 全局默认记录是否启用（即使没有注解也记录基本信息）
     */
    public static final boolean GLOBAL_RECORD_ENABLED_DEFAULT = false;

    /**
     * 日志存储类型：控制台（默认）
     */
    public static final String STORAGE_TYPE_CONSOLE = "console";

    /**
     * 日志存储类型：数据库
     */
    public static final String STORAGE_TYPE_DATABASE = "database";

    /**
     * 日志存储类型：文件
     */
    public static final String STORAGE_TYPE_FILE = "file";

    /**
     * 日志存储类型：Redis
     */
    public static final String STORAGE_TYPE_REDIS = "redis";

    /**
     * 日志存储类型：消息队列
     */
    public static final String STORAGE_TYPE_MQ = "mq";

    /**
     * 默认日志存储类型
     */
    public static final String STORAGE_TYPE_DEFAULT = STORAGE_TYPE_CONSOLE;

    /**
     * 敏感字段脱敏：手机号（保留前3位和后4位）
     */
    public static final String DESENSITIZE_PHONE = "phone";

    /**
     * 敏感字段脱敏：身份证号（保留前6位和后4位）
     */
    public static final String DESENSITIZE_ID_CARD = "idCard";

    /**
     * 敏感字段脱敏：邮箱（保留@前3位和@后全部）
     */
    public static final String DESENSITIZE_EMAIL = "email";

    /**
     * 敏感字段脱敏：密码（全部替换为*）
     */
    public static final String DESENSITIZE_PASSWORD = "password";

    /**
     * 敏感字段脱敏：银行卡号（保留前4位和后4位）
     */
    public static final String DESENSITIZE_BANK_CARD = "bankCard";

    /*=============================================    日志状态常量    =============================================*/

    /**
     * 日志状态：成功
     * <p>
     * 注意：此常量已废弃，请使用 {@link CommonConstants#STATUS_SUCCESS}
     *
     * @deprecated 使用 {@link CommonConstants#STATUS_SUCCESS} 替代
     */
    @Deprecated
    public static final String LOG_STATUS_SUCCESS = CommonConstants.STATUS_SUCCESS;

    /**
     * 日志状态：失败
     * <p>
     * 注意：此常量已废弃，请使用 {@link CommonConstants#STATUS_FAILED}
     *
     * @deprecated 使用 {@link CommonConstants#STATUS_FAILED} 替代
     */
    @Deprecated
    public static final String LOG_STATUS_FAILED = CommonConstants.STATUS_FAILED;
}