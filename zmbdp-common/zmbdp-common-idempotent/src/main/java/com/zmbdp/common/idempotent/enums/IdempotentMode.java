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
 * <b>优先级：</b>注解显式指定（TRUE/FALSE） > 全局配置 > 默认值（false，即防重模式）
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 使用全局配置（如果全局配置为 true，则开启强幂等；如果为 false，则防重模式）
 * @Idempotent
 * public Result<String> method1() {
 *     return Result.success("结果");
 * }
 *
 * // 2. 强制开启强幂等模式（即使全局配置为 false）
 * @Idempotent(returnCachedResult = IdempotentMode.TRUE)
 * public Result<String> method2() {
 *     // 重复请求返回缓存结果
 *     return Result.success("结果");
 * }
 *
 * // 3. 强制关闭强幂等模式（即使全局配置为 true）
 * @Idempotent(returnCachedResult = IdempotentMode.FALSE)
 * public Result<String> method3() {
 *     // 重复请求返回错误
 *     return Result.success("结果");
 * }
 *
 * // 4. 查询接口（使用强幂等模式）
 * @GetMapping("/order/{orderId}")
 * @Idempotent(returnCachedResult = IdempotentMode.TRUE)
 * public Result<OrderInfo> getOrder(@PathVariable Long orderId) {
 *     return Result.success(orderService.getById(orderId));
 * }
 *
 * // 5. 支付接口（使用防重模式）
 * @PostMapping("/pay")
 * @Idempotent(returnCachedResult = IdempotentMode.FALSE)
 * public Result<String> pay() {
 *     return Result.success("支付成功");
 * }
 * }</pre>
 * </p>
 *
 * @author 稚名不带撇
 */
public enum IdempotentMode {

    /**
     * 使用全局配置
     * <p>
     * 如果全局配置 {@code idempotent.return-cached-result} 为 true，则开启强幂等模式；
     * 如果为 false 或未配置，则使用防重模式。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用全局配置
     * @Idempotent  // 等同于 @Idempotent(returnCachedResult = IdempotentMode.DEFAULT)
     * public Result<String> createOrder() {
     *     // 行为取决于全局配置 idempotent.return-cached-result
     *     return Result.success("订单创建成功");
     * }
     * }</pre>
     */
    DEFAULT,

    /**
     * 强制开启强幂等模式
     * <p>
     * 重复请求返回第一次的结果，即使全局配置为 false。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 强制开启强幂等模式
     * @Idempotent(returnCachedResult = IdempotentMode.TRUE)
     * public Result<OrderInfo> getOrderInfo() {
     *     // 第一次请求：执行方法并缓存结果
     *     // 第二次请求：返回第一次的结果，不执行方法
     *     return Result.success(orderInfo);
     * }
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>查询接口：需要保证多次调用返回相同结果</li>
     *     <li>计算接口：避免重复计算，提高性能</li>
     *     <li>需要保证结果一致性的场景</li>
     * </ul>
     */
    TRUE,

    /**
     * 强制关闭强幂等模式（防重模式）
     * <p>
     * 重复请求直接报错，即使全局配置为 true。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 强制关闭强幂等模式（防重模式）
     * @Idempotent(returnCachedResult = IdempotentMode.FALSE)
     * public Result<String> pay() {
     *     // 第一次请求：执行方法
     *     // 第二次请求：返回错误"请勿重复提交"
     *     return Result.success("支付成功");
     * }
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>支付接口：不允许重复支付</li>
     *     <li>下单接口：不允许重复下单</li>
     *     <li>需要明确拒绝重复请求的场景</li>
     * </ul>
     */
    FALSE
}