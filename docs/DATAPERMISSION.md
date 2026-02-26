# 数据权限控制

## 概述

数据权限控制通过 **AOP + 注解 + MyBatis 拦截器** 实现，业务代码无感知。支持 **本人、本部门、本部门及子部门、全部、自定义** 五种权限类型，支持 **多租户过滤** 与 **Nacos 热配置**。

## 核心组件

- `@DataPermission`：标记需要数据权限控制的方法
- `DataPermissionInterceptor`：MyBatis 拦截器，拦截 SQL 执行
- `DataPermissionHandler`：权限处理器接口（策略模式，可扩展）
- `SelfDataPermissionHandler`：本人数据处理器
- `DeptDataPermissionHandler`：本部门数据处理器
- `DeptAndChildDataPermissionHandler`：本部门及子部门数据处理器
- `AllDataPermissionHandler`：全部数据处理器
- `CustomDataPermissionHandler`：自定义条件处理器
- `DataPermissionContext`：权限上下文（ThreadLocal）
- Redis：存储部门层级关系（可选，用于子部门查询优化）

## 应用场景

### 多租户 SaaS 系统

- **租户隔离**：每个租户只能看到自己的数据
- **部门隔离**：部门主管只能看到本部门的数据
- **角色权限**：不同角色看到不同范围的数据

### 企业内部系统

- **数据安全**：员工只能看到自己创建的数据
- **层级管理**：经理可以看到下属部门的数据
- **超级管理员**：可以看到所有数据

## 实现方案

FrameworkJava 采用 **MyBatis 拦截器 + 策略模式** 实现数据权限控制。

### 工作原理

```
请求 → 设置权限上下文 → 执行业务方法 → MyBatis 执行 SQL
                                    ↓
                          拦截器拦截 SQL → 解析 SQL
                                    ↓
                          获取权限上下文 → 选择权限处理器
                                    ↓
                          构建权限条件 → 修改 SQL（添加 WHERE 条件）
                                    ↓
                          执行修改后的 SQL → 返回结果
```

## 使用方式

### 1. 引入依赖

在需要使用数据权限的服务模块（如 portal、admin）的 `pom.xml` 中增加：

```xml
<dependency>
    <groupId>com.zmbdp</groupId>
    <artifactId>zmbdp-common-datapermission</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 在 Mapper 方法上使用注解

```java
// 仅查询本人数据
@DataPermission(type = DataPermissionType.SELF)
List<Order> selectOrderList();

// 查询本部门数据
@DataPermission(type = DataPermissionType.DEPT)
List<Order> selectDeptOrderList();

// 查询本部门及子部门数据
@DataPermission(type = DataPermissionType.DEPT_AND_CHILD)
List<Order> selectDeptAndChildOrderList();

// 查询全部数据（超级管理员）
@DataPermission(type = DataPermissionType.ALL)
List<Order> selectAllOrderList();

// 自定义条件（如：只查询已支付的订单）
@DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '2'")
List<Order> selectPaidOrderList();

// 自定义字段名（默认：user_id、dept_id）
@DataPermission(type = DataPermissionType.SELF, userColumn = "create_user_id")
List<Order> selectMyOrderList();

// 启用多租户过滤
@DataPermission(type = DataPermissionType.SELF, enableTenant = true)
List<Order> selectMyOrderListWithTenant();

// 使用表别名
@DataPermission(type = DataPermissionType.SELF, tableAlias = "o")
List<Order> selectOrderListWithAlias();
```

### 3. 设置权限上下文

在 Controller 或 Service 中设置权限上下文：

**方式一：使用工具类（推荐）**

```java
@RestController
public class OrderController {
    
    @GetMapping("/order/list")
    public Result<List<Order>> list() {
        // 方式1：自动清理（推荐）
        List<Order> orders = DataPermissionUtils.executeWithSelf(getCurrentUserId(), () -> {
            return orderMapper.selectOrderList();
        });
        return Result.success(orders);
        
        // 方式2：手动清理
        DataPermissionUtils.setSelf(getCurrentUserId());
        try {
            return Result.success(orderMapper.selectOrderList());
        } finally {
            DataPermissionUtils.clear();
        }
    }
    
    @GetMapping("/order/dept")
    public Result<List<Order>> listDept() {
        // 查询本部门数据
        List<Order> orders = DataPermissionUtils.executeWithDept(
            getCurrentUserId(), 
            getCurrentDeptId(), 
            () -> orderMapper.selectOrderList()
        );
        return Result.success(orders);
    }
    
    @GetMapping("/order/all")
    public Result<List<Order>> listAll() {
        // 自定义权限配置
        DataPermissionContext context = DataPermissionUtils.builder()
            .userId(getCurrentUserId())
            .deptId(getCurrentDeptId())
            .tenantId(getCurrentTenantId())
            .permissionType(DataPermissionType.DEPT)
            .isAdmin(isSuperAdmin())
            .buildContext();
        
        List<Order> orders = DataPermissionUtils.executeWith(context, () -> {
            return orderMapper.selectOrderList();
        });
        return Result.success(orders);
    }
}
```

**方式二：直接使用上下文（不推荐）**

```java
@RestController
public class OrderController {
    
    @GetMapping("/order/list")
    public Result<List<Order>> list() {
        // 设置权限上下文
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(getCurrentUserId());
        context.setDeptId(getCurrentDeptId());
        context.setTenantId(getCurrentTenantId());
        context.setPermissionType(DataPermissionType.SELF);
        context.setIsAdmin(isSuperAdmin());
        DataPermissionContext.set(context);
        
        try {
            return Result.success(orderMapper.selectOrderList());
        } finally {
            DataPermissionContext.clear(); // 必须清理
        }
    }
}
```

## 权限类型

| 类型 | 说明 | SQL 示例 |
|------|------|----------|
| `SELF` | 仅本人数据 | `WHERE user_id = 当前用户ID` |
| `DEPT` | 本部门数据 | `WHERE dept_id = 当前部门ID` |
| `DEPT_AND_CHILD` | 本部门及子部门数据 | `WHERE dept_id IN (当前部门ID, 子部门ID...)` |
| `ALL` | 全部数据 | 不添加条件 |
| `CUSTOM` | 自定义条件 | `WHERE {customCondition}` |

## 配置说明

### Nacos 全局配置（可选）

在 Nacos 配置 `share-datapermission-{env}.yaml`（可选，不配置也能用）：

```yaml
datapermission:
  # 全局开关（默认：true）
  enabled: true
  
  # 默认用户字段名（默认：user_id）
  default-user-column: user_id
  
  # 默认部门字段名（默认：dept_id）
  default-dept-column: dept_id
  
  # 默认租户字段名（默认：tenant_id）
  default-tenant-column: tenant_id
  
  # 是否启用多租户过滤（默认：false）
  enable-tenant: false
```

### 注解级覆盖

- `userColumn`：用户字段名；为空时使用全局配置。
- `deptColumn`：部门字段名；为空时使用全局配置。
- `tenantColumn`：租户字段名；为空时使用全局配置。
- `enableTenant`：是否启用多租户过滤；默认 false。
- `tableAlias`：表别名；为空时不使用别名。
- `customCondition`：自定义条件（仅 `CUSTOM` 类型有效）。

## SQL 改写示例

### 原始 SQL

```sql
SELECT * FROM t_order WHERE status = '1'
```

### 改写后 SQL

**本人数据（SELF）**：
```sql
SELECT * FROM t_order WHERE status = '1' AND user_id = 4
```

**本部门数据（DEPT）**：
```sql
SELECT * FROM t_order WHERE status = '1' AND dept_id = 5
```

**本部门及子部门（DEPT_AND_CHILD）**：
```sql
SELECT * FROM t_order WHERE status = '1' AND dept_id IN (5, 6, 7)
```

**自定义条件（CUSTOM）**：
```sql
SELECT * FROM t_order WHERE status = '1' AND (status = '2')
```

**多租户过滤**：
```sql
SELECT * FROM t_order WHERE status = '1' AND user_id = 4 AND tenant_id = 1
```

**使用表别名**：
```sql
SELECT * FROM t_order o WHERE o.status = '1' AND o.user_id = 4
```

## 注意事项

### 1. 权限上下文管理

- **设置上下文**：在 Controller 或 Service 中设置权限上下文
- **清理上下文**：使用 `try-finally` 确保上下文被清理，避免内存泄漏
- **ThreadLocal**：上下文存储在 ThreadLocal 中，线程安全

### 2. 超级管理员

- 超级管理员（`isSuperAdmin = true`）跳过数据权限过滤
- 适用于系统管理员、运维人员等需要查看所有数据的角色

### 3. 部门层级查询

- `DEPT_AND_CHILD` 类型需要查询子部门列表
- 建议在数据库中维护部门层级关系表（如 `parent_id`）
- 可使用 Redis 缓存部门层级关系，提升性能

### 4. 多租户支持

- 启用多租户过滤后，会自动添加 `tenant_id` 条件
- 适用于 SaaS 系统，确保租户数据隔离

### 5. 性能考虑

- MyBatis 拦截器会拦截所有 SQL，建议只在需要的 Mapper 方法上添加注解
- 部门层级查询建议使用 Redis 缓存，避免频繁查询数据库
- 自定义条件建议使用索引字段，避免全表扫描

### 6. SQL 兼容性

- 支持 MySQL、PostgreSQL、Oracle 等主流数据库
- 使用 JSqlParser 解析 SQL，支持复杂 SQL（JOIN、子查询等）
- 不支持存储过程、函数调用等特殊 SQL

## 最佳实践

1. **权限上下文管理**：使用 AOP 或拦截器统一设置权限上下文，避免在每个方法中重复设置。
2. **超级管理员判断**：根据用户角色判断是否为超级管理员，跳过数据权限过滤。
3. **部门层级缓存**：使用 Redis 缓存部门层级关系，提升查询性能。
4. **多租户隔离**：SaaS 系统建议启用多租户过滤，确保数据安全。
5. **自定义条件**：复杂权限逻辑可使用自定义条件，灵活控制数据范围。
6. **日志记录**：记录数据权限过滤日志，便于问题排查和审计。

## 常见问题

### Q: 如何判断用户是否为超级管理员？

A: 根据用户角色判断，如 `user.getRoles().contains("SUPER_ADMIN")`，然后在权限上下文中设置 `isSuperAdmin = true`。

### Q: 如何查询子部门列表？

A: 在数据库中维护部门层级关系表（如 `parent_id`），递归查询子部门。建议使用 Redis 缓存部门层级关系。

### Q: 数据权限会影响性能吗？

A: 影响很小。MyBatis 拦截器只在有 `@DataPermission` 注解的方法上生效，SQL 改写是内存操作，性能损耗可忽略。

### Q: 如何处理复杂 SQL（JOIN、子查询）？

A: 使用 JSqlParser 解析 SQL，支持复杂 SQL。建议在主表上添加权限条件，避免在 JOIN 表上添加。

### Q: 如何关闭数据权限？

A: 在 Nacos 配置 `datapermission.enabled = false`，或在方法上不添加 `@DataPermission` 注解。

