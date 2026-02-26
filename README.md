<div align="center">

# FrameworkJava

### 企业级 Spring Boot 微服务工程脚手架

（工程基线与通用能力集合，而非通用后台系统）

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.2-blueviolet.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/github/license/zmbdp/frameworkjava)](LICENSE)
[![Stars](https://img.shields.io/github/stars/zmbdp/frameworkjava)](https://github.com/zmbdp/frameworkjava/stargazers)

**这是一个在真实项目中反复使用和调整过的 Spring Boot 微服务工程脚手架，  
主要用于解决新项目启动阶段工程结构混乱、基础能力重复建设的问题。**

这个仓库里已经准备好了我在项目中最常用的一些工程基础，包括：  
一套相对稳定的微服务目录结构和模块拆分方式  
常见基础能力的工程化实现（缓存、幂等、认证等）  
新建业务服务时可以直接复用的模板和约定

[⚡ 快速开始](#-快速开始) · [✨ 核心特性](#-核心特性) · [📚 文档中心](docs/README.md)

</div>

---

## 📖 项目简介

**FrameworkJava** 是我在多个项目中反复使用的一套基于 **Spring Boot 3.x + Spring Cloud 2023** 的 **微服务工程脚手架（Scaffold）**。

这个项目一开始只是我自己用来起新服务的一套工程骨架。

后来项目多了，每个项目里都会重复用到一些东西，
比如网关、认证、缓存、幂等、日志、链路追踪、服务监控等，
就干脆把这些部分慢慢拆出来，整理成现在这个结构。


> ⚠️ **适合人群**
> - ✅ 具备 Spring Boot / Spring Cloud 基础的开发者
> - ✅ 希望快速落地微服务工程架构的团队
> - ✅ 需要统一工程规范与公共能力沉淀的项目
>
> ⚠️ **不适合**
> - ❌ 完全零基础的 Java 初学者
> - ❌ 仅需简单单体应用的项目

---

## 🧱 能力边界说明（请务必阅读）

FrameworkJava 本身并不是一个可以直接上线使用的后台系统，这一点在一开始用的时候就刻意控制住了。

它更多是用来当一个工程起点，哪些业务要做、做到什么程度，还是应该由具体项目自己决定。

### 本项目提供的能力

- ✅ 微服务工程结构与模块拆分规范
- ✅ 通用基础能力组件（缓存、幂等、认证、消息、文件抽象等）
- ✅ 新业务服务的标准模板与参考实现
- ✅ 可直接复用的工程实践与设计模式

### 本项目**不强制提供**的内容

- ❌ 统一的用户 / 组织 / 菜单 / 权限业务模型
- ❌ 固定的后台功能集合
- ❌ 面向所有公司的通用业务表结构

实际用下来，不同项目之间差异最大的往往就是这两块：

- 组织结构怎么划
- 权限到底要细到什么程度

这些东西如果在脚手架阶段就定死，后续基本都会被推翻重来。

## 🎯 设计理念

- **工程化优先**：关注项目结构、模块边界和长期可维护性，而不是单纯堆砌功能
- **能力下沉**：公共能力尽量集中到基础模块，业务服务保持轻量
- **模块解耦**：服务间通过清晰接口协作，减少隐式依赖
- **可裁剪性**：示例业务模块仅作参考，可根据需要删除或替换

## ✨ 核心特性

### 🚀 一键式环境部署

Docker Compose 一键部署所有中间件（MySQL、Redis、Nacos、RabbitMQ、SkyWalking、Prometheus、Grafana），支持 dev/test/prd 多环境配置，
**5 分钟快速搭建完整开发环境**，告别繁琐的环境配置

### 🔐 统一认证与鉴权能力

JWT 无状态认证，网关统一校验，支持 B 端 / C 端用户体系，**开箱即用的安全方案**

### ⚡ 三级缓存体系

布隆过滤器 + Caffeine 本地缓存 + Redis 分布式缓存，**有效防止缓存穿透，性能提升 10 倍+**  
*（支持 Redis/Fast/Safe 三种布隆过滤器实现，本地缓存支持自定义过期策略，非简单封装）*

### 🛡️ 分布式幂等性控制

基于 AOP + Redis 的幂等性方案，支持 HTTP / MQ 场景，**针对高并发做了专项优化**，保障数据一致性  
*（支持防重模式/强幂等模式，Redis + Lua 脚本保证原子性，支持并发穿透控制）*

### 🔒 数据权限控制

基于 MyBatis 拦截器的数据权限方案，支持本人/本部门/本部门及子部门/全部/自定义五种权限类型，**自动过滤 SQL，零侵入实现数据隔离**  
*（支持多租户隔离、自定义字段、表别名，适用于 SaaS 系统和企业内部系统）*

### 🔍 全链路追踪（SkyWalking）

**无侵入式链路追踪**，自动追踪微服务间的调用链路，支持性能分析、慢查询定位、服务拓扑图、日志关联 TraceId  
*（基于 Java Agent，零代码侵入，支持 HTTP、RPC、数据库、缓存、消息队列全链路追踪）*

### 📊 全方位服务监控（Prometheus + Grafana）

**实时监控 JVM、接口、数据库、缓存、系统资源**，Grafana 可视化大盘，多级别智能告警（邮件/钉钉/短信）  
*（30+ 监控指标，10+ 告警规则，开箱即用的监控大盘，支持自定义扩展）*

### 📦 模块化微服务结构

API / Service 分离，公共能力集中到基础模块，  
新业务模块接入时只需关注自身领域逻辑。

### 🛠️ 丰富的工具包生态

FrameworkJava 集成了 **20 多个常用工具类**，涵盖复杂泛型BeanCopy、加密、JSON、Excel、邮件、分页、流处理等功能。  
同时提供 **三套策略模式实现**，支持登录流程、验证码发送、账号校验等场景。

基础业务能力也已工程化实现，包括用户管理、配置管理、文件处理、消息处理等模块。  
这些工具和能力都是从真实项目中沉淀出来的，开箱即用，  
能够在新业务开发中直接复用，避免重复造轮子，帮助团队快速启动并保持工程规范。

## 🧭 项目结构概览

```
frameworkjava
├── zmbdp-gateway        # 网关服务（工程必选）
├── zmbdp-common         # 公共基础能力模块（工程必选）
├── zmbdp-admin          # 业务示例：管理端参考实现
├── zmbdp-portal         # 业务示例：门户参考实现
├── zmbdp-file           # 业务示例：文件服务参考实现
└── zmbdp-mstemplate     # 微服务模板（规范示例）
```


> **说明**  
> admin / portal / file 等模块主要用于演示：
> - 脚手架在实际业务中的落地方式
> - 业务分层、接口设计、领域拆分的参考实现
>
> 它们不是脚手架的必选部分，可以根据实际项目自由裁剪或替换。

完整结构说明：见 [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)

## ⚡ 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Docker & Docker Compose

### 三步启动

#### 1. 克隆项目
```bash
git clone https://github.com/zmbdp/frameworkjava.git
cd frameworkjava
```

#### 2. 启动中间件（一键部署）

```bash
cd deploy/dev/app
docker compose -p frameworkjava -f docker-compose-mid.yml up -d
```

**等待 30-60 秒后，访问以下地址验证：**

- Nacos 控制台：[http://localhost:8848/nacos](http://localhost:8848/nacos)
- RabbitMQ 管理界面：[http://localhost:15672](http://localhost:15672)
- SkyWalking UI 界面：[http://localhost:8080](http://localhost:8080)
- Prometheus 界面：[http://localhost:9090](http://localhost:9090)
- Grafana 界面：[http://localhost:3000](http://localhost:3000)
- AlertManager 界面：[http://localhost:9093](http://localhost:9093)

#### 3. 启动服务

```bash
# 编译项目
mvn clean install -DskipTests

# 启动网关服务
cd zmbdp-gateway
mvn spring-boot:run

# 启动管理服务（新开终端）
cd zmbdp-admin/zmbdp-admin-service
mvn spring-boot:run
```

> **提示**：首次启动会自动初始化数据库和配置，请耐心等待。详细配置说明见 [配置中心文档](docs/CONFIGURATION.md)

### 🎉 启动成功后你可以做什么？

- **统一认证**：通过网关访问登录接口，体验 JWT 无状态认证
- **配置管理**：在 Nacos 控制台查看完整的配置结构和管理方式
- **缓存体验**：测试三级缓存（布隆过滤器 + Caffeine + Redis）的命中与降级效果
- **链路追踪**：访问 [SkyWalking UI](http://localhost:8080)，查看服务调用链路和性能分析
- **服务监控**：访问 [Grafana](http://localhost:3000)，查看 JVM、接口、系统资源监控大盘
- **告警测试**：访问 [Prometheus](http://localhost:9090)，查看告警规则和触发状态
- **快速开发**：使用 `zmbdp-mstemplate` 模块作为模板，10 分钟快速新建一个业务微服务
- **能力验证**：体验幂等性控制、限流防刷、消息发送等核心能力

---

## 📚 文档中心

### 📖 使用手册

完整的使用手册请访问：[📘 使用手册](https://gcnrxp4nkh9d.feishu.cn/docx/GVUPdzmLJoWhMNxygsQc1F3Enyd?from=from_copylink)

### 📋 技术文档

| 文档                                   | 说明                            |
|--------------------------------------|-------------------------------|
| [项目结构说明](docs/PROJECT_STRUCTURE.md)  | 详细的模块划分和职责说明                  |
| [工具类使用指南](docs/UTILS.md)             | 20 多个工具类完整说明与使用示例             |
| [配置中心与环境配置](docs/CONFIGURATION.md)   | Nacos 配置、多环境切换指南              |
| [三级缓存架构](docs/CACHE_ARCHITECTURE.md) | 布隆过滤器 + Caffeine + Redis 缓存设计 |
| [分布式幂等性设计](docs/IDEMPOTENT.md)       | 幂等性控制原理与使用指南                  |
| [数据权限控制](docs/DATAPERMISSION.md)      | 数据权限控制原理与使用指南                 |
| [频控 / 防刷](docs/RATELIMIT.md)         | 限流组件说明与使用指南                   |
| [操作日志](docs/LOG.md)                  | 操作日志组件说明与使用指南                 |
| [链路追踪](docs/TRACING.md)              | SkyWalking 链路追踪使用指南           |
| [服务监控与告警](docs/MONITORING.md)        | Prometheus + Grafana 监控告警指南   |
| [新增业务模块指南](docs/ADD_NEW_MODULE.md)   | 快速创建新微服务模块                    |
| [性能与并发设计](docs/PERFORMANCE.md)       | 性能优化策略与并发设计                   |
| [常见问题](docs/FAQ.md)                  | 开发中常见问题解答                     |

---

## 📦 SDK 开发文档

SDK 开发文档位于项目 `javapro/javadoc` 目录下，基于 JavaDoc 自动生成。

**访问方式**：在项目根目录找到 `javapro/javadoc/index.html`，使用浏览器打开即可浏览完整 API 文档。

---

## 🤝 参与贡献

欢迎任何形式的贡献，让这个项目变得更好。  
无论是功能改进，还是非常细微的优化，都非常欢迎：

- **提交 Issue**：Bug、异常行为、设计建议等
- **文档与注释**：错别字、标点符号、说明补充、示例完善
- **代码优化**：性能改进、结构调整、最佳实践
- **新增能力**：通用组件、公共能力、工程规范
- **Star 支持**：如果这个项目对你有帮助，欢迎 Star 鼓励

### 贡献规范

1. **分支规范**：从 `main` 分支创建 `feature/xxx` 或 `fix/xxx` 分支
2. **提交规范**：使用清晰的提交信息，格式：`type: description`（如：`feat: 新增限流组件`）
3. **代码规范**：遵循现有项目代码风格，参考 `zmbdp-mstemplate` 模块
4. **文档完善**：如有新增功能，请同步更新相关文档

> 即使只是一个标点符号的修改，也同样值得一次 Pull Request。

---

## 🎯 适用场景

- 快速搭建企业级微服务项目
- 学习 Spring Cloud 微服务架构
- 作为项目脚手架，快速启动新业务
- 参考工程化实践和最佳实践

---

## 📮 联系方式

- 👤 **作者**: 稚名不带撇
- 📧 **邮箱**: [JavaFH@163.com](mailto:JavaFH@163.com)
- 🐙 **GitHub**: [@zmbdp](https://github.com/zmbdp)
- 📚 **项目地址**: [https://github.com/zmbdp/frameworkjava](https://github.com/zmbdp/frameworkjava)

---

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。

---

## 📸 架构与界面预览

<table>
  <tr>
    <td align="center">
      <b>功能概述</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/%E5%8A%9F%E8%83%BD%E6%A6%82%E8%BF%B0.PNG" style="max-width: 100%; height: auto;">
    </td>
    <td align="center">
      <b>系统架构</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/%E7%B3%BB%E7%BB%9F%E6%9E%B6%E6%9E%84%E9%A1%B5%E9%9D%A2.png" style="max-width: 100%; height: auto;">
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>使用手册</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/%E4%BD%BF%E7%94%A8%E6%89%8B%E5%86%8C%E6%88%AA%E5%9B%BE.png" style="max-width: 100%; height: auto;">
    </td>
    <td align="center">
      <b>API文档</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/API%E6%96%87%E6%A1%A3%E6%88%AA%E5%9B%BE.png" style="max-width: 100%; height: auto;">
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>SDK文档</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/SDK%E6%88%AA%E5%9B%BE.png" style="max-width: 100%; height: auto;">
    </td>
    <td align="center">
      <b>管理端登录页</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/%E7%99%BB%E5%BD%95%E9%A1%B5.png" style="max-width: 100%; height: auto;">
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>管理端管理页</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86%E9%A1%B5.png" style="max-width: 100%; height: auto;">
    </td>
    <td align="center">
      <b>字典管理页</b><br>
      <img src="https://framework-java-web.oss-cn-shanghai.aliyuncs.com/FrameworkJava/%E5%AD%97%E5%85%B8%E8%AE%BE%E7%BD%AE%E9%A1%B5.png" style="max-width: 100%; height: auto;">
    </td>
  </tr>
</table>

<div align="center">

如果这个项目对你有帮助，请给一个 ⭐ Star  
**FrameworkJava只负责打基础，真正的业务复杂度，还是应该由你自己的项目来承载。**

</div>