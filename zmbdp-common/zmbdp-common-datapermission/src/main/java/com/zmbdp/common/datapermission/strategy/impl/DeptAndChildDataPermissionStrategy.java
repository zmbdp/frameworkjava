package com.zmbdp.common.datapermission.strategy.impl;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import com.zmbdp.common.datapermission.strategy.DataPermissionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 本部门及子部门数据权限策略处理器
 * <p>
 * 可以查看本部门及所有子部门的数据，通过部门 ID 列表进行过滤，使用 IN 条件。<br>
 * 适用于部门经理、区域经理等需要查看本部门及下级部门数据的角色。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>处理 {@code DataPermissionType.DEPT_AND_CHILD} 权限类型</li>
 *     <li>从上下文中获取当前用户的部门及子部门 ID 列表（{@code context.getDeptIds()}）</li>
 *     <li>从注解或全局配置中获取部门字段名（默认 {@code dept_id}）</li>
 *     <li>构建 SQL 过滤条件：{@code dept_id IN (?, ?, ?)}</li>
 *     <li>支持表别名，适配复杂 SQL（JOIN、子查询等）</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>部门经理：</b>查看本部门及所有子部门的员工、订单、报销单等</li>
 *     <li><b>区域经理：</b>查看所辖区域所有部门的数据</li>
 *     <li><b>层级管理：</b>支持多级部门树的数据权限控制</li>
 *     <li><b>数据统计：</b>统计本部门及下级部门的业绩、考勤等数据</li>
 * </ul>
 * <p>
 * <b>SQL 示例：</b>
 * <pre>{@code
 * // 原始 SQL
 * SELECT * FROM sys_user WHERE status = '1'
 *
 * // 改写后（无表别名）
 * SELECT * FROM sys_user WHERE (dept_id IN (10, 11, 12)) AND status = '1'
 *
 * // 改写后（有表别名）
 * SELECT * FROM sys_user u WHERE (u.dept_id IN (10, 11, 12)) AND u.status = '1'
 * }</pre>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * // 使用默认字段名（dept_id）
 * DataPermission(type = DataPermissionType.DEPT_AND_CHILD);
 * List<SysUser> selectUserList(SysUserQuery query);
 *
 * // 自定义字段名（department_id）
 * DataPermission(type = DataPermissionType.DEPT_AND_CHILD, deptColumn = "department_id");
 * List<SysUser> selectUserList(SysUserQuery query);
 *
 * // 使用表别名（适配 JOIN 查询）
 * DataPermission(type = DataPermissionType.DEPT_AND_CHILD, tableAlias = "u");
 * List<SysUser> selectUserList(SysUserQuery query);
 * }</pre>
 * <p>
 * <b>字段名优先级：</b>
 * <ol>
 *     <li>注解配置：{@code @DataPermission(deptColumn = "department_id")}</li>
 *     <li>Nacos 全局配置：{@code datapermission.default-dept-column=dept_id}</li>
 *     <li>代码默认值：{@code dept_id}</li>
 * </ol>
 * <p>
 * <b>与 DEPT 的区别：</b>
 * <ul>
 *     <li><b>DEPT：</b>只查看本部门数据，使用 {@code dept_id = ?} 过滤</li>
 *     <li><b>DEPT_AND_CHILD：</b>查看本部门及所有子部门数据，使用 {@code dept_id IN (?, ?, ?)} 过滤</li>
 * </ul>
 * <p>
 * <b>部门 ID 列表说明：</b>
 * <ul>
 *     <li>需要提前查询并设置部门树的所有子部门 ID，拦截器不会自动查询</li>
 *     <li>通常在用户登录时，从数据库或缓存中加载部门树，递归查询所有子部门 ID</li>
 *     <li>部门 ID 列表包含当前部门 ID 和所有子部门 ID（如 [10, 11, 12, 13]）</li>
 *     <li>如果部门树层级较深，建议使用缓存（Redis）存储部门树，避免频繁查询数据库</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>如果 {@code context.getDeptIds()} 为空，返回 null，不添加过滤条件（记录警告日志）</li>
 *     <li>部门 ID 列表直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据）</li>
 *     <li>如果表有别名，必须在注解中配置 {@code tableAlias}，否则 SQL 可能报错</li>
 *     <li>支持 Nacos 热配置，可动态修改默认字段名，无需重启服务</li>
 *     <li>部门 ID 列表需要提前查询并设置到上下文中，拦截器不会自动查询</li>
 *     <li>如果部门树层级较深，IN 条件可能包含大量 ID，注意 SQL 性能</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType#DEPT_AND_CHILD
 * @see com.zmbdp.common.datapermission.context.DataPermissionContext#getDeptIds()
 * @see DeptDataPermissionStrategy
 */
@Slf4j
@Component
@RefreshScope
public class DeptAndChildDataPermissionStrategy implements DataPermissionStrategy {

    /**
     * 默认部门字段名（从 Nacos 配置中心读取）
     * <p>
     * 用于构建本部门及子部门数据权限过滤条件的字段名。<br>
     * 如果注解中未指定 {@code deptColumn}，则使用此默认值。
     * <p>
     * <b>配置项：</b>{@code datapermission.default-dept-column}，默认值：{@code dept_id}
     */
    @Value("${datapermission.default-dept-column:dept_id}")
    private String defaultDeptColumn;

    /**
     * 获取处理器支持的权限类型
     *
     * @return 数据权限类型枚举 {@code DEPT_AND_CHILD}
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.DEPT_AND_CHILD;
    }

    /**
     * 构建本部门及子部门数据权限过滤条件
     * <p>
     * 根据当前用户的部门及子部门 ID 列表构建 SQL 过滤条件，限制用户只能查看本部门及所有子部门的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. Mapper 接口方法配置
     * @DataPermission(type = DataPermissionType.DEPT_AND_CHILD)
     * List<SysUser> selectUserList(SysUserQuery query);
     * 
     * // 原始 SQL：SELECT * FROM sys_user WHERE status = '1'
     * // 改写后：SELECT * FROM sys_user WHERE (dept_id IN (10, 11, 12)) AND status = '1'
     *
     * // 2. 自定义部门字段名
     * @DataPermission(type = DataPermissionType.DEPT_AND_CHILD, deptColumn = "department_id")
     * List<SysUser> selectUserList(SysUserQuery query);
     * 
     * // 改写后：SELECT * FROM sys_user WHERE (department_id IN (10, 11, 12)) AND status = '1'
     *
     * // 3. 使用表别名（JOIN 查询）
     * @DataPermission(type = DataPermissionType.DEPT_AND_CHILD, tableAlias = "u")
     * List<SysUser> selectUserList(SysUserQuery query);
     * 
     * // 原始 SQL：SELECT * FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id
     * // 改写后：SELECT * FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id WHERE (u.dept_id IN (10, 11, 12))
     *
     * // 4. 设置部门 ID 列表（在 Filter 或 Interceptor 中）
     * DataPermissionContext context = new DataPermissionContext();
     * context.setDeptId(10L); // 当前部门 ID
     * context.setDeptIds(Arrays.asList(10L, 11L, 12L)); // 包含所有子部门 ID
     * DataPermissionContext.set(context);
     * }</pre>
     * <p>
     * <b>字段名优先级：</b>注解配置 > 全局配置<br>
     * <b>表别名支持：</b>无别名 {@code dept_id IN (10, 11, 12)}，有别名 {@code d.dept_id IN (10, 11, 12)}<br>
     * <b>注意：</b>部门 ID 列表需要提前查询并设置到上下文中，拦截器不会自动查询
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果上下文中 deptIds 为空，返回 null，不添加过滤条件（记录警告日志）</li>
     *     <li>部门 ID 列表直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据）</li>
     *     <li>如果表有别名，必须在注解中配置 {@code tableAlias}，否则 SQL 可能报错</li>
     *     <li>支持 Nacos 热配置，可动态修改默认字段名，无需重启服务</li>
     *     <li>部门 ID 列表需要提前查询并设置到上下文中，拦截器不会自动查询</li>
     *     <li>如果部门树层级较深，IN 条件可能包含大量 ID，注意 SQL 性能</li>
     *     <li>建议使用缓存（Redis）存储部门树，避免频繁查询数据库</li>
     * </ul>
     *
     * @param annotation 数据权限注解，包含部门字段名、表别名等配置，不能为 null
     * @param context    数据权限上下文，包含当前用户的部门及子部门 ID 列表，不能为 null
     * @return SQL 过滤条件（如 {@code dept_id IN (10, 11, 12)}），如果部门 ID 列表为空返回 null
     * @see com.zmbdp.common.datapermission.annotation.DataPermission
     * @see com.zmbdp.common.datapermission.context.DataPermissionContext#getDeptIds()
     * @see DeptDataPermissionStrategy
     */
    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        if (context == null || context.getDeptIds() == null || context.getDeptIds().isEmpty()) {
            log.warn("数据权限：DEPT_AND_CHILD - 部门 ID 列表为空，跳过过滤");
            return null;
        }

        // 获取部门字段名（优先级：注解 > 全局配置）
        String deptColumn = StringUtil.isNotEmpty(annotation.deptColumn())
                ? annotation.deptColumn()
                : defaultDeptColumn;

        // 如果有表别名，添加别名前缀
        if (StringUtil.isNotEmpty(annotation.tableAlias())) {
            deptColumn = annotation.tableAlias() + "." + deptColumn;
        }

        // 构建 IN 条件
        List<Long> deptIds = context.getDeptIds();
        String deptIdStr = deptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        String condition = deptColumn + " IN (" + deptIdStr + ")";
        log.debug("数据权限：DEPT_AND_CHILD - 过滤条件：{}", condition);
        return condition;
    }
}