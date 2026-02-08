package com.zmbdp.common.log.strategy.impl;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.LogConstants;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.strategy.ILogProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 日志注解合并策略
 * <p>
 * 实现 {@link ILogProcessStrategy} 接口，提供日志注解的合并功能。<br>
 * 合并方法注解、类注解和 Nacos 全局配置，优先级：方法注解 > 类注解 > 全局默认。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>注解合并：合并方法注解、类注解和全局配置</li>
 *     <li>优先级控制：方法注解 > 类注解 > Nacos 全局配置 > 默认值</li>
 *     <li>动态配置支持：支持从 Nacos 读取全局默认配置</li>
 *     <li>代理模式实现：返回动态代理对象，按需计算配置值</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private LogAnnotationMergeStrategy annotationMergeStrategy;
 *
 * // 合并方法注解和类注解
 * LogAction mergedAction = annotationMergeStrategy.merge(methodLogAction, classLogAction);
 *
 * // 获取合并后的配置
 * boolean recordParams = mergedAction.recordParams();
 * boolean recordResult = mergedAction.recordResult();
 * }</pre>
 * <p>
 * <b>合并规则：</b>
 * <ul>
 *     <li><b>value</b>：使用方法注解的值（必填）</li>
 *     <li><b>recordParams</b>：方法注解 true → true，否则类注解 true → true，否则全局配置</li>
 *     <li><b>recordResult</b>：方法注解 true → true，否则类注解 true → true，否则全局配置</li>
 *     <li><b>recordException</b>：方法注解 false → false，否则类注解 false → false，否则全局配置</li>
 *     <li><b>throwException</b>：方法注解 false → false，否则类注解 false → false，否则全局配置</li>
 *     <li><b>condition</b>：方法注解非空 → 方法注解，否则类注解</li>
 *     <li><b>paramsExpression</b>：方法注解非空 → 方法注解，否则类注解</li>
 *     <li><b>resultExpression</b>：方法注解非空 → 方法注解，否则类注解</li>
 *     <li><b>module</b>：方法注解非空 → 方法注解，否则类注解</li>
 *     <li><b>businessType</b>：方法注解非空 → 方法注解，否则类注解</li>
 *     <li><b>desensitizeFields</b>：方法注解非空 → 方法注解，否则类注解</li>
 *     <li><b>storageType</b>：方法注解非空 → 方法注解，否则类注解</li>
 * </ul>
 * <p>
 * <b>Nacos 全局配置：</b>
 * <ul>
 *     <li>log.default.record-params：全局默认是否记录参数（默认 false）</li>
 *     <li>log.default.record-result：全局默认是否记录返回值（默认 false）</li>
 *     <li>log.default.record-exception：全局默认是否记录异常（默认 true）</li>
 *     <li>log.default.throw-exception：全局默认是否抛出异常（默认 true）</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>方法注解不能为 null，类注解可以为 null</li>
 *     <li>返回的是动态代理对象，不是真实的注解对象</li>
 *     <li>配置值按需计算，支持动态更新</li>
 *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogProcessStrategy
 * @see LogAction
 * @see LogConstants
 */
@Slf4j
@Component
public class LogAnnotationMergeStrategy implements ILogProcessStrategy {

    /**
     * Spring 环境对象
     * <p>
     * 用于从配置中心（Nacos）读取全局默认配置。
     */
    @Autowired
    private Environment environment;

    @Override
    public String getType() {
        return "ANNOTATION_MERGE";
    }

    /**
     * 合并方法注解、类注解和全局配置
     * <p>
     * 按优先级合并方法注解、类注解和 Nacos 全局配置，返回动态代理对象。<br>
     * 优先级：方法注解 > 类注解 > Nacos 全局配置 > 默认值。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>创建 LogAction 动态代理对象</li>
     *     <li>实现 annotationType() 方法返回 LogAction.class</li>
     *     <li>实现各个配置方法，按优先级返回配置值</li>
     *     <li>返回代理对象</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 方法注解
     * @LogAction(value = "用户登录", recordParams = true)
     * public Result login(LoginDTO loginDTO) { ... }
     *
     * // 类注解
     * @LogAction(module = "用户模块", recordResult = true)
     * public class UserService { ... }
     *
     * // 合并注解
     * LogAction mergedAction = annotationMergeStrategy.merge(methodLogAction, classLogAction);
     * // mergedAction.value() = "用户登录"（方法注解）
     * // mergedAction.module() = "用户模块"（类注解）
     * // mergedAction.recordParams() = true（方法注解）
     * // mergedAction.recordResult() = true（类注解）
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 methodLogAction 为 null，可能抛出 NullPointerException</li>
     *     <li>classLogAction 可以为 null，表示没有类注解</li>
     *     <li>返回的是动态代理对象，不是真实的注解对象</li>
     *     <li>配置值按需计算，支持动态更新</li>
     * </ul>
     *
     * @param methodLogAction 方法注解（必不为 null）
     * @param classLogAction  类注解（可为 null）
     * @return 合并后的注解代理对象
     * @see LogAction
     */
    public LogAction merge(LogAction methodLogAction, LogAction classLogAction) {
        // 创建动态代理对象，实现 LogAction 接口
        return new LogAction() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LogAction.class;
            }

            @Override
            public String value() {
                // 操作描述：直接使用方法注解的值（必填）
                return methodLogAction.value();
            }

            @Override
            public boolean recordParams() {
                // 是否记录参数：方法注解 true → true，否则类注解 true → true，否则全局配置
                if (methodLogAction.recordParams()) return true;
                if (classLogAction != null && classLogAction.recordParams()) return true;
                return getBool(LogConstants.NACOS_LOG_DEFAULT_RECORD_PARAMS_PREFIX, LogConstants.DEFAULT_RECORD_PARAMS);
            }

            @Override
            public boolean recordResult() {
                // 是否记录返回值：方法注解 true → true，否则类注解 true → true，否则全局配置
                if (methodLogAction.recordResult()) return true;
                if (classLogAction != null && classLogAction.recordResult()) return true;
                return getBool(LogConstants.NACOS_LOG_DEFAULT_RECORD_RESULT_PREFIX, LogConstants.DEFAULT_RECORD_RESULT);
            }

            @Override
            public boolean recordException() {
                // 是否记录异常：方法注解 false → false，否则类注解 false → false，否则全局配置
                if (!methodLogAction.recordException()) return false;
                if (classLogAction != null && !classLogAction.recordException()) return false;
                return getBool(LogConstants.NACOS_LOG_DEFAULT_RECORD_EXCEPTION_PREFIX, LogConstants.DEFAULT_RECORD_EXCEPTION);
            }

            @Override
            public boolean throwException() {
                // 是否抛出异常：方法注解 false → false，否则类注解 false → false，否则全局配置
                if (!methodLogAction.throwException()) return false;
                if (classLogAction != null && !classLogAction.throwException()) return false;
                return getBool(LogConstants.NACOS_LOG_DEFAULT_THROW_EXCEPTION_PREFIX, LogConstants.DEFAULT_THROW_EXCEPTION);
            }

            @Override
            public String condition() {
                // 条件表达式：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.condition()) ? methodLogAction.condition() : (classLogAction != null ? classLogAction.condition() : "");
            }

            @Override
            public String paramsExpression() {
                // 参数表达式：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.paramsExpression()) ? methodLogAction.paramsExpression() : (classLogAction != null ? classLogAction.paramsExpression() : "");
            }

            @Override
            public String resultExpression() {
                // 返回值表达式：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.resultExpression()) ? methodLogAction.resultExpression() : (classLogAction != null ? classLogAction.resultExpression() : "");
            }

            @Override
            public String module() {
                // 模块名称：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.module()) ? methodLogAction.module() : (classLogAction != null ? classLogAction.module() : "");
            }

            @Override
            public String businessType() {
                // 业务类型：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.businessType()) ? methodLogAction.businessType() : (classLogAction != null ? classLogAction.businessType() : "");
            }

            @Override
            public String desensitizeFields() {
                // 脱敏字段：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.desensitizeFields()) ? methodLogAction.desensitizeFields() : (classLogAction != null ? classLogAction.desensitizeFields() : "");
            }

            @Override
            public String storageType() {
                // 存储类型：方法注解非空 → 方法注解，否则类注解
                return StringUtil.isNotEmpty(methodLogAction.storageType()) ? methodLogAction.storageType() : (classLogAction != null ? classLogAction.storageType() : "");
            }

            /**
             * 从配置中心读取布尔值配置
             *
             * @param key          配置键
             * @param defaultValue 默认值
             * @return 配置值，如果不存在则返回默认值
             */
            private boolean getBool(String key, boolean defaultValue) {
                Boolean v = environment.getProperty(key, Boolean.class);
                return v != null ? v : defaultValue;
            }
        };
    }
}