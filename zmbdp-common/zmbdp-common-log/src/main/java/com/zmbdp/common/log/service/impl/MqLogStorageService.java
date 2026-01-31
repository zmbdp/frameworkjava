package com.zmbdp.common.log.service.impl;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.mq.producer.OperationLogProducer;
import com.zmbdp.common.log.service.ILogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

/**
 * 消息队列日志存储服务实现
 * <p>
 * 将日志通过 {@link OperationLogProducer} 发送到 RabbitMQ 广播交换机，
 * 由 {@link com.zmbdp.common.log.mq.consumer.OperationLogConsumer} 消费并处理（可扩展落库、同步 ES 等）。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>可通过配置 {@code log.storage-type=mq} 或注解 {@code @LogAction(storageType = "mq")} 指定使用</li>
 *     <li>需要引入 {@code zmbdp-common-rabbitmq} 依赖（如果未引入，此 Bean 不会注册）</li>
 *     <li>使用 Fanout 交换机 {@code log.operation.exchange}，消息广播到所有绑定队列</li>
 *     <li>消费者在 {@link com.zmbdp.common.log.mq.consumer.OperationLogConsumer} 中扩展业务逻辑</li>
 *     <li>适合高并发场景，发送与消费解耦，不阻塞业务线程</li>
 * </ul>
 * <p>
 * <b>条件注册说明：</b>
 * <ul>
 *     <li>使用 {@code @ConditionalOnClass(RabbitTemplate.class)} 而不是 {@code @ConditionalOnBean}</li>
 *     <li>只要 classpath 中存在 RabbitTemplate 类就注册，不依赖 Bean 加载顺序</li>
 *     <li>如果项目没有引入 {@code zmbdp-common-rabbitmq} 依赖，此 Bean 不会注册</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 * @see OperationLogProducer
 */
@Slf4j
@Service("mqLogStorageService")
@ConditionalOnClass(RabbitTemplate.class)
public class MqLogStorageService implements ILogStorageService {

    /**
     * 操作日志 MQ 生产者
     */
    @Autowired(required = false)
    private OperationLogProducer operationLogProducer;

    /**
     * 保存操作日志
     * <p>
     * 将日志转为 JSON 并通过生产者发送到 RabbitMQ 交换机。
     *
     * @param logDTO 操作日志数据传输对象
     */
    @Override
    public void save(OperationLogDTO logDTO) {
        try {
            if (operationLogProducer == null) {
                log.warn("OperationLogProducer 未注入，无法发送日志到 MQ");
                return;
            }
            operationLogProducer.sendMessage(JsonUtil.classToJson(logDTO));
        } catch (Exception e) {
            // 存储失败不应该影响业务，只记录错误日志
            log.error("发送操作日志到消息队列失败: {}", e.getMessage(), e);
        }
    }
}