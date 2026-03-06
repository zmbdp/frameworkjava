package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bean 拷贝工具类
 * <p>
 * 提供便捷的 Bean 属性拷贝功能，支持单个对象、List 集合、Map 集合等多种场景的批量拷贝。<br>
 * 基于 Spring 的 BeanUtils 实现，自动处理同名属性的拷贝。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>DTO 与 Entity 之间的转换</li>
 *   <li>VO 与 DTO 之间的转换</li>
 *   <li>批量数据转换</li>
 *   <li>复杂嵌套结构的拷贝</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>只拷贝同名同类型的属性</li>
 *   <li>忽略 null 值（不会覆盖目标对象的已有值）</li>
 *   <li>不支持深拷贝，嵌套对象是引用拷贝</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class BeanCopyUtil extends BeanUtils {

    /**
     * 批量拷贝 List 集合中的元素到目标类型的新集合（浅拷贝，不支持复杂泛型嵌套）
     * <p>
     * 将源 List 中的每个元素拷贝到目标类型的新实例中，并返回新的 List 集合。<br>
     * 适用于批量转换场景，如将 Entity 列表转换为 DTO 列表。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 UserEntity 列表转换为 UserDTO 列表（简单对象，无嵌套）
     * List<UserEntity> entityList = userService.findAll();
     * List<UserDTO> dtoList = BeanCopyUtil.copyListProperties(entityList, UserDTO.class);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空集合（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>只拷贝同名同类型的属性（浅拷贝）</li>
     *   <li>不支持复杂泛型嵌套转换，如需深拷贝请使用 {@link #copyListProperties(List, Supplier)}</li>
     * </ul>
     *
     * @param source      待拷贝的源数据集合，可以为 null
     * @param targetClass 目标对象的 Class 对象，必须有无参构造函数，不能为 null
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 目标对象集合，如果 source 为 null 则返回空集合
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    public static <S, T> List<T> copyListProperties(List<S> source, Class<T> targetClass) {
        if (source == null) {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>(source.size());
        for (S s : source) {
            try {
                // 创建目标对象实例
                T t = targetClass.getDeclaredConstructor().newInstance();
                // 浅拷贝属性
                if (s != null) {
                    copyProperties(s, t);
                }
                list.add(t);
            } catch (Exception e) {
                throw new RuntimeException("Bean拷贝并创建实例失败（通过Class<T>创建）", e);
            }
        }
        return list;
    }

    /**
     * 批量深度拷贝 List 集合中的元素到目标类型的新集合（深拷贝，支持复杂泛型嵌套）
     * <p>
     * 通过 JSON 序列化和反序列化的方式避免泛型擦除问题，适用于 List 中的对象中还包含复杂泛型嵌套的场景。<br>
     * 与 {@link #copyListProperties(List, Class)} 的区别是，本方法会递归处理嵌套的 List 属性。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 场景1：MenuEntity 中包含 List<MenuEntity> children 属性
     * // 需要转换为 MenuDTO 中的 List<MenuDTO> children 属性
     * List<MenuEntity> menuEntityList = menuService.findAll();
     * List<MenuDTO> menuDTOList = BeanCopyUtil.deepCopyListProperties(menuEntityList, MenuDTO::new);
     *
     * // 场景2：普通对象列表（也可以用，但性能不如浅拷贝）
     * List<UserEntity> userEntityList = userService.findAll();
     * List<UserDTO> userDTOList = BeanCopyUtil.deepCopyListProperties(userEntityList, UserDTO::new);
     * }</pre>
     * <p>
     * <b>工作原理：</b>
     * <ol>
     *   <li>将源 List 转换为 JSON 字符串</li>
     *   <li>利用 Jackson 的 TypeFactory 构造目标 List 类型</li>
     *   <li>将 JSON 反序列化为目标类型的 List</li>
     *   <li>Jackson 会自动处理嵌套的泛型类型</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空集合（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>会递归处理嵌套的 List 属性，避免泛型擦除</li>
     *   <li>适用于树形结构、嵌套列表等复杂数据结构的转换</li>
     *   <li>性能略低于浅拷贝，但更可靠</li>
     * </ul>
     *
     * @param source   待拷贝的源数据集合，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuDTO::new），不能为 null
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     * @return 目标对象集合，如果 source 为 null 则返回空集合
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    public static <S, T> List<T> copyListProperties(List<S> source, Supplier<T> supplier) {
        if (source == null) {
            return new ArrayList<>();
        }
        // 通过 supplier 获取目标类型的 Class
        T tempInstance = supplier.get();
        Class<T> targetClass = (Class<T>) tempInstance.getClass();

        // 通过 JSON 中转，利用 Jackson 处理复杂泛型
        String json = JsonUtil.classToJson(source);
        return JsonUtil.jsonToList(json, targetClass);
    }

    /**
     * 批量拷贝 Map 集合中的 value 元素到目标类型的新 Map（浅拷贝，不支持复杂泛型嵌套）
     * <p>
     * 将源 Map 中的每个 value 拷贝到目标类型的新实例中，保持原有的 key 不变，返回新的 Map 集合。<br>
     * 适用于需要转换 Map 中 value 类型的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 Map<String, UserEntity> 转换为 Map<String, UserDTO>（简单对象，无嵌套 List）
     * Map<String, UserEntity> entityMap = new HashMap<>();
     * entityMap.put("user1", userEntity1);
     * entityMap.put("user2", userEntity2);
     *
     * Map<String, UserDTO> dtoMap = BeanCopyUtil.copyMapProperties(entityMap, UserDTO.class);
     * // 结果：dtoMap 的 key 保持不变，value 转换为 UserDTO 类型
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空 Map（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>只拷贝同名同类型的属性（浅拷贝）</li>
     *   <li>只支持 Map&lt;String, S&gt; 类型，key 必须是 String 类型</li>
     *   <li>目标 Map 的 key 与源 Map 的 key 保持一致</li>
     *   <li>不支持复杂泛型嵌套转换，如需深拷贝请使用 {@link #copyMapProperties(Map, Supplier)}</li>
     * </ul>
     *
     * @param source      待拷贝的源数据 Map，key 必须是 String 类型，可以为 null
     * @param targetClass 目标对象的 Class 对象，必须有无参构造函数，不能为 null
     * @param <S>         源 value 对象类型
     * @param <T>         目标 value 对象类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    public static <S, T> Map<String, T> copyMapProperties(Map<String, S> source, Class<T> targetClass) {
        Map<String, T> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        for (Map.Entry<String, S> entry : source.entrySet()) {
            String key = entry.getKey();
            S sourceValue = entry.getValue();
            try {
                T targetValue = targetClass.getDeclaredConstructor().newInstance();
                if (sourceValue != null) {
                    copyProperties(sourceValue, targetValue);
                }
                map.put(key, targetValue);
            } catch (Exception e) {
                throw new RuntimeException("Bean拷贝并创建实例失败（通过Class<T>创建）", e);
            }
        }
        return map;
    }

    /**
     * 批量深度拷贝 Map 集合中的 value 元素到目标类型的新 Map（深拷贝，支持复杂泛型嵌套）
     * <p>
     * 通过 JSON 序列化和反序列化的方式避免泛型擦除问题，适用于 Map 的 value 包含 List 属性的复杂嵌套场景。<br>
     * 与 {@link #copyMapProperties(Map, Class)} 的区别是，本方法会递归处理嵌套的 List 属性。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 场景1：Map 的 value 是包含复杂泛型嵌套对象的
     * // MenuEntity 中包含 List<MenuEntity> children 属性
     * Map<String, MenuEntity> entityMap = new HashMap<>();
     * entityMap.put("menu1", menuEntity1);
     * entityMap.put("menu2", menuEntity2);
     *
     * Map<String, MenuDTO> dtoMap = BeanCopyUtil.deepCopyMapProperties(entityMap, MenuDTO::new);
     * // 结果：每个 MenuDTO 的 children 都会被正确转换为 List<MenuDTO>
     *
     * // 场景2：普通对象（也可以用，但性能不如浅拷贝）
     * Map<String, UserEntity> userMap = new HashMap<>();
     * Map<String, UserDTO> userDTOMap = BeanCopyUtil.deepCopyMapProperties(userMap, UserDTO::new);
     * }</pre>
     * <p>
     * <b>工作原理：</b>
     * <ol>
     *   <li>将源 Map 转换为 JSON 字符串</li>
     *   <li>利用 Jackson 的 TypeFactory 构造目标 Map 类型</li>
     *   <li>将 JSON 反序列化为目标类型的 Map</li>
     *   <li>Jackson 会自动处理嵌套的泛型类型</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空 Map（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>会递归处理嵌套的 List 属性，避免泛型擦除</li>
     *   <li>只支持 Map&lt;String, S&gt; 类型，key 必须是 String 类型</li>
     *   <li>性能略低于浅拷贝，但更可靠</li>
     * </ul>
     *
     * @param source   待拷贝的源数据 Map，key 必须是 String 类型，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuDTO::new），不能为 null
     * @param <S>      源 value 对象类型
     * @param <T>      目标 value 对象类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    public static <S, T> Map<String, T> copyMapProperties(Map<String, S> source, Supplier<T> supplier) {
        if (source == null) {
            return new HashMap<>();
        }
        // 通过 supplier 获取目标类型的 Class
        T tempInstance = supplier.get();
        Class<T> targetClass = (Class<T>) tempInstance.getClass();

        // 通过 JSON 中转，利用 Jackson 处理复杂泛型
        String json = JsonUtil.classToJson(source);
        return JsonUtil.jsonToMap(json, targetClass);
    }

    /**
     * 批量拷贝 Map 集合中嵌套的 List 元素（浅拷贝，不支持复杂泛型嵌套）
     * <p>
     * 将源 Map 中每个 key 对应的 List&lt;S&gt; 转换为 List&lt;T&gt;，保持原有的 key 不变。<br>
     * 适用于 Map 的 value 是 List 集合，且需要转换 List 中元素类型的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 Map<String, List<UserEntity>> 转换为 Map<String, List<UserDTO>>（简单对象，无嵌套）
     * Map<String, List<UserEntity>> entityMap = new HashMap<>();
     * entityMap.put("group1", Arrays.asList(userEntity1, userEntity2));
     * entityMap.put("group2", Arrays.asList(userEntity3));
     *
     * Map<String, List<UserDTO>> dtoMap = BeanCopyUtil.copyMapListProperties(entityMap, UserDTO.class);
     * // 结果：dtoMap 的 key 保持不变，每个 value 中的 List 元素都转换为 UserDTO 类型
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空 Map（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>只拷贝同名同类型的属性（浅拷贝）</li>
     *   <li>如果 source 中的某个 List 为 null，会在目标 Map 中添加一个空 List</li>
     *   <li>内部调用 {@link #copyListProperties(List, Class)} 方法进行 List 元素的拷贝</li>
     *   <li>只支持 Map&lt;String, List&lt;S&gt;&gt; 类型，key 必须是 String 类型</li>
     *   <li>不支持复杂泛型嵌套转换，如需深拷贝请使用 {@link #copyMapListProperties(Map, Supplier)}</li>
     * </ul>
     *
     * @param source      待拷贝的源数据 Map，key 必须是 String 类型，value 是 List 集合，可以为 null
     * @param targetClass 目标对象的 Class 对象，必须有无参构造函数，不能为 null
     * @param <S>         源 List 中的元素类型
     * @param <T>         目标 List 中的元素类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    public static <S, T> Map<String, List<T>> copyMapListProperties(Map<String, List<S>> source, Class<T> targetClass) {
        Map<String, List<T>> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        // 拿出源数据
        for (Map.Entry<String, List<S>> entry : source.entrySet()) {
            // 拿出源数据的 key
            String key = entry.getKey();
            List<S> sourceList = entry.getValue();
            List<T> targetList = copyListProperties(sourceList, targetClass);
            map.put(key, targetList);
        }
        return map;
    }

    /**
     * 批量深度拷贝 Map 集合中嵌套的 List 元素（深拷贝，支持复杂泛型嵌套）
     * <p>
     * 将源 Map 中每个 key 对应的 List&lt;S&gt; 转换为 List&lt;T&gt;，保持原有的 key 不变。<br>
     * 通过 JSON 序列化和反序列化的方式避免泛型擦除问题，适用于 List 中的对象还包含 List 属性的复杂嵌套场景。<br>
     * 与 {@link #copyMapListProperties(Map, Class)} 的区别是，本方法会递归处理嵌套的 List 属性。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 场景1：List 中的对象包含嵌套 List
     * // MenuEntity 中包含 List<MenuEntity> children 属性
     * Map<String, List<MenuEntity>> entityMap = new HashMap<>();
     * entityMap.put("system", Arrays.asList(menuEntity1, menuEntity2));
     * entityMap.put("business", Arrays.asList(menuEntity3));
     *
     * Map<String, List<MenuDTO>> dtoMap = BeanCopyUtil.copyMapListProperties(entityMap, MenuDTO::new);
     * // 结果：每个 MenuDTO 的 children 都会被正确转换为 List<MenuDTO>
     *
     * // 场景2：普通对象列表（也可以用，但性能不如浅拷贝）
     * Map<String, List<UserEntity>> userMap = new HashMap<>();
     * Map<String, List<UserDTO>> userDTOMap = BeanCopyUtil.copyMapListProperties(userMap, UserDTO::new);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空 Map（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>会递归处理嵌套的 List 属性，避免泛型擦除</li>
     *   <li>如果 source 中的某个 List 为 null，会在目标 Map 中添加一个空 List</li>
     *   <li>内部调用 {@link #copyListProperties(List, Supplier)} 方法进行深度拷贝</li>
     *   <li>只支持 Map&lt;String, List&lt;S&gt;&gt; 类型，key 必须是 String 类型</li>
     *   <li>性能略低于浅拷贝，但更可靠</li>
     * </ul>
     *
     * @param source   待拷贝的源数据 Map，key 必须是 String 类型，value 是 List 集合，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuDTO::new），用于创建 List 中的元素，不能为 null
     * @param <S>      源 List 中的元素类型
     * @param <T>      目标 List 中的元素类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    public static <S, T> Map<String, List<T>> copyMapListProperties(Map<String, List<S>> source, Supplier<T> supplier) {
        Map<String, List<T>> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        // 拿出源数据
        for (Map.Entry<String, List<S>> entry : source.entrySet()) {
            // 拿出源数据的 key
            String key = entry.getKey();
            List<S> sourceList = entry.getValue();
            List<T> targetList = copyListProperties(sourceList, supplier);
            map.put(key, targetList);
        }
        return map;
    }

    /**
     * 将源对象的属性拷贝到目标类的新实例中（浅拷贝，不支持复杂泛型嵌套）
     * <p>
     * 通过反射创建目标类的实例，然后将源对象的属性拷贝到新实例中。
     * 适用于目标类具有无参构造函数且无需复杂初始化的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 UserEntity 转换为 UserDTO（简单对象，无嵌套 List）
     * UserEntity entity = userService.findById(1L);
     * UserDTO dto = BeanCopyUtil.copyProperties(entity, UserDTO.class);
     *
     * // 如果 entity 为 null，则返回 null（不会抛出异常）
     * UserDTO dto2 = BeanCopyUtil.copyProperties(null, UserDTO.class); // 返回 null
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回 null（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数，否则会抛出 RuntimeException</li>
     *   <li>只拷贝同名同类型的属性（浅拷贝）</li>
     *   <li>不支持深拷贝，嵌套对象是引用拷贝</li>
     *   <li>不支持复杂泛型嵌套转换，如需深拷贝请使用 {@link #copyProperties(Object, Supplier)}</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *   <li>简单的 DTO/Entity 转换</li>
     *   <li>目标类结构简单，只需无参构造即可</li>
     * </ul>
     *
     * @param source      源对象，可以为 null
     * @param targetClass 目标类的 Class 对象，必须有无参构造函数，不能为 null
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 目标对象实例，若源对象为 null 则返回 null
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    public static <S, T> T copyProperties(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            // 创建目标对象实例
            T target = targetClass.getDeclaredConstructor().newInstance();
            // 拷贝属性
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean拷贝并创建实例失败（通过Class<T>创建）", e);
        }
    }

    /**
     * 深度拷贝对象属性（深拷贝，支持复杂泛型嵌套）
     * <p>
     * 通过 JSON 序列化和反序列化的方式避免泛型擦除问题，适用于树形结构、嵌套列表等复杂数据结构的转换。<br>
     * 与普通的 {@link #copyProperties(Object, Class)} 不同，本方法会递归处理类型的属性。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 场景：MenuEntity 中包含 List<MenuEntity> children 属性
     * MenuEntity menuEntity = menuService.findById(1L);
     * MenuDTO menuDTO = BeanCopyUtil.deepCopyProperties(menuEntity, MenuDTO::new);
     * // menuDTO.children 会被正确转换为 List<MenuDTO> 类型
     * }</pre>
     * <p>
     * <b>工作原理：</b>
     * <ol>
     *   <li>将源对象转换为 JSON 字符串</li>
     *   <li>将 JSON 反序列化为目标类型的对象</li>
     *   <li>Jackson 会自动处理嵌套的 List 泛型类型</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回 null（不会抛出异常）</li>
     *   <li>目标类必须有无参构造函数</li>
     *   <li>会递归处理嵌套的 List 属性，避免泛型擦除</li>
     *   <li>性能略低于浅拷贝，但更可靠</li>
     *   <li>适用于树形菜单、部门层级、评论嵌套等场景</li>
     * </ul>
     *
     * @param source   源对象，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuDTO::new），不能为 null
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     * @return 目标对象实例，若源对象为 null 则返回 null
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    public static <S, T> T copyProperties(S source, Supplier<T> supplier) {
        if (source == null) {
            return null;
        }
        // 通过 supplier 获取目标类型的 Class
        T tempInstance = supplier.get();
        Class<T> targetClass = (Class<T>) tempInstance.getClass();

        // 通过 JSON 中转，利用 Jackson 处理复杂泛型
        String json = JsonUtil.classToJson(source);
        return JsonUtil.jsonToClass(json, targetClass);
    }
}