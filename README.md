# FrameworkJava - Spring Boot 微服务脚手架

<align"center">
  <href"https://spring.io/projects/spring-boot">
    <src"https://img.shields.io/badge/Spring%20Boot-3.3.3-green.svg" alt"Spring Boot">
  </>
  <href"https://spring.io/projects/spring-cloud">
    <src"https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg" alt"Spring Cloud">
  </>
</>

## 项目简介

FrameworkJava是一个基于Spring Boot 3.3.3和Spring Cloud 2023.0.3的企业级微服务脚手架，旨在帮助开发者快速构建高可用、高性能的Java微服务应用。项目采用模块化设计，集成了企业级应用所需的常见功能，包括但不限于统一认证授权、多级缓存、消息队列、配置中心等。

## 核心特性

**微服务架构**
**安全认证**
**三级缓存**
**模块化设计**
**开箱即用**
**容器化部署**
**监控友好**

## 技术栈

| 技术 | 版本 | 说明 |
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
## 快速开始

### 环境要求

-
-
-

### 一键部署

1.
bash git clone https://github.com/zmbdp/frameworkjava.git cd frameworkjava
2.
bash cd deploy/dev/app docker-compose -p frameworkjava -f docker-compose-mid.yml up -d
3.
bash docker-compose -p frameworkjava -f docker-compose-mid.yml ps
4.
bash 返回项目根目录
cd ../../../ mvn clean install -DskipTests 启动各服务模块...
### 访问地址

-
-
-
-

## 核心亮点详解

### 1. 三级缓存架构

项目实现了布隆过滤器 + Caffeine + Redis的三级缓存架构：
请求 -> 布隆过滤器(判断是否存在) -> Caffeine本地缓存 -> Redis缓存 -> 数据库
优势：
-
-
-

### 2. 安全认证机制

采用JWT无状态认证：
-
-
-

### 3. 微服务治理

-
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
// 使用CacheUtil工具类 java
T result = CacheUtil.getL2Cache(redisService, bloomFilterService, key, valueTypeRef, caffeineCache);
