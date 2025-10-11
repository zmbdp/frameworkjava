package com.zmbdp.common.snowflake.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 雪花算法生成全局唯一 ID 服务
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class SnowflakeIdService {

    /**
     * 开始时间截 2020-06-26 10:40:04
     */
    private final long twepoch = 1593139205000L;

    /**
     * 机器 id 所占的位数
     */
    private final long workerIdBits = 5L;


    /**
     * Twitter_Snowflake<br>
     * SnowFlake 的结构如下 (每部分用-分开):<br>
     * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
     * 1位标识，由于 long 基本类型在 Java 中是带符号的，最高位是符号位，正数是 0，负数是 1，所以 id 一般是正数，最高位是0<br>
     * 41位时间截(毫秒级)，注意，41位 时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
     * 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序 IdWorker 类的 startTime 属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
     * 10位 的数据机器位，可以部署在 1024个节点，包括 5位 datacenterId 和 5位 instanceId<br>
     * 12位 序列，毫秒内的计数，12位 的计数顺序号支持每个节点每毫秒 (同一机器，同一时间截) 产生 4096 个 ID 序号<br>
     * 加起来刚好 64 位，为一个 Long 型。<br>
     * SnowFlake 的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生 ID 碰撞 (由数据中心 ID 和机器 ID 作区分)，并且效率较高，经测试，SnowFlake 每秒能够产生 26 万 ID 左右。
     */

    /**
     * 数据标识id所占的位数
     */
    private final long datacenterIdBits = 5L;

    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    /**
     * 支持的最大数据标识id，结果是31
     */
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    /**
     * 序列在id中占的位数
     */
    private final long sequenceBits = 12L;

    /**
     * 机器ID向左移12位
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 数据标识id向左移17位(12+5)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 时间截向左移22位(5+5+12)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 生成序列的掩码，这里为 4095 (0b111111111111 = 0 x fff = 4095)
     */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    /**
     * 数据中心 ID(0~31)
     */
    private long datacenterId;

    /**
     * 工作机器 ID(0~31)
     */
    private long instanceId;

    /**
     * 毫秒内序列 (0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成 ID 的时间截
     */
    private long lastTimestamp = -1L;


    /**
     * 构造函数
     */
    public SnowflakeIdService() {
        if (instanceId > maxWorkerId || instanceId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次 ID 生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp
            ));
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        // 上次生成 ID 的时间截
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) // 时间戳部分
                | (datacenterId << datacenterIdShift) // 数据中心部分
                | (instanceId << workerIdShift) // 机器标识部分
                | sequence; // 毫秒内序列部分
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 批量生成 ID
     *
     * @param count 生成数量
     * @return ID列表
     */
    public synchronized List<Long> nextIds(int count) {
        List<Long> nextIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            nextIds.add(nextId());
        }
        return nextIds;
    }

    /**
     * 生成带前缀的 ID
     *
     * @param prefix 前缀
     * @return ID
     */
    public String nextIdStr(String prefix) {
        return prefix + nextId();
    }
}