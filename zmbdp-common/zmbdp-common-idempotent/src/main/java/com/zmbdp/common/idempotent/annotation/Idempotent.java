package com.zmbdp.common.idempotent.annotation;

import com.zmbdp.common.idempotent.enums.IdempotentMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解
 * <p>
 * 用于标记需要保证幂等性的接口方法，防止重复提交和重复请求。
 * 基于 Redis 实现分布式幂等性控制，支持 HTTP 请求和 MQ 消费者两种场景。
 * <p>
 * <b>功能说明：</b>
 * <ul>
 *     <li>标注在方法上，用于标记需要保证幂等性的接口</li>
 *     <li>支持 HTTP 请求和 MQ 消费者两种场景</li>
 *     <li>基于 Redis 实现分布式幂等性控制</li>
 *     <li>支持防重模式和强幂等模式两种模式</li>
 *     <li>支持从请求头、请求参数、SpEL 表达式、MQ 消息头等多种方式获取 Token</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用方式：</b>
 * <ol>
 *     <li>在需要保证幂等性的方法上添加 {@code @Idempotent} 注解</li>
 *     <li>客户端在请求时携带幂等性 Token（请求头或参数）</li>
 *     <li>服务端自动校验 Token，防止重复请求</li>
 *     <li>可通过配置文件 {@code idempotent.return-cached-result} 全局控制强幂等模式</li>
 * </ol>
 * </p>
 * <p>
 * <b>HTTP 请求示例：</b>
 * <pre>{@code
 * // 1. 基础用法（从请求头获取 Token）
 * @PostMapping("/createOrder")
 * @Idempotent
 * public Result<String> createOrder() {
 *     // 客户端请求头：Idempotent-Token: token-123456
 *     // 第一次请求：执行方法
 *     // 第二次请求：返回错误"请勿重复提交"
 *     return Result.success("订单创建成功");
 * }
 *
 * // 2. 自定义请求头名称
 * @PostMapping("/pay")
 * @Idempotent(headerName = "X-Idempotency-Key", message = "订单已处理，请勿重复支付")
 * public Result<String> pay() {
 *     // 客户端请求头：X-Idempotency-Key: pay-token-789
 *     return Result.success("支付成功");
 * }
 *
 * // 3. 从请求参数获取 Token
 * @PostMapping("/submit")
 * @Idempotent(allowParam = true, paramName = "token")
 * public Result<String> submit(@RequestParam String token) {
 *     // 客户端请求：POST /submit?token=token-abc
 *     return Result.success("提交成功");
 * }
 *
 * // 4. 强幂等模式（返回缓存结果）
 * @GetMapping("/getOrderInfo")
 * @Idempotent(returnCachedResult = IdempotentMode.TRUE)
 * public Result<OrderInfo> getOrderInfo() {
 *     // 第一次请求：执行方法并缓存结果
 *     // 第二次请求：返回第一次的结果，不执行方法
 *     return Result.success(orderInfo);
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>MQ 消费者示例：</b>
 * <pre>{@code
 * // 1. 从消息对象字段获取 Token（SpEL 表达式）
 * @RabbitListener(queues = "order.queue")
 * @Idempotent(tokenExpression = "#messageDTO.idempotentToken")
 * public void handleOrder(MessageDTO messageDTO) {
 *     // MessageDTO 对象需要有 idempotentToken 字段
 *     // 第一次消费：处理消息
 *     // 第二次消费：拒绝处理（防重模式）或返回缓存结果（强幂等模式）
 * }
 *
 * // 2. 从消息头获取 Token
 * @RabbitListener(queues = "payment.queue")
 * @Idempotent(headerName = "Idempotent-Token")
 * public void handlePayment(MessageDTO messageDTO) {
 *     // 发送消息时在消息头设置：Idempotent-Token: token-xyz
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>全局配置（Nacos 或 application.yml）：</b>
 * <pre>{@code
 * # 全局启用强幂等模式
 * idempotent:
 *   return-cached-result: true
 *   expire-time: 300  # Token 过期时间（秒），默认 300
 * }</pre>
 * </p>
 *
 * <p>
 * <b>Token 获取优先级：</b>
 * <ol>
 *     <li>SpEL 表达式（{@code tokenExpression}）</li>
 *     <li>HTTP 请求头（{@code headerName}）</li>
 *     <li>HTTP 请求参数（{@code paramName}，需 {@code allowParam = true}）</li>
 *     <li>RabbitMQ 消息头（{@code headerName}）</li>
 * </ol>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>Token 必须唯一，建议使用 UUID 或业务唯一标识</li>
 *     <li>Token 过期时间建议根据业务场景设置，默认 5 分钟</li>
 *     <li>强幂等模式会缓存方法执行结果，占用 Redis 存储空间</li>
 *     <li>强幂等模式最大重试 + 等待时间为 3s，不适合高并发接口</li>
 *     <li>方法执行失败时，Token 会被删除，允许重试</li>
 *     <li>MQ 消费者使用 SpEL 表达式时，确保消息对象包含对应字段</li>
 *     <li>配置 {@code idempotent.return-cached-result} 支持动态刷新（需添加 {@code @RefreshScope}）</li>
 * </ul>
 * </p>
 *
 * @author 稚名不带撇
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等性 Token 的过期时间（秒）
     * <p>
     * 设置 Token 在 Redis 中的过期时间。超过过期时间后，Token 自动失效，可以重新提交。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. 注解配置（优先级最高）
     * @Idempotent(expireTime = 1800)  // 30 分钟
     * public Result<String> createOrder() {
     *     return Result.success("订单创建成功");
     * }
     *
     * // 2. 全局配置（Nacos 或 application.yml）
     * // 配置：idempotent.expire-time=1800
     * @Idempotent  // 使用全局配置的 1800 秒
     * public Result<String> pay() {
     *     return Result.success("支付成功");
     * }
     *
     * // 3. 支付场景（30 分钟）
     * @Idempotent(expireTime = 1800)
     * public Result<String> processPayment() {
     *     return Result.success("支付成功");
     * }
     *
     * // 4. 查询场景（5 分钟）
     * @Idempotent(expireTime = 300)
     * public Result<OrderInfo> getOrderInfo() {
     *     return Result.success(orderInfo);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>建议根据业务场景设置，如支付场景可设置 30 分钟，查询场景可设置 5 分钟</li>
     *     <li>可通过配置文件 {@code idempotent.expire-time} 全局控制</li>
     *     <li>优先级：注解值 > 全局配置 > 默认值（300秒）</li>
     *     <li>强幂等模式下，结果缓存时间与 Token 过期时间相同</li>
     * </ul>
     *
     * @return 过期时间（秒），默认 300（5 分钟）
     */
    long expireTime() default 300L;

    /**
     * 幂等性 Token 的请求头名称
     * <p>
     * 用于指定从 HTTP 请求头或 RabbitMQ 消息头中获取 Token 的字段名称。
     * 支持自定义请求头名称，如 "X-Idempotency-Key"、"X-Request-Id" 等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // HTTP 请求：自定义请求头名称
     * @Idempotent(headerName = "X-Idempotency-Key")
     * public Result<String> createOrder() {
     *     // 客户端请求头：X-Idempotency-Key: token-123
     *     return Result.success("订单创建成功");
     * }
     *
     * // HTTP 请求：使用默认请求头
     * @Idempotent
     * public Result<String> pay() {
     *     // 客户端请求头：Idempotent-Token: token-456
     *     return Result.success("支付成功");
     * }
     *
     * // RabbitMQ 消息：从消息头获取 Token
     * @RabbitListener(queues = "order.queue")
     * @Idempotent(headerName = "Idempotent-Token")
     * public void handleOrder(MessageDTO messageDTO) {
     *     // 发送消息时在消息头设置：Idempotent-Token: token-xyz
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 请求：从请求头中获取 Token</li>
     *     <li>RabbitMQ 消息：从消息头中获取 Token</li>
     *     <li>如果同时配置了 {@code tokenExpression}，SpEL 表达式优先级更高</li>
     *     <li>如果请求头中没有 Token 且 {@code allowParam = true}，会尝试从请求参数获取</li>
     * </ul>
     *
     * @return 请求头名称，默认 "Idempotent-Token"
     */
    String headerName() default "Idempotent-Token";

    /**
     * 重复请求时的错误提示信息
     * <p>
     * 防重模式下，当检测到重复请求时返回的错误提示信息。
     * 可以根据业务场景自定义提示信息，提高用户体验。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 支付场景：自定义错误提示
     * @Idempotent(message = "订单已处理，请勿重复支付")
     * public Result<String> pay() {
     *     // 重复请求时返回：订单已处理，请勿重复支付
     *     return Result.success("支付成功");
     * }
     *
     * // 下单场景：自定义错误提示
     * @Idempotent(message = "订单已创建，请勿重复提交")
     * public Result<String> createOrder() {
     *     // 重复请求时返回：订单已创建，请勿重复提交
     *     return Result.success("订单创建成功");
     * }
     *
     * // 使用默认提示
     * @Idempotent
     * public Result<String> submit() {
     *     // 重复请求时返回：请勿重复提交（默认提示）
     *     return Result.success("提交成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>仅在防重模式下生效，强幂等模式下不会使用此提示（直接返回缓存结果）</li>
     *     <li>可以根据业务场景自定义提示信息，提高用户体验</li>
     *     <li>建议提示信息清晰明确，告知用户为什么不能重复提交</li>
     * </ul>
     *
     * @return 错误提示信息，默认 "请勿重复提交"
     */
    String message() default "请勿重复提交";

    /**
     * 是否从请求参数中获取 Token（当请求头中没有时）
     * <p>
     * 当无法在请求头中传递 Token 时（如 GET 请求、表单提交等），可以启用此选项从请求参数中获取 Token。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // GET 请求：从查询参数获取 Token
     * @GetMapping("/submit")
     * @Idempotent(allowParam = true, paramName = "token")
     * public Result<String> submit(@RequestParam String token) {
     *     // 客户端请求：GET /submit?token=token-123
     *     return Result.success("提交成功");
     * }
     *
     * // POST 表单提交：从表单参数获取 Token
     * @PostMapping("/submit")
     * @Idempotent(allowParam = true, paramName = "idempotentToken")
     * public Result<String> submitForm() {
     *     // 客户端表单提交：idempotentToken=token-123
     *     return Result.success("提交成功");
     * }
     *
     * // 混合使用：优先从请求头获取，如果没有则从参数获取
     * @PostMapping("/create")
     * @Idempotent(allowParam = true, paramName = "token")
     * public Result<String> create() {
     *     // 优先从请求头获取，如果没有则从参数获取
     *     return Result.success("创建成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>优先级：请求头 > 请求参数（如果请求头中有 Token，则不会从参数获取）</li>
     *     <li>需要配合 {@code paramName} 使用，指定参数名称</li>
     *     <li>适用于 GET 请求、表单提交等无法在请求头传递 Token 的场景</li>
     *     <li>如果 {@code tokenExpression} 已配置，SpEL 表达式优先级最高</li>
     * </ul>
     *
     * @return 是否从请求参数获取，默认 false（仅从请求头获取）
     * @see #paramName()
     */
    boolean allowParam() default false;

    /**
     * 请求参数名称（当 {@code allowParam} 为 true 时使用）
     * <p>
     * 指定从请求参数中获取 Token 的参数名称。仅当 {@code allowParam = true} 时生效。
     * 支持 GET 请求的查询参数和 POST 请求的表单参数。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // GET 请求：从查询参数获取
     * @GetMapping("/submit")
     * @Idempotent(allowParam = true, paramName = "idempotentToken")
     * public Result<String> submit() {
     *     // 客户端请求：GET /submit?idempotentToken=token-123
     *     return Result.success("提交成功");
     * }
     *
     * // POST 表单提交：从表单参数获取
     * @PostMapping("/submit")
     * @Idempotent(allowParam = true, paramName = "token")
     * public Result<String> submitForm() {
     *     // 客户端表单提交：token=token-123
     *     return Result.success("提交成功");
     * }
     *
     * // 自定义参数名称
     * @PostMapping("/create")
     * @Idempotent(allowParam = true, paramName = "requestId")
     * public Result<String> create() {
     *     // 客户端请求：POST /create?requestId=token-123
     *     return Result.success("创建成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>仅当 {@code allowParam = true} 时生效</li>
     *     <li>支持 GET 请求的查询参数和 POST 请求的表单参数</li>
     *     <li>如果请求头中有 Token，不会从参数获取（请求头优先级更高）</li>
     * </ul>
     *
     * @return 参数名称，默认 "idempotentToken"
     * @see #allowParam()
     */
    String paramName() default "idempotentToken";

    /**
     * 从方法参数中获取 Token 的 SpEL 表达式
     * <p>
     * 用于 MQ 消费者等非 HTTP 请求场景，从消息对象中动态提取 Token。
     * 支持字段访问、方法调用、数组索引等多种表达式。
     * <p>
     * <b>SpEL 表达式示例：</b>
     * <pre>{@code
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
     *
     * // 5. 从 Map 获取
     * tokenExpression = "#messageMap['idempotentToken']"
     * // 从 Map 中获取 idempotentToken 键的值
     * }</pre>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // MQ 消费者：从消息对象字段获取 Token
     * @RabbitListener(queues = "order.queue")
     * @Idempotent(tokenExpression = "#messageDTO.idempotentToken")
     * public void handleOrder(MessageDTO messageDTO) {
     *     // MessageDTO 需要有 idempotentToken 字段
     *     // 第一次消费：处理消息
     *     // 第二次消费：拒绝处理或返回缓存结果
     * }
     *
     * // MQ 消费者：从方法参数索引获取
     * @RabbitListener(queues = "payment.queue")
     * @Idempotent(tokenExpression = "#args[0].idempotentToken")
     * public void handlePayment(PaymentDTO paymentDTO) {
     *     // 从第一个参数的 idempotentToken 字段获取
     * }
     *
     * // MQ 消费者：调用方法获取
     * @RabbitListener(queues = "order.queue")
     * @Idempotent(tokenExpression = "#messageDTO.getIdempotentToken()")
     * public void handleOrder(MessageDTO messageDTO) {
     *     // 调用 getIdempotentToken() 方法获取 Token
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <li>优先级最高：tokenExpression > HTTP请求头/参数 > RabbitMQ消息头</li>
     * <li>适用于 MQ 消费者等非 HTTP 请求场景</li>
     * <li>支持字段访问、方法调用、数组索引等多种表达式</li>
     * <li>确保消息对象包含对应字段或方法，否则表达式解析会失败</li>
     * <li>如果表达式解析失败，会尝试从其他方式获取 Token</li>
     * </ul>
     *
     * @return SpEL 表达式，默认空字符串（不使用表达式）
     * @see <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">Spring SpEL 文档</a>
     */
    String tokenExpression() default "";

    /**
     * 幂等性模式（三态设计）
     * <p>
     * 控制幂等性的行为模式，支持三态设计：
     * <ul>
     *     <li><b>DEFAULT</b>：使用全局配置（Nacos 或 application.yml 中的 {@code idempotent.return-cached-result}）</li>
     *     <li><b>TRUE</b>：强制开启强幂等模式（返回缓存结果），即使全局配置为 false</li>
     *     <li><b>FALSE</b>：强制关闭强幂等模式（防重模式，直接报错），即使全局配置为 true</li>
     * </ul>
     * <p>
     * <b>优先级：</b>注解显式指定（TRUE/FALSE） > 全局配置 > 默认值（false，即防重模式）
     * <p>
     * <b>模式说明：</b>
     * <ul>
     *     <li><b>防重模式</b>：重复请求直接报错，适用于支付、下单等不允许重复执行的场景</li>
     *     <li><b>强幂等模式</b>：重复请求返回第一次的结果，适用于需要保证多次调用返回相同结果的场景</li>
     * </ul>
     * <p>
     * <b>防重模式行为：</b>
     * <ul>
     *     <li>第一次请求：执行方法，Token 存入 Redis</li>
     *     <li>第二次请求：直接返回错误提示（使用 {@code message} 配置的提示信息）</li>
     * </ul>
     * <p>
     * <b>强幂等模式行为：</b>
     * <ul>
     *     <li>第一次请求：执行方法并将结果缓存到 Redis</li>
     *     <li>第二次请求：从 Redis 返回第一次的结果，不执行方法</li>
     *     <li>结果缓存时间与 Token 过期时间相同（{@code expireTime}）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. 使用全局配置（如果全局配置为 true，则开启强幂等；如果为 false，则防重模式）
     * @Idempotent
     * public Result<String> createOrder() {
     *     return Result.success("订单创建成功");
     * }
     *
     * // 2. 强制开启强幂等模式（即使全局配置为 false）
     * @Idempotent(returnCachedResult = IdempotentMode.TRUE)
     * public Result<OrderInfo> getOrderInfo() {
     *     // 第一次请求：执行方法并缓存结果
     *     // 第二次请求：返回第一次的结果，不执行方法
     *     return Result.success(orderInfo);
     * }
     *
     * // 3. 强制关闭强幂等模式（即使全局配置为 true）
     * @Idempotent(returnCachedResult = IdempotentMode.FALSE)
     * public Result<String> pay() {
     *     // 第一次请求：执行方法
     *     // 第二次请求：返回错误"请勿重复提交"
     *     return Result.success("支付成功");
     * }
     *
     * // 4. 支付场景：强制使用防重模式
     * @Idempotent(returnCachedResult = IdempotentMode.FALSE, message = "订单已处理，请勿重复支付")
     * public Result<String> processPayment() {
     *     return Result.success("支付成功");
     * }
     *
     * // 5. 查询场景：强制使用强幂等模式
     * @Idempotent(returnCachedResult = IdempotentMode.TRUE, expireTime = 300)
     * public Result<OrderInfo> queryOrder() {
     *     return Result.success(orderInfo);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>强幂等模式会缓存方法执行结果，占用 Redis 存储空间</li>
     *     <li>方法返回类型为 void 时，强幂等模式不会缓存结果</li>
     *     <li>方法执行失败时，Token 和结果缓存都会被删除，允许重试</li>
     *     <li>全局配置支持动态刷新（需在切面类添加 {@code @RefreshScope}）</li>
     *     <li>强幂等模式最大重试 + 等待时间为 3s，不适合高并发接口</li>
     * </ul>
     *
     * @return 幂等性模式，默认 DEFAULT（使用全局配置）
     * @see IdempotentMode
     */
    IdempotentMode returnCachedResult() default IdempotentMode.DEFAULT;
}