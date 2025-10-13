package com.zmbdp.common.bloomfilter.service;

import java.util.Collection;

/**
 * 布隆过滤器服务 service 接口层
 *
 * @author 稚名不带撇
 */
public interface BloomFilterService {

    /**
     * 添加元素
     *
     * @param key 键
     */
    void put(String key);

    /**
     * 批量添加元素
     *
     * @param keys 键集合
     */
    void putAll(Collection<String> keys);

    /**
     * 判断单个元素是否存在
     *
     * @param key 键
     * @return true 存在，false 不存在
     */
    boolean mightContain(String key);

    /**
     * 判断集合中是否有任意元素存在
     *
     * @param keys 键集合
     * @return true 至少有一个存在，false 一个都不存在
     */
    boolean mightContainAny(Collection<String> keys);

    /**
     * 重置
     */
    void reset();

    /**
     * 扩容
     */
    void expand();

    /**
     * 获取状态
     *
     * @return 状态
     */
    String getStatus();

    /**
     * 计算负载因子
     *
     * @return 负载因子
     */
    double calculateLoadFactor();

    /**
     * 获取近似元素数量
     *
     * @return 数量
     */
    long approximateElementCount();

    /**
     * 获取精确元素数量
     *
     * @return 数量
     */
    long exactElementCount();

    /**
     * 获取实际元素数量
     *
     * @return 数量
     */
    int actualElementCount();

    /**
     * 清空布隆过滤器
     */
    void clear();
}
