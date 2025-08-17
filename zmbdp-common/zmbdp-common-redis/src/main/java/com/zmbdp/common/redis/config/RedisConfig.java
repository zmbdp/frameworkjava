package com.zmbdp.common.redis.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.zmbdp.common.domain.constants.CommonConstants;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Redis 配置类
 *
 * @author 稚名不带撇
 */
@Configuration
@EnableCaching // 加上这个注解，可以在方法上使用 @Cacheable、@CacheEvict、@CachePut 等注解来实现缓存功能。
@AutoConfigureBefore(RedisAutoConfiguration.class) // 确保当前配置类在 RedisAutoConfiguration 之前被加载
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     *
     * @param redisConnectionFactory redis连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //Redis 中 key的序列化设置
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // 配置正常 key 的序列化方式
        redisTemplate.setHashKeySerializer(new StringRedisSerializer()); // 配置 hashKey 的序列化方式
        //Redis 中 value的序列化设置
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = createJacksonSerializer();
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer); // 配置 value 的序列化方式
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer); // 配置 hash 的 value 的序列化方式
        redisTemplate.afterPropertiesSet(); // 初始化 redisTemplate
        return redisTemplate;
    }

    /**
     * 创建序列化器
     *
     * @return redis 序列化器
     */
    private GenericJackson2JsonRedisSerializer createJacksonSerializer() {
        // 在这里配置 value 的序列化配置, 这里配置的属性和 JsonUtil 那里的一摸一样
        ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .addModule(new JavaTimeModule())
                .addModule(new SimpleModule()
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(CommonConstants.STANDARD_FORMAT)))
                        .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(CommonConstants.STANDARD_FORMAT)))
                )
                .defaultDateFormat(new SimpleDateFormat(CommonConstants.STANDARD_FORMAT))
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        // 把配置好的 json 配置格式给 GenericJackson2JsonRedisSerializer 就可以了
        return new GenericJackson2JsonRedisSerializer(OBJECT_MAPPER);
    }
}
