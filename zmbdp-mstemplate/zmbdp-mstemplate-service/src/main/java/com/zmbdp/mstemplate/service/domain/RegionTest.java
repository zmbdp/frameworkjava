package com.zmbdp.mstemplate.service.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 区划测试实体
 *
 * @author 稚名不带撇
 */
@Getter
@Setter
public class RegionTest {

    /**
     * 区划 id
     */
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
