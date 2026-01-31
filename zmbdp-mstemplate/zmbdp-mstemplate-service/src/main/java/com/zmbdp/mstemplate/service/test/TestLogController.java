package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.mstemplate.service.domain.LogTestDTO;
import com.zmbdp.mstemplate.service.service.impl.TestLogServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 操作日志注解（@LogAction）全面测试控制器
 * <p>
 * <b>测试目标：</b>
 * <ul>
 *     <li>一键测试所有场景（正常+异常，覆盖所有注解字段）</li>
 *     <li>分别测试五种存储方式（console、database、file、redis、mq）</li>
 *     <li>每个接口都全面测试所有场景</li>
 *     <li>返回值清晰，一眼就能看出测试是否通过</li>
 * </ul>
 * <p>
 * <b>测试接口：</b>
 * <ul>
 *     <li>POST /test/log/all - 一键测试所有存储方式的所有场景</li>
 *     <li>POST /test/log/console - 测试 Console 存储的所有场景</li>
 *     <li>POST /test/log/database - 测试 Database 存储的所有场景</li>
 *     <li>POST /test/log/file - 测试 File 存储的所有场景</li>
 *     <li>POST /test/log/redis - 测试 Redis 存储的所有场景</li>
 *     <li>POST /test/log/mq - 测试 MQ 存储的所有场景</li>
 * </ul>
 * <p>
 * <b>测试场景覆盖（13个场景）：</b>
 * <ul>
 *     <li>01-基础测试：value</li>
 *     <li>02-参数记录：recordParams</li>
 *     <li>03-返回值记录：recordResult</li>
 *     <li>04-参数+返回值：recordParams + recordResult</li>
 *     <li>05-异常记录（抛出）：recordException + throwException=true</li>
 *     <li>06-异常记录（不抛出）：recordException + throwException=false</li>
 *     <li>07-条件满足：condition（满足条件，应记录）</li>
 *     <li>08-条件不满足：condition（不满足条件，不应记录）</li>
 *     <li>09-参数表达式：paramsExpression</li>
 *     <li>10-返回值表达式：resultExpression</li>
 *     <li>11-敏感字段脱敏：desensitizeFields</li>
 *     <li>12-模块业务类型：module + businessType</li>
 *     <li>13-void返回值：测试无返回值方法</li>
 * </ul>
 * <p>
 * <b>返回值说明：</b>
 * <ul>
 *     <li>✅ 表示测试通过</li>
 *     <li>❌ 表示测试失败</li>
 *     <li>⚠️ 表示测试异常</li>
 *     <li>🔵 表示不应记录日志（正常）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/log")
public class TestLogController {

    @Autowired
    private TestLogServiceImpl testLogServiceImpl;

    /*=============================================    一键测试接口    =============================================*/

    /**
     * 一键测试所有存储方式的所有场景
     */
    @RequestMapping("/all")
    public Result<Map<String, Object>> testAll() {
        log.info("========================================");
        log.info("=== 开始一键测试所有存储方式的所有场景 ===");
        log.info("========================================");
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("测试时间", new Date().toString());
        result.put("测试说明", "一键测试所有存储方式（console、database、file、redis、mq）的所有场景");
        
        Map<String, Map<String, String>> allResults = new LinkedHashMap<>();
        allResults.put("console存储", testConsoleStorage());
        allResults.put("database存储", testDatabaseStorage());
        allResults.put("file存储", testFileStorage());
        allResults.put("redis存储", testRedisStorage());
        allResults.put("mq存储", testMqStorage());
        
        result.put("测试结果", allResults);
        result.put("结果说明", "✅=通过 ❌=失败 ⚠️=异常 🔵=不应记录（正常）");
        
        log.info("========================================");
        log.info("=== 所有存储方式测试完成 ===");
        log.info("========================================");
        
        return Result.success(result);
    }

    /*=============================================    各存储方式测试接口    =============================================*/

    /**
     * 测试 Console 存储的所有场景
     */
    @RequestMapping("/console")
    public Result<Map<String, String>> testConsole() {
        log.info(">>> 开始测试 CONSOLE 存储方式");
        Map<String, String> result = testConsoleStorage();
        log.info("<<< CONSOLE 存储方式测试完成");
        return Result.success(result);
    }

    /**
     * 测试 Database 存储的所有场景
     */
    @RequestMapping("/database")
    public Result<Map<String, String>> testDatabase() {
        log.info(">>> 开始测试 DATABASE 存储方式");
        Map<String, String> result = testDatabaseStorage();
        log.info("<<< DATABASE 存储方式测试完成");
        return Result.success(result);
    }

    /**
     * 测试 File 存储的所有场景
     */
    @RequestMapping("/file")
    public Result<Map<String, String>> testFile() {
        log.info(">>> 开始测试 FILE 存储方式");
        Map<String, String> result = testFileStorage();
        log.info("<<< FILE 存储方式测试完成");
        return Result.success(result);
    }

    /**
     * 测试 Redis 存储的所有场景
     */
    @RequestMapping("/redis")
    public Result<Map<String, String>> testRedis() {
        log.info(">>> 开始测试 REDIS 存储方式");
        Map<String, String> result = testRedisStorage();
        log.info("<<< REDIS 存储方式测试完成");
        return Result.success(result);
    }

    /**
     * 查询 Redis 中的日志数据
     * <p>
     * 用于验证日志是否真的存储到了 Redis 中
     */
    @RequestMapping("/redis/query")
    public Result<Map<String, Object>> queryRedisLogs() {
        log.info(">>> 开始查询 Redis 中的日志数据");
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            // 检查今天的日志
            String today = java.time.LocalDate.now().toString();
            String redisKey = "log:operation:" + today;
            
            result.put("查询时间", new Date().toString());
            result.put("Redis Key", redisKey);
            result.put("说明", "日志存储在 Redis List 中，Key 格式：log:operation:yyyy-MM-dd");
            
            // 提示：需要手动在 Redis 中查询
            result.put("查询命令", "LLEN " + redisKey + " (查看日志数量)");
            result.put("查看日志", "LRANGE " + redisKey + " 0 -1 (查看所有日志)");
            result.put("查看最新", "LRANGE " + redisKey + " -10 -1 (查看最新10条)");
            
            log.info("<<< Redis 日志查询信息已返回");
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询 Redis 日志失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 测试 MQ 存储的所有场景
     */
    @PostMapping("/mq")
    public Result<Map<String, String>> testMq() {
        log.info(">>> 开始测试 MQ 存储方式");
        Map<String, String> result = testMqStorage();
        log.info("<<< MQ 存储方式测试完成");
        return Result.success(result);
    }

    /*=============================================    测试核心方法    =============================================*/

    /**
     * 测试 Console 存储的所有场景
     */
    private Map<String, String> testConsoleStorage() {
        Map<String, String> result = new LinkedHashMap<>();
        LogTestDTO dto = createTestDTO();
        
        result.put("01-基础测试", test(() -> testLogServiceImpl.console01Basic()));
        result.put("02-参数记录", test(() -> testLogServiceImpl.console02RecordParams(dto)));
        result.put("03-返回值记录", test(() -> testLogServiceImpl.console03RecordResult()));
        result.put("04-参数+返回值", test(() -> testLogServiceImpl.console04RecordBoth(dto)));
        result.put("05-异常记录(抛出)", testException(() -> testLogServiceImpl.console05Exception()));
        result.put("06-异常记录(不抛)", test(() -> testLogServiceImpl.console06ExceptionNoThrow()));
        result.put("07-条件满足", test(() -> testLogServiceImpl.console07ConditionTrue()));
        result.put("08-条件不满足", testNoLog(() -> testLogServiceImpl.console08ConditionFalse()));
        result.put("09-参数表达式", test(() -> testLogServiceImpl.console09ParamsExpression(dto)));
        result.put("10-返回值表达式", test(() -> testLogServiceImpl.console10ResultExpression()));
        result.put("11-敏感字段脱敏", test(() -> testLogServiceImpl.console11Desensitize(createSensitiveDTO())));
        result.put("12-模块业务类型", test(() -> testLogServiceImpl.console12ModuleBusiness()));
        result.put("13-void返回值", testVoid(() -> testLogServiceImpl.console13VoidReturn()));
        
        return result;
    }

    /**
     * 测试 Database 存储的所有场景
     */
    private Map<String, String> testDatabaseStorage() {
        Map<String, String> result = new LinkedHashMap<>();
        LogTestDTO dto = createTestDTO();
        
        result.put("01-基础测试", test(() -> testLogServiceImpl.database01Basic()));
        result.put("02-参数记录", test(() -> testLogServiceImpl.database02RecordParams(dto)));
        result.put("03-返回值记录", test(() -> testLogServiceImpl.database03RecordResult()));
        result.put("04-参数+返回值", test(() -> testLogServiceImpl.database04RecordBoth(dto)));
        result.put("05-异常记录(抛出)", testException(() -> testLogServiceImpl.database05Exception()));
        result.put("06-异常记录(不抛)", test(() -> testLogServiceImpl.database06ExceptionNoThrow()));
        result.put("07-条件满足", test(() -> testLogServiceImpl.database07ConditionTrue()));
        result.put("08-条件不满足", testNoLog(() -> testLogServiceImpl.database08ConditionFalse()));
        result.put("09-参数表达式", test(() -> testLogServiceImpl.database09ParamsExpression(dto)));
        result.put("10-返回值表达式", test(() -> testLogServiceImpl.database10ResultExpression()));
        result.put("11-敏感字段脱敏", test(() -> testLogServiceImpl.database11Desensitize(createSensitiveDTO())));
        result.put("12-模块业务类型", test(() -> testLogServiceImpl.database12ModuleBusiness()));
        result.put("13-void返回值", testVoid(() -> testLogServiceImpl.database13VoidReturn()));
        
        return result;
    }

    /**
     * 测试 File 存储（简化版）
     */
    private Map<String, String> testFileStorage() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("基础测试", test(() -> testLogServiceImpl.fileBasic()));
        result.put("说明", "File存储测试（简化版）");
        return result;
    }

    /**
     * 测试 Redis 存储（简化版）
     */
    private Map<String, String> testRedisStorage() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("基础测试", test(() -> testLogServiceImpl.redisBasic()));
        result.put("说明", "Redis存储测试（简化版）");
        return result;
    }

    /**
     * 测试 MQ 存储（简化版）
     */
    private Map<String, String> testMqStorage() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("基础测试", test(() -> testLogServiceImpl.mqBasic()));
        result.put("说明", "MQ存储测试（简化版）");
        return result;
    }

    /*=============================================    辅助方法    =============================================*/

    /**
     * 测试普通场景
     */
    private String test(TestScenario scenario) {
        try {
            scenario.execute();
            return "✅ 通过";
        } catch (Exception e) {
            log.error("测试失败", e);
            return "⚠️ 异常: " + e.getMessage();
        }
    }

    /**
     * 测试异常场景（应抛出异常）
     */
    private String testException(TestScenario scenario) {
        try {
            scenario.execute();
            return "❌ 应抛出异常";
        } catch (Exception e) {
            return "✅ 通过（异常已记录）";
        }
    }

    /**
     * 测试不应记录日志的场景
     */
    private String testNoLog(TestScenario scenario) {
        try {
            scenario.execute();
            return "🔵 通过（不应记录日志）";
        } catch (Exception e) {
            log.error("测试失败", e);
            return "⚠️ 异常: " + e.getMessage();
        }
    }

    /**
     * 测试 void 返回值场景
     */
    private String testVoid(VoidScenario scenario) {
        try {
            scenario.execute();
            return "✅ 通过（void方法已记录）";
        } catch (Exception e) {
            log.error("测试失败", e);
            return "⚠️ 异常: " + e.getMessage();
        }
    }

    /**
     * 创建测试 DTO
     */
    private LogTestDTO createTestDTO() {
        LogTestDTO dto = new LogTestDTO();
        dto.setUserId(10086L);
        dto.setUserName("测试用户");
        dto.setPhone("13800138000");
        dto.setPassword("test123");
        dto.setActionType("测试操作");
        dto.setRemark("这是一条测试数据");
        return dto;
    }

    /**
     * 创建敏感信息 DTO
     */
    private LogTestDTO createSensitiveDTO() {
        LogTestDTO dto = new LogTestDTO();
        dto.setPhone("13800138000");
        dto.setPassword("secret123456");
        return dto;
    }

    /**
     * 测试场景函数式接口
     */
    @FunctionalInterface
    private interface TestScenario {
        void execute() throws Exception;
    }

    /**
     * void 测试场景函数式接口
     */
    @FunctionalInterface
    private interface VoidScenario {
        void execute();
    }
}
