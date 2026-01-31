package com.zmbdp.common.log.strategy;

import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.strategy.impl.LogAnnotationMergeStrategy;
import com.zmbdp.common.log.strategy.impl.LogConditionEvaluateStrategy;
import com.zmbdp.common.log.strategy.impl.LogDtoBuildStrategy;
import com.zmbdp.common.log.strategy.impl.LogParamExtractStrategy;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 日志处理策略接口
 * <p>
 * 定义日志处理的统一规范，支持多种日志处理方式（参数提取、条件评估、DTO 构建、注解合并等）。<br>
 * 每个实现类负责一种特定的日志处理逻辑。
 * <p>
 * <b>设计目的：</b>
 * <ul>
 *     <li>统一日志处理接口，屏蔽不同处理方式的实现细节</li>
 *     <li>支持多种处理方式，易于扩展</li>
 *     <li>每个实现类专注于单一职责，符合单一职责原则</li>
 * </ul>
 * <p>
 * <b>实现类：</b>
 * <ul>
 *     <li>{@link LogParamExtractStrategy}：参数提取策略（提取方法参数和返回值）</li>
 *     <li>{@link LogConditionEvaluateStrategy}：条件评估策略（评估是否应记录日志）</li>
 *     <li>{@link LogDtoBuildStrategy}：DTO 构建策略（构建日志 DTO）</li>
 *     <li>{@link LogAnnotationMergeStrategy}：注解合并策略（合并方法注解、类注解和全局配置）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 直接注入指定实现类
 * @Autowired
 * private LogParamExtractStrategy paramExtractStrategy;
 * String params = paramExtractStrategy.extractParams(joinPoint, logAction);
 *
 * @Autowired
 * private LogConditionEvaluateStrategy conditionEvaluateStrategy;
 * boolean shouldRecord = conditionEvaluateStrategy.shouldRecord(joinPoint, condition, result);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>每个实现类需要标注 {@code @Component} 注解</li>
 *     <li>实现类应该处理异常，避免影响日志记录流程</li>
 *     <li>建议直接注入具体实现类使用，不需要路由器</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see LogParamExtractStrategy
 * @see LogConditionEvaluateStrategy
 * @see LogDtoBuildStrategy
 * @see LogAnnotationMergeStrategy
 */
public interface ILogProcessStrategy {

    /**
     * 获取策略类型
     * <p>
     * 返回当前策略的类型标识，用于区分不同的策略实现。
     * <p>
     * <b>策略类型：</b>
     * <ul>
     *     <li>PARAM_EXTRACT：参数提取策略</li>
     *     <li>CONDITION_EVALUATE：条件评估策略</li>
     *     <li>DTO_BUILD：DTO 构建策略</li>
     *     <li>ANNOTATION_MERGE：注解合并策略</li>
     * </ul>
     *
     * @return 策略类型标识
     */
    String getType();
}

