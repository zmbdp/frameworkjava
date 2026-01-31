package com.zmbdp.mstemplate.service.service.impl;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.mstemplate.service.domain.LogTestDTO;
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

    @LogAction(value = "Console-01-基础测试", storageType = "console")
    public Result<String> console01Basic() {
        return Result.success("console存储-基础测试成功");
    }

    @LogAction(value = "Console-02-参数记录", recordParams = true, storageType = "console")
    public Result<String> console02RecordParams(LogTestDTO dto) {
        return Result.success("console存储-参数记录成功，userId=" + dto.getUserId());
    }

    @LogAction(value = "Console-03-返回值记录", recordResult = true, storageType = "console")
    public Result<LogTestDTO> console03RecordResult() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        dto.setUserName("测试用户");
        dto.setRemark("console存储-返回值记录成功");
        return Result.success(dto);
    }

    @LogAction(value = "Console-04-参数+返回值", recordParams = true, recordResult = true, storageType = "console")
    public Result<LogTestDTO> console04RecordBoth(LogTestDTO dto) {
        dto.setRemark("console存储-参数+返回值记录成功");
        return Result.success(dto);
    }

    @LogAction(value = "Console-05-异常记录(抛出)", recordException = true, throwException = true, storageType = "console")
    public Result<String> console05Exception() {
        throw new ServiceException("console存储-模拟业务异常（应抛出）");
    }

    @LogAction(value = "Console-06-异常记录(不抛出)", recordException = true, throwException = false, storageType = "console")
    public Result<String> console06ExceptionNoThrow() {
        throw new ServiceException("console存储-模拟业务异常（不抛出，仅记录）");
    }

    @LogAction(value = "Console-07-条件满足", condition = "#result != null && #result.code == 200000", storageType = "console")
    public Result<String> console07ConditionTrue() {
        return Result.success("console存储-条件满足（应记录日志）");
    }

    @LogAction(value = "Console-08-条件不满足", condition = "#result != null && #result.code != 200000", storageType = "console")
    public Result<String> console08ConditionFalse() {
        return Result.success("console存储-条件不满足（不应记录日志）");
    }

    @LogAction(value = "Console-09-参数表达式", recordParams = true, 
            paramsExpression = "{'userId': #dto.userId, 'userName': #dto.userName}", storageType = "console")
    public Result<String> console09ParamsExpression(LogTestDTO dto) {
        return Result.success("console存储-参数表达式成功");
    }

    @LogAction(value = "Console-10-返回值表达式", recordResult = true, resultExpression = "#result.data", storageType = "console")
    public Result<LogTestDTO> console10ResultExpression() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        dto.setUserName("测试用户");
        return Result.success(dto);
    }

    @LogAction(value = "Console-11-敏感字段脱敏", recordParams = true, desensitizeFields = "password,phone", storageType = "console")
    public Result<String> console11Desensitize(LogTestDTO dto) {
        return Result.success("console存储-脱敏成功");
    }

    @LogAction(value = "Console-12-模块业务类型", module = "日志测试", businessType = "功能验证", storageType = "console")
    public Result<String> console12ModuleBusiness() {
        return Result.success("console存储-模块业务类型成功");
    }

    @LogAction(value = "Console-13-void返回值", storageType = "console")
    public void console13VoidReturn() {
        log.info("console存储-void返回值测试执行");
    }

    /*=============================================    Database 存储测试方法    =============================================*/

    @LogAction(value = "Database-01-基础测试", storageType = "database")
    public Result<String> database01Basic() {
        return Result.success("database存储-基础测试成功");
    }

    @LogAction(value = "Database-02-参数记录", recordParams = true, storageType = "database")
    public Result<String> database02RecordParams(LogTestDTO dto) {
        return Result.success("database存储-参数记录成功");
    }

    @LogAction(value = "Database-03-返回值记录", recordResult = true, storageType = "database")
    public Result<LogTestDTO> database03RecordResult() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        return Result.success(dto);
    }

    @LogAction(value = "Database-04-参数+返回值", recordParams = true, recordResult = true, storageType = "database")
    public Result<LogTestDTO> database04RecordBoth(LogTestDTO dto) {
        return Result.success(dto);
    }

    @LogAction(value = "Database-05-异常记录(抛出)", recordException = true, throwException = true, storageType = "database")
    public Result<String> database05Exception() {
        throw new ServiceException("database存储-模拟业务异常");
    }

    @LogAction(value = "Database-06-异常记录(不抛出)", recordException = true, throwException = false, storageType = "database")
    public Result<String> database06ExceptionNoThrow() {
        throw new ServiceException("database存储-模拟业务异常");
    }

    @LogAction(value = "Database-07-条件满足", condition = "#result != null && #result.code == 200000", storageType = "database")
    public Result<String> database07ConditionTrue() {
        return Result.success("database存储-条件满足");
    }

    @LogAction(value = "Database-08-条件不满足", condition = "#result != null && #result.code != 200000", storageType = "database")
    public Result<String> database08ConditionFalse() {
        return Result.success("database存储-条件不满足");
    }

    @LogAction(value = "Database-09-参数表达式", recordParams = true, paramsExpression = "{'userId': #dto.userId}", storageType = "database")
    public Result<String> database09ParamsExpression(LogTestDTO dto) {
        return Result.success("database存储-参数表达式成功");
    }

    @LogAction(value = "Database-10-返回值表达式", recordResult = true, resultExpression = "#result.data", storageType = "database")
    public Result<LogTestDTO> database10ResultExpression() {
        return Result.success(new LogTestDTO());
    }

    @LogAction(value = "Database-11-敏感字段脱敏", recordParams = true, desensitizeFields = "password,phone", storageType = "database")
    public Result<String> database11Desensitize(LogTestDTO dto) {
        return Result.success("database存储-脱敏成功");
    }

    @LogAction(value = "Database-12-模块业务类型", module = "日志测试", businessType = "功能验证", storageType = "database")
    public Result<String> database12ModuleBusiness() {
        return Result.success("database存储-模块业务类型成功");
    }

    @LogAction(value = "Database-13-void返回值", storageType = "database")
    public void database13VoidReturn() {
        log.info("database存储-void返回值测试执行");
    }

    /*=============================================    File、Redis、MQ 存储测试方法（简化版）    =============================================*/

    @LogAction(value = "File-基础测试", storageType = "file")
    public Result<String> fileBasic() {
        return Result.success("file存储-基础测试成功");
    }

    @LogAction(value = "Redis-基础测试", storageType = "redis")
    public Result<String> redisBasic() {
        return Result.success("redis存储-基础测试成功");
    }

    @LogAction(value = "MQ-基础测试", storageType = "mq")
    public Result<String> mqBasic() {
        return Result.success("mq存储-基础测试成功");
    }
}

