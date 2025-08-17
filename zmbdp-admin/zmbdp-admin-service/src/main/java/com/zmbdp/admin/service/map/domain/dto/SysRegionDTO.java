package com.zmbdp.admin.service.map.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 区域信息 DTO
 *
 * @author 稚名不带撇
 */
@Getter
@Setter
public class SysRegionDTO {

    /**
     * 区域 ID
     */
    private Long id;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 区域全称
     */
    private String fullName;

    /**
     * 父级区域 ID
     */
    private Long parentId;

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
