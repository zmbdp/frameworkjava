# 三级缓存架构

## 架构设计

FrameworkJava 采用**三级缓存架构**，有效防止缓存穿透，提升系统性能。

```
请求
  ↓
┌─────────────────┐
│    布隆过滤器    │  ← 第一级：快速判断数据是否存在
└────────┬────────┘
         │ 存在
         ↓
┌─────────────────┐
│     Caffeine    │  ← 第二级：本地缓存（JVM 内存）
│     本地缓存     │
└────────┬────────┘
         │ 未命中
         ↓
┌─────────────────┐
│      Redis      │  ← 第三级：分布式缓存
│    分布式缓存    │
└────────┬────────┘
         │ 未命中
         ↓
┌─────────────────┐
│      MySQL      │  ← 数据源
│      数据库      │
└─────────────────┘
```

## 各级缓存说明

### 第一级：布隆过滤器（Bloom Filter）

**作用**：快速判断数据是否**可能**存在，避免无效的数据库查询

**特点**：
- 内存占用极小
- 查询速度极快（O(1)）
- 可能存在误判（假阳性），但不会漏判（假阴性）

**实现类型**（通过 `bloom.filter.type` 配置选择）：
- **fast**：不加锁版本，性能最高，但线程不安全，适用于对准确性要求不高的场景
- **safe**：加锁版本，线程安全，适用于单实例部署且对准确性要求高的场景
- **redis**：Redis 实现，分布式共享，适用于多实例部署场景（推荐）

**使用场景**：
- 防止缓存穿透
- 判断用户 ID、订单 ID 等是否存在

### 第二级：Caffeine 本地缓存

**作用**：JVM 内存缓存，减少 Redis 网络调用

**特点**：
- 访问速度最快（本地内存）
- 容量有限（受 JVM 堆内存限制）
- 单机缓存，多实例不共享

**使用场景**：
- 热点数据缓存
- 配置信息缓存
- 字典数据缓存

### 第三级：Redis 分布式缓存

**作用**：分布式缓存，多实例共享

**特点**：
- 容量大（可扩展）
- 支持持久化
- 多实例共享数据

**使用场景**：
- 用户会话信息
- 分布式锁
- 幂等性控制
- 业务数据缓存

## 缓存流程

### 查询流程

1. **布隆过滤器检查**
   - 不存在 → 直接返回，不查询数据库
   - 可能存在 → 继续下一步

2. **Caffeine 本地缓存查询**
   - 命中 → 直接返回
   - 未命中 → 继续下一步

3. **Redis 分布式缓存查询**
   - 命中 → 写入 Caffeine，返回数据
   - 未命中 → 继续下一步

4. **数据库查询**
   - 查询数据 → 写入 Redis → 写入 Caffeine → 返回数据

### 更新流程

1. 更新数据库
2. 删除 Redis 缓存
3. 删除 Caffeine 缓存
4. 更新布隆过滤器（如需要）

## 使用示例

### 代码示例

使用 `CacheUtil` 工具类实现三级缓存：

```java
@Service
public class UserService {
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private BloomFilterService bloomFilterService;
    
    @Autowired
    private Cache<String, Object> caffeineCache;  // Caffeine 本地缓存
    
    @Autowired
    private UserMapper userMapper;
    
    public User getUserById(Long userId) {
        String key = "user:" + userId;
        
        // 使用 CacheUtil 自动处理三级缓存查询
        // 1. 布隆过滤器检查 → 2. Caffeine 查询 → 3. Redis 查询
        User user = CacheUtil.getL2Cache(redisService, bloomFilterService, key, User.class, caffeineCache);
        
        if (user != null) {
            return user;
        }
        
        // 4. 数据库查询
        user = userMapper.selectById(userId);
        if (user != null) {
            // 写入三级缓存：Redis → Caffeine → 布隆过滤器
            CacheUtil.setL2Cache(redisService, bloomFilterService, key, user, caffeineCache, 300L, TimeUnit.SECONDS);
        }
        
        return user;
    }
}
```

**说明：**

- `CacheUtil.getL2Cache()` 会自动按顺序查询：布隆过滤器 → Caffeine → Redis
- `CacheUtil.setL2Cache()` 会自动写入：Redis → Caffeine → 布隆过滤器
- 无需手动处理缓存层级逻辑，工具类已封装好

## 配置说明

### 布隆过滤器配置

在 Nacos 配置 `share-filter-{env}.yaml`：

```yaml
bloom:
  filter:
    type: redis              # 类型选择：fast（不加锁）/ safe（加锁）/ redis（分布式，推荐）
    expectedInsertions: 10000 # 预期插入元素数量
    falseProbability: 0.01   # 误判率
    warningThreshold: 0.7     # 扩容阈值
    checkWarning: true        # 是否开启阈值检查
```

**类型选择建议**：
- **单实例 + 准确性要求高**：使用 `safe`
- **单实例 + 准确性要求不高**：使用 `fast`（性能最优）
- **多实例部署**：使用 `redis`（推荐）

### Caffeine 配置

在 Nacos 配置 `share-caffeine-{env}.yaml`：

```yaml
caffeine:
  cache:
    max-size: 10000        # 最大缓存数量
    expire-after-write: 30 # 写入后过期时间（分钟）
    expire-after-access: 15 # 访问后过期时间（分钟）
```

### Redis 配置

在 Nacos 配置 `share-redis-{env}.yaml`：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## 最佳实践

1. **缓存预热**：系统启动时加载热点数据到缓存
2. **缓存穿透防护**：使用布隆过滤器 + 空值缓存
3. **缓存雪崩防护**：设置随机过期时间
4. **缓存更新策略**：采用 Cache Aside 模式（先更新数据库，再删除缓存）
5. **本地缓存容量**：根据 JVM 堆内存合理设置，避免 OOM

## 性能优化

- **热点数据识别**：监控缓存命中率，识别热点数据
- **缓存分层**：根据数据访问频率选择缓存层级
- **批量操作**：支持批量查询，减少网络往返
