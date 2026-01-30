package com.zmbdp.common.log.service.impl;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.service.ILogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 控制台日志存储服务实现
 * <p>
 * 将日志输出到控制台/日志文件（SLF4J），这是默认的存储方式。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>默认存储方式，无需额外配置</li>
 *     <li>日志会输出到应用日志文件中，便于查看和排查问题</li>
 *     <li>适合开发环境和日志量较小的场景</li>
 *     <li>生产环境建议使用数据库存储，便于查询和分析</li>
 *     <li>可通过配置 {@code log.storage-type=console} 或注解 {@code @LogAction(storageType = "console")} 指定使用</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 */
@Slf4j
@Service("consoleLogStorageService")
public class ConsoleLogStorageService implements ILogStorageService {

    /**
     * 保存操作日志
     * <p>
     * 将日志输出到控制台/日志文件（SLF4J）。
     * <p>
     * <b>日志格式：</b>
     * <pre>
     * [操作日志] 操作: {}, 方法: {}, 用户: {}({}), IP: {}, 耗时: {}ms, 状态: {}, 时间: {}
     * </pre>
     *
     * @param logDTO 操作日志数据传输对象
     */
    @Override
    public void save(OperationLogDTO logDTO) {
        try {
            // 输出到日志文件（通过 SLF4J）
            log.info(
                    "[操作日志] 操作: {}, 方法: {}, 用户: {}({}), IP: {}, 耗时: {}ms, 状态: {}, 时间: {}",
                    logDTO.getOperation(),
                    logDTO.getMethod(),
                    logDTO.getUserName(),
                    logDTO.getUserId(),
                    logDTO.getClientIp(),
                    logDTO.getCostTime(),
                    logDTO.getStatus(),
                    logDTO.getOperationTime()
            );

            // 如果有参数，记录参数
            if (logDTO.getParams() != null && !logDTO.getParams().isEmpty()) {
                log.debug("[操作日志-参数] {}", logDTO.getParams());
            }

            // 如果有返回值，记录返回值
            if (logDTO.getResult() != null && !logDTO.getResult().isEmpty()) {
                log.debug("[操作日志-返回值] {}", logDTO.getResult());
            }

            // 如果有异常，记录异常
            if (logDTO.getException() != null && !logDTO.getException().isEmpty()) {
                log.error("[操作日志-异常] {}, 堆栈: {}", logDTO.getException(), logDTO.getExceptionStack());
            }

            // 输出完整日志对象（JSON 格式，便于后续处理）
            String logJson = JsonUtil.classToJson(logDTO);
            log.debug("[操作日志-完整] {}", logJson);
        } catch (Exception e) {
            // 存储失败不应该影响业务，只记录错误日志
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}
