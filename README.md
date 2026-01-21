<div align="center">

# FrameworkJava  
### 企业级 Spring Boot 微服务脚手架

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.2-blueviolet.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/github/license/zmbdp/frameworkjava)](LICENSE)
[![Stars](https://img.shields.io/github/stars/zmbdp/frameworkjava)](https://github.com/zmbdp/frameworkjava/stargazers)

**一个开箱即用的企业级微服务脚手架，  
用于快速构建高可用、高性能、可扩展的 Java 微服务系统**

[⚡ 快速开始](#-快速开始) · [✨ 核心特性](#-核心特性) · [📚 文档中心](docs/README.md)

</div>

---

## 📑 目录

- [📖 项目简介](#-项目简介)
- [✨ 核心特性](#-核心特性)
- [🧭 项目结构概览](#-项目结构概览)
- [⚡ 快速开始](#-快速开始)
- [📚 文档中心](#-文档中心)
- [🤝 参与贡献](#-参与贡献)
- [📄 License](#-license)

---

## 📖 项目简介

FrameworkJava 是一个基于 **Spring Boot 3.x + Spring Cloud 2023** 的企业级微服务脚手架，  
目标不是"演示功能"，而是**沉淀一套可直接用于真实项目的工程实践**。

它关注的是：

- 微服务工程结构是否清晰
- 高并发与一致性问题如何解决
- 公共能力如何模块化复用
- 新业务是否能快速、低成本接入

---

## ✨ 核心特性

- **统一认证与鉴权**  
  JWT 无状态认证，网关统一校验，支持 B 端 / C 端用户体系

- **三级缓存体系**  
  布隆过滤器 + Caffeine 本地缓存 + Redis 分布式缓存，有效防止缓存穿透

- **分布式幂等性控制**  
  基于 AOP 的幂等性方案，支持 HTTP / MQ 场景，针对高并发做了专项优化

- **模块化微服务结构**  
  API 与 Service 分离，公共能力下沉，业务服务解耦

- **开箱即用的基础能力**  
  用户、配置、文件、消息、定时任务、Excel、邮件等常见能力已预置

---

## 🧭 项目结构概览

```
frameworkjava
├── zmbdp-gateway        # 网关服务
├── zmbdp-common         # 公共基础模块
├── zmbdp-admin          # 管理服务
├── zmbdp-portal         # 门户服务
├── zmbdp-file           # 文件服务
└── zmbdp-mstemplate     # 微服务模板（模板示例，用于开发风格参考）
```

> 📁 **完整结构说明**：见 [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)

---

## ⚡ 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- Docker & Docker Compose

### 启动基础中间件
```bash
git clone https://github.com/zmbdp/frameworkjava.git
cd frameworkjava

cd deploy/dev/app
docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
```

* Nacos：[http://localhost:8848/nacos](http://localhost:8848/nacos)
* RabbitMQ：[http://localhost:15672](http://localhost:15672)

### 启动服务

```bash
mvn clean install -DskipTests

# 网关
cd zmbdp-gateway
mvn spring-boot:run

# 管理服务
cd ../zmbdp-admin/zmbdp-admin-service
mvn spring-boot:run
```

---

## 📚 文档中心

* [📁 项目结构说明](docs/PROJECT_STRUCTURE.md)
* [⚙️ 配置中心与环境配置](docs/CONFIGURATION.md)
* [🛡️ 三级缓存架构](docs/CACHE_ARCHITECTURE.md)
* [🔐 分布式幂等性设计](docs/IDEMPOTENT.md)
* [🚀 新增业务模块指南](docs/ADD_NEW_MODULE.md)
* [❓ 常见问题](docs/FAQ.md)

---

## 🤝 参与贡献

欢迎任何形式的贡献，包括但不限于：

* 提交 Issue
* 改进文档
* 提交 Pull Request
* 分享使用经验

---

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。

---

<div align="center">

如果这个项目对你有帮助，请给一个 ⭐ Star
**Made with ❤️ by 稚名不带撇**

</div>
