package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 城市信息 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class RegionCityDTO {

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