# 链路追踪使用指南

## 📖 概述

FrameworkJava 集成了 **Apache SkyWalking** 作为分布式链路追踪解决方案，提供：

- **全链路追踪**：自动追踪微服务间的调用链路
- **性能分析**：分析接口响应时间、慢查询、性能瓶颈
- **拓扑图**：可视化服务依赖关系
- **告警功能**：异常自动告警
- **日志关联**：日志自动关联 TraceId

## 🚀 快速开始

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
2026-02-02 10:30:45.123 [TID:a1b2c3d4e5f6] INFO  [http-nio-10010-exec-1] c.z.a.s.u.c.SysUserController : 用户登录成功
```

**根据 TraceId 查询日志：**

```bash
# 在日志文件中搜索
grep "TID:a1b2c3d4e5f6" logs/zmbdp-admin-service.log
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

