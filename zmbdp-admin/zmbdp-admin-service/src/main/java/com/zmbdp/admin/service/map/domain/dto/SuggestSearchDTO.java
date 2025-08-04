package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 城市搜索地点查询条件
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
     * 限制是否仅在当前城市中搜索 0 - 否; 1 - 是
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
