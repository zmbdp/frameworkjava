package com.zmbdp.common.log.mq.consumer;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.log.config.RabbitConfig;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 操作日志 MQ 消费者
 * <p>
 * 监听 {@link RabbitConfig#OPERATION_LOG_EXCHANGE} 广播交换机，接收操作日志消息。
 * 使用匿名队列绑定，应用下线后队列自动删除，无需手动维护。
 * <p>
 * <b>扩展说明：</b>
 * <ul>
 *     <li>当前仅输出 debug 日志，可按需扩展：解析 JSON 为 {@link OperationLogDTO}，落库或同步到 ES 等</li>
 *     <li>可注入 {@link com.zmbdp.common.log.mapper.OperationLogMapper} 或自定义存储服务进行持久化</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.log.mq.producer.OperationLogProducer
 */
@Slf4j
@Component
@RabbitListener(bindings = @QueueBinding(
        value = @Queue(), // 匿名队列，应用下线后自动删除，无需担心队列堆积
        exchange = @Exchange(value = RabbitConfig.OPERATION_LOG_EXCHANGE, type = ExchangeTypes.FANOUT)
))
public class OperationLogConsumer {

    /**
     * 处理操作日志消息
     * <p>
     * 消息体为 {@link OperationLogDTO} 的 JSON 序列化结果，
     * 可使用 {@link JsonUtil#jsonToClass(String, Class)} 反序列化为 {@link OperationLogDTO} 后落库或做其他处理。
     *
     * @param logJson 操作日志 JSON 字符串
     */
    @RabbitHandler
    public void process(String logJson) {
        try {
            // TODO: 用户自定义处理逻辑
            log.debug("[MQ接收操作日志] {}", logJson);
        } catch (Exception e) {
            log.error("MQ 消费操作日志失败: {}", logJson, e);
        }
    }
}