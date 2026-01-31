package com.zmbdp.common.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 日志异常工具
 *
 * @author 稚名不带撇
 */
public class LogExceptionUtil {

    private LogExceptionUtil() {
    }

    /**
     * 获取异常堆栈字符串
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