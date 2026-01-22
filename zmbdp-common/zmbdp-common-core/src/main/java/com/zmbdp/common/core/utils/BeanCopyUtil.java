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
 * 提供便捷的 Bean 属性拷贝功能，支持单个对象、List 集合、Map 集合等多种场景的批量拷贝。
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
     * 批量拷贝 List 集合中的元素到目标类型的新集合
     * <p>
     * 将源 List 中的每个元素拷贝到目标类型的新实例中，并返回新的 List 集合。
     * 适用于批量转换场景，如将 Entity 列表转换为 DTO 列表。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 UserEntity 列表转换为 UserDTO 列表
     * List<UserEntity> entityList = userService.findAll();
     * List<UserDTO> dtoList = BeanCopyUtil.copyListProperties(entityList, UserDTO::new);
     *
     * // 或者使用 Lambda 表达式
     * List<UserDTO> dtoList2 = BeanCopyUtil.copyListProperties(entityList, () -> new UserDTO());
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空集合（不会抛出异常）</li>
     *   <li>如果 source 中的某个元素为 null，会在目标集合中添加一个未拷贝属性的新实例</li>
     *   <li>target 参数必须是 Supplier 函数式接口，用于创建目标对象实例</li>
     * </ul>
     *
     * @param source 待拷贝的源数据集合，可以为 null
     * @param target 目标对象的创建函数（例如：UserDTO::new），不能为 null
     * @param <S>    源对象类型
     * @param <T>    目标对象类型
     * @return 目标对象集合，如果 source 为 null 则返回空集合
     * @throws RuntimeException 当 target.get() 返回 null 或拷贝过程中发生异常时抛出
     */
    public static <S, T> List<T> copyListProperties(List<S> source, Supplier<T> target) {
        if (source == null) {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>(source.size());
        for (S s : source) {
            // 通过给我们的创建目标方法的引用 来 创建目标对象
            T t = target.get();
            // 然后单个对象拷贝属性，循环完成之后就拷贝完了
            if (s != null) {
                copyProperties(s, t);
            }
            list.add(t);
        }
        return list;
    }

    /**
     * 批量拷贝 Map 集合中的 value 元素到目标类型的新 Map
     * <p>
     * 将源 Map 中的每个 value 拷贝到目标类型的新实例中，保持原有的 key 不变，返回新的 Map 集合。
     * 适用于需要转换 Map 中 value 类型的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 Map<String, UserEntity> 转换为 Map<String, UserDTO>
     * Map<String, UserEntity> entityMap = new HashMap<>();
     * entityMap.put("user1", userEntity1);
     * entityMap.put("user2", userEntity2);
     *
     * Map<String, UserDTO> dtoMap = BeanCopyUtil.copyMapProperties(entityMap, UserDTO::new);
     * // 结果：dtoMap 的 key 保持不变，value 转换为 UserDTO 类型
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空 Map（不会抛出异常）</li>
     *   <li>如果 source 中的某个 value 为 null，会在目标 Map 中添加一个未拷贝属性的新实例</li>
     *   <li>只支持 Map&lt;String, S&gt; 类型，key 必须是 String 类型</li>
     *   <li>目标 Map 的 key 与源 Map 的 key 保持一致</li>
     * </ul>
     *
     * @param source 待拷贝的源数据 Map，key 必须是 String 类型，可以为 null
     * @param target 目标对象的创建函数（例如：UserDTO::new），不能为 null
     * @param <S>    源 value 对象类型
     * @param <T>    目标 value 对象类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当 target.get() 返回 null 或拷贝过程中发生异常时抛出
     */
    public static <S, T> Map<String, T> copyMapProperties(Map<String, S> source, Supplier<T> target) {
        Map<String, T> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        for (Map.Entry<String, S> entry : source.entrySet()) {
            String key = entry.getKey();
            S sourceValue = entry.getValue();
            T targetValue = target.get();
            if (sourceValue != null) {
                copyProperties(sourceValue, targetValue);
            }
            map.put(key, targetValue);
        }
        return map;
    }

    /**
     * 批量拷贝 Map 集合中嵌套的 List 元素（支持复杂泛型嵌套）
     * <p>
     * 将源 Map 中每个 key 对应的 List&lt;S&gt; 转换为 List&lt;T&gt;，保持原有的 key 不变。
     * 适用于 Map 的 value 是 List 集合，且需要转换 List 中元素类型的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 Map<String, List<UserEntity>> 转换为 Map<String, List<UserDTO>>
     * Map<String, List<UserEntity>> entityMap = new HashMap<>();
     * entityMap.put("group1", Arrays.asList(userEntity1, userEntity2));
     * entityMap.put("group2", Arrays.asList(userEntity3));
     *
     * Map<String, List<UserDTO>> dtoMap = BeanCopyUtil.copyMapListProperties(entityMap, UserDTO::new);
     * // 结果：dtoMap 的 key 保持不变，每个 value 中的 List 元素都转换为 UserDTO 类型
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回空 Map（不会抛出异常）</li>
     *   <li>如果 source 中的某个 List 为 null，会在目标 Map 中添加一个空 List</li>
     *   <li>内部调用 copyListProperties 方法进行 List 元素的拷贝</li>
     *   <li>只支持 Map&lt;String, List&lt;S&gt;&gt; 类型，key 必须是 String 类型</li>
     * </ul>
     *
     * @param source 待拷贝的源数据 Map，key 必须是 String 类型，value 是 List 集合，可以为 null
     * @param target 目标对象的创建函数（例如：UserDTO::new），用于创建 List 中的元素，不能为 null
     * @param <S>    源 List 中的元素类型
     * @param <T>    目标 List 中的元素类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当 target.get() 返回 null 或拷贝过程中发生异常时抛出
     */
    public static <S, T> Map<String, List<T>> copyMapListProperties(Map<String, List<S>> source, Supplier<T> target) {
        Map<String, List<T>> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        // 拿出源数据
        for (Map.Entry<String, List<S>> entry : source.entrySet()) {
            // 拿出源数据的 key
            String key = entry.getKey();
            List<S> sourceList = entry.getValue();
            List<T> targetList = copyListProperties(sourceList, target);
            map.put(key, targetList);
        }
        return map;
    }

    /**
     * 将源对象的属性拷贝到目标类的新实例中
     * <p>
     * 通过反射创建目标类的实例，然后将源对象的属性拷贝到新实例中。
     * 适用于目标类具有无参构造函数且无需复杂初始化的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将 UserEntity 转换为 UserDTO
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
     *   <li>只拷贝同名同类型的属性</li>
     *   <li>不支持深拷贝，嵌套对象是引用拷贝</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *   <li>简单的 DTO/Entity 转换</li>
     *   <li>目标类结构简单，只需无参构造即可</li>
     * </ul>
     * <p>
     * <b>不适用场景：</b>
     * <ul>
     *   <li>目标类需要复杂初始化（使用 {@link #copyProperties(Object, Supplier)} 替代）</li>
     *   <li>目标类使用 Builder 模式（使用 {@link #copyProperties(Object, Supplier)} 替代）</li>
     *   <li>目标类是 Spring Bean（使用 {@link #copyProperties(Object, Supplier)} 替代）</li>
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
     * 将源对象的属性拷贝到由 Supplier 提供的目标对象实例中
     * <p>
     * 通过 Supplier 函数式接口创建目标对象实例，然后将源对象的属性拷贝到该实例中。
     * 适用于需要自定义目标对象创建逻辑的场景，如使用工厂、Spring Bean 或 Builder 模式。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 方式1：使用构造方法引用
     * UserEntity entity = userService.findById(1L);
     * UserDTO dto = BeanCopyUtil.copyProperties(entity, UserDTO::new);
     *
     * // 方式2：使用 Lambda 表达式
     * UserDTO dto2 = BeanCopyUtil.copyProperties(entity, () -> new UserDTO());
     *
     * // 方式3：使用 Spring Bean（需要注入）
     * @Autowired
     * private UserDTOFactory userDTOFactory;
     * UserDTO dto3 = BeanCopyUtil.copyProperties(entity, () -> userDTOFactory.create());
     *
     * // 方式4：使用 Builder 模式
     * UserDTO dto4 = BeanCopyUtil.copyProperties(entity, () -> UserDTO.builder().build());
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 source 为 null，返回 null（不会抛出异常）</li>
     *   <li>supplier.get() 不能返回 null，否则会抛出 RuntimeException</li>
     *   <li>只拷贝同名同类型的属性</li>
     *   <li>不支持深拷贝，嵌套对象是引用拷贝</li>
     * </ul>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *   <li>目标类需要复杂初始化</li>
     *   <li>使用 Builder 模式创建对象</li>
     *   <li>从 Spring 容器获取 Bean 实例</li>
     *   <li>使用工厂方法创建对象</li>
     *   <li>需要自定义对象创建逻辑</li>
     * </ul>
     * <p>
     * <b>与 copyProperties(Object, Class) 的区别：</b>
     * <ul>
     *   <li>本方法更灵活，支持任意创建逻辑</li>
     *   <li>本方法不需要目标类有无参构造函数</li>
     *   <li>本方法可以复用已存在的对象实例</li>
     * </ul>
     *
     * @param source   源对象，可以为 null
     * @param supplier 用于创建目标对象的工厂函数（例如：UserDTO::new），不能为 null，且 get() 不能返回 null
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     * @return 目标对象实例，若源对象为 null 则返回 null
     * @throws RuntimeException 当 supplier.get() 返回 null 或拷贝过程中发生异常时抛出
     */
    public static <S, T> T copyProperties(S source, Supplier<T> supplier) {
        if (source == null) {
            return null;
        }
        try {
            T target = supplier.get();
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean拷贝并创建实例失败（通过Supplier<T>创建）", e);
        }
    }
}