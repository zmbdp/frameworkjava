package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 地图 poi
 *
 * @author 稚名不带撇
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PoiListDTO extends QQMapBaseResponseDTO {

    /**
     * 本次搜索的结果数
     */
    private Integer count;

    /**
     * 查出来的 poi 列表
     */
    private List<PoiDTO> data;
}