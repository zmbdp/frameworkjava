package com.zmbdp.common.idempotent.enums;

/**
 * 幂等性模式枚举
 * <p>
 * 用于控制幂等性的行为模式，支持三态设计：
 * <ul>
 *     <li><b>DEFAULT</b>：使用全局配置（Nacos 或 application.yml 中的 {@code idempotent.return-cached-result}）</li>
 *     <li><b>TRUE</b>：强制开启强幂等模式（返回缓存结果），即使全局配置为 false</li>
 *     <li><b>FALSE</b>：强制关闭强幂等模式（防重模式，直接报错），即使全局配置为 true</li>
 * </ul>
 * </p>
 * <p>
 * 优先级：注解显式指定（TRUE/FALSE） > 全局配置 > 默认值（false，即防重模式）
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 * // 1. 使用全局配置（如果全局配置为 true，则开启强幂等；如果为 false，则防重模式）
 * &#64;Idempotent
 * public Result&lt;String&gt; method1() {
 *     return Result.success("结果");
 * }
 *
 * // 2. 强制开启强幂等模式（即使全局配置为 false）
 * &#64;Idempotent(returnCachedResult = IdempotentMode.TRUE)
 * public Result&lt;String&gt; method2() {
 *     return Result.success("结果");
 * }
 *
 * // 3. 强制关闭强幂等模式（即使全局配置为 true）
 * &#64;Idempotent(returnCachedResult = IdempotentMode.FALSE)
 * public Result&lt;String&gt; method3() {
 *     return Result.success("结果");
 * }
 * </pre>
 * </p>
 *
 * @author 稚名不带撇
 * @since 1.0.0
 */
public enum IdempotentMode {

    /**
     * 使用全局配置
     * <p>
     * 如果全局配置 {@code idempotent.return-cached-result} 为 true，则开启强幂等模式；
     * 如果为 false 或未配置，则使用防重模式。
     * </p>
     */
    DEFAULT,

    /**
     * 强制开启强幂等模式
     * <p>
     * 重复请求返回第一次的结果，即使全局配置为 false。
     * </p>
     */
    TRUE,

    /**
     * 强制关闭强幂等模式
     * <p>
     * 重复请求直接报错，即使全局配置为 true。
     * </p>
     */
    FALSE
}
