package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 时间戳工具类
 * <p>
 * 提供时间戳相关的工具方法，包括获取当前时间戳、计算未来时间戳、计算时间差等。
 * 基于 Java 8+ 的 Instant 和 ZonedDateTime 实现，使用 UTC 时区。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>获取当前时间戳（秒级和毫秒级）</li>
 *     <li>计算未来时间戳（秒、天、月、年）</li>
 *     <li>计算两个时间戳之间的差异</li>
 *     <li>统一使用 UTC 时区</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 获取当前时间戳
 * long seconds = TimestampUtil.getCurrentSeconds();
 * long millis = TimestampUtil.getCurrentMillis();
 *
 * // 计算未来时间戳
 * long futureSeconds = TimestampUtil.getSecondsLaterSeconds(3600); // 1小时后
 * long futureDays = TimestampUtil.getDaysLaterSeconds(7); // 7天后
 *
 * // 计算时间差
 * long diff = TimestampUtil.calculateDifferenceMillis(timestamp1, timestamp2);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>统一使用 UTC 时区进行计算</li>
 *     <li>时间戳分为秒级和毫秒级两种</li>
 *     <li>计算未来时间时，参数可以为负数（表示过去时间）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class TimestampUtil {

    /**
     * 获取当前时间戳（秒级）
     * <p>
     * 获取当前时间的 Unix 时间戳，单位为秒。
     * 适用于需要秒级精度的时间戳场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取当前时间戳（秒）
     * long timestamp = TimestampUtil.getCurrentSeconds();
     * // 结果：类似 1704067200（2024-01-01 00:00:00 UTC）
     *
     * // 用于 Token 过期时间
     * long expireTime = TimestampUtil.getCurrentSeconds() + 3600; // 1小时后过期
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是 Unix 时间戳（从 1970-01-01 00:00:00 UTC 开始的秒数）</li>
     *     <li>使用 UTC 时区</li>
     *     <li>精度为秒级</li>
     *     <li>如果需要毫秒级精度，使用 {@link #getCurrentMillis()}</li>
     * </ul>
     *
     * @return 当前时间戳（秒级，Unix 时间戳）
     * @see #getCurrentMillis()
     */
    public static long getCurrentSeconds() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取当前时间戳（毫秒级）
     * <p>
     * 获取当前时间的 Unix 时间戳，单位为毫秒。
     * 适用于需要毫秒级精度的时间戳场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取当前时间戳（毫秒）
     * long timestamp = TimestampUtil.getCurrentMillis();
     * // 结果：类似 1704067200000（2024-01-01 00:00:00 UTC）
     *
     * // 用于记录操作时间
     * long operationTime = TimestampUtil.getCurrentMillis();
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的是 Unix 时间戳（从 1970-01-01 00:00:00 UTC 开始的毫秒数）</li>
     *     <li>使用 UTC 时区</li>
     *     <li>精度为毫秒级</li>
     *     <li>如果需要秒级精度，使用 {@link #getCurrentSeconds()}</li>
     * </ul>
     *
     * @return 当前时间戳（毫秒级，Unix 时间戳）
     * @see #getCurrentSeconds()
     */
    public static long getCurrentMillis() {
        return Instant.now().toEpochMilli();
    }

    /**
     * 获取未来 x 秒的时间戳（秒级）
     * <p>
     * 计算当前时间加上指定秒数后的时间戳，返回秒级时间戳。
     * 适用于计算过期时间、有效期等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算1小时后的时间戳
     * long expireTime = TimestampUtil.getSecondsLaterSeconds(3600);
     *
     * // 计算30分钟后的时间戳
     * long futureTime = TimestampUtil.getSecondsLaterSeconds(1800);
     *
     * // 计算过去时间（负数）
     * long pastTime = TimestampUtil.getSecondsLaterSeconds(-3600); // 1小时前
     *
     * // 用于 Token 过期时间设置
     * long tokenExpireTime = TimestampUtil.getSecondsLaterSeconds(7200); // 2小时后过期
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回秒级时间戳</li>
     *     <li>seconds 可以为负数（表示过去时间）</li>
     *     <li>如果需要毫秒级时间戳，使用 {@link #getSecondsLaterMillis(long)}</li>
     * </ul>
     *
     * @param seconds 要增加的秒数（可以为负数），例如：3600 表示 1 小时后
     * @return 未来（或过去）x 秒的时间戳（秒级）
     * @see #getSecondsLaterMillis(long)
     */
    public static long getSecondsLaterSeconds(long seconds) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime secondsLater = now.plusSeconds(seconds);
        return secondsLater.toEpochSecond();
    }

    /**
     * 获取未来 x 秒的时间戳（毫秒级）
     * <p>
     * 计算当前时间加上指定秒数后的时间戳，返回毫秒级时间戳。
     * 注意：参数名为 seconds，但返回的是毫秒级时间戳。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算1小时后的时间戳（毫秒）
     * long expireTime = TimestampUtil.getSecondsLaterMillis(3600);
     *
     * // 计算30分钟后的时间戳（毫秒）
     * long futureTime = TimestampUtil.getSecondsLaterMillis(1800);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回毫秒级时间戳（虽然参数是秒）</li>
     *     <li>seconds 可以为负数（表示过去时间）</li>
     *     <li>参数单位是秒，但返回值是毫秒级时间戳</li>
     *     <li>如果需要秒级时间戳，使用 {@link #getSecondsLaterSeconds(long)}</li>
     * </ul>
     *
     * @param seconds 要增加的秒数（可以为负数），例如：3600 表示 1 小时后
     * @return 未来（或过去）x 秒的时间戳（毫秒级）
     * @see #getSecondsLaterSeconds(long)
     */
    public static long getSecondsLaterMillis(long seconds) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime secondsLater = now.plusSeconds(seconds);
        return secondsLater.toInstant().toEpochMilli();
    }

    /**
     * 获取未来 x 天的时间戳（秒级）
     * <p>
     * 计算当前时间加上指定天数后的时间戳，返回秒级时间戳。
     * 适用于计算几天后的过期时间等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算7天后的时间戳
     * long expireTime = TimestampUtil.getDaysLaterSeconds(7);
     *
     * // 计算30天后的时间戳
     * long futureTime = TimestampUtil.getDaysLaterSeconds(30);
     *
     * // 计算过去时间（负数）
     * long pastTime = TimestampUtil.getDaysLaterSeconds(-7); // 7天前
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回秒级时间戳</li>
     *     <li>days 可以为负数（表示过去时间）</li>
     *     <li>如果需要毫秒级时间戳，使用 {@link #getDaysLaterMillis(long)}</li>
     * </ul>
     *
     * @param days 要增加的天数（可以为负数），例如：7 表示 7 天后
     * @return 未来（或过去）x 天的时间戳（秒级）
     * @see #getDaysLaterMillis(long)
     */
    public static long getDaysLaterSeconds(long days) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime secondsLater = now.plusDays(days);
        return secondsLater.toEpochSecond();
    }

    /**
     * 获取未来 x 天的时间戳（毫秒级）
     * <p>
     * 计算当前时间加上指定天数后的时间戳，返回毫秒级时间戳。
     * 适用于计算几天后的过期时间等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算7天后的时间戳（毫秒）
     * long expireTime = TimestampUtil.getDaysLaterMillis(7);
     *
     * // 计算30天后的时间戳（毫秒）
     * long futureTime = TimestampUtil.getDaysLaterMillis(30);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回毫秒级时间戳</li>
     *     <li>days 可以为负数（表示过去时间）</li>
     *     <li>如果需要秒级时间戳，使用 {@link #getDaysLaterSeconds(long)}</li>
     * </ul>
     *
     * @param days 要增加的天数（可以为负数），例如：7 表示 7 天后
     * @return 未来（或过去）x 天的时间戳（毫秒级）
     * @see #getDaysLaterSeconds(long)
     */
    public static long getDaysLaterMillis(long days) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime monthsLater = now.plusDays(days);
        return monthsLater.toInstant().toEpochMilli();
    }

    /**
     * 获取未来 x 月的时间戳（秒级）
     * <p>
     * 计算当前时间加上指定月数后的时间戳，返回秒级时间戳。
     * 适用于计算几个月后的过期时间等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算1个月后的时间戳
     * long expireTime = TimestampUtil.getMonthsLaterSeconds(1);
     *
     * // 计算3个月后的时间戳
     * long futureTime = TimestampUtil.getMonthsLaterSeconds(3);
     *
     * // 计算过去时间（负数）
     * long pastTime = TimestampUtil.getMonthsLaterSeconds(-1); // 1个月前
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回秒级时间戳</li>
     *     <li>months 可以为负数（表示过去时间）</li>
     *     <li>月份计算会考虑月份天数差异（如 1 月 31 日 + 1 月 = 2 月 28/29 日）</li>
     *     <li>如果需要毫秒级时间戳，使用 {@link #getMonthsLaterMillis(long)}</li>
     * </ul>
     *
     * @param months 要增加的月数（可以为负数），例如：1 表示 1 个月后
     * @return 未来（或过去）x 月的时间戳（秒级）
     * @see #getMonthsLaterMillis(long)
     */
    public static long getMonthsLaterSeconds(long months) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime monthsLater = now.plusMonths(months);
        return monthsLater.toEpochSecond();
    }

    /**
     * 获取未来 x 月的时间戳（毫秒级）
     * <p>
     * 计算当前时间加上指定月数后的时间戳，返回毫秒级时间戳。
     * 适用于计算几个月后的过期时间等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算1个月后的时间戳（毫秒）
     * long expireTime = TimestampUtil.getMonthsLaterMillis(1);
     *
     * // 计算3个月后的时间戳（毫秒）
     * long futureTime = TimestampUtil.getMonthsLaterMillis(3);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回毫秒级时间戳</li>
     *     <li>months 可以为负数（表示过去时间）</li>
     *     <li>月份计算会考虑月份天数差异</li>
     *     <li>如果需要秒级时间戳，使用 {@link #getMonthsLaterSeconds(long)}</li>
     * </ul>
     *
     * @param months 要增加的月数（可以为负数），例如：1 表示 1 个月后
     * @return 未来（或过去）x 月的时间戳（毫秒级）
     * @see #getMonthsLaterSeconds(long)
     */
    public static long getMonthsLaterMillis(long months) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime monthsLater = now.plusMonths(months);
        return monthsLater.toInstant().toEpochMilli();
    }

    /**
     * 获取未来 x 年的时间戳（秒级）
     * <p>
     * 计算当前时间加上指定年数后的时间戳，返回秒级时间戳。
     * 适用于计算几年后的过期时间等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算1年后的时间戳
     * long expireTime = TimestampUtil.getYearLaterSeconds(1);
     *
     * // 计算5年后的时间戳
     * long futureTime = TimestampUtil.getYearLaterSeconds(5);
     *
     * // 计算过去时间（负数）
     * long pastTime = TimestampUtil.getYearLaterSeconds(-1); // 1年前
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回秒级时间戳</li>
     *     <li>years 可以为负数（表示过去时间）</li>
     *     <li>年份计算会考虑闰年（2 月 29 日）</li>
     *     <li>如果需要毫秒级时间戳，使用 {@link #getYearLaterMillis(long)}</li>
     * </ul>
     *
     * @param years 要增加的年数（可以为负数），例如：1 表示 1 年后
     * @return 未来（或过去）x 年的时间戳（秒级）
     * @see #getYearLaterMillis(long)
     */
    public static long getYearLaterSeconds(long years) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yearLater = now.plusYears(years);
        return yearLater.toEpochSecond();
    }

    /**
     * 获取未来 x 年的时间戳（毫秒级）
     * <p>
     * 计算当前时间加上指定年数后的时间戳，返回毫秒级时间戳。
     * 适用于计算几年后的过期时间等场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算1年后的时间戳（毫秒）
     * long expireTime = TimestampUtil.getYearLaterMillis(1);
     *
     * // 计算5年后的时间戳（毫秒）
     * long futureTime = TimestampUtil.getYearLaterMillis(5);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTC 时区进行计算</li>
     *     <li>返回毫秒级时间戳</li>
     *     <li>years 可以为负数（表示过去时间）</li>
     *     <li>年份计算会考虑闰年</li>
     *     <li>如果需要秒级时间戳，使用 {@link #getYearLaterSeconds(long)}</li>
     * </ul>
     *
     * @param years 要增加的年数（可以为负数），例如：1 表示 1 年后
     * @return 未来（或过去）x 年的时间戳（毫秒级）
     * @see #getYearLaterSeconds(long)
     */
    public static long getYearLaterMillis(long years) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yearLater = now.plusYears(years);
        return yearLater.toInstant().toEpochMilli();
    }

    /**
     * 计算两个时间戳之间的差异（毫秒）
     * <p>
     * 计算两个毫秒级时间戳之间的时间差，返回差异的毫秒数。
     * 结果 = timestamp2 - timestamp1（如果 timestamp2 更大，结果为正数）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算时间差
     * long startTime = TimestampUtil.getCurrentMillis();
     * // ... 执行操作
     * long endTime = TimestampUtil.getCurrentMillis();
     * long duration = TimestampUtil.calculateDifferenceMillis(startTime, endTime);
     * // 结果：操作耗时（毫秒）
     *
     * // 计算过期时间差
     * long now = TimestampUtil.getCurrentMillis();
     * long expireTime = TimestampUtil.getDaysLaterMillis(7);
     * long remaining = TimestampUtil.calculateDifferenceMillis(now, expireTime);
     * // 结果：剩余时间（毫秒），如果已过期则为负数
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>两个时间戳都必须是毫秒级</li>
     *     <li>结果 = timestamp2 - timestamp1</li>
     *     <li>如果 timestamp2 > timestamp1，结果为正数</li>
     *     <li>如果 timestamp2 < timestamp1，结果为负数</li>
     *     <li>如果需要秒级差异，使用 {@link #calculateDifferenceSeconds(long, long)}</li>
     * </ul>
     *
     * @param timestamp1 第一个时间戳（毫秒级）
     * @param timestamp2 第二个时间戳（毫秒级）
     * @return 时间戳差异（毫秒），结果 = timestamp2 - timestamp1
     * @see #calculateDifferenceSeconds(long, long)
     */
    public static long calculateDifferenceMillis(long timestamp1, long timestamp2) {
        return ChronoUnit.MILLIS.between(
                Instant.ofEpochMilli(timestamp1),
                Instant.ofEpochMilli(timestamp2));
    }

    /**
     * 计算两个时间戳之间的差异（秒）
     * <p>
     * 计算两个秒级时间戳之间的时间差，返回差异的秒数。
     * 结果 = timestamp2 - timestamp1（如果 timestamp2 更大，结果为正数）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 计算时间差
     * long startTime = TimestampUtil.getCurrentSeconds();
     * // ... 执行操作
     * long endTime = TimestampUtil.getCurrentSeconds();
     * long duration = TimestampUtil.calculateDifferenceSeconds(startTime, endTime);
     * // 结果：操作耗时（秒）
     *
     * // 计算过期时间差
     * long now = TimestampUtil.getCurrentSeconds();
     * long expireTime = TimestampUtil.getDaysLaterSeconds(7);
     * long remaining = TimestampUtil.calculateDifferenceSeconds(now, expireTime);
     * // 结果：剩余时间（秒），如果已过期则为负数
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>两个时间戳都必须是秒级</li>
     *     <li>结果 = timestamp2 - timestamp1</li>
     *     <li>如果 timestamp2 > timestamp1，结果为正数</li>
     *     <li>如果 timestamp2 < timestamp1，结果为负数</li>
     *     <li>如果需要毫秒级差异，使用 {@link #calculateDifferenceMillis(long, long)}</li>
     * </ul>
     *
     * @param timestamp1 第一个时间戳（秒级）
     * @param timestamp2 第二个时间戳（秒级）
     * @return 时间戳差异（秒），结果 = timestamp2 - timestamp1
     * @see #calculateDifferenceMillis(long, long)
     */
    public static long calculateDifferenceSeconds(long timestamp1, long timestamp2) {
        return ChronoUnit.SECONDS.between(
                Instant.ofEpochSecond(timestamp1),
                Instant.ofEpochSecond(timestamp2));
    }
}