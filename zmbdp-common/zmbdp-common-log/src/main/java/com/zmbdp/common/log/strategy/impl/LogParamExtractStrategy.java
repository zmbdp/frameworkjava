package com.zmbdp.common.log.strategy.impl;

import com.zmbdp.common.core.utils.DesensitizeUtil;
import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.strategy.ILogProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志参数提取策略
 * <p>
 * 实现 {@link ILogProcessStrategy} 接口，提供方法参数和返回值的提取功能。<br>
 * 支持 SpEL 表达式和敏感字段脱敏处理。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>提取方法参数：支持完整参数对象和 SpEL 表达式提取</li>
 *     <li>提取方法返回值：支持完整返回值对象和 SpEL 表达式提取</li>
 *     <li>敏感字段脱敏：支持手机号、身份证、邮箱、密码、银行卡等敏感信息脱敏</li>
 *     <li>异常容错处理：提取失败时不影响日志记录</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private LogParamExtractStrategy paramExtractStrategy;
 *
 * // 提取方法参数
 * String params = paramExtractStrategy.extractParams(joinPoint, logAction);
 *
 * // 提取方法返回值
 * String result = paramExtractStrategy.extractResult(result, logAction, joinPoint);
 * }</pre>
 * <p>
 * <b>SpEL 表达式支持：</b>
 * <ul>
 *     <li>参数表达式：{@code @LogAction(paramsExpression = "#user.name")}</li>
 *     <li>返回值表达式：{@code @LogAction(resultExpression = "#result.data")}</li>
 *     <li>表达式执行失败时，使用完整对象</li>
 * </ul>
 * <p>
 * <b>敏感字段脱敏：</b>
 * <ul>
 *     <li>手机号：{@code @LogAction(desensitizeFields = "phone,mobile")}</li>
 *     <li>身份证：{@code @LogAction(desensitizeFields = "idCard,id_card")}</li>
 *     <li>邮箱：{@code @LogAction(desensitizeFields = "email")}</li>
 *     <li>密码：{@code @LogAction(desensitizeFields = "password,pwd")}</li>
 *     <li>银行卡：{@code @LogAction(desensitizeFields = "bankCard,bank_card")}</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
 *     <li>SpEL 表达式执行失败时，使用完整对象</li>
 *     <li>脱敏仅支持 Map 类型的参数对象</li>
 *     <li>脱敏字段名不区分大小写</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogProcessStrategy
 * @see DesensitizeUtil
 */
@Slf4j
@Component
public class LogParamExtractStrategy implements ILogProcessStrategy {

    /**
     * 参数名称发现器
     * <p>
     * 用于获取方法参数名称，支持从字节码中读取参数名。
     */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 条件评估策略
     * <p>
     * 用于执行 SpEL 表达式，提取参数和返回值。
     */
    @Autowired
    private LogConditionEvaluateStrategy conditionEvaluateStrategy;

    @Override
    public String getType() {
        return "PARAM_EXTRACT";
    }

    /**
     * 提取方法参数
     * <p>
     * 根据注解配置提取方法参数，支持 SpEL 表达式和敏感字段脱敏。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>检查是否配置了参数表达式（paramsExpression）</li>
     *     <li>如果配置了表达式，执行 SpEL 表达式提取参数</li>
     *     <li>如果表达式执行失败，使用完整参数对象</li>
     *     <li>获取方法参数名称和参数值</li>
     *     <li>构建参数 Map（参数名 → 参数值）</li>
     *     <li>如果配置了脱敏字段，对敏感字段进行脱敏处理</li>
     *     <li>将参数 Map 转换为 JSON 字符串返回</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 提取完整参数对象
     * @LogAction(value = "用户登录", recordParams = true)
     * public Result login(LoginDTO loginDTO) { ... }
     * // 提取结果：{"loginDTO": {"username": "admin", "password": "******"}}
     *
     * // 使用 SpEL 表达式提取部分参数
     * @LogAction(value = "用户登录", recordParams = true, paramsExpression = "#loginDTO.username")
     * public Result login(LoginDTO loginDTO) { ... }
     * // 提取结果："admin"
     *
     * // 敏感字段脱敏
     * @LogAction(value = "用户注册", recordParams = true, desensitizeFields = "phone,password")
     * public Result register(RegisterDTO registerDTO) { ... }
     * // 提取结果：{"registerDTO": {"phone": "138****8000", "password": "******"}}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 joinPoint 或 logAction 为 null，可能抛出 NullPointerException</li>
     *     <li>SpEL 表达式执行失败时，会记录警告日志并使用完整参数对象</li>
     *     <li>脱敏仅支持 Map 类型的参数对象</li>
     *     <li>脱敏字段名不区分大小写</li>
     *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
     * </ul>
     *
     * @param joinPoint 连接点，不能为 null
     * @param logAction 日志注解，不能为 null
     * @return 参数 JSON 字符串，如果提取失败返回空 Map 的 JSON
     * @see LogAction#paramsExpression()
     * @see LogAction#desensitizeFields()
     */
    public String extractParams(ProceedingJoinPoint joinPoint, LogAction logAction) {
        // 如果配置了参数表达式，执行 SpEL 表达式提取参数
        if (StringUtil.isNotEmpty(logAction.paramsExpression())) {
            try {
                // 执行 SpEL 表达式
                Object paramsObj = conditionEvaluateStrategy.evaluateExpression(joinPoint, logAction.paramsExpression(), null);
                if (paramsObj != null) {
                    // 将提取的参数对象转换为 JSON
                    return JsonUtil.classToJson(paramsObj);
                }
            } catch (Exception e) {
                log.warn("执行参数表达式失败: {}, 使用完整参数对象", e.getMessage());
            }
        }

        // 获取方法参数数组
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取方法参数名称
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());

        // 构建参数 Map（参数名 → 参数值）
        Map<String, Object> paramsMap = new HashMap<>();
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                Object arg = args[i];
                // 如果配置了脱敏字段，对敏感字段进行脱敏处理
                if (arg != null && StringUtil.isNotEmpty(logAction.desensitizeFields())) {
                    arg = desensitizeObject(arg, logAction.desensitizeFields());
                }
                // 将参数名和参数值放入 Map
                paramsMap.put(parameterNames[i], arg);
            }
        }
        // 将参数 Map 转换为 JSON 字符串
        return JsonUtil.classToJson(paramsMap);
    }

    /**
     * 提取方法返回值
     * <p>
     * 根据注解配置提取方法返回值，支持 SpEL 表达式。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>检查是否配置了返回值表达式（resultExpression）</li>
     *     <li>如果配置了表达式且返回值不为 null，执行 SpEL 表达式提取返回值</li>
     *     <li>如果表达式执行失败，使用完整返回值对象</li>
     *     <li>将返回值转换为 JSON 字符串返回</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 提取完整返回值对象
     * @LogAction(value = "查询用户", recordResult = true)
     * public Result<UserVO> getUser(Long userId) { ... }
     * // 提取结果：{"code": 200, "data": {"id": 1, "name": "admin"}}
     *
     * // 使用 SpEL 表达式提取部分返回值
     * @LogAction(value = "查询用户", recordResult = true, resultExpression = "#result.data")
     * public Result<UserVO> getUser(Long userId) { ... }
     * // 提取结果：{"id": 1, "name": "admin"}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 result 为 null，直接返回 null 的 JSON</li>
     *     <li>SpEL 表达式执行失败时，会记录警告日志并使用完整返回值对象</li>
     *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
     * </ul>
     *
     * @param result    方法返回值，可以为 null
     * @param logAction 日志注解，不能为 null
     * @param joinPoint 连接点，不能为 null
     * @return 返回值 JSON 字符串，如果返回值为 null 返回 null 的 JSON
     * @see LogAction#resultExpression()
     */
    public String extractResult(Object result, LogAction logAction, ProceedingJoinPoint joinPoint) {
        // 如果配置了返回值表达式，执行 SpEL 表达式提取返回值
        if (StringUtil.isNotEmpty(logAction.resultExpression()) && result != null) {
            try {
                // 执行 SpEL 表达式
                Object resultObj = conditionEvaluateStrategy.evaluateExpression(joinPoint, logAction.resultExpression(), result);
                if (resultObj != null) {
                    // 将提取的返回值对象转换为 JSON
                    return JsonUtil.classToJson(resultObj);
                }
            } catch (Exception e) {
                log.warn("执行返回值表达式失败: {}, 使用完整返回值对象", e.getMessage());
            }
        }
        // 将完整返回值对象转换为 JSON
        return JsonUtil.classToJson(result);
    }

    /**
     * 对对象进行脱敏处理
     * <p>
     * 根据配置的脱敏字段，对敏感信息进行脱敏处理。<br>
     * 目前仅支持 Map 类型的对象。
     * <p>
     * <b>支持的脱敏字段：</b>
     * <ul>
     *     <li>手机号：phone、mobile</li>
     *     <li>身份证：idCard、id_card</li>
     *     <li>邮箱：email</li>
     *     <li>密码：password、pwd</li>
     *     <li>银行卡：bankCard、bank_card</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>仅支持 Map 类型的对象</li>
     *     <li>脱敏字段名不区分大小写</li>
     *     <li>脱敏失败时返回原对象</li>
     * </ul>
     *
     * @param obj               待脱敏对象，可以为 null
     * @param desensitizeFields 脱敏字段列表（逗号分隔），不能为空
     * @return 脱敏后的对象，如果脱敏失败返回原对象
     */
    @SuppressWarnings("unchecked")
    private Object desensitizeObject(Object obj, String desensitizeFields) {
        if (obj == null || StringUtil.isEmpty(desensitizeFields)) {
            return obj;
        }
        try {
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                // 按逗号分隔脱敏字段列表
                for (String field : desensitizeFields.split(CommonConstants.COMMA_SEPARATOR)) {
                    field = field.trim();
                    // 获取字段值
                    Object value = map.get(field);
                    if (value instanceof String) {
                        // 根据字段名选择脱敏方式并替换原值
                        map.put(field, desensitizeByFieldName(field, (String) value));
                    }
                }
                return map;
            }
            return obj;
        } catch (Exception e) {
            log.warn("脱敏处理失败: {}", e.getMessage());
            return obj;
        }
    }

    /**
     * 根据字段名称选择脱敏方式
     * <p>
     * 根据字段名称判断敏感信息类型，选择对应的脱敏方式。
     * <p>
     * <b>脱敏规则：</b>
     * <ul>
     *     <li>手机号（phone、mobile）：保留前 3 位和后 4 位，中间用 * 代替</li>
     *     <li>身份证（idCard、id_card）：保留前 6 位和后 4 位，中间用 * 代替</li>
     *     <li>邮箱（email）：保留邮箱前缀第 1 位和 @ 后的域名，中间用 * 代替</li>
     *     <li>密码（password、pwd）：全部用 * 代替</li>
     *     <li>银行卡（bankCard、bank_card）：保留前 4 位和后 4 位，中间用 * 代替</li>
     * </ul>
     *
     * @param fieldName 字段名称，不能为空
     * @param value     字段值，可以为空
     * @return 脱敏后的字符串，如果字段值为空返回原值
     */
    private String desensitizeByFieldName(String fieldName, String value) {
        if (StringUtil.isEmpty(value)) {
            return value;
        }
        // 将字段名转换为小写，便于匹配
        String lower = fieldName.toLowerCase();
        // 判断字段类型并选择对应的脱敏方式
        if (lower.contains("phone") || lower.contains("mobile")) {
            // 手机号脱敏：保留前 3 位和后 4 位
            return DesensitizeUtil.desensitizePhone(value);
        } else if (lower.contains("idcard") || lower.contains("id_card")) {
            // 身份证脱敏：保留前 6 位和后 4 位
            return DesensitizeUtil.desensitizeIdCard(value);
        } else if (lower.contains("email")) {
            // 邮箱脱敏：保留邮箱前缀第 1 位和 @ 后的域名
            return DesensitizeUtil.desensitizeEmail(value);
        } else if (lower.contains("password") || lower.contains("pwd")) {
            // 密码脱敏：全部替换为 *
            return DesensitizeUtil.desensitizePassword(value);
        } else if (lower.contains("bankcard") || lower.contains("bank_card")) {
            // 银行卡脱敏：保留前 4 位和后 4 位
            return DesensitizeUtil.desensitizeBankCard(value);
        }
        // 不匹配任何类型，返回原值
        return value;
    }
}

