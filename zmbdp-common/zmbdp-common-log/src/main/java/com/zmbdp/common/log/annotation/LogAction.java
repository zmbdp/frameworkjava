package com.zmbdp.common.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 用于标记需要记录操作日志的方法或类，自动记录方法执行、业务行为、参数、返回值、异常等信息。<br>
 * 基于 Spring AOP 实现，支持异步处理、条件记录、SpEL 表达式、性能监控等功能。
 * <p>
 * <b>支持两种使用方式：</b>
 * <ul>
 *     <li><b>方法注解（推荐）</b>：标注在方法上，精确控制每个方法的日志策略</li>
 *     <li><b>类注解（辅助）</b>：标注在类上，作为默认配置，方法注解可覆盖</li>
 * </ul>
 * <p>
 * <b>优先级规则：</b>
 * <ul>
 *     <li>方法注解 > 类注解（方法注解存在时，完全使用方法注解的配置）</li>
 *     <li>如果方法注解不存在，使用类注解的配置</li>
 *     <li>类注解通常用于设置默认策略（如全局记录参数、全局开启异步等）</li>
 * </ul>
 * <p>
 * <b>功能说明：</b>
 * <ul>
 *     <li>记录方法执行和业务行为（如"新增用户"、"删除订单"）</li>
 *     <li>自动记录方法入参和返回值（可选）</li>
 *     <li>自动捕获异常并记录异常堆栈</li>
 *     <li>记录操作者信息（用户ID、用户名）</li>
 *     <li>记录请求来源（IP、User-Agent）</li>
 *     <li>记录操作时间戳（精确到毫秒）</li>
 *     <li>记录方法执行耗时（性能监控）</li>
 *     <li>支持 SpEL 表达式进行条件记录和模板化</li>
 *     <li>支持敏感字段脱敏</li>
 *     <li>支持异步处理，不阻塞业务线程</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用示例：</b>
 * <p>
 * <b>方法注解示例：</b>
 * <pre>{@code
 * // 示例 1：基础用法（仅记录操作描述）
 * @PostMapping("/user/add")
 * @LogAction("新增用户")
 * public Result<String> addUser(@RequestBody UserDTO dto) {
 *     return Result.success("用户创建成功");
 * }
 *
 * // 示例 2：记录入参和返回值
 * @PostMapping("/order/create")
 * @LogAction(value = "创建订单", recordParams = true, recordResult = true)
 * public Result<OrderVO> createOrder(@RequestBody OrderDTO dto) {
 *     OrderVO order = orderService.create(dto);
 *     return Result.success(order);
 * }
 *
 * // 示例 3：条件记录（只有成功时才记录）
 * @PostMapping("/order/update")
 * @LogAction(
 *     value = "更新订单状态",
 *     recordParams = true,
 *     recordResult = true,
 *     condition = "#result.success == true"
 * )
 * public Result<String> updateOrder(@RequestBody OrderDTO dto) {
 *     return Result.success("更新成功");
 * }
 *
 * // 示例 4：使用 SpEL 表达式记录特定字段
 * @PostMapping("/user/edit")
 * @LogAction(
 *     value = "编辑用户",
 *     recordParams = true,
 *     paramsExpression = "{'userId': #userDTO.userId, 'userName': #userDTO.userName}"
 * )
 * public Result<String> editUser(@RequestBody UserDTO userDTO) {
 *     return Result.success("编辑成功");
 * }
 *
 * // 示例 5：记录异常但不抛出（静默处理）
 * @PostMapping("/data/sync")
 * @LogAction(value = "数据同步", recordException = true, throwException = false)
 * public Result<String> syncData() {
 *     // 即使异常也会记录日志，但不会抛出异常
 *     return Result.success("同步成功");
 * }
 *
 * // 示例 6：指定业务模块和类型
 * @PostMapping("/product/add")
 * @LogAction(
 *     value = "新增商品",
 *     module = "商品管理",
 *     businessType = "商品操作",
 *     recordParams = true
 * )
 * public Result<String> addProduct(@RequestBody ProductDTO dto) {
 *     return Result.success("商品创建成功");
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>注解必须标注在方法上，类级别不支持</li>
 *     <li>SpEL 表达式中可以使用方法参数名（如 {@code #userDTO}）或 {@code args[0]} 访问参数</li>
 *     <li>SpEL 表达式中可以使用 {@code #result} 访问方法返回值</li>
 *     <li>条件表达式返回 {@code false} 时，不会记录日志</li>
 *     <li>异步处理默认启用，可通过配置 {@code log.async-enabled} 控制</li>
 *     <li>敏感字段（如密码、手机号）会自动脱敏，可通过 {@code desensitizeFields} 配置</li>
 *     <li>无 HTTP 请求上下文时（如内部调用、单元测试）会跳过部分信息收集</li>
 * </ul>
 * </p>
 * <p>
 * <b>类注解示例：</b>
 * <pre>{@code
 * // 在 Controller 类上设置默认配置
 * @RestController
 * @RequestMapping("/user")
 * @LogAction(recordParams = true, recordException = true, module = "用户管理")
 * public class UserController {
 *
 *     // 方法1：使用类注解的默认配置，只需要设置操作描述
 *     @PostMapping("/add")
 *     @LogAction("新增用户")
 *     public Result<String> addUser(@RequestBody UserDTO dto) {
 *         return Result.success("用户创建成功");
 *     }
 *
 *     // 方法2：覆盖类注解的配置，不记录参数
 *     @PostMapping("/delete")
 *     @LogAction(value = "删除用户", recordParams = false)
 *     public Result<String> deleteUser(@RequestParam Long id) {
 *         return Result.success("删除成功");
 *     }
 *
 *     // 方法3：完全使用类注解的配置（如果类注解设置了 value，这里可以不设置）
 *     @PostMapping("/update")
 *     public Result<String> updateUser(@RequestBody UserDTO dto) {
 *         // 注意：如果类注解的 value 为空，此方法不会记录日志
 *         return Result.success("更新成功");
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>注解可以标注在方法上或类上，支持同时使用</li>
 *     <li>方法注解优先级高于类注解，方法注解存在时完全使用方法注解的配置</li>
 *     <li>类注解的 {@code value} 通常不设置（为空），仅作为默认配置使用</li>
 *     <li>如果类注解设置了 {@code value}，该类下所有方法都会使用该操作描述（除非方法注解覆盖）</li>
 *     <li>SpEL 表达式中可以使用方法参数名（如 {@code #userDTO}）或 {@code args[0]} 访问参数</li>
 *     <li>SpEL 表达式中可以使用 {@code #result} 访问方法返回值</li>
 *     <li>条件表达式返回 {@code false} 时，不会记录日志</li>
 *     <li>异步处理默认启用，可通过配置 {@code log.async-enabled} 控制</li>
 *     <li>敏感字段（如密码、手机号）会自动脱敏，可通过 {@code desensitizeFields} 配置</li>
 *     <li>无 HTTP 请求上下文时（如内部调用、单元测试）会跳过部分信息收集</li>
 * </ul>
 * </p>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.log.aspect.LogActionAspect
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LogAction {

    /**
     * 操作描述（必填）
     * <p>
     * 用于描述当前操作的类型，如"新增用户"、"删除订单"、"更新商品状态"等。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * @LogAction("新增用户")
     * public Result<String> addUser(@RequestBody UserDTO dto) {
     *     return Result.success("用户创建成功");
     * }
     * }</pre>
     *
     * @return 操作描述
     */
    String value();

    /**
     * 是否记录方法入参
     * <p>
     * 设置为 {@code true} 时，会将方法参数序列化为 JSON 格式记录到日志中。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>参数会被序列化为 JSON，大对象可能影响性能</li>
     *     <li>敏感字段会自动脱敏（如密码、手机号）</li>
     *     <li>如果配置了 {@code paramsExpression}，优先使用表达式结果</li>
     * </ul>
     *
     * @return 是否记录方法入参，默认 false
     */
    boolean recordParams() default false;

    /**
     * 是否记录方法返回值
     * <p>
     * 设置为 {@code true} 时，会将方法返回值序列化为 JSON 格式记录到日志中。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回值会被序列化为 JSON，大对象可能影响性能</li>
     *     <li>如果配置了 {@code resultExpression}，优先使用表达式结果</li>
     *     <li>void 方法无返回值，不会记录</li>
     * </ul>
     *
     * @return 是否记录方法返回值，默认 false
     */
    boolean recordResult() default false;

    /**
     * 是否记录异常信息
     * <p>
     * 设置为 {@code true} 时，如果方法执行抛出异常，会记录异常信息和堆栈。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>异常信息包括异常类型、异常消息和完整堆栈</li>
     *     <li>即使 {@code throwException = false}，异常信息仍会记录</li>
     *     <li>建议在生产环境启用，便于问题排查</li>
     * </ul>
     *
     * @return 是否记录异常信息，默认 true
     */
    boolean recordException() default true;

    /**
     * 异常时是否抛出异常
     * <p>
     * 设置为 {@code false} 时，即使方法执行失败，也不会抛出异常，仅记录日志。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>非关键业务操作（如日志记录、数据同步）</li>
     *     <li>需要静默处理的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>设置为 {@code false} 时，方法会返回默认值（void 返回 null，其他返回类型返回 null 或默认值）</li>
     *     <li>异常信息仍会记录到日志中</li>
     *     <li>不建议在关键业务中使用，可能导致业务逻辑错误</li>
     * </ul>
     *
     * @return 异常时是否抛出异常，默认 true
     */
    boolean throwException() default true;

    /**
     * 条件表达式（SpEL）
     * <p>
     * 使用 SpEL 表达式判断是否记录日志。只有当表达式返回 {@code true} 时才记录日志。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 只有成功时才记录
     * @LogAction(value = "更新订单", condition = "#result.success == true")
     * public Result<String> updateOrder(@RequestBody OrderDTO dto) {
     *     return Result.success("更新成功");
     * }
     *
     * // 只有特定用户操作时才记录
     * @LogAction(value = "删除数据", condition = "#userId != null && #userId > 0")
     * public Result<String> deleteData(@RequestParam Long userId) {
     *     return Result.success("删除成功");
     * }
     * }</pre>
     * <p>
     * <b>SpEL 表达式可用变量：</b>
     * <ul>
     *     <li>{@code #result}：方法返回值</li>
     *     <li>{@code #参数名}：方法参数（如 {@code #userDTO}、{@code #orderId}）</li>
     *     <li>{@code args[0]}、{@code args[1]}：方法参数数组</li>
     * </ul>
     *
     * @return 条件表达式，默认空字符串（无条件限制）
     */
    String condition() default "";

    /**
     * 参数记录表达式（SpEL）
     * <p>
     * 使用 SpEL 表达式自定义参数记录内容。如果配置了此表达式，会优先使用表达式结果，而不是完整的参数对象。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 只记录用户ID和用户名
     * @LogAction(
     *     value = "编辑用户",
     *     paramsExpression = "{'userId': #userDTO.userId, 'userName': #userDTO.userName}"
     * )
     * public Result<String> editUser(@RequestBody UserDTO userDTO) {
     *     return Result.success("编辑成功");
     * }
     *
     * // 记录订单ID和金额
     * @LogAction(
     *     value = "创建订单",
     *     paramsExpression = "{'orderId': #orderDTO.id, 'amount': #orderDTO.amount}"
     * )
     * public Result<String> createOrder(@RequestBody OrderDTO orderDTO) {
     *     return Result.success("订单创建成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>表达式应返回 Map 或对象，会被序列化为 JSON</li>
     *     <li>如果表达式执行失败，会降级使用完整参数对象</li>
     *     <li>需要同时设置 {@code recordParams = true} 才会生效</li>
     * </ul>
     *
     * @return 参数记录表达式，默认空字符串（使用完整参数对象）
     */
    String paramsExpression() default "";

    /**
     * 返回值记录表达式（SpEL）
     * <p>
     * 使用 SpEL 表达式自定义返回值记录内容。如果配置了此表达式，会优先使用表达式结果，而不是完整的返回值对象。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 只记录订单ID
     * @LogAction(
     *     value = "创建订单",
     *     recordResult = true,
     *     resultExpression = "#result.data.id"
     * )
     * public Result<OrderVO> createOrder(@RequestBody OrderDTO dto) {
     *     return Result.success(orderVO);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>表达式应返回可序列化的对象</li>
     *     <li>如果表达式执行失败，会降级使用完整返回值对象</li>
     *     <li>需要同时设置 {@code recordResult = true} 才会生效</li>
     * </ul>
     *
     * @return 返回值记录表达式，默认空字符串（使用完整返回值对象）
     */
    String resultExpression() default "";

    /**
     * 业务模块（可选）
     * <p>
     * 用于分类日志，便于后续查询和分析。如"用户管理"、"订单管理"、"商品管理"等。
     *
     * @return 业务模块名称，默认空字符串
     */
    String module() default "";

    /**
     * 业务类型（可选）
     * <p>
     * 用于进一步分类日志，便于后续查询和分析。如"新增"、"删除"、"更新"、"查询"等。
     *
     * @return 业务类型，默认空字符串
     */
    String businessType() default "";

    /**
     * 需要脱敏的字段名（多个用逗号分隔）
     * <p>
     * 指定需要脱敏的参数字段名，支持自动脱敏。如"password,phone,idCard"。
     * <p>
     * <b>支持的脱敏类型：</b>
     * <ul>
     *     <li>{@code password}：密码（全部替换为*）</li>
     *     <li>{@code phone}：手机号（保留前3位和后4位）</li>
     *     <li>{@code idCard}：身份证号（保留前6位和后4位）</li>
     *     <li>{@code email}：邮箱（保留@前3位和@后全部）</li>
     *     <li>{@code bankCard}：银行卡号（保留前4位和后4位）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * @LogAction(
     *     value = "用户注册",
     *     recordParams = true,
     *     desensitizeFields = "password,phone"
     * )
     * public Result<String> register(@RequestBody UserDTO dto) {
     *     // password 和 phone 字段会自动脱敏
     *     return Result.success("注册成功");
     * }
     * }</pre>
     *
     * @return 需要脱敏的字段名，多个用逗号分隔，默认空字符串
     */
    String desensitizeFields() default "";

    /**
     * 日志存储类型（可选）
     * <p>
     * 指定当前方法使用的日志存储方式，优先级高于 Nacos 全局配置。
     * <p>
     * <b>支持的存储类型：</b>
     * <ul>
     *     <li>{@code console}：输出到控制台/日志文件（SLF4J，默认）</li>
     *     <li>{@code database}：存储到数据库（需要配置数据库存储服务）</li>
     *     <li>{@code file}：存储到文件系统</li>
     *     <li>{@code redis}：存储到 Redis</li>
     *     <li>{@code mq}：发送到消息队列</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用数据库存储
     * @LogAction(value = "新增用户", storageType = "database")
     * public Result<String> addUser(@RequestBody UserDTO dto) {
     *     return Result.success("用户创建成功");
     * }
     *
     * // 使用消息队列存储
     * @LogAction(value = "创建订单", storageType = "mq")
     * public Result<String> createOrder(@RequestBody OrderDTO dto) {
     *     return Result.success("订单创建成功");
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果为空字符串，使用 Nacos 全局配置 {@code log.storage-type}</li>
     *     <li>如果 Nacos 也未配置，使用默认值 {@code console}</li>
     *     <li>优先级：方法注解 > Nacos 全局配置 > 默认值（console）</li>
     * </ul>
     *
     * @return 日志存储类型，默认空字符串（使用全局配置）
     */
    String storageType() default "";
}