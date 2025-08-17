package com.zmbdp.admin.service.user.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMq 配置信息
 *
 * @author 稚名不带撇
 */
@Configuration
public class RabbitConfig {

    /**
     * 交换机的名称
     */
    public final static String EXCHANGE_NAME = "edit_user_exchange";

    /**
     * 广播交换机
     *
     * @return 广播交换机
     */
    @Bean
    public FanoutExchange editUserExchange() {
        return new FanoutExchange(EXCHANGE_NAME, true, false);
    }
}
