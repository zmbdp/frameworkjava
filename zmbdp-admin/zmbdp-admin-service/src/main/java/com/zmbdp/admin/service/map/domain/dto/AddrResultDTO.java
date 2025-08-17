package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 逆地址解析结果
 *
 * @author 稚名不带撇
 */
@Data
public class AddrResultDTO {

    /**
     * 具体的详细地址
     */
    private String address;

    /**
     * 城市地址详细信息
     */
    private AdInfoDTO ad_info;
}