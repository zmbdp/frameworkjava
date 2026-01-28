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
 * Stream 流工具类
 * <p>
 * 提供常用的 Stream API 封装，简化集合操作，提高代码可读性。<br>
 * 所有方法都自动处理空集合情况，避免 NPE，返回空集合或空 Map。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>集合过滤（filter）</li>
 *     <li>集合转换（toList、toSet、toMap）</li>
 *     <li>集合分组（groupByKey、groupBy2Key）</li>
 *     <li>集合排序（sorted）</li>
 *     <li>集合拼接（join）</li>
 *     <li>Map 合并（merge）</li>
 *     <li>自动处理空集合，避免 NPE</li>
 *     <li>自动过滤 null 值</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 过滤
 * List<User> adults = StreamUtil.filter(users, u -> u.getAge() >= 18);
 *
 * // 拼接（默认逗号分隔）
 * String names = StreamUtil.join(users, User::getName);
 *
 * // 拼接（自定义分隔符）
 * String names2 = StreamUtil.join(users, User::getName, " | ");
 *
 * // 转换为 Map（value 类型不变）
 * Map<Long, User> userMap = StreamUtil.toIdentityMap(users, User::getId);
 *
 * // 转换为 Map（value 类型改变）
 * Map<Long, String> nameMap = StreamUtil.toMap(users, User::getId, User::getName);
 *
 * // 分组
 * Map<String, List<User>> groupMap = StreamUtil.groupByKey(users, User::getDept);
 *
 * // 二级分组
 * Map<String, Map<String, List<User>>> group2Map =
 *     StreamUtil.groupBy2Key(users, User::getDept, User::getCity);
 *
 * // 排序
 * List<User> sorted = StreamUtil.sorted(users, Comparator.comparing(User::getAge));
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>如果 collection 为 null 或空，返回空集合或空 Map（不会抛出异常）</li>
 *     <li>转换方法会自动过滤 null 值</li>
 *     <li>分组方法使用 LinkedHashMap 保持插入顺序</li>
 *     <li>Map 转换时，如果 key 重复，保留第一个元素</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtil {

    /**
     * 将集合过滤
     * <p>
     * 根据指定的条件过滤集合中的元素，返回满足条件的所有元素。<br>
     * 使用 Stream API 的 filter 方法实现。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 过滤年龄大于等于 18 的用户
     * List<User> adults = StreamUtil.filter(users, u -> u.getAge() >= 18);
     *
     * // 过滤非空名称的用户
     * List<User> validUsers = StreamUtil.filter(users, u -> StringUtil.isNotEmpty(u.getName()));
     *
     * // 空集合处理
     * List<User> empty = StreamUtil.filter(null, u -> true); // 返回空列表
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空列表（不会抛出异常）</li>
     *     <li>function 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>返回新的 List，不会修改原集合</li>
     *     <li>使用 ArrayList 作为返回类型</li>
     * </ul>
     *
     * @param collection 需要过滤的集合，可以为 null 或空集合
     * @param function   过滤方法（Predicate，返回 true 表示保留该元素），不能为 null
     * @param <E>        集合元素类型
     * @return 过滤后的列表，如果 collection 为 null 或空则返回空列表
     * @throws NullPointerException 当 function 为 null 时抛出
     */
    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().filter(function).collect(Collectors.toList());
    }

    /**
     * 将集合拼接为字符串（使用默认分隔符逗号）
     * <p>
     * 将集合中的每个元素通过指定的函数转换为字符串，然后使用逗号（,）作为分隔符拼接。<br>
     * 自动过滤 null 值。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 拼接用户名称（逗号分隔）
     * String names = StreamUtil.join(users, User::getName);
     * // 结果：类似 "张三,李四,王五"
     *
     * // 拼接 ID
     * String ids = StreamUtil.join(users, u -> String.valueOf(u.getId()));
     * // 结果：类似 "1,2,3"
     *
     * // 空集合处理
     * String empty = StreamUtil.join(null, User::getName); // 返回空字符串
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空字符串</li>
     *     <li>function 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>自动过滤 null 值（function 返回 null 的元素会被忽略）</li>
     *     <li>默认使用逗号（,）作为分隔符</li>
     *     <li>如果需要自定义分隔符，使用 {@link #join(Collection, Function, CharSequence)}</li>
     * </ul>
     *
     * @param collection 需要拼接的集合，可以为 null 或空集合
     * @param function   拼接方法（Function，将元素转换为字符串），不能为 null
     * @param <E>        集合元素类型
     * @return 拼接后的字符串，如果 collection 为 null 或空则返回空字符串
     * @see #join(Collection, Function, CharSequence)
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function) {
        return join(collection, function, CommonConstants.COMMA_SEPARATOR);
    }

    /**
     * 将集合拼接为字符串（指定分隔符）
     * <p>
     * 将集合中的每个元素通过指定的函数转换为字符串，然后使用指定的分隔符拼接。<br>
     * 自动过滤 null 值。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用自定义分隔符
     * String names = StreamUtil.join(users, User::getName, " | ");
     * // 结果：类似 "张三 | 李四 | 王五"
     *
     * // 使用换行符分隔
     * String names2 = StreamUtil.join(users, User::getName, "\n");
     *
     * // 使用分号分隔
     * String ids = StreamUtil.join(users, u -> String.valueOf(u.getId()), ";");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空字符串</li>
     *     <li>function 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>delimiter 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>自动过滤 null 值（function 返回 null 的元素会被忽略）</li>
     *     <li>支持任意分隔符（包括空字符串、换行符等）</li>
     * </ul>
     *
     * @param collection 需要拼接的集合，可以为 null 或空集合
     * @param function   拼接方法（Function，将元素转换为字符串），不能为 null
     * @param delimiter  分隔符，不能为 null
     * @param <E>        集合元素类型
     * @return 拼接后的字符串，如果 collection 为 null 或空则返回空字符串
     * @see #join(Collection, Function)
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return "";
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    /**
     * 将集合排序
     * <p>
     * 根据指定的比较器对集合进行排序，返回排序后的新列表。<br>
     * 使用 Stream API 的 sorted 方法实现，不会修改原集合。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 按年龄升序排序
     * List<User> sortedByAge = StreamUtil.sorted(users, Comparator.comparing(User::getAge));
     *
     * // 按年龄降序排序
     * List<User> sortedByAgeDesc = StreamUtil.sorted(users,
     *     Comparator.comparing(User::getAge).reversed());
     *
     * // 多字段排序（先按部门，再按年龄）
     * List<User> sortedMulti = StreamUtil.sorted(users,
     *     Comparator.comparing(User::getDept)
     *         .thenComparing(User::getAge));
     *
     * // 按名称排序（忽略大小写）
     * List<User> sortedByName = StreamUtil.sorted(users,
     *     Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
     *
     * // 空集合处理
     * List<User> empty = StreamUtil.sorted(null, Comparator.comparing(User::getAge));
     * // 返回空列表
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空列表（不会抛出异常）</li>
     *     <li>comparing 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>返回新的 List，不会修改原集合</li>
     *     <li>使用 ArrayList 作为返回类型</li>
     *     <li>排序是稳定的（相等元素的相对顺序保持不变）</li>
     *     <li>如果集合元素实现了 Comparable 接口，也可以使用 Comparator.naturalOrder()</li>
     * </ul>
     *
     * @param collection 需要排序的集合，可以为 null 或空集合
     * @param comparing  排序比较器（Comparator，定义排序规则），不能为 null
     * @param <E>        集合元素类型
     * @return 排序后的新列表，如果 collection 为 null 或空则返回空列表
     * @throws NullPointerException 当 comparing 为 null 时抛出
     */
    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream().sorted(comparing).collect(Collectors.toList());
    }

    /**
     * 将集合转换为 Map（value 类型与集合元素类型相同）
     * <p>
     * 将集合转换为 Map，使用指定的函数提取 key，value 就是集合元素本身。<br>
     * 适用于需要根据某个属性作为 key 快速查找的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 以 ID 为 key，User 对象为 value
     * Map<Long, User> userMap = StreamUtil.toIdentityMap(users, User::getId);
     * // 使用：User user = userMap.get(1L);
     *
     * // 以名称为 key
     * Map<String, User> nameMap = StreamUtil.toIdentityMap(users, User::getName);
     *
     * // 空集合处理
     * Map<Long, User> empty = StreamUtil.toIdentityMap(null, User::getId); // 返回空 Map
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空 Map</li>
     *     <li>key 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>如果 key 重复，保留第一个元素（后面的会被忽略）</li>
     *     <li>value 类型与集合元素类型相同（identity 映射）</li>
     *     <li>如果需要转换 value 类型，使用 {@link #toMap(Collection, Function, Function)}</li>
     * </ul>
     *
     * @param collection 需要转换的集合，可以为 null 或空集合
     * @param key        提取 key 的函数（Function，从元素中提取 key），不能为 null
     * @param <V>        集合元素类型（也是 Map 的 value 类型）
     * @param <K>        Map 的 key 类型
     * @return 转换后的 Map，如果 collection 为 null 或空则返回空 Map
     * @throws NullPointerException 当 key 为 null 或 key 函数返回 null 时抛出
     * @see #toMap(Collection, Function, Function)
     */
    public static <V, K> Map<K, V> toIdentityMap(Collection<V> collection, Function<V, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().collect(Collectors.toMap(key, Function.identity(), (l, r) -> l));
    }

    /**
     * 将集合转换为 Map（value 类型与集合元素类型不同）
     * <p>
     * 将集合转换为 Map，同时指定 key 和 value 的转换方法。<br>
     * 适用于需要同时转换 key 和 value 类型的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 以 ID 为 key，名称为 value
     * Map<Long, String> nameMap = StreamUtil.toMap(users, User::getId, User::getName);
     *
     * // 以名称为 key，年龄为 value
     * Map<String, Integer> ageMap = StreamUtil.toMap(users, User::getName, User::getAge);
     *
     * // 复杂转换
     * Map<String, String> infoMap = StreamUtil.toMap(users,
     *     User::getName,
     *     u -> u.getAge() + "岁");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空 Map</li>
     *     <li>key 和 value 函数不能为 null，否则会抛出 NullPointerException</li>
     *     <li>key 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>如果 key 重复，保留第一个元素（后面的会被忽略）</li>
     *     <li>value 可以为 null</li>
     *     <li>如果只需要转换 key，value 保持原样，使用 {@link #toIdentityMap(Collection, Function)}</li>
     * </ul>
     *
     * @param collection 需要转换的集合，可以为 null 或空集合
     * @param key        提取 key 的函数（Function，从元素中提取 key），不能为 null
     * @param value      提取 value 的函数（Function，从元素中提取 value），不能为 null
     * @param <E>        集合元素类型
     * @param <K>        Map 的 key 类型
     * @param <V>        Map 的 value 类型
     * @return 转换后的 Map，如果 collection 为 null 或空则返回空 Map
     * @throws NullPointerException 当 key 或 value 为 null，或 key 函数返回 null 时抛出
     * @see #toIdentityMap(Collection, Function)
     */
    public static <E, K, V> Map<K, V> toMap(Collection<E> collection, Function<E, K> key, Function<E, V> value) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream().collect(Collectors.toMap(key, value, (l, r) -> l));
    }

    /**
     * 将集合按照规则分组（一级分组）
     * <p>
     * 将集合按照指定的 key 函数进行分组，相同 key 的元素会被放到同一个 List 中。<br>
     * 使用 LinkedHashMap 保持插入顺序。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 按部门分组
     * Map<String, List<User>> deptMap = StreamUtil.groupByKey(users, User::getDept);
     * // 结果：{"技术部": [user1, user2], "销售部": [user3]}
     *
     * // 按城市分组
     * Map<String, List<User>> cityMap = StreamUtil.groupByKey(users, User::getCity);
     *
     * // 空集合处理
     * Map<String, List<User>> empty = StreamUtil.groupByKey(null, User::getDept); // 返回空 Map
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空 Map</li>
     *     <li>key 函数不能为 null，否则会抛出 NullPointerException</li>
     *     <li>key 可以为 null，null 值的元素会被分到同一个组</li>
     *     <li>使用 LinkedHashMap 保持插入顺序</li>
     *     <li>每个 key 对应一个 List，即使只有一个元素</li>
     *     <li>如果需要二级分组，使用 {@link #groupBy2Key(Collection, Function, Function)}</li>
     * </ul>
     *
     * @param collection 需要分组的集合，可以为 null 或空集合
     * @param key        分组规则（Function，从元素中提取分组的 key），不能为 null
     * @param <E>        集合元素类型
     * @param <K>        分组的 key 类型
     * @return 分组后的 Map（key 为分组键，value 为该组的所有元素列表）
     * @throws NullPointerException 当 key 为 null 时抛出
     * @see #groupBy2Key(Collection, Function, Function)
     */
    public static <E, K> Map<K, List<E>> groupByKey(Collection<E> collection, Function<E, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 将集合按照两个规则分组（二级分组）
     * <p>
     * 将集合按照两个 key 函数进行二级分组，形成双层 Map 结构。
     * 相同 key1 和 key2 的元素会被放到同一个 List 中。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 先按部门分组，再按城市分组
     * Map<String, Map<String, List<User>>> groupMap =
     *     StreamUtil.groupBy2Key(users, User::getDept, User::getCity);
     * // 结果：{"技术部": {"北京": [user1], "上海": [user2]}, "销售部": {"北京": [user3]}}
     *
     * // 先按年份分组，再按月份分组
     * Map<Integer, Map<Integer, List<Order>>> orderMap =
     *     StreamUtil.groupBy2Key(orders, Order::getYear, Order::getMonth);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空 Map</li>
     *     <li>key1 和 key2 函数不能为 null，否则会抛出 NullPointerException</li>
     *     <li>key1 和 key2 都可以为 null，null 值的元素会被分到对应的组</li>
     *     <li>使用 LinkedHashMap 保持插入顺序</li>
     *     <li>每个 (key1, key2) 组合对应一个 List，即使只有一个元素</li>
     *     <li>如果需要一级分组，使用 {@link #groupByKey(Collection, Function)}</li>
     * </ul>
     *
     * @param collection 需要分组的集合，可以为 null 或空集合
     * @param key1       第一级分组规则（Function，从元素中提取第一层 key），不能为 null
     * @param key2       第二级分组规则（Function，从元素中提取第二层 key），不能为 null
     * @param <E>        集合元素类型
     * @param <K>        第一层 Map 的 key 类型
     * @param <U>        第二层 Map 的 key 类型
     * @return 二级分组后的双层 Map
     * @throws NullPointerException 当 key1 或 key2 为 null 时抛出
     * @see #groupByKey(Collection, Function)
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
     * 将集合按照两个规则分组为双层 Map（每个元素只保留一个）
     * <p>
     * 将集合按照两个 key 函数进行二级分组，形成双层 Map 结构。
     * 与 {@link #groupBy2Key(Collection, Function, Function)} 不同的是，每个 (key1, key2) 组合只保留一个元素（最后一个），而不是 List。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 先按部门分组，再按城市分组，每个部门+城市组合只保留一个用户（最后一个）
     * Map<String, Map<String, User>> groupMap =
     *     StreamUtil.group2Map(users, User::getDept, User::getCity);
     * // 结果：{"技术部": {"北京": user1, "上海": user2}, "销售部": {"北京": user3}}
     * // 注意：如果技术部-北京有多个用户，只保留最后一个
     *
     * // 先按年份分组，再按月份分组，每个年月组合只保留一个订单（最后一个）
     * Map<Integer, Map<Integer, Order>> orderMap =
     *     StreamUtil.group2Map(orders, Order::getYear, Order::getMonth);
     * }</pre>
     * <p>
     * <b>与 groupBy2Key 的区别：</b>
     * <ul>
     *     <li>groupBy2Key：返回 Map&lt;K, Map&lt;U, List&lt;E&gt;&gt;&gt;，保留所有元素</li>
     *     <li>group2Map：返回 Map&lt;K, Map&lt;U, E&gt;&gt;，每个组合只保留最后一个元素</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空 Map</li>
     *     <li>如果 key1 或 key2 为 null，返回空 Map（不会抛出异常）</li>
     *     <li>key1 和 key2 都可以为 null，null 值的元素会被分到对应的组</li>
     *     <li>使用 LinkedHashMap 保持插入顺序</li>
     *     <li>如果相同 (key1, key2) 组合有多个元素，只保留最后一个（后面的覆盖前面的）</li>
     *     <li>如果需要保留所有元素，使用 {@link #groupBy2Key(Collection, Function, Function)}</li>
     * </ul>
     *
     * @param collection 需要分组的集合，可以为 null 或空集合
     * @param key1       第一级分组规则（Function，从元素中提取第一层 key），可以为 null
     * @param key2       第二级分组规则（Function，从元素中提取第二层 key），可以为 null
     * @param <E>        集合元素类型
     * @param <K>        第一层 Map 的 key 类型
     * @param <U>        第二层 Map 的 key 类型
     * @return 二级分组后的双层 Map（每个组合只保留一个元素），如果 collection 为 null 或空则返回空 Map
     * @see #groupBy2Key(Collection, Function, Function)
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
     * 将集合转换为 List（元素类型转换）
     * <p>
     * 将集合中的每个元素通过指定的函数转换为另一种类型，生成新的 List 集合。
     * 自动过滤 null 值。适用于 DTO 转换、属性提取等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 提取用户名称列表
     * List<String> names = StreamUtil.toList(users, User::getName);
     *
     * // Entity 转 DTO
     * List<UserDTO> dtos = StreamUtil.toList(entities, e -> {
     *     UserDTO dto = new UserDTO();
     *     BeanCopyUtil.copyProperties(e, dto);
     *     return dto;
     * });
     *
     * // 提取 ID 列表
     * List<Long> ids = StreamUtil.toList(users, User::getId);
     *
     * // 复杂转换
     * List<String> fullNames = StreamUtil.toList(users,
     *     u -> u.getFirstName() + " " + u.getLastName());
     *
     * // 空集合处理
     * List<String> empty = StreamUtil.toList(null, User::getName); // 返回空列表
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空列表</li>
     *     <li>function 不能为 null，否则会抛出 NullPointerException</li>
     *     <li>自动过滤 null 值（function 返回 null 的元素会被忽略）</li>
     *     <li>返回新的 List，不会修改原集合</li>
     *     <li>使用 ArrayList 作为返回类型</li>
     *     <li>如果需要转换为 Set（去重），使用 {@link #toSet(Collection, Function)}</li>
     * </ul>
     *
     * @param collection 需要转换的集合，可以为 null 或空集合
     * @param function   转换函数（Function，将元素转换为目标类型），不能为 null
     * @param <E>        集合元素类型
     * @param <T>        目标 List 的元素类型
     * @return 转换后的 List，如果 collection 为 null 或空则返回空列表
     * @throws NullPointerException 当 function 为 null 时抛出
     * @see #toSet(Collection, Function)
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
     * 将集合转换为 Set（元素类型转换，自动去重）
     * <p>
     * 将集合中的每个元素通过指定的函数转换为另一种类型，生成新的 Set 集合。
     * 自动去重和过滤 null 值。适用于需要去重的转换场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 提取唯一的部门名称
     * Set<String> depts = StreamUtil.toSet(users, User::getDept);
     * // 结果：自动去重，只保留唯一的部门名称
     *
     * // 提取唯一的城市列表
     * Set<String> cities = StreamUtil.toSet(users, User::getCity);
     *
     * // 提取唯一的 ID（虽然 ID 本身就应该唯一）
     * Set<Long> uniqueIds = StreamUtil.toSet(users, User::getId);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 collection 为 null 或空，返回空 Set</li>
     *     <li>如果 function 为 null，返回空 Set（不会抛出异常）</li>
     *     <li>自动过滤 null 值（function 返回 null 的元素会被忽略）</li>
     *     <li>自动去重（使用 Set 的特性）</li>
     *     <li>返回新的 Set，不会修改原集合</li>
     *     <li>如果需要保留顺序或允许重复，使用 {@link #toList(Collection, Function)}</li>
     * </ul>
     *
     * @param collection 需要转换的集合，可以为 null 或空集合
     * @param function   转换函数（Function，将元素转换为目标类型），可以为 null
     * @param <E>        集合元素类型
     * @param <T>        目标 Set 的元素类型
     * @return 转换后的 Set，如果 collection 为 null 或空则返回空 Set
     * @see #toList(Collection, Function)
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
     * 合并两个 Map（自定义合并逻辑）
     * <p>
     * 合并两个具有相同 key 类型的 Map，通过自定义的 merge 函数处理相同 key 的 value 合并。
     * 只保留合并后不为 null 的值。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 合并两个 Map，相同 key 的值相加
     * Map<String, Integer> map1 = Map.of("a", 1, "b", 2);
     * Map<String, Integer> map2 = Map.of("a", 3, "c", 4);
     * Map<String, Integer> merged = StreamUtil.merge(map1, map2,
     *     (v1, v2) -> (v1 == null ? 0 : v1) + (v2 == null ? 0 : v2));
     * // 结果：{"a": 4, "b": 2, "c": 4}
     *
     * // 合并两个 Map，相同 key 的值取较大者
     * Map<String, Integer> merged2 = StreamUtil.merge(map1, map2,
     *     (v1, v2) -> Math.max(v1 == null ? 0 : v1, v2 == null ? 0 : v2));
     *
     * // 合并字符串 Map，相同 key 的值拼接
     * Map<String, String> strMap1 = Map.of("name", "张三");
     * Map<String, String> strMap2 = Map.of("name", "李四");
     * Map<String, String> strMerged = StreamUtil.merge(strMap1, strMap2,
     *     (v1, v2) -> (v1 == null ? "" : v1) + "," + (v2 == null ? "" : v2));
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果两个 Map 都为空，返回空 Map</li>
     *     <li>如果其中一个 Map 为空，会使用空 Map 替代（不会抛出异常）</li>
     *     <li>merge 函数不能为 null，否则会抛出 NullPointerException</li>
     *     <li>merge 函数的参数 v1 和 v2 可能为 null（当某个 key 只在一个 Map 中存在时）</li>
     *     <li>如果 merge 函数返回 null，该 key 不会被添加到结果 Map 中</li>
     *     <li>会遍历两个 Map 的所有 key 的并集</li>
     * </ul>
     *
     * @param map1  第一个需要合并的 Map，可以为 null 或空 Map
     * @param map2  第二个需要合并的 Map，可以为 null 或空 Map
     * @param merge 合并函数（BiFunction，将两个 value 合并为最终值），不能为 null
     * @param <K>   Map 的 key 类型（两个 Map 的 key 类型必须相同）
     * @param <X>   第一个 Map 的 value 类型
     * @param <Y>   第二个 Map 的 value 类型
     * @param <V>   合并后 Map 的 value 类型
     * @return 合并后的 Map，如果两个 Map 都为空则返回空 Map
     * @throws NullPointerException 当 merge 为 null 时抛出
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