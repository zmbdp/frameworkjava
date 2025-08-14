# FrameworkJava - 企业级Spring Boot微服务脚手架

<p align="center">
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg" alt="Spring Boot">
  </a>
  <a href="https://github.com/alibaba/spring-cloud-alibaba">
    <img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-blue.svg" alt="Spring Cloud Alibaba">
  </a>
  <a href="https://github.com/zmbdp/frameworkjava/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/zmbdp/frameworkjava" alt="License">
  </a>
</p>

## 项目简介

FrameworkJava是一个基于Spring Boot 3.3.3和Spring Cloud Alibaba 2023.0.1.0的企业级微服务脚手架，旨在帮助开发者快速构建高可用、高性能的Java微服务应用。项目采用模块化设计，集成了企业级应用所需的常见功能，包括但不限于统一认证授权、多级缓存、消息队列、配置中心等。

## 核心特性

- 🚀 **微服务架构**: 基于Spring Cloud的完整微服务解决方案
- 🔒 **安全认证**: JWT无状态认证 + Redis状态管理
- ⚡ **三级缓存**: 布隆过滤器 + Caffeine本地缓存 + Redis分布式缓存
- 📦 **模块化设计**: 清晰的模块划分，便于扩展和维护
- 🛠️ **开箱即用**: 预置常见业务模块（用户管理、配置管理、地图服务等）
- 🐳 **容器化部署**: 完整的Docker Compose部署方案
- 📊 **监控友好**: 集成健康检查和监控端点

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.3.3 | 核心框架 |
| Spring Cloud | 2023.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.2 | 阿里微服务组件 |
| MyBatis-Plus | 3.5.7 | ORM框架 |
| Redis | 7.0.15 | 缓存和分布式锁 |
| MySQL | 8.4.2 | 主数据库 |
| Nacos | 2.2.2 | 配置中心和服务发现 |
| RabbitMQ | 3.12.6 | 消息队列 |

## 项目结构
frameworkjava<p>
├── zmbdp-gateway          # 网关服务<p>
│   ├── src/main/java/com/zmbdp/gateway<p>
│   │   ├── ZmbdpGatewayServiceApplication.java    # 启动类<p>
│   │   ├── config/IgnoreWhiteProperties.java      # 忽略白名单配置<p>
│   │   ├── filter/AuthFilter.java                 # 认证过滤器<p>
│   │   └── handler/GatewayExceptionHandler.java   # 全局异常处理器<p>
│<p>
├── zmbdp-common           # 公共模块<p>
│   ├── zmbdp-common-cache     # 缓存相关<p>
│   │   ├── src/main/java/com/zmbdp/common/cache
│   │   │   ├── config/BloomFilterConfig.java      # 布隆过滤器配置<p>
│   │   │   ├── service/BloomFilterService.java    # 布隆过滤器服务<p>
│   │   │   └── utils/CacheUtil.java               # 缓存工具类<p>
│   │<p>
│   ├── zmbdp-common-core      # 核心工具类<p>
│   │   ├── src/main/java/com/zmbdp/common/core<p>
│   │   │   ├── config/MybatisPlusConfig.java      # MyBatis-Plus配置<p>
│   │   │   ├── config/RestTemplateConfig.java     # RestTemplate配置<p>
│   │   │   ├── config/ThreadPoolConfig.java       # 线程池配置<p>
│   │   │   ├── domain/BasePageDTO.java            # 分页DTO基类<p>
│   │   │   ├── entity/BaseDO.java                 # 实体基类<p>
│   │   │   ├── enums/RejectType.java              # 拒绝类型枚举<p>
│   │   │   └── utils/...                          # 各种工具类<p>
│   │<p>
│   ├── zmbdp-common-domain    # 公共领域对象<p>
│   │   ├── src/main/java/com/zmbdp/common/domain<p>
│   │   │   ├── constants/...                      # 常量类<p>
│   │   │   ├── Result.java                        # 统一响应结果<p>
│   │   │   ├── dto/BasePageReqDTO.java            # 分页请求DTO<p>
│   │   │   └── exception/ServiceException.java    # 业务异常<p>
│   │<p>
│   ├── zmbdp-common-messaging # 消息服务<p>
│   │   ├── src/main/java/com/zmbdp/common/messaging<p>
│   │   │   ├── config/RabbitMqCommonConfig.java   # RabbitMQ配置<p>
│   │   │   └── service/AliSmsService.java         # 阿里云短信服务<p>
│   │<p>
│   ├── zmbdp-common-redis     # Redis相关<p>
│   │   ├── src/main/java/com/zmbdp/common/redis<p>
│   │   │   ├── config/RedisConfig.java            # Redis配置<p>
│   │   │   └── service/RedisService.java          # Redis服务<p>
│   │<p>
│   ├── zmbdp-common-security  # 安全相关<p>
│   │   ├── src/main/java/com/zmbdp/common/security<p>
│   │   │   ├── config/JwtConfig.java              # JWT配置<p>
│   │   │   ├── service/TokenService.java          # Token服务<p>
│   │   │   └── utils/JwtUtil.java                 # JWT工具类<p>
│   │<p>
│   └── zmbdp-common-thread    # 线程相关<p>
│       ├── src/main/java/com/zmbdp/common/thread<p>
│       │   ├── config/ThreadPoolConfig.java       # 线程池配置<p>
│       │   ├── executor/CustomThreadPoolExecutor.java # 自定义线程池执行器<p>
│       │   └── utils/ThreadUtils.java             # 线程工具类<p>
│<p>
├── zmbdp-admin            # 管理服务<p>
│   ├── zmbdp-admin-api      # API接口定义<p>
│   └── zmbdp-admin-service  # 业务实现<p>
│<p>
├── zmbdp-file             # 文件服务<p>
└── zmbdp-portal           # 门户服务<p>


## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Docker & Docker Compose

### 一键部署

1. 克隆项目
bash git clone https://github.com/yourusername/frameworkjava.git cd frameworkjava
2. 启动基础服务
bash cd deploy/dev/app docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
3. 等待服务启动完成（约2-3分钟），检查服务状态
bash docker-compose -p frameworkjava -f docker-compose-mid.yml ps
4. 构建并启动应用服务
bash
返回项目根目录
cd ../../../ mvn clean install -DskipTests
启动各服务模块...

### 访问地址

- Nacos控制台: http://localhost:8848/nacos (用户名/密码: nacos/nacos)
- Redis: localhost:6379
- MySQL: localhost:3306
- RabbitMQ管理界面: http://localhost:15672 (用户名/密码: zmbdp/Hf@173503494)

## 核心亮点详解

### 1. 三级缓存架构

项目实现了布隆过滤器 + Caffeine + Redis的三级缓存架构：
请求 -> 布隆过滤器(判断是否存在) -> Caffeine本地缓存 -> Redis缓存 -> 数据库

优势：
- 布隆过滤器有效防止缓存穿透
- 本地缓存提供最快访问速度
- 分布式缓存保证数据一致性

### 2. 安全认证机制

采用JWT无状态认证：
- 网关层统一鉴权
- 用户信息加密存储
- 支持B端和C端用户分离认证

### 3. 微服务治理

- Nacos配置中心和服务发现
- 网关统一路由和限流
- Feign服务间调用
- 统一异常处理和响应格式

## 配置说明

主要配置项位于Nacos配置中心，包括：
- 数据库连接配置
- Redis连接配置
- JWT密钥配置
- 缓存参数配置
- 短信服务配置

## 开发指南

### 新增业务模块

1. 在`zmbdp-admin`或创建新模块
2. 遵循Controller-Service-Mapper分层架构
3. 使用统一响应格式和异常处理
4. 集成到网关路由配置

### 扩展缓存功能

java // 使用CacheUtil工具类 <p>
T result = CacheUtil.getL2Cache(redisService, bloomFilterService, key, valueTypeRef, caffeineCache);

## 性能优化

1. **数据库优化**: 连接池配置、索引优化
2. **缓存优化**: 多级缓存减少数据库访问
3. **并发优化**: Redisson分布式锁、线程池配置
4. **JVM优化**: 合理的堆内存和GC配置

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目：

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

## 许可证

本项目采用MIT许可证，详情请见 [LICENSE](LICENSE) 文件。

## 联系方式

- 作者: 稚名不带撇
- 邮箱: JavaFH@163.com
- GitHub: [https://github.com/zmbdp](https://github.com/zmbdp)

## 鸣谢

- 感谢所有为开源社区做出贡献的开发者
- 特别感谢 Spring 团队提供的优秀框架
