package com.zmbdp.admin.api.map.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询请求 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class PlaceSearchReqDTO extends BasePageReqDTO {

    /**
     * 请求的关键字
     */
    @NotNull(message = "请求关键字不允许为空")
    private String keyword;

    /**
     * 请求区域 ID
     */
    @NotNull(message = "请求区域 ID 不能为空")
    private Long id;

    /**
     * 0：[默认] 不限制当前城市，会召回其他城市的 poi
     * 1：仅限制在当前城市
     */
    @NotNull(message = "请求区域限制不能为空")
    private String regionFix;
}