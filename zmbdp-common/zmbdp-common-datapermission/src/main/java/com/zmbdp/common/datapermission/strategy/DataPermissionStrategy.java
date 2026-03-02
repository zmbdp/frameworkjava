package com.zmbdp.common.datapermission.strategy;

import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import com.zmbdp.common.datapermission.enums.DataPermissionType;

/**
 * 数据权限策略处理器接口
 * <p>
 * 定义数据权限处理的核心方法，用于根据不同的权限类型构建 SQL 过滤条件。<br>
 * 采用策略模式，每种权限类型对应一个具体的处理器实现，实现权限过滤逻辑的解耦和扩展。
 * <p>
 * <b>设计思想：</b>
 * <ul>
 *     <li><b>策略模式：</b>每种权限类型对应一个处理器，便于扩展和维护</li>
 *     <li><b>单一职责：</b>每个处理器只负责一种权限类型的 SQL 构建</li>
 *     <li><b>开闭原则：</b>新增权限类型只需新增处理器，无需修改现有代码</li>
 *     <li><b>依赖倒置：</b>拦截器依赖接口而非具体实现，降低耦合</li>
 * </ul>
 * <p>
 * <b>实现类列表：</b>
 * <ul>
 *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.AllDataPermissionStrategy}：全部数据权限，不添加任何过滤条件</li>
 *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.SelfDataPermissionStrategy}：仅本人数据权限，添加 {@code user_id = ?} 条件</li>
 *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.DeptDataPermissionStrategy}：本部门数据权限，添加 {@code dept_id = ?} 条件</li>
 *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.DeptAndChildDataPermissionStrategy}：本部门及子部门数据权限，添加 {@code dept_id IN (?, ?, ?)} 条件</li>
 *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.CustomDataPermissionStrategy}：自定义数据权限，使用注解中配置的 SQL 条件</li>
 * </ul>
 * <p>
 * <b>使用流程：</b>
 * <ol>
 *     <li>拦截器从注解或上下文中获取权限类型（{@link com.zmbdp.common.datapermission.enums.DataPermissionType}）</li>
 *     <li>根据权限类型从策略缓存中获取对应的处理器（调用 {@code getSupportType()} 匹配）</li>
 *     <li>调用处理器的 {@code buildCondition} 方法，构建 SQL 过滤条件</li>
 *     <li>拦截器将过滤条件添加到原始 SQL 的 WHERE 子句中</li>
 * </ol>
 * <p>
 * <b>扩展示例：</b>
 * <pre>{@code
 * // 新增区域权限处理器
 * &#64;Component
 * public class RegionDataPermissionStrategy implements DataPermissionStrategy {
 *     &#64;Override
 *     public DataPermissionType getSupportType() {
 *         return DataPermissionType.REGION;
 *     }
 *
 *     &#64;Override
 *     public String buildCondition(DataPermission annotation, DataPermissionContext context) {
 *         if (context.getRegionIds() == null || context.getRegionIds().isEmpty()) {
 *             return null;
 *         }
 *         String regionColumn = annotation.regionColumn();
 *         String regionIds = context.getRegionIds().stream()
 *             .map(String::valueOf)
 *             .collect(Collectors.joining(", "));
 *         return regionColumn + " IN (" + regionIds + ")";
 *     }
 * }
 * }</pre>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.interceptor.DataPermissionInterceptor
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType
 * @see com.zmbdp.common.datapermission.annotation.DataPermission
 * @see com.zmbdp.common.datapermission.context.DataPermissionContext
 */
public interface DataPermissionStrategy {

    /**
     * 获取处理器支持的权限类型
     * <p>
     * 用于拦截器根据权限类型匹配对应的处理器。<br>
     * 每个处理器实现类必须返回唯一的权限类型，确保一对一映射关系。
     * <p>
     * <b>权限类型映射：</b>
     * <ul>
     *     <li>{@code ALL} → {@link com.zmbdp.common.datapermission.strategy.impl.AllDataPermissionStrategy}</li>
     *     <li>{@code SELF} → {@link com.zmbdp.common.datapermission.strategy.impl.SelfDataPermissionStrategy}</li>
     *     <li>{@code DEPT} → {@link com.zmbdp.common.datapermission.strategy.impl.DeptDataPermissionStrategy}</li>
     *     <li>{@code DEPT_AND_CHILD} → {@link com.zmbdp.common.datapermission.strategy.impl.DeptAndChildDataPermissionStrategy}</li>
     *     <li>{@code CUSTOM} → {@link com.zmbdp.common.datapermission.strategy.impl.CustomDataPermissionStrategy}</li>
     * </ul>
     *
     * @return 数据权限类型枚举
     */
    DataPermissionType getSupportType();

    /**
     * 构建 SQL 过滤条件
     * <p>
     * 根据当前用户的权限信息和注解配置，构建 SQL WHERE 条件。<br>
     * 拦截器会将返回的条件添加到原始 SQL 的 WHERE 子句中，实现数据权限过滤。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li><b>返回 null 或空字符串：</b>不添加任何过滤条件，直接执行原始 SQL</li>
     *     <li><b>返回 SQL 条件：</b>会通过 AND 连接到原始 SQL 的 WHERE 子句</li>
     * </ul>
     * <p>
     * <b>SQL 条件示例：</b>
     * <ul>
     *     <li><b>SELF 权限：</b>{@code user_id = 123}</li>
     *     <li><b>DEPT 权限：</b>{@code dept_id = 10}</li>
     *     <li><b>DEPT_AND_CHILD 权限：</b>{@code dept_id IN (10, 11, 12)}</li>
     *     <li><b>CUSTOM 权限：</b>{@code status = '1' AND region_id IN (1, 2, 3)}</li>
     *     <li><b>带表别名：</b>{@code u.user_id = 123}（{@code @DataPermission(tableAlias = "u")}）</li>
     * </ul>
     * <p>
     * <b>字段名优先级：</b>
     * <ol>
     *     <li>注解配置：{@code @DataPermission(userColumn = "creator_id")}</li>
     *     <li>Nacos 全局配置：{@code datapermission.default-user-column=user_id}</li>
     *     <li>代码默认值：{@code user_id}、{@code dept_id} 等</li>
     * </ol>
     * <p>
     * <b>表别名支持：</b>
     * <ul>
     *     <li>无别名：{@code user_id = 123}</li>
     *     <li>有别名：{@code u.user_id = 123}（适配 JOIN、子查询等复杂 SQL）</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>返回的条件不需要包含 WHERE 关键字，拦截器会自动添加</li>
     *     <li>如果上下文中缺少必要的字段（如 userId、deptId），应返回 null 并记录警告日志</li>
     *     <li>条件中的值直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据，注意 SQL 注入风险）</li>
     *     <li>如果需要多个条件，使用 AND 或 OR 连接，并用括号包裹（如 {@code (a = 1 OR b = 2)}）</li>
     * </ul>
     * <p>
     * <b>实现建议：</b>
     * <ul>
     *     <li>优先从注解中获取字段名配置，未配置则使用全局配置或默认值</li>
     *     <li>如果有表别名，添加别名前缀（如 {@code tableAlias + "." + column}）</li>
     *     <li>如果上下文中缺少必要字段，记录警告日志并返回 null</li>
     *     <li>使用 {@code log.debug} 记录构建的条件，便于调试</li>
     * </ul>
     *
     * @param annotation 数据权限注解，包含权限类型、字段名、表别名、自定义条件等配置
     * @param context    数据权限上下文，包含用户 ID、部门 ID、租户 ID、管理员标识等信息
     * @return SQL 过滤条件（不包含 WHERE 关键字），如果不需要过滤返回 null
     */
    String buildCondition(DataPermission annotation, DataPermissionContext context);
}