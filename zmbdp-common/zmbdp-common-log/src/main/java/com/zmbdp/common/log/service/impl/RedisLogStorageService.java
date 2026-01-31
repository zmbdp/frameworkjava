package com.zmbdp.common.log.service.impl;

import com.zmbdp.common.domain.constants.LogConstants;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.service.ILogStorageService;
import com.zmbdp.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 日志存储服务实现
 * <p>
 * 将日志保存到 Redis，适合临时存储和快速查询。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>可通过配置 {@code log.storage-type=redis} 或注解 {@code @LogAction(storageType = "redis")} 指定使用</li>
 *     <li>需要引入 {@code zmbdp-common-redis} 依赖（如果未引入，此Bean不会注册）</li>
 *     <li>可通过 {@code log.redis.expire-time} 配置过期时间（秒），默认：7天</li>
 *     <li>日志以 JSON 格式存储到 Redis List 中，Key 格式：{@code log:operation:{date}}</li>
 *     <li>适合临时存储和快速查询，不适合长期存储</li>
 * </ul>
 * <p>
 * <b>条件注册说明：</b>
 * <ul>
 *     <li>使用 {@code @ConditionalOnClass(RedisService.class)} 而不是 {@code @ConditionalOnBean}</li>
 *     <li>只要 classpath 中存在 RedisService 类就注册，不依赖 Bean 加载顺序</li>
 *     <li>如果项目没有引入 {@code zmbdp-common-redis} 依赖，此 Bean 不会注册</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 */
@Slf4j
@Service("redisLogStorageService")
@ConditionalOnClass(RedisService.class)
public class RedisLogStorageService implements ILogStorageService {

    /**
     * Redis 服务
     */
    @Autowired(required = false)
    private RedisService redisService;

    /**
     * 日志过期时间（秒，默认：7天）
     */
    @Value("${log.redis.expire-time:604800}")
    private long expireTime;

    /**
     * 保存操作日志
     * <p>
     * 将日志对象直接存储到 Redis List 中，由 RedisTemplate 自动序列化为 JSON。
     *
     * @param logDTO 操作日志数据传输对象
     */
    @Override
    public void save(OperationLogDTO logDTO) {
        try {
            if (redisService == null) {
                log.warn("RedisService 未注入，无法保存日志到 Redis");
                return;
            }

            // 构建 Redis Key（按日期分类）
            String dateKey = logDTO.getOperationTime() != null
                    ? logDTO.getOperationTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : java.time.LocalDate.now().toString();
            String redisKey = LogConstants.LOG_KEY_PREFIX + dateKey;

            // 直接存储对象，RedisTemplate 会自动序列化为 JSON
            redisService.rightPushForList(redisKey, logDTO);

            // 设置过期时间（只在第一次设置）
            redisService.expire(redisKey, expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 存储失败不应该影响业务，只记录错误日志
            log.error("保存操作日志到 Redis 失败: {}", e.getMessage(), e);
        }
    }
}