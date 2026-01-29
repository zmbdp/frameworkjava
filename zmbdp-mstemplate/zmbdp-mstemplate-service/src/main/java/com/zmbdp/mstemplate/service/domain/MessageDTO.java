package com.zmbdp.mstemplate.service.domain;

import lombok.Data;

@Data
public class MessageDTO {

    private String type;

    private String desc;

    /**
     * 幂等性Token（用于MQ消费者幂等性判断）
     */
    private String idempotentToken;
}
