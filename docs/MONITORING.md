# 服务监控与告警使用指南

## 📖 概述

FrameworkJava 集成了 **Prometheus + Grafana + AlertManager** 作为服务监控与告警解决方案，提供：

- **全方位监控**：JVM、接口、数据库、缓存、系统资源
- **可视化大盘**：Grafana 实时监控大盘
- **智能告警**：多级别告警规则，支持多渠道通知
- **历史数据**：30 天数据保留，支持趋势分析

## 🚀 快速开始

### 1. 启动监控服务

```bash
cd deploy/dev/app
docker compose -p frameworkjava -f docker-compose-mid.yml up -d frameworkjava-prometheus frameworkjava-grafana frameworkjava-alertmanager
```

### 2. 访问监控界面

| 服务               | 地址                                             |
|------------------|------------------------------------------------|
| **Prometheus**   | [http://localhost:9090](http://localhost:9090) |
| **Grafana**      | [http://localhost:3000](http://localhost:3000) |
| **AlertManager** | [http://localhost:9093](http://localhost:9093) |

### 3. 查看监控大盘

登录 Grafana 后，在左侧菜单选择 "Dashboards" → "FrameworkJava"，可以看到：

- **JVM Monitoring**：JVM 内存、GC、线程、CPU 监控
- **API Monitoring**：接口 QPS、响应时间、错误率监控

## 📊 监控指标说明

### 1. JVM 监控

#### 内存指标

| 指标                                      | 说明      | 告警阈值              |
|-----------------------------------------|---------|-------------------|
| `jvm_memory_used_bytes{area="heap"}`    | 堆内存使用量  | > 85% 警告，> 95% 严重 |
| `jvm_memory_max_bytes{area="heap"}`     | 堆内存最大值  | -                 |
| `jvm_memory_used_bytes{area="nonheap"}` | 非堆内存使用量 | -                 |

**查询示例：**

```promql
# 堆内存使用率
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# 堆内存使用趋势
rate(jvm_memory_used_bytes{area="heap"}[5m])
```

#### GC 指标

| 指标                           | 说明     | 告警阈值             |
|------------------------------|--------|------------------|
| `jvm_gc_pause_seconds_count` | GC 次数  | Full GC > 0.1次/秒 |
| `jvm_gc_pause_seconds_sum`   | GC 总耗时 | > 0.5秒/秒         |

**查询示例：**

```promql
# GC 频率（5分钟内平均每秒 GC 次数）
rate(jvm_gc_pause_seconds_count[5m])

# GC 耗时占比（5分钟内平均每秒 GC 耗时）
rate(jvm_gc_pause_seconds_sum[5m])
```

#### 线程指标

| 指标                               | 说明    | 告警阈值     |
|----------------------------------|-------|----------|
| `jvm_threads_live_threads`       | 活跃线程数 | > 500 警告 |
| `jvm_threads_daemon_threads`     | 守护线程数 | -        |
| `jvm_threads_deadlocked_threads` | 死锁线程数 | > 0 严重   |

**查询示例：**

```promql
# 线程数趋势
jvm_threads_live_threads

# 死锁检测
jvm_threads_deadlocked_threads > 0
```

### 2. 接口监控

#### 性能指标

| 指标                                    | 说明     | 告警阈值     |
|---------------------------------------|--------|----------|
| `http_server_requests_seconds_count`  | 请求总数   | -        |
| `http_server_requests_seconds_sum`    | 请求总耗时  | -        |
| `http_server_requests_seconds_bucket` | 响应时间分布 | P95 > 3秒 |

**查询示例：**

```promql
# QPS（每秒请求数）
rate(http_server_requests_seconds_count[1m])

# 平均响应时间
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# P99 响应时间
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

#### 错误率指标

**查询示例：**

```promql
# 5xx 错误率
(rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])) * 100

# 4xx 错误率
(rate(http_server_requests_seconds_count{status=~"4.."}[5m]) / rate(http_server_requests_seconds_count[5m])) * 100
```

### 3. 数据库连接池监控

| 指标                             | 说明    | 告警阈值    |
|--------------------------------|-------|---------|
| `hikaricp_connections_active`  | 活跃连接数 | -       |
| `hikaricp_connections_max`     | 最大连接数 | -       |
| `hikaricp_connections_pending` | 等待连接数 | > 10 警告 |

**查询示例：**

```promql
# 连接池使用率
(hikaricp_connections_active / hikaricp_connections_max) * 100

# 等待连接数
hikaricp_connections_pending
```

### 4. 系统资源监控

#### CPU 指标

| 指标                  | 说明         | 告警阈值     |
|---------------------|------------|----------|
| `system_cpu_usage`  | 系统 CPU 使用率 | > 80% 警告 |
| `process_cpu_usage` | 进程 CPU 使用率 | > 80% 警告 |

**查询示例：**

```promql
# 系统 CPU 使用率
system_cpu_usage * 100

# 进程 CPU 使用率
process_cpu_usage * 100
```

#### 文件描述符指标

| 指标                         | 说明     | 告警阈值     |
|----------------------------|--------|----------|
| `process_files_open_files` | 打开的文件数 | -        |
| `process_files_max_files`  | 最大文件数  | > 80% 警告 |

**查询示例：**

```promql
# 文件描述符使用率
(process_files_open_files / process_files_max_files) * 100
```

## 🔔 告警规则说明

### 告警级别

| 级别           | 说明          | 响应时间   | 通知方式         |
|--------------|-------------|--------|--------------|
| **critical** | 严重告警，需要立即处理 | 5 分钟内  | 邮件 + 钉钉 + 短信 |
| **warning**  | 警告告警，需要关注   | 30 分钟内 | 邮件 + 钉钉      |
| **info**     | 信息告警，仅记录    | -      | 邮件           |

### 已配置的告警规则

#### 1. 服务可用性告警

| 告警名称             | 触发条件 | 级别       | 持续时间 |
|------------------|------|----------|------|
| ServiceDown      | 服务下线 | critical | 1 分钟 |
| ServiceRestarted | 服务重启 | warning  | 1 分钟 |

#### 2. JVM 性能告警

| 告警名称                  | 触发条件                | 级别       | 持续时间 |
|-----------------------|---------------------|----------|------|
| JvmHeapMemoryHigh     | 堆内存使用率 > 85%        | warning  | 5 分钟 |
| JvmHeapMemoryCritical | 堆内存使用率 > 95%        | critical | 2 分钟 |
| FullGCFrequent        | Full GC 频率 > 0.1次/秒 | warning  | 5 分钟 |
| GCTimeTooLong         | GC 耗时 > 0.5秒/秒      | warning  | 5 分钟 |
| ThreadCountHigh       | 线程数 > 500           | warning  | 5 分钟 |
| ThreadDeadlock        | 检测到死锁               | critical | 1 分钟 |

#### 3. 接口性能告警

| 告警名称                 | 触发条件          | 级别       | 持续时间 |
|----------------------|---------------|----------|------|
| ApiResponseTimeSlow  | P95 响应时间 > 3秒 | warning  | 5 分钟 |
| ApiErrorRateHigh     | 5xx 错误率 > 5%  | warning  | 5 分钟 |
| ApiErrorRateCritical | 5xx 错误率 > 20% | critical | 2 分钟 |
| ApiQPSAbnormallyHigh | QPS > 1000    | warning  | 5 分钟 |

#### 4. 数据库告警

| 告警名称                           | 触发条件         | 级别      | 持续时间 |
|--------------------------------|--------------|---------|------|
| DatabaseConnectionPoolHigh     | 连接池使用率 > 80% | warning | 5 分钟 |
| DatabaseConnectionWaitTimeLong | 等待连接数 > 10   | warning | 2 分钟 |

#### 5. 系统资源告警

| 告警名称               | 触发条件         | 级别      | 持续时间 |
|--------------------|--------------|---------|------|
| SystemCpuHigh      | 系统 CPU > 80% | warning | 5 分钟 |
| ProcessCpuHigh     | 进程 CPU > 80% | warning | 5 分钟 |
| FileDescriptorHigh | 文件描述符 > 80%  | warning | 5 分钟 |

## ⚙️ 配置说明

### 1. Prometheus 配置

编辑 `deploy/dev/app/prometheus/prometheus.yml`：

```yaml
scrape_configs:
  # 添加新服务监控
  - job_name: 'your-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:10060']
        labels:
          service: 'your-service'
          application: 'your-app'
```

### 2. 告警规则配置

编辑 `deploy/dev/app/prometheus/rules/service_alerts.yml`：

```yaml
groups:
  - name: custom_alerts
    interval: 30s
    rules:
      # 自定义告警规则
      - alert: CustomAlert
        expr: your_metric > threshold
        for: 5m
        labels:
          severity: warning
          category: custom
        annotations:
          summary: "自定义告警"
          description: "详细描述"
```

### 3. AlertManager 配置

编辑 `deploy/dev/app/alertmanager/alertmanager.yml`：

#### 配置邮件通知

```yaml
global:
  smtp_smarthost: 'smtp.qq.com:587'
  smtp_from: 'your-email@qq.com'
  smtp_auth_username: 'your-email@qq.com'
  smtp_auth_password: 'your-smtp-password'
  smtp_require_tls: true

receivers:
  - name: 'email'
    email_configs:
      - to: 'admin@example.com'
        headers:
          Subject: '【告警】{{ .GroupLabels.alertname }}'
```

#### 配置钉钉通知

```yaml
receivers:
  - name: 'dingtalk'
    webhook_configs:
      - url: 'http://your-webhook-server:5001/webhook/dingtalk'
        send_resolved: true
```

### 4. Grafana 配置

#### 添加数据源

1. 登录 Grafana
2. 点击左侧菜单 "Configuration" → "Data Sources"
3. 点击 "Add data source"
4. 选择 "Prometheus"
5. 配置 URL：`http://frameworkjava-prometheus:9090`
6. 点击 "Save & Test"

#### 导入 Dashboard

1. 点击左侧菜单 "+" → "Import"
2. 输入 Dashboard ID 或上传 JSON 文件
3. 选择数据源
4. 点击 "Import"

**推荐 Dashboard：**

- **JVM (Micrometer)**：ID 4701
- **Spring Boot 2.1 Statistics**：ID 10280
- **MySQL Overview**：ID 7362
- **Redis Dashboard**：ID 11835

## 🔍 常见问题

### 1. Prometheus 无法采集数据

**原因**：服务未暴露 Prometheus 端点

**解决方案**：

检查服务配置文件（`application.yml`）：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2. Grafana 无法连接 Prometheus

**原因**：网络不通或数据源配置错误

**解决方案**：

```bash
# 在 Grafana 容器中测试连接
docker exec -it frameworkjava-grafana curl http://frameworkjava-prometheus:9090/-/healthy
```

### 3. 告警未触发

**原因**：告警规则配置错误或 AlertManager 未配置

**解决方案**：

1. 检查 Prometheus 告警规则：访问 `http://localhost:9090/alerts`
2. 检查 AlertManager 配置：访问 `http://localhost:9093`
3. 查看 AlertManager 日志：`docker logs frameworkjava-alertmanager`

### 4. 告警通知未收到

**原因**：邮件配置错误或 Webhook 不可达

**解决方案**：

1. 测试邮件配置：
   ```bash
   # 发送测试告警
   curl -X POST http://localhost:9093/api/v1/alerts -d '[{"labels":{"alertname":"test"}}]'
   ```

2. 查看 AlertManager 日志：
   ```bash
   docker logs frameworkjava-alertmanager
   ```

### 5. 监控数据不准确

**原因**：时间不同步或采集间隔过长

**解决方案**：

1. 同步服务器时间：
   ```bash
   ntpdate -u ntp.aliyun.com
   ```

2. 调整采集间隔（`prometheus.yml`）：
   ```yaml
   global:
     scrape_interval: 15s  # 默认 15 秒
   ```

## 📚 最佳实践

### 1. 监控指标选择

**必须监控的指标：**

- ✅ 服务可用性（up）
- ✅ JVM 堆内存使用率
- ✅ GC 频率和耗时
- ✅ 接口响应时间（P95、P99）
- ✅ 接口错误率
- ✅ 数据库连接池使用率

**可选监控的指标：**

- 线程数
- CPU 使用率
- 文件描述符
- 磁盘 IO
- 网络流量

### 2. 告警规则设计

**原则：**

1. **分级告警**：critical > warning > info
2. **避免告警风暴**：设置合理的持续时间和抑制规则
3. **可操作性**：告警信息要包含足够的上下文
4. **避免误报**：设置合理的阈值和持续时间

**示例：**

```yaml
# 好的告警规则
- alert: JvmHeapMemoryHigh
  expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100 > 85
  for: 5m  # 持续 5 分钟才触发
  labels:
    severity: warning
  annotations:
    summary: "服务 {{ $labels.service }} JVM 堆内存使用率过高"
    description: "当前值: {{ $value | humanize }}%，建议检查内存泄漏或增加堆内存"
```

### 3. Dashboard 设计

**原则：**

1. **分层设计**：总览 → 服务 → 接口 → 详情
2. **关键指标突出**：使用 Stat、Gauge 面板
3. **趋势分析**：使用 Time Series 面板
4. **对比分析**：使用多个服务对比

**推荐布局：**

```
+------------------+------------------+------------------+
|   总 QPS         |   错误率         |   P95 响应时间   |
+------------------+------------------+------------------+
|   QPS 趋势图（按服务）                                 |
+-------------------------------------------------------+
|   响应时间趋势图（按接口）                             |
+-------------------------------------------------------+
|   错误率趋势图（按状态码）                             |
+-------------------------------------------------------+
```

### 4. 数据保留策略

**Prometheus 数据保留：**

```yaml
# prometheus.yml
storage:
  tsdb:
    retention.time: 30d  # 保留 30 天
    retention.size: 50GB  # 最大 50GB
```

**长期存储方案：**

- 使用 Thanos 或 Cortex 进行长期存储
- 定期导出重要指标到数据库

### 5. 性能优化

**Prometheus 优化：**

1. 减少采集频率（生产环境 30s-60s）
2. 使用 relabel 过滤不需要的指标
3. 启用压缩

**Grafana 优化：**

1. 使用变量减少 Dashboard 数量
2. 设置合理的刷新间隔（10s-30s）
3. 限制查询时间范围

## 🔗 相关链接

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [AlertManager 官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [链路追踪文档](TRACING.md)
- [性能优化文档](PERFORMANCE.md)

---

如有问题，请联系：[JavaFH@163.com](mailto:JavaFH@163.com)

