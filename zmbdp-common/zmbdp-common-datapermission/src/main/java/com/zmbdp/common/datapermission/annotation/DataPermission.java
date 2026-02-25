package com.zmbdp.common.datapermission.annotation;

import com.zmbdp.common.datapermission.enums.DataPermissionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解
 * <p>
 * 用于标记需要进行数据权限过滤的 Mapper 方法，基于 <b>MyBatis 拦截器</b>实现。<br>
 * 业务代码无感知，仅加注解即可生效。<br>
 * 支持多种数据权限类型，可通过配置 {@code datapermission.enabled} 全局开关，默认开启。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 *     <li>支持 <b>全部、部门、部门及子部门、仅本人、自定义</b>五种权限类型</li>
 *     <li>支持 <b>Nacos 热配置</b>，无需重启即可调整数据权限参数</li>
 *     <li>支持 <b>方法级覆盖</b>，每个 Mapper 方法可独立配置权限规则</li>
 *     <li>支持 <b>多租户场景</b>，自动添加租户 ID 过滤条件</li>
 *     <li>支持 <b>自定义字段名</b>，灵活适配不同表结构</li>
 * </ul>
 * <p>
 * <b>工作原理：</b>
 * <ol>
 *     <li>拦截带 {@code @DataPermission} 的 Mapper 方法</li>
 *     <li>从当前登录用户上下文获取用户 ID、部门 ID、权限类型等信息</li>
 *     <li>根据权限类型动态构建 SQL 过滤条件</li>
 *     <li>通过 MyBatis 拦截器改写原始 SQL，添加 WHERE 条件</li>
 *     <li>执行改写后的 SQL，返回过滤后的数据</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 示例 1：使用默认配置（根据当前用户权限自动过滤）
 * @Mapper
 * public interface SysUserMapper extends BaseMapper<SysUser> {
 *     @DataPermission
 *     List<SysUser> selectList(SysUser sysUser);
 * }
 *
 * // 示例 2：指定用户字段名（表中用户字段不是 user_id）
 * @DataPermission(userColumn = "create_by")
 * List<Order> selectOrderList(Order order);
 *
 * // 示例 3：指定部门字段名（表中部门字段不是 dept_id）
 * @DataPermission(deptColumn = "department_id")
 * List<Employee> selectEmployeeList(Employee employee);
 *
 * // 示例 4：多租户场景（自动添加租户过滤）
 * @DataPermission(enableTenant = true, tenantColumn = "tenant_id")
 * List<Product> selectProductList(Product product);
 *
 * // 示例 5：自定义权限类型（强制使用指定权限）
 * @DataPermission(type = DataPermissionType.DEPT)
 * List<Report> selectReportList(Report report);
 *
 * // 示例 6：自定义 SQL 条件（复杂业务场景）
 * @DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '1' AND region_id IN (1, 2, 3)")
 * List<Data> selectCustomData(Data data);
 *
 * // 示例 7：指定表别名（SQL 中使用了表别名）
 * @DataPermission(tableAlias = "u")
 * @Select("SELECT u.* FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id WHERE u.status = '1'")
 * List<SysUser> selectUserWithDept();
 * }</pre>
 * <p>
 * <b>配置优先级：</b>
 * <ol>
 *     <li>注解参数（type、userColumn、deptColumn 等）</li>
 *     <li>Nacos 全局配置（datapermission.default-*）</li>
 *     <li>代码默认值</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>需要在用户登录时将权限类型、部门 ID 等信息存入上下文（如 ThreadLocal、Redis）</li>
 *     <li>表中必须有对应的用户字段或部门字段，否则过滤条件无法生效</li>
 *     <li>自定义权限类型需要提供 {@code customCondition} 参数</li>
 *     <li>多租户场景需要确保表中有租户字段</li>
 *     <li>使用表别名时，必须在注解中指定 {@code tableAlias} 参数</li>
 * </ul>
 * <p>
 * <b>最佳实践：</b>
 * <ul>
 *     <li>超级管理员建议使用 {@code ALL} 权限，不添加任何过滤条件</li>
 *     <li>部门主管建议使用 {@code DEPT} 或 {@code DEPT_AND_CHILD} 权限</li>
 *     <li>普通员工建议使用 {@code SELF} 权限，只能查看自己的数据</li>
 *     <li>多租户场景建议开启 {@code enableTenant}，确保数据隔离</li>
 *     <li>生产环境建议通过 Nacos 配置，便于动态调整，无需发版</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see DataPermissionType
 * @see com.zmbdp.common.datapermission.interceptor.DataPermissionInterceptor
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /**
     * 数据权限类型
     * <p>
     * 指定数据权限的过滤范围，支持全部、部门、部门及子部门、仅本人、自定义五种类型。
     * <p>
     * <b>类型说明：</b>
     * <ul>
     *     <li>{@code ALL}：全部数据权限，不添加任何过滤条件（适用于超级管理员）</li>
     *     <li>{@code DEPT}：本部门数据权限，仅查看本部门数据（适用于部门主管）</li>
     *     <li>{@code DEPT_AND_CHILD}：本部门及子部门数据权限（适用于部门经理）</li>
     *     <li>{@code SELF}：仅本人数据权限，只能查看自己创建的数据（适用于普通员工）</li>
     *     <li>{@code CUSTOM}：自定义数据权限，需要配合 {@code customCondition} 使用</li>
     * </ul>
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>未配置或配置为 {@code null}：从当前用户上下文获取权限类型</li>
     *     <li>已配置：使用注解指定的权限类型，优先级高于用户上下文</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：使用默认配置（从用户上下文获取）
     * @DataPermission
     * List<SysUser> selectList(SysUser sysUser);
     *
     * // 示例 2：强制使用部门权限
     * @DataPermission(type = DataPermissionType.DEPT)
     * List<Report> selectReportList(Report report);
     *
     * // 示例 3：强制使用仅本人权限
     * @DataPermission(type = DataPermissionType.SELF)
     * List<Order> selectMyOrders(Order order);
     *
     * // 示例 4：自定义权限（需要配合 customCondition）
     * @DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '1'")
     * List<Data> selectCustomData(Data data);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果不指定 {@code type}，会从当前用户上下文获取权限类型</li>
     *     <li>如果用户上下文中没有权限类型，默认使用 {@code SELF} 权限</li>
     *     <li>自定义权限类型必须提供 {@code customCondition} 参数</li>
     * </ul>
     *
     * @return 数据权限类型，默认 null（从用户上下文获取）
     * @see DataPermissionType
     */
    DataPermissionType type() default DataPermissionType.SELF;

    /**
     * 用户字段名（表中存储用户 ID 的字段）
     * <p>
     * 用于指定表中存储用户 ID 的字段名称，用于构建 {@code SELF} 权限的过滤条件。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用 Nacos 全局配置 {@code datapermission.default-user-column}</li>
     *     <li>非空：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：使用默认字段名（user_id）
     * @DataPermission
     * List<Order> selectOrderList(Order order);
     * // 生成 SQL：WHERE user_id = #{currentUserId}
     *
     * // 示例 2：自定义字段名（create_by）
     * @DataPermission(userColumn = "create_by")
     * List<Order> selectOrderList(Order order);
     * // 生成 SQL：WHERE create_by = #{currentUserId}
     *
     * // 示例 3：使用表别名
     * @DataPermission(userColumn = "creator_id", tableAlias = "o")
     * List<Order> selectOrderList(Order order);
     * // 生成 SQL：WHERE o.creator_id = #{currentUserId}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>字段名必须与表中实际字段名一致（区分大小写）</li>
     *     <li>如果使用了表别名，字段名前会自动添加别名前缀</li>
     *     <li>仅在 {@code SELF} 权限类型下生效</li>
     * </ul>
     *
     * @return 用户字段名，默认空字符串表示使用全局配置
     */
    String userColumn() default "";

    /**
     * 部门字段名（表中存储部门 ID 的字段）
     * <p>
     * 用于指定表中存储部门 ID 的字段名称，用于构建 {@code DEPT} 和 {@code DEPT_AND_CHILD} 权限的过滤条件。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用 Nacos 全局配置 {@code datapermission.default-dept-column}</li>
     *     <li>非空：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：使用默认字段名（dept_id）
     * @DataPermission(type = DataPermissionType.DEPT)
     * List<Employee> selectEmployeeList(Employee employee);
     * // 生成 SQL：WHERE dept_id = #{currentDeptId}
     *
     * // 示例 2：自定义字段名（department_id）
     * @DataPermission(type = DataPermissionType.DEPT, deptColumn = "department_id")
     * List<Employee> selectEmployeeList(Employee employee);
     * // 生成 SQL：WHERE department_id = #{currentDeptId}
     *
     * // 示例 3：部门及子部门权限
     * @DataPermission(type = DataPermissionType.DEPT_AND_CHILD, deptColumn = "dept_id")
     * List<Employee> selectEmployeeList(Employee employee);
     * // 生成 SQL：WHERE dept_id IN (#{currentDeptId}, #{childDeptIds})
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>字段名必须与表中实际字段名一致（区分大小写）</li>
     *     <li>如果使用了表别名，字段名前会自动添加别名前缀</li>
     *     <li>仅在 {@code DEPT} 和 {@code DEPT_AND_CHILD} 权限类型下生效</li>
     * </ul>
     *
     * @return 部门字段名，默认空字符串表示使用全局配置
     */
    String deptColumn() default "";

    /**
     * 表别名（SQL 中使用的表别名）
     * <p>
     * 用于指定 SQL 中使用的表别名，用于构建带别名的过滤条件。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>SQL 中使用了表别名（如 {@code SELECT u.* FROM sys_user u}）</li>
     *     <li>SQL 中有多表关联，需要明确指定字段所属表</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：单表查询使用别名
     * @DataPermission(tableAlias = "u")
     * @Select("SELECT u.* FROM sys_user u WHERE u.status = '1'")
     * List<SysUser> selectUserList();
     * // 生成 SQL：WHERE u.user_id = #{currentUserId}
     *
     * // 示例 2：多表关联查询
     * @DataPermission(tableAlias = "o", userColumn = "user_id")
     * @Select("SELECT o.*, u.name FROM orders o LEFT JOIN sys_user u ON o.user_id = u.id")
     * List<Order> selectOrderWithUser();
     * // 生成 SQL：WHERE o.user_id = #{currentUserId}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 SQL 中使用了表别名，必须在注解中指定 {@code tableAlias}</li>
     *     <li>别名会自动添加到字段名前，格式为 {@code alias.column}</li>
     *     <li>如果不指定别名，字段名前不会添加任何前缀</li>
     * </ul>
     *
     * @return 表别名，默认空字符串表示不使用别名
     */
    String tableAlias() default "";

    /**
     * 是否启用多租户过滤
     * <p>
     * 用于指定是否在数据权限过滤的基础上，额外添加租户 ID 过滤条件。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>SaaS 多租户系统，需要确保数据隔离</li>
     *     <li>同一套系统服务多个客户，每个客户的数据互相隔离</li>
     * </ul>
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>未配置：使用 Nacos 全局配置 {@code datapermission.enable-tenant}</li>
     *     <li>已配置：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：启用多租户过滤
     * @DataPermission(enableTenant = true, tenantColumn = "tenant_id")
     * List<Product> selectProductList(Product product);
     * // 生成 SQL：WHERE user_id = #{currentUserId} AND tenant_id = #{currentTenantId}
     *
     * // 示例 2：禁用多租户过滤
     * @DataPermission(enableTenant = false)
     * List<SysConfig> selectConfigList(SysConfig config);
     * // 生成 SQL：WHERE user_id = #{currentUserId}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>启用多租户过滤时，表中必须有租户字段</li>
     *     <li>需要在用户登录时将租户 ID 存入上下文</li>
     *     <li>租户过滤条件会与数据权限过滤条件通过 AND 连接</li>
     * </ul>
     *
     * @return 是否启用多租户过滤，默认 false
     */
    boolean enableTenant() default false;

    /**
     * 租户字段名（表中存储租户 ID 的字段）
     * <p>
     * 用于指定表中存储租户 ID 的字段名称，用于构建多租户过滤条件。
     * <p>
     * <b>配置规则：</b>
     * <ul>
     *     <li>为空或空白字符串：使用 Nacos 全局配置 {@code datapermission.default-tenant-column}</li>
     *     <li>非空：使用注解配置的值，优先级高于全局配置</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：使用默认字段名（tenant_id）
     * @DataPermission(enableTenant = true)
     * List<Product> selectProductList(Product product);
     * // 生成 SQL：WHERE user_id = #{currentUserId} AND tenant_id = #{currentTenantId}
     *
     * // 示例 2：自定义字段名（company_id）
     * @DataPermission(enableTenant = true, tenantColumn = "company_id")
     * List<Product> selectProductList(Product product);
     * // 生成 SQL：WHERE user_id = #{currentUserId} AND company_id = #{currentTenantId}
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>仅在 {@code enableTenant = true} 时生效</li>
     *     <li>字段名必须与表中实际字段名一致（区分大小写）</li>
     *     <li>如果使用了表别名，字段名前会自动添加别名前缀</li>
     * </ul>
     *
     * @return 租户字段名，默认空字符串表示使用全局配置
     */
    String tenantColumn() default "";

    /**
     * 自定义 SQL 条件（仅在 {@code type = CUSTOM} 时生效）
     * <p>
     * 用于指定自定义的 SQL 过滤条件，适用于复杂业务场景。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>需要根据多个字段进行过滤</li>
     *     <li>需要使用复杂的 SQL 表达式（如 IN、BETWEEN、LIKE 等）</li>
     *     <li>需要根据业务规则动态构建过滤条件</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 示例 1：简单条件
     * @DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '1'")
     * List<Data> selectActiveData(Data data);
     * // 生成 SQL：WHERE status = '1'
     *
     * // 示例 2：多条件组合
     * @DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '1' AND region_id IN (1, 2, 3)")
     * List<Data> selectRegionData(Data data);
     * // 生成 SQL：WHERE status = '1' AND region_id IN (1, 2, 3)
     *
     * // 示例 3：使用表别名
     * @DataPermission(type = DataPermissionType.CUSTOM, customCondition = "d.status = '1'", tableAlias = "d")
     * List<Data> selectCustomData(Data data);
     * // 生成 SQL：WHERE d.status = '1'
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>仅在 {@code type = CUSTOM} 时生效</li>
     *     <li>自定义条件会直接拼接到 SQL 中，注意 SQL 注入风险</li>
     *     <li>建议使用参数化查询，避免直接拼接用户输入</li>
     *     <li>如果使用了表别名，条件中的字段名需要手动添加别名前缀</li>
     * </ul>
     *
     * @return 自定义 SQL 条件，默认空字符串
     */
    String customCondition() default "";
}

