package com.zmbdp.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRegion {

    private Long id;

    /**
     * 区划名称
     */
    private String name;

    /**
     * 区划全称
     */
    private String fullName;

    /**
     * 区域编码
     */
    private String code;

    /**
     * 区域父id
     */
    private Long parentId;

    /**
     * 区域父编码
     */
    private String parentCode;

    /**
     * 拼音
     */
    private String pinyin;

    /**
     * 级别
     */
    private Integer level;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;
}
