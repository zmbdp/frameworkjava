package com.zmbdp.common.log.strategy.impl;

import com.zmbdp.common.log.strategy.ILogProcessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 日志条件评估策略
 * <p>
 * 实现 {@link ILogProcessStrategy} 接口，提供基于 SpEL 表达式的条件评估功能。<br>
 * 用于判断是否应该记录日志，支持复杂的条件表达式。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>条件评估：使用 SpEL 表达式评估是否应记录日志</li>
 *     <li>表达式执行：执行 SpEL 表达式并返回结果</li>
 *     <li>上下文构建：构建包含方法参数和返回值的 SpEL 上下文</li>
 *     <li>异常容错处理：表达式执行失败时默认记录日志</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private LogConditionEvaluateStrategy conditionEvaluateStrategy;
 *
 * // 评估条件表达式
 * boolean shouldRecord = conditionEvaluateStrategy.shouldRecord(joinPoint, "#result.code == 200", result);
 *
 * // 执行 SpEL 表达式
 * Object value = conditionEvaluateStrategy.evaluateExpression(joinPoint, "#user.name", null);
 * }</pre>
 * <p>
 * <b>SpEL 表达式支持：</b>
 * <ul>
 *     <li>访问方法参数：{@code #paramName}（如 {@code #userId}）</li>
 *     <li>访问参数属性：{@code #paramName.property}（如 {@code #user.name}）</li>
 *     <li>访问返回值：{@code #result}（如 {@code #result.code}）</li>
 *     <li>访问所有参数：{@code #args[0]}、{@code #args[1]}（按索引访问）</li>
 *     <li>逻辑运算：{@code #result.code == 200 && #result.data != null}</li>
 *     <li>方法调用：{@code #user.getName().length() > 0}</li>
 * </ul>
 * <p>
 * <b>条件表达式示例：</b>
 * <pre>{@code
 * // 仅记录成功的操作
 * @LogAction(value = "查询用户", condition = "#result.code == 200")
 *
 * // 仅记录特定用户的操作
 * @LogAction(value = "更新用户", condition = "#userId == 1")
 *
 * // 仅记录返回值不为空的操作
 * @LogAction(value = "查询列表", condition = "#result.data != null && #result.data.size() > 0")
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>表达式执行失败时，默认返回 true（记录日志）</li>
 *     <li>表达式返回非 Boolean 类型时，判断是否为 null（非 null 返回 true）</li>
 *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
 *     <li>建议使用简单的表达式，避免复杂的逻辑</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogProcessStrategy
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 */
@Slf4j
@Component
public class LogConditionEvaluateStrategy implements ILogProcessStrategy {

    /**
     * SpEL 表达式解析器
     * <p>
     * 用于解析和执行 SpEL 表达式。
     */
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * 参数名称发现器
     * <p>
     * 用于获取方法参数名称，支持从字节码中读取参数名。
     */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public String getType() {
        return "CONDITION_EVALUATE";
    }

    /**
     * 评估条件表达式
     * <p>
     * 使用 SpEL 表达式评估是否应该记录日志。<br>
     * 表达式执行失败时默认返回 true（记录日志）。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>调用 {@link #evaluateExpression(ProceedingJoinPoint, String, Object)} 执行表达式</li>
     *     <li>如果返回值是 Boolean 类型，直接返回</li>
     *     <li>如果返回值不是 Boolean 类型，判断是否为 null（非 null 返回 true）</li>
     *     <li>如果表达式执行失败，记录警告日志并返回 true</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 仅记录成功的操作
     * boolean shouldRecord = conditionEvaluateStrategy.shouldRecord(joinPoint, "#result.code == 200", result);
     *
     * // 仅记录特定用户的操作
     * boolean shouldRecord = conditionEvaluateStrategy.shouldRecord(joinPoint, "#userId == 1", null);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 condition 为 null 或空字符串，应在调用前判断</li>
     *     <li>表达式执行失败时，默认返回 true（记录日志）</li>
     *     <li>表达式返回非 Boolean 类型时，判断是否为 null</li>
     *     <li>异常会被记录到日志中，但不会影响日志记录流程</li>
     * </ul>
     *
     * @param joinPoint 连接点，不能为 null
     * @param condition 条件表达式（SpEL），不能为空
     * @param result    方法返回值（可为 null）
     * @return true 表示应记录日志，false 表示不应记录
     * @see #evaluateExpression(ProceedingJoinPoint, String, Object)
     */
    public boolean shouldRecord(ProceedingJoinPoint joinPoint, String condition, Object result) {
        try {
            // 执行 SpEL 表达式
            Object value = evaluateExpression(joinPoint, condition, result);
            // 如果返回值是 Boolean 类型，直接返回
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            // 如果返回值不是 Boolean 类型，判断是否为 null（非 null 返回 true）
            return value != null;
        } catch (Exception e) {
            log.warn("执行条件表达式失败: {}, 默认返回 true", condition, e);
            // 表达式执行失败，默认记录日志
            return true;
        }
    }

    /**
     * 执行 SpEL 表达式
     * <p>
     * 构建包含方法参数和返回值的 SpEL 上下文，执行表达式并返回结果。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>调用 {@link #buildContext(ProceedingJoinPoint, Object)} 构建 SpEL 上下文</li>
     *     <li>解析 SpEL 表达式</li>
     *     <li>在上下文中执行表达式</li>
     *     <li>返回执行结果</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 提取参数属性
     * Object userName = conditionEvaluateStrategy.evaluateExpression(joinPoint, "#user.name", null);
     *
     * // 提取返回值属性
     * Object code = conditionEvaluateStrategy.evaluateExpression(joinPoint, "#result.code", result);
     *
     * // 执行复杂表达式
     * Object value = conditionEvaluateStrategy.evaluateExpression(joinPoint, "#user.name + '-' + #user.age", null);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 expression 为 null 或空字符串，可能抛出异常</li>
     *     <li>如果表达式语法错误，会抛出 ExpressionException</li>
     *     <li>如果表达式访问不存在的属性，会抛出 SpelEvaluationException</li>
     *     <li>调用方应该捕获异常并进行适当处理</li>
     * </ul>
     *
     * @param joinPoint  连接点，不能为 null
     * @param expression SpEL 表达式，不能为空
     * @param result     方法返回值（可为 null）
     * @return 表达式执行结果，可能为 null
     * @throws org.springframework.expression.ExpressionException 如果表达式语法错误或执行失败
     * @see #buildContext(ProceedingJoinPoint, Object)
     */
    public Object evaluateExpression(ProceedingJoinPoint joinPoint, String expression, Object result) {
        // 构建 SpEL 上下文（包含方法参数和返回值）
        EvaluationContext context = buildContext(joinPoint, result);
        // 解析 SpEL 表达式
        Expression expr = expressionParser.parseExpression(expression);
        // 在上下文中执行表达式并返回结果
        return expr.getValue(context);
    }

    /**
     * 构建 SpEL 上下文
     * <p>
     * 构建包含方法参数和返回值的 SpEL 评估上下文。<br>
     * 上下文中包含所有方法参数（按参数名访问）、参数数组（#args）和返回值（#result）。
     * <p>
     * <b>上下文变量：</b>
     * <ul>
     *     <li><b>#paramName</b>：方法参数（如 #userId、#user）</li>
     *     <li><b>#args</b>：参数数组（如 #args[0]、#args[1]）</li>
     *     <li><b>#result</b>：方法返回值（仅在返回值不为 null 时可用）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 构建上下文
     * EvaluationContext context = buildContext(joinPoint, result);
     *
     * // 访问参数
     * Object userId = context.lookupVariable("userId");
     *
     * // 访问参数数组
     * Object[] args = (Object[]) context.lookupVariable("args");
     *
     * // 访问返回值
     * Object result = context.lookupVariable("result");
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果方法没有参数，上下文中只包含 #args（空数组）</li>
     *     <li>如果返回值为 null，上下文中不包含 #result 变量</li>
     *     <li>参数名称从字节码中读取，需要编译时保留参数名（-parameters）</li>
     * </ul>
     *
     * @param joinPoint 连接点，不能为 null
     * @param result    方法返回值（可为 null）
     * @return SpEL 评估上下文
     * @see org.springframework.expression.spel.support.StandardEvaluationContext
     */
    private EvaluationContext buildContext(ProceedingJoinPoint joinPoint, Object result) {
        org.aspectj.lang.reflect.MethodSignature signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取方法参数名称
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        // 获取方法参数值
        Object[] args = joinPoint.getArgs();

        // 创建 SpEL 上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 将方法参数添加到上下文中（按参数名访问，如 #userId、#user）
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        // 将参数数组添加到上下文中（按索引访问，如 #args[0]、#args[1]）
        context.setVariable("args", args);
        // 将返回值添加到上下文中（访问方式：#result）
        if (result != null) {
            context.setVariable("result", result);
        }
        return context;
    }
}