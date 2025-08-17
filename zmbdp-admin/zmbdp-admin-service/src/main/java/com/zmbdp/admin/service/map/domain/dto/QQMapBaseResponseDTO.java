package com.zmbdp.admin.service.map.domain.dto;

import lombok.Data;

/**
 * 腾讯位置服务响应基类
 *
 * @author 稚名不带撇
 */
@Data
public class QQMapBaseResponseDTO {

    /**
     * 响应码  0 - 表示成功
     */
    private Integer status;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 请求 ID
     */
    private String request_id;
}
