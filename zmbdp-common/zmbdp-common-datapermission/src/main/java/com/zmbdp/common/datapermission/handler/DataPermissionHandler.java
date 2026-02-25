package com.zmbdp.common.datapermission.handler;

import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;

/**
 * 数据权限处理器接口
 * <p>
 * 定义数据权限处理的核心方法，用于根据不同的权限类型构建 SQL 过滤条件。<br>
 * 采用策略模式，每种权限类型对应一个具体的处理器实现。
 * <p>
 * <b>设计思想：</b>
 * <ul>
 *     <li>策略模式：每种权限类型对应一个处理器，便于扩展和维护</li>
 *     <li>单一职责：每个处理器只负责一种权限类型的 SQL 构建</li>
 *     <li>开闭原则：新增权限类型只需新增处理器，无需修改现有代码</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
public interface DataPermissionHandler {

    /**
     * 获取处理器支持的权限类型
     *
     * @return 数据权限类型
     */
    DataPermissionType getSupportType();

    /**
     * 构建 SQL 过滤条件
     * <p>
     * 根据当前用户的权限信息和注解配置，构建 SQL WHERE 条件。
     * <p>
     * <b>返回值说明：</b>
     * <ul>
     *     <li>返回 null 或空字符串：不添加任何过滤条件</li>
     *     <li>返回 SQL 条件：会通过 AND 连接到原始 SQL 的 WHERE 子句</li>
     * </ul>
     * <p>
     * <b>示例：</b>
     * <ul>
     *     <li>SELF 权限：返回 "user_id = 123"</li>
     *     <li>DEPT 权限：返回 "dept_id = 10"</li>
     *     <li>DEPT_AND_CHILD 权限：返回 "dept_id IN (10, 11, 12)"</li>
     * </ul>
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return SQL 过滤条件，如果不需要过滤返回 null
     */
    String buildCondition(DataPermission annotation, DataPermissionContext context);
}

