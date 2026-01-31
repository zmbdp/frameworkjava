# 配置中心与环境配置

## Nacos 配置中心

FrameworkJava 使用 **Nacos** 作为配置中心，实现配置的统一管理和动态刷新。

### 配置命名规范

配置文件命名格式：`{服务名}-{环境}.yaml`

示例：
- `zmbdp-gateway-service-dev.yaml`
- `zmbdp-admin-service-test.yaml`
- `zmbdp-portal-service-prd.yaml`

### 配置分组

所有配置默认放在 `DEFAULT_GROUP` 分组下。

### 共享配置

公共配置使用 `share-` 前缀，各服务可引用：

- `share-redis-{env}.yaml` - Redis 配置
- `share-mysql-{env}.yaml` - MySQL 配置
- `share-rabbitmq-{env}.yaml` - RabbitMQ 配置
- `share-caffeine-{env}.yaml` - 本地缓存配置
- `share-idempotent-{env}.yaml` - 幂等性配置
- `share-ratelimit-{env}.yaml` - 频控 / 防刷配置
- `share-log-{env}.yaml` - 操作日志配置
- `share-token-{env}.yaml` - Token 配置
- `share-filter-{env}.yaml` - 过滤器配置
- `share-upload-{env}.yaml` - 文件上传配置
- `share-mail-{env}.yaml` - 邮件配置
- `share-captcha-{env}.yaml` - 验证码配置
- `share-map-{env}.yaml` - 地图配置
- `share-thread-{env}.yaml` - 线程池配置

### 服务配置引用

在服务配置中通过 `spring.cloud.nacos.config.shared-configs` 引用共享配置：

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: share-redis-{env}.yaml  # {env} 替换为实际环境：dev/test/prd
            group: DEFAULT_GROUP
            refresh: true
          - data-id: share-mysql-{env}.yaml
            group: DEFAULT_GROUP
            refresh: true
```

## 环境说明

所有环境的配置文件都位于 `deploy/test/res/sql/nacos_config/DEFAULT_GROUP/` 目录下，通过修改文件名后缀来区分不同环境。

### dev（开发环境）
- 配置路径：`deploy/test/res/sql/nacos_config/DEFAULT_GROUP/`
- 配置文件：将 `-test.yaml` 后缀改为 `-dev.yaml`
- 导入方式：将目录打包成 zip，在 Nacos 控制台通过"导入配置"功能导入
- 用途：本地开发调试

### test（测试环境）
- 配置路径：`deploy/test/res/sql/nacos_config/DEFAULT_GROUP/`
- 配置文件：使用 `-test.yaml` 后缀
- 导入方式：将目录打包成 zip，在 Nacos 控制台通过"导入配置"功能导入
- 用途：测试环境部署

### prd（生产环境）
- 配置路径：`deploy/test/res/sql/nacos_config/DEFAULT_GROUP/`
- 配置文件：将 `-test.yaml` 后缀改为 `-prd.yaml`
- 导入方式：将目录打包成 zip，在 Nacos 控制台通过"导入配置"功能导入
- 用途：生产环境部署

### 配置导入步骤

1. 进入配置目录：`deploy/test/res/sql/nacos_config/DEFAULT_GROUP/`
2. 根据环境修改文件名后缀（dev/test/prd）
3. 将整个 `DEFAULT_GROUP` 目录打包成 zip 文件
4. 登录 Nacos 控制台，进入"配置管理 > 导入配置"
5. 选择 zip 文件导入，系统会自动创建对应的配置文件

## 本地开发配置

### bootstrap.yml

每个服务在 `src/main/resources/bootstrap.yml` 中配置 Nacos 连接：

```yaml
spring:
  application:
    name: zmbdp-admin-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: {命名空间ID}  # 在 Nacos 控制台创建的命名空间 ID
      config:
        server-addr: localhost:8848
        namespace: {命名空间ID}  # 与 discovery 保持一致
        file-extension: yaml
        shared-configs:
          - data-id: share-redis-{env}.yaml  # {env} 替换为实际环境：dev/test/prd
            group: DEFAULT_GROUP
            refresh: true
```

**说明**：
- `namespace` 需要在 Nacos 控制台创建命名空间后获取对应的命名空间 ID
- `discovery` 和 `config` 的 `namespace` 应保持一致
- 不同环境可以使用不同的命名空间进行隔离

### 配置优先级

1. Nacos 配置中心（最高优先级）
2. `bootstrap.yml`（本地配置）
3. `application.yml`（默认配置）

## 配置更新

配置修改后，Nacos 会自动推送到服务，实现**热更新**（需配置 `refresh: true`）。

## 注意事项

1. **敏感信息**：生产环境密码等敏感信息建议使用加密配置
2. **配置版本**：重要配置变更建议记录版本号
3. **配置备份**：定期备份 Nacos 配置，避免配置丢失
