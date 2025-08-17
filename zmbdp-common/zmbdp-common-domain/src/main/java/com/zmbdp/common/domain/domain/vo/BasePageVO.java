package com.zmbdp.common.domain.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页 VO 基类
 *
 * @param <T> 报文类型
 * @author 稚名不带撇
 */
@Data
public class BasePageVO<T> {

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
}