# 网关服务

frameworkJava Gateway 基于 Spring Cloud Gateway 实现，作为系统统一入口负责路由转发、链路追踪注入、Token 鉴权、B/C 端隔离以及统一异常处理，自身不依赖数据源、仅依赖 Redis。

## 概述

| 项 | 值 |
| --- | --- |
| 模块 | `zmbdp-gateway` |
| 启动类 | `com.zmbdp.gateway.ZmbdpGatewayServiceApplication` |
| 应用名 | `zmbdp-gateway-service` |
| 端口 | `10030`（来自 `zmbdp-gateway-service-dev.yaml`） |
| 编程模型 | WebFlux 响应式（`spring.main.web-application-type=reactive`） |
| 注册中心 / 配置中心 | Nacos |
| 中间件依赖 | 仅 Redis |
| 数据源 | 不依赖（启动类显式 `exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class}`） |

核心组件：

```
ZmbdpGatewayServiceApplication        启动类
├─ config/
│   └─ IgnoreWhiteProperties          白名单配置（security.ignore.whites，支持热刷新）
├─ filter/
│   ├─ TraceFilter                    链路追踪过滤器（Order = -300）
│   └─ AuthFilter                     鉴权过滤器（Order = -200，@RefreshScope）
└─ handler/
    └─ GatewayExceptionHandler        全局异常处理器（@Order(-1)）
```

## 过滤器执行顺序

```
请求进入
   ↓
┌──────────────────────────┐
│  TraceFilter (Order=-300) │  ← 生成 / 获取 traceId，写入 MDC，注入 X-Trace-Id
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│   AuthFilter (Order=-200) │  ← 白名单放行 + Token 校验 + B/C 端隔离 + 用户信息透传
└─────────────┬────────────┘
              ↓
┌──────────────────────────┐
│  Spring Cloud Gateway 路由 │  ← 按 predicates / filters 转发到下游微服务
└─────────────┬────────────┘
              ↓
         下游微服务
```

`TraceFilter` 先于 `AuthFilter` 执行，确保进入鉴权阶段时日志上下文已经具备 `traceId`，便于问题定位。

## 链路追踪过滤器（TraceFilter）

`com.zmbdp.gateway.filter.TraceFilter` 实现 `GlobalFilter` + `Ordered`，`getOrder()` 返回 `-300`。

### 关键常量

```java
// TraceId 请求头名称
private static final String TRACE_ID_HEADER = "X-Trace-Id";

// MDC 中的 traceId 键名
private static final String TRACE_ID_MDC_KEY = "traceId";
```

### traceId 生成优先级

`getOrGenerateTraceId(ServerHttpRequest request)` 按以下顺序选取 traceId：

1. **SkyWalking 的 traceId**：如果配置了 SkyWalking Agent，Agent 会提前把 traceId 写入 MDC，`MDC.get("traceId")` 不为空时直接复用，保证与 SkyWalking 链路一致。
2. **请求头中的 traceId**：从 `X-Trace-Id` 请求头获取，便于上游调用方传入。
3. **新生成 traceId**：使用 `UUID.randomUUID().toString().replace("-", "")`，去掉横线后得到 32 位字符串。

```java
// 1. 优先使用 SkyWalking 的 traceId（如果配置了 Agent）
String traceId = MDC.get(TRACE_ID_MDC_KEY);
if (traceId != null && !traceId.isEmpty()) {
    log.debug("使用 SkyWalking traceId: {}", traceId);
    return traceId;
}

// 2. 从请求头获取 traceId
traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
if (traceId != null && !traceId.isEmpty()) {
    log.debug("使用请求头 traceId: {}", traceId);
    return traceId;
}

// 3. 生成新的 traceId
traceId = generateTraceId();
log.debug("生成新的 traceId: {}", traceId);
return traceId;
```

### MDC 写入与下游传递

```java
// 2. 将 traceId 设置到 MDC 中（用于日志输出）
MDC.put(TRACE_ID_MDC_KEY, traceId);

// 3. 将 traceId 添加到请求头，传递给下游服务
ServerHttpRequest mutatedRequest = request.mutate()
        .header(TRACE_ID_HEADER, traceId)
        .build();

ServerWebExchange mutatedExchange = exchange.mutate()
        .request(mutatedRequest)
        .build();

// 4. 继续执行过滤器链，并在完成后清理 MDC
return chain.filter(mutatedExchange)
        .doFinally(signalType -> {
            // 清理 MDC，避免内存泄漏
            MDC.remove(TRACE_ID_MDC_KEY);
        });
```

要点：

- `MDC.put` 后日志框架可以通过 `%X{traceId}` 输出当前请求的 traceId。
- 通过 `request.mutate().header(...)` 将 `X-Trace-Id` 注入到下游请求头，下游服务可继续复用同一 traceId。
- `doFinally` 中调用 `MDC.remove` 清理键值，避免在响应式线程被复用时发生 MDC 泄漏。

## 认证过滤器（AuthFilter）

`com.zmbdp.gateway.filter.AuthFilter` 实现 `GlobalFilter` + `Ordered`，标注 `@RefreshScope`，`getOrder()` 返回 `-200`。注入依赖：

```java
@Autowired
private IgnoreWhiteProperties ignoreWhiteProperties;   // 白名单配置

@Autowired
private RedisService redisService;                     // Redis 操作

@Value("${jwt.token.secret}")
private String secret;                                 // JWT 密钥（来自 share-token 配置）
```

### 鉴权主流程

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String url = request.getURI().getPath();
    // 1. 命中白名单直接放行
    try {
        if (StringUtil.matches(url, ignoreWhiteProperties.getWhites())) {
            return chain.filter(exchange);
        }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    // 2. 提取 Token
    String token = getToken(request);
    if (StringUtil.isEmpty(token)) {
        return unauthorizedResponse(exchange, ResultCode.TOKEN_EMPTY);
    }
    // 3. JWT 解析
    Claims claims = JwtUtil.parseToken(token, secret);
    if (claims == null) {
        return unauthorizedResponse(exchange, ResultCode.TOKEN_INVALID);
    }
    // 4. Redis 校验登录态
    String userKey = JwtUtil.getUserKey(claims);
    if (!redisService.hasKey(getTokenKey(userKey))) {
        return unauthorizedResponse(exchange, ResultCode.LOGIN_STATUS_OVERTIME);
    }
    // 5. B/C 端隔离校验
    String userFrom = JwtUtil.getUserFrom(claims);
    if (url.contains(HttpConstants.SYS_USER_PATH)
            && !UserConstants.USER_FROM_TU_B.equals(userFrom)) {
        return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
    }
    // 6. 提取用户信息并校验完整性
    String userId = JwtUtil.getUserId(claims);
    String userName = JwtUtil.getUserName(claims);
    if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(userName)) {
        return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
    }
    // 7. 用户信息透传到下游 header
    ServerHttpRequest.Builder mutate = request.mutate();
    addHeader(mutate, SecurityConstants.USER_KEY, userKey);
    addHeader(mutate, SecurityConstants.USER_ID, userId);
    addHeader(mutate, SecurityConstants.USERNAME, userName);
    addHeader(mutate, SecurityConstants.USER_FROM, userFrom);
    return chain.filter(exchange);
}
```

### Token 提取

从 `Authorization` 请求头取出，去除 `Bearer ` 前缀：

```java
private String getToken(ServerHttpRequest request) {
    String token = request.getHeaders().getFirst(SecurityConstants.AUTHENTICATION);
    // 裁剪令牌前缀
    if (StringUtil.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
        token = token.replaceFirst(TokenConstants.PREFIX, "");
    }
    return token;
}
```

涉及常量：

- `SecurityConstants.AUTHENTICATION` = `"Authorization"`
- `TokenConstants.PREFIX` = `"Bearer "`

### JWT 解析

使用 `zmbdp-common-security` 中的 `JwtUtil`，基于 HS512 算法签名，密钥来自 `jwt.token.secret`（由 `share-token-{env}.yaml` 提供）。`JwtUtil` 提供以下方法（均通过 `Claims` 提取）：

- `JwtUtil.parseToken(token, secret)`：解析并校验签名。
- `JwtUtil.getUserKey(claims)`：提取 `user_key`，用于在 Redis 中查找登录态。
- `JwtUtil.getUserId(claims)`：提取 `user_id`。
- `JwtUtil.getUserName(claims)`：提取 `username`。
- `JwtUtil.getUserFrom(claims)`：提取 `user_from`。

### Redis 校验

Token 本身不带过期时间，过期由 Redis 控制。校验逻辑：

```java
String userKey = JwtUtil.getUserKey(claims);
if (!redisService.hasKey(getTokenKey(userKey))) {
    // 拿到令牌，但是用户信息为空，说明令牌已过期
    return unauthorizedResponse(exchange, ResultCode.LOGIN_STATUS_OVERTIME);
}
```

Redis Key 规则：`logintoken:` 前缀 + `userKey`：

```java
private String getTokenKey(String token) {
    return TokenConstants.LOGIN_TOKEN_KEY + token;   // TokenConstants.LOGIN_TOKEN_KEY = "logintoken:"
}
```

### 未授权响应

```java
private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, ResultCode resultCode) {
    log.error("AuthFilter.unauthorizedResponse err [鉴权处理异常] 请求路径: {}", exchange.getRequest().getPath());
    int retCode = Integer.parseInt(String.valueOf(resultCode.getCode()).substring(0, 3));
    return ServletUtil.webFluxResponseWriter(exchange.getResponse(), HttpStatus.valueOf(retCode),
            resultCode.getErrMsg(), resultCode.getCode());
}
```

`ResultCode.getCode()` 前 3 位即 HTTP 状态码（如 `401000` → `401`、`404000` → `404`），统一通过 `ServletUtil.webFluxResponseWriter` 以 `Result.fail(code, msg)` 格式写回。

涉及的鉴权错误码（来自 `ResultCode`）：

| ResultCode | code | 含义 |
| --- | --- | --- |
| `TOKEN_EMPTY` | 401000 | 令牌不能为空 |
| `TOKEN_INVALID` | 401001 | 令牌已过期或验证不正确 |
| `LOGIN_STATUS_OVERTIME` | 401003 | 登录状态已过期 |
| `TOKEN_CHECK_FAILED` | 401004 | 令牌验证失败 |

## B/C 端隔离机制（核心）

frameworkJava 通过「路径关键字 + Token 中 `user_from`」实现 B 端与 C 端的访问隔离，避免 C 端 Token 被拿到 B 端使用。

### 关键常量

`com.zmbdp.common.domain.constants.HttpConstants`：

```java
public static final String SYS_USER_PATH = "sys_user";   // 系统管理员路径关键字
public static final String APP_USER_PATH  = "app_user";  // 普通用户路径关键字
```

`com.zmbdp.common.domain.constants.UserConstants`：

```java
public static final String USER_FROM_TU_B = "sys";   // 用户来源：系统（B 端）
public static final String USER_FROM_TU_C = "app";   // 用户来源：C 端用户
```

### 隔离逻辑

```java
String userFrom = JwtUtil.getUserFrom(claims);
if (
        url.contains(HttpConstants.SYS_USER_PATH) && // 如果路径是系统路径
        !UserConstants.USER_FROM_TU_B.equals(userFrom) // 但是用户不是系统来源的话（相当于 C端用户在 C端拿到的 jwt 想在 B端使用）
) {
    return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
}
```

含义：

- 当请求 URL 中包含 `sys_user` 时，认为该路径属于 B 端系统路径。
- 此时要求 Token 中的 `user_from` 必须为 `sys`（即 `USER_FROM_TU_B`）。
- 若 `user_from` 为 `app`（C 端用户）或其他值，则直接返回 `TOKEN_CHECK_FAILED`（HTTP 401）。
- 反过来，B 端用户访问 C 端接口不做此强校验，仅校验 Token 合法性。

通过这一规则，C 端 Token 无法访问任何路径中包含 `sys_user` 的接口，实现 B/C 端隔离。

## 透传到下游的 header 列表

`AuthFilter` 在校验通过后，将以下用户信息通过 `addHeader` 写入下游请求 header，下游微服务可通过这些 header 拿到当前登录用户信息：

| Header 名 | 来源常量 | 含义 | 取自 |
| --- | --- | --- | --- |
| `user_key` | `SecurityConstants.USER_KEY` | 用户标识（Redis 中登录态的 key 后缀） | `JwtUtil.getUserKey(claims)` |
| `user_id` | `SecurityConstants.USER_ID` | 用户 ID | `JwtUtil.getUserId(claims)` |
| `username` | `SecurityConstants.USERNAME` | 用户名 | `JwtUtil.getUserName(claims)` |
| `user_from` | `SecurityConstants.USER_FROM` | 用户来源（`sys` / `app`） | `JwtUtil.getUserFrom(claims)` |

### URL 编码后传递

`addHeader` 在写入前对 value 做 URL 编码，避免中文用户名等特殊字符在 header 传输中出错：

```java
private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value) {
    if (value == null) {
        return;
    }
    String valueStr = value.toString();
    String valueEncode = ServletUtil.urlEncode(valueStr);   // URLEncoder.encode(str, UTF-8)
    mutate.header(name, valueEncode);
}
```

下游服务在读取这些 header 时需要先做 URL 解码。

### 链路追踪 header

除了用户信息外，`TraceFilter` 还会向下游注入 `X-Trace-Id`（详见上文「链路追踪过滤器」一节）。

## 白名单配置

### 配置类

`com.zmbdp.gateway.config.IgnoreWhiteProperties`：

```java
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties {

    /**
     * 放行白名单
     */
    private List<String> whites;
}
```

特性：

- 绑定前缀 `security.ignore`，对应配置项 `security.ignore.whites`。
- `@RefreshScope` 支持 Nacos 配置热刷新，修改白名单后无需重启网关即可生效。
- `AuthFilter` 同样标注 `@RefreshScope`，保证白名单变更能被即时感知。

### 匹配规则

`AuthFilter` 使用 `StringUtil.matches(url, whites)` 判断是否命中白名单，底层使用 Spring 的 `AntPathMatcher`，支持 Ant 风格通配符：

- `?`：匹配单个字符（不含 `/`）
- `*`：匹配单层路径内任意字符串（不含 `/`）
- `**`：匹配任意层路径（含 `/`）

只要命中列表中任意一条规则即放行。

### 配置示例

配置位于 Nacos 配置文件 `zmbdp-gateway-service-{env}.yaml`：

```yaml
security:
  ignore:
    # 白名单配置
    whites:
      - /**/register/**
      - /admin/codeLogin
      - /**/login/**
      - /**/send_code/**
      - /**/nologin/**
      - /**/test/**
```

## 路由配置

路由同样托管在 Nacos 配置文件 `zmbdp-gateway-service-{env}.yaml`，基于 Spring Cloud Gateway 的 `routes` 配置，使用 `lb://` 协议结合 LoadBalancer 完成服务发现与转发：

```yaml
server:
  port: 10030

spring:
  cloud:
    gateway:
      routes:
        # 模板微服务
        - id: zmbdp-mstemplate-service
          uri: lb://zmbdp-mstemplate-service
          predicates:
            - Path=/mstemplate/**
          filters:
            - StripPrefix=1
        # 文件微服务
        - id: zmbdp-file-service
          uri: lb://zmbdp-file-service
          predicates:
            - Path=/file/**
          metadata:
            # 响应超时时间
            response-timeout: 300000
            # 连接超时时间
            connect-timeout: 300000
        # 地图微服务
        - id: zmbdp-admin-service
          uri: lb://zmbdp-admin-service
          predicates:
            - Path=/admin/**
          filters:
            - StripPrefix=1
        # C端用户微服务
        - id: zmbdp-portal-service
          uri: lb://zmbdp-portal-service
          predicates:
            - Path=/portal/**
          filters:
            - StripPrefix=1
```

`StripPrefix=1` 表示转发到下游时会去掉 URL 的第一层路径前缀，例如 `/admin/sys_user/list` 转发到 `zmbdp-admin-service` 时变为 `/sys_user/list`。

## 异常处理（GatewayExceptionHandler）

`com.zmbdp.gateway.handler.GatewayExceptionHandler` 实现 `ErrorWebExceptionHandler`，标注 `@Order(-1)`、`@Configuration`，作为网关全局异常处理器。

### 处理逻辑

```java
@Override
public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    ServerHttpResponse response = exchange.getResponse();

    // 响应已提交则直接抛出，无法再修改
    if (response.isCommitted()) {
        return Mono.error(ex);
    }

    // 兜底错误码
    int retCode = ResultCode.ERROR.getCode();
    String errMsg = ResultCode.ERROR.getErrMsg();

    // 1. 未找到服务（NoResourceFoundException）
    if (ex instanceof NoResourceFoundException) {
        retCode = ResultCode.SERVICE_NOT_FOUND.getCode();
        errMsg = ResultCode.SERVICE_NOT_FOUND.getErrMsg();
    } else if (ex instanceof ServiceException) {
        // 2. 业务自定义异常
        retCode = ((ServiceException) ex).getCode();
        errMsg = ex.getMessage();
    }

    // 前 3 位作为 HTTP 状态码
    int httpCode = Integer.parseInt(String.valueOf(retCode).substring(0, 3));
    log.error("[网关异常处理]请求路径: {},异常信息: {}, 服务未找到",
            exchange.getRequest().getPath(), ex.getMessage());
    return webFluxResponseWriter(response, HttpStatus.valueOf(httpCode), errMsg, retCode);
}
```

### 异常分类

| 异常类型 | 响应码 | HTTP 状态码 | 含义 |
| --- | --- | --- | --- |
| `NoResourceFoundException` | `404000`（SERVICE_NOT_FOUND） | 404 | 找不到匹配的下游服务 / 资源 |
| `ServiceException` | 取 `ex.getCode()` | 取前 3 位 | 业务自定义异常，使用异常自身的 code 与 message |
| 其他异常 | `500000`（ERROR） | 500 | 兜底处理 |

### 响应体格式

通过 `webFluxResponseWriter` 统一以 `Result.fail(code, errMsg)` 格式返回 JSON：

```java
private static Mono<Void> webFluxResponseWriter(
        ServerHttpResponse response, String contentType, HttpStatus status,
        Object value, int code) {
    response.setStatusCode(status);                                   // 设置 HTTP 状态码
    response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType); // Content-Type: application/json
    Result<?> result = Result.fail(code, value.toString());           // 统一响应结构
    DataBuffer dataBuffer = response.bufferFactory()
            .wrap(JsonUtil.classToJson(result).getBytes());
    return response.writeWith(Mono.just(dataBuffer));
}
```

## 配置依赖

网关自身配置仅 `bootstrap.yml`，所有业务相关配置均托管在 Nacos，并通过 `shared-configs` 共享其他模块的配置。

### bootstrap.yml

```yaml
spring:
  output:
    ansi:
      enabled: ALWAYS
  application:
    name: zmbdp-gateway-service
  profiles:
    active: ${RUN_ENV}
  main:
    web-application-type: reactive
    allow-bean-definition-overriding: true
  cloud:
    nacos:
      discovery:
        username: nacos
        password: Hf@173503494
        namespace: scaffold-ai-assistant-${RUN_ENV}
        server-addr: ${NACOS_ADDR}
      config:
        username: nacos
        password: Hf@173503494
        namespace: scaffold-ai-assistant-${RUN_ENV}
        server-addr: ${NACOS_ADDR}
        file-extension: yaml
        shared-configs:
          - data-id: share-redis-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
            refresh: true
          - data-id: share-token-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
            refresh: true
          - data-id: share-log-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
            refresh: true
          - data-id: share-monitor-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
            refresh: true
```

### 共享配置依赖

| 共享配置 | 作用 | 网关中的用途 |
| --- | --- | --- |
| `share-redis-{env}.yaml` | Redis 连接信息 | `RedisService` 用于校验 `logintoken:{userKey}` 是否存在 |
| `share-token-{env}.yaml` | `jwt.token.secret` | `AuthFilter` 解析 JWT 的密钥 |
| `share-log-{env}.yaml` | 日志配置 | 日志格式、traceId 输出、操作日志策略等 |
| `share-monitor-{env}.yaml` | Actuator / Prometheus / SkyWalking | 监控指标暴露与链路追踪接入 |

### 环境变量

`bootstrap.yml` 中通过环境变量注入：

- `RUN_ENV`：当前运行环境（如 `dev`、`test`、`prd`），决定 Nacos namespace 与共享配置后缀。
- `NACOS_ADDR`：Nacos 服务地址。

## 注意事项

1. **WebFlux 响应式编程**：网关 `spring.main.web-application-type=reactive`，所有过滤器返回 `Mono<Void>`，禁止使用阻塞 IO（如 `ThreadLocal`、`RequestContextHolder` 等 Servlet API）。`TraceFilter` 在 `doFinally` 中清理 MDC，正是为了适配响应式线程复用机制。
2. **网关自身不依赖数据库**：启动类 `ZmbdpGatewayServiceApplication` 显式 `exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class}`，无需配置数据源、无需 MyBatis / JDBC 依赖。
3. **仅需 Redis**：网关唯一的中间件依赖是 Redis，用于校验登录态是否存在。JWT 本身不携带过期时间，过期完全由 Redis Key 的 TTL 控制。
4. **白名单支持热刷新**：`IgnoreWhiteProperties` 与 `AuthFilter` 均标注 `@RefreshScope`，在 Nacos 修改 `security.ignore.whites` 后无需重启网关即可生效。
5. **下游 header 需 URL 解码**：网关透传的 `user_key` / `user_id` / `username` / `user_from` 已经过 URL 编码，下游服务读取时需调用 `URLDecoder.decode` 还原。
6. **B/C 端隔离仅做 B 端强校验**：只有当 URL 包含 `sys_user` 时才校验 `user_from == sys`；C 端接口（路径含 `app_user` 等）不做反向校验，由下游业务自行判断。
7. **HTTP 状态码取自 ResultCode 前 3 位**：`AuthFilter` 与 `GatewayExceptionHandler` 都遵循该规则——业务码前 3 位即 HTTP 状态码（`401xxx` → 401、`404xxx` → 404、`500xxx` → 500）。
8. **SkyWalking 优先**：若部署时挂载 SkyWalking Agent，`TraceFilter` 会复用 Agent 注入的 traceId，避免与 SkyWalking 链路出现双 traceId。
