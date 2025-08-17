package com.zmbdp.common.core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.zmbdp.common.domain.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Json 工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
public class JsonUtil {

    /**
     * 创建一个 ObjectMapper 对象，这个对象会根据我们后续的配置，进行 json 转换
     */
    private static ObjectMapper OBJECT_MAPPER;

    /**
     * 静态代码块，初始化 ObjectMapper 对象
     */
    static {
        OBJECT_MAPPER =
                JsonMapper.builder()
                        // 在反序列化时，如果 json 里面有个属性 class 里没有，默认会抛异常，false 就是不让他抛异常，给忽略掉
                        // 比如 json {name: zhangsan, age: 20} 转换成 bloom {name, id} 对象，默认是会抛出异常的，这就是给他忽略掉
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        // 在序列化时，默认会给日期属性变成时间戳，false 就是这么做，按照后续配置去转换
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        // 在序列化时，Java对象中没有任何属性（不是没有属性值），默认情况下 Jackson 可能会抛出异常。设置
                        // 此项为 false 后，允许这种情况，直接就返回一个 {}
                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                        // 在反序列化时，如果 JSON 数据中指定的类型信息与期望的 Java 类型层次结构不匹配（例如类型
                        // 标识错误等情况），默认会抛出异常。将这个配置设为 false，可以放宽这种限制，使得在遇到类
                        // 型不太准确但仍有可能处理的情况下，尝试继续进行反序列化而不是直接失败，提高对可能存在错
                        // 误类型标识的 JSON 数据的容错性。
                        .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                        // 在序列化时，会把日期键（比如 Map 类型的）转换成时间戳，设置成 false 就按照我们后续配置进行转换
                        .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)
                        // Jackson 支持通过在 Java 类的属性或方法上添加各种注解来定制序列化和反序列化行为。设置为 false 就不让他生效
                        .configure(MapperFeature.USE_ANNOTATIONS, false)
                        // 这是序列化 LocalDateTIme 和 LocalDate 属性的必要配置， 默认是不支持转换这种类型的
                        .addModule(new JavaTimeModule())
                        // 对 Date 类型的日期格式都统一为以下的样式: yyyy-MM-dd HH:mm:ss
                        .defaultDateFormat(new SimpleDateFormat(CommonConstants.STANDARD_FORMAT))
                        // 对 LocalDateTIme 和 LocalDate 类型起作用的
                        .addModule(new SimpleModule()
                                // 序列时起作用
                                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(CommonConstants.STANDARD_FORMAT)))
                                // 反序列时起作用
                                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(CommonConstants.STANDARD_FORMAT)))
                        )
                        // 只针对 非空 的值进行序列化
                        .serializationInclusion(JsonInclude.Include.NON_NULL)
                        .build();
    }

    /**
     * 对象转 json
     *
     * @param clazz 需要转成 json 的对象
     * @param <T>    泛型
     * @return 转换好的 json 字符串
     */
    public static <T> String classToJson(T clazz) {
        if (clazz == null || clazz instanceof String) {
            return (String) clazz;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(clazz);
        } catch (JsonProcessingException e) {
            log.warn("JsonUtil.classToJson Class to JSON error: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对象转 Json 格式字符串(格式化的 Json 字符串, 调整缩进美化一下)
     *
     * @param clazz 需要转 json 的对象
     * @param <T>    对象类型
     * @return 美化后的 Json 格式字符串
     */
    public static <T> String classToJsonPretty(T clazz) {
        if (clazz == null || clazz instanceof String) {
            return (String) clazz;
        }
        try {
            // writerWithDefaultPrettyPrinter(): 调整缩进格式的方法
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(clazz);
        } catch (JsonProcessingException e) {
            log.warn("JsonUtil.classToJsonPretty Class to JSON error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * json 转对象
     *
     * @param json  需要转的 json 字符串
     * @param clazz 需要转成的对象类型
     * @param <T>   泛型
     * @return 转换好的对象
     */
    public static <T> T jsonToClass(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json) || clazz == null) {
            return null;
        }
        if (clazz.equals(String.class)) {
            return (T) json;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("JsonUtil.jsonToClass JSON to Class error: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * json 转换为自定义对象, 支持复杂的泛型嵌套
     *
     * @param json         需要转换的 json 字符串
     * @param valueTypeRef 自定义对象类型
     * @param <T>          泛型
     * @return 转换好的对象
     */
    public static <T> T jsonToClass(String json, TypeReference<T> valueTypeRef) {
        if (StringUtils.isEmpty(json) || valueTypeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, valueTypeRef);
        } catch (Exception e) {
            log.warn("JsonUtil.jsonToClass JSON to custom Class error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * json 转 List
     *
     * @param json  需要转换的 json 字符串
     * @param clazz List 里面的对象类型
     * @param <T>   泛型
     * @return 转换好的 List
     */
    public static <T> List<T> jsonToList(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json) || clazz == null) {
            return null;
        }
        // 拿到 List 的里面是啥类型
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.warn("JsonUtil.jsonToList JSON to List error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * json 转 Map
     *
     * @param json       需要转换成 map 的 json 数据
     * @param valueClass map 中 value 的类型
     * @param <T>        泛型
     * @return 转换好的 Map
     */
    public static <T> Map<String, T> jsonToMap(String json, Class<T> valueClass) {
        if (StringUtils.isEmpty(json) || valueClass == null) {
            return null;
        }
        // 拿到 Map 里面的 value 是啥类型
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, valueClass);
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.warn("JsonUtil.jsonToMap JSON to Map error: {}", e.getMessage());
            return null;
        }
    }
}
