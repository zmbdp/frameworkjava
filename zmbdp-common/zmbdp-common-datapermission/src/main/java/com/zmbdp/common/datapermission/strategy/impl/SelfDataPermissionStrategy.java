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

/**
 * 仅本人数据权限策略处理器
 * <p>
 * 只能查看自己创建的数据，通过用户 ID 字段进行过滤，实现最严格的数据权限控制。<br>
 * 适用于普通员工等需要严格数据隔离的角色。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>处理 {@code DataPermissionType.SELF} 权限类型</li>
 *     <li>从上下文中获取当前用户 ID（{@code context.getUserId()}）</li>
 *     <li>从注解或全局配置中获取用户字段名（默认 {@code user_id}）</li>
 *     <li>构建 SQL 过滤条件：{@code user_id = ?}</li>
 *     <li>支持表别名，适配复杂 SQL（JOIN、子查询等）</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>普通员工：</b>只能查看自己创建的工单、订单、报销单等</li>
 *     <li><b>个人中心：</b>查看自己的个人信息、操作日志</li>
 *     <li><b>数据隔离：</b>严格限制用户只能访问自己的数据</li>
 *     <li><b>隐私保护：</b>防止用户查看其他用户的敏感数据</li>
 * </ul>
 * <p>
 * <b>SQL 示例：</b>
 * <pre>{@code
 * // 原始 SQL
 * SELECT * FROM sys_order WHERE status = '1'
 *
 * // 改写后（无表别名）
 * SELECT * FROM sys_order WHERE (user_id = 123) AND status = '1'
 *
 * // 改写后（有表别名）
 * SELECT * FROM sys_order o WHERE (o.user_id = 123) AND o.status = '1'
 * }</pre>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * // 使用默认字段名（user_id）
 * DataPermission(type = DataPermissionType.SELF);
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 *
 * // 自定义字段名（creator_id）
 * DataPermission(type = DataPermissionType.SELF, userColumn = "creator_id");
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 *
 * // 使用表别名（适配 JOIN 查询）
 * DataPermission(type = DataPermissionType.SELF, tableAlias = "o");
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 * }</pre>
 * <p>
 * <b>字段名优先级：</b>
 * <ol>
 *     <li>注解配置：{@code @DataPermission(userColumn = "creator_id")}</li>
 *     <li>Nacos 全局配置：{@code datapermission.default-user-column=user_id}</li>
 *     <li>代码默认值：{@code user_id}</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>如果 {@code context.getUserId()} 为空，返回 null，不添加过滤条件（记录警告日志）</li>
 *     <li>用户 ID 直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据）</li>
 *     <li>如果表有别名，必须在注解中配置 {@code tableAlias}，否则 SQL 可能报错</li>
 *     <li>支持 Nacos 热配置，可动态修改默认字段名，无需重启服务</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType#SELF
 * @see com.zmbdp.common.datapermission.context.DataPermissionContext#getUserId()
 */
@Slf4j
@Component
@RefreshScope
public class SelfDataPermissionStrategy implements DataPermissionStrategy {

    /**
     * 默认用户字段名（从 Nacos 配置中心读取）
     * <p>
     * 用于构建仅本人数据权限过滤条件的字段名。<br>
     * 如果注解中未指定 {@code userColumn}，则使用此默认值。
     * <p>
     * <b>配置项：</b>{@code datapermission.default-user-column}，默认值：{@code user_id}
     */
    @Value("${datapermission.default-user-column:user_id}")
    private String defaultUserColumn;

    /**
     * 获取处理器支持的权限类型
     *
     * @return 数据权限类型枚举 {@code SELF}
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.SELF;
    }

    /**
     * 构建仅本人数据权限过滤条件
     * <p>
     * 根据当前用户 ID 构建 SQL 过滤条件，限制用户只能查看自己创建的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. Mapper 接口方法配置
     * @DataPermission(type = DataPermissionType.SELF)
     * List<SysOrder> selectOrderList(SysOrderQuery query);
     * 
     * // 原始 SQL：SELECT * FROM sys_order WHERE status = '1'
     * // 改写后：SELECT * FROM sys_order WHERE (user_id = 123) AND status = '1'
     *
     * // 2. 自定义用户字段名
     * @DataPermission(type = DataPermissionType.SELF, userColumn = "creator_id")
     * List<SysOrder> selectOrderList(SysOrderQuery query);
     * 
     * // 改写后：SELECT * FROM sys_order WHERE (creator_id = 123) AND status = '1'
     *
     * // 3. 使用表别名（JOIN 查询）
     * @DataPermission(type = DataPermissionType.SELF, tableAlias = "o")
     * List<SysOrder> selectOrderList(SysOrderQuery query);
     * 
     * // 原始 SQL：SELECT * FROM sys_order o LEFT JOIN sys_user u ON o.user_id = u.id
     * // 改写后：SELECT * FROM sys_order o LEFT JOIN sys_user u ON o.user_id = u.id WHERE (o.user_id = 123)
     * }</pre>
     * <p>
     * <b>字段名优先级：</b>注解配置 > 全局配置<br>
     * <b>表别名支持：</b>无别名 {@code user_id = 123}，有别名 {@code u.user_id = 123}
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果上下文中 userId 为空，返回 null，不添加过滤条件（记录警告日志）</li>
     *     <li>用户 ID 直接拼接到 SQL 中，不使用预编译参数（仅限内部权限数据）</li>
     *     <li>如果表有别名，必须在注解中配置 {@code tableAlias}，否则 SQL 可能报错</li>
     *     <li>支持 Nacos 热配置，可动态修改默认字段名，无需重启服务</li>
     * </ul>
     *
     * @param annotation 数据权限注解，包含用户字段名、表别名等配置，不能为 null
     * @param context    数据权限上下文，包含当前用户 ID，不能为 null
     * @return SQL 过滤条件（如 {@code user_id = 123}），如果用户 ID 为空返回 null
     * @see com.zmbdp.common.datapermission.annotation.DataPermission
     * @see com.zmbdp.common.datapermission.context.DataPermissionContext#getUserId()
     */
    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        if (context == null || context.getUserId() == null) {
            log.warn("数据权限：SELF - 用户 ID 为空，跳过过滤");
            return null;
        }

        // 获取用户字段名（优先级：注解 > 全局配置）
        String userColumn = StringUtil.isNotEmpty(annotation.userColumn())
                ? annotation.userColumn()
                : defaultUserColumn;

        // 如果有表别名，添加别名前缀
        if (StringUtil.isNotEmpty(annotation.tableAlias())) {
            userColumn = annotation.tableAlias() + "." + userColumn;
        }

        String condition = userColumn + " = " + context.getUserId();
        log.debug("数据权限：SELF - 过滤条件：{}", condition);
        return condition;
    }
}