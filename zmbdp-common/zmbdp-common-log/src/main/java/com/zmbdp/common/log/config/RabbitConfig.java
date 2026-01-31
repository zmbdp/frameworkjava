package com.zmbdp.common.log.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 操作日志 RabbitMQ 配置
 * <p>
 * 自管理 MQ 相关 Bean：声明交换机，并扫描 {@code com.zmbdp.common.log.mq} 包注册生产者和消费者。
 * 使用 Fanout 广播模式，消息发送到交换机后，所有绑定的队列都会收到。
 * <p>
 * <b>生效条件：</b>
 * <ul>
 *     <li>需引入 {@code zmbdp-common-rabbitmq} 依赖</li>
 *     <li>应用需配置 RabbitMQ 连接信息</li>
 * </ul>
 * <p>
 * <b>扫描内容：</b>
 * <ul>
 *     <li>{@link com.zmbdp.common.log.mq.producer.OperationLogProducer}：将日志 JSON 发送到交换机</li>
 *     <li>{@link com.zmbdp.common.log.mq.consumer.OperationLogConsumer}：消费消息，可扩展持久化到 DB/ES 等</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Configuration
@ComponentScan("com.zmbdp.common.log.mq")
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class RabbitConfig {

    /**
     * 操作日志交换机名称
     */
    public static final String OPERATION_LOG_EXCHANGE = "log.operation.exchange";

    /**
     * 操作日志广播交换机（持久化、不自动删除）
     *
     * @return Fanout 交换机
     */
    @Bean
    public FanoutExchange operationLogExchange() {
        return new FanoutExchange(OPERATION_LOG_EXCHANGE, true, false);
    }
}