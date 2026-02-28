package com.zmbdp.mstemplate.service.domain.dto;

import lombok.Data;

/**
 * 消息传输对象
 *
 * @author 稚名不带撇
 */
@Data
public class MessageDTO {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息描述
     */
    private String desc;

    /**
     * 幂等性Token（用于MQ消费者幂等性判断）
     */
    private String idempotentToken;
}