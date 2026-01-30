package com.zmbdp.common.log.service;

import com.zmbdp.common.log.domain.dto.OperationLogDTO;

/**
 * 日志存储服务接口
 * <p>
 * 定义日志存储的抽象接口，支持多种存储方式（数据库、文件、Redis、消息队列等）。<br>
 * 实现类可以根据业务需求选择不同的存储方式。
 * <p>
 * <b>实现方式：</b>
 * <ul>
 *     <li><b>数据库存储</b>：将日志保存到数据库表中，便于查询和分析</li>
 *     <li><b>文件存储</b>：将日志保存到文件系统，适合日志量大的场景</li>
 *     <li><b>Redis 存储</b>：将日志保存到 Redis，适合临时存储和快速查询</li>
 *     <li><b>消息队列存储</b>：将日志发送到消息队列（如 RabbitMQ、Kafka），由消费者异步处理</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 实现数据库存储
 * @Service
 * public class DatabaseLogStorageService implements ILogStorageService {
 *     @Autowired
 *     private OperationLogMapper logMapper;
 *
 *     @Override
 *     public void save(OperationLogDTO logDTO) {
 *         OperationLog log = convertToEntity(logDTO);
 *         logMapper.insert(log);
 *     }
 * }
 *
 * // 实现文件存储
 * @Service
 * public class FileLogStorageService implements ILogStorageService {
 *     @Override
 *     public void save(OperationLogDTO logDTO) {
 *         String logContent = formatLog(logDTO);
 *         FileUtil.appendToFile("logs/operation.log", logContent);
 *     }
 * }
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>实现类应该处理异常，避免影响业务逻辑</li>
 *     <li>建议使用异步方式存储，避免阻塞业务线程</li>
 *     <li>存储失败时应该记录错误日志，便于排查问题</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.log.service.impl.ConsoleLogStorageService
 * @see com.zmbdp.common.log.service.impl.DatabaseLogStorageService
 * @see com.zmbdp.common.log.service.impl.FileLogStorageService
 * @see com.zmbdp.common.log.service.impl.MqLogStorageService
 * @see com.zmbdp.common.log.service.impl.RedisLogStorageService
 */
public interface ILogStorageService {

    /**
     * 保存操作日志
     * <p>
     * 将操作日志保存到指定的存储介质中。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此方法可能被异步调用，实现类应该保证线程安全</li>
     *     <li>如果存储失败，应该记录错误日志，但不应该抛出异常影响业务</li>
     *     <li>建议实现类对日志进行批量处理，提高性能</li>
     * </ul>
     *
     * @param logDTO 操作日志数据传输对象，不能为 null
     */
    void save(OperationLogDTO logDTO);
}
