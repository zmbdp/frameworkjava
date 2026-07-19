# 部署指南

frameworkJava 提供完整的三环境（dev/test/prd）部署方案，基于 Docker Compose 编排中间件与服务，dev/test 单机部署、prd 采用双 VM 主从架构，覆盖 MySQL、Redis、Nacos、RabbitMQ、SkyWalking、Prometheus、Grafana、Alertmanager、Nginx 等全套基础设施。

## 一、概述

### 1.1 部署目录结构

```
deploy/
├── docker-install/                  # Docker 一键安装脚本
│   ├── install_docker.sh
│   └── docker-mirrors.conf
├── skywalking/                      # SkyWalking OAP 镜像构建与 Agent
│   ├── Dockerfile
│   ├── download-agent.sh
│   ├── download-agent.ps1
│   └── skywalking-agent/            # Agent 文件目录（运行时挂载）
├── dev/                             # 开发环境
│   ├── app/                         # 中间件 docker-compose 配置
│   │   ├── docker-compose-mid.yml
│   │   ├── mysql/redis/nacos/...
│   │   └── grafana/prometheus/alertmanager/...
│   └── res/sql/DEFAULT_GROUP/       # Nacos 配置文件
├── test/                            # 测试环境
│   ├── app/docker-compose-mid.yml
│   └── res/sql/nacos_config/DEFAULT_GROUP/
└── prd/                             # 生产环境（双 VM）
    ├── vm1/                         # 主节点：应用 + 中间件主节点
    │   ├── app/
    │   │   ├── docker-compose-mid.yml
    │   │   ├── docker-compose-app.yml
    │   │   └── service/{admin,file,gateway,portal}/Dockerfile
    │   └── res/sql/DEFAULT_GROUP/
    └── vm2/                         # 从节点：中间件从节点 + 应用
        └── app/
            ├── docker-compose-mid.yml
            ├── docker-compose-app.yml
            └── service/{admin,file,gateway,portal}/Dockerfile
```

### 1.2 统一命令格式

所有环境统一使用 `frameworkJava` 作为 docker compose 项目名，容器名前缀为 `frameworkJava-`：

```bash
# 启动中间件
docker compose -p frameworkJava -f docker-compose-mid.yml up -d

# 启动应用服务（仅 prd 环境）
docker compose -p frameworkJava -f docker-compose-app.yml up -d

# 查看容器状态
docker compose -p frameworkJava -f docker-compose-mid.yml ps

# 停止并移除容器
docker compose -p frameworkJava -f docker-compose-mid.yml down
```

### 1.3 网络

每个环境使用独立的 Docker 网络，实现环境间隔离：

| 环境 | 网络名 |
|------|--------|
| dev  | `frameworkJava_network_dev` |
| test | `frameworkJava_network_test` |
| prd  | `frameworkJava_network_prd` |

## 二、三环境结构对照表

| 维度 | dev（开发） | test（测试） | prd（生产） |
|------|------------|-------------|------------|
| 部署目录 | `deploy/dev/app/` | `deploy/test/app/` | `deploy/prd/vm1/app/` + `deploy/prd/vm2/app/` |
| 机器规模 | 单机 | 单机 | 双 VM（vm1 + vm2） |
| compose 文件 | `docker-compose-mid.yml` | `docker-compose-mid.yml` | `docker-compose-mid.yml` + `docker-compose-app.yml`（每台 VM） |
| MySQL | 单节点（server-id=1） | 单节点（server-id=1） | 主从（vm1 master server-id=1，vm2 slave server-id=2） |
| Redis | 单节点 6379 | 单节点 6379 | 集群 6380-6385（3 主 3 从，跨机部署） |
| Nacos | standalone 模式 | standalone 模式 | cluster 模式（vm1 部署 2 节点，vm2 部署 1 节点） |
| RabbitMQ | 单节点 | 单节点 | 集群（vm1 rabbitmq01 + vm2 rabbitmq02，共享 Erlang Cookie） |
| 应用服务 | 通过 docker-maven-plugin 本地运行 | 通过 docker-maven-plugin 本地运行 | docker-compose 构建并部署到双 VM |
| 数据卷目录 | `../data/frameworkJavadata/` | `../data/frameworkJavadata/` | vm1：`../data/frameworkJavadata/`；vm2：`../data2/frameworkJavadata/` |
| Nacos 配置路径 | `deploy/dev/res/sql/DEFAULT_GROUP/` | `deploy/test/res/sql/nacos_config/DEFAULT_GROUP/` | `deploy/prd/vm1/res/sql/DEFAULT_GROUP/` |
| 数据库账号 | `zmbdpdev` | `zmbdptest` | `zmbdpprd` |

## 三、prd 双 VM 架构

生产环境采用双 VM 主从架构，将中间件主从节点和应用服务分散到两台机器，提升可用性与负载能力。

```
┌────────────────────────── VM1（主节点）──────────────────────────┐
│  中间件主节点：                                                   │
│  ├── MySQL Master（server-id=1，端口 3308）                       │
│  ├── Redis 01/02/03（端口 6380-6382，集群主节点）                  │
│  ├── Nacos 01/02（cluster 模式，端口 8854/8856）                  │
│  ├── RabbitMQ 01（集群节点，端口 5673/15673）                     │
│  ├── SkyWalking OAP + UI                                         │
│  ├── Prometheus + Grafana + Alertmanager                         │
│  ├── XXL-JOB Admin                                               │
│  ├── Milvus + etcd + minio                                       │
│  └── Web 前端 Nginx（端口 8666）                                  │
│  应用服务：                                                       │
│  └── admin / file / gateway / portal                             │
└──────────────────────────────────────────────────────────────────┘
┌────────────────────────── VM2（从节点）──────────────────────────┐
│  中间件从节点：                                                   │
│  ├── MySQL Slave（server-id=2，端口 3308）                        │
│  ├── Redis 04/05/06（端口 6383-6385，集群从节点）                  │
│  ├── Nacos 03（cluster 模式，端口 8858）                          │
│  ├── RabbitMQ 02（集群节点，端口 5673/15673）                     │
│  └── Webnacos Nginx（端口 8866，Nacos 负载均衡代理）              │
│  应用服务：                                                       │
│  └── admin / file / gateway / portal                             │
└──────────────────────────────────────────────────────────────────┘
```

**VM1 启动顺序：**

```bash
cd deploy/prd/vm1/app
# 1. 启动中间件
docker compose -p frameworkJava -f docker-compose-mid.yml up -d
# 2. 启动应用服务
docker compose -p frameworkJava -f docker-compose-app.yml up -d
```

**VM2 启动顺序：**

```bash
cd deploy/prd/vm2/app
# 1. 启动中间件
docker compose -p frameworkJava -f docker-compose-mid.yml up -d
# 2. 启动应用服务
docker compose -p frameworkJava -f docker-compose-app.yml up -d
```

> **说明**：VM2 的 MySQL 为 slave 节点，需在 VM1 的 MySQL Master 启动并配置好主从复制后再启动；Redis 集群需在 6 个节点全部启动后执行 `redis-cli --cluster create` 完成集群搭建；Nacos 集群需 3 个节点全部启动后才能对外服务。

## 四、中间件部署清单

| 中间件 | 镜像版本 | dev 端口 | test 端口 | prd 端口（vm1 / vm2） | 用途 |
|--------|---------|----------|----------|----------------------|------|
| MySQL | mysql:8.4.2 | 3306 | 3306 | 3308 / 3308 | 业务数据库 + Nacos + XXL-JOB + SkyWalking 元数据存储 |
| Redis | rebloom:latest | 6379 | 6379 | 6380-6382 / 6383-6385 | 缓存（带布隆过滤器） |
| Nacos | nacos-server:v2.2.2 | 8848 | 8848 | 8854、8856 / 8858 | 配置中心 + 服务注册发现 |
| RabbitMQ | rabbitmq:3.12.6-management | 5672、15672 | 5672、15672 | 5673、15673 / 5673、15673 | 消息队列 |
| Prometheus | prometheus:v2.48.0 | 9090 | 9090 | 9090 / - | 指标采集 |
| Grafana | grafana:10.2.2 | 3000 | 3000 | 3000 / - | 监控可视化 |
| Alertmanager | alertmanager:v0.26.0 | 9093 | 9093 | 9093 / - | 告警通知 |
| Nginx | nginx:1.24.0 | - | 80 | 8666 / 8866（Nacos 代理） | 前端部署 / Nacos 负载均衡 |
| SkyWalking OAP | skywalking-oap:9.7.0-mysql | 11800、12800 | 11800、12800 | 11800、12800 / - | 链路追踪后端 |
| SkyWalking UI | skywalking-ui:9.7.0 | 8080 | 8080 | 8080 / - | 链路追踪界面 |
| XXL-JOB Admin | xxl-job-admin:2.4.2 | 8081 | 8081 | 8081 / - | 定时任务调度中心 |
| Milvus | milvus:v2.4.5 | 19530、9091 | 19530、9091 | 19530、9091 / - | 向量数据库 |
| etcd | etcd:v3.5.5 | 2379 | 2379 | 2379 / - | Milvus 元数据存储 |
| minio | minio:RELEASE.2023-03-20T20-16-18Z | 9000 | 9000 | 9000 / - | Milvus 对象存储 |

**MySQL 启动参数（所有环境一致）：**

```yaml
command:
  - 'mysqld'
  - '--character-set-server=utf8mb4'
  - '--collation-server=utf8mb4_unicode_ci'
  - '--default-time-zone=+8:00'
  - '--lower-case-table-names=1'
  - '--log-bin=mysql-bin'           # 开启 binlog，用于主从复制
  - '--binlog-format=ROW'
  - '--max_connections=2000'
```

## 五、Redis 部署模式

### 5.1 dev / test：单节点

dev 与 test 环境使用单节点 Redis，采用带布隆过滤器的 `rebloom:latest` 镜像：

```yaml
frameworkJava-redis:
  image: rebloom:latest              # 带布隆过滤器版本
  ports:
    - "6379:6379"
  volumes:
    - ./redis/conf/redis.conf:/usr/local/etc/redis/redis.conf
    - ../data/frameworkJavadata/redis/data:/data
  command: redis-server /usr/local/etc/redis/redis.conf
```

### 5.2 prd：集群模式（3 主 3 从，跨机部署）

prd 环境部署 6 个 Redis 节点构成 Cluster 集群，3 主 3 从，跨 VM 部署以实现故障隔离：

| 节点 | 容器名 | 部署位置 | 端口 | 集群总线端口 |
|------|--------|---------|------|------------|
| redis01 | `frameworkJava-redis01` | vm1 | 6380 | 16380 |
| redis02 | `frameworkJava-redis02` | vm1 | 6381 | 16381 |
| redis03 | `frameworkJava-redis03` | vm1 | 6382 | 16382 |
| redis04 | `frameworkJava-redis04` | vm2 | 6383 | 16383 |
| redis05 | `frameworkJava-redis05` | vm2 | 6384 | 16384 |
| redis06 | `frameworkJava-redis06` | vm2 | 6385 | 16385 |

每个节点使用独立的配置文件（`redis01.conf` ~ `redis06.conf`）与独立数据卷。6 个节点全部启动后，执行以下命令完成集群创建（3 主 3 从，每个主节点配一个从节点，主从跨机分布）：

```bash
# 在任意一个 Redis 节点所在宿主机执行
redis-cli --cluster create \
  <vm1内网ip>:6380 <vm1内网ip>:6381 <vm1内网ip>:6382 \
  <vm2内网ip>:6383 <vm2内网ip>:6384 <vm2内网ip>:6385 \
  --cluster-replicas 1
```

> **说明**：集群总线端口（16380-16385）需与数据端口（6380-6385）一同开放，否则集群节点间通信会失败。

## 六、服务 Dockerfile 模板

所有业务服务（admin、file、gateway、portal）共用同一套 Dockerfile 模板，统一基于 `eclipse-temurin:21-jre` 基础镜像。以 `deploy/prd/vm1/app/service/admin/Dockerfile` 为例：

```dockerfile
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /workspace
LABEL maintainer="JavaFH@163.com"
COPY ./*.jar /workspace/app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```

**说明：**

- 基础镜像 `eclipse-temurin:21-jre` 提供 Java 21 运行时
- 通过 `TZ=Asia/Shanghai` 与 `ln -snf` 设置时区
- 构建上下文为对应服务模块目录（`service/admin`、`service/file` 等），需包含 Maven 打包生成的 `*.jar`
- `JAVA_OPTS` 通过环境变量注入，支持运行时配置 JVM 参数（如 SkyWalking Agent、内存限制等）

## 七、镜像构建方式

### 7.1 dev / test：docker-maven-plugin

dev 与 test 环境不提供 `docker-compose-app.yml`，业务服务镜像通过 Maven 的 `docker-maven-plugin` 插件在构建期打包并推送：

```bash
# 在项目根目录执行
mvn clean package -DskipTests
# 由 docker-maven-plugin 自动构建镜像并推送到镜像仓库
```

构建出的镜像可直接在本地或测试服务器上通过 `docker run` 启动，便于开发调试。

### 7.2 prd：docker-compose 本地构建

prd 环境通过 `docker-compose-app.yml` 中的 `build` 字段在部署时本地构建镜像，确保生产环境镜像与代码版本严格一致：

```yaml
frameworkJava-admin:
  build:
    context: service/admin          # 构建上下文为 service/admin 目录
  image: <镜像仓库>/zmbdp-admin-service:1.0
  container_name: frameworkJava-admin-prd
  ports:
    - "18081:18081"
  environment:
    RUN_ENV: prd
    NACOS_ADDR: <vm1内网ip>:8854,<vm1内网ip>:8856,<vm2内网ip>:8858
    JAVA_OPTS: >-
      -javaagent:/skywalking-agent/skywalking-agent.jar
      -Dskywalking.agent.service_name=zmbdp-admin
      -Dskywalking.collector.backend_service=<vm1内网ip>:11800
      -Xmx256m
      -Dspring.cloud.nacos.discovery.ip=<当前VM内网ip>
  volumes:
    - ../../skywalking/skywalking-agent:/skywalking-agent  # 挂载 SkyWalking Agent
```

**prd 业务服务端口清单（vm1 与 vm2 一致）：**

| 服务 | 容器名 | 端口 | SkyWalking service_name |
|------|--------|------|------------------------|
| admin | `frameworkJava-admin-prd` | 18081 | zmbdp-admin |
| file | `frameworkJava-file-prd` | 18082 | zmbdp-file |
| gateway | `frameworkJava-gateway-prd` | 10030 | zmbdp-gateway |
| portal | `frameworkJava-portal-prd` | 18083 | zmbdp-portal |

> **说明**：vm2 上的应用服务通过 `JAVA_OPTS` 中的 `-Dspring.cloud.nacos.discovery.ip` 指向 vm2 内网 IP，确保服务注册时上报的是 vm2 地址。

## 八、数据库初始化顺序

MySQL 容器首次启动时，会自动执行 `docker-entrypoint-initdb.d/` 下的初始化脚本。框架通过 `init.sql` 串联所有初始化 SQL，确保按依赖顺序执行。

`deploy/dev/app/mysql/init/init.sql` 内容如下：

```sql
source /opt/sql/inituser.sql;
source /opt/sql/nacos.sql;
source /opt/sql/xxljob.sql;
source /opt/sql/db.sql;
```

**执行顺序与用途：**

| 顺序 | 脚本 | 说明 |
|------|------|------|
| 1 | `init.sql` | 入口脚本，由 MySQL 镜像 `docker-entrypoint-initdb.d` 自动执行 |
| 2 | `inituser.sql` | 创建业务数据库账号（dev：`zmbdpdev`，test：`zmbdptest`，prd：`zmbdpprd`）并授权 |
| 3 | `nacos.sql` | 创建 Nacos 配置中心数据库 `frameworkJava_nacos_{env}` 及表结构 |
| 4 | `xxljob.sql` | 创建 XXL-JOB 调度数据库 `frameworkJava_xxljob_{env}` 及表结构 |
| 5 | `db.sql` | 创建业务数据库 `frameworkJava_db_{env}` 及业务表结构 |

**SQL 文件位置：**

- dev：`deploy/dev/app/mysql/sql/`（含 `inituser.sql`、`nacos.sql`、`xxljob.sql`、`db.sql`）
- test：`deploy/test/app/mysql/sql/`（含 `inituser.sql`、`nacos.sql`、`xxljob.sql`），`db.sql` 位于 `deploy/test/res/sql/`
- prd：`deploy/prd/vm1/app/mysql/sql/`（含 `inituser.sql`、`nacos.sql`、`xxljob.sql`），`db.sql` 位于 `deploy/prd/vm1/res/sql/`

> **说明**：`init.sql` 通过 `source /opt/sql/xxx.sql` 引用其他脚本，对应 compose 中挂载的 `./mysql/sql:/opt/sql` 目录。prd vm2 的 MySQL 为 slave 节点，使用 `init.sh` 而非 `init.sql`，通过主从复制同步数据，不重复执行初始化。

## 九、Nacos 配置导入流程

部署前需将各环境的 Nacos 配置文件导入到 Nacos 配置中心，配置导入的详细说明参见 [CONFIGURATION.md](./CONFIGURATION.md)。

**导入步骤：**

1. 进入对应环境的配置目录：

   ```bash
   # dev
   cd deploy/dev/res/sql/DEFAULT_GROUP/
   # test
   cd deploy/test/res/sql/nacos_config/DEFAULT_GROUP/
   # prd
   cd deploy/prd/vm1/res/sql/DEFAULT_GROUP/
   ```

2. 将整个 `DEFAULT_GROUP` 目录打包成 zip 文件：

   ```bash
   cd ..
   zip -r DEFAULT_GROUP.zip DEFAULT_GROUP
   ```

3. 启动 Nacos 后，登录 Nacos 控制台（如 dev：`http://localhost:8848/nacos`）
4. 进入「配置管理 → 配置列表」，点击「导入配置」
5. 选择上一步生成的 zip 文件上传，系统会自动创建对应的配置

**配置文件命名规范：**

- 共享配置：`share-{模块}-{env}.yaml`（如 `share-redis-dev.yaml`、`share-mysql-prd.yaml`）
- 服务配置：`zmbdp-{service}-{env}.yaml`（如 `zmbdp-admin-service-prd.yaml`）

完整配置项与共享配置清单参见 [CONFIGURATION.md](./CONFIGURATION.md)。

## 十、监控告警部署

监控告警体系由 Prometheus（指标采集）、Grafana（可视化）、Alertmanager（告警通知）组成，三者均通过 `docker-compose-mid.yml` 部署在 vm1（dev/test 为单机）。监控指标的详细说明与告警规则参见 [MONITORING.md](./MONITORING.md)。

### 10.1 Grafana Dashboard 自动 provisioning

Grafana 通过文件方式自动加载 Dashboard，无需手动导入。配置文件位于 `deploy/{env}/app/grafana/provisioning/dashboards/dashboards.yml`：

```yaml
# Grafana Dashboard 配置
apiVersion: 1

providers:
  - name: 'frameworkJava Dashboards'
    orgId: 1
    folder: 'frameworkJava'              # Dashboard 所属文件夹
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10            # 每 10 秒扫描一次变更
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards  # Dashboard JSON 文件挂载路径
      foldersFromFilesStructure: true
```

Dashboard JSON 文件位于 `deploy/{env}/app/grafana/dashboards/`，通过 volume 挂载到容器内 `/var/lib/grafana/dashboards`：

- `api-monitoring.json`：接口 QPS、响应时间、错误率监控
- `jvm-monitoring.json`：JVM 内存、GC、线程、CPU 监控

### 10.2 Prometheus 数据源自动配置

数据源通过 `provisioning/datasources/prometheus.yml` 自动注入，无需手动在 Grafana 中添加。

### 10.3 Prometheus 告警规则

告警规则文件位于 `deploy/{env}/app/prometheus/rules/service_alerts.yml`，通过 volume 挂载到容器内 `/etc/prometheus/rules`，Prometheus 启动时自动加载。告警规则与通知渠道的详细配置参见 [MONITORING.md](./MONITORING.md)。

### 10.4 访问地址

| 服务 | dev/test 地址 | prd 地址（vm1） |
|------|--------------|----------------|
| Prometheus | `http://localhost:9090` | `http://<vm1内网ip>:9090` |
| Grafana | `http://localhost:3000` | `http://<vm1内网ip>:3000` |
| Alertmanager | `http://localhost:9093` | `http://<vm1内网ip>:9093` |

## 十一、SkyWalking Agent 部署

SkyWalking 链路追踪由 OAP 后端 + UI + Agent 三部分组成。链路追踪的使用说明与方案选型参见 [TRACING.md](./TRACING.md)。

### 11.1 OAP 镜像构建

SkyWalking OAP 镜像通过 `deploy/skywalking/Dockerfile` 构建，基于官方 `skywalking-oap:9.7.0-mysql` 镜像，额外修正 MySQL 驱动权限：

```dockerfile
FROM skywalking-oap:9.7.0-mysql
# 设置权限
RUN chmod 644 /skywalking/oap-libs/mysql-connector-j-8.0.33.jar
```

各环境 `docker-compose-mid.yml` 中通过 `build` 字段引用该 Dockerfile：

```yaml
frameworkJava-skywalking-oap:
  build:
    context: ../../skywalking       # dev/test
    # context: ../../skywalking     # prd（路径相对 prd/vm1/app）
    dockerfile: Dockerfile
  ports:
    - "11800:11800"                 # gRPC 端口（Agent 上报数据）
    - "12800:12800"                 # HTTP 端口（UI 查询数据）
  environment:
    SW_STORAGE: mysql
    SW_JDBC_URL: jdbc:mysql://frameworkJava-mysql:3306/frameworkJava_skywalking_{env}?...
```

### 11.2 Agent 下载

Agent 文件通过 `deploy/skywalking/download-agent.sh` 脚本下载（版本 9.5.0）：

```bash
#!/bin/bash
agentVersion="9.5.0"
agentDir="apache-skywalking-java-agent-${agentVersion}"
agentTar="${agentDir}.tgz"
downloadUrl="https://archive.apache.org/dist/skywalking/java-agent/${agentVersion}/${agentTar}"

# 下载并解压
curl -L -O "$downloadUrl"
tar -xzf "$agentTar"
rm "$agentTar"
```

Windows 环境可使用 `download-agent.ps1`。下载完成后，Agent 文件位于 `deploy/skywalking/skywalking-agent/` 目录。

### 11.3 Agent 挂载

prd 环境业务服务通过 volume 挂载 Agent，并在 `JAVA_OPTS` 中通过 `-javaagent` 参数加载：

```yaml
volumes:
  - ../../skywalking/skywalking-agent:/skywalking-agent  # 挂载 SkyWalking Agent
environment:
  JAVA_OPTS: >-
    -javaagent:/skywalking-agent/skywalking-agent.jar
    -Dskywalking.agent.service_name=zmbdp-admin
    -Dskywalking.collector.backend_service=<vm1内网ip>:11800
```

## 十二、Nginx 前端部署

前端通过 Nginx 容器部署，将打包后的静态资源（`web/dist/index.html`）通过 volume 挂载到容器内对外提供服务。

### 12.1 test 环境

```yaml
frameworkJava-web:
  image: nginx:1.24.0
  container_name: frameworkJava-web
  ports:
    - "80:80"
  volumes:
    - ./nginx/conf/nginx.conf:/etc/nginx/nginx.conf
    - ./nginx/web/dist:/data/frameworkJava/web/dist   # 前端静态资源
```

访问地址：`http://<test服务器ip>/`

### 12.2 prd 环境（vm1）

```yaml
frameworkJava-web-prd01:
  image: nginx:1.24.0
  container_name: frameworkJava-web-prd01
  ports:
    - "8666:80"
  volumes:
    - ./nginx/conf/nginx.conf:/etc/nginx/nginx.conf
    - ./nginx/web/dist:/data/frameworkJava/web/dist   # 前端静态资源，需包含 index.html
```

访问地址：`http://<vm1内网ip>:8666/`

> **说明**：部署前需将前端构建产物（含 `index.html`）放入对应环境的 `nginx/web/dist/` 目录。prd vm1 的 `deploy/prd/vm1/app/nginx/web/dist/index.html` 为前端入口文件。

### 12.3 prd 环境 Nacos 代理（vm2）

vm2 额外部署一个 Nginx 容器 `frameworkJava-webnacos`，用于对 3 节点 Nacos 集群做负载均衡：

```yaml
frameworkJava-webnacos:
  image: nginx:1.24.0
  container_name: frameworkJava-webnacos
  ports:
    - "8866:8886"
  volumes:
    - ./nacos/nginxconf/nginx.conf:/etc/nginx/nginx.conf  # Nacos 负载均衡配置
```

## 十三、Docker 安装

项目提供 Docker 一键安装脚本，位于 `deploy/docker-install/`，支持 Ubuntu、Debian、CentOS、Rocky 系统。

### 13.1 脚本文件

| 文件 | 说明 |
|------|------|
| `install_docker.sh` | 一键安装脚本，自动检测系统、配置软件源、安装 Docker Engine 与 Compose 插件 |
| `docker-mirrors.conf` | 国内镜像加速地址配置 |

### 13.2 安装步骤

```bash
# 进入安装脚本目录
cd deploy/docker-install

# 使用 sudo 执行安装脚本
sudo bash install_docker.sh
```

### 13.3 脚本能力

- **系统检测**：自动识别 Ubuntu / Debian / CentOS / Rocky，选择对应包管理器
- **安装检测**：检测是否已安装 Docker，支持保留、卸载重装、跳过检测三种选项
- **网络区域选择**：
  - 国内网络方案：使用阿里云 Docker 软件源，可选配置镜像加速
  - 国际网络方案：使用 Docker 官方软件源
- **安装内容**：`docker-ce`、`docker-ce-cli`、`containerd.io`、`docker-buildx-plugin`、`docker-compose-plugin`
- **自动启动**：安装完成后自动启动 Docker 服务并设置为开机自启
- **安装验证**：通过运行 `hello-world` 镜像验证安装结果

> **说明**：脚本必须使用 `sudo` 或 root 用户执行。安装完成后可通过 `docker --version` 与 `docker compose version` 验证安装结果。
