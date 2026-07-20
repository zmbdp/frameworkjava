package com.zmbdp.common.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 日志异常工具
 *
 * @author 稚名不带撇
 */
public class LogExceptionUtil {

    /**
     * 私有构造方法，防止外部实例化
     */
    private LogExceptionUtil() {
    }

    /**
     * 获取异常堆栈字符串
     * <p>
     * 将 Throwable 的堆栈信息转换为字符串，便于记录日志。
     *
     * @param throwable 异常对象，可以为 null
     * @return 异常堆栈字符串，如果 throwable 为 null 则返回空字符串
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}