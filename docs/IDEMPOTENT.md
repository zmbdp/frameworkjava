# 分布式幂等性设计

## 什么是幂等性

**幂等性**是指同一个请求执行一次和执行多次的效果是一样的。

在分布式系统中，由于网络重试、消息重复消费等原因，同一个业务操作可能被多次执行，幂等性控制可以确保业务只被执行一次。

## 应用场景

### HTTP 接口幂等性

- **支付接口**：防止重复支付
- **订单创建**：防止重复下单
- **库存扣减**：防止超卖

### 消息队列幂等性

- **防止消息重复消费**：RabbitMQ 可能重复投递消息，幂等性注解确保相同消息只处理一次
- **防止定时任务重复执行**：定时任务可能被多次触发，幂等性注解确保只执行一次

## 实现方案

FrameworkJava 采用 **AOP + Redis** 实现分布式幂等性控制。

### 核心组件

- `@Idempotent` 注解：标记需要幂等性控制的方法
- `IdempotentAspect`：AOP 切面，拦截方法执行
- Redis：存储幂等性令牌，控制并发

### 工作原理

**防重模式（默认）**：
```
请求 → 获取 Token → 构建 Redis Key → SETNX 获取锁
                          ↓
                    获取成功 → 执行业务 → 更新状态(SUCCESS/FAILED) → 返回
                          ↓
                    获取失败 → 检查状态
                          ├─ PROCESSING → 直接拒绝
                          ├─ SUCCESS → 直接拒绝
                          └─ FAILED → 允许重试（删除Token，继续尝试）
```

**强幂等模式**：
```
请求 → 获取 Token → 构建 Redis Key → 检查缓存结果
                          ↓
                    有缓存结果 → 直接返回
                          ↓
                    无缓存结果 → SETNX 获取锁
                          ↓
                    获取成功 → 执行业务 → 缓存结果 → 更新状态(SUCCESS/FAILED) → 返回
                          ↓
                    获取失败 → 检查状态
                          ├─ SUCCESS → 从缓存获取结果返回
                          ├─ PROCESSING → 轮询等待结果
                          └─ FAILED → 轮询等待结果
```

## 使用方式

### HTTP 接口幂等性

#### 1. 添加注解

```java
@RestController
public class OrderController {
    
    @PostMapping("/order/create")
    @Idempotent(
        expireTime = 300,                   // 过期时间（秒）
        message = "订单正在处理中，请勿重复提交"
    )
    public Result createOrder(@RequestBody OrderRequest request) {
        // 业务逻辑
        // 客户端需要在请求头传递：Idempotent-Token: xxx
        return Result.success();
    }
}
```

#### 2. 客户端传递幂等性令牌

**方式一：请求头传递**

```javascript
// 前端请求
fetch('/api/order/create', {
    method: 'POST',
    headers: {
        'Idempotent-Token': 'unique-token-12345'  // 客户端生成的唯一令牌
    },
    body: JSON.stringify({ orderId: '123' })
});
```

**方式二：请求参数传递**

```java
@Idempotent(
    allowParam = true,              // 允许从请求参数获取
    paramName = "idempotentToken",  // 参数名称
    expireTime = 300
)
public Result createOrder(@RequestParam String idempotentToken) {
    // 客户端请求：POST /order/create?idempotentToken=xxx
}
```

**方式三：SpEL 表达式（从请求对象获取）**

```java
@Idempotent(
    tokenExpression = "#request.orderId",  // 从请求对象的 orderId 字段获取
    expireTime = 300
)
public Result createOrder(@RequestBody OrderRequest request) {
    // request.orderId 作为幂等性 Token
}
```

### 消息队列幂等性

```java
@RabbitListener(queues = "order.queue")
@Idempotent(
    tokenExpression = "#message.orderId",  // 从消息对象的 orderId 字段获取
    expireTime = 600
)
public void handleOrderMessage(OrderMessage message) {
    // 处理订单消息
    // message.orderId 作为幂等性 Token
}
```

## 配置说明

在 Nacos 配置 `share-idempotent-{env}.yaml`：

```yaml
idempotent:
  # Redis Key 前缀（可选，默认值：idempotent:token:）
  key-prefix: "idempotent:token:"
  
  # Token 过期时间（秒，全局配置）
  # 优先级：注解值 > 全局配置 > 默认值（300秒）
  expire-time: 300
  
  # 是否启用强幂等模式（全局配置）
  # true: 强幂等模式 - 重复请求返回第一次的结果
  # false: 防重模式 - 重复请求直接报错（默认）
  return-cached-result: false
  
  # 强幂等模式：等待结果的最大重试次数（可选，默认值：3）
  # 总等待时间 = max-retry-count × retry-interval-ms（默认：3 × 100ms = 300ms）
  max-retry-count: 3
  
  # 强幂等模式：每次重试的等待时间（毫秒，可选，默认值：100）
  retry-interval-ms: 100
```

## 高并发优化

### 1. 分布式锁优化

- 使用 **Redis SETNX** 原子操作实现分布式锁
- 设置 PROCESSING 状态，标识正在执行
- 支持锁超时（通过 expireTime 控制），避免死锁

### 2. 结果缓存（强幂等模式）

- 第一次请求执行完成后，将结果缓存到 Redis
- 后续相同请求直接返回缓存结果，不重复执行业务
- 结果缓存时间与 Token 过期时间相同

### 3. 退避等待机制（防重模式）

- 获取锁失败时，采用指数退避策略等待
- 避免忙等打爆 Redis 和 CPU
- 最大等待时间 300ms，重试次数受 max-retry-count 限制

### 4. 轮询等待机制（强幂等模式）

- 检测到正在执行时，轮询等待结果
- 支持配置最大重试次数和等待间隔
- **注意**：HTTP 场景下会占用 Tomcat 线程，高并发时建议仅用于 MQ 场景

## 注意事项

### 1. 幂等性 Token 设计

- **唯一性**：确保同一个业务操作的 Token 相同
- **生成方式**：客户端生成唯一 Token（UUID、雪花算法等）
- **传递方式**：支持请求头、请求参数、SpEL 表达式、MQ 消息头
- **粒度**：根据业务需求选择合适的粒度
  - 用户级别：`user:${userId}:order:${orderId}`
  - 订单级别：`order:${orderId}`
  - 全局级别：`global:${operation}`

### 2. 过期时间设置

- **HTTP 接口**：建议 5-10 分钟
- **消息队列**：建议 10-30 分钟
- **定时任务**：建议 1-24 小时

### 3. 异常处理

- **防重模式**：
  - 业务执行失败：更新状态为 FAILED，允许重试（删除 Token）
  - 业务执行成功：更新状态为 SUCCESS，拒绝重复请求
- **强幂等模式**：
  - 业务执行失败：更新状态为 FAILED，删除结果缓存，允许重试
  - 业务执行成功：更新状态为 SUCCESS，缓存结果，后续请求返回缓存结果

### 4. 性能考虑

- Redis 连接池配置
- 锁粒度控制
- 避免长时间持有锁

## 最佳实践

1. **幂等性 Key 生成规则**：使用业务唯一标识（订单号、用户ID等）
2. **客户端令牌生成**：使用 UUID 或雪花算法生成唯一令牌
3. **结果缓存**：对于查询类接口，缓存查询结果
4. **监控告警**：监控幂等性拦截次数，及时发现异常
5. **日志记录**：记录幂等性拦截日志，便于问题排查

## 常见问题

### Q: 如何保证幂等性 Token 的唯一性？

A: 客户端生成唯一 Token（UUID、雪花算法等），或使用业务唯一标识（如订单号、用户ID + 操作类型）作为 Token。

### Q: 幂等性控制会影响性能吗？

A: 影响很小。Redis 操作是微秒级，分布式锁只在并发场景下才会等待。

### Q: 如何处理业务执行失败的情况？

A: 
- **防重模式**：执行失败时更新状态为 FAILED，自动删除 Token 允许重试
- **强幂等模式**：执行失败时更新状态为 FAILED，删除结果缓存，等待的请求会检测到失败状态
