package com.zmbdp.mstemplate.service.service.impl;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.mstemplate.service.domain.dto.LogTestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志测试辅助服务
 * <p>
 * 提供各种测试场景的实际业务方法，每个方法都标注了 @LogAction 注解。<br>
 * 通过不同的 storageType 参数来测试不同的存储方式。
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class TestLogServiceImpl {

    /*=============================================    Console 存储测试方法    =============================================*/

    /**
     * Console 存储-基础测试
     * <p>
     * 仅使用 @LogAction 的 value 属性，验证最基础的日志记录功能
     *
     * @return 测试结果
     */
    @LogAction(value = "Console-01-基础测试", storageType = "console")
    public Result<String> console01Basic() {
        return Result.success("console存储-基础测试成功");
    }

    /**
     * Console 存储-参数记录
     * <p>
     * 启用 recordParams 参数，验证日志中是否能正确记录入参
     *
     * @param dto 日志测试 DTO
     * @return 测试结果
     */
    @LogAction(value = "Console-02-参数记录", recordParams = true, storageType = "console")
    public Result<String> console02RecordParams(LogTestDTO dto) {
        return Result.success("console存储-参数记录成功，userId=" + dto.getUserId());
    }

    /**
     * Console 存储-返回值记录
     * <p>
     * 启用 recordResult 参数，验证日志中是否能正确记录返回值
     *
     * @return 包含测试数据的 DTO
     */
    @LogAction(value = "Console-03-返回值记录", recordResult = true, storageType = "console")
    public Result<LogTestDTO> console03RecordResult() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        dto.setUserName("测试用户");
        dto.setRemark("console存储-返回值记录成功");
        return Result.success(dto);
    }

    /**
     * Console 存储-参数+返回值
     * <p>
     * 同时启用 recordParams 和 recordResult，验证日志能否同时记录入参和返回值
     *
     * @param dto 日志测试 DTO
     * @return 处理后的 DTO
     */
    @LogAction(value = "Console-04-参数+返回值", recordParams = true, recordResult = true, storageType = "console")
    public Result<LogTestDTO> console04RecordBoth(LogTestDTO dto) {
        dto.setRemark("console存储-参数+返回值记录成功");
        return Result.success(dto);
    }

    /**
     * Console 存储-异常记录（抛出）
     * <p>
     * 启用 recordException 和 throwException=true，验证抛出异常时能否记录异常信息
     *
     * @throws ServiceException 模拟业务异常
     */
    @LogAction(value = "Console-05-异常记录(抛出)", recordException = true, throwException = true, storageType = "console")
    public Result<String> console05Exception() {
        throw new ServiceException("console存储-模拟业务异常（应抛出）");
    }

    /**
     * Console 存储-异常记录（不抛出）
     * <p>
     * 启用 recordException 且 throwException=false，验证记录异常但不抛出的场景
     *
     * @throws ServiceException 模拟业务异常
     */
    @LogAction(value = "Console-06-异常记录(不抛出)", recordException = true, throwException = false, storageType = "console")
    public Result<String> console06ExceptionNoThrow() {
        throw new ServiceException("console存储-模拟业务异常（不抛出，仅记录）");
    }

    /**
     * Console 存储-条件满足
     * <p>
     * 配置 condition 表达式（条件满足），验证满足条件时日志会被记录
     *
     * @return 测试结果
     */
    @LogAction(value = "Console-07-条件满足", condition = "#result != null && #result.code == 200000", storageType = "console")
    public Result<String> console07ConditionTrue() {
        return Result.success("console存储-条件满足（应记录日志）");
    }

    /**
     * Console 存储-条件不满足
     * <p>
     * 配置 condition 表达式（条件不满足），验证不满足条件时日志不会被记录
     *
     * @return 测试结果
     */
    @LogAction(value = "Console-08-条件不满足", condition = "#result != null && #result.code != 200000", storageType = "console")
    public Result<String> console08ConditionFalse() {
        return Result.success("console存储-条件不满足（不应记录日志）");
    }

    /**
     * Console 存储-参数表达式
     * <p>
     * 使用 paramsExpression 自定义参数记录内容，验证 SpEL 表达式能否正确解析
     *
     * @param dto 日志测试 DTO
     * @return 测试结果
     */
    @LogAction(value = "Console-09-参数表达式", recordParams = true, 
            paramsExpression = "{'userId': #dto.userId, 'userName': #dto.userName}", storageType = "console")
    public Result<String> console09ParamsExpression(LogTestDTO dto) {
        return Result.success("console存储-参数表达式成功");
    }

    /**
     * Console 存储-返回值表达式
     * <p>
     * 使用 resultExpression 自定义返回值记录内容，验证 SpEL 表达式能否正确解析
     *
     * @return 包含测试数据的 DTO
     */
    @LogAction(value = "Console-10-返回值表达式", recordResult = true, resultExpression = "#result.data", storageType = "console")
    public Result<LogTestDTO> console10ResultExpression() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        dto.setUserName("测试用户");
        return Result.success(dto);
    }

    /**
     * Console 存储-敏感字段脱敏
     * <p>
     * 配置 desensitizeFields 对 password 和 phone 字段进行脱敏处理
     *
     * @param dto 日志测试 DTO
     * @return 测试结果
     */
    @LogAction(value = "Console-11-敏感字段脱敏", recordParams = true, desensitizeFields = "password,phone", storageType = "console")
    public Result<String> console11Desensitize(LogTestDTO dto) {
        return Result.success("console存储-脱敏成功");
    }

    /**
     * Console 存储-模块业务类型
     * <p>
     * 配置 module 和 businessType 属性，验证日志的分类标签是否正确
     *
     * @return 测试结果
     */
    @LogAction(value = "Console-12-模块业务类型", module = "日志测试", businessType = "功能验证", storageType = "console")
    public Result<String> console12ModuleBusiness() {
        return Result.success("console存储-模块业务类型成功");
    }

    /**
     * Console 存储-void 返回值
     * <p>
     * 测试方法无返回值的场景，验证 void 方法能否正常记录日志
     */
    @LogAction(value = "Console-13-void返回值", storageType = "console")
    public void console13VoidReturn() {
        log.info("console存储-void返回值测试执行");
    }

    /*=============================================    Database 存储测试方法    =============================================*/

    /**
     * Database 存储-基础测试
     * <p>
     * 仅使用 @LogAction 的 value 属性，验证日志写入数据库的最基础功能
     *
     * @return 测试结果
     */
    @LogAction(value = "Database-01-基础测试", storageType = "database")
    public Result<String> database01Basic() {
        return Result.success("database存储-基础测试成功");
    }

    /**
     * Database 存储-参数记录
     * <p>
     * 启用 recordParams 参数，验证数据库日志中能否记录入参
     *
     * @param dto 日志测试 DTO
     * @return 测试结果
     */
    @LogAction(value = "Database-02-参数记录", recordParams = true, storageType = "database")
    public Result<String> database02RecordParams(LogTestDTO dto) {
        return Result.success("database存储-参数记录成功");
    }

    /**
     * Database 存储-返回值记录
     * <p>
     * 启用 recordResult 参数，验证数据库日志中能否记录返回值
     *
     * @return 包含测试数据的 DTO
     */
    @LogAction(value = "Database-03-返回值记录", recordResult = true, storageType = "database")
    public Result<LogTestDTO> database03RecordResult() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        return Result.success(dto);
    }

    /**
     * Database 存储-参数+返回值
     * <p>
     * 同时启用 recordParams 和 recordResult，验证数据库日志能否同时记录入参和返回值
     *
     * @param dto 日志测试 DTO
     * @return 处理后的 DTO
     */
    @LogAction(value = "Database-04-参数+返回值", recordParams = true, recordResult = true, storageType = "database")
    public Result<LogTestDTO> database04RecordBoth(LogTestDTO dto) {
        return Result.success(dto);
    }

    /**
     * Database 存储-异常记录（抛出）
     * <p>
     * 启用 recordException 和 throwException=true，验证抛出异常时能否记录异常信息到数据库
     *
     * @throws ServiceException 模拟业务异常
     */
    @LogAction(value = "Database-05-异常记录(抛出)", recordException = true, throwException = true, storageType = "database")
    public Result<String> database05Exception() {
        throw new ServiceException("database存储-模拟业务异常");
    }

    /**
     * Database 存储-异常记录（不抛出）
     * <p>
     * 启用 recordException 且 throwException=false，验证记录异常但不抛出的场景
     *
     * @throws ServiceException 模拟业务异常
     */
    @LogAction(value = "Database-06-异常记录(不抛出)", recordException = true, throwException = false, storageType = "database")
    public Result<String> database06ExceptionNoThrow() {
        throw new ServiceException("database存储-模拟业务异常");
    }

    /**
     * Database 存储-条件满足
     * <p>
     * 配置 condition 表达式（条件满足），验证满足条件时数据库日志会被记录
     *
     * @return 测试结果
     */
    @LogAction(value = "Database-07-条件满足", condition = "#result != null && #result.code == 200000", storageType = "database")
    public Result<String> database07ConditionTrue() {
        return Result.success("database存储-条件满足");
    }

    /**
     * Database 存储-条件不满足
     * <p>
     * 配置 condition 表达式（条件不满足），验证不满足条件时数据库日志不会被记录
     *
     * @return 测试结果
     */
    @LogAction(value = "Database-08-条件不满足", condition = "#result != null && #result.code != 200000", storageType = "database")
    public Result<String> database08ConditionFalse() {
        return Result.success("database存储-条件不满足");
    }

    /**
     * Database 存储-参数表达式
     * <p>
     * 使用 paramsExpression 自定义参数记录内容，验证 SpEL 表达式能否正确解析
     *
     * @param dto 日志测试 DTO
     * @return 测试结果
     */
    @LogAction(value = "Database-09-参数表达式", recordParams = true, paramsExpression = "{'userId': #dto.userId}", storageType = "database")
    public Result<String> database09ParamsExpression(LogTestDTO dto) {
        return Result.success("database存储-参数表达式成功");
    }

    /**
     * Database 存储-返回值表达式
     * <p>
     * 使用 resultExpression 自定义返回值记录内容，验证 SpEL 表达式能否正确解析
     *
     * @return 包含测试数据的 DTO
     */
    @LogAction(value = "Database-10-返回值表达式", recordResult = true, resultExpression = "#result.data", storageType = "database")
    public Result<LogTestDTO> database10ResultExpression() {
        return Result.success(new LogTestDTO());
    }

    /**
     * Database 存储-敏感字段脱敏
     * <p>
     * 配置 desensitizeFields 对 password 和 phone 字段进行脱敏处理
     *
     * @param dto 日志测试 DTO
     * @return 测试结果
     */
    @LogAction(value = "Database-11-敏感字段脱敏", recordParams = true, desensitizeFields = "password,phone", storageType = "database")
    public Result<String> database11Desensitize(LogTestDTO dto) {
        return Result.success("database存储-脱敏成功");
    }

    /**
     * Database 存储-模块业务类型
     * <p>
     * 配置 module 和 businessType 属性，验证数据库日志的分类标签是否正确
     *
     * @return 测试结果
     */
    @LogAction(value = "Database-12-模块业务类型", module = "日志测试", businessType = "功能验证", storageType = "database")
    public Result<String> database12ModuleBusiness() {
        return Result.success("database存储-模块业务类型成功");
    }

    /**
     * Database 存储-void 返回值
     * <p>
     * 测试方法无返回值的场景，验证 void 方法能否正常记录日志到数据库
     */
    @LogAction(value = "Database-13-void返回值", storageType = "database")
    public void database13VoidReturn() {
        log.info("database存储-void返回值测试执行");
    }

    /*=============================================    File、Redis、MQ 存储测试方法（简化版）    =============================================*/

    /**
     * File 存储-基础测试
     * <p>
     * 验证日志写入文件的基本功能
     *
     * @return 测试结果
     */
    @LogAction(value = "File-基础测试", storageType = "file")
    public Result<String> fileBasic() {
        return Result.success("file存储-基础测试成功");
    }

    /**
     * Redis 存储-基础测试
     * <p>
     * 验证日志写入 Redis 的基本功能
     *
     * @return 测试结果
     */
    @LogAction(value = "Redis-基础测试", storageType = "redis")
    public Result<String> redisBasic() {
        return Result.success("redis存储-基础测试成功");
    }

    /**
     * MQ 存储-基础测试
     * <p>
     * 验证日志通过消息队列发送的基本功能
     *
     * @return 测试结果
     */
    @LogAction(value = "MQ-基础测试", storageType = "mq")
    public Result<String> mqBasic() {
        return Result.success("mq存储-基础测试成功");
    }
}
