package com.zmbdp.common.domain.domain.dto;

import lombok.Data;

/**
 * 分页查询基类 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class BasePageReqDTO {

    /**
     * 第几页
     */
    private Integer pageNo = 1;

    /**
     * 分页数量
     */
    private Integer pageSize = 10;

    /**
     * 获取偏移
     *
     * @return 偏移信息
     */
    public Integer getOffset() {
        return (pageNo - 1) * pageSize;
    }

}