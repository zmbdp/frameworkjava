# 频控 / 防刷

## 概述

频控（防刷）通过 **AOP + 注解 + Redis** 实现，业务代码无感知。支持 **IP、账号双维度** 限流、**Nacos 热配置** 与 **单接口级配置**。

## 核心组件

- `@RateLimit`：标记需要限流的方法
- `RateLimitAspect`：切面，负责流程编排（约 150 行，职责单一）
- `RateLimiterExecutor`：限流执行器接口（可扩展不同算法）
- `RedisTokenBucketRateLimiter`：令牌桶实现（Hash + Lua，默认算法）
- `RedisSlidingWindowRateLimiter`：滑动窗口实现（ZSET + Lua，可选算法）
- `RateLimitKeyBuilder`：Key 构建器（集中管理维度逻辑，便于扩展）
- `RateLimitConfigResolver`：配置解析器（注解 > Nacos > 默认值）
- Redis：支持**令牌桶**和**滑动窗口**两种算法，可通过配置选择

## 使用方式

### 1. 引入依赖

在需要使用频控的服务模块（如 portal、admin）的 `pom.xml` 中增加：

```xml
<dependency>
    <groupId>com.zmbdp</groupId>
    <artifactId>zmbdp-common-ratelimit</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 引用 Nacos 配置

在对应服务的 `bootstrap.yml` 的 `spring.cloud.nacos.config.shared-configs` 中增加（与 `share-idempotent` 等一起）：

```yaml
- data-id: share-ratelimit-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
  refresh: true
```

### 2.1 选择限流算法（可选）

支持两种限流算法，可通过配置选择：

```yaml
ratelimit:
  algorithm: token-bucket  # 或 sliding-window
```

**算法说明：**
- **`token-bucket`**（默认）：令牌桶算法，允许突发流量，适合需要平滑限流的场景
- **`sliding-window`**：滑动窗口算法，严格限制时间窗口内的请求数，适合需要精确控制的场景

**配置位置：** 在 Nacos 的 `share-ratelimit-{env}.yaml` 中配置，或服务的 `application.yml` 中配置。

### 3. 在接口上使用注解

```java
// 仅 IP 限流：每 IP 每分钟 10 次
@GetMapping("/api/list")
@RateLimit(limit = 10, windowSec = 60, dimensions = RateLimitDimension.IP)
public Result<List<Item>> list() { ... }

// 仅账号限流（未登录时按 IP）
@PostMapping("/api/submit")
@RateLimit(limit = 5, windowSec = 60, dimensions = RateLimitDimension.ACCOUNT)
public Result<Void> submit() { ... }

// 双维度：IP 与账号均需满足
@PostMapping("/send_code")
@RateLimit(limit = 3, windowSec = 60, dimensions = RateLimitDimension.BOTH,
           message = "操作过于频繁，请稍后重试")
public Result<String> sendCode(@RequestParam String account) { ... }

// 使用全局默认（limit / windowSec 从 Nacos 读取）
@RateLimit(dimensions = RateLimitDimension.IP)
public Result<?> someApi() { ... }
```

## 限流维度

| 维度 | 说明 |
|------|------|
| `IP` | 按客户端 IP 限流（支持 X-Forwarded-For、X-Real-IP） |
| `ACCOUNT` | 按用户身份限流（已登录：从 JWT Token 提取 `userId`；未登录：从请求参数提取 `account`）；未找到时退化为 IP |
| `BOTH` | IP 与身份同时限流，任一超限即拒绝 |

### Redis Key 格式

- **IP 维度**：`ratelimit:ip:{ip}:{类名#方法名}`
- **身份维度**：`ratelimit:identity:{userIdentifier}:{类名#方法名}`

其中 `userIdentifier` 可以是：
- 已登录：`userId`（从 JWT Token 中提取）
- 未登录：`account`（从请求参数或方法参数中提取，如发送验证码、登录接口）

## 配置说明

### Nacos 全局配置（`share-ratelimit-{env}.yaml`）

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `ratelimit.enabled` | 全局开关 | `true` |
| `ratelimit.algorithm` | 限流算法<br/>`token-bucket`=令牌桶（默认）<br/>`sliding-window`=滑动窗口 | `token-bucket` |
| `ratelimit.key-prefix` | Redis Key 前缀 | `ratelimit:` |
| `ratelimit.default-limit` | 全局默认限流阈值<br/>令牌桶：桶容量（最大令牌数）<br/>滑动窗口：时间窗口内最大请求数 | `60` |
| `ratelimit.default-window-sec` | 全局默认时间窗口（秒）<br/>令牌桶：用于计算补充速率（refillRate = limit / windowSec）<br/>滑动窗口：滑动窗口的时间范围 | `60` |
| `ratelimit.default-message` | 全局默认提示文案 | `请求过于频繁，请稍后重试` |
| `ratelimit.fail-open` | Redis 异常时的降级策略<br/>`true`=失败放行（可用性优先）<br/>`false`=失败拒绝（安全性优先） | `false` |
| `ratelimit.ip-header-name` | IP 请求头名称（如 X-Forwarded-For、X-Real-IP） | `X-Forwarded-For` |
| `ratelimit.ip-allow-param` | 是否允许从请求参数获取 IP | `false` |
| `ratelimit.ip-param-name` | IP 请求参数名称（当 `ip-allow-param=true` 时使用） | `clientIp` |

以上均支持 **Nacos 热更新**（`refresh: true`），切面使用 `@RefreshScope` 读取。

### 注解级覆盖

- `limit`、`windowSec`：接口级限流阈值与窗口；≤0 时使用全局配置。
  - **令牌桶**：`limit` 表示桶容量（最大令牌数），`windowSec` 用于计算补充速率
  - **滑动窗口**：`limit` 表示时间窗口内最大请求数，`windowSec` 表示滑动窗口的时间范围
- `message`：触发限流时的提示；为空时使用全局配置。
- `keySuffix`：可选，自定义限流 key 后缀；为空时使用「类名#方法名」。
- `ipHeaderName`：IP 请求头名称；为空时使用全局配置。
- `allowIpParam`：是否允许从请求参数获取 IP；默认 false。
- `ipParamName`：IP 请求参数名称；为空时使用全局配置。

## IP 获取配置

限流需要获取客户端真实 IP，支持多种获取方式：

### IP 获取优先级

1. **请求头获取**（默认）：从配置的请求头获取（如 `X-Forwarded-For`、`X-Real-IP`）
2. **请求参数获取**（可选）：如果允许且请求头中没有，从请求参数获取
3. **标准获取逻辑**：回退到标准 IP 获取（X-Forwarded-For → X-Real-IP → getRemoteAddr()）

### 使用示例

```java
// 示例 1：从自定义请求头获取 IP
@RateLimit(ipHeaderName = "X-Client-IP", limit = 10)
public Result<?> someApi() { ... }

// 示例 2：允许从请求参数获取 IP
@RateLimit(allowIpParam = true, ipParamName = "clientIp", limit = 10)
public Result<?> someApi() { ... }

// 示例 3：混合使用（优先请求头，没有则从参数获取）
@RateLimit(
    ipHeaderName = "X-Client-IP",
    allowIpParam = true,
    ipParamName = "clientIp",
    limit = 10
)
public Result<?> someApi() { ... }
```

## 注意事项

1. **请求上下文**：限流依赖 HTTP 请求（`ServletRequest`）。无请求上下文时（如内部调用、部分测试）会**跳过限流**，直接放行。
2. **身份维度**：支持多种获取方式：
   - 已登录：从 JWT Token 中提取 `userId`
   - 未登录（GET 请求）：从 Query 参数中提取 `account`
   - 未登录（POST 请求）：从方法参数中提取 `account`（通过 SpEL 表达式）
   - 未找到身份标识时，`ACCOUNT` 退化为 IP，避免重复计数。
3. **Redis**：需确保服务已引入并配置 `zmbdp-common-redis`，且 Nacos 中 `share-redis` 正确。
4. **原子性保证**：使用 Lua 脚本保证限流操作的原子性。
   - **令牌桶**：Hash + Lua（HSET + EXPIRE），保证补充和消耗令牌的原子性
   - **滑动窗口**：ZSET + Lua（ZREMRANGEBYSCORE + ZCARD + ZADD + EXPIRE），保证窗口统计的原子性
5. **降级策略**：Redis 异常时可配置 `fail-open`（失败放行）或 `fail-close`（失败拒绝）。生产环境建议 `false`（安全优先）。
6. **限流算法选择**：
   - **令牌桶算法**（默认）：允许突发流量（桶满时），令牌以固定速率持续补充，适合需要平滑限流的场景。使用 Redis Hash 存储桶状态（tokens, lastRefillTime），内存占用较小。
   - **滑动窗口算法**：严格限制时间窗口内的请求数，按「当前时刻往前 windowSec 秒」统计请求数，窗口随请求滑动，避免固定窗口在 59s/61s 等边界的突发流量。使用 Redis ZSET 存储请求时间戳，适合需要精确控制的场景。
7. **IP 获取**：默认从 `X-Forwarded-For` 请求头获取，支持自定义请求头或从请求参数获取（需配置 `ip-allow-param=true`）。

## 错误码

触发限流时抛出 `ServiceException`，业务码为 `ResultCode.REQUEST_TOO_FREQUENT`（400020），文案可使用注解或 Nacos 配置的 `message`。
