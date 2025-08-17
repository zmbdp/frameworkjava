package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 逆地址解析的结果
 *
 * @author 稚名不带撇
 */
@Data
public class GeoResultDTO extends QQMapBaseResponseDTO {

    /**
     * 结果信息
     */
    private AddrResultDTO result;
}