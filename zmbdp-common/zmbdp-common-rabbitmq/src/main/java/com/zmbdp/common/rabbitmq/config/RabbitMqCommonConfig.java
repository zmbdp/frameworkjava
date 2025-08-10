package com.zmbdp.common.rabbitmq.config;

import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rabbitmq配置
 *
 * @author 稚名不带撇
 */
@Configuration
public class RabbitMqCommonConfig {

    /**
     * 创建用于 JSON 与 Map 相互转换的消息转换器
     * <p>
     * 该转换器主要用于 RabbitMQ 等消息中间件中，将 JSON 格式的消息体自动转换为 Map 结构，
     * 或将 Map 结构序列化为 JSON 消息体。特别适用于处理动态结构的消息内容。
     *
     * @return 配置完成的 MessageConverter 实例
     * @apiNote <p>
     * 1. 信任所有包的反序列化（setTrustedPackages("*")）存在安全风险，仅应在受控环境使用<p>
     * 2. 生产环境建议明确指定可信包列表而非通配符 <p>
     * 3. 默认使用 Jackson 库处理 JSON 序列化/反序列化
     */
    @Bean
    public MessageConverter jsonToMapMessageConverter() {
        // 创建默认的类映射器，用于处理消息转换过程中的类型信息
        DefaultClassMapper defaultClassMapper = new DefaultClassMapper();

        // 设置可信的反序列化包路径（此处使用"*"表示允许所有包，方便开发测试）
        // ⚠️ 生产环境应改为明确指定的可信包列表，例如：
        // defaultClassMapper.setTrustedPackages("com.example.dto", "org.springframework.amqp");
        defaultClassMapper.setTrustedPackages("*");

        // 创建 Jackson2JsonMessageConverter，用于 JSON 与 Java 对象的相互转换
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

        // 配置转换器使用我们定义的类映射器
        jackson2JsonMessageConverter.setClassMapper(defaultClassMapper);

        return jackson2JsonMessageConverter;
    }
}
