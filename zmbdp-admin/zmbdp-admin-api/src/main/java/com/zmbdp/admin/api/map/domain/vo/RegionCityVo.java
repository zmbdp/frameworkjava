package com.zmbdp.admin.api.map.domain.vo;

import lombok.Data;

/**
 * 城市信息 VO
 *
 * @author 稚名不带撇
 */
@Data
public class RegionCityVo {

    /**
     * 城市 ID
     */
    private Long id;

    /**
     * 城市名称
     */
    private String name;

    /**
     * 城市全称
     */
    private String fullName;
}