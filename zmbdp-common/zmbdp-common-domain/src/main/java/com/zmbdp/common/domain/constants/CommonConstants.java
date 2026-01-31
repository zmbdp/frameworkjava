package com.zmbdp.common.domain.constants;

/**
 * 通用常量
 *
 * @author 稚名不带撇
 */
public class CommonConstants {

    /**
     * 标准时间格式
     */
    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认编码
     */
    public final static String UTF8 = "UTF-8";

    /**
     * 默认分隔符
     */
    public final static String DEFAULT_DELIMITER = "; ";

    /**
     * 逗号分隔符
     */
    public static final String COMMA_SEPARATOR = ",";

    /**
     * 井号分隔符（#）
     * <p>
     * 用于拼接类名和方法名，格式：类名#方法名
     */
    public static final String HASH_SEPARATOR = "#";

    /**
     * 点号分隔符（.）
     * <p>
     * 用于拼接类名和方法名，格式：类名.方法名
     */
    public static final String DOT_SEPARATOR = ".";

    /**
     * 冒号分隔符（:）
     * <p>
     * 常用于 Redis Key 的拼接，格式：前缀:业务:ID
     */
    public static final String COLON_SEPARATOR = ":";

    /**
     * 空字符串
     */
    public final static String EMPTY_STR = "";

    /*=============================================    通用状态常量    =============================================*/

    /**
     * 状态：成功
     */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * 状态：失败
     */
    public static final String STATUS_FAILED = "FAILED";

    /**
     * 状态：处理中
     */
    public static final String STATUS_PROCESSING = "PROCESSING";

    /**
     * 未知标识
     * <p>
     * 某些代理服务器在无法获取真实信息时会设置此值
     */
    public static final String UNKNOWN = "unknown";

    /*=============================================    线程池常量    =============================================*/

    /**
     * 异步线程池名字
     */
    public final static String ASYNCHRONOUS_THREADS_BEAN_NAME = "threadPoolTaskExecutor";

    /**
     * 定时任务线程池名字
     */
    public final static String SCHEDULED_THREADS_BEAN_NAME = "scheduledExecutorService";
}