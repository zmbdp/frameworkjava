package com.zmbdp.common.core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zmbdp.common.core.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;

/**
 * Json 工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
public class JsonUtil {

    private static ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER =
                JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                        .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                        .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS,
                                false)
                        .configure(MapperFeature.USE_ANNOTATIONS, false)
                        .addModule(new JavaTimeModule())
                        .defaultDateFormat(new
                                SimpleDateFormat(CommonConstants.STANDARD_FORMAT))
                        .serializationInclusion(JsonInclude.Include.NON_NULL)
                        .build();
    }

    /**
     * 对象转 json
     *
     * @param object 需要转成 json 的对象
     * @param <T>    泛型
     * @return 转换好的 json 字符串
     */
    public static <T> String objToString(T object) {
        if (object == null || object instanceof String) {
            return (String) object;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("JsonUtil.objToString Object to String error; 对象转 json 异常 {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对象转Json格式字符串(格式化的Json字符串, 调整缩进美化一下)
     *
     * @param object 需要转 json 的对象
     * @param <T>    对象类型
     * @return 美化后的 Json 格式字符串
     */
    public static <T> String objToStringPretty(T object) {
        if (object == null || object instanceof String) {
            return (String) object;
        }
        try {
            // writerWithDefaultPrettyPrinter(): 调整缩进格式的方法
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error : {}", e.getMessage());
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
    public static <T> T stringToObj(String json, Class<T> clazz) {
        // todo json == null || json.isEmpty() 提取到 StringUtils 工具类中去
        if (json == null || json.isEmpty() || clazz == null) {
            return null;
        }
        if (clazz.equals(String.class)) {
            return (T) json;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Parse String to Object error; json 转对象异常 : {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
