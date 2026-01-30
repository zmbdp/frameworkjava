package com.zmbdp.common.log.service.impl;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.domain.entity.OperationLog;
import com.zmbdp.common.log.mapper.OperationLogMapper;
import com.zmbdp.common.log.service.ILogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据库日志存储服务实现
 * <p>
 * 将日志保存到数据库中，适合需要查询和分析的场景。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>可通过配置 {@code log.storage-type=database} 或注解 {@code @LogAction(storageType = "database")} 指定使用</li>
 *     <li>需要引入 MyBatis Plus 依赖</li>
 *     <li>需要执行建表SQL创建 {@code operation_log} 表</li>
 *     <li>适合需要查询和分析日志的场景</li>
 * </ul>
 * <p>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 */
@Slf4j
@Service("databaseLogStorageService")
public class DatabaseLogStorageService implements ILogStorageService {

    /**
     * 操作日志 Mapper
     */
    @Autowired(required = false)
    private OperationLogMapper operationLogMapper;

    /**
     * 保存操作日志
     * <p>
     * 将日志保存到数据库表中。
     *
     * @param logDTO 操作日志数据传输对象
     */
    @Override
    public void save(OperationLogDTO logDTO) {
        try {
            if (operationLogMapper == null) {
                log.warn("OperationLogMapper 未注入，无法保存日志到数据库");
                return;
            }

            // 将 DTO 转换为实体对象
            OperationLog operationLog = BeanCopyUtil.copyProperties(logDTO, OperationLog.class);

            // 插入数据库
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            // 存储失败不应该影响业务，只记录错误日志
            log.error("保存操作日志到数据库失败: {}", e.getMessage(), e);
        }
    }
}
