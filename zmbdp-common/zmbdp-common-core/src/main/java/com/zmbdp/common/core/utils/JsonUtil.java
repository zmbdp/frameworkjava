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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * <p>
 * 基于 Jackson 提供的 JSON 序列化和反序列化功能，封装了常用的 JSON 转换方法。<br>
 * 配置了日期格式、空值处理、类型容错等常用设置，开箱即用。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>对象转 JSON 字符串（普通格式和美化格式）</li>
 *     <li>JSON 字符串转对象（支持普通类型和复杂泛型）</li>
 *     <li>JSON 转 List 集合</li>
 *     <li>JSON 转 Map 集合</li>
 *     <li>自动处理日期格式（统一为 yyyy-MM-dd HH:mm:ss）</li>
 *     <li>自动忽略未知属性（反序列化时）</li>
 *     <li>自动忽略 null 值（序列化时）</li>
 *     <li>支持 LocalDateTime 类型转换</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 对象转 JSON
 * UserDTO user = new UserDTO();
 * String json = JsonUtil.classToJson(user);
 *
 * // JSON 转对象
 * UserDTO user2 = JsonUtil.jsonToClass(json, UserDTO.class);
 *
 * // JSON 转 List
 * List<UserDTO> userList = JsonUtil.jsonToList(jsonArray, UserDTO.class);
 *
 * // JSON 转 Map
 * Map<String, UserDTO> userMap = JsonUtil.jsonToMap(jsonMap, UserDTO.class);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>日期格式统一为：yyyy-MM-dd HH:mm:ss</li>
 *     <li>序列化时自动忽略 null 值</li>
 *     <li>反序列化时自动忽略未知属性（不会抛出异常）</li>
 *     <li>转换失败时返回 null，不会抛出异常（记录警告日志）</li>
 *     <li>不支持 Jackson 注解（USE_ANNOTATIONS 设置为 false）</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class JsonUtil {

    /**
     * 创建一个 ObjectMapper 对象，这个对象会根据我们后续的配置，进行 json 转换
     */
    private static final ObjectMapper OBJECT_MAPPER;

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
     * 对象转 JSON 字符串
     * <p>
     * 将 Java 对象转换为 JSON 字符串，使用紧凑格式（无换行和缩进）。<br>
     * 如果对象为 null 或已经是 String 类型，直接返回。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 普通对象转 JSON
     * UserDTO user = new UserDTO();
     * user.setName("张三");
     * String json = JsonUtil.classToJson(user);
     * // 结果：{"name":"张三"}
     *
     * // null 值处理
     * String json2 = JsonUtil.classToJson(null); // 返回 null
     *
     * // String 类型直接返回
     * String json3 = JsonUtil.classToJson("已经是JSON"); // 返回 "已经是JSON"
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 clazz 为 null，返回 null</li>
     *     <li>如果 clazz 是 String 类型，直接返回该字符串</li>
     *     <li>null 值属性不会出现在 JSON 中（配置了 NON_NULL）</li>
     *     <li>日期格式统一为：yyyy-MM-dd HH:mm:ss</li>
     *     <li>转换失败时返回 null，并记录警告日志</li>
     * </ul>
     *
     * @param clazz 需要转成 JSON 的对象，可以为 null
     * @param <T>   对象类型泛型
     * @return 转换好的 JSON 字符串，如果转换失败或对象为 null 则返回 null
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
     * 对象转 JSON 格式字符串（美化格式）
     * <p>
     * 将 Java 对象转换为格式化的 JSON 字符串，包含换行和缩进，便于阅读和调试。<br>
     * 如果对象为 null 或已经是 String 类型，直接返回。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 对象转美化 JSON
     * UserDTO user = new UserDTO();
     * user.setName("张三");
     * user.setAge(20);
     * String json = JsonUtil.classToJsonPretty(user);
     * // 结果：
     * // {
     * //   "name" : "张三",
     * //   "age" : 20
     * // }
     *
     * // 用于日志输出
     * log.debug("用户信息：{}", JsonUtil.classToJsonPretty(user));
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>日志输出（便于阅读）</li>
     *     <li>调试时查看 JSON 结构</li>
     *     <li>配置文件生成</li>
     *     <li>API 文档示例</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 clazz 为 null，返回 null</li>
     *     <li>如果 clazz 是 String 类型，直接返回该字符串</li>
     *     <li>格式化会增加字符串长度，不适合生产环境大量使用</li>
     *     <li>其他注意事项同 {@link #classToJson(Object)}</li>
     * </ul>
     *
     * @param clazz 需要转 JSON 的对象，可以为 null
     * @param <T>   对象类型泛型
     * @return 美化后的 JSON 格式字符串，如果转换失败或对象为 null 则返回 null
     * @see #classToJson(Object)
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
     * JSON 字符串转对象
     * <p>
     * 将 JSON 字符串转换为指定类型的 Java 对象。<br>
     * 支持自动忽略 JSON 中的未知属性，不会因为 JSON 中有对象类中不存在的属性而抛出异常。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // JSON 转对象
     * String json = "{\"name\":\"张三\",\"age\":20}";
     * UserDTO user = JsonUtil.jsonToClass(json, UserDTO.class);
     *
     * // null 值处理
     * UserDTO user2 = JsonUtil.jsonToClass(null, UserDTO.class); // 返回 null
     * UserDTO user3 = JsonUtil.jsonToClass(json, null); // 返回 null
     *
     * // String 类型直接返回
     * String str = JsonUtil.jsonToClass("\"test\"", String.class); // 返回 "test"
     *
     * // JSON 中有未知属性不会报错
     * String json2 = "{\"name\":\"张三\",\"unknown\":\"value\"}";
     * UserDTO user4 = JsonUtil.jsonToClass(json2, UserDTO.class); // 正常转换，忽略 unknown
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 json 为 null 或空字符串，返回 null</li>
     *     <li>如果 clazz 为 null，返回 null</li>
     *     <li>如果 clazz 是 String.class，直接返回 json 字符串</li>
     *     <li>JSON 中的未知属性会被自动忽略（不会抛出异常）</li>
     *     <li>日期字符串会自动转换为 LocalDateTime 或 Date 类型</li>
     *     <li>转换失败时返回 null，并记录警告日志</li>
     *     <li>不支持复杂泛型嵌套，请使用 {@link #jsonToClass(String, TypeReference)}</li>
     * </ul>
     *
     * @param json  需要转换的 JSON 字符串，可以为 null 或空字符串
     * @param clazz 需要转换成的对象类型，不能为 null
     * @param <T>   对象类型泛型
     * @return 转换好的对象，如果转换失败或参数为 null 则返回 null
     * @see #jsonToClass(String, TypeReference)
     */
    public static <T> T jsonToClass(String json, Class<T> clazz) {
        if (StringUtil.isEmpty(json) || clazz == null) {
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
     * JSON 转换为自定义对象（支持复杂泛型嵌套）
     * <p>
     * 使用 TypeReference 支持复杂泛型类型的 JSON 转换，如 List&lt;Map&lt;String, UserDTO&gt;&gt; 等。<br>
     * 适用于需要转换复杂嵌套结构的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 转换复杂泛型：List<Map<String, UserDTO>>
     * String json = "[{\"key1\":{\"name\":\"张三\"}},{\"key2\":{\"name\":\"李四\"}}]";
     * List<Map<String, UserDTO>> result = JsonUtil.jsonToClass(json,
     *     new TypeReference<List<Map<String, UserDTO>>>() {});
     *
     * // 转换 Map<String, List<UserDTO>>
     * String json2 = "{\"group1\":[{\"name\":\"张三\"}],\"group2\":[{\"name\":\"李四\"}]}";
     * Map<String, List<UserDTO>> result2 = JsonUtil.jsonToClass(json2,
     *     new TypeReference<Map<String, List<UserDTO>>>() {});
     *
     * // 转换自定义泛型类
     * String json3 = "{\"data\":[{\"name\":\"张三\"}],\"total\":100}";
     * PageResult<UserDTO> result3 = JsonUtil.jsonToClass(json3,
     *     new TypeReference<PageResult<UserDTO>>() {});
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>需要转换复杂泛型嵌套的场景</li>
     *     <li>List&lt;Map&lt;String, T&gt;&gt; 等复杂结构</li>
     *     <li>自定义泛型类的转换</li>
     *     <li>无法通过 Class 参数指定的类型</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 json 为 null 或空字符串，返回 null</li>
     *     <li>如果 valueTypeRef 为 null，返回 null</li>
     *     <li>需要使用匿名内部类创建 TypeReference 实例</li>
     *     <li>转换失败时返回 null，并记录警告日志</li>
     *     <li>其他注意事项同 {@link #jsonToClass(String, Class)}</li>
     * </ul>
     *
     * @param json         需要转换的 JSON 字符串，可以为 null 或空字符串
     * @param valueTypeRef 自定义对象类型（使用 TypeReference），不能为 null
     * @param <T>          对象类型泛型
     * @return 转换好的对象，如果转换失败或参数为 null 则返回 null
     * @see com.fasterxml.jackson.core.type.TypeReference
     * @see #jsonToClass(String, Class)
     */
    public static <T> T jsonToClass(String json, TypeReference<T> valueTypeRef) {
        if (StringUtil.isEmpty(json) || valueTypeRef == null) {
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
     * JSON 转 List 集合
     * <p>
     * 将 JSON 数组字符串转换为指定元素类型的 List 集合。<br>
     * 适用于将 JSON 数组转换为 Java List 的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // JSON 数组转 List
     * String json = "[{\"name\":\"张三\",\"age\":20},{\"name\":\"李四\",\"age\":25}]";
     * List<UserDTO> userList = JsonUtil.jsonToList(json, UserDTO.class);
     *
     * // 空数组处理
     * String json2 = "[]";
     * List<UserDTO> emptyList = JsonUtil.jsonToList(json2, UserDTO.class); // 返回空列表
     *
     * // null 值处理
     * List<UserDTO> nullList = JsonUtil.jsonToList(null, UserDTO.class); // 返回 null
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 json 为 null 或空字符串，返回 null</li>
     *     <li>如果 clazz 为 null，返回 null</li>
     *     <li>JSON 必须是数组格式，否则转换失败</li>
     *     <li>数组中的每个元素会转换为 clazz 类型的对象</li>
     *     <li>转换失败时返回 null，并记录警告日志</li>
     *     <li>如果只需要简单 List，可以使用此方法；复杂泛型请使用 {@link #jsonToClass(String, TypeReference)}</li>
     * </ul>
     *
     * @param json  需要转换的 JSON 字符串（必须是数组格式），可以为 null 或空字符串
     * @param clazz List 里面的对象类型，不能为 null
     * @param <T>   对象类型泛型
     * @return 转换好的 List，如果转换失败或参数为 null 则返回 null
     * @see #jsonToClass(String, TypeReference)
     */
    public static <T> List<T> jsonToList(String json, Class<T> clazz) {
        if (StringUtil.isEmpty(json) || clazz == null) {
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
     * JSON 转 Map 集合
     * <p>
     * 将 JSON 对象字符串转换为 Map&lt;String, T&gt; 集合。<br>
     * Map 的 key 必须是 String 类型，value 是指定的类型。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // JSON 对象转 Map
     * String json = "{\"user1\":{\"name\":\"张三\",\"age\":20},\"user2\":{\"name\":\"李四\",\"age\":25}}";
     * Map<String, UserDTO> userMap = JsonUtil.jsonToMap(json, UserDTO.class);
     *
     * // 空对象处理
     * String json2 = "{}";
     * Map<String, UserDTO> emptyMap = JsonUtil.jsonToMap(json2, UserDTO.class); // 返回空 Map
     *
     * // null 值处理
     * Map<String, UserDTO> nullMap = JsonUtil.jsonToMap(null, UserDTO.class); // 返回 null
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 json 为 null 或空字符串，返回 null</li>
     *     <li>如果 valueClass 为 null，返回 null</li>
     *     <li>JSON 必须是对象格式（{}），不能是数组格式</li>
     *     <li>Map 的 key 必须是 String 类型</li>
     *     <li>Map 的 value 会转换为 valueClass 类型的对象</li>
     *     <li>转换失败时返回 null，并记录警告日志</li>
     *     <li>如果只需要简单 Map，可以使用此方法；复杂泛型请使用 {@link #jsonToClass(String, TypeReference)}</li>
     * </ul>
     *
     * @param json       需要转换成 Map 的 JSON 数据（必须是对象格式），可以为 null 或空字符串
     * @param valueClass Map 中 value 的类型，不能为 null
     * @param <T>        value 类型泛型
     * @return 转换好的 Map，如果转换失败或参数为 null 则返回 null
     * @see #jsonToClass(String, TypeReference)
     */
    public static <T> Map<String, T> jsonToMap(String json, Class<T> valueClass) {
        if (StringUtil.isEmpty(json) || valueClass == null) {
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