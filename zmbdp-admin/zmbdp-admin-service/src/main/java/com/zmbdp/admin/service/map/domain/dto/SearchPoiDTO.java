package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 查询结果 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class SearchPoiDTO {

    /**
     * 地点名称
     */
    private String title;

    /**
     * 地点地址
     */
    private String address;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;
}