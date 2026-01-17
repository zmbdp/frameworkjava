package com.zmbdp.common.idempotent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解<br>
 * 用于标记需要保证幂等性的接口方法，防止重复提交和重复请求
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>标注在方法上，用于标记需要保证幂等性的接口</li>
 *     <li>支持 HTTP 请求和 MQ 消费者两种场景</li>
 *     <li>基于 Redis 实现分布式幂等性控制</li>
 *     <li>支持防重模式和强幂等模式两种模式</li>
 *     <li>支持从请求头、请求参数、SpEL 表达式、MQ 消息头等多种方式获取 Token</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>在需要保证幂等性的方法上添加 {@code @Idempotent} 注解</li>
 *     <li>客户端在请求时携带幂等性 Token（请求头或参数）</li>
 *     <li>服务端自动校验 Token，防止重复请求</li>
 *     <li>可通过配置文件 {@code idempotent.return-cached-result} 全局控制强幂等模式</li>
 * </ol>
 *
 * <p>
 * HTTP 请求示例：
 * <pre>
 * // 1. 基础用法（从请求头获取 Token）
 * &#64;PostMapping("/createOrder")
 * &#64;Idempotent
 * public Result&lt;String&gt; createOrder() {
 *     // 客户端请求头：Idempotent-Token: token-123456
 *     // 第一次请求：执行方法
 *     // 第二次请求：返回错误"请勿重复提交"
 *     return Result.success("订单创建成功");
 * }
 *
 * // 2. 自定义请求头名称
 * &#64;PostMapping("/pay")
 * &#64;Idempotent(headerName = "X-Idempotency-Key", message = "订单已处理，请勿重复支付")
 * public Result&lt;String&gt; pay() {
 *     // 客户端请求头：X-Idempotency-Key: pay-token-789
 *     return Result.success("支付成功");
 * }
 *
 * // 3. 从请求参数获取 Token
 * &#64;PostMapping("/submit")
 * &#64;Idempotent(allowParam = true, paramName = "token")
 * public Result&lt;String&gt; submit(@RequestParam String token) {
 *     // 客户端请求：POST /submit?token=token-abc
 *     return Result.success("提交成功");
 * }
 *
 * // 4. 强幂等模式（返回缓存结果）
 * &#64;GetMapping("/getOrderInfo")
 * &#64;Idempotent(returnCachedResult = true)
 * public Result&lt;OrderInfo&gt; getOrderInfo() {
 *     // 第一次请求：执行方法并缓存结果
 *     // 第二次请求：返回第一次的结果，不执行方法
 *     return Result.success(orderInfo);
 * }
 * </pre>
 *
 * <p>
 * MQ 消费者示例：
 * <pre>
 * // 1. 从消息对象字段获取 Token（SpEL 表达式）
 * &#64;RabbitListener(queues = "order.queue")
 * &#64;Idempotent(tokenExpression = "#messageDTO.idempotentToken")
 * public void handleOrder(MessageDTO messageDTO) {
 *     // MessageDTO 对象需要有 idempotentToken 字段
 *     // 第一次消费：处理消息
 *     // 第二次消费：拒绝处理（防重模式）或返回缓存结果（强幂等模式）
 * }
 *
 * // 2. 从消息头获取 Token
 * &#64;RabbitListener(queues = "payment.queue")
 * &#64;Idempotent(headerName = "Idempotent-Token")
 * public void handlePayment(MessageDTO messageDTO) {
 *     // 发送消息时在消息头设置：Idempotent-Token: token-xyz
 * }
 * </pre>
 *
 * <p>
 * 全局配置（Nacos 或 application.yml）：
 * <pre>
 * # 全局启用强幂等模式
 * idempotent:
 *   return-cached-result: true
 * </pre>
 *
 * <p>
 * Token 获取优先级：
 * <ol>
 *     <li>SpEL 表达式（{@code tokenExpression}）</li>
 *     <li>HTTP 请求头（{@code headerName}）</li>
 *     <li>HTTP 请求参数（{@code paramName}，需 {@code allowParam = true}）</li>
 *     <li>RabbitMQ 消息头（{@code headerName}）</li>
 * </ol>
 *
 * <p>
 * 注意事项：
 * <ul>
 *     <li>Token 必须唯一，建议使用 UUID 或业务唯一标识</li>
 *     <li>Token 过期时间建议根据业务场景设置，默认 5 分钟</li>
 *     <li>强幂等模式会缓存方法执行结果，占用 Redis 存储空间</li>
 *     <li>方法执行失败时，Token 会被删除，允许重试</li>
 *     <li>MQ 消费者使用 SpEL 表达式时，确保消息对象包含对应字段</li>
 *     <li>配置 {@code idempotent.return-cached-result} 支持动态刷新（需添加 {@code @RefreshScope}）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等性 Token 的过期时间（秒）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>设置 Token 在 Redis 中的过期时间</li>
     *     <li>超过过期时间后，Token 自动失效，可以重新提交</li>
     *     <li>建议根据业务场景设置，如支付场景可设置 30 分钟，查询场景可设置 5 分钟</li>
     *     <li>可通过配置文件 {@code idempotent.expire-time} 全局控制</li>
     *     <li>优先级：注解值 > 全局配置 > 默认值（300秒）</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * // 1. 注解配置（优先级最高）
     * &#64;Idempotent(expireTime = 1800)  // 30 分钟
     * public Result&lt;String&gt; createOrder() {
     *     return Result.success("订单创建成功");
     * }
     *
     * // 2. 全局配置（Nacos 或 application.yml）
     * // 配置：idempotent.expire-time=1800
     * &#64;Idempotent  // 使用全局配置的 1800 秒
     * public Result&lt;String&gt; pay() {
     *     return Result.success("支付成功");
     * }
     * </pre>
     *
     * @return 过期时间（秒），默认 300（5 分钟）
     */
    long expireTime() default 300;

    /**
     * 幂等性 Token 的请求头名称
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>HTTP 请求：从请求头中获取 Token</li>
     *     <li>RabbitMQ 消息：从消息头中获取 Token</li>
     *     <li>支持自定义请求头名称，如 "X-Idempotency-Key"、"X-Request-Id" 等</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * // HTTP 请求
     * &#64;Idempotent(headerName = "X-Idempotency-Key")
     * public Result&lt;String&gt; createOrder() {
     *     // 客户端请求头：X-Idempotency-Key: token-123
     *     return Result.success("订单创建成功");
     * }
     *
     * // RabbitMQ 消息
     * &#64;RabbitListener(queues = "order.queue")
     * &#64;Idempotent(headerName = "Idempotent-Token")
     * public void handleOrder(MessageDTO messageDTO) {
     *     // 发送消息时在消息头设置：Idempotent-Token: token-xyz
     * }
     * </pre>
     *
     * @return 请求头名称，默认 "Idempotent-Token"
     */
    String headerName() default "Idempotent-Token";

    /**
     * 重复请求时的错误提示信息
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>防重模式下，重复请求时返回的错误提示</li>
     *     <li>可以根据业务场景自定义提示信息</li>
     *     <li>强幂等模式下不会使用此提示（直接返回缓存结果）</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * &#64;Idempotent(message = "订单已处理，请勿重复支付")
     * public Result&lt;String&gt; pay() {
     *     // 重复请求时返回：订单已处理，请勿重复支付
     *     return Result.success("支付成功");
     * }
     * </pre>
     *
     * @return 错误提示信息，默认 "请勿重复提交"
     */
    String message() default "请勿重复提交";

    /**
     * 是否从请求参数中获取 Token（当请求头中没有时）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>当无法在请求头中传递 Token 时（如 GET 请求、表单提交等）</li>
     *     <li>优先级：请求头 > 请求参数（如果请求头中有 Token，则不会从参数获取）</li>
     *     <li>需要配合 {@code paramName} 使用，指定参数名称</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * &#64;PostMapping("/submit")
     * &#64;Idempotent(allowParam = true, paramName = "token")
     * public Result&lt;String&gt; submit(@RequestParam String token) {
     *     // 客户端请求：POST /submit?token=token-123
     *     // 或表单提交：token=token-123
     *     return Result.success("提交成功");
     * }
     * </pre>
     *
     * @return 是否从请求参数获取，默认 false（仅从请求头获取）
     */
    boolean allowParam() default false;

    /**
     * 请求参数名称（当 {@code allowParam} 为 true 时使用）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>指定从请求参数中获取 Token 的参数名称</li>
     *     <li>仅当 {@code allowParam = true} 时生效</li>
     *     <li>支持 GET 请求的查询参数和 POST 请求的表单参数</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * &#64;PostMapping("/submit")
     * &#64;Idempotent(allowParam = true, paramName = "idempotentToken")
     * public Result&lt;String&gt; submit() {
     *     // 客户端请求：POST /submit?idempotentToken=token-123
     *     return Result.success("提交成功");
     * }
     * </pre>
     *
     * @return 参数名称，默认 "idempotentToken"
     */
    String paramName() default "idempotentToken";

    /**
     * 从方法参数中获取 Token 的 SpEL 表达式
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>MQ 消费者等非 HTTP 请求场景</li>
     *     <li>从消息对象中动态提取 Token</li>
     *     <li>支持字段访问、方法调用、数组索引等多种表达式</li>
     *     <li>优先级最高：tokenExpression > HTTP请求头/参数 > RabbitMQ消息头</li>
     * </ul>
     *
     * <p>SpEL 表达式示例：</p>
     * <pre>
     * // 1. 从对象字段获取
     * tokenExpression = "#messageDTO.idempotentToken"
     * // MessageDTO 对象需要有 idempotentToken 字段
     *
     * // 2. 从方法参数索引获取
     * tokenExpression = "#args[0].idempotentToken"
     * // 从第一个参数的 idempotentToken 字段获取
     *
     * // 3. 调用方法获取
     * tokenExpression = "#messageDTO.getIdempotentToken()"
     * // 调用 MessageDTO 的 getIdempotentToken() 方法
     *
     * // 4. 从嵌套对象获取
     * tokenExpression = "#messageDTO.header.idempotentToken"
     * // 从嵌套对象的字段获取
     * </pre>
     *
     * <p>使用示例：</p>
     * <pre>
     * // MQ 消费者
     * &#64;RabbitListener(queues = "order.queue")
     * &#64;Idempotent(tokenExpression = "#messageDTO.idempotentToken")
     * public void handleOrder(MessageDTO messageDTO) {
     *     // MessageDTO 需要有 idempotentToken 字段
     *     // 第一次消费：处理消息
     *     // 第二次消费：拒绝处理或返回缓存结果
     * }
     * </pre>
     *
     * @return SpEL 表达式，默认空字符串（不使用表达式）
     */
    String tokenExpression() default "";

    /**
     * 是否返回缓存的结果（强幂等模式）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>控制幂等性模式：防重模式 vs 强幂等模式</li>
     *     <li>可通过配置文件 {@code idempotent.return-cached-result} 全局控制</li>
     *     <li>优先级：注解值 > 全局配置 > 默认值（false）</li>
     * </ul>
     *
     * <p>模式说明：</p>
     * <ul>
     *     <li><b>false（防重模式，默认）</b>：重复请求直接报错，适用于支付、下单等不允许重复执行的场景</li>
     *     <li><b>true（强幂等模式）</b>：重复请求返回第一次的结果，适用于需要保证多次调用返回相同结果的场景</li>
     * </ul>
     *
     * <p>防重模式行为：</p>
     * <ul>
     *     <li>第一次请求：执行方法，Token 存入 Redis</li>
     *     <li>第二次请求：直接返回错误提示（使用 {@code message} 配置的提示信息）</li>
     * </ul>
     *
     * <p>强幂等模式行为：</p>
     * <ul>
     *     <li>第一次请求：执行方法并将结果缓存到 Redis</li>
     *     <li>第二次请求：从 Redis 返回第一次的结果，不执行方法</li>
     *     <li>结果缓存时间与 Token 过期时间相同（{@code expireTime}）</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>
     * // 1. 防重模式（默认）
     * &#64;Idempotent
     * public Result&lt;String&gt; createOrder() {
     *     // 第一次请求：执行方法
     *     // 第二次请求：返回错误"请勿重复提交"
     *     return Result.success("订单创建成功");
     * }
     *
     * // 2. 强幂等模式（注解配置）
     * &#64;Idempotent(returnCachedResult = true)
     * public Result&lt;OrderInfo&gt; getOrderInfo() {
     *     // 第一次请求：执行方法并缓存结果
     *     // 第二次请求：返回第一次的结果，不执行方法
     *     return Result.success(orderInfo);
     * }
     *
     * // 3. 强幂等模式（全局配置）
     * // 在 Nacos 或 application.yml 中配置：idempotent.return-cached-result=true
     * &#64;Idempotent  // 使用全局配置
     * public Result&lt;String&gt; getData() {
     *     return Result.success("数据");
     * }
     * </pre>
     *
     * <p>注意事项：</p>
     * <ul>
     *     <li>强幂等模式会缓存方法执行结果，占用 Redis 存储空间</li>
     *     <li>方法返回类型为 void 时，强幂等模式不会缓存结果</li>
     *     <li>方法执行失败时，Token 和结果缓存都会被删除，允许重试</li>
     *     <li>全局配置支持动态刷新（需在切面类添加 {@code @RefreshScope}）</li>
     * </ul>
     *
     * @return true-强幂等模式（返回缓存结果），false-防重模式（直接报错），默认 false
     */
    boolean returnCachedResult() default false;
}
