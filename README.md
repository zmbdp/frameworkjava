<div align="center">

# FrameworkJava - 企业级Spring Boot微服务脚手架

</div>

<p align="center">
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg" alt="Spring Boot">
  </a>
  <a href="https://spring.io/projects/spring-cloud">
    <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg" alt="Spring Cloud">
  </a>
  <a href="https://github.com/alibaba/spring-cloud-alibaba">
    <img src="https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.2-blueviolet.svg" alt="Spring Cloud Alibaba">
  </a>
  <a href="https://github.com/zmbdp/frameworkjava/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/zmbdp/frameworkjava" alt="License">
  </a>
</p>

## 项目简介

FrameworkJava是一个基于Spring Boot 3.3.3和Spring Cloud 2023.0.3的企业级微服务脚手架，旨在帮助开发者快速构建高可用、高性能的Java微服务应用。项目采用模块化设计，集成了企业级应用所需的常见功能，包括但不限于统一认证授权、多级缓存、异步线程、消息队列、配置中心等。

## 核心特性

- 🚀 **微服务架构**: 基于Spring Cloud的完整微服务解决方案
- 🔒 **安全认证**: JWT无状态认证 + Redis状态管理
- ⚡ **三级缓存**: 布隆过滤器 + Caffeine本地缓存 + Redis分布式缓存
- 📦 **模块化设计**: 清晰的模块划分，便于扩展和维护
- 🛠️ **开箱即用**: 预置常见业务模块（用户管理、配置管理、地图服务等）
- 🐳 **容器化部署**: 完整的Docker Compose部署方案
- 📊 **监控友好**: 集成Docker容器健康检查和Spring Boot Actuator监控端点

## 技术栈

| 技术 | 说明 | 版本 |
|------|------|------|
| Spring Boot | 核心框架 | 3.3.3 |
| Spring Cloud | 微服务框架 | 2023.0.3 |
| Spring Cloud Alibaba | 阿里微服务组件 | 2023.0.1.2 |
| MyBatis-Plus | ORM框架 | 3.5.7 |
| Redis | 缓存和分布式锁 | 7.0.15 |
| MySQL | 主数据库 | 8.4.2 |
| Nacos | 配置中心和服务发现 | 2.2.2 |
| RabbitMQ | 消息队列 | 3.12.6 |

## 项目结构
<pre>
frameworkjava
├── zmbdp-gateway                    # 网关服务
│   └── src/main/java/com/zmbdp/gateway
│       ├── ZmbdpGatewayServiceApplication.java  # 启动类
│       ├── config/IgnoreWhiteProperties.java    # 忽略白名单配置
│       ├── filter/AuthFilter.java               # 认证过滤器
│       └── handler/GatewayExceptionHandler.java # 网关层全局异常处理器
│
├── zmbdp-common                     # 公共模块
│   ├── zmbdp-common-cache           # 缓存相关
│   │   └── src/main/java/com/zmbdp/common/cache
│   │       ├── config/CaffeineConfig.java       # Caffeine缓存配置
│   │       └── utils/CacheUtil.java             # 缓存工具类
│   │
│   ├── zmbdp-common-core            # 核心工具类
│   │   └── src/main/java/com/zmbdp/common/core
│   │       ├── config/
│   │       │   ├── MybatisPlusConfig.java       # MyBatis-Plus配置
│   │       │   ├── RestTemplateConfig.java      # RestTemplate配置
│   │       │   └── ThreadPoolConfig.java        # 线程池配置
│   │       ├── domain/
│   │       │   ├── dto/BasePageDTO.java         # 分页DTO基类
│   │       │   └── entity/BaseDO.java           # 实体基类
│   │       ├── enums/
│   │       │   └── RejectType.java              # 拒绝类型枚举
│   │       └── utils/
│   │           ├── AESUtil.java                 # AES加密工具类
│   │           ├── BeanCopyUtil.java            # Bean拷贝工具类
│   │           ├── JsonUtil.java                # JSON工具类
│   │           ├── PageUtil.java                # 分页工具类
│   │           ├── ServletUtil.java             # Servlet工具类
│   │           ├── StringUtil.java              # 字符串工具类
│   │           ├── TimestampUtil.java           # 时间戳工具类
│   │           └── VerifyUtil.java              # 验证工具类
│   │
│   ├── zmbdp-common-domain          # 公共领域对象
│   │   └── src/main/java/com/zmbdp/common/domain
│   │       ├── constants/
│   │       │   ├── CacheConstants.java          # 缓存常量
│   │       │   ├── CommonConstants.java         # 通用常量
│   │       │   ├── HttpConstants.java           # HTTP常量
│   │       │   ├── MessageConstants.java        # 消息常量
│   │       │   ├── SecurityConstants.java       # 安全常量
│   │       │   ├── TokenConstants.java          # Token常量
│   │       │   └── UserConstants.java           # 用户常量
│   │       ├── domain/
│   │       │   ├── Result.java                  # 统一响应结果
│   │       │   ├── ResultCode.java              # 响应码枚举
│   │       │   ├── dto/BasePageReqDTO.java      # 分页请求DTO
│   │       │   └── vo/...                       # 各种VO对象
│   │       └── exception/
│   │           └── ServiceException.java        # 业务异常
│   │
│   ├── zmbdp-common-filter          # 过滤器相关
│   │   └── src/main/java/com/zmbdp/common/bloomfilter
│   │       ├── config/BloomFilterConfig.java    # 布隆过滤器配置
│   │       └── service/BloomFilterService.java  # 布隆过滤器服务
│   │
│   ├── zmbdp-common-message         # 消息服务
│   │   └── src/main/java/com/zmbdp/common/message
│   │       ├── config/AliSmsConfig.java         # 阿里云短信配置
│   │       └── service/...                      # 短信和验证码服务
│   │
│   ├── zmbdp-common-rabbitmq        # RabbitMQ相关
│   │   └── src/main/java/com/zmbdp/common/rabbitmq
│   │       └── config/RabbitMqCommonConfig.java # RabbitMQ通用配置
│   │
│   ├── zmbdp-common-redis           # Redis相关
│   │   └── src/main/java/com/zmbdp/common/redis
│   │       ├── config/
│   │       │   └── RedisConfig.java             # Redis配置
│   │       └── service/
│   │           ├── RedisService.java            # Redis服务
│   │           └── RedissonLockService.java     # Redisson分布式锁服务
│   │
│   ├── zmbdp-common-security        # 安全相关
│   │   └── src/main/java/com/zmbdp/common/security
│   │       ├── domain/dto/
│   │       │   ├── LoginUserDTO.java            # 登录用户DTO
│   │       │   └── TokenDTO.java                # Token DTO
│   │       ├── handler/
│   │       │   └── GlobalExceptionHandler.java  # 服务层全局异常处理器
│   │       ├── service/
│   │       │   └── TokenService.java            # Token服务
│   │       └── utils/
│   │           ├── JwtUtil.java                 # JWT工具类
│   │           └── SecurityUtil.java            # 安全工具类
│   │
├── zmbdp-admin                      # 管理服务
│   ├── zmbdp-admin-api              # API接口定义
│   └── zmbdp-admin-service          # 业务实现
│       └── user/config/RabbitConfig.java        # RabbitMQ交换机配置
│
├── zmbdp-file                       # 文件服务
├── zmbdp-portal                     # 门户服务
└── zmbdp-mstemplate                 # 微服务模板
</pre>

## 部署环境

项目支持多种部署环境：

1. **开发环境** (`deploy/dev/`) - 用于本地开发和测试
2. **测试环境** (`deploy/test/`) - 用于集成测试和预发布
3. **生产环境** (`deploy/prd/`) - 用于生产部署

每个环境都包含完整的Docker Compose配置和相关中间件配置。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Docker & Docker Compose

### 一键部署

1. 克隆项目<p>
```bash
git clone https://github.com/zmbdp/frameworkjava.git cd frameworkjava
```
2. 启动基础服务<p>
```bash
# 开发环境
cd deploy/dev/app
docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
# 测试环
cd deploy/test/app
docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
# 生产环境
cd deploy/prd/app
docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
```
3. 等待服务启动完成（约2-3分钟），检查服务状态<p>
```bash
docker-compose -p frameworkjava -f docker-compose-mid.yml ps
```
4. 构建并启动应用服务<p>
```bash
# 返回项目根目录
cd ../../../
mvn clean install -DskipTests
# 启动各服务模块...
# 启动网关服务
cd zmbdp-gateway/zmbdp-gateway-service
mvn spring-boot:run
# 启动管理服务
cd ../../zmbdp-admin/zmbdp-admin-service
mvn spring-boot:run
# 启动其他服务...
```


### 访问地址

- Nacos控制台: http://localhost:8848/nacos (用户名/密码: nacos/Hf@173503494)
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
- 支持复杂泛型类型缓存
- 缓存工具类封装完整操作流程

### 2. 安全认证机制

采用JWT无状态认证：
- 网关层统一鉴权
- 用户信息加密存储
- 支持B端和C端用户分离认证
- 支持白名单配置
- Token与Redis结合实现状态管理

### 3. 微服务治理

- Nacos配置中心和服务发现
- 网关统一路由和限流
- Feign服务间调用
- 统一异常处理和响应格式

### 4. 高性能线程池

- 支持四种拒绝策略配置（AbortPolicy、CallerRunsPolicy、DiscardOldestPolicy、DiscardPolicy）
- 参数可动态配置
- 线程安全关闭机制
- 异步任务执行支持

### 5. 完善的工具类库

- BeanCopyUtil：支持List、Map、Map<List>等复杂结构对象拷贝
- JsonUtil：全面的JSON处理能力，支持Java 8时间类型
- CacheUtil：封装完整的三级缓存操作
- JwtUtil：完整的JWT处理功能

### 6. 增强型布隆过滤器

- 线程安全实现
- 支持动态重置
- 精确计数和近似计数双重统计
- 负载率和误判率监控
- 定时任务自动刷新

### 7. 完整的用户管理体系

- B端用户管理：登录、注册、权限控制
- C端用户管理：微信登录、手机号登录、用户信息维护
- 用户信息服务：用户信息获取、编辑、退出登录

### 8. 配置管理服务

- 参数配置管理
- 支持根据键值查询配置
- 支持批量查询配置

### 9. 地图服务功能

- 城市列表获取
- 城市拼音归类查询
- 热门城市列表
- 根据关键词搜索地点
- 根据经纬度定位城市

### 10. 文件服务功能

- 文件上传功能
- 签名信息获取
- Feign远程调用接口

### 11. 消息队列集成

- RabbitMQ集成
- 消息生产者和消费者示例
- 支持消息的发送和接收

### 12. 定时任务系统

- 布隆过滤器定时刷新任务
- 每天凌晨4点自动执行
- 日志记录和异常处理

### 13. 完善的异常处理机制

- 统一异常处理
- 业务异常封装
- 错误码体系

### 14. 标准化的API设计

- Feign远程调用接口
- RESTful API设计
- 统一响应格式

### 15. 容器化部署支持

- Docker Compose部署方案
- Nacos配置中心集成
- 完整的中间件支持（MySQL、Redis、RabbitMQ）

### 16. SDK开发文档

项目提供完整的SDK开发文档，位于`javapro/javadoc`目录下：
- 基于JavaDoc生成的完整API文档
- 包含所有公共接口、类、方法的详细说明
- 支持在线浏览和搜索功能
- 为开发者提供便捷的参考手册

访问方式：
1. 在项目根目录下找到`javapro/javadoc/index.html`文件
2. 使用浏览器直接打开该文件即可浏览完整的SDK文档
3. 例如：`file:///{项目路径}/javapro/javadoc/index.html`


## 配置说明

主要配置项位于Nacos配置中心，包括：
- **服务配置**: 
  - zmbdp-mstemplate-service-dev.yaml（微服务模板配置）
  - zmbdp-gateway-service-dev.yaml（网关配置）
- **公共组件配置**: 
  - share-redis-dev.yaml（Redis公共配置）
  - share-caffeine-dev.yaml（本地缓存公共配置）
  - share-rabbitmq-dev.yaml（RabbitMQ公共配置）
  - share-mysql-dev.yaml（MySQL公共配置）
  - share-map-dev.yaml（地图服务公共配置）
  - share-token-dev.yaml（Token公共配置）
  - share-sms-dev.yaml（短信服务公共配置）
  - share-filter-dev.yaml（ [布隆] 过滤器公共配置）
- **业务服务配置**: 
  - zmbdp-file-service-dev.yaml（文件服务配置）
  - zmbdp-admin-service-dev.yaml（基础服务配置）
  - zmbdp-portal-service-dev.yaml（C端用户服务配置）



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
