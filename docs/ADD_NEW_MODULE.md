# 新增业务模块指南

本文档说明如何在 FrameworkJava 中新增一个业务模块。

## 模块结构

每个业务模块遵循 **API + Service** 分离的结构：

```
zmbdp-{module}/
├── zmbdp-{module}-api/          # API 模块
│   ├── pom.xml
│   └── src/main/java/
│       └── com/zmbdp/{module}/api/
│           ├── domain/          # DTO、VO
│           ├── constants/        # 常量
│           └── feign/            # Feign 客户端（可选）
└── zmbdp-{module}-service/      # Service 模块
    ├── pom.xml
    ├── Dockerfile
    └── src/main/
        ├── java/
        │   └── com/zmbdp/{module}/service/
        │       ├── controller/   # 控制器
        │       ├── service/      # 服务层
        │       │   └── impl/     # 服务实现
        │       ├── mapper/       # MyBatis Mapper
        │       └── {Module}ServiceApplication.java
        └── resources/
            ├── banner.txt
            ├── bootstrap.yml
            └── mapper/           # MyBatis XML
```

## 创建步骤

### 1. 创建模块目录

在项目根目录下创建模块文件夹：

```bash
mkdir -p zmbdp-{module}/zmbdp-{module}-api
mkdir -p zmbdp-{module}/zmbdp-{module}-service
```

### 2. 创建 API 模块

#### 2.1 创建 pom.xml

参考 `zmbdp-portal/zmbdp-portal-api/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <parent>
        <groupId>com.zmbdp</groupId>
        <artifactId>frameworkjava</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>zmbdp-{module}-api</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.zmbdp</groupId>
            <artifactId>zmbdp-common-domain</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### 2.2 创建 DTO/VO

在 `zmbdp-{module}-api/src/main/java/com/zmbdp/{module}/api/domain/` 下创建：

- `{Module}Request.java` - 请求 DTO
- `{Module}Response.java` - 响应 VO

### 3. 创建 Service 模块

#### 3.1 创建 pom.xml

参考 `zmbdp-portal/zmbdp-portal-service/pom.xml`，主要依赖：

```xml
<dependencies>
    <!-- API 模块 -->
    <dependency>
        <groupId>com.zmbdp</groupId>
        <artifactId>zmbdp-{module}-api</artifactId>
    </dependency>
    
    <!-- 公共模块 -->
    <dependency>
        <groupId>com.zmbdp</groupId>
        <artifactId>zmbdp-common-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zmbdp</groupId>
        <artifactId>zmbdp-common-redis</artifactId>
    </dependency>
    <!-- 其他需要的 common 模块 -->
    
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
    </dependency>
</dependencies>
```

#### 3.2 创建启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class {Module}ServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run({Module}ServiceApplication.class, args);
    }
}
```

#### 3.3 创建 bootstrap.yml

参考其他服务的配置：

```yaml
spring:
  application:
    name: zmbdp-{module}-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: dev
      config:
        server-addr: localhost:8848
        namespace: dev
        file-extension: yaml
        shared-configs:
          - data-id: share-redis-dev.yaml
            group: DEFAULT_GROUP
            refresh: true
          - data-id: share-mysql-dev.yaml
            group: DEFAULT_GROUP
            refresh: true
```

### 4. 注册到父 POM

在根目录 `pom.xml` 的 `<modules>` 中添加：

```xml
<modules>
    <!-- 其他模块 -->
    <module>zmbdp-{module}</module>
</modules>
```

在 `zmbdp-{module}/pom.xml` 中：

```xml
<modules>
    <module>zmbdp-{module}-api</module>
    <module>zmbdp-{module}-service</module>
</modules>
```

### 5. 创建 Nacos 配置

在 Nacos 配置中心创建配置文件：`zmbdp-{module}-service-dev.yaml`

```yaml
server:
  port: 8080
```

### 6. 配置网关路由

在网关服务中添加路由配置（如使用配置文件）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: {module}-service
          uri: lb://zmbdp-{module}-service
          predicates:
            - Path=/{module}/**
          filters:
            - StripPrefix=1
```

## 代码示例

### Controller

```java
@RestController
@RequestMapping("/api/{module}")
public class {Module}Controller {
    
    @Autowired
    private I{Module}Service {module}Service;
    
    @PostMapping("/create")
    @Idempotent(expireTime = 300) // 幂等性注解（可选），详见 IDEMPOTENT.md
    public Result create(@RequestBody {Module}Request request) {
        return Result.success({module}Service.create(request));
    }
}
```

### Service

```java
public interface I{Module}Service {
    {Module}Response create({Module}Request request);
}

@Service
public class {Module}ServiceImpl implements I{Module}Service {
    
    @Autowired
    private {Module}Mapper {module}Mapper;
    
    @Override
    public {Module}Response create({Module}Request request) {
        // 业务逻辑
        return new {Module}Response();
    }
}
```

## 参考模板

可以直接参考 `zmbdp-mstemplate` 模块，它提供了完整的模板代码。

## 注意事项

1. **模块命名**：使用小写字母和连字符，如 `zmbdp-order-service`
2. **包名规范**：遵循 `com.zmbdp.{module}` 规范
3. **依赖管理**：只依赖 common 模块，不依赖其他业务模块
4. **配置管理**：使用 Nacos 配置中心，不要硬编码配置
5. **异常处理**：使用统一的异常处理机制
6. **日志规范**：使用统一的日志格式

## 验证清单

创建完成后，按以下步骤验证模块是否正常工作：

1. **模块可以正常启动**
   - 运行启动类，检查是否有启动错误
   - 查看控制台日志，确认服务启动成功

2. **可以注册到 Nacos**
   - 访问 Nacos 控制台：http://localhost:8848/nacos
   - 在"服务管理 > 服务列表"中查看服务是否已注册

3. **网关可以路由到新服务**
   - 通过网关访问接口，检查路由是否正常
   - 确认网关日志中路由转发成功

4. **数据库连接正常**
   - 检查启动日志，确认数据库连接池初始化成功
   - 执行一个简单的查询接口验证

5. **Redis 连接正常**
   - 检查启动日志，确认 Redis 连接成功
   - 执行一个使用 Redis 的接口验证

6. **日志输出正常**
   - 确认日志格式统一
   - 确认日志级别配置正确

7. **接口可以正常访问**
   - 使用 Postman 或 curl 测试接口
   - 验证请求和响应格式正确
