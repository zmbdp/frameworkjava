package com.zmbdp.common.core.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bean 拷贝工具类
 *
 * @author 稚名不带撇
 */
public class BeanCopyUtil extends BeanUtils {
    /**
     * 批量拷贝集合类型里面的元素
     * @param source 待拷贝的数据
     * @param target 拷贝之后的目标对象
     * @return 目标对象集合
     * @param <S> 源类型
     * @param <T> 目标对象类型
     */
    public static <S, T> List<T> copyListProperties(List<S> source, Supplier<T> target) {
        List<T> list = new ArrayList<>(source.size());
        for (S s : source) {
            // 通过给我们的创建目标方法的引用 来 创建目标对象
            T t = target.get();
            // 然后单个对象拷贝属性，循环完成之后就拷贝完了
            copyProperties(s, t);
            list.add(t);
        }
        return list;
    }
}
