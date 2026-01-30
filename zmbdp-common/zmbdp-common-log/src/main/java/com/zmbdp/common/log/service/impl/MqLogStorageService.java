package com.zmbdp.common.log.service.impl;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.service.ILogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * 消息队列日志存储服务实现
 * <p>
 * 将日志发送到消息队列（RabbitMQ），由消费者异步处理。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>可通过配置 {@code log.storage-type=mq} 或注解 {@code @LogAction(storageType = "mq")} 指定使用</li>
 *     <li>需要引入 {@code zmbdp-common-rabbitmq} 依赖（如果未引入，此Bean不会注册）</li>
 *     <li>可通过 {@code log.mq.queue-name} 配置队列名称，默认：{@code log.operation.queue}</li>
 *     <li>日志以 JSON 格式发送到消息队列，由消费者异步处理</li>
 *     <li>适合高并发场景，避免阻塞业务线程</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 */
@Slf4j
@Service("mqLogStorageService")
@ConditionalOnBean(RabbitTemplate.class)
public class MqLogStorageService implements ILogStorageService {

    /**
     * RabbitMQ 模板
     */
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /**
     * 日志队列名称（可通过配置修改）
     */
    @Value("${log.mq.queue-name:log.operation.queue}")
    private String queueName;

    /**
     * 保存操作日志
     * <p>
     * 将日志以 JSON 格式发送到消息队列。
     *
     * @param logDTO 操作日志数据传输对象
     */
    @Override
    public void save(OperationLogDTO logDTO) {
        try {
            if (rabbitTemplate == null) {
                log.warn("RabbitTemplate 未注入，无法发送日志到消息队列");
                return;
            }

            // 转换为 JSON 字符串
            String logJson = JsonUtil.classToJson(logDTO);

            // 发送到消息队列
            rabbitTemplate.convertAndSend(queueName, logJson);
        } catch (Exception e) {
            // 存储失败不应该影响业务，只记录错误日志
            log.error("发送操作日志到消息队列失败: {}, 队列名称: {}", e.getMessage(), queueName, e);
        }
    }
}
