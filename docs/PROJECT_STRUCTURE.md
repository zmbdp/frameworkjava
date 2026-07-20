# 项目结构与模块职责

## 整体架构

FrameworkJava 采用模块化微服务架构，遵循 **API 与 Service 分离**的设计原则。

```
frameworkjava/
├── zmbdp-gateway/                     # 网关服务
├── zmbdp-common/                      # 公共基础模块
│   ├── zmbdp-common-cache/            # 缓存组件
│   ├── zmbdp-common-core/             # 核心工具类
│   ├── zmbdp-common-datapermission/   # 数据权限组件
│   ├── zmbdp-common-domain/           # 领域模型
│   ├── zmbdp-common-excel/            # Excel 处理
│   ├── zmbdp-common-filter/           # 过滤器（布隆过滤器）
│   ├── zmbdp-common-idempotent/       # 幂等性组件
│   ├── zmbdp-common-log/              # 操作日志组件
│   ├── zmbdp-common-message/          # 消息服务
│   ├── zmbdp-common-monitor/          # 监控组件
│   ├── zmbdp-common-rabbitmq/         # 消息队列
│   ├── zmbdp-common-ratelimit/        # 限流组件
│   ├── zmbdp-common-redis/            # Redis 封装
│   ├── zmbdp-common-security/         # 安全认证
│   ├── zmbdp-common-snowflake/        # 雪花算法
│   └── zmbdp-common-xxljob/           # XXL-JOB 执行器自动配置
├── zmbdp-admin/                       # 基础服务
│   ├── zmbdp-admin-api/               # API 接口定义
│   └── zmbdp-admin-service/           # 服务实现
├── zmbdp-portal/                      # C 端服务
│   ├── zmbdp-portal-api/              # API 接口定义
│   └── zmbdp-portal-service/          # 服务实现
├── zmbdp-file/                        # 文件服务
│   ├── zmbdp-file-api/                # API 接口定义
│   └── zmbdp-file-service/            # 服务实现
└── zmbdp-mstemplate/                  # 微服务模板
```

## 模块职责说明

### zmbdp-gateway
**职责**：统一网关入口
- 路由转发
- 统一认证鉴权
- 限流控制
- 异常处理

### zmbdp-common
**职责**：公共能力下沉
- **cache**：三级缓存实现（布隆过滤器 + Caffeine + Redis）
- **core**：基础工具类（20+ 个）、常量、异常定义
- **datapermission**：数据权限控制（基于 MyBatis 拦截器）
- **domain**：领域模型、实体基类
- **excel**：Excel 处理（导入导出、单元格合并）
- **filter**：布隆过滤器（Redis/Fast/Safe 三种实现）
- **idempotent**：分布式幂等性控制
- **log**：操作日志组件（支持多种存储方式）
- **message**：消息服务能力（短信、邮件验证码）
- **monitor**：监控组件（Prometheus 指标采集）
- **rabbitmq**：消息队列封装
- **ratelimit**：限流组件（令牌桶、滑动窗口）
- **redis**：Redis 操作封装、Redisson 分布式锁
- **security**：JWT 认证、权限校验
- **snowflake**：分布式 ID 生成
- **xxljob**：XXL-JOB 执行器自动配置，各服务引入依赖后即可注册执行器，无需重复编写初始化代码

### zmbdp-admin
**职责**：基础服务
- **用户管理**：B端系统用户管理（登录、增删改查）、C端应用用户管理（注册、查询、编辑）
- **配置管理**：系统参数配置、数据字典管理（字典类型、字典数据）
- **地图服务**：城市列表、区域查询、POI搜索、定位等
- **定时任务**：基于 XXL-JOB 的系统级定时任务（如布隆过滤器每日重置），同时提供 Spring Scheduled 降级兜底方案

### zmbdp-portal
**职责**：C 端门户服务
- 用户注册登录
- 用户信息管理

### zmbdp-file
**职责**：文件服务
- 文件上传
- 文件下载
- 文件管理

### zmbdp-mstemplate
**职责**：微服务模板
- 新业务模块的参考模板
- 包含完整的 API + Service 结构

## 设计原则

1. **API 与 Service 分离**
   - API 模块：定义接口、DTO、常量
   - Service 模块：实现业务逻辑、依赖实现

2. **公共能力下沉**
   - 所有通用能力放在 `zmbdp-common` 下
   - 业务服务只关注业务逻辑

3. **模块化设计**
   - 每个业务服务独立部署
   - 通过 API 模块实现服务间调用

## 依赖关系

```
业务服务 (admin/portal/file)
    ↓
公共模块 (common-*)
    ↓
Spring Boot / Spring Cloud
```

**原则**：业务服务只能依赖 common 模块，不能相互依赖。