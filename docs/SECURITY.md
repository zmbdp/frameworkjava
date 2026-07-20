# 安全认证与异常处理

frameworkJava 采用 **JWT + Redis** 双层机制实现身份认证：JWT 携带用户基础信息作为访问凭证，Redis 存储完整的登录上下文并控制 Token 生命周期，配合全局异常处理器统一对外错误响应，构成完整的安全认证与异常处理体系。

## 一、概述

### 设计目标

- **无状态 + 有状态结合**：JWT 自包含用户标识（无状态），Redis 保存完整用户上下文并支持主动失效（有状态）
- **B 端 / C 端隔离**：通过 `userFrom` 字段区分用户来源，网关层校验防止跨端滥用 Token
- **自动续期**：活跃用户的 Token 临近过期时自动续期，无需重新登录
- **统一异常出口**：所有异常通过 `GlobalExceptionHandler` 转换为标准 `Result` 响应

### 核心组件

| 组件 | 所在类 | 职责 |
| --- | --- | --- |
| JWT 工具 | `JwtUtil` | Token 的创建、解析、字段提取 |
| 安全工具 | `SecurityUtil` | 从请求头提取 Token、处理前缀 |
| Token 服务 | `TokenService` | Token 生命周期管理（创建、验证、续期、删除） |
| 登录上下文 | `LoginUserDTO` | 封装 Redis 中存储的登录用户信息 |
| Token 出参 | `TokenDTO` | 登录成功后返回给前端的 Token 信息 |
| 全局异常处理 | `GlobalExceptionHandler` | 统一异常拦截与错误码映射 |

### 模块结构

```
zmbdp-common/zmbdp-common-security
└── src/main/java/com/zmbdp/common/security
    ├── domain/dto
    │   ├── LoginUserDTO.java        # 登录用户上下文
    │   └── TokenDTO.java            # Token 出参
    ├── handler
    │   └── GlobalExceptionHandler.java
    ├── service
    │   └── TokenService.java
    └── utils
        ├── JwtUtil.java
        └── SecurityUtil.java
```

`GlobalExceptionHandler` 与 `TokenService` 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动装配，引入 `zmbdp-common-security` 依赖即可生效：

```
com.zmbdp.common.security.handler.GlobalExceptionHandler
com.zmbdp.common.security.service.TokenService
```

## 二、JWT 工具（JwtUtil）

`com.zmbdp.common.security.utils.JwtUtil` 是无实例工具类（`@NoArgsConstructor(access = AccessLevel.PRIVATE)`），基于 `io.jsonwebtoken` 实现，统一使用 **HS512** 算法签名。

### 算法与密钥

- **签名算法**：`SignatureAlgorithm.HS512`
- **密钥构建**：`Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`
- **密钥要求**：建议长度至少 64 字符（HS512 安全强度要求）
- **密钥来源**：由调用方传入，全项目统一从配置项 `jwt.token.secret` 读取

### 核心方法

| 方法签名 | 作用 |
| --- | --- |
| `String createToken(Map<String, Object> claims, String secret)` | 根据 Claims 生成 JWT Token |
| `Claims parseToken(String token, String secret)` | 解析 Token 并验证签名，返回 Claims |
| `String getUserKey(String token, String secret)` | 从 Token 提取 `user_key`（UUID） |
| `String getUserId(String token, String secret)` | 从 Token 提取 `user_id` |
| `String getUserName(String token, String secret)` | 从 Token 提取 `username` |
| `String getUserFrom(String token, String secret)` | 从 Token 提取 `user_from` |
| `String getUserKey(Claims claims)` 等 | 从已解析的 Claims 提取字段，避免重复解析 |

> 当 Claims 中字段不存在或值为 `null` 时，统一返回空字符串 `""`（由私有方法 `getValue(Claims, String)` 保证）。

### 关键源码

```java
public static String createToken(Map<String, Object> claims, String secret) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return Jwts.builder().setClaims(claims).signWith(key, SignatureAlgorithm.HS512).compact();
}

public static Claims parseToken(String token, String secret) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
}
```

### 注意事项

- Token 中数据以 Base64 编码存储（明文可见），**禁止存放密码等敏感信息**
- Token 本身不携带过期时间，有效期完全由 Redis 控制
- 签名验证失败或格式错误时，`parseToken` 会抛出 `io.jsonwebtoken.JwtException`
- 若需从同一个 Token 提取多个字段，应先调用 `parseToken` 获取 `Claims`，再使用 `getXxx(Claims)` 系列方法，避免重复解析

## 三、安全工具（SecurityUtil）

`com.zmbdp.common.security.utils.SecurityUtil` 负责从 HTTP 请求中提取 Token，并处理可能的 `Bearer ` 前缀。

### 核心方法

| 方法签名 | 作用 |
| --- | --- |
| `String getToken()` | 从当前线程的请求中提取 Token（内部调用 `ServletUtil.getRequest()`） |
| `String getToken(HttpServletRequest request)` | 从指定请求头 `Authorization` 中提取 Token |
| `String replaceTokenPrefix(String token)` | 移除 `Bearer ` 前缀，无前缀时原样返回 |

### 关键源码

```java
public static String getToken(HttpServletRequest request) {
    String token = request.getHeader(SecurityConstants.AUTHENTICATION);
    return replaceTokenPrefix(token);
}

public static String replaceTokenPrefix(String token) {
    // 假如前端设置了令牌的前缀，需要替换裁剪
    if (StringUtil.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
        // 把前缀换成空串
        token = token.replaceFirst(TokenConstants.PREFIX, "");
    }
    return token;
}
```

### 涉及的常量

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `SecurityConstants.AUTHENTICATION` | `Authorization` | 请求头名称 |
| `TokenConstants.PREFIX` | `Bearer ` | Token 前缀（含尾部空格） |

## 四、Token 服务（TokenService）

`com.zmbdp.common.security.service.TokenService` 是 `@Component`，自动注入 `RedisService`，是 Token 生命周期的核心管理者。

### 关键常量

```java
private final static long MILLIS_SECOND = 1000;
private final static long MILLIS_MINUTE = 60 * MILLIS_SECOND;
// 续期阈值：120 分钟（CacheConstants.REFRESH_TIME * MILLIS_MINUTE）
private final static Long MILLIS_MINUTE_TEN = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;
// 过期时间：720 分钟（CacheConstants.EXPIRATION）
private final static Long EXPIRE_TIME = CacheConstants.EXPIRATION;
// Redis Key 前缀（TokenConstants.LOGIN_TOKEN_KEY）
private final static String ACCESS_TOKEN = TokenConstants.LOGIN_TOKEN_KEY;
```

对应常量定义：

| 常量来源 | 字段 | 值 |
| --- | --- | --- |
| `CacheConstants.EXPIRATION` | Token 过期时间 | `720`（分钟） |
| `CacheConstants.REFRESH_TIME` | 自动续期阈值 | `120`（分钟） |
| `TokenConstants.LOGIN_TOKEN_KEY` | Redis Key 前缀 | `logintoken:` |

### Redis Key 设计

- **格式**：`logintoken:{uuid}`
- **Value**：`LoginUserDTO` 序列化后的 JSON
- **TTL**：720 分钟
- **构建方法**：`getTokenKey(String token)` → `ACCESS_TOKEN + token`

```
logintoken:32a8b3e4-5668-4a70-b876-fac8c0150f3c   →   LoginUserDTO{...}   TTL=720min
```

### Token 生命周期

```
登录请求
   ↓
createToken()
   ├─ 生成 UUID 作为 user_key
   ├─ 设置到 loginUserDTO.token
   ├─ refreshToken() → 写入 Redis（TTL 720min）
   ├─ 构建 Claims（user_key/user_id/username/user_from）
   ├─ JwtUtil.createToken() 生成 JWT
   └─ 返回 TokenDTO{accessToken, expires=720}
   ↓
后续请求（携带 Authorization: Bearer <jwt>）
   ↓
getLoginUser(token, secret)
   ├─ JwtUtil.getUserKey(token, secret)  解析出 uuid
   ├─ getTokenKey(uuid) → logintoken:{uuid}
   ├─ redisService.getCacheObject(...) → LoginUserDTO
   └─ 返回用户上下文
   ↓
verifyToken(loginUserDTO)    ← 在拦截器中调用
   ├─ expireTime - currentTime <= 120min ?
   ├─ 是 → refreshToken() 续期到 720min
   └─ 否 → 不操作
   ↓
delLoginUser(token, secret)  ← 用户登出 / 强制下线
   └─ redisService.deleteObject(logintoken:{uuid})
```

### 核心方法

#### 1. 创建 Token

```java
public TokenDTO createToken(LoginUserDTO loginUserDTO, String secret) {
    // 给用户创建一个 uuid 作为唯一标识
    String token = UUID.randomUUID().toString();
    loginUserDTO.setToken(token); // 设置用户的唯一标识
    // 设置到缓存中
    refreshToken(loginUserDTO);
    // 生成原始数据的声明
    Map<String, Object> claimsMap = new HashMap<>();
    claimsMap.put(SecurityConstants.USER_KEY, token);
    claimsMap.put(SecurityConstants.USER_ID, loginUserDTO.getUserId());
    claimsMap.put(SecurityConstants.USERNAME, loginUserDTO.getUserName());
    claimsMap.put(SecurityConstants.USER_FROM, loginUserDTO.getUserFrom());
    // 创建令牌返回
    TokenDTO tokenDTO = new TokenDTO();
    tokenDTO.setAccessToken(JwtUtil.createToken(claimsMap, secret));
    tokenDTO.setExpires(EXPIRE_TIME);
    return tokenDTO;
}
```

- **入参**：`LoginUserDTO`（需已填充 userId/userName/userFrom）、`secret`
- **出参**：`TokenDTO`，其中 `accessToken` 为 JWT，`expires` 为 `720`（分钟）
- **副作用**：将 `LoginUserDTO`（含 uuid、登录时间、过期时间）写入 Redis

#### 2. 刷新缓存（私有）

```java
private void refreshToken(LoginUserDTO loginUserDTO) {
    loginUserDTO.setLoginTime(System.currentTimeMillis());
    // 表示设置用户的过期时间是，用户当前登陆的时间加上 720 * 1 分钟的时间，就是 720 分钟
    loginUserDTO.setExpireTime(loginUserDTO.getLoginTime() + EXPIRE_TIME * MILLIS_MINUTE);
    // 根据随机产生用户标识生成 redis 的 key
    String userKey = getTokenKey(loginUserDTO.getToken());
    // 生成 loginUserDTO 缓存, 720 单位分钟
    redisService.setCacheObject(userKey, loginUserDTO, EXPIRE_TIME, TimeUnit.MINUTES);
}
```

- `loginTime` = 当前时间（毫秒）
- `expireTime` = `loginTime + 720 * 60000` 毫秒（即 12 小时后）
- Redis TTL = 720 分钟

#### 3. 验证并自动续期

```java
public void verifyToken(LoginUserDTO loginUserDTO) {
    // 原先设定好的过期的时间
    long expireTime = loginUserDTO.getExpireTime();
    // 现在的时间
    long currentTime = System.currentTimeMillis();
    // 如果说设定好的时间减去现在的时间在 120 分钟之内的话就续期
    if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
        // 刷新缓存
        refreshToken(loginUserDTO);
    }
}
```

- **续期条件**：`expireTime - currentTime <= 120 分钟`（7,200,000 毫秒）
- **续期效果**：重置 Redis TTL 为 720 分钟，并更新 `loginTime` / `expireTime`
- **使用场景**：在拦截器中每次请求都调用，保证活跃用户 Token 不过期

#### 4. 获取登录用户

提供三个重载：

```java
// 1. 直接传入 JWT
public LoginUserDTO getLoginUser(String token, String secret)

// 2. 传入 HttpServletRequest，内部调用 SecurityUtil.getToken(request)
public LoginUserDTO getLoginUser(HttpServletRequest request, String secret)

// 3. 便捷方法，从当前线程请求中获取
public LoginUserDTO getLoginUser(String secret)
```

执行流程：解析 JWT 取出 `user_key` → 拼接 Redis Key → 从 Redis 读取 `LoginUserDTO`。Token 为空返回 `null`；解析或读取异常时记录 `warn` 日志并抛出 `RuntimeException("获取用户信息异常")`。

#### 5. 删除登录状态

提供两个重载：

```java
// 1. 根据 JWT 删除（用户主动登出）
public void delLoginUser(String token, String secret)

// 2. 根据 userId + userFrom 删除（管理员强制下线）
public void delLoginUser(Long userId, String userFrom)
```

第二个重载会扫描所有 `logintoken:*` 的 Key，逐个比对 `userId` 与 `userFrom`，**两者必须同时匹配**才会删除。典型场景：B 端管理员禁用某账号后调用 `delLoginUser(userId, USER_FROM_TU_B)` 实现踢人下线。

#### 6. 设置用户身份信息

```java
public void setLoginUser(LoginUserDTO loginUserDTO) {
    if (loginUserDTO != null && StringUtil.isNotEmpty(loginUserDTO.getToken())) {
        refreshToken(loginUserDTO);
    }
}
```

用于更新 Redis 中的用户上下文（例如修改昵称后立即生效），会重置 TTL 为 720 分钟。

## 五、用户来源机制（userFrom）

`userFrom` 是 frameworkJava 实现 **B 端 / C 端隔离** 的核心字段，存储在 JWT Claims 与 Redis 中的 `LoginUserDTO` 内。

### 取值约定

| 常量 | 字段值 | 含义 |
| --- | --- | --- |
| `UserConstants.USER_FROM_TU_B` | `sys` | B 端系统用户（管理后台） |
| `UserConstants.USER_FROM_TU_C` | `app` | C 端 App 用户 |

### 各端的设置位置

- **B 端登录**（`SysUserServiceImpl`）：`loginUserDTO.setUserFrom(UserConstants.USER_FROM_TU_B);`
- **C 端登录**（`UserServiceImpl`）：`loginUserDTO.setUserFrom(UserConstants.USER_FROM_TU_C);`

### 网关层的隔离校验

`zmbdp-gateway` 的 `AuthFilter` 在解析 Token 后会校验 `userFrom` 与请求路径的匹配关系：

```java
// 再判断用户来源是否合法
String userFrom = JwtUtil.getUserFrom(claims);
if (
        url.contains(HttpConstants.SYS_USER_PATH) && // 如果路径是系统路径
        !UserConstants.USER_FROM_TU_B.equals(userFrom) // 但是用户不是系统来源的话
                                                              // （相当于 C端用户在 C端拿到的 jwt 想在 B端使用）
) {
    return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
}
```

- `HttpConstants.SYS_USER_PATH` = `"sys_user"`
- 若 URL 包含 `sys_user` 但 `userFrom != "sys"`，则返回 `TOKEN_CHECK_FAILED`（错误码 401004，HTTP 401）
- 通过网关后，`userFrom` 会通过请求头 `user_from` 透传给下游服务

### 强制下线的精确性

`delLoginUser(Long userId, String userFrom)` 必须同时匹配 `userId` 和 `userFrom`，避免误删同 ID 的另一端登录态（理论上 B 端 sys_user 与 C 端 app_user 是不同的 ID 空间，但该机制提供了额外的隔离保障）。

## 六、JWT Claims 字段约定

JWT Payload 中的字段由 `SecurityConstants` 统一定义：

| 字段 Key | 常量 | 值类型 | 含义 |
| --- | --- | --- | --- |
| `user_key` | `SecurityConstants.USER_KEY` | String（UUID） | 用户唯一标识，用于拼接 Redis Key |
| `user_id` | `SecurityConstants.USER_ID` | Long | 用户 ID |
| `username` | `SecurityConstants.USERNAME` | String | 用户名 |
| `user_from` | `SecurityConstants.USER_FROM` | String | 用户来源（`sys` / `app`） |

一个真实的 Token Payload（来自 `TestTokenController`）：

```json
{
  "user_id": 1,
  "user_from": "test",
  "user_key": "32a8b3e4-5668-4a70-b876-fac8c0150f3c",
  "username": "test"
}
```

对应 JWT（HS512 签名）：

```
eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxLCJ1c2VyX2Zyb20iOiJ0ZXN0IiwidXNlcl9rZXkiOiIzMmE4YjNlNC01NjY4LTRhNzAtYjg3Ni1mYWM4YzAxNTBmM2MiLCJ1c2VybmFtZSI6InRlc3QifQ.GsdrAyyc9xORoWZLSe_jp9rKwMLSRUtra2c6zadsve8HWcg4H2H2OWOiSSn5uAlhY4r78uWi3vq-7XcES7kHBQ
```

### 设计要点

- `user_key` 是每次登录生成的随机 UUID，**同一用户多次登录会产生不同 UUID**，从而支持多端同时登录
- `user_id` 在 JWT 中为明文，下游服务可直接从请求头读取（网关 `AuthFilter` 已透传），无需重复解析
- 网关透传后下游服务通过请求头 `user_id` / `username` / `user_from` / `user_key` 接收，**避免每个微服务都持有 JWT 密钥**

## 七、全局异常处理器（GlobalExceptionHandler）

`com.zmbdp.common.security.handler.GlobalExceptionHandler` 使用 `@RestControllerAdvice` 拦截所有控制器异常，统一转换为 `Result` 响应。仅在 Servlet Web 应用生效（`@ConditionalOnWebApplication(type = Type.SERVLET)`）。

### 错误码到 HTTP 状态码的映射规则

```java
private void setResponseCode(HttpServletResponse response, Integer errCode) {
    // 把前面三个拿出来返回给前端，设置 http 响应码
    int httpCode = Integer.parseInt(String.valueOf(errCode).substring(0, 3));
    response.setStatus(httpCode);
}
```

**规则**：取错误码的前三位作为 HTTP 状态码。

| 错误码 | 前三位 | HTTP 状态码 | 含义 |
| --- | --- | --- | --- |
| `400000` | `400` | 400 Bad Request | 无效的参数 |
| `400006` | `400` | 400 Bad Request | 参数类型不匹配 |
| `401004` | `401` | 401 Unauthorized | 令牌验证失败 |
| `404001` | `404` | 404 Not Found | url 未找到 |
| `405000` | `405` | 405 Method Not Allowed | 请求方法不支持 |
| `500000` | `500` | 500 Internal Server Error | 服务繁忙请稍后重试 |

### 支持的 8 种异常类型

| 序号 | 异常类型 | 处理方法 | 错误码 | 错误信息 | HTTP 状态码 |
| --- | --- | --- | --- | --- | --- |
| 1 | `HttpRequestMethodNotSupportedException` | `handleHttpRequestMethodNotSupported` | `405000` | 请求方法不支持 | 405 |
| 2 | `MethodArgumentTypeMismatchException` | `handleMethodArgumentTypeMismatchException` | `400006` | 参数类型不匹配 | 400 |
| 3 | `NoResourceFoundException` | `handleMethodNoResourceFoundException` | `404001` | url 未找到 | 404 |
| 4 | `ServiceException` | `handleServiceException` | 取自异常 `code` | 取自异常 `message` | 取 `code` 前三位 |
| 5 | `MethodArgumentNotValidException` | `handleMethodArgumentNotValidException` | `400000` | 多个错误信息用 `; ` 拼接 | 400 |
| 6 | `ConstraintViolationException` | `handleConstraintViolationException` | `400000` | 取自 `e.getMessage()` | 400 |
| 7 | `RuntimeException` | `handleRuntimeException` | `500000` | 服务繁忙请稍后重试 | 500 |
| 8 | `Exception` | `handleException` | `500000` | 服务繁忙请稍后重试 | 500（兜底） |

### 处理策略说明

1. **请求方式不支持**：记录请求地址与请求方式，返回 `405000`
2. **参数类型不匹配**：例如路径变量期望 `Long` 但传入了字符串，返回 `400006`
3. **URL 未找到**：请求路径不存在，返回 `404001`
4. **业务异常**（`ServiceException`）：使用异常自带的 `code` 和 `message`，HTTP 状态码由 `code` 前三位决定。这是业务代码主动抛出的标准方式
5. **`@Valid` 校验异常**：合并所有 `ObjectError` 的 `defaultMessage`，用 `CommonConstants.DEFAULT_DELIMITER`（即 `"; "`）拼接
6. **`@Validated` 校验异常**：直接使用 `e.getMessage()` 作为错误信息
7. **运行时异常**：兜底处理所有非 `ServiceException` 的 `RuntimeException`，仅返回通用错误信息，**不暴露具体异常细节**，但会记录完整堆栈到日志
8. **系统异常**：最后一道防线，捕获所有未被前面处理器拦截的异常（如 `IOException`、`SQLException`）

### 参数校验异常信息合并源码

```java
private String joinMessage(MethodArgumentNotValidException e) {
    // 先获取所有异常信息的列表
    List<ObjectError> allErrors = e.getAllErrors();
    if (CollectionUtils.isEmpty(allErrors)) {
        return CommonConstants.EMPTY_STR;
    }
    // 流处理获取异常信息
    return allErrors
            .stream() // 获取所有错误信息
            .map(ObjectError::getDefaultMessage) // 获取所有错误信息
            .collect(Collectors.joining(CommonConstants.DEFAULT_DELIMITER)); // 转换成字符串, 用分号隔开
}
```

- `CommonConstants.DEFAULT_DELIMITER` = `"; "`（分号 + 空格）
- `CommonConstants.EMPTY_STR` = `""`

### 业务异常的抛出方式

`ServiceException` 提供三种构造方式，业务代码推荐使用第一种：

```java
// 1. 通过 ResultCode 构造（推荐）
throw new ServiceException(ResultCode.INVALID_PARA);

// 2. 仅消息构造（错误码默认取 ResultCode.ERROR = 500000）
throw new ServiceException("用户不存在");

// 3. 消息 + 错误码定制
throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
```

## 八、配置项

JWT 密钥通过 Nacos 共享配置 `share-token-{env}.yaml` 下发，所有需要解析 Token 的服务（网关、admin、portal 等）均通过 `@Value("${jwt.token.secret}")` 注入。

### 配置文件

`deploy/{env}/res/sql/DEFAULT_GROUP/share-token-{env}.yaml`：

```yaml
jwt:
  token:
    secret: # JWT 密钥
```

### 注入方式

```java
@Value("${jwt.token.secret}")
private String secret;
```

### 使用该配置的位置

| 模块 | 类 | 用途 |
| --- | --- | --- |
| `zmbdp-gateway` | `AuthFilter` | 网关层解析 JWT、校验 userFrom |
| `zmbdp-admin-service` | `SysUserServiceImpl` | B 端登录创建 Token、获取当前登录用户、强制下线 |
| `zmbdp-portal-service` | `UserServiceImpl` | C 端登录创建 Token |
| `zmbdp-portal-service` | 其他需要获取当前登录 C 端用户的业务 Service | 通过 `tokenService.getLoginUser(secret)` 解析当前用户 |
| `zmbdp-common-ratelimit` | `RateLimitAspect` | 限流维度按 userId 区分，需从 Token 提取 userId |
| `zmbdp-common-log` | `UserContextResolver` | 操作日志记录当前用户（可选，配置后启用 Token 解析） |

### 安全要求

- **密钥长度**：至少 64 字符（HS512 算法要求）
- **密钥保密**：禁止提交到代码仓库，仅存放于 Nacos；生产环境通过 Nacos 权限体系隔离访问
- **密钥一致性**：所有服务必须使用同一密钥，否则 Token 跨服务解析会失败
- **密钥稳定性**：更换密钥会导致所有已签发的 Token 失效，需评估业务影响

## 九、LoginUserDTO 字段说明

`com.zmbdp.common.security.domain.dto.LoginUserDTO` 是 Redis 中存储的登录用户上下文，使用 `@Data` 注解。

| 字段 | 类型 | 说明 | 设置时机 |
| --- | --- | --- | --- |
| `token` | `String` | 用户标识（UUID），与 JWT 中的 `user_key` 一致 | `createToken` 时由 `UUID.randomUUID().toString()` 生成 |
| `userId` | `Long` | 用户 ID | 业务层登录时填充 |
| `userFrom` | `String` | 用户来源（`sys` / `app`） | 业务层登录时填充 |
| `userName` | `String` | 用户名（昵称） | 业务层登录时填充 |
| `loginTime` | `Long` | 登录时间（毫秒） | `refreshToken` 中设为 `System.currentTimeMillis()` |
| `expireTime` | `Long` | 过期时间（毫秒） | `refreshToken` 中设为 `loginTime + 720 * 60000` |

### TokenDTO 字段说明

`com.zmbdp.common.security.domain.dto.TokenDTO` 是登录成功后返回给前端的出参。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | `String` | JWT Token 字符串，前端需在后续请求的 `Authorization` 头中携带（可加 `Bearer ` 前缀） |
| `expires` | `Long` | 过期时间，单位：分钟（当前实现固定为 `720`） |

提供 `convertToVO()` 方法，通过 `BeanCopyUtil` 转换为 `TokenVO` 用于接口返回。

## 十、使用示例

### 1. 完整登录流程

以 B 端登录为例（参考 `SysUserServiceImpl`）：

```java
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.token.secret}")
    private String secret;

    @Override
    public TokenDTO login(SysUserLoginDTO loginDTO) {
        // 1. 校验用户名密码（略）
        SysUser sysUser = doLogin(loginDTO);

        // 2. 构建登录上下文
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(sysUser.getId());
        loginUserDTO.setUserName(sysUser.getNickName());
        loginUserDTO.setUserFrom(UserConstants.USER_FROM_TU_B);  // B 端来源

        // 3. 创建 Token，内部已写入 Redis
        return tokenService.createToken(loginUserDTO, secret);
    }
}
```

### 2. 在拦截器中获取并续期 Token

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.token.secret}")
    private String secret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取 Token（自动去除 Bearer 前缀）
        LoginUserDTO user = tokenService.getLoginUser(request, secret);
        if (user == null) {
            // Token 无效或已过期
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        // 2. 自动续期（剩余时间 <= 120 分钟才会真正续期）
        tokenService.verifyToken(user);
        // 3. 将用户上下文放入 ThreadLocal 供后续使用（略）
        return true;
    }
}
```

### 3. 在业务代码中获取当前登录用户

```java
@Service
public class SomeService {

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.token.secret}")
    private String secret;

    public void doSomething() {
        // 便捷方法：内部从当前线程请求中提取 Token
        LoginUserDTO user = tokenService.getLoginUser(secret);
        Long userId = user.getUserId();
        String userFrom = user.getUserFrom();
        // ...
    }
}
```

### 4. 用户登出

```java
@PostMapping("/logout")
public Result<String> logout(HttpServletRequest request) {
    String token = SecurityUtil.getToken(request);
    tokenService.delLoginUser(token, secret);
    return Result.success("登出成功");
}
```

### 5. 管理员强制下线（踢人）

```java
// 禁用用户后立即让其 Token 失效
if (sysUserDTO.getStatus().equals(UserConstants.USER_DISABLE)) {
    tokenService.delLoginUser(sysUserDTO.getUserId(), UserConstants.USER_FROM_TU_B);
}
```

### 6. 业务异常抛出

```java
// 直接抛出，全局异常处理器会自动转换为标准 Result 响应
if (user == null) {
    throw new ServiceException(ResultCode.INVALID_PARA, "用户不存在");
}
```

### 7. 网关层 Token 校验（参考实现）

`zmbdp-gateway` 的 `AuthFilter` 实现了完整的网关侧校验链路，下游服务可借鉴其思路：

```java
// 1. 白名单放行
if (StringUtil.matches(url, ignoreWhiteProperties.getWhites())) {
    return chain.filter(exchange);
}
// 2. 提取 Token
String token = getToken(request);
if (StringUtil.isEmpty(token)) {
    return unauthorizedResponse(exchange, ResultCode.TOKEN_EMPTY);
}
// 3. 解析 JWT
Claims claims = JwtUtil.parseToken(token, secret);
String userKey = JwtUtil.getUserKey(claims);
// 4. 校验 Redis 中是否仍存在（用于主动失效）
if (!redisService.hasKey(getTokenKey(userKey))) {
    return unauthorizedResponse(exchange, ResultCode.LOGIN_STATUS_OVERTIME);
}
// 5. 校验用户来源
String userFrom = JwtUtil.getUserFrom(claims);
if (url.contains(HttpConstants.SYS_USER_PATH)
        && !UserConstants.USER_FROM_TU_B.equals(userFrom)) {
    return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
}
// 6. 透传用户信息到下游
String userId = JwtUtil.getUserId(claims);
String userName = JwtUtil.getUserName(claims);
ServerHttpRequest.Builder mutate = request.mutate();
addHeader(mutate, SecurityConstants.USER_KEY, userKey);
addHeader(mutate, SecurityConstants.USER_ID, userId);
addHeader(mutate, SecurityConstants.USERNAME, userName);
addHeader(mutate, SecurityConstants.USER_FROM, userFrom);
return chain.filter(exchange);
```

## 最佳实践

1. **密钥管理**：JWT 密钥仅存放于 Nacos `share-token-{env}.yaml`，禁止硬编码或提交到代码仓库；不同环境使用不同密钥
2. **Token 透传**：网关解析 Token 后通过请求头透传用户信息，下游服务**不应**再次持有密钥解析 Token，避免密钥扩散
3. **续期调用位置**：`verifyToken` 应在统一的登录拦截器中调用，避免在每个 Controller 中重复调用
4. **异常分层**：业务错误抛 `ServiceException` + 明确 `ResultCode`；参数校验使用 `@Valid` / `@Validated`；不要在业务代码中捕获后自己返回 `Result.fail`，让全局异常处理器统一处理
5. **userFrom 一致性**：登录时设置的 `userFrom` 必须与 `UserConstants` 中的常量一致，避免网关校验误判
6. **强制下线粒度**：调用 `delLoginUser(userId, userFrom)` 时务必传入正确的 `userFrom`，避免误删另一端登录态
7. **敏感信息**：JWT Payload 为 Base64 明文，**严禁**存放密码、手机号等敏感信息
