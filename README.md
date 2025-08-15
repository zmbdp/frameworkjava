# FrameworkJava - 企业级Spring Boot微服务脚手架

<align"center">
  <href"https://spring.io/projects/spring-boot">
    <src"https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg" alt"Spring Boot">
  </>
  <href"https://spring.io/projects/spring-cloud">
    <src"https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg" alt"Spring Cloud">
  </>
  <href"https://github.com/zmbdp/frameworkjava/blob/master/LICENSE">
    <src"https://img.shields.io/github/license/zmbdp/frameworkjava" alt"License">
  </>
</>

## 项目简介

FrameworkJava是一个基于Spring Boot 3.3.3和Spring Cloud 2023.0.3的企业级微服务脚手架，旨在帮助开发者快速构建高可用、高性能的Java微服务应用。项目采用模块化设计，集成了企业级应用所需的常见功能，包括但不限于统一认证授权、多级缓存、消息队列、配置中心等。

## 核心特性

-**微服务架构**
-**安全认证**
-**三级缓存**
-**模块化设计**
-**开箱即用**
-**容器化部署**
-**监控友好**

## 技术栈

| 技术 | 说明 | 版本 |
|------|------|------|
||||
||||
||||
||||
||||
||||
||||
||||

## 项目结构
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
<>
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
bash<p>
`git clone https://github.com/zmbdp/frameworkjava.git cd frameworkjava`
2. 启动基础服务
bash<p>
`cd deploy/dev/app`<p>
`docker-compose -p frameworkjava -f docker-compose-mid.yml up -d`
3. 等待服务启动完成（约2-3分钟），检查服务状态<p>
`bash`<p>
`docker-compose -p frameworkjava -f docker-compose-mid.yml ps`
4. 构建并启动应用服务
`bash`<p>
返回项目根目录
`cd ../../../`<p>
`mvn clean install -DskipTests`<p>
启动各服务模块...

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

-
-
-



## 配置说明

主要配置项位于Nacos配置中心，包括：
-
-
-
-
-

## 开发指南

### 新增业务模块

`zmbdp-admin`
2.
3.
4.

### 扩展缓存功能

<>
T result = CacheUtil.getL2Cache(redisService, bloomFilterService, key, valueTypeRef, caffeineCache);

## 性能优化

1. **数据库优化**
2. **缓存优化**
3. **并发优化**
4. **JVM优化**

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进项目：

1.
`git checkout -b feature/AmazingFeature`
`git commit -m 'Add some AmazingFeature'`
`git push origin feature/AmazingFeature`
5.

## 许可证

[LICENSE](LICENSE)

## 联系方式

-
-
[https://github.com/zmbdp](https://github.com/zmbdp)

## 鸣谢

-
-
