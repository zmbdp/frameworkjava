<div align="center">

# FrameworkJava - 企业级Spring Boot微服务脚手架

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.2-blueviolet.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/github/license/zmbdp/frameworkjava)](https://github.com/zmbdp/frameworkjava/blob/master/LICENSE)

**一个开箱即用的企业级微服务脚手架，助力快速构建高可用、高性能的Java微服务应用**

[功能特性](#核心特性) • [快速开始](#-快速开始) • [文档](#21-api文档和使用手册) • [问题反馈](https://github.com/zmbdp/frameworkjava/issues)

</div>

---

## 📑 目录

- [项目简介](#-项目简介)
  - [为什么选择FrameworkJava？](#为什么选择frameworkjava)
  - [适用场景](#适用场景)
  - [系统架构](#系统架构)
- [核心特性](#核心特性)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [部署环境](#-部署环境)
- [快速开始](#-快速开始)
  - [环境要求](#环境要求)
  - [一键部署](#一键部署)
  - [访问地址](#访问地址)
- [核心亮点详解](#-核心亮点详解)
- [配置说明](#-配置说明)
- [开发指南](#-开发指南)
- [常见问题](#-常见问题)
- [性能优化](#-性能优化)
- [贡献指南](#-贡献指南)
- [API文档和使用手册](#22-api文档和使用手册)
- [许可证](#-许可证)
- [联系方式](#-联系方式)

---

## 📖 项目简介

FrameworkJava是一个基于Spring Boot 3.3.3和Spring Cloud 2023.0.3的企业级微服务脚手架，旨在帮助开发者快速构建高可用、高性能的Java微服务应用。

### 为什么选择FrameworkJava？

- ✅ **开箱即用**：预置企业级应用所需的核心功能，无需从零开始
- ✅ **模块化设计**：清晰的模块划分，便于扩展和维护
- ✅ **最佳实践**：集成了业界成熟的技术方案和设计模式
- ✅ **完整文档**：提供详细的开发文档和使用手册
- ✅ **持续更新**：活跃的社区支持和持续的功能迭代

### 适用场景

- 🏢 企业级微服务应用开发
- 📦 快速搭建项目脚手架
- 🎓 学习Spring Cloud微服务架构
- 🔧 作为技术选型参考

项目采用模块化设计，集成了企业级应用所需的常见功能，包括但不限于统一认证授权、多级缓存、异步线程、消息队列、配置中心、幂等性控制等。

### 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│                    (Web/Mobile/API)                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                     网关服务层                               │
│              (Spring Cloud Gateway)                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  认证过滤器 → 路由转发 → 限流控制 → 异常处理            │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼────────────────┐
        │               │                │
┌───────▼──────┐ ┌──────▼──────┐ ┌───────▼──────┐
│  管理服务     │ │  文件服务   │  │  门户服务    │
│ (Admin)      │ │  (File)     │ │   (Portal)   │
└───┬──────┬───┘ └───┬──────┬──┘ └────┬─────┬───┘
    │      │         │      │         │     │
    │      │         │      │         │     │
    │      │         │      │         │     │
    └──────┼─────────┼──────┼─────────┼─────┘
           │         │      │         │
    ┌──────┼─────────┼──────┼─────────┼────┐
    │      │         │      │         │    │
┌───▼──┐   │     ┌───▼──┐   │     ┌───▼──┐ │
│Nacos │   │     │Redis │   │     │Rabbit│ │
└──────┘   │     └───┬──┘   │     └──────┘ │
           │         │      │              │
           │         │      │              │
           └─────────┼──────┼──────────────┘
                     │      │
              ┌──────▼──────▼──────┐
              │        MySQL       │
              └────────────────────┘
```

**架构特点**：
- 🏗️ **分层架构**：清晰的网关层、服务层、数据层划分
- 🔄 **服务治理**：基于Nacos实现服务注册发现和配置管理
- 💾 **数据存储**：MySQL主数据库 + Redis缓存 + RabbitMQ消息队列
- 🔐 **安全认证**：网关层统一认证，支持JWT无状态认证
- ⚡ **性能优化**：三级缓存架构，有效提升系统性能

## 核心特性

- 🚀 **微服务架构**: 基于Spring Cloud的完整微服务解决方案
- 🔒 **安全认证**: JWT无状态认证 + Redis状态管理
- ⚡ **三级缓存**: 布隆过滤器 + Caffeine本地缓存 + Redis分布式缓存
- 🔄 **幂等性控制**: 基于AOP的分布式幂等性解决方案，支持HTTP和MQ场景
- 📦 **模块化设计**: 清晰的模块划分，便于扩展和维护
- 🛠️ **开箱即用**: 预置常见业务模块（用户管理、配置管理、地图服务等）
- 📊 **Excel处理**: 完整的Excel导入导出功能，支持大数值、单元格合并等
- 📧 **智能验证码**: 根据用户输入格式（手机号/邮箱）自动选择短信或邮件发送验证码
- 📨 **邮件服务**: 完善的邮件发送功能，支持HTML、附件、内嵌图片等
- 🐳 **容器化部署**: 完整的Docker Compose部署方案
- 📈 **监控友好**: 集成Docker容器健康检查和Spring Boot Actuator监控端点

## 🛠️ 技术栈

### 核心框架

| 技术 | 说明 | 版本 | 用途 |
|------|------|------|------|
| Spring Boot | 核心框架 | 3.3.3 | 应用基础框架 |
| Spring Cloud | 微服务框架 | 2023.0.3 | 微服务治理 |
| Spring Cloud Alibaba | 阿里微服务组件 | 2023.0.1.2 | 服务注册发现、配置中心 |
| MyBatis-Plus | ORM框架 | 3.5.7 | 数据库操作 |
| EasyExcel | Excel处理 | 3.x | Excel导入导出 |

### 中间件

| 技术 | 版本 | 用途 |
|------|------|------|
| Redis | 7.0.15 | 缓存、分布式锁、幂等性控制 |
| MySQL | 8.4.2 | 主数据库 |
| Nacos | 2.2.2 | 配置中心和服务发现 |
| RabbitMQ | 3.12.6 | 消息队列 |

### 工具库

| 技术 | 说明 |
|------|------|
| Hutool | Java工具类库 |
| Caffeine | 本地缓存 |
| Redisson | 分布式锁 |
| JWT | 无状态认证 |

## 📁 项目结构
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
│   │       ├── annotation/
│   │       │   └── excel/
│   │       │       └── CellMerge.java           # Excel单元格合并注解
│   │       ├── config/
│   │       │   ├── MailConfig.java              # 邮件配置
│   │       │   ├── MybatisPlusConfig.java       # MyBatis-Plus配置
│   │       │   ├── RestTemplateConfig.java      # RestTemplate配置
│   │       │   └── ThreadPoolConfig.java        # 线程池配置
│   │       ├── domain/
│   │       │   ├── dto/BasePageDTO.java         # 分页DTO基类
│   │       │   └── entity/BaseDO.java           # 实体基类
│   │       ├── enums/
│   │       │   └── RejectType.java              # 拒绝类型枚举
│   │       ├── excel/
│   │       │   ├── CellMergeStrategy.java       # Excel单元格合并策略
│   │       │   ├── DefaultExcelListener.java    # Excel默认导入监听器
│   │       │   ├── DefaultExcelResult.java      # Excel默认导入结果
│   │       │   ├── ExcelBigNumberConverter.java # Excel大数值转换器
│   │       │   ├── ExcelListener.java           # Excel导入监听器接口
│   │       │   └── ExcelResult.java             # Excel导入结果接口
│   │       └── utils/
│   │           ├── AESUtil.java                 # AES加密工具类
│   │           ├── BeanCopyUtil.java            # Bean拷贝工具类
│   │           ├── ExcelUtil.java               # Excel工具类
│   │           ├── FileUtil.java                # 文件工具类
│   │           ├── JsonUtil.java                # JSON工具类
│   │           ├── MailUtil.java                # 邮件工具类
│   │           ├── PageUtil.java                # 分页工具类
│   │           ├── ServletUtil.java             # Servlet工具类
│   │           ├── StreamUtil.java              # 流工具类
│   │           ├── StringUtil.java              # 字符串工具类
│   │           ├── ThreadUtil.java              # 线程工具类
│   │           ├── TimestampUtil.java           # 时间戳工具类
│   │           ├── ValidatorUtil.java           # 校验工具类
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
│   │       └── service/
│   │           ├── BloomFilterService.java      # 布隆过滤器服务接口
│   │           └── impl/
│   │               ├── FastBloomFilterService.java  # 快速版（不加锁）布隆过滤器实现
│   │               ├── RedisBloomFilterService.java # Redis版布隆过滤器实现
│   │               └── SafeBloomFilterService.java  # 线程安全（分布式锁）版布隆过滤器实现
│   │
│   ├── zmbdp-common-message         # 消息服务
│   │   └── src/main/java/com/zmbdp/common/message
│   │       ├── config/
│   │       │   ├── AliSmsConfig.java            # 阿里云短信配置
│   │       │   └── MailCodeProperties.java      # 邮件验证码配置属性
│   │       └── service/
│   │           ├── CaptchaService.java          # 验证码服务
│   │           ├── CaptchaSenderFactory.java    # 验证码发送器工厂（根据格式自动选择）
│   │           ├── ICaptchaSender.java          # 验证码发送接口
│   │           └── impl/
│   │               ├── AliSmsServiceImpl.java   # 阿里云短信发送实现
│   │               └── MailCodeServiceImpl.java # 邮件验证码发送实现
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
│   ├── zmbdp-common-snowflake        # 雪花算法服务
│   │   └── src/main/java/com/zmbdp/common/snowflake
│   │       └── service/SnowflakeIdService.java         # 雪花算法服务
│   │
│   ├── zmbdp-common-idempotent       # 幂等性控制
│   │   └── src/main/java/com/zmbdp/common/idempotent
│   │       ├── annotation/
│   │       │   └── Idempotent.java                    # 幂等性注解
│   │       ├── aspect/
│   │       │   └── IdempotentAspect.java              # 幂等性切面
│   │       └── enums/
│   │           └── IdempotentMode.java                # 幂等性模式枚举
│   │
├── zmbdp-admin                      # 管理服务
│   ├── zmbdp-admin-api              # API接口定义
│   │   └── src/main/java/com/zmbdp/admin/api
│   │       ├── appuser/             # C端用户API
│   │       │   ├── domain/          # 领域对象（DTO、VO）
│   │       │   └── feign/           # Feign接口
│   │       ├── config/              # 字典管理服务API
│   │       │   ├── domain/          # 领域对象（参数配置、字典配置等）
│   │       │   └── feign/           # Feign接口
│   │       └── map/                 # 地图服务API
│   │           ├── constants/       # 地图常量
│   │           ├── domain/          # 领域对象（地区、位置搜索等）
│   │           └── feign/           # Feign接口
│   │
│   └── zmbdp-admin-service          # 业务实现
│       └── src/main/java/com/zmbdp/admin/service
│           ├── config/              # 字典管理服务
│           │   ├── comtroller/      # 字典管理控制器
│           │   ├── domain/          # 领域对象（实体、DTO）
│           │   ├── mapper/          # MyBatis Mapper
│           │   └── service/         # 业务服务实现
│           ├── map/                 # 地图服务
│           │   ├── controller/      # 地图服务控制器
│           │   ├── domain/          # 领域对象（实体、DTO）
│           │   ├── mapper/          # MyBatis Mapper
│           │   └── service/         # 业务服务实现
│           ├── timedtask/           # 定时任务
│           │   └── bloom/           # 布隆过滤器定时刷新任务
│           ├── user/                # 用户管理服务
│           │   ├── config/          # 用户服务配置（RabbitMQ交换机等）
│           │   ├── controller/      # 用户管理控制器（B端、C端用户）
│           │   ├── domain/          # 领域对象（实体、DTO、VO）
│           │   ├── mapper/          # MyBatis Mapper
│           │   └── service/         # 业务服务实现
│           └── ZmbdpAdminServiceApplication.java  # 启动类
│
├── zmbdp-file                       # 文件服务
│   ├── zmbdp-file-api               # API接口定义
│   │   └── src/main/java/com/zmbdp/file/api
│   │       ├── domain/              # 领域对象（DTO、VO）
│   │       └── feign/               # Feign接口
│   │
│   └── zmbdp-file-service           # 业务实现
│       └── src/main/java/com/zmbdp/file/service
│           ├── config/              # OSS配置
│           ├── constants/           # 常量定义
│           ├── controller/          # 文件服务控制器
│           ├── service/             # 业务服务实现
│           └── ZmbdpFileServiceApplication.java  # 启动类
│
├── zmbdp-portal                     # 门户服务
│   ├── zmbdp-portal-api             # API接口定义
│   │   └── src/main/java/com/zmbdp/portal/api
│   │
│   └── zmbdp-portal-service         # 业务实现
│       └── src/main/java/com/zmbdp/portal/service
│           ├── user/                # C端用户服务
│           │   ├── controller/      # 用户服务控制器
│           │   ├── domain/          # 领域对象（DTO、VO）
│           │   ├── service/         # 业务服务实现
│           │   └── validator/       # 账号校验策略
│           │       ├── AccountValidator.java         # 账号校验接口
│           │       ├── AccountValidatorFactory.java  # 账号校验工厂（根据格式自动选择）
│           │       ├── PhoneValidator.java           # 手机号校验策略
│           │       └── EmailValidator.java           # 邮箱校验策略
│           └── ZmbdpPortalServiceApplication.java  # 启动类
│
└── zmbdp-mstemplate                 # 微服务模板（模板示例，用于开发风格参考）
</pre>

## 🌍 部署环境

项目支持多种部署环境，每个环境都包含完整的Docker Compose配置和相关中间件配置：

| 环境 | 目录 | 用途 | 说明 |
|------|------|------|------|
| 开发环境 | `deploy/dev/` | 本地开发和测试 | 适合开发人员本地调试 |
| 测试环境 | `deploy/test/` | 集成测试和预发布 | 用于功能测试和性能测试 |
| 生产环境 | `deploy/prd/` | 生产部署 | 生产环境配置，需谨慎使用 |

> 💡 **提示**：不同环境的配置文件相互独立，可根据实际需求调整配置参数。

## 🚀 快速开始

### 环境要求

在开始之前，请确保您的开发环境已安装以下软件：

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Java开发工具包 |
| Maven | 3.6+ | 项目构建工具 |
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | 2.0+ | 容器编排工具 |

### 一键部署

1. 克隆项目<p>
```bash
git clone https://github.com/zmbdp/frameworkjava.git
cd frameworkjava
```
2. 启动基础服务<p>
```bash
# 开发环境
cd deploy/dev/app
docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
# 测试环境
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

服务启动成功后，可通过以下地址访问：

| 服务 | 地址 | 用户名/密码                                | 说明 |
|------|------|---------------------------------------|------|
| Nacos控制台 | http://localhost:8848/nacos | nacos / Hf@173503494                  | 配置中心和服务发现 |
| RabbitMQ管理界面 | http://localhost:15672 | zmbdp / Hf@173503494                  | 消息队列管理 |
| Redis | localhost:6379 | - / Hf@173503494 | 缓存服务 |
| MySQL | localhost:3306 | root / Hf@173503494 <br/> zmbdpdev / Hf@173503494 | 数据库服务 |

> 💡 **提示**：首次启动需要等待2-3分钟，确保所有中间件服务完全启动后再访问。

## ⭐ 核心亮点详解

本章节详细介绍项目的核心功能和技术亮点，帮助您更好地理解和使用框架。

### 1. 三级缓存架构

项目实现了布隆过滤器 + Caffeine + Redis的三级缓存架构，有效提升系统性能和可用性。

**缓存流程**：
```
请求 → 布隆过滤器(判断是否存在) → Caffeine本地缓存 → Redis缓存 → 数据库
```

**架构优势**：
- ✅ **布隆过滤器**：有效防止缓存穿透，减少无效查询
- ✅ **本地缓存**：Caffeine提供毫秒级访问速度，减少网络开销
- ✅ **分布式缓存**：Redis保证多实例间数据一致性
- ✅ **类型支持**：支持复杂泛型类型缓存，如`Map<String, List<Set<T>>>`
- ✅ **工具封装**：CacheUtil工具类封装完整操作流程，使用简单

### 2. Redis 复杂数据结构操作

RedisService 提供了对 Redis 各种数据结构的增强操作支持：

- **String 类型**：支持复杂泛型对象的存取操作
- **List 类型**：提供丰富的列表操作，包括头插、尾插、批量删除、范围获取等
- **Set 类型**：支持交集、并集、差集运算，以及元素移动等高级操作
- **ZSet (有序集合)**：支持按分数范围、排名等条件的查询和操作
- **Hash 类型**：提供批量获取字段值、数值字段原子增减等增强功能

所有操作均支持复杂泛型嵌套结构，如 `Map<String, List<Set<RegionTest>>>` 等。

### 3. 安全认证机制

采用JWT无状态认证，结合Redis实现灵活的状态管理：

**核心特性**：
- 🔐 **网关层统一鉴权**：在网关层统一处理认证逻辑，减少业务层负担
- 🔒 **用户信息加密存储**：敏感信息加密存储，保障数据安全
- 👥 **多端用户支持**：支持B端（管理端）和C端（用户端）用户分离认证
- ⚪ **白名单配置**：支持配置白名单，灵活控制接口访问权限
- 💾 **状态管理**：Token与Redis结合，支持Token刷新、注销等功能

### 4. 微服务治理

完整的微服务治理方案，保障系统稳定运行：

**核心功能**：
- 📋 **配置中心**：基于Nacos实现配置集中管理和动态刷新
- 🔍 **服务发现**：自动服务注册与发现，支持健康检查
- 🚪 **网关路由**：统一网关入口，支持路由转发和负载均衡
- 🛡️ **限流保护**：网关层限流，防止系统过载
- 📞 **服务调用**：基于Feign实现声明式服务调用
- ⚠️ **异常处理**：统一异常处理和响应格式，提升开发效率

### 5. 高性能线程池

- 支持四种拒绝策略配置（AbortPolicy、CallerRunsPolicy、DiscardOldestPolicy、DiscardPolicy）
- 参数可动态配置
- 线程安全关闭机制
- 异步任务执行支持

### 6. 完善的工具类库

- BeanCopyUtil：支持List、Map、Map<List>等复杂结构对象拷贝
- JsonUtil：全面的JSON处理能力，支持Java 8时间类型
- CacheUtil：封装完整的三级缓存操作
- JwtUtil：完整的JWT处理功能
- ExcelUtil：完整的Excel导入导出功能，支持大数值处理、单元格合并等
- MailUtil：邮件发送工具类，支持文本/HTML邮件、附件、内嵌图片等
- StreamUtil：流操作工具类，提供丰富的集合和流处理方法
- ValidatorUtil：数据校验工具类，基于Jakarta Validation框架
- ThreadUtil：线程工具类，提供线程休眠、线程池管理等实用方法
- FileUtil：文件处理工具类，支持文件下载响应头设置、文件名编码等

### 7. 增强型布隆过滤器

- 多实现版本支持（安全版、快速版和Redis版）
- 线程安全实现，支持高并发场景
- 精确计数和近似计数双重统计
- 负载率和误判率实时监控
- 配置化管理，支持Nacos动态刷新
- 定时任务自动维护
- 支持手动扩容和自动重置功能

### 8. 智能验证码系统

- **自动识别格式**：根据用户输入（手机号/邮箱）自动选择发送方式
- **策略模式设计**：账号校验和验证码发送均采用策略模式，便于扩展
- **短信和邮件双通道**：同时支持短信验证码（阿里云）和邮件验证码发送
- **动态配置**：邮件标题和内容模板支持多个选项，发送时随机选择
- **统一接口**：提供统一的验证码发送接口，无需关心具体发送方式
- **格式校验**：自动校验账号格式，确保发送到正确的目标

### 9. 完整的用户管理体系

- B端用户管理：登录、注册、权限控制
- C端用户管理：微信登录、手机号/邮箱登录、用户信息维护
- 用户信息服务：用户信息获取、编辑、退出登录

### 10. 配置管理服务

- 参数配置管理
- 支持根据键值查询配置
- 支持批量查询配置

### 11. 地图服务功能

- 城市列表获取
- 城市拼音归类查询
- 热门城市列表
- 根据关键词搜索地点
- 根据经纬度定位城市

### 12. 文件服务功能

- 文件上传功能
- 签名信息获取
- Feign远程调用接口

### 13. 消息队列集成

- RabbitMQ集成
- 消息生产者和消费者示例
- 支持消息的发送和接收

### 14. 定时任务系统

- 布隆过滤器定时刷新任务
- 每天凌晨4点自动执行
- 日志记录和异常处理

### 15. 完善的异常处理机制

- 统一异常处理
- 业务异常封装
- 错误码体系

### 16. Excel导入导出功能

- 基于EasyExcel实现的Excel导入导出
- 支持大数值处理（超过15位自动转换为字符串）
- 支持单元格合并（基于注解自动合并相同值）
- 支持数据校验（基于Jakarta Validation）
- 支持自定义转换器和监听器
- 提供完整的导入结果反馈机制

### 17. 邮件发送功能

- 基于Hutool Mail + Jakarta Mail的邮件发送
- 支持文本/HTML格式邮件
- 支持抄送（CC）、密送（BCC）
- 支持附件和内嵌图片（cid方式）
- 支持自定义MailAccount或使用全局配置
- 线程安全的邮件账号配置

### 18. 标准化的API设计

- Feign远程调用接口
- RESTful API设计
- 统一响应格式

### 19. 容器化部署支持

- Docker Compose部署方案
- Nacos配置中心集成
- 完整的中间件支持（MySQL、Redis、RabbitMQ）

### 20. SDK开发文档

项目提供完整的SDK开发文档，位于`javapro/javadoc`目录下：
- 基于JavaDoc生成的完整API文档
- 包含所有公共接口、类、方法的详细说明
- 支持在线浏览和搜索功能
- 为开发者提供便捷的参考手册

访问方式：
1. 在项目根目录下找到`javapro/javadoc/index.html`文件
2. 使用浏览器直接打开该文件即可浏览完整的SDK文档
3. 例如：`file:///{项目路径}/javapro/javadoc/index.html`

### 21. 分布式幂等性控制

项目提供了完整的幂等性解决方案，基于AOP实现，支持HTTP请求和MQ消息两种场景：

**核心特性**：
- **防重模式**：重复请求直接报错，适用于支付、下单等不允许重复执行的场景
- **强幂等模式**：重复请求返回第一次的结果，适用于需要保证多次调用返回相同结果的场景
- **三态设计**：支持DEFAULT（使用全局配置）、TRUE（强制开启）、FALSE（强制关闭）三种模式
- **多种Token获取方式**：支持请求头、请求参数、SpEL表达式、MQ消息头等多种方式
- **业务失败重试**：方法执行失败时自动删除Token，允许重试，支持重试次数追踪
- **重试次数管理**：基于Redis持久化重试次数，支持跨请求追踪，最多重试3次（Nacos上可配置）

**使用方式**：
```java
// 1. 基础用法（防重模式）
@PostMapping("/createOrder")
@Idempotent(headerName = "Idempotent-Token", expireTime = 300)
public Result<String> createOrder() {
    // 客户端请求头：Idempotent-Token: token-123456
    // 第一次请求：执行方法
    // 第二次请求：返回错误"请勿重复提交"
    return Result.success("订单创建成功");
}

// 2. 强幂等模式（返回缓存结果）
@GetMapping("/getOrderInfo")
@Idempotent(headerName = "Idempotent-Token", returnCachedResult = IdempotentMode.TRUE)
public Result<OrderInfo> getOrderInfo() {
    // 第一次请求：执行方法并缓存结果
    // 第二次请求：返回第一次的结果，不执行方法
    return Result.success(orderInfo);
}

// 3. MQ消费者场景（SpEL表达式）
@RabbitListener(queues = "order.queue")
@Idempotent(tokenExpression = "#messageDTO.idempotentToken")
public void handleOrder(MessageDTO messageDTO) {
    // 第一次消费：处理消息
    // 第二次消费：拒绝处理（防重模式）或返回缓存结果（强幂等模式）
}
```

**配置说明**：
- Token获取优先级：SpEL表达式 > HTTP请求头 > HTTP请求参数 > RabbitMQ消息头
- 支持全局配置和注解配置，注解配置优先级更高
- 支持Nacos动态刷新配置
- 业务失败时自动删除Token，允许重试，最多重试3次（Nacos上可配置）

### 22. API文档和使用手册

- **API文档**: [https://zmbdpframeworkjava.apifox.cn](https://zmbdpframeworkjava.apifox.cn) (访问密码: zmbdp@123.com)
- **使用手册**: [https://gcnrxp4nkh9d.feishu.cn/docx/GVUPdzmLJoWhMNxygsQc1F3Enyd?from=from_copylink](https://gcnrxp4nkh9d.feishu.cn/docx/GVUPdzmLJoWhMNxygsQc1F3Enyd?from=from_copylink)


## ⚙️ 配置说明

项目采用Nacos作为配置中心，所有配置项均支持动态刷新，无需重启服务即可生效。

### 配置分类

#### 服务配置

| 配置文件 | 说明 |
|---------|------|
| `zmbdp-mstemplate-service-dev.yaml` | 微服务模板配置 |
| `zmbdp-gateway-service-dev.yaml` | 网关服务配置 |

#### 公共组件配置

| 配置文件 | 说明 |
|---------|------|
| `share-redis-dev.yaml` | Redis公共配置（连接信息、序列化方式等） |
| `share-caffeine-dev.yaml` | 本地缓存公共配置（缓存大小、过期时间等） |
| `share-rabbitmq-dev.yaml` | RabbitMQ公共配置（连接信息、交换机等） |
| `share-mysql-dev.yaml` | MySQL公共配置（数据源配置等） |
| `share-map-dev.yaml` | 地图服务公共配置 |
| `share-token-dev.yaml` | Token公共配置（JWT密钥、过期时间等） |
| `share-captcha-dev.yaml` | 验证码服务公共配置（包含邮件验证码标题和内容模板配置） |
| `share-filter-dev.yaml` | 布隆过滤器公共配置（容量、误判率等） |
| `share-thread-dev.yaml` | 线程池公共配置（核心线程数、最大线程数等） |
| `share-mail-dev.yaml` | 邮件服务公共配置（SMTP配置等） |
| `share-idempotent-dev.yaml` | 幂等性公共配置（Token过期时间、强幂等模式开关、重试次数等） |

#### 业务服务配置

| 配置文件 | 说明 |
|---------|------|
| `zmbdp-file-service-dev.yaml` | 文件服务配置（OSS配置等） |
| `zmbdp-admin-service-dev.yaml` | 基础服务配置 |
| `zmbdp-portal-service-dev.yaml` | C端用户服务配置 |

> 💡 **提示**：所有配置均支持Nacos动态刷新，修改配置后无需重启服务即可生效。



## 📚 开发指南

本章节提供详细的开发指南和最佳实践，帮助您快速上手项目开发。

### 新增业务模块

创建新业务模块的步骤：

1. **选择模板**：基于`zmbdp-mstemplate`微服务模板创建新模块
2. **架构设计**：遵循Controller-Service-Mapper分层架构
3. **统一规范**：使用统一响应格式和异常处理
4. **网关集成**：将新模块集成到网关路由配置
5. **配置管理**：在Nacos中添加对应的配置文件

### 扩展缓存功能

使用三级缓存架构，有效防止缓存穿透：

```java 
// 使用 CacheUtil 工具类获取缓存
T result = CacheUtil.getL2Cache(
    redisService,           // Redis服务
    bloomFilterService,      // 布隆过滤器服务
    key,                    // 缓存键
    valueTypeRef,           // 值类型引用
    caffeineCache           // Caffeine本地缓存
);
```

### Excel导入导出

支持大数值处理、单元格合并、数据校验等功能：

```java
// 导出Excel
ExcelUtil.exportExcel(
    response,              // HttpServletResponse
    "用户列表",            // 文件名
    UserDTO.class,         // 数据类
    userList,             // 数据列表
    true                  // 是否合并相同值单元格
);

// 导入Excel
DefaultExcelListener<UserDTO> listener = new DefaultExcelListener<>(true);
EasyExcel.read(inputStream, UserDTO.class, listener).sheet().doRead();
ExcelResult<UserDTO> result = listener.getExcelResult();
// 处理导入结果
if (result.isSuccess()) {
    List<UserDTO> data = result.getData();
    // 业务处理...
}
```

### 邮件发送

支持文本、HTML、附件、内嵌图片等多种邮件格式：

```java
// 发送HTML邮件
MailUtil.sendHtml(
    "user@example.com",           // 收件人
    "邮件标题",                    // 主题
    "<h1>邮件内容</h1>"            // HTML内容
);

// 发送带附件的邮件
MailUtil.send(
    "user@example.com",            // 收件人
    "标题",                        // 主题
    "内容",                        // 正文
    false,                         // 是否为HTML格式
    new File("附件.pdf")           // 附件文件
);

// 发送带内嵌图片的邮件
MailUtil.sendHtmlWithImage(
    "user@example.com",
    "标题",
    "<img src='cid:image1'>",     // 使用cid引用图片
    new File("image.jpg")
);
```

### 幂等性控制

基于AOP实现的分布式幂等性解决方案，支持HTTP和MQ两种场景：

```java
// 1. HTTP接口幂等性（防重模式）- 适用于支付、下单等场景
@PostMapping("/createOrder")
@Idempotent(
    headerName = "Idempotent-Token",  // Token来源：请求头
    expireTime = 300,                 // 过期时间：300秒
    message = "请勿重复提交"           // 重复请求提示信息
)
public Result<String> createOrder() {
    // 第一次请求：执行方法
    // 第二次请求：返回错误"请勿重复提交"
    return Result.success("订单创建成功");
}

// 2. HTTP接口幂等性（强幂等模式）- 适用于查询等场景
@GetMapping("/getOrderInfo")
@Idempotent(
    headerName = "Idempotent-Token",
    returnCachedResult = IdempotentMode.TRUE  // 开启强幂等模式
)
public Result<OrderInfo> getOrderInfo() {
    // 第一次请求：执行方法并缓存结果
    // 第二次请求：返回第一次的结果，不执行方法
    return Result.success(orderInfo);
}

// 3. MQ消费者幂等性（SpEL表达式方式）
@RabbitListener(queues = "order.queue")
@Idempotent(tokenExpression = "#messageDTO.idempotentToken")
public void handleOrder(MessageDTO messageDTO) {
    // 第一次消费：处理消息
    // 第二次消费：拒绝处理（防重模式）或返回缓存结果（强幂等模式）
}

// 4. MQ消费者幂等性（消息头方式）
@RabbitListener(queues = "payment.queue")
@Idempotent(headerName = "Idempotent-Token")
public void handlePayment(MessageDTO messageDTO, Message message) {
    // 从消息头获取Token进行幂等性校验
}
```

**最佳实践**：
- Token建议使用UUID或业务唯一标识
- 根据业务场景选择合适的过期时间（支付场景建议30分钟，查询场景建议5分钟）
- 强幂等模式会缓存结果，注意Redis存储空间
- 业务失败时Token会自动删除，允许重试，最多重试3次（Nacos上可配置）

### 扩展功能开发

#### 1. 自定义业务模块扩展
- 基于`zmbdp-mstemplate`微服务模板快速创建新业务模块
- 支持独立部署或与现有服务集群部署
- 继承通用配置，也可自定义特定业务配置
- 集成统一认证授权体系，开箱即用

#### 2. 缓存策略扩展
- 支持自定义缓存过期策略
- 可扩展布隆过滤器哈希函数
- 支持多级缓存降级策略
- 可配置缓存预热和刷新机制

#### 3. 认证授权扩展
- 支持JWT Token无状态认证
- 支持微信登录、手机号验证码登录、邮箱验证码登录
- 验证码发送根据输入格式自动选择（手机号→短信，邮箱→邮件）
- 可自定义权限验证规则
- 可扩展第三方登录集成（微信、手机号、邮箱等）

#### 4. 消息队列扩展
- 支持多种消息模式（点对点、发布订阅）
- 可扩展消息序列化方式
- 支持死信队列和延迟消息
- 可配置消息确认和重试机制

#### 5. 线程池扩展
- 支持按业务模块自定义线程池
- 可扩展拒绝策略
- 支持线程池监控和调优
- 可配置动态线程池参数调整

#### 6. 异常处理扩展
- 支持自定义业务异常类型
- 可扩展全局异常处理逻辑
- 支持异常信息脱敏处理
- 可集成第三方监控告警系统

#### 7. 工具类扩展
- 可扩展BeanCopyUtil支持更多复杂对象拷贝
- 支持自定义JSON序列化/反序列化规则
- 可扩展安全工具类支持更多加密算法
- 支持自定义验证规则

#### 8. 幂等性控制扩展
- 支持自定义Token生成策略
- 可扩展Token获取方式（如从请求体、Cookie等获取）
- 支持自定义重试策略和重试次数限制
- 可集成第三方存储（如数据库）存储幂等性状态
- 支持幂等性状态监控和告警


## ❓ 常见问题

### Q1: 如何配置幂等性Token的过期时间？

**A**: 可以通过以下方式配置：
1. **注解配置**：在`@Idempotent`注解中设置`expireTime`参数
2. **全局配置**：在Nacos配置中心的`share-idempotent-dev.yaml`中配置`idempotent.expire-time`
3. **优先级**：注解配置 > 全局配置 > 默认值（300秒）

### Q2: 防重模式和强幂等模式有什么区别？

**A**: 
- **防重模式**：重复请求直接返回错误，适用于支付、下单等不允许重复执行的场景
- **强幂等模式**：重复请求返回第一次执行的结果，适用于需要保证多次调用返回相同结果的场景

### Q3: 业务执行失败后，Token会被删除吗？

**A**: 是的。当方法执行失败（抛出异常）时，Token会被自动删除，状态设置为`FAILED`，允许重试。最多支持重试3次（Nacos上可配置），重试次数会持久化到Redis中。

### Q4: 重试次数如何查询和追踪？

**A**: 重试次数存储在Redis中，Key格式为：`{keyPrefix}{token}:retry:count`。可以通过Redis查询当前重试次数。重试次数会在以下情况被清除：
- 方法执行成功时
- 超过最大重试次数时
- 检测到SUCCESS状态时

### Q5: 如何自定义Token获取方式？

**A**: 支持多种Token获取方式，优先级如下：
1. SpEL表达式（`tokenExpression`）
2. HTTP请求头（`headerName`）
3. HTTP请求参数（`paramName`，需`allowParam = true`）
4. RabbitMQ消息头（`headerName`）

### Q6: 三级缓存架构如何工作？

**A**: 请求流程：布隆过滤器（判断是否存在） → Caffeine本地缓存 → Redis分布式缓存 → 数据库。这种架构可以有效防止缓存穿透，提供最快的访问速度，同时保证数据一致性。

### Q7: 如何扩展新的业务模块？

**A**: 
1. 基于`zmbdp-mstemplate`微服务模板创建新模块
2. 遵循Controller-Service-Mapper分层架构
3. 使用统一响应格式和异常处理
4. 集成到网关路由配置

### Q8: 验证码发送如何自动选择短信或邮件？

**A**: 系统会根据用户输入的格式自动判断：
- 手机号格式（11位数字）→ 发送短信验证码
- 邮箱格式（包含@符号）→ 发送邮件验证码
- 采用策略模式设计，便于扩展新的发送方式

## 🚀 性能优化

### 数据库优化
- 连接池配置优化（HikariCP）
- 索引优化和查询优化
- 分页查询优化

### 缓存优化
- 多级缓存减少数据库访问
- 缓存预热和刷新机制
- 缓存穿透、击穿、雪崩防护

### 并发优化
- Redisson分布式锁
- 线程池参数调优
- 异步任务处理

### JVM优化
- 合理的堆内存配置
- GC参数调优
- 内存泄漏监控

## 🤝 贡献指南

我们欢迎所有形式的贡献，包括但不限于：

- 🐛 **Bug修复**：发现并修复项目中的问题
- ✨ **新功能**：添加新的功能特性
- 📝 **文档改进**：完善项目文档和示例
- 🎨 **代码优化**：改进代码质量和性能
- 🧪 **测试补充**：增加测试用例覆盖

### 贡献流程

1. **Fork项目**：点击右上角的Fork按钮
2. **创建分支**：从`master`分支创建功能分支
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. **提交更改**：编写代码并提交
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
4. **推送分支**：推送到你的Fork仓库
   ```bash
   git push origin feature/AmazingFeature
   ```
5. **开启PR**：在GitHub上创建Pull Request

### 代码规范

- 遵循阿里巴巴Java开发手册规范
- 代码注释完整，特别是公共方法
- 提交信息清晰明确
- 新增功能需补充对应的测试用例

### 问题反馈

如果发现Bug或有功能建议，请通过以下方式反馈：
- 📧 发送邮件至：JavaFH@163.com
- 🐛 提交Issue：[GitHub Issues](https://github.com/zmbdp/frameworkjava/issues)

## 📄 许可证

本项目采用 [MIT](LICENSE) 许可证，您可以自由使用、修改和分发。

## 📮 联系方式

- 👤 **作者**: 稚名不带撇
- 📧 **邮箱**: JavaFH@163.com
- 🐙 **GitHub**: [https://github.com/zmbdp](https://github.com/zmbdp)
- 📚 **项目地址**: [https://github.com/zmbdp/frameworkjava](https://github.com/zmbdp/frameworkjava)

## 🙏 鸣谢

感谢以下优秀的开源项目和技术社区：

- [Spring Boot](https://spring.io/projects/spring-boot) - 核心框架
- [Spring Cloud](https://spring.io/projects/spring-cloud) - 微服务框架
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba) - 阿里微服务组件
- [MyBatis-Plus](https://github.com/baomidou/mybatis-plus) - ORM框架
- [EasyExcel](https://github.com/alibaba/easyexcel) - Excel处理
- [Hutool](https://github.com/dromara/hutool) - Java工具类库
- 以及所有为开源社区做出贡献的开发者们

---

<div align="center">

**如果这个项目对您有帮助，请给一个 ⭐ Star 支持一下！**

Made with ❤️ by [稚名不带撇](https://github.com/zmbdp)

</div>
