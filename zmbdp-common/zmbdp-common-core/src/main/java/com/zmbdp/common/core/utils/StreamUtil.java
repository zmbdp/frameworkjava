package com.zmbdp.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.zmbdp.common.domain.constants.CommonConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Stream 流工具类<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>提供常用的 Stream API 封装</li>
 *     <li>简化集合操作，提高代码可读性</li>
 *     <li>支持集合过滤、转换、分组、排序等操作</li>
 *     <li>自动处理空集合情况，避免 NPE</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>直接调用静态方法进行集合操作</li>
 *     <li>工具类会自动处理空集合，返回空集合或空 Map</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * // 过滤
 * List&lt;User&gt; adults = StreamUtil.filter(users, u -&gt; u.getAge() &gt;= 18);
 *
 * // 拼接
 * String names = StreamUtil.join(users, User::getName, ", ");
 *
 * // 转换为 Map
 * Map&lt;Long, User&gt; userMap = StreamUtil.toIdentityMap(users, User::getId);
 * </pre>
 *
 * <p>
 * 工具类说明：
 * <ul>
 *     <li>不允许实例化</li>
 *     <li>所有方法均为静态方法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtil {

    /**
     * 将集合过滤
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>根据条件过滤集合中的元素</li>
     *     <li>返回满足条件的所有元素</li>
     *     <li>空集合会返回空列表，不会抛出异常</li>
     * </ul>
     *
     * @param collection 需要过滤的集合
     * @param function   过滤方法（Predicate，返回 true 表示保留该元素）
     * @param <E>        集合元素类型
     * @return List&lt;E&gt; 过滤后的列表
     */
    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().filter(function).collect(Collectors.toList());
    }

    /**
     * 将集合拼接为字符串（使用默认分隔符逗号）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合元素转换为字符串并拼接</li>
     *     <li>使用逗号作为默认分隔符</li>
     *     <li>空集合会返回空字符串</li>
     * </ul>
     *
     * @param collection 需要拼接的集合
     * @param function   拼接方法（Function，将元素转换为字符串）
     * @param <E>        集合元素类型
     * @return String 拼接后的字符串
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function) {
        return join(collection, function, CommonConstants.COMMA_SEPARATOR);
    }

    /**
     * 将集合拼接为字符串（指定分隔符）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合元素转换为字符串并拼接</li>
     *     <li>可以自定义分隔符</li>
     *     <li>空集合会返回空字符串</li>
     *     <li>会自动过滤 null 值</li>
     * </ul>
     *
     * @param collection 需要拼接的集合
     * @param function   拼接方法（Function，将元素转换为字符串）
     * @param delimiter  分隔符
     * @param <E>        集合元素类型
     * @return String 拼接后的字符串
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return "";
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    /**
     * 将集合排序
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>根据指定的比较器对集合进行排序</li>
     *     <li>返回排序后的新列表</li>
     *     <li>空集合会返回空列表</li>
     * </ul>
     *
     * @param collection 需要排序的集合
     * @param comparing  排序方法（Comparator，定义排序规则）
     * @param <E>        集合元素类型
     * @return List&lt;E&gt; 排序后的列表
     */
    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().sorted(comparing).collect(Collectors.toList());
    }

    /**
     * 将集合转化为类型不变的 Map（Collection&lt;V&gt; -----&gt; Map&lt;K,V&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合转换为 Map，value 类型与集合元素类型相同</li>
     *     <li>适用于需要根据某个属性作为 key 的场景</li>
     *     <li>如果 key 重复，保留第一个元素</li>
     *     <li>空集合会返回空 Map</li>
     * </ul>
     *
     * @param collection 需要转化的集合
     * @param key        V 类型转化为 K 类型的 lambda 方法（用于生成 Map 的 key）
     * @param <V>        collection 中的泛型（也是 Map 的 value 类型）
     * @param <K>        Map 中的 key 类型
     * @return Map&lt;K, V&gt; 转化后的 Map
     */
    public static <V, K> Map<K, V> toIdentityMap(Collection<V> collection, Function<V, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().collect(Collectors.toMap(key, Function.identity(), (l, r) -> l));
    }

    /**
     * 将 Collection 转化为 Map（value 类型与 collection 的泛型不同）（Collection&lt;E&gt; -----&gt; Map&lt;K,V&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合转换为 Map，value 类型与集合元素类型不同</li>
     *     <li>需要同时指定 key 和 value 的转换方法</li>
     *     <li>如果 key 重复，保留第一个元素</li>
     *     <li>空集合会返回空 Map</li>
     * </ul>
     *
     * @param collection 需要转化的集合
     * @param key        E 类型转化为 K 类型的 lambda 方法（用于生成 Map 的 key）
     * @param value      E 类型转化为 V 类型的 lambda 方法（用于生成 Map 的 value）
     * @param <E>        collection 中的泛型
     * @param <K>        Map 中的 key 类型
     * @param <V>        Map 中的 value 类型
     * @return Map&lt;K, V&gt; 转化后的 Map
     */
    public static <E, K, V> Map<K, V> toMap(Collection<E> collection, Function<E, K> key, Function<E, V> value) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().collect(Collectors.toMap(key, value, (l, r) -> l));
    }

    /**
     * 将 collection 按照规则分类成 Map（Collection&lt;E&gt; -------&gt; Map&lt;K,List&lt;E&gt;&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合按照某个属性分组</li>
     *     <li>相同 key 的元素会被放到同一个 List 中</li>
     *     <li>使用 LinkedHashMap 保持插入顺序</li>
     *     <li>空集合会返回空 Map</li>
     * </ul>
     *
     * @param collection 需要分类的集合
     * @param key        分类的规则（Function，用于生成分组的 key）
     * @param <E>        collection 中的泛型
     * @param <K>        Map 中的 key 类型
     * @return Map&lt;K, List&lt;E&gt;&gt; 分类后的 Map
     */
    public static <E, K> Map<K, List<E>> groupByKey(Collection<E> collection, Function<E, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 将 collection 按照两个规则分类成双层 Map（Collection&lt;E&gt; ---&gt; Map&lt;K,Map&lt;U,List&lt;E&gt;&gt;&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合按照两个属性进行二级分组</li>
     *     <li>相同 key1 和 key2 的元素会被放到同一个 List 中</li>
     *     <li>使用 LinkedHashMap 保持插入顺序</li>
     *     <li>空集合会返回空 Map</li>
     * </ul>
     *
     * @param collection 需要分类的集合
     * @param key1       第一个分类的规则（Function，用于生成第一层 Map 的 key）
     * @param key2       第二个分类的规则（Function，用于生成第二层 Map 的 key）
     * @param <E>        集合元素类型
     * @param <K>        第一个 Map 中的 key 类型
     * @param <U>        第二个 Map 中的 key 类型
     * @return Map&lt;K, Map&lt;U, List&lt;E&gt;&gt;&gt; 分类后的双层 Map
     */
    public static <E, K, U> Map<K, Map<U, List<E>>> groupBy2Key(Collection<E> collection, Function<E, K> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(
                        key1, LinkedHashMap::new,
                        Collectors.groupingBy(key2, LinkedHashMap::new, Collectors.toList())
                ));
    }

    /**
     * 将 collection 按照两个规则分类成双层 Map（Collection&lt;E&gt; ---&gt; Map&lt;K,Map&lt;U,E&gt;&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合按照两个属性进行二级分组，每个元素只保留一个（最后一个）</li>
     *     <li>相同 key1 和 key2 的元素只会保留最后一个</li>
     *     <li>使用 LinkedHashMap 保持插入顺序</li>
     *     <li>空集合会返回空 Map</li>
     * </ul>
     *
     * @param collection 需要分类的集合
     * @param key1       第一个分类的规则（Function，用于生成第一层 Map 的 key）
     * @param key2       第二个分类的规则（Function，用于生成第二层 Map 的 key）
     * @param <E>        collection 中的泛型
     * @param <K>        第一个 Map 中的 key 类型
     * @param <U>        第二个 Map 中的 key 类型
     * @return Map&lt;K, Map&lt;U, E&gt;&gt; 分类后的双层 Map
     */
    public static <E, K, U> Map<K, Map<U, E>> group2Map(Collection<E> collection, Function<E, K> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection) || key1 == null || key2 == null) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(
                        key1, LinkedHashMap::new,
                        Collectors.toMap(key2, Function.identity(), (l, r) -> l)
                ));
    }

    /**
     * 将 collection 转化为 List 集合，但是两者的泛型不同（Collection&lt;E&gt; ------&gt; List&lt;T&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合元素转换为另一种类型的新集合</li>
     *     <li>适用于 DTO 转换等场景</li>
     *     <li>会自动过滤 null 值</li>
     *     <li>空集合会返回空列表</li>
     * </ul>
     *
     * @param collection 需要转化的集合
     * @param function   collection 中的泛型转化为 list 泛型的 lambda 表达式
     * @param <E>        collection 中的泛型
     * @param <T>        List 中的泛型
     * @return List&lt;T&gt; 转化后的 List
     */
    public static <E, T> List<T> toList(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection
                .stream()
                .map(function)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将 collection 转化为 Set 集合，但是两者的泛型不同（Collection&lt;E&gt; ------&gt; Set&lt;T&gt;）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将集合元素转换为另一种类型的新集合（Set，自动去重）</li>
     *     <li>适用于 DTO 转换等场景</li>
     *     <li>会自动过滤 null 值</li>
     *     <li>空集合会返回空 Set</li>
     * </ul>
     *
     * @param collection 需要转化的集合
     * @param function   collection 中的泛型转化为 set 泛型的 lambda 表达式
     * @param <E>        collection 中的泛型
     * @param <T>        Set 中的泛型
     * @return Set&lt;T&gt; 转化后的 Set
     */
    public static <E, T> Set<T> toSet(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection) || function == null) {
            return CollUtil.newHashSet();
        }
        return collection
                .stream()
                .map(function)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 合并两个相同 key 类型的 Map
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>合并两个 Map，根据相同的 key 进行合并</li>
     *     <li>可以通过 merge 函数自定义合并逻辑</li>
     *     <li>只保留合并后不为 null 的值</li>
     *     <li>空 Map 会返回空 Map</li>
     * </ul>
     *
     * @param map1  第一个需要合并的 Map
     * @param map2  第二个需要合并的 Map
     * @param merge 合并的 lambda，将 key、value1、value2 合并成最终的类型，注意 value 可能为空的情况
     * @param <K>   Map 中的 key 类型
     * @param <X>   第一个 Map 的 value 类型
     * @param <Y>   第二个 Map 的 value 类型
     * @param <V>   最终 Map 的 value 类型
     * @return Map&lt;K, V&gt; 合并后的 Map
     */
    public static <K, X, Y, V> Map<K, V> merge(Map<K, X> map1, Map<K, Y> map2, BiFunction<X, Y, V> merge) {
        // 如果两个 Map 都为空，则返回空 Map
        if (MapUtil.isEmpty(map1) && MapUtil.isEmpty(map2)) {
            return MapUtil.newHashMap();
        } else if (MapUtil.isEmpty(map1)) {
            // 如果第一个 Map 为空，则将第二个 Map 赋给第一个 Map
            map1 = MapUtil.newHashMap();
        } else if (MapUtil.isEmpty(map2)) {
            // 如果第二个 Map 为空，则将第一个 Map 赋给第二个 Map
            map2 = MapUtil.newHashMap();
        }
        // 获取两个 Map 的 key 集合
        Set<K> key = new HashSet<>();
        key.addAll(map1.keySet());
        key.addAll(map2.keySet());
        Map<K, V> map = new HashMap<>();
        //  遍历 key 集合，根据 key 获取对应的 value，并合并
        for (K t : key) {
            X x = map1.get(t);
            Y y = map2.get(t);
            V z = merge.apply(x, y);
            if (z != null) {
                map.put(t, z);
            }
        }
        return map;
    }
}
