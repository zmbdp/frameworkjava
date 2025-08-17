package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 城市搜索地点查询条件
 *
 * @author 稚名不带撇
 */
@Data
public class SuggestSearchDTO {

    /**
     * 搜索的关键字
     */
    private String keyword;

    /**
     * 城市 id（邮编）
     */
    private String id;

    /**
     * 0：[默认] 不限制当前城市，会召回其他城市的 poi
     * 1：仅限制在当前城市
     */
    private String regionFix;

    /**
     * 页码
     */
    private Integer pageIndex;

    /**
     * 每页的数量
     */
    private Integer pageSize;
}
