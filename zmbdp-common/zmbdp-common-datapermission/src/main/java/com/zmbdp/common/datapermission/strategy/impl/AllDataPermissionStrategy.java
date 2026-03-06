package com.zmbdp.common.datapermission.strategy.impl;

import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import com.zmbdp.common.datapermission.strategy.DataPermissionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 全部数据权限策略处理器
 * <p>
 * 不添加任何过滤条件，用户可以查看所有数据，不受数据权限限制。<br>
 * 适用于超级管理员、系统管理员等需要查看全局数据的角色。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>处理 {@code DataPermissionType.ALL} 权限类型</li>
 *     <li>返回 null，不添加任何 SQL 过滤条件</li>
 *     <li>记录调试日志，便于追踪权限过滤流程</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>超级管理员：</b>需要查看所有用户、部门、数据</li>
 *     <li><b>系统管理员：</b>需要进行系统运维、数据维护</li>
 *     <li><b>数据分析：</b>需要统计全局数据，生成报表</li>
 *     <li><b>审计日志：</b>需要查看所有操作日志</li>
 * </ul>
 * <p>
 * <b>SQL 示例：</b>
 * <pre>{@code
 * // 原始 SQL
 * SELECT * FROM sys_user WHERE status = '1'
 *
 * // 执行 SQL（不添加任何过滤条件）
 * SELECT * FROM sys_user WHERE status = '1'
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>此权限类型不添加任何过滤条件，用户可以查看所有数据</li>
 *     <li>应严格控制此权限的分配，避免数据泄露</li>
 *     <li>建议结合 {@code DataPermissionContext.isAdmin} 标识使用</li>
 *     <li>如果 {@code isAdmin = true}，拦截器会直接跳过过滤，无需调用此处理器</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType#ALL
 */
@Slf4j
@Component
public class AllDataPermissionStrategy implements DataPermissionStrategy {

    /**
     * 获取处理器支持的权限类型
     * <p>
     * 返回 {@code DataPermissionType.ALL}，表示此处理器处理全部数据权限。
     *
     * @return 数据权限类型枚举 {@code ALL}
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.ALL;
    }

    /**
     * 构建全部数据权限过滤条件
     * <p>
     * 全部数据权限不添加任何过滤条件，直接返回 null。<br>
     * 拦截器会跳过 SQL 改写，执行原始 SQL，用户可以查看所有数据。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>记录调试日志，标识使用全部数据权限</li>
     *     <li>返回 null，不添加任何过滤条件</li>
     * </ol>
     *
     * @param annotation 数据权限注解（此处理器不使用注解配置）
     * @param context    数据权限上下文（此处理器不使用上下文信息）
     * @return null，不添加任何过滤条件
     */
    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        // 全部数据权限，不添加任何过滤条件
        log.debug("数据权限：ALL - 不添加过滤条件");
        return null;
    }
}