package com.zmbdp.common.core.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 响应结果分页报文
 *
 * @author 稚名不带撇
 */
@Data
public class BasePageDTO<T> {

    /**
     * 查询结果总数
     */
    Integer totals;

    /**
     * 总页数
     */
    Integer totalPages;

    /**
     * 数据列表
     */
    List<T> list;

    /**
     * 计算总页数
     *
     * @param totals   总数量
     * @param pageSize 页大小
     * @return 页数
     */
    public static int calculateTotalPages(long totals, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        return (int) Math.ceil((double) totals / pageSize);
    }

}
