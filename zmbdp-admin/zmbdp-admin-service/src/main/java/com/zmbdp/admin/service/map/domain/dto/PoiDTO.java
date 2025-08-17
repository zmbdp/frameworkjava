package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 地点 poi 信息
 *
 * @author 稚名不带撇
 */
@Data
public class PoiDTO {

    /**
     * poi 地点的唯一标识
     */
    private String id;

    /**
     * poi 地点的名称
     */
    private String title;

    /**
     * 具体地址
     */
    private String address;

    /**
     * POI 类型，值说明：0: 普通 POI / 1: 公交车站 / 2: 地铁站 / 3: 公交线路 / 4: 行政区划
     */
    private String type;

    /**
     * poi 地点所处经纬度坐标
     */
    private LocationDTO location;
}
