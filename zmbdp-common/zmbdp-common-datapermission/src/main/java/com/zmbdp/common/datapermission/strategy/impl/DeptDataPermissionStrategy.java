package com.zmbdp.common.datapermission.strategy.impl;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.strategy.DataPermissionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 本部门数据权限策略处理器
 * <p>
 * 只能查看本部门的数据，不包含子部门，通过部门 ID 字段进行过滤。<br>
 * 适用于部门主管等需要查看本部门数据但不包含下级部门的角色。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>处理 {@code DataPermissionType.DEPT} 权限类型</li>
 *     <li>从上下文中获取当前用户的部门 ID（{@code context.getDeptId()}）</li>
 *     <li>从注解或全局配置中获取部门字段名（默认 {@code dept_id}）</li>
 *     <li>构建 SQL 过滤条件：{@code dept_id = ?}</li>
 *     <li>支持表别名，适配复杂 SQL（JOIN、子查询等）</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>部门主管：</b>查看本部门的员工、订单、报销单等，不包含子部门</li>
 *     <li><b>部门统计：</b>统计本部门的业绩、考勤等数据</li>
 *     <li><b>部门管理：</b>管理本部门的资源、权限等</li>
 *     <li><b>数据隔离：</b>限制用户只能访问本部门的数据</li>
 * </ul>
 * <p>
 * <b>SQL 示例：</b>
 * <pre>{@code
 * // 原始 SQL
 * SELECT * FROM sys_user WHERE status = '1'
 *
 * // 改写后（无表别名）
 * SELECT * FROM sys_user WHERE (dept_id = 10) AND status = '1'
 *
 * // 改写后（有表别名）
 * SELECT * FROM sys_user u WHERE (u.dept_id = 10) AND u.status = '1'
 * }</pre>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * // 使用默认字段名（dept_id）
 * DataPermission(type = DataPermissionType.DEPT);
 * List<SysUser> selectUserList(SysUserQuery query);
 *
 * // 自定义字段名（department_id）
 * DataPermission(type = DataPermissionType.DEPT, deptColumn = "department_id");
 * List<SysUser> selectUserList(SysUserQuery query);
 *
 * // 使用表别名（适配 JOIN 查询）
 * DataPermission(type = DataPermissionType.DEPT, tableAlias = "u");
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
 * <b>与 DEPT_AND_CHILD 的区别：</b>
 * <ul>
 *     <li><b>DEPT：</b>只查看本部门数据，使用 {@code dept_id = ?} 过滤</li>
 *     <li><b>DEPT_AND_CHILD：</b>查看本部门及所有子部门数据，使用 {@code dept_id IN (?, ?, ?)} 过滤</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>如果 {@code context.getDeptId()} 为空，返回 null，不添加过滤条件（记录警告日志）</li>
 *     <li>部门 ID 直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据）</li>
 *     <li>如果表有别名，必须在注解中配置 {@code tableAlias}，否则 SQL 可能报错</li>
 *     <li>支持 Nacos 热配置，可动态修改默认字段名，无需重启服务</li>
 *     <li>不包含子部门数据，如需包含子部门请使用 {@code DEPT_AND_CHILD} 权限</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType#DEPT
 * @see com.zmbdp.common.datapermission.context.DataPermissionContext#getDeptId()
 * @see DeptAndChildDataPermissionStrategy
 */
@Slf4j
@Component
@RefreshScope
public class DeptDataPermissionStrategy implements DataPermissionStrategy {

    /**
     * 默认部门字段名（从 Nacos 配置中心读取）
     * <p>
     * 用于构建本部门数据权限过滤条件的字段名。<br>
     * 如果注解中未指定 {@code deptColumn}，则使用此默认值。
     * <p>
     * <b>配置项：</b>{@code datapermission.default-dept-column}，默认值：{@code dept_id}
     */
    @Value("${datapermission.default-dept-column:dept_id}")
    private String defaultDeptColumn;

    /**
     * 获取处理器支持的权限类型
     * <p>
     * 返回 {@code DataPermissionType.DEPT}，表示此处理器处理本部门数据权限。
     *
     * @return 数据权限类型枚举 {@code DEPT}
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.DEPT;
    }

    /**
     * 构建本部门数据权限过滤条件
     * <p>
     * 根据当前用户的部门 ID 构建 SQL 过滤条件，限制用户只能查看本部门的数据（不包含子部门）。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. Mapper 接口方法配置
     * @DataPermission(type = DataPermissionType.DEPT)
     * List<SysUser> selectUserList(SysUserQuery query);
     *
     * // 原始 SQL：SELECT * FROM sys_user WHERE status = '1'
     * // 改写后：SELECT * FROM sys_user WHERE (dept_id = 10) AND status = '1'
     *
     * // 2. 自定义部门字段名
     * @DataPermission(type = DataPermissionType.DEPT, deptColumn = "department_id")
     * List<SysUser> selectUserList(SysUserQuery query);
     *
     * // 改写后：SELECT * FROM sys_user WHERE (department_id = 10) AND status = '1'
     *
     * // 3. 使用表别名（JOIN 查询）
     * @DataPermission(type = DataPermissionType.DEPT, tableAlias = "u")
     * List<SysUser> selectUserList(SysUserQuery query);
     *
     * // 原始 SQL：SELECT * FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id
     * // 改写后：SELECT * FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id WHERE (u.dept_id = 10)
     * }</pre>
     * <p>
     * <b>字段名优先级：</b>注解配置 > 全局配置<br>
     * <b>表别名支持：</b>无别名 {@code dept_id = 10}，有别名 {@code d.dept_id = 10}
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果上下文中 deptId 为空，返回 null，不添加过滤条件（记录警告日志）</li>
     *     <li>部门 ID 直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据）</li>
     *     <li>如果表有别名，必须在注解中配置 {@code tableAlias}，否则 SQL 可能报错</li>
     *     <li>支持 Nacos 热配置，可动态修改默认字段名，无需重启服务</li>
     *     <li>不包含子部门数据，如需包含子部门请使用 {@code DEPT_AND_CHILD} 权限</li>
     * </ul>
     *
     * @param annotation 数据权限注解，包含部门字段名、表别名等配置，不能为 null
     * @param context    数据权限上下文，包含当前用户的部门 ID，不能为 null
     * @return SQL 过滤条件（如 {@code dept_id = 10}），如果部门 ID 为空返回 null
     * @see com.zmbdp.common.datapermission.annotation.DataPermission
     * @see com.zmbdp.common.datapermission.context.DataPermissionContext#getDeptId()
     * @see DeptAndChildDataPermissionStrategy
     */
    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        if (context == null || context.getDeptId() == null) {
            log.warn("数据权限：DEPT - 部门 ID 为空，跳过过滤");
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

        String condition = deptColumn + " = " + context.getDeptId();
        log.debug("数据权限：DEPT - 过滤条件：{}", condition);
        return condition;
    }
}