# 分布式定时任务（XXL-JOB）

## 概述

分布式定时任务通过 **XXL-JOB + 分布式锁** 实现，支持多实例安全调度、失败重试、实时日志查看和 CRON 动态调整。`zmbdp-common-xxljob` 提供执行器自动配置，引入依赖即完成注册，无需重复编写初始化代码。

同时提供 **Spring Scheduled 降级兜底方案**：当 XXL-JOB 调度中心不可用时，可通过 Nacos 动态开启本地定时任务，保证系统可用性。

## 核心组件

- `XxlJobConfig`：执行器自动配置（`zmbdp-common-xxljob`），读取 Nacos 配置自动注册执行器
- `ResetBloomFilterJobHandler`：布隆过滤器重置 Handler，每日凌晨 4 点触发，使用 Redisson 分布式锁保证多实例只执行一次
- `ResetBloomFilterTimedTask`：Spring Scheduled 降级兜底任务，XXL-JOB 不可用时通过配置启用
- `TestXxlJobController`：`zmbdp-mstemplate` 中的示例 Handler，包含简单任务和分片广播任务两种实现

## 使用方式

### 1. 引入依赖

在 Service 模块的 `pom.xml` 中增加：

```xml
<dependency>
    <groupId>com.zmbdp</groupId>
    <artifactId>zmbdp-common-xxljob</artifactId>
</dependency>
```

### 2. 引用 Nacos 配置

在服务的 `bootstrap.yml` 的 `shared-configs` 中增加：

```yaml
- data-id: share-xxljob-${spring.profiles.active}.yaml
  group: DEFAULT_GROUP
  refresh: true
```

`share-xxljob-{env}.yaml` 配置内容如下：

```yaml
xxl:
  job:
    admin:
      addresses: http://frameworkjava-xxljob-admin:8080/xxl-job-admin
    executor:
      appname: ${spring.application.name}-executor
      address:
      ip:
      port: -1
      accessToken: frameworkjava_dev
      logpath: /data/applogs/xxl-job/executor
      logretentiondays: 30
```

### 3. 编写 Handler

参考 `zmbdp-mstemplate` 模块的 `TestXxlJobController`，使用 `@XxlJob` 注解注册 Handler：

```java
@Slf4j
@Component
public class DemoJobHandler {

    /**
     * 简单任务示例
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        XxlJobHelper.log("任务开始执行，参数: {}", XxlJobHelper.getJobParam());
        log.info("任务开始执行");

        // 业务逻辑

        XxlJobHelper.handleSuccess("执行成功");
    }

    /**
     * 分片广播任务示例（多实例并行处理）
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("分片序号: {}, 总分片数: {}", shardIndex, shardTotal);

        // 按分片过滤数据，实现并行处理
        // List<Order> orders = orderService.listBySharding(shardIndex, shardTotal);

        XxlJobHelper.handleSuccess(String.format("分片 [%d/%d] 处理完成", shardIndex + 1, shardTotal));
    }
}
```

### 4. 在 XXL-JOB 管控台配置任务

1. 进入「执行器管理」，确认服务已自动注册（AppName 为 `{spring.application.name}-executor`）
2. 进入「任务管理」，新增任务：

| 配置项        | 说明                                      |
|-------------|-------------------------------------------|
| 执行器        | 选择对应服务的执行器                          |
| JobHandler   | 填写 `@XxlJob` 注解的 value，如 `demoJobHandler` |
| 调度类型       | 选择 CRON，填写 CRON 表达式，如 `0 0 4 * * ?`  |
| 路由策略       | 单实例选 FIRST，多实例分片选 SHARDING_BROADCAST |
| 阻塞处理策略    | 建议选 SERIAL_EXECUTION（串行）               |
| 失败重试次数    | 根据业务需要设置，建议 1-3 次                   |

## 分布式锁保障

多实例部署时，同一任务可能被多个节点同时触发。使用 `RedissonLockService` 保证只有一个节点执行：

```java
@XxlJob("safeJobHandler")
public void safeJobHandler() throws Exception {
    RLock lock = redissonLockService.acquire("job:lock:key", 10, TimeUnit.SECONDS);
    if (lock == null) {
        XxlJobHelper.handleSuccess("获取锁失败，跳过本次执行（其他节点正在执行）");
        return;
    }
    try {
        // 业务逻辑
        XxlJobHelper.handleSuccess("执行成功");
    } catch (Exception e) {
        XxlJobHelper.handleFail("执行失败: " + e.getMessage());
    } finally {
        redissonLockService.releaseLock(lock);
    }
}
```

## 降级兜底方案

当 XXL-JOB 调度中心不可用时，可通过 Nacos 动态开启本地 Spring Scheduled 兜底任务，参考 `ResetBloomFilterTimedTask` 的实现：

```java
@Slf4j
@Component
@RefreshScope
public class DemoFallbackTask {

    @Value("${demo.fallback.enabled:false}")
    private boolean fallbackEnabled;

    @Scheduled(cron = "${demo.fallback.cron:0 0 4 * * ?}")
    public void fallback() {
        if (!fallbackEnabled) {
            return;
        }
        log.warn("[Fallback] XXL-JOB 降级模式激活，执行本地定时任务");
        // 委托给 XXL-JOB Handler 执行，避免重复代码
        // demoJobHandler.execute();
    }
}
```

在 Nacos 动态下发以下配置即可启用：

```yaml
demo:
  fallback:
    enabled: true
    cron: 0 0 4 * * ?
```

## 内置任务说明

### 布隆过滤器重置任务

`zmbdp-admin` 中内置了布隆过滤器每日重置任务，在 XXL-JOB 管控台按如下配置添加：

```
executor.handler    : resetBloomFilterJobHandler
schedule_type       : CRON
schedule_conf       : 0 0 4 * * ?
executor_route      : FIRST
block_strategy      : SERIAL_EXECUTION
fail_retry_count    : 1
```

任务执行逻辑：
1. 获取 Redisson 分布式锁，防止多实例并发执行
2. 重置布隆过滤器
3. 全量加载 C 端用户数据重新预热（手机号、微信 OpenId、邮箱、用户 ID）
4. 上报执行结果到调度中心

## 注意事项

1. **执行器 AppName**：默认为 `${spring.application.name}-executor`，需与调度中心配置一致
2. **分布式锁**：多实例部署时务必加分布式锁，避免重复执行
3. **失败处理**：业务异常时调用 `XxlJobHelper.handleFail()` 通知调度中心，触发重试策略
4. **日志记录**：使用 `XxlJobHelper.log()` 记录的日志可在调度中心「执行日志」页实时查看
5. **降级方案**：生产环境建议同时配置降级兜底任务，保证调度中心故障时系统仍可正常运行
