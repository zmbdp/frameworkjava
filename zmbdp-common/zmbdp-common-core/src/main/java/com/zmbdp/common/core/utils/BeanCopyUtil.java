package com.zmbdp.common.core.utils;

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
     * @param target 拷贝之后的目标对象
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
     * @param target 拷贝之后的目标对象
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
}
