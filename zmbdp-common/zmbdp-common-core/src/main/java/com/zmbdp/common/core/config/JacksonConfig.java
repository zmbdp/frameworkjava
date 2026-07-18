package com.zmbdp.common.core.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * <p>
 * 将 Long 类型统一序列化为字符串，避免前端 JavaScript 处理雪花算法生成的
 * Long 主键时发生精度丢失（JS Number 安全整数上限为 2^53-1，雪花 ID 超出）。
 *
 * @author 稚名不带撇
 */
@Configuration
public class JacksonConfig {

    /**
     * 注册 Long → String 的全局序列化定制器
     *
     * @return Jackson2ObjectMapperBuilderCustomizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.modules(module);
        };
    }
}
