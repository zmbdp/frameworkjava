# 操作日志

## 概述

操作日志通过 **AOP + 注解** 实现，自动记录业务操作信息，包括操作描述、参数、返回值、异常、用户信息、IP、耗时等。支持 **SpEL 表达式
**、**敏感字段脱敏**、**异步处理** 和 **可扩展存储**。

## 核心组件

- `@LogAction`：标记需要记录日志的方法
- `LogActionAspect`：切面，负责日志收集和处理
- `ILogStorageService`：日志存储服务接口（可扩展）
- `DefaultLogStorageService`：默认存储实现（输出到日志文件）
- `DesensitizeUtil`：敏感字段脱敏工具

## 使用方式

### 1. 引入依赖

在需要使用操作日志的服务模块（如 portal、admin）的 `pom.xml` 中增加：

```xml
<dependency>
    <groupId>com.zmbdp</groupId>
    <artifactId>zmbdp-common-log</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 引用 Nacos 配置

在对应服务的 `bootstrap.yml` 的 `spring.cloud.nacos.config.shared-configs` 中增加（与 `share-idempotent` 等一起）：

```yaml
- data-id: share-log-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
  refresh: true
```

### 3. 基础使用

#### 3.1 方法注解（推荐）

在需要记录日志的方法上添加 `@LogAction` 注解：

```java
@PostMapping("/user/add")
@LogAction("新增用户")
public Result<String> addUser(@RequestBody UserDTO dto) {
    return Result.success("用户创建成功");
}
```

#### 3.2 类注解（辅助）

在类上添加 `@LogAction` 注解，作为默认配置：

```java
@RestController
@RequestMapping("/user")
@LogAction(recordParams = true, recordException = true, module = "用户管理")
public class UserController {

    // 方法1：使用类注解的默认配置，只需要设置操作描述
    @PostMapping("/add")
    @LogAction("新增用户")
    public Result<String> addUser(@RequestBody UserDTO dto) {
        return Result.success("用户创建成功");
    }

    // 方法2：覆盖类注解的配置
    @PostMapping("/delete")
    @LogAction(value = "删除用户", recordParams = false)
    public Result<String> deleteUser(@RequestParam Long id) {
        return Result.success("删除成功");
    }
}
```

**优先级规则（三层策略）：**

- **方法注解是核心**：必须存在才能记录日志，用于记录具体业务操作（value 必填）
- **类注解是辅助**：提供默认策略，如全局开启参数记录、异常记录等
- **Nacos 全局配置是兜底**：提供全局默认值，可通过配置中心动态调整
- **合并策略**：
    - 方法注解 > 类注解 > Nacos 全局默认策略
    - 方法注解的配置优先，完全覆盖类注解和全局配置
    - 如果方法注解的某个属性为默认值，使用类注解的值
    - 如果类注解也不存在或为默认值，使用 Nacos 全局默认配置
    - 例如：类注解设置了 `recordParams = true`，方法注解未设置（默认 false），最终会使用类注解的 `true`

### 4. 记录参数和返回值

```java
@PostMapping("/order/create")
@LogAction(value = "创建订单", recordParams = true, recordResult = true)
public Result<OrderVO> createOrder(@RequestBody OrderDTO dto) {
    OrderVO order = orderService.create(dto);
    return Result.success(order);
}
```

### 5. 条件记录

使用 SpEL 表达式控制是否记录日志：

```java
@PostMapping("/order/update")
@LogAction(
    value = "更新订单状态",
    recordParams = true,
    recordResult = true,
    condition = "#result.success == true"
)
public Result<String> updateOrder(@RequestBody OrderDTO dto) {
    return Result.success("更新成功");
}
```

### 6. SpEL 表达式记录特定字段

```java
@PostMapping("/user/edit")
@LogAction(
    value = "编辑用户",
    recordParams = true,
    paramsExpression = "{'userId': #userDTO.userId, 'userName': #userDTO.userName}"
)
public Result<String> editUser(@RequestBody UserDTO userDTO) {
    return Result.success("编辑成功");
}
```

### 7. 敏感字段脱敏

```java
@PostMapping("/user/register")
@LogAction(
    value = "用户注册",
    recordParams = true,
    desensitizeFields = "password,phone"
)
public Result<String> register(@RequestBody UserDTO dto) {
    // password 和 phone 字段会自动脱敏
    return Result.success("注册成功");
}
```

### 8. 指定业务模块和类型

```java
@PostMapping("/product/add")
@LogAction(
    value = "新增商品",
    module = "商品管理",
    businessType = "商品操作",
    recordParams = true
)
public Result<String> addProduct(@RequestBody ProductDTO dto) {
    return Result.success("商品创建成功");
}
```

## 配置说明

### Nacos 配置

在 Nacos 配置中心添加以下配置（`share-log-{env}.yaml`）：

```yaml
# 日志功能全局配置
log:
  # 是否启用日志功能（默认：true）
  enabled: true
  
  # 是否启用异步处理（默认：true）
  async-enabled: true
  
  # 全局默认策略（三层策略的兜底配置）
  # 注意：日志存储方式通过实现 ILogStorageService 接口自定义，当前版本默认实现为输出到日志文件（SLF4J）
  default:
    # 全局默认是否记录参数（默认：false）
    record-params: false
    # 全局默认是否记录返回值（默认：false）
    record-result: false
    # 全局默认是否记录异常（默认：true）
    record-exception: true
    # 全局默认异常时是否抛出异常（默认：true）
    throw-exception: true
```

**配置优先级说明：**

- 方法注解 > 类注解 > Nacos 全局默认策略
- 方法注解的配置优先，如果方法注解的某个属性为默认值，使用类注解的值
- 如果类注解也不存在或为默认值，使用 Nacos 全局默认配置

### 存储类型选择

支持5种存储方式，可通过配置或注解选择：

#### 1. 控制台存储（默认）

```yaml
log:
  storage-type: console  # 默认值，输出到日志文件（SLF4J）
```

#### 2. 文件存储

```yaml
log:
  storage-type: file
  file:
    path: ./logs/operation.log  # 日志文件路径
```

#### 3. Redis 存储

需要引入 `zmbdp-common-redis` 依赖：

```yaml
log:
  storage-type: redis
  redis:
    expire-time: 604800  # 过期时间（秒，默认7天）
```

#### 4. 消息队列存储

需要引入 `zmbdp-common-rabbitmq` 依赖：

```yaml
log:
  storage-type: mq
  mq:
    queue-name: log.operation.queue  # 队列名称
```

#### 5. 数据库存储（需要自定义实现）

```yaml
log:
  storage-type: database
```

然后实现自定义的数据库存储服务：

```java
@Service
@Primary
@ConditionalOnProperty(name = "log.storage-type", havingValue = "database")
public class CustomDatabaseLogStorageService implements ILogStorageService {
    
    @Autowired
    private OperationLogMapper logMapper;
    
    @Override
    public void save(OperationLogDTO logDTO) {
        OperationLog log = new OperationLog();
        BeanCopyUtil.copyProperties(logDTO, log);
        logMapper.insert(log);
    }
}
```

#### 方法级别指定存储类型

```java
// 方法级别指定存储方式
@LogAction(value = "新增用户", storageType = "database")
public Result<String> addUser(@RequestBody UserDTO dto) {
    return Result.success("用户创建成功");
}
```

## 注解参数说明

| 参数                | 类型      | 必填 | 默认值   | 说明             |
|-------------------|---------|----|-------|----------------|
| value             | String  | 是  | -     | 操作描述           |
| recordParams      | boolean | 否  | false | 是否记录方法入参       |
| recordResult      | boolean | 否  | false | 是否记录方法返回值      |
| recordException   | boolean | 否  | true  | 是否记录异常信息       |
| throwException    | boolean | 否  | true  | 异常时是否抛出异常      |
| condition         | String  | 否  | ""    | 条件表达式（SpEL）    |
| paramsExpression  | String  | 否  | ""    | 参数记录表达式（SpEL）  |
| resultExpression  | String  | 否  | ""    | 返回值记录表达式（SpEL） |
| module            | String  | 否  | ""    | 业务模块           |
| businessType      | String  | 否  | ""    | 业务类型           |
| desensitizeFields | String  | 否  | ""    | 需要脱敏的字段（逗号分隔）  |

## SpEL 表达式

### 可用变量

- `#result`：方法返回值
- `#参数名`：方法参数（如 `#userDTO`、`#orderId`）
- `args[0]`、`args[1]`：方法参数数组

### 示例

```java
// 条件表达式：只有成功时才记录
@LogAction(value = "更新订单", condition = "#result.success == true")

// 参数表达式：只记录用户ID和用户名
@LogAction(
    value = "编辑用户",
    paramsExpression = "{'userId': #userDTO.userId, 'userName': #userDTO.userName}"
)

// 返回值表达式：只记录订单ID
@LogAction(
    value = "创建订单",
    recordResult = true,
    resultExpression = "#result.data.id"
)
```

## 脱敏类型

支持的脱敏类型：

- `phone`：手机号（保留前3位和后4位，如：138****5678）
- `idCard`：身份证号（保留前6位和后4位，如：110101********1234）
- `email`：邮箱（保留@前3位和@后全部，如：abc***@example.com）
- `password`：密码（全部替换为*）
- `bankCard`：银行卡号（保留前4位和后4位，如：6222****1234）

## 工作原理

```
请求 → 拦截方法 → 解析注解配置 → 执行条件表达式
                          ↓
                    条件满足 → 记录方法参数 → 执行原方法
                          ↓
                    执行成功 → 记录返回值 → 计算耗时 → 异步保存日志
                          ↓
                    执行失败 → 记录异常信息 → 计算耗时 → 异步保存日志
```

## 注意事项

1. 注解可以标注在方法上或类上，支持同时使用
2. 方法注解优先级高于类注解，方法注解存在时完全使用方法注解的配置
3. 类注解的 `value` 通常不设置（为空），仅作为默认配置使用
4. 如果类注解设置了 `value`，该类下所有方法都会使用该操作描述（除非方法注解覆盖）
5. SpEL 表达式执行失败时会降级使用默认值
6. 异步处理默认启用，可通过配置 `log.async-enabled` 控制
7. 无 HTTP 请求上下文时（如内部调用、单元测试）会跳过部分信息收集（IP、User-Agent 等）
8. 日志存储失败不会影响业务逻辑，只记录错误日志
9. 默认实现将日志输出到日志文件（SLF4J），生产环境建议实现数据库存储

## 最佳实践

1. **关键操作必记录**：重要业务操作（如新增、删除、修改）建议记录日志
2. **合理使用条件记录**：避免记录过多无用日志，使用条件表达式过滤
3. **敏感字段必脱敏**：涉及用户隐私的字段（如密码、手机号）必须脱敏
4. **自定义存储方式**：生产环境建议实现数据库存储，便于查询和分析
5. **异步处理**：高并发场景建议启用异步处理，避免影响业务性能
6. **合理控制日志量**：避免记录过大对象，使用 SpEL 表达式只记录关键字段
