package com.zmbdp.common.snowflake.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 雪花算法 ID 生成服务
 * <p>
 * 基于 Twitter 的 Snowflake 算法实现，用于生成全局唯一的 64 位长整型 ID。
 * 生成的 ID 具有时间有序性，适合作为数据库主键、分布式系统中的唯一标识等。
 * <p>
 * <b>ID 结构（64 位）：</b>
 * <ul>
 *     <li><b>1 位符号位</b>：固定为 0（正数）</li>
 *     <li><b>41 位时间戳</b>：当前时间与起始时间的差值（毫秒），可使用 69 年</li>
 *     <li><b>5 位数据中心 ID</b>：支持 32 个数据中心（0-31）</li>
 *     <li><b>5 位机器 ID</b>：支持 32 台机器（0-31）</li>
 *     <li><b>12 位序列号</b>：同一毫秒内的序列号，支持每毫秒生成 4096 个 ID</li>
 * </ul>
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>全局唯一</b>：通过数据中心 ID 和机器 ID 确保分布式环境下不重复</li>
 *     <li><b>时间有序</b>：ID 按时间递增，便于排序和分页</li>
 *     <li><b>高性能</b>：本地生成，无需网络请求，每秒可生成约 26 万个 ID</li>
 *     <li><b>线程安全</b>：使用 {@code synchronized} 关键字保证线程安全</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 生成单个 ID
 * @Autowired
 * private SnowflakeIdService snowflakeIdService;
 *
 * Long id = snowflakeIdService.nextId();
 * // 返回：1234567890123456789（64 位长整型）
 *
 * // 2. 批量生成 ID
 * List<Long> ids = snowflakeIdService.nextIds(100);
 * // 返回：包含 100 个唯一 ID 的列表
 *
 * // 3. 生成带前缀的 ID（字符串格式）
 * String orderId = snowflakeIdService.nextIdStr("ORDER_");
 * // 返回：ORDER_1234567890123456789
 *
 * // 4. 在实体类中使用
 * @Entity
 * public class Order {
 *     @Id
 *     private Long id; // 使用雪花算法生成的 ID
 *
 *     public void setId() {
 *         this.id = snowflakeIdService.nextId();
 *     }
 * }
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>数据中心 ID 和机器 ID 必须在 0-31 范围内</li>
 *     <li>同一数据中心和机器组合下，ID 保证唯一</li>
 *     <li>如果系统时钟回退，会抛出异常（防止 ID 重复）</li>
 *     <li>同一毫秒内最多生成 4096 个 ID，超过会阻塞到下一毫秒</li>
 *     <li>起始时间戳：2020-06-26 10:40:04（{@code twepoch}）</li>
 *     <li>ID 可使用约 69 年（从起始时间开始计算）</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see <a href="https://github.com/twitter/snowflake">Twitter Snowflake</a>
 */
@Slf4j
@Component
public class SnowflakeIdService {

    /**
     * 起始时间戳（2020-06-26 10:40:04）
     * <p>
     * 雪花算法的起始时间，ID 中的时间戳部分是从当前时间减去此起始时间得到的差值。
     * 使用此起始时间可以延长 ID 的使用期限（约 69 年）。
     */
    private final long twepoch = 1593139205000L;

    /**
     * 机器 ID 所占的位数
     * <p>
     * 5 位可以表示 0-31，共 32 个机器。
     */
    private final long workerIdBits = 5L;

    /**
     * 数据中心 ID 所占的位数
     * <p>
     * 5 位可以表示 0-31，共 32 个数据中心。
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
     * <p>
     * 验证数据中心 ID 和机器 ID 的有效性，确保它们在允许的范围内。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>机器 ID 必须在 0-31 范围内</li>
     *     <li>数据中心 ID 必须在 0-31 范围内</li>
     *     <li>如果超出范围，会抛出 {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @throws IllegalArgumentException 如果机器 ID 或数据中心 ID 超出允许范围
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
     * 生成下一个唯一 ID（线程安全）
     * <p>
     * 使用雪花算法生成全局唯一的 64 位长整型 ID。
     * 方法使用 {@code synchronized} 关键字保证线程安全。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 生成单个 ID
     * Long id = snowflakeIdService.nextId();
     * // 返回：1234567890123456789
     *
     * // 在实体类中使用
     * Order order = new Order();
     * order.setId(snowflakeIdService.nextId());
     * orderService.save(order);
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取当前时间戳（毫秒）</li>
     *     <li>检查系统时钟是否回退（如果回退则抛出异常）</li>
     *     <li>如果是同一毫秒，递增序列号；如果序列号溢出，阻塞到下一毫秒</li>
     *     <li>如果时间戳改变，重置序列号为 0</li>
     *     <li>组合时间戳、数据中心 ID、机器 ID、序列号生成 64 位 ID</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>线程安全：使用 {@code synchronized} 关键字</li>
     *     <li>如果系统时钟回退，会抛出 {@link RuntimeException}</li>
     *     <li>同一毫秒内最多生成 4096 个 ID，超过会阻塞到下一毫秒</li>
     *     <li>生成的 ID 按时间递增，便于排序</li>
     *     <li>性能：每秒可生成约 26 万个 ID</li>
     * </ul>
     *
     * @return 64 位长整型 ID，全局唯一且时间有序
     * @throws RuntimeException 如果系统时钟回退（防止 ID 重复）
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

        // 移位并通过或运算拼到一起组成 64 位的 ID
        return ((timestamp - twepoch) << timestampLeftShift) // 时间戳部分
                | (datacenterId << datacenterIdShift) // 数据中心部分
                | (instanceId << workerIdShift) // 机器标识部分
                | sequence; // 毫秒内序列部分
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * <p>
     * 当同一毫秒内序列号溢出时（超过 4096），需要等待到下一毫秒才能继续生成 ID。
     * 此方法会循环获取当前时间，直到时间戳大于上次生成 ID 的时间戳。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>同一毫秒内生成的 ID 数量超过 4096 个时调用</li>
     *     <li>确保时间戳递增，避免 ID 重复</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此方法会阻塞当前线程，直到下一毫秒</li>
     *     <li>在高并发场景下，如果同一毫秒内生成超过 4096 个 ID，会有轻微延迟</li>
     *     <li>正常情况下很少触发，因为 4096/毫秒 的生成速度已经很高</li>
     * </ul>
     *
     * @param lastTimestamp 上次生成 ID 的时间戳（毫秒），不能为负数
     * @return 新的时间戳（毫秒），保证大于 {@code lastTimestamp}
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒）
     * <p>
     * 返回系统当前时间的毫秒数，用于计算时间戳差值。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是系统当前时间的毫秒数</li>
     *     <li>时间戳会与起始时间戳（{@code twepoch}）做差值计算</li>
     *     <li>如果系统时钟被调整，可能影响 ID 的生成</li>
     * </ul>
     *
     * @return 当前时间戳（毫秒）
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 批量生成 ID（线程安全）
     * <p>
     * 连续生成指定数量的唯一 ID，返回 ID 列表。
     * 适用于需要一次性生成多个 ID 的场景，如批量插入数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 批量生成 100 个 ID
     * List<Long> ids = snowflakeIdService.nextIds(100);
     * // 返回：包含 100 个唯一 ID 的列表
     *
     * // 批量插入数据时使用
     * List<Order> orders = new ArrayList<>();
     * List<Long> ids = snowflakeIdService.nextIds(orders.size());
     * for (int i = 0; i < orders.size(); i++) {
     *     orders.get(i).setId(ids.get(i));
     * }
     * orderService.saveBatch(orders);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>线程安全：使用 {@code synchronized} 关键字</li>
     *     <li>生成的 ID 按时间有序排列</li>
     *     <li>如果数量很大，可能需要较长时间（每毫秒最多 4096 个）</li>
     *     <li>建议批量数量不要过大，避免阻塞时间过长</li>
     * </ul>
     *
     * @param count 要生成的 ID 数量，必须大于 0
     * @return ID 列表，按生成顺序排列，数量等于 {@code count}
     * @see #nextId()
     */
    public synchronized List<Long> nextIds(int count) {
        List<Long> nextIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            nextIds.add(nextId());
        }
        return nextIds;
    }

    /**
     * 生成带前缀的 ID（字符串格式）
     * <p>
     * 生成唯一 ID 并在前面添加指定前缀，返回字符串格式。
     * 适用于需要字符串格式 ID 的场景，如订单号、流水号等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 生成订单号
     * String orderId = snowflakeIdService.nextIdStr("ORDER_");
     * // 返回：ORDER_1234567890123456789
     *
     * // 生成流水号
     * String serialNo = snowflakeIdService.nextIdStr("SN");
     * // 返回：SN1234567890123456789
     *
     * // 在实体类中使用
     * Order order = new Order();
     * order.setOrderNo(snowflakeIdService.nextIdStr("ORDER_"));
     * orderService.save(order);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>前缀会直接拼接到 ID 前面，不做任何格式化</li>
     *     <li>返回的是字符串格式，不是数字格式</li>
     *     <li>前缀可以为空字符串（返回纯数字字符串）</li>
     *     <li>ID 部分仍然是全局唯一的</li>
     * </ul>
     *
     * @param prefix ID 前缀，可以为 null 或空字符串（返回纯数字字符串）
     * @return 带前缀的 ID 字符串，格式：{@code prefix + id}
     * @see #nextId()
     */
    public String nextIdStr(String prefix) {
        return prefix + nextId();
    }
}