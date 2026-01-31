package com.zmbdp.common.log.mq.producer;

import com.zmbdp.common.log.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 操作日志生产者
 * <p>
 * 负责将操作日志 JSON 发送到 RabbitMQ 广播交换机。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>由 {@link com.zmbdp.common.log.service.impl.MqLogStorageService} 调用</li>
 *     <li>发送到 {@link RabbitConfig#OPERATION_LOG_EXCHANGE} 广播交换机</li>
 *     <li>使用 Fanout 模式，所有绑定的队列都会收到消息</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class OperationLogProducer {

    /**
     * RabbitMQ 模板
     */
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送操作日志消息到 RabbitMQ
     * <p>
     * 将日志 JSON 发送到广播交换机，所有绑定的队列都会收到消息。
     *
     * @param logJson 操作日志 JSON 字符串
     */
    public void sendMessage(String logJson) {
        try {
            if (rabbitTemplate == null) {
                log.warn("RabbitTemplate 未注入，无法发送日志到消息队列");
                return;
            }
            log.debug("发送操作日志到消息队列：{}", logJson);
            rabbitTemplate.convertAndSend(RabbitConfig.OPERATION_LOG_EXCHANGE, "", logJson);
        } catch (Exception e) {
            log.error("发送操作日志到消息队列失败: {}", e.getMessage(), e);
        }
    }
}