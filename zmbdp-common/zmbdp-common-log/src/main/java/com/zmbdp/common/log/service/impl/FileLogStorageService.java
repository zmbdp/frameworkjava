package com.zmbdp.common.log.service.impl;

import cn.hutool.core.io.FileUtil;
import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.service.ILogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件日志存储服务实现
 * <p>
 * 将日志保存到文件系统中，适合日志量大的场景。
 * <p>
 * <b>使用说明：</b>
 * <ul>
 *     <li>可通过配置 {@code log.storage-type=file} 或注解 {@code @LogAction(storageType = "file")} 指定使用</li>
 *     <li>可通过 {@code log.file.path} 配置日志文件路径，默认：{@code ./logs/operation.log}</li>
 *     <li>日志以 JSON 格式追加到文件，每行一条记录</li>
 *     <li>适合日志量大的场景，便于后续日志采集和分析</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 */
@Slf4j
@Service("fileLogStorageService")
public class FileLogStorageService implements ILogStorageService {

    /**
     * 日期时间格式化器（用于日志文件名）
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 日志文件路径（可通过配置修改）
     */
    @Value("${log.file.path:./logs/operation/}")
    private String logFilePath;

    /**
     * 服务名称，用于日志文件名
     */
    @Value("${spring.application.name}")
    private String appName;

    /**
     * 保存操作日志
     * <p>
     * 将日志以 JSON 格式追加到文件，每行一条记录。
     *
     * @param logDTO 操作日志数据传输对象
     */
    @Override
    public void save(OperationLogDTO logDTO) {
        try {
            String logJson = JsonUtil.classToJson(logDTO);

            // 日期字符串
            String dateStr = LocalDate.now().format(DATE_FORMATTER);

            // 动态文件路径：./logs/operation/{服务名}/{日期}.log
            File logFile = new File(logFilePath + appName + "/" + dateStr + ".log");
            // 创建父目录（如果不存在）
            File parentDir = logFile.getParentFile();
            if (!parentDir.exists()) {
                FileUtil.mkdir(parentDir); // 正确创建目录
            }

            // 追加日志
            FileUtil.appendUtf8String(logJson + "\n", logFile);
        } catch (Exception e) {
            log.error("保存操作日志到文件失败: {}, 文件路径: {}", e.getMessage(), logFilePath, e);
        }
    }
}