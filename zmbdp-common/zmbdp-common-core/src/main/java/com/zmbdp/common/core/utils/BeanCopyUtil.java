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
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class BeanCopyUtil extends BeanUtils {

    /**
     * 批量拷贝 List 集合类型里面的元素
     *
     * @param source 待拷贝的数据
     * @param target 拷贝之后的目标对象
     * @param <S>    源类型
     * @param <T>    目标对象类型
     * @return 目标对象集合
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
     * 批量拷贝 Map 集合类型里面的元素
     *
     * @param source 待拷贝的数据
     * @param target 拷贝之后 value 的目标对象
     * @param <S>    源类型
     * @param <T>    目标对象类型
     * @return 目标对象集合
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
     * 批量拷贝 Map 集合类型里面的元素（支持复杂泛型嵌套）
     *
     * @param source 待拷贝的数据
     * @param target 拷贝之后 List 集合里面的的目标对象
     * @param <S>    源类型
     * @param <T>    目标对象类型
     * @return 目标对象集合
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
     * 将源对象的属性拷贝到目标类的新实例中。
     * <p>
     * 适用于目标类具有无参构造函数且无需复杂初始化的场景。
     *
     * @param source      源对象
     * @param targetClass 目标类的 Class 对象（必须有无参构造函数）
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
     * 将源对象的属性拷贝到由 Supplier 提供的目标对象实例中。
     * <p>
     * 适用于需要自定义目标对象创建逻辑的场景，如使用工厂、Spring Bean 或 Builder 模式。
     *
     * @param source   源对象
     * @param supplier 用于创建目标对象的工厂函数（例如：MyClass::new）
     * @param <S>      源对象类型
     * @param <T>      目标对象类型
     * @return 目标对象实例，若源对象为 null 则返回 null
     * @throws RuntimeException 当 supplier.get() 返回 null 或拷贝失败时抛出异常
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
