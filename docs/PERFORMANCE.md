# 性能与并发设计

## 性能优化策略

### 1. 缓存优化

- **三级缓存架构**：布隆过滤器 + Caffeine + Redis
- **缓存预热**：系统启动时加载热点数据
- **缓存更新策略**：Cache Aside 模式

详见 [三级缓存架构](CACHE_ARCHITECTURE.md)

### 2. 数据库优化

- **索引优化**：为常用查询字段建立索引
- **分页查询**：使用 MyBatis-Plus 分页插件
- **批量操作**：使用批量插入/更新减少数据库交互
- **连接池配置**：使用 HikariCP 连接池，通过 Nacos 动态配置

#### 数据库连接池配置说明

连接池配置位于 Nacos 配置中心，配置文件：`share-mysql-{env}.yaml`

**配置参数：**

```yaml
spring:
  datasource:
    hikari:
      # 连接池大小配置（根据实际并发量调整）
      maximum-pool-size: 20          # 最大连接数，建议：CPU核心数 * 2 + 磁盘数
      minimum-idle: 5                # 最小空闲连接数，建议：maximum-pool-size 的 1/4
      
      # 连接超时配置
      connection-timeout: 30000      # 获取连接超时时间（毫秒）
      idle-timeout: 600000           # 空闲连接超时时间（毫秒，10分钟）
      max-lifetime: 1800000          # 连接最大生命周期（毫秒，30分钟）
      
      # 性能优化配置
      leak-detection-threshold: 60000 # 连接泄漏检测（毫秒，生产环境建议开启）
      pool-name: FrameworkJavaHikariCP # 连接池名称（便于监控和日志追踪）
```

**配置建议：**

- **最大连接数**：根据实际并发量和数据库性能设置，一般建议 `CPU核心数 * 2 + 磁盘数`
- **最小空闲连接数**：建议为最大连接数的 1/4，保证系统启动时有可用连接
- **连接超时**：根据网络环境设置，避免长时间等待
- **连接泄漏检测**：生产环境建议开启，及时发现连接泄漏问题

### 3. 异步处理

- **异步方法**：使用 `@Async` 处理耗时操作
- **消息队列**：使用 RabbitMQ 处理异步任务
- **线程池配置**：通过 Nacos 动态配置线程池参数

#### 线程池配置说明

线程池配置位于 Nacos 配置中心，配置文件：`share-thread-{env}.yaml`

**配置参数：**

```yaml
thread:
  pool-executor:
    # 核心线程数（默认：CPU 核数 + 1）
    corePoolSize: 5
    # 最大线程数（默认：核心线程数 * 2）
    maxPoolSize: 100
    # 阻塞队列大小（默认：100）
    queueCapacity: 100
    # 空闲线程存活时间，单位：秒（默认：60）
    keepAliveSeconds: 60
    # 线程名称前缀（默认：zmbdp-thread-）
    prefixName: zmbdp-thread-
    # 拒绝策略（默认：2 - CallerRunsPolicy）
    # 1: AbortPolicy - 抛异常
    # 2: CallerRunsPolicy - 调用者线程运行
    # 3: DiscardOldestPolicy - 丢弃最老任务
    # 4: DiscardPolicy - 直接丢弃
    rejectHandler: 2
```

**使用方式：**

```java
// 使用 @Async 注解，指定线程池 Bean 名称
@Async(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
public void asyncMethod() {
    // 异步执行的业务逻辑
}
```

**配置建议：**

- **核心线程数**：根据 CPU 核心数和业务类型设置，IO 密集型可设置较大值
- **最大线程数**：建议为核心线程数的 2-4 倍
- **队列大小**：根据业务并发量设置，避免队列过大导致内存占用
- **拒绝策略**：生产环境建议使用 `CallerRunsPolicy`，保证任务不丢失

### 4. 接口优化

- **幂等性控制**：防止重复请求，详见 [分布式幂等性设计](IDEMPOTENT.md)
- **结果缓存**：缓存查询结果，减少数据库压力

## 并发控制

### 1. 分布式锁

使用 **Redisson** 实现分布式锁，项目已封装 `RedissonLockService`：

```java
@Autowired
private RedissonLockService redissonLockService;

public void doSomething() {
    // 方式一：获取带看门狗的锁（自动续期，适合业务执行时间不确定的场景）
    RLock lock = redissonLockService.acquire("lock:key");
    
    // 方式二：获取带固定过期时间的锁（适合业务执行时间确定的场景）
    // RLock lock = redissonLockService.acquire("lock:key", 30);
    
    // 方式三：尝试获取锁（带超时，启用看门狗）
    // RLock lock = redissonLockService.acquire("lock:key", 5, TimeUnit.SECONDS);
    
    if (lock == null) {
        throw new BusinessException("获取锁失败");
    }
    try {
        // 业务逻辑
    } finally {
        // 确保释放锁
        redissonLockService.releaseLock(lock);
    }
}
```

**特性**：
- **看门狗机制**：自动续期，防止业务执行时间过长导致锁过期
- **安全释放**：自动校验锁的持有者，防止误释放其他线程的锁
- **可重入锁**：同一线程可以多次获取同一把锁

### 2. 幂等性控制

使用 `@Idempotent` 注解实现接口幂等性，详见 [分布式幂等性设计](IDEMPOTENT.md)

### 3. 数据库锁

#### 乐观锁

使用版本号字段实现乐观锁：

```java
@Entity
public class Order {
    @Version
    private Integer version;  // 版本号字段
    
    // 更新时自动检查版本号
    public void update() {
        // MyBatis-Plus 会自动在更新时检查 version 字段
        orderMapper.updateById(this);
    }
}
```

#### 悲观锁

使用 `SELECT ... FOR UPDATE` 实现悲观锁：

```java
@Select("SELECT * FROM order WHERE id = #{id} FOR UPDATE")
Order selectByIdForUpdate(Long id);

public void updateWithLock(Long id) {
    Order order = selectByIdForUpdate(id);  // 加锁
    // 执行业务逻辑
    orderMapper.updateById(order);
}
```

## 高并发场景

### 1. 秒杀场景

- **缓存预热**：提前加载商品信息到缓存
- **异步处理**：订单创建异步化
- **库存扣减**：使用 Redis 原子操作、Redisson 分布式锁或数据库乐观锁

#### 库存扣减示例

**方式一：Redis 原子操作**

```java
// 使用 Redis 的 DECR 原子操作
Long stock = redisService.decrement("stock:product:" + productId);
if (stock < 0) {
    // 库存不足，回滚
    redisService.increment("stock:product:" + productId);
    throw new BusinessException("库存不足");
}
```

**方式二：Redisson 分布式锁**

```java
@Autowired
private RedissonLockService redissonLockService;

// 使用 Redisson 分布式锁保证库存扣减的原子性
// 方式 1：尝试获取锁（带超时，启用看门狗自动续期）
RLock lock = redissonLockService.acquire("lock:stock:" + productId, 3, TimeUnit.SECONDS);
// 方式 2：尝试获取锁（带超时和固定过期时间，不启用看门狗）
// RLock lock = redissonLockService.acquire("lock:stock:" + productId, 3, 10, TimeUnit.SECONDS);

if (lock == null) {
    throw new BusinessException("获取锁失败，请稍后重试");
}
try {
    // 获取库存
    Integer stock = redisService.getCacheObject("stock:product:" + productId, Integer.class);
    if (stock == null || stock <= 0) {
        throw new BusinessException("库存不足");
    }
    // 扣减库存
    stock--;
    redisService.setCacheObject("stock:product:" + productId, stock);
} finally {
    // 确保释放锁
    redissonLockService.releaseLock(lock);
}
```

**方式三：数据库乐观锁**

```java
// 使用版本号实现乐观锁
Order order = orderMapper.selectById(orderId);
order.setVersion(order.getVersion() + 1);
int rows = orderMapper.updateById(order);
if (rows == 0) {
    // 版本号冲突，说明已被其他线程修改
    throw new BusinessException("库存已被占用，请重试");
}
```

### 2. 高并发查询

- **多级缓存**：本地缓存 + 分布式缓存
- **缓存穿透防护**：布隆过滤器 + 空值缓存
- **数据库读写分离**：主从复制，读操作走从库

### 3. 消息处理

- **消息幂等性**：使用 `@Idempotent` 注解
- **批量处理**：批量消费消息，提高吞吐量
- **死信队列**：处理失败消息

## 监控指标

### 1. 性能指标

- **响应时间**：P50、P95、P99
- **吞吐量**：QPS、TPS
- **错误率**：4xx、5xx 错误率

### 2. 资源指标

- **CPU 使用率**
- **内存使用率**
- **数据库连接数**
- **Redis 连接数**

### 3. 业务指标

- **缓存命中率**
- **接口调用量**
- **消息消费速率**

## 性能测试

### 1. 测试方式

FrameworkJava 采用 **多线程并发测试** 的方式验证高并发场景下的功能正确性和性能表现。

#### 实现方式

测试代码位于 `zmbdp-mstemplate-service` 模块的 `test` 包下，主要使用：

- **线程池（Executor）**：创建并发请求
- **CountDownLatch**：等待所有线程完成
- **原子计数器（AtomicInteger）**：统计成功数、拒绝数、错误数
- **时间统计**：记录测试耗时

#### 测试示例

```java
// 高并发测试示例（幂等性测试）
@PostMapping("/concurrent")
public Result<Map<String, Object>> testConcurrent(
    @RequestParam(value = "concurrency", defaultValue = "100") int concurrency) {
    
    // 使用线程池并发执行请求
    for (int i = 0; i < concurrency; i++) {
        threadPoolExecutor.execute(() -> {
            // 执行测试请求
            Result<String> response = idempotentTestAPI.testHttpBasicHeader(token);
            // 统计结果
        });
    }
    
    // 等待所有任务完成并统计结果
    latch.await(30, TimeUnit.SECONDS);
    // 返回测试结果：成功数、拒绝数、错误数、耗时等
}
```

#### 测试场景

- **防重模式并发测试**：验证同一 token 并发请求，只有一个成功
- **强幂等模式并发测试**：验证第一个成功后，其他请求返回缓存结果
- **不同 Token 并发测试**：验证不同 token 互不干扰
- **布隆过滤器线程安全测试**：验证高并发下的线程安全性

#### 优缺点

**优点：**
- ✅ 简单直接，无需额外工具
- ✅ 可以精确控制测试逻辑
- ✅ 适合验证功能正确性（如幂等性）
- ✅ 可以集成到代码中，便于持续验证

**局限性：**
- ⚠️ 无法模拟真实网络环境（HTTP 连接、网络延迟等）
- ⚠️ 测试结果受 JVM 线程调度影响
- ⚠️ 难以模拟大规模分布式压测场景
- ⚠️ 缺少专业的性能指标分析（QPS、TPS、响应时间分布等）

### 2. 专业压测工具（推荐）

对于更专业的性能测试，建议使用以下工具：

- **JMeter**：HTTP 接口压测，支持图形化界面和脚本
- **wrk**：命令行压测工具，轻量级、高性能
- **Gatling**：Scala 编写的压测工具，支持复杂场景
- **Apache Bench (ab)**：简单快速的 HTTP 压测工具

#### 使用示例（wrk）

```bash
# 压测示例
wrk -t12 -c400 -d30s --latency http://localhost:8080/api/test
```

### 3. 测试代码位置

所有测试代码位于 `zmbdp-mstemplate-service` 模块：

```
zmbdp-mstemplate-service/src/main/java/com/zmbdp/mstemplate/service/test/
├── TestIdempotentController.java      # 幂等性并发测试
├── TestCacheController.java            # 缓存性能测试
├── TestRedisController.java            # Redis 性能测试
├── TestRedissonController.java         # 分布式锁测试
└── ...                                 # 其他功能测试
```

#### 如何运行测试

1. **启动服务**：确保 `zmbdp-mstemplate-service` 服务已启动
2. **调用测试接口**：通过 HTTP 请求调用测试接口
3. **查看结果**：接口返回测试结果，同时查看日志获取详细信息

**示例：**
```bash
# 幂等性高并发测试（默认100并发）
POST http://localhost:8080/mstemplate/test/idempotent/concurrent?concurrency=100

# 布隆过滤器线程安全测试
POST http://localhost:8080/mstemplate/test/cache/threads/bloom
```

### 4. 测试结果解读

#### 幂等性测试结果

```json
{
  "防重模式-并发测试": {
    "成功数": 1,
    "拒绝数": 99,
    "错误数": 0,
    "总请求数": 100,
    "耗时(ms)": 1250,
    "结果": "✅ 通过（只有1个成功，其他都被正确拒绝）"
  }
}
```

**解读：**
- ✅ **成功数 = 1**：符合预期，只有一个请求成功
- ✅ **拒绝数 = 总请求数 - 1**：其他请求被正确拒绝
- ✅ **错误数 = 0**：没有异常发生
- ⚠️ **耗时过长**：如果耗时超过预期，需要检查锁竞争和网络延迟

#### 缓存性能测试结果

```json
{
  "performanceWithoutBloom": "1250ms",
  "performanceWithBloom": "320ms",
  "performanceImprovement": "930ms"
}
```

**解读：**
- ✅ **性能提升明显**：布隆过滤器有效减少了无效的 Redis 查询
- ⚠️ **提升不明显**：可能需要调整布隆过滤器配置或检查测试数据

#### 线程安全测试结果

```json
{
  "expectedElements": 1000,
  "actualElements": 998,
  "foundCount": 998,
  "notFoundCount": 2
}
```

**解读：**
- ✅ **元素数量接近**：布隆过滤器的近似计数允许一定误差
- ⚠️ **丢失元素过多**：如果 `notFoundCount` 较大，可能存在线程安全问题

### 5. 性能调优

根据测试结果进行优化：

1. **定位性能瓶颈**：分析耗时最长的操作
2. **优化慢查询**：检查数据库查询和 Redis 操作
3. **调整缓存策略**：优化缓存命中率和过期时间
4. **优化代码逻辑**：减少不必要的计算和网络调用
5. **调整线程池和连接池配置**：根据并发量调整资源池大小

### 6. 未来改进方向

#### 何时需要专业压测工具

- **生产环境性能评估**：需要真实的网络环境和负载
- **容量规划**：需要准确的 QPS、TPS 等指标
- **性能基准测试**：需要可重复的标准化测试
- **大规模压测**：需要模拟数千甚至数万并发

#### 建议的改进路径

1. **短期**：继续使用多线程测试验证功能正确性
2. **中期**：引入 JMeter 或 wrk 进行接口级性能测试
3. **长期**：建立完整的性能测试体系，包括：
   - 自动化性能测试流程
   - 性能基准数据库
   - 性能回归测试
   - 性能监控和告警

## 最佳实践

1. **避免 N+1 查询**：使用批量查询或关联查询
2. **合理使用缓存**：缓存热点数据，设置合理过期时间
3. **异步化处理**：耗时操作异步处理，提高响应速度
4. **连接池配置**：根据实际并发量配置连接池大小
5. **监控告警**：建立完善的监控体系，及时发现问题

## 注意事项

1. **缓存一致性**：注意缓存与数据库的一致性
2. **锁粒度**：尽量减小锁的粒度，提高并发性能
3. **资源限制**：注意 JVM 堆内存、线程数等资源限制
4. **网络延迟**：减少不必要的网络调用
5. **序列化性能**：选择合适的序列化方式（如 JSON、Protobuf）
