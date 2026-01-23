package com.zmbdp.common.ratelimit.annotation;

import com.zmbdp.common.ratelimit.enums.RateLimitDimension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 频控 / 防刷注解
 * <p>
 * 标记需要限流的方法，基于 <b>AOP + Redis 滑动窗口</b>实现。业务代码无感知，仅加注解即可生效。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *     <li>支持 <b>IP、账号、双维度</b>限流（{@link RateLimitDimension}）</li>
 *     <li>支持 <b>Nacos 热配置</b>，无需重启即可调整限流参数</li>
 *     <li>支持 <b>接口级覆盖</b>，每个方法可独立配置限流规则</li>
 *     <li>基于 <b>滑动窗口算法</b>（ZSET + Lua），限流更平滑，无边界突发问题</li>
 *     <li>支持 <b>降级策略</b>（fail-open/fail-close），Redis 异常时可配置处理方式</li>
 * </ul>
 * <p>
 * <b>配置优先级：</b>
 * <ol>
 *     <li>注解参数（{@code limit}、{@code windowSec}、{@code message}）</li>
 *     <li>Nacos 全局配置（{@code ratelimit.default-*}）</li>
 *     <li>代码默认值</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 示例 1：仅 IP 限流（每 IP 每分钟 10 次）
 * @GetMapping("/api/list")
 * @RateLimit(limit = 10, windowSec = 60, dimensions = RateLimitDimension.IP)
 * public Result<List<Item>> list() {
 *     return Result.success(itemService.list());
 * }
 *
 * // 示例 2：仅账号限流（未登录时自动退化为 IP 限流）
 * @PostMapping("/api/submit")
 * @RateLimit(limit = 5, windowSec = 60, dimensions = RateLimitDimension.ACCOUNT)
 * public Result<Void> submit(@RequestBody SubmitDTO dto) {
 *     return Result.success();
 * }
 *
 * // 示例 3：双维度限流（IP 和账号均需满足，更严格）
 * @PostMapping("/send_code")
 * @RateLimit(
 *     limit = 3,
 *     windowSec = 60,
 *     dimensions = RateLimitDimension.BOTH,
 *     message = "操作过于频繁，请稍后重试"
 * )
 * public Result<String> sendCode(@RequestParam String account) {
 *     return Result.success(userService.sendCode(account));
 * }
 *
 * // 示例 4：使用全局默认配置（limit/windowSec 从 Nacos 读取）
 * @GetMapping("/api/info")
 * @RateLimit(dimensions = RateLimitDimension.IP)
 * public Result<InfoVO> getInfo() {
 *     return Result.success(infoService.getInfo());
 * }
 *
 * // 示例 5：自定义 key 后缀（用于区分同一方法的不同路径）
 * @GetMapping("/api/v1/user")
 * @RateLimit(limit = 10, keySuffix = "v1")
 * public Result<UserVO> getUserV1() { ... }
 *
 * @GetMapping("/api/v2/user")
 * @RateLimit(limit = 20, keySuffix = "v2")
 * public Result<UserVO> getUserV2() { ... }
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>无 HTTP 请求上下文时（如内部调用、单元测试）会跳过限流，直接放行</li>
 *     <li>账号维度（{@code ACCOUNT} 或 {@code BOTH}）未登录时（无 {@code userId} 请求头）会退化为 IP 限流</li>
 *     <li>双维度限流时，如果未登录，{@code identityKey == ipKey}，只限流一次，避免重复计数</li>
 *     <li>限流基于 Redis，确保 Redis 可用性，建议配置降级策略（{@code ratelimit.fail-open}）</li>
 *     <li>滑动窗口算法使用 ZSET 存储，每个请求会生成一个 UUID 作为 member，注意 Redis 内存占用</li>
 * </ul>
 * <p>
 * <b>最佳实践：</b>
 * <ul>
 *     <li>敏感接口（如登录、注册、发送验证码）建议使用 {@code BOTH} 双维度限流</li>
 *     <li>查询类接口建议使用 {@code IP} 维度，避免影响正常用户</li>
 *     <li>限流阈值建议根据业务场景调整，避免误杀正常用户</li>
 *     <li>生产环境建议通过 Nacos 配置，便于动态调整，无需发版</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see RateLimitDimension
 * @see com.zmbdp.common.ratelimit.aspect.RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求数
     * <p>
     * 在 {@code windowSec} 时间窗口内，允许的最大请求次数。
     * 超过此阈值后，后续请求将被拒绝，返回 {@code ResultCode.REQUEST_TOO_FREQUENT}。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>未配置或 {@code <= 0}：使用 Nacos 全局配置 {@code ratelimit.default-limit}</li>
     *     <li>配置 {@code > 0}：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <ul>
     *     <li>{@code limit = 10, windowSec = 60}：每分钟最多 10 次请求</li>
     *     <li>{@code limit = 100, windowSec = 1}：每秒最多 100 次请求</li>
     * </ul>
     *
     * @return 限流阈值（时间窗口内最大请求数），默认 0 表示使用全局配置
     */
    int limit() default 0;

    /**
     * 时间窗口大小（秒）
     * <p>
     * 滑动窗口的时间范围，限流统计基于此时间窗口内的请求数。
     * 使用滑动窗口算法，窗口随请求时间滑动，避免固定窗口的边界突发问题。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>未配置或 {@code <= 0}：使用 Nacos 全局配置 {@code ratelimit.default-window-sec}</li>
     *     <li>配置 {@code > 0}：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <ul>
     *     <li>{@code windowSec = 60}：60 秒时间窗口（1 分钟）</li>
     *     <li>{@code windowSec = 3600}：3600 秒时间窗口（1 小时）</li>
     * </ul>
     * <p>
     * <b>注意：</b>时间窗口过小可能导致正常用户被误限流，建议根据业务场景合理设置。
     *
     * @return 时间窗口大小（秒），默认 0 表示使用全局配置
     */
    long windowSec() default 0L;

    /**
     * 限流维度
     * <p>
     * 指定限流的统计维度，支持 IP、账号、或双维度限流。
     * <p>
     * <b>维度说明：</b>
     * <ul>
     *     <li>{@code IP}：仅按客户端 IP 限流，适用于查询类接口</li>
     *     <li>{@code ACCOUNT}：按用户账号限流，未登录时自动退化为 IP 限流</li>
     *     <li>{@code BOTH}：同时按 IP 和账号限流，任一维度超限即拒绝，适用于敏感接口</li>
     * </ul>
     * <p>
     * <b>使用建议：</b>
     * <ul>
     *     <li>查询类接口：使用 {@code IP} 维度，避免影响正常用户</li>
     *     <li>写操作接口：使用 {@code ACCOUNT} 维度，防止单个用户刷接口</li>
     *     <li>敏感接口（登录、注册、发送验证码）：使用 {@code BOTH} 维度，更严格</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>账号维度（{@code ACCOUNT} 或 {@code BOTH}）需要网关下发 {@code userId} 请求头</li>
     *     <li>未登录时（无 {@code userId} 请求头），账号维度会自动退化为 IP 限流</li>
     *     <li>双维度限流时，如果未登录，{@code identityKey == ipKey}，只限流一次，避免重复计数</li>
     * </ul>
     *
     * @return 限流维度，默认 {@code IP}
     * @see RateLimitDimension
     */
    RateLimitDimension dimensions() default RateLimitDimension.IP;

    /**
     * 触发限流时的提示信息
     * <p>
     * 当请求被限流时，返回给客户端的错误提示信息。<br>
     * 此信息会封装在 {@link com.zmbdp.common.domain.exception.ServiceException} 中，
     * 最终返回给客户端。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用 Nacos 全局配置 {@code ratelimit.default-message}</li>
     *     <li>非空：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <ul>
     *     <li>{@code message = "操作过于频繁，请稍后重试"}</li>
     *     <li>{@code message = "请求频率过高，请 1 分钟后再试"}</li>
     * </ul>
     * <p>
     * <b>注意：</b>消息会被自动 trim，避免前后空格导致判断错误。
     *
     * @return 限流提示信息，默认空字符串表示使用全局配置
     */
    String message() default "";

    /**
     * 接口级 key 后缀（可选）
     * <p>
     * 用于区分同一服务内不同接口的限流 key，支持自定义限流粒度。
     * <p>
     * <b>Key 构建规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用「类全限定名#方法名」作为后缀</li>
     *     <li>非空：使用指定的 {@code keySuffix} 作为后缀</li>
     * </ul>
     * <p>
     * <b>完整 Key 格式：</b>
     * <ul>
     *     <li>IP 维度：{@code {prefix}ip:{ip}:{api}}</li>
     *     <li>账号维度：{@code {prefix}identity:{userIdentifier}:{api}}</li>
     * </ul>
     * <p>
     * 其中 {@code api} 为 {@code keySuffix}（如果指定）或「类名#方法名」。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>同一方法对应多个路径，需要区分限流（如 {@code /api/v1/user} 和 {@code /api/v2/user}）</li>
     *     <li>需要将多个接口合并限流（如多个查询接口共享同一个限流 key）</li>
     *     <li>需要自定义限流粒度，不依赖类名和方法名</li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <pre>{@code
     * // 示例 1：区分不同版本的接口
     * @GetMapping("/api/v1/user")
     * @RateLimit(limit = 10, keySuffix = "user-v1")
     * public Result<UserVO> getUserV1() { ... }
     *
     * @GetMapping("/api/v2/user")
     * @RateLimit(limit = 20, keySuffix = "user-v2")
     * public Result<UserVO> getUserV2() { ... }
     *
     * // 示例 2：多个接口共享限流（合并限流）
     * @GetMapping("/api/list")
     * @RateLimit(limit = 100, keySuffix = "query-all")
     * public Result<List<Item>> list() { ... }
     *
     * @GetMapping("/api/search")
     * @RateLimit(limit = 100, keySuffix = "query-all")
     * public Result<List<Item>> search() { ... }
     * }</pre>
     * <p>
     * <b>注意：</b>
     * <ul>
     *     <li>如果不指定 {@code keySuffix}，默认使用「类全限定名#方法名」，通常已足够区分不同接口</li>
     *     <li>建议只在特殊场景下使用 {@code keySuffix}，避免增加维护成本</li>
     * </ul>
     *
     * @return key 后缀，默认空字符串表示使用「类名#方法名」
     */
    String keySuffix() default "";

    /**
     * IP 请求头名称（可选）
     * <p>
     * 用于指定从 HTTP 请求头中获取客户端 IP 的字段名称。<br>
     * 支持自定义请求头名称，如 "X-Forwarded-For"、"X-Real-IP"、"X-Client-IP" 等。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用 Nacos 全局配置 {@code ratelimit.ip-header-name}</li>
     *     <li>非空：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <ul>
     *     <li>{@code ipHeaderName = "X-Forwarded-For"}：从 X-Forwarded-For 请求头获取（默认）</li>
     *     <li>{@code ipHeaderName = "X-Real-IP"}：从 X-Real-IP 请求头获取</li>
     *     <li>{@code ipHeaderName = "X-Client-IP"}：从 X-Client-IP 请求头获取</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果请求头中没有 IP 且 {@code allowIpParam = true}，会尝试从请求参数获取</li>
     *     <li>如果请求头中没有 IP 且 {@code allowIpParam = false}，会回退到标准 IP 获取逻辑（X-Forwarded-For → X-Real-IP → getRemoteAddr()）</li>
     *     <li>建议根据实际部署环境（Nginx、CDN、负载均衡器等）选择合适的请求头</li>
     * </ul>
     *
     * @return IP 请求头名称，默认空字符串表示使用全局配置
     * @see #allowIpParam()
     * @see #ipParamName()
     */
    String ipHeaderName() default "";

    /**
     * 是否从请求参数中获取 IP（当请求头中没有时）
     * <p>
     * 当无法在请求头中传递 IP 时（如特殊场景、自定义需求等），可以启用此选项从请求参数中获取 IP。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>未配置：使用 Nacos 全局配置 {@code ratelimit.ip-allow-param}</li>
     *     <li>已配置：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // GET 请求：从查询参数获取 IP
     * @GetMapping("/api/list")
     * @RateLimit(allowIpParam = true, ipParamName = "clientIp")
     * public Result<List<Item>> list() {
     *     // 客户端请求：GET /api/list?clientIp=192.168.1.100
     *     return Result.success(itemService.list());
     * }
     *
     * // POST 表单提交：从表单参数获取 IP
     * @PostMapping("/api/submit")
     * @RateLimit(allowIpParam = true, ipParamName = "ip")
     * public Result<Void> submit() {
     *     // 客户端表单提交：ip=192.168.1.100
     *     return Result.success();
     * }
     *
     * // 混合使用：优先从请求头获取，如果没有则从参数获取
     * @PostMapping("/api/create")
     * @RateLimit(ipHeaderName = "X-Client-IP", allowIpParam = true, ipParamName = "clientIp")
     * public Result<String> create() {
     *     // 优先从 X-Client-IP 请求头获取，如果没有则从 clientIp 参数获取
     *     return Result.success();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>优先级：请求头 > 请求参数（如果请求头中有 IP，则不会从参数获取）</li>
     *     <li>需要配合 {@code ipParamName} 使用，指定参数名称</li>
     *     <li>如果请求头和参数都没有 IP，会回退到标准 IP 获取逻辑（X-Forwarded-For → X-Real-IP → getRemoteAddr()）</li>
     *     <li>适用于特殊场景，如客户端需要显式传递 IP 地址</li>
     * </ul>
     *
     * @return 是否从请求参数获取 IP，默认 false（仅从请求头获取）
     * @see #ipParamName()
     * @see #ipHeaderName()
     */
    boolean allowIpParam() default false;

    /**
     * IP 请求参数名称（当 {@code allowIpParam} 为 true 时使用）
     * <p>
     * 指定从请求参数中获取 IP 的参数名称。仅当 {@code allowIpParam = true} 时生效。<br>
     * 支持 GET 请求的查询参数和 POST 请求的表单参数。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用 Nacos 全局配置 {@code ratelimit.ip-param-name}</li>
     *     <li>非空：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // GET 请求：从查询参数获取
     * @GetMapping("/api/list")
     * @RateLimit(allowIpParam = true, ipParamName = "clientIp")
     * public Result<List<Item>> list() {
     *     // 客户端请求：GET /api/list?clientIp=192.168.1.100
     *     return Result.success(itemService.list());
     * }
     *
     * // POST 表单提交：从表单参数获取
     * @PostMapping("/api/submit")
     * @RateLimit(allowIpParam = true, ipParamName = "ip")
     * public Result<Void> submit() {
     *     // 客户端表单提交：ip=192.168.1.100
     *     return Result.success();
     * }
     *
     * // 自定义参数名称
     * @PostMapping("/api/create")
     * @RateLimit(allowIpParam = true, ipParamName = "remoteIp")
     * public Result<String> create() {
     *     // 客户端请求：POST /api/create?remoteIp=192.168.1.100
     *     return Result.success();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>仅当 {@code allowIpParam = true} 时生效</li>
     *     <li>支持 GET 请求的查询参数和 POST 请求的表单参数</li>
     *     <li>如果请求头中有 IP，不会从参数获取（请求头优先级更高）</li>
     *     <li>如果请求头和参数都没有 IP，会回退到标准 IP 获取逻辑</li>
     * </ul>
     *
     * @return IP 请求参数名称，默认空字符串表示使用全局配置
     * @see #allowIpParam()
     * @see #ipHeaderName()
     */
    String ipParamName() default "";
}