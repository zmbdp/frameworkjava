package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 经纬度 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class LocationDTO {

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 经度
     */
    private Double lng;

    /**
     * 格式化经纬度
     *
     * @return 格式化后的经纬度
     */
    public String formatInfo() {
        return lat + "," + lng;
    }
}
