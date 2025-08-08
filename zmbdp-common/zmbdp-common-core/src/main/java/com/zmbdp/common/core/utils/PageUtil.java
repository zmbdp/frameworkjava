package com.zmbdp.common.core.utils;

/**
 * 分页工具类
 *
 * @author 稚名不带撇
 */
public class PageUtil {

    /**
     * 根据总数和页面计算分页总数
     *
     * @param totals   总数量
     * @param pageSize 分页大小
     * @return 分页数量
     */
    public static int getTotalPages(int totals, int pageSize) {
        return totals % pageSize == 0 ? totals / pageSize : (totals / pageSize + 1);
    }
}
