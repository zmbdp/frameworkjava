package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Bean 拷贝工具类
 * <p>
 * 基于 Spring {@link BeanUtils} 封装，提供单对象、List、Map、Map&lt;List&gt; 四种结构的拷贝能力，
 * 并在此基础上扩展了 <b>浅拷贝 / 深拷贝 / 自定义字段映射</b> 三个维度，覆盖日常开发中绝大多数 DTO / VO / Entity 转换场景。
 * <p>
 * <b>方法速查表</b>
 * <table border="1">
 *   <tr><th>结构</th><th>浅拷贝（.class）</th><th>深拷贝（::new）</th></tr>
 *   <tr><td>单对象</td><td>{@code copyProperties(s, T.class)}</td><td>{@code copyProperties(s, T::new)}</td></tr>
 *   <tr><td>List</td><td>{@code copyListProperties(list, T.class)}</td><td>{@code copyListProperties(list, T::new)}</td></tr>
 *   <tr><td>Map</td><td>{@code copyMapProperties(map, T.class)}</td><td>{@code copyMapProperties(map, T::new)}</td></tr>
 *   <tr><td>Map&lt;List&gt;</td><td>{@code copyMapListProperties(map, T.class)}</td><td>{@code copyMapListProperties(map, T::new)}</td></tr>
 * </table>
 * <p>
 * 以上所有方法均有对应的 <b>带字段映射</b> 重载版本，在末尾追加可变参数 {@code FieldMapping<S, T>... mappings} 即可。
 * <p>
 * <b>浅拷贝 vs 深拷贝</b>
 * <ul>
 *   <li><b>浅拷贝（.class）</b>：基于 {@link BeanUtils#copyProperties} 反射实现，只拷贝同名同类型的属性，
 *       嵌套对象（如 {@code List<Child> children}）是引用拷贝，不会递归转换泛型类型。
 *       适用于无嵌套或嵌套类型相同的简单场景，性能较好。</li>
 *   <li><b>深拷贝（::new）</b>：通过 JSON 序列化/反序列化实现，借助 Jackson 的类型推断递归处理嵌套泛型。
 *       适用于树形结构、对象嵌套 List 等复杂场景，性能略低于浅拷贝。</li>
 * </ul>
 * <p>
 * <b>字段映射（FieldMapping）</b>
 * <p>
 * 当源对象与目标对象字段名不一致时，可通过 {@link #mapping(Function, BiConsumer)} 创建映射对，
 * 传入任意方法的可变参数，数量不限：
 * <pre>{@code
 * // 浅拷贝 + 字段映射：nickName -> displayName，score -> point
 * UserVO vo = BeanCopyUtil.copyProperties(userDTO, UserVO.class,
 *     BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setDisplayName),
 *     BeanCopyUtil.mapping(UserDTO::getScore, UserVO::setPoint)
 * );
 *
 * // 深拷贝 + 字段映射（同名字段自动拷贝，异名字段通过 mapping 处理）
 * MenuVO vo = BeanCopyUtil.copyProperties(menuEntity, (Supplier<MenuVO>) MenuVO::new,
 *     BeanCopyUtil.mapping(MenuEntity::getMenuName, MenuVO::setName)
 * );
 * }</pre>
 * <p>
 * 当字段类型也不一致时（如 {@code List<SourceChild>} → {@code List<TargetChild>}），
 * 使用三参数版本 {@link #mapping(Function, BiConsumer, Function)} 提供类型转换器：
 * <pre>{@code
 * BeanCopyUtil.mapping(
 *     Source::getChildren, // getter：读取源字段
 *     Target::setChildren, // setter：写入目标字段
 *     // converter：类型转换（可递归应用映射）
 *     list -> BeanCopyUtil.copyListProperties(list, (Supplier<TargetChild>) TargetChild::new,
 *         BeanCopyUtil.mapping(SourceChild::getNickName, TargetChild::setDisplayName)
 *     )
 * )
 * }</pre>
 * <p>
 * <b>注意事项</b>
 * <ul>
 *   <li>深拷贝依赖 Jackson，目标类及其嵌套类必须有<b>无参构造函数</b></li>
 *   <li>source 为 null 时，所有方法均<b>安全返回 null 或空集合</b>，不抛异常</li>
 *   <li>Map 的 value / List 的元素为 null 时，浅拷贝会创建空目标对象，深拷贝会跳过该元素</li>
 *   <li>深拷贝 + 字段映射时，{@code ::new} 的 JSON 中转只处理同名字段，
 *       异名字段的映射在 JSON 之后<b>覆盖写入</b>，因此嵌套层级的异名字段需在 converter 中递归处理</li>
 *   <li>{@code copyProperties(source, supplier, mappings)} 与
 *       {@code copyProperties(source, target, mappings)} 存在方法重载歧义时，
 *       请显式转型：{@code (Supplier<T>) T::new}</li>
 *   <li>Map 的 key 仅支持 {@code String} 类型</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class BeanCopyUtil extends BeanUtils {

    /**
     * 创建一个字段映射对（工厂方法）
     * <p>
     * 将源对象的某个字段值映射到目标对象的某个字段，用于处理字段名不一致的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // UserDTO.nickName -> UserVO.name
     * BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName)
     *
     * // UserDTO.age -> UserVO.userAge
     * BeanCopyUtil.mapping(UserDTO::getAge, UserVO::setUserAge)
     * }</pre>
     *
     * @param getter 从源对象读取值的方法引用（例如：UserDTO::getNickName）
     * @param setter 向目标对象写入值的方法引用（例如：UserVO::setName）
     * @param <S>    源对象类型
     * @param <T>    目标对象类型
     * @param <V>    字段值类型
     * @return 封装好的 FieldMapping 实例
     */
    public static <S, T, V> FieldMapping<S, T> mapping(Function<S, V> getter, BiConsumer<T, V> setter) {
        return (source, target) -> setter.accept(target, getter.apply(source));
    }

    /**
     * 创建一个带类型转换的字段映射对（工厂方法）
     * <p>
     * 在字段名不一致的同时，字段类型也不一致时使用。<br>
     * 先通过 {@code getter} 读取源字段值，再通过 {@code setter} 拿到设置目标值的方法，最后通过 {@code converter} 转换类型写入目标对象。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * @Data
     * public static class MappingSourceNode {
     *     private Integer age;
     *     private String name;
     * }
     * @Data
     * public static class MappingSourceChildren {
     *     private Integer age;
     *     private String name;
     *     private List<MappingSourceChildren> children;
     * }
     * @Data
     * public static class MappingSource {
     *      private Long id;
     *      private String nickName;
     *      private Integer score;
     *      private Long parentId;
     *      private MappingSourceNode sourceNode;
     *      private List<MappingSourceChildren> children;
     *      private List<MappingSource> children1;
     * }
     * @Data
     * public static class MappingTargetNode {
     *      private Integer age;
     *      private String name;
     * }
     * @Data
     * public static class MappingTargetChildren {
     *      private Integer age;
     *      private String name;
     *      private List<MappingTargetChildren> children;
     * }
     * @Data
     * public static class MappingTarget {
     *      private Long id;
     *      private String displayName;
     *      private Integer point;
     *      private Long parentId;
     *      private MappingTargetNode targetNode;
     *      private List<MappingTargetChildren> children;
     *      private List<MappingTarget> children2;
     * }
     * // 普通对象，MappingSource.sourceNode（MappingSourceNode） -> MappingTarget.targetNode（MappingTargetNode）
     * BeanCopyUtil.mapping(
     *     MappingSource::getSourceNode,
     *     MappingTarget::setTargetNode,
     *     sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
     * )
     * // 复杂泛型，MappingSource.children1（List<MappingSource>）-> MappingTarget.children2（List<MappingTarget>）
     * BeanCopyUtil.mapping(
     *     MappingSource::getChildren1,
     *     MappingTarget::setChildren2,
     *     list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
     *         BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
     *         BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
     *         BeanCopyUtil.mapping(
     *             MappingSource::getSourceNode,
     *             MappingTarget::setTargetNode,
     *             sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
     *         )
     *     )
     * )
     * }</pre>
     *
     * @param getter    从源对象读取值的方法引用
     * @param setter    向目标对象写入值的方法引用
     * @param converter 类型转换函数
     * @param <S>       源对象类型
     * @param <T>       目标对象类型
     * @param <V>       源字段值类型
     * @param <R>       目标字段值类型（转换后）
     * @return 封装好的 FieldMapping 实例
     */
    public static <S, T, V, R> FieldMapping<S, T> mapping(Function<S, V> getter, BiConsumer<T, R> setter, Function<V, R> converter) {
        return (source, target) -> setter.accept(target, converter.apply(getter.apply(source)));
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
     * MenuDTO menuDTO = BeanCopyUtil.copyProperties(menuEntity, MenuDTO::new);
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

    /**
     * 将源对象属性拷贝到已有目标对象，并应用自定义字段映射（无返回值）
     * <p>
     * 先通过 {@code BeanUtils.copyProperties} 拷贝同名字段，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于目标对象已经创建好，只需要补充映射的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * UserVO userVO = new UserVO();
     * BeanCopyUtil.copyProperties(userDTO, userVO,
     *     BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName),
     *     BeanCopyUtil.mapping(UserDTO::getPhone, UserVO::setMobile)
     * );
     * }</pre>
     *
     * @param source   源对象，可以为 null（为 null 时直接返回，不做任何操作）
     * @param target   目标对象，不能为 null
     * @param mappings 自定义字段映射，可变参数，数量不限
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     */
    @SafeVarargs
    public static <S, T> void copyProperties(S source, T target, FieldMapping<S, T>... mappings) {
        if (source == null) {
            return;
        }
        // 先拷贝同名字段
        copyProperties(source, target);
        // 再处理自定义映射
        if (mappings != null) {
            for (FieldMapping<S, T> m : mappings) {
                m.apply(source, target);
            }
        }
    }

    /**
     * 浅拷贝：将源对象属性拷贝到目标类新实例，并应用自定义字段映射（有返回值，浅拷贝）
     * <p>
     * 先通过 {@code BeanUtils.copyProperties} 拷贝同名字段，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 使用 {@code targetClass} 通过反射创建目标实例，适用于目标类结构简单、无复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // UserDTO.nickName 映射到 UserVO.name，UserDTO.phone 映射到 UserVO.mobile
     * UserVO userVO = BeanCopyUtil.copyProperties(userDTO, UserVO.class,
     *     BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName),
     *     BeanCopyUtil.mapping(UserDTO::getPhone, UserVO::setMobile)
     * );
     * }</pre>
     *
     * @param source      源对象，可以为 null（为 null 时返回 null）
     * @param targetClass 目标类的 Class 对象，必须有无参构造函数，不能为 null
     * @param mappings    自定义字段映射，可变参数，数量不限
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 目标对象实例，若源对象为 null 则返回 null
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    @SafeVarargs
    public static <S, T> T copyProperties(S source, Class<T> targetClass, FieldMapping<S, T>... mappings) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            // 先拷贝同名字段
            copyProperties(source, target);
            // 再处理自定义映射
            if (mappings != null) {
                for (FieldMapping<S, T> m : mappings) {
                    m.apply(source, target);
                }
            }
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean拷贝并创建实例失败（通过Class<T>创建）", e);
        }
    }

    /**
     * 深拷贝：将源对象属性深度拷贝到目标类新实例，并应用自定义字段映射（有返回值，深拷贝）
     * <p>
     * 先通过 JSON 序列化/反序列化进行深拷贝（处理嵌套 List 等复杂泛型），再按照传入的映射关系处理字段名不一致的字段。<br>
     * 使用 {@code supplier}（即 {@code TargetClass::new}）创建目标实例，适用于有复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // MenuEntity 中包含 List<MenuEntity> children，需要转为 MenuVO 的 List<MenuVO> children
     * // 同时 MenuEntity.menuName 映射到 MenuVO.name
     * MenuVO menuVO = BeanCopyUtil.copyProperties(menuEntity, (Supplier<MenuVO>)MenuVO::new,
     *     BeanCopyUtil.mapping(MenuEntity::getMenuName, MenuVO::setName),
     *     BeanCopyUtil.mapping(MenuEntity::getSort, MenuVO::setOrderNum)
     * );
     * }</pre>
     *
     * @param source   源对象，可以为 null（为 null 时返回 null）
     * @param supplier 目标对象的创建函数（例如：MenuVO::new），不能为 null
     * @param mappings 自定义字段映射，可变参数，数量不限
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     * @return 目标对象实例，若源对象为 null 则返回 null
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    @SafeVarargs
    public static <S, T> T copyProperties(S source, Supplier<T> supplier, FieldMapping<S, T>... mappings) {
        if (source == null) {
            return null;
        }
        // 通过 supplier 获取目标类型的 Class
        T tempInstance = supplier.get();
        Class<T> targetClass = (Class<T>) tempInstance.getClass();

        // 先通过 JSON 深拷贝
        String json = JsonUtil.classToJson(source);
        T target = JsonUtil.jsonToClass(json, targetClass);

        // 再处理自定义映射（覆盖 JSON 拷贝的同名结果）
        if (mappings != null && target != null) {
            for (FieldMapping<S, T> m : mappings) {
                m.apply(source, target);
            }
        }
        return target;
    }

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
     * List<MenuDTO> menuDTOList = BeanCopyUtil.copyListProperties(menuEntityList, MenuDTO::new);
     *
     * // 场景2：普通对象列表（也可以用，但性能不如浅拷贝）
     * List<UserEntity> userEntityList = userService.findAll();
     * List<UserDTO> userDTOList = BeanCopyUtil.copyListProperties(userEntityList, UserDTO::new);
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
     * 浅拷贝：批量拷贝 List 集合，并对每个元素应用自定义字段映射（有返回值，浅拷贝）
     * <p>
     * 先通过 {@code BeanUtils.copyProperties} 拷贝同名字段，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于 List 元素结构简单、无复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * List<UserVO> voList = BeanCopyUtil.copyListProperties(userDTOList, UserVO.class,
     *     BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName),
     *     BeanCopyUtil.mapping(UserDTO::getPhone, UserVO::setMobile)
     * );
     * }</pre>
     *
     * @param source      待拷贝的源数据集合，可以为 null
     * @param targetClass 目标对象的 Class 对象，必须有无参构造函数，不能为 null
     * @param mappings    自定义字段映射，可变参数，数量不限
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 目标对象集合，如果 source 为 null 则返回空集合
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    @SafeVarargs
    public static <S, T> List<T> copyListProperties(List<S> source, Class<T> targetClass, FieldMapping<S, T>... mappings) {
        if (source == null) {
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>(source.size());
        for (S s : source) {
            try {
                T target = targetClass.getDeclaredConstructor().newInstance();
                if (s != null) {
                    copyProperties(s, target);
                    if (mappings != null) {
                        for (FieldMapping<S, T> m : mappings) {
                            m.apply(s, target);
                        }
                    }
                }
                list.add(target);
            } catch (Exception e) {
                throw new RuntimeException("Bean拷贝并创建实例失败（通过Class<T>创建）", e);
            }
        }
        return list;
    }

    /**
     * 深拷贝：批量深度拷贝 List 集合，并对每个元素应用自定义字段映射（有返回值，深拷贝）
     * <p>
     * 先通过 JSON 序列化/反序列化进行深拷贝，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于 List 元素中包含复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // MenuEntity 中包含 List<MenuEntity> children，同时 menuName 映射到 name
     * List<MenuVO> voList = BeanCopyUtil.copyListProperties(menuEntityList, MenuVO::new,
     *     BeanCopyUtil.mapping(MenuEntity::getMenuName, MenuVO::setName),
     *     BeanCopyUtil.mapping(MenuEntity::getSort, MenuVO::setOrderNum)
     * );
     * }</pre>
     *
     * @param source   待拷贝的源数据集合，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuVO::new），不能为 null
     * @param mappings 自定义字段映射，可变参数，数量不限
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     * @return 目标对象集合，如果 source 为 null 则返回空集合
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    @SafeVarargs
    public static <S, T> List<T> copyListProperties(List<S> source, Supplier<T> supplier, FieldMapping<S, T>... mappings) {
        if (source == null) {
            return new ArrayList<>();
        }
        T tempInstance = supplier.get();
        Class<T> targetClass = (Class<T>) tempInstance.getClass();

        // 先整体深拷贝
        String json = JsonUtil.classToJson(source);
        List<T> list = JsonUtil.jsonToList(json, targetClass);

        // 再对每个元素应用自定义映射
        if (mappings != null && list != null) {
            for (int i = 0; i < source.size() && i < list.size(); i++) {
                S s = source.get(i);
                T t = list.get(i);
                if (s != null && t != null) {
                    for (FieldMapping<S, T> m : mappings) {
                        m.apply(s, t);
                    }
                }
            }
        }
        return list;
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
     * Map<String, MenuDTO> dtoMap = BeanCopyUtil.copyMapProperties(entityMap, MenuDTO::new);
     * // 结果：每个 MenuDTO 的 children 都会被正确转换为 List<MenuDTO>
     *
     * // 场景2：普通对象（也可以用，但性能不如浅拷贝）
     * Map<String, UserEntity> userMap = new HashMap<>();
     * Map<String, UserDTO> userDTOMap = BeanCopyUtil.copyMapProperties(userMap, UserDTO::new);
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
     * 浅拷贝：批量拷贝 Map 集合中的 value，并对每个元素应用自定义字段映射（有返回值，浅拷贝）
     * <p>
     * 先通过 {@code BeanUtils.copyProperties} 拷贝同名字段，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于 Map 的 value 结构简单、无复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Map<String, UserVO> voMap = BeanCopyUtil.copyMapProperties(userDTOMap, UserVO.class,
     *     BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName),
     *     BeanCopyUtil.mapping(UserDTO::getPhone, UserVO::setMobile)
     * );
     * }</pre>
     *
     * @param source      待拷贝的源数据 Map，key 必须是 String 类型，可以为 null
     * @param targetClass 目标对象的 Class 对象，必须有无参构造函数，不能为 null
     * @param mappings    自定义字段映射，可变参数，数量不限
     * @param <S>         源 value 对象类型
     * @param <T>         目标 value 对象类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    @SafeVarargs
    public static <S, T> Map<String, T> copyMapProperties(Map<String, S> source, Class<T> targetClass, FieldMapping<S, T>... mappings) {
        Map<String, T> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        for (Map.Entry<String, S> entry : source.entrySet()) {
            String key = entry.getKey();
            S sourceValue = entry.getValue();
            try {
                T target = targetClass.getDeclaredConstructor().newInstance();
                if (sourceValue != null) {
                    copyProperties(sourceValue, target);
                    if (mappings != null) {
                        for (FieldMapping<S, T> m : mappings) {
                            m.apply(sourceValue, target);
                        }
                    }
                }
                map.put(key, target);
            } catch (Exception e) {
                throw new RuntimeException("Bean拷贝并创建实例失败（通过Class<T>创建）", e);
            }
        }
        return map;
    }

    /**
     * 深拷贝：批量深度拷贝 Map 集合中的 value，并对每个元素应用自定义字段映射（有返回值，深拷贝）
     * <p>
     * 先通过 JSON 序列化/反序列化进行深拷贝，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于 Map 的 value 包含复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Map<String, MenuVO> voMap = BeanCopyUtil.copyMapProperties(menuEntityMap, MenuVO::new,
     *     BeanCopyUtil.mapping(MenuEntity::getMenuName, MenuVO::setName),
     *     BeanCopyUtil.mapping(MenuEntity::getSort, MenuVO::setOrderNum)
     * );
     * }</pre>
     *
     * @param source   待拷贝的源数据 Map，key 必须是 String 类型，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuVO::new），不能为 null
     * @param mappings 自定义字段映射，可变参数，数量不限
     * @param <S>      源 value 对象类型
     * @param <T>      目标 value 对象类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    @SafeVarargs
    public static <S, T> Map<String, T> copyMapProperties(Map<String, S> source, Supplier<T> supplier, FieldMapping<S, T>... mappings) {
        if (source == null) {
            return new HashMap<>();
        }
        T tempInstance = supplier.get();
        Class<T> targetClass = (Class<T>) tempInstance.getClass();

        // 先整体深拷贝
        String json = JsonUtil.classToJson(source);
        Map<String, T> map = JsonUtil.jsonToMap(json, targetClass);

        // 再对每个 value 应用自定义映射
        if (mappings != null && map != null) {
            for (Map.Entry<String, S> entry : source.entrySet()) {
                S sourceValue = entry.getValue();
                T targetValue = map.get(entry.getKey());
                if (sourceValue != null && targetValue != null) {
                    for (FieldMapping<S, T> m : mappings) {
                        m.apply(sourceValue, targetValue);
                    }
                }
            }
        }
        return map;
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
     * 浅拷贝：批量拷贝 Map&lt;String, List&gt; 集合，并对每个元素应用自定义字段映射（有返回值，浅拷贝）
     * <p>
     * 先通过 {@code BeanUtils.copyProperties} 拷贝同名字段，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于 Map 的 value 是 List，且 List 元素结构简单、无复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * Map<String, List<UserVO>> voMap = BeanCopyUtil.copyMapListProperties(userDTOMap, UserVO.class,
     *     BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName),
     *     BeanCopyUtil.mapping(UserDTO::getPhone, UserVO::setMobile)
     * );
     * }</pre>
     *
     * @param source      待拷贝的源数据 Map，key 必须是 String 类型，value 是 List 集合，可以为 null
     * @param targetClass 目标对象的 Class 对象，必须有无参构造函数，不能为 null
     * @param mappings    自定义字段映射，可变参数，数量不限
     * @param <S>         源 List 中的元素类型
     * @param <T>         目标 List 中的元素类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当目标类无法通过无参构造函数创建实例时抛出异常
     */
    @SafeVarargs
    public static <S, T> Map<String, List<T>> copyMapListProperties(Map<String, List<S>> source, Class<T> targetClass, FieldMapping<S, T>... mappings) {
        Map<String, List<T>> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        for (Map.Entry<String, List<S>> entry : source.entrySet()) {
            String key = entry.getKey();
            List<S> sourceList = entry.getValue();
            List<T> targetList = copyListProperties(sourceList, targetClass, mappings);
            map.put(key, targetList);
        }
        return map;
    }

    /**
     * 深拷贝：批量深度拷贝 Map&lt;String, List&gt; 集合，并对每个元素应用自定义字段映射（有返回值，深拷贝）
     * <p>
     * 先通过 JSON 序列化/反序列化进行深拷贝，再按照传入的映射关系处理字段名不一致的字段。<br>
     * 适用于 Map 的 value 是 List，且 List 元素包含复杂泛型嵌套的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // MenuEntity 中包含 List<MenuEntity> children，同时 menuName 映射到 name
     * Map<String, List<MenuVO>> voMap = BeanCopyUtil.copyMapListProperties(menuEntityMap, MenuVO::new,
     *     BeanCopyUtil.mapping(MenuEntity::getMenuName, MenuVO::setName),
     *     BeanCopyUtil.mapping(MenuEntity::getSort, MenuVO::setOrderNum)
     * );
     * }</pre>
     *
     * @param source   待拷贝的源数据 Map，key 必须是 String 类型，value 是 List 集合，可以为 null
     * @param supplier 目标对象的创建函数（例如：MenuVO::new），不能为 null
     * @param mappings 自定义字段映射，可变参数，数量不限
     * @param <S>      源 List 中的元素类型
     * @param <T>      目标 List 中的元素类型
     * @return 目标对象 Map，如果 source 为 null 则返回空 Map
     * @throws RuntimeException 当 JSON 转换失败时抛出异常
     */
    @SafeVarargs
    public static <S, T> Map<String, List<T>> copyMapListProperties(Map<String, List<S>> source, Supplier<T> supplier, FieldMapping<S, T>... mappings) {
        Map<String, List<T>> map = new HashMap<>();
        if (source == null) {
            return map;
        }
        for (Map.Entry<String, List<S>> entry : source.entrySet()) {
            String key = entry.getKey();
            List<S> sourceList = entry.getValue();
            List<T> targetList = copyListProperties(sourceList, supplier, mappings);
            map.put(key, targetList);
        }
        return map;
    }

    /**
     * 字段映射函数式接口
     * <p>
     * 用于描述一对字段的映射关系：从源对象读取值，写入目标对象。<br>
     * 配合 {@link #copyProperties(Object, Object, FieldMapping[])}、
     * {@link #copyProperties(Object, Class, FieldMapping[])}、
     * {@link #copyProperties(Object, Supplier, FieldMapping[])} 使用。
     * <p>
     * <b>创建方式（推荐使用工厂方法）：</b>
     * <pre>{@code
     * FieldMapping<UserDTO, UserVO> mapping = BeanCopyUtil.mapping(UserDTO::getNickName, UserVO::setName);
     * }</pre>
     *
     * @param <S> 源对象类型
     * @param <T> 目标对象类型
     */
    @FunctionalInterface
    public interface FieldMapping<S, T> {

        /**
         * 执行字段映射：从 source 中读取值，写入 target
         *
         * @param source 源对象
         * @param target 目标对象
         */
        void apply(S source, T target);
    }
}