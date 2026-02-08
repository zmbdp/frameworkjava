# 链路追踪使用指南

## 📖 概述

FrameworkJava 提供了两种链路追踪方案：

### 1. 轻量级 TraceId 方案（推荐用于日志追踪）

基于 MDC 实现的轻量级 TraceId 传递，无需 Agent，开箱即用：

- **全链路 TraceId**：从网关到所有微服务保持一致的 TraceId
- **日志关联**：所有日志自动包含 TraceId，便于问题排查
- **零侵入**：无需修改业务代码，自动传递
- **高性能**：基于 MDC，性能损耗极小

### 2. Apache SkyWalking 方案（推荐用于性能分析）

功能强大的 APM 系统，提供：

- **全链路追踪**：自动追踪微服务间的调用链路
- **性能分析**：分析接口响应时间、慢查询、性能瓶颈
- **拓扑图**：可视化服务依赖关系
- **告警功能**：异常自动告警
- **日志关联**：日志自动关联 TraceId

---

## 🎯 方案一：轻量级 TraceId（推荐）

### 🌟 智能适配：自动兼容 SkyWalking

**重要特性：我们的实现会自动检测并适配 SkyWalking！**

**TraceId 获取优先级：**

```
1. SkyWalking 的 traceId（如果配置了 Agent）
   ↓ 如果没有
2. 请求头中的 traceId（X-Trace-Id）
   ↓ 如果没有
3. 生成新的 traceId（32位UUID）
```

**这意味着：**

- ✅ **配置了 SkyWalking**：自动使用 SkyWalking 的 traceId，获得性能分析 + 统一的 traceId
- ✅ **未配置 SkyWalking**：自动生成轻量级 traceId，仍然有完整的日志追踪
- ✅ **跨服务 traceId 始终保持一致**
- ✅ **无需修改代码，自动适配**

### 架构说明

```
客户端请求
    ↓
Gateway [生成 traceId=abc123]
    ↓ (请求头: X-Trace-Id: abc123)
Admin Service [traceId=abc123]
    ↓ (Feign 请求头: X-Trace-Id: abc123)
File Service [traceId=abc123]
```

### 实现原理

1. **Gateway 层（TraceFilter）**
   - 生成全局唯一的 traceId（32位UUID）
   - 设置到 MDC：`MDC.put("traceId", traceId)`
   - 添加到请求头：`X-Trace-Id`

2. **微服务层（TraceIdFilter）**
   - 从请求头提取 traceId
   - 设置到 MDC：`MDC.put("traceId", traceId)`
   - 请求结束后清理 MDC

3. **Feign 调用（FeignTraceInterceptor）**
   - 从 MDC 获取 traceId
   - 添加到 Feign 请求头：`X-Trace-Id`
   - 确保下游服务接收到相同的 traceId

### 核心组件

#### 1. Gateway 过滤器

**位置：** `zmbdp-gateway/src/main/java/com/zmbdp/gateway/filter/TraceFilter.java`

**功能：**

- 优先使用 SkyWalking 的 traceId（如果配置了 Agent）
- 否则生成轻量级 traceId（32位UUID）
- 设置到 MDC 和请求头
- 优先级：-300（最先执行）

#### 2. 微服务过滤器

**位置：** `zmbdp-common-log/src/main/java/com/zmbdp/common/log/filter/TraceIdFilter.java`

**功能：**

- 优先使用 SkyWalking 的 traceId（如果配置了 Agent）
- 否则从请求头提取 traceId
- 设置到 MDC
- 请求结束后清理 MDC

#### 3. Feign 拦截器

**位置：** `zmbdp-common-log/src/main/java/com/zmbdp/common/log/interceptor/FeignTraceInterceptor.java`

**功能：**

- 从 MDC 获取 traceId（可能是 SkyWalking 的，也可能是轻量级的）
- 添加到 Feign 请求头
- 实现跨服务传递

#### 4. Feign 配置类

**位置：** `zmbdp-common-log/src/main/java/com/zmbdp/common/log/config/FeignConfig.java`

**功能：**

- 注册 Feign 拦截器
- 使用 `@ConditionalOnClass` 确保只在有 Feign 时生效
- Gateway 不会加载此配置（因为 Gateway 没有 Feign 依赖）

### 使用方式

#### 场景 1：开发环境 - 只需要日志追踪

```bash
# 不需要配置 SkyWalking Agent
# 直接启动服务即可
java -jar zmbdp-gateway.jar
```

✅ **效果：**

- 自动生成轻量级 traceId（32位UUID）
- 所有日志自动包含 traceId
- 跨服务 traceId 保持一致
- 零配置，开箱即用

**日志示例：**

```
2026-02-03 10:00:00.123 [abc123def456] INFO  Gateway 处理请求
2026-02-03 10:00:00.130 [abc123def456] INFO  Admin 处理业务
2026-02-03 10:00:00.140 [abc123def456] INFO  File 上传文件
```

#### 场景 2：生产环境 - 性能分析 + 日志追踪（推荐）⭐

```bash
# 配置 SkyWalking Agent
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=zmbdp-gateway-service \
     -Dskywalking.collector.backend_service=skywalking-oap:11800 \
     -jar zmbdp-gateway.jar
```

✅ **效果：**

- **自动使用 SkyWalking 的 traceId**（无需修改代码）
- 完整的 APM 功能（性能分析、服务拓扑图、慢查询分析）
- 跨服务 traceId 保持一致（使用 SkyWalking 的 traceId）
- 所有日志自动包含 traceId
- 可以在 SkyWalking UI 中查看完整的调用链路

**日志示例：**

```
2026-02-03 10:00:00.123 [TID:N.e4a.16753.17] INFO  Gateway 处理请求
2026-02-03 10:00:00.130 [TID:N.e4a.16753.17] INFO  Admin 处理业务
2026-02-03 10:00:00.140 [TID:N.e4a.16753.17] INFO  File 上传文件
```

**SkyWalking UI 中可以看到：**

- 完整的调用链路（Gateway → Admin → File）
- 每个服务的响应时间
- SQL 查询详情
- 异常堆栈信息
- 根据 traceId 关联所有日志

#### 两者对比

| 特性             | 未配置 SkyWalking | 配置了 SkyWalking |
|----------------|----------------|----------------|
| **TraceId 格式** | 32位UUID        | SkyWalking 格式  |
| **日志追踪**       | ✅ 支持           | ✅ 支持           |
| **跨服务一致**      | ✅ 一致           | ✅ 一致           |
| **性能分析**       | ❌ 不支持          | ✅ 支持           |
| **服务拓扑图**      | ❌ 不支持          | ✅ 支持           |
| **慢查询分析**      | ❌ 不支持          | ✅ 支持           |
| **配置复杂度**      | 零配置            | 需要配置 Agent     |
| **性能损耗**       | <1%            | 1-3%           |

### 1. 无需任何配置

所有组件已自动注册，无需额外配置。

#### 2. 日志配置（Nacos）

在 `share-monitor-{env}.yaml` 中配置：

```yaml
logging:
   pattern:
      console: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] %-5level [%thread] %logger{36} : %msg%n'
      file: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] %-5level [%thread] %logger{36} : %msg%n'
```

#### 3. 查看日志

**Gateway 日志：**

```
2026-02-03 10:00:00.123 [abc123def456] INFO  [reactor-http-nio-2] c.z.g.f.TraceFilter : 生成 traceId
```

**Admin Service 日志：**

```
2026-02-03 10:00:00.130 [abc123def456] INFO  [http-nio-18081-exec-1] c.z.a.c.UploadController : 处理上传请求
```

**File Service 日志：**

```
2026-02-03 10:00:00.140 [abc123def456] INFO  [http-nio-18082-exec-1] c.z.f.c.FileController : 接收文件上传
```

✅ **所有日志的 traceId 保持一致！**

### 优势

- ✅ **零配置**：无需 Agent，无需 XML 配置
- ✅ **高性能**：基于 MDC，性能损耗极小
- ✅ **全链路**：从网关到所有微服务保持一致
- ✅ **易排查**：根据 traceId 快速定位问题

### 日志查询

```bash
# 根据 traceId 查询所有相关日志
grep "abc123def456" logs/*.log

# 或使用 ELK 查询
traceId: "abc123def456"
```

---

## 🚀 方案二：Apache SkyWalking

## 🚀 SkyWalking 快速开始

### 1. 启动 SkyWalking 服务

```bash
cd deploy/dev/app
docker compose -p frameworkjava -f docker-compose-mid.yml up -d frameworkjava-skywalking-oap frameworkjava-skywalking-ui
```

### 2. 访问 SkyWalking UI

浏览器访问：[http://localhost:8080](http://localhost:8080)

### 3. 下载 SkyWalking Agent

从官方下载 SkyWalking Agent：

```bash
# 下载地址
https://skywalking.apache.org/downloads/

# 或使用 Maven 下载
mvn dependency:get -Dartifact=org.apache.skywalking:apm-agent:9.0.0:jar
```

解压后得到 `skywalking-agent` 目录。

### 4. 配置服务启动参数

#### 方式一：IDEA 启动配置

在 IDEA 的 VM Options 中添加：

```bash
-javaagent:/path/to/skywalking-agent/skywalking-agent.jar
-Dskywalking.agent.service_name=zmbdp-admin
-Dskywalking.collector.backend_service=localhost:11800
```

#### 方式二：命令行启动

```bash
java -javaagent:/path/to/skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=zmbdp-admin \
     -Dskywalking.collector.backend_service=localhost:11800 \
     -jar zmbdp-admin-service.jar
```

#### 方式三：Docker 启动（推荐生产环境）

在 Dockerfile 中添加：

```dockerfile
FROM openjdk:17-jdk-slim

# 复制 SkyWalking Agent
COPY skywalking-agent /skywalking-agent

# 复制应用 JAR
COPY target/app.jar /app.jar

# 启动参数
ENV JAVA_OPTS="-javaagent:/skywalking-agent/skywalking-agent.jar"
ENV SW_AGENT_NAME="zmbdp-admin"
ENV SW_AGENT_COLLECTOR_BACKEND_SERVICES="frameworkjava-skywalking-oap:11800"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
```

## 📊 功能说明

### 1. 全链路追踪

SkyWalking 会自动追踪以下组件：

- **HTTP 请求**：Spring MVC、Spring WebFlux
- **RPC 调用**：OpenFeign、Dubbo
- **数据库**：MySQL、PostgreSQL、Oracle
- **缓存**：Redis、Memcached
- **消息队列**：RabbitMQ、Kafka、RocketMQ
- **网关**：Spring Cloud Gateway

**查看链路追踪：**

1. 访问 SkyWalking UI
2. 点击 "Trace" 菜单
3. 选择服务和时间范围
4. 查看详细调用链路

### 2. 性能分析

**查看服务性能：**

1. 访问 SkyWalking UI
2. 点击 "Service" 菜单
3. 选择服务
4. 查看：
    - 响应时间（P50、P75、P90、P95、P99）
    - 吞吐量（QPS）
    - 错误率
    - 慢端点（Slow Endpoints）

**查看端点性能：**

1. 点击 "Endpoint" 菜单
2. 选择具体接口
3. 查看详细性能指标

### 3. 服务拓扑图

**查看服务依赖关系：**

1. 访问 SkyWalking UI
2. 点击 "Topology" 菜单
3. 查看服务间的调用关系和流量

### 4. 日志关联 TraceId

FrameworkJava 已集成 SkyWalking Logback 插件，日志会自动关联 TraceId。

**日志格式：**

```
2026-02-02 10:30:45.123 [a1b2c3d4e5f6] INFO  [http-nio-10010-exec-1] c.z.a.s.u.c.SysUserController : 用户登录成功
```

**根据 TraceId 查询日志：**

```bash
# 在日志文件中搜索
grep "a1b2c3d4e5f6" logs/zmbdp-admin-service.log
```

### 5. 手动埋点（可选）

如果需要手动追踪某些方法，可以使用 SkyWalking Toolkit：

```java
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

@Service
public class UserService {
    
    /**
     * 自动追踪该方法
     */
    @Trace
    public User getUserById(Long userId) {
        // 获取当前 TraceId
        String traceId = TraceContext.traceId();
        log.info("TraceId: {}, 查询用户: {}", traceId, userId);
        
        // 业务逻辑
        return userMapper.selectById(userId);
    }
}
```

## ⚙️ 配置说明

### SkyWalking Agent 配置

编辑 `skywalking-agent/config/agent.config`：

```properties
# 服务名称
agent.service_name=${SW_AGENT_NAME:zmbdp-admin}

# OAP 服务地址
collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:localhost:11800}

# 采样率（0.0 - 1.0，1.0 表示全量采集）
agent.sample_n_per_3_secs=${SW_AGENT_SAMPLE:-1}

# 日志级别
logging.level=${SW_LOGGING_LEVEL:INFO}

# 忽略的端点（正则表达式）
trace.ignore_path=${SW_IGNORE_PATH:/actuator/**,/health,/metrics}

# 最大 Span 数量
agent.span_limit_per_segment=${SW_AGENT_SPAN_LIMIT:300}
```

### 常用配置项

| 配置项                            | 说明         | 默认值             |
|--------------------------------|------------|-----------------|
| `agent.service_name`           | 服务名称       | -               |
| `collector.backend_service`    | OAP 服务地址   | localhost:11800 |
| `agent.sample_n_per_3_secs`    | 采样率        | -1（全量）          |
| `logging.level`                | 日志级别       | INFO            |
| `trace.ignore_path`            | 忽略的端点      | -               |
| `agent.span_limit_per_segment` | 最大 Span 数量 | 300             |

## 🔍 常见问题

### 1. SkyWalking UI 无法访问

**原因**：OAP 服务未启动或启动失败

**解决方案**：

```bash
# 查看 OAP 日志
docker logs frameworkjava-skywalking-oap

# 检查 OAP 健康状态
curl http://localhost:12800/internal/l7check
```

### 2. 服务未显示在 SkyWalking UI

**原因**：Agent 未正确配置或未连接到 OAP

**解决方案**：

1. 检查 Agent 配置是否正确
2. 检查 OAP 地址是否可访问
3. 查看应用日志，搜索 "SkyWalking"

### 3. 链路追踪数据不完整

**原因**：采样率设置过低

**解决方案**：

调整采样率：

```properties
# 全量采集
agent.sample_n_per_3_secs=-1

# 或者每 3 秒采集 1000 个
agent.sample_n_per_3_secs=1000
```

### 4. 性能影响

**问题**：SkyWalking Agent 对性能有影响吗？

**答案**：

- **CPU 开销**：约 1-3%
- **内存开销**：约 50-100MB
- **网络开销**：取决于采样率

**优化建议**：

1. 生产环境适当降低采样率
2. 配置忽略不重要的端点
3. 限制 Span 数量

### 5. 日志中没有 TraceId

**原因**：未配置 Logback 插件

**解决方案**：

在 `logback-spring.xml` 中添加：

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%tid] %-5level [%thread] %logger{36} : %msg%n</pattern>
            </layout>
        </encoder>
    </appender>
</configuration>
```

## 📚 最佳实践

### 1. 服务命名规范

建议使用统一的服务命名规范：

```
{项目名}-{模块名}

例如：
- frameworkjava-gateway
- frameworkjava-admin
- frameworkjava-portal
```

### 2. 采样策略

**开发环境**：全量采集（-1）

**测试环境**：高采样率（每 3 秒 1000 个）

**生产环境**：适中采样率（每 3 秒 100-500 个）

### 3. 告警配置

在 SkyWalking UI 中配置告警规则：

1. 点击 "Alarm" 菜单
2. 配置告警规则（响应时间、错误率等）
3. 配置告警通知（Webhook、邮件等）

### 4. 性能优化

1. **忽略健康检查端点**：
   ```properties
   trace.ignore_path=/actuator/**,/health,/metrics
   ```

2. **限制 Span 数量**：
   ```properties
   agent.span_limit_per_segment=300
   ```

3. **异步上报**：
   ```properties
   buffer.channel_size=5000
   buffer.buffer_size=300
   ```

## 🔗 相关链接

- [SkyWalking 官方文档](https://skywalking.apache.org/docs/)
- [SkyWalking GitHub](https://github.com/apache/skywalking)
- [服务监控文档](MONITORING.md)
- [性能优化文档](PERFORMANCE.md)

---

如有问题，请联系：[JavaFH@163.com](mailto:JavaFH@163.com)

