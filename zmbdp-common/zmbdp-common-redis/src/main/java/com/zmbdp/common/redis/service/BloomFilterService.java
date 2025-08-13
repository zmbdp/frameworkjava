package com.zmbdp.common.redis.service;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import com.zmbdp.common.redis.config.BloomFilterConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BloomFilterService {

    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    private BitMapBloomFilter bloomFilter;

    @PostConstruct
    public void init() {
        try {
            int expectedInsertions = bloomFilterConfig.getExpectedInsertions();

            // 添加边界检查
            if (expectedInsertions <= 0) {
                expectedInsertions = 500;
            }

            // 限制最大值防止计算溢出
            if (expectedInsertions > 1000) {
                expectedInsertions = 1000;
            }

            // 使用安全的方式初始化
            this.bloomFilter = new BitMapBloomFilter(expectedInsertions);

            log.info("布隆过滤器初始化成功，预期插入元素数量: {}", expectedInsertions);
        } catch (OutOfMemoryError e) {
            log.error("布隆过滤器初始化内存不足，使用最小配置", e);
            // 使用最小配置初始化
            this.bloomFilter = new BitMapBloomFilter(1000);
            log.info("布隆过滤器使用最小配置初始化成功");
        } catch (Exception e) {
            log.error("布隆过滤器初始化失败", e);
            throw new RuntimeException("布隆过滤器初始化失败", e);
        }
    }


    /**
     * 添加元素到布隆过滤器
     */
    public void put(String key) {
        bloomFilter.add(key);
    }

    /**
     * 判断元素是否可能存在于布隆过滤器中
     */
    public boolean mightContain(String key) {
        return bloomFilter.contains(key);
    }

    /**
     * 根据预期插入元素数量和误判率计算位图容量
     */
    private int calculateCapacity(int expectedInsertions, double falseProbability) {
        // 简单的容量计算公式
        // 实际应用中可以根据更精确的公式计算
        return (int) (expectedInsertions * Math.log(1 / falseProbability) / (Math.log(2) * Math.log(2))) + 1;
    }
}
