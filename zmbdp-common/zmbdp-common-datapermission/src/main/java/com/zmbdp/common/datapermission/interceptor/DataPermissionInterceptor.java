package com.zmbdp.common.datapermission.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.handler.DataPermissionContext;
import com.zmbdp.common.datapermission.handler.DataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限拦截器
 * <p>
 * 基于 MyBatis 拦截器实现数据权限过滤，拦截 SQL 执行前的准备阶段，动态改写 SQL。<br>
 * 支持多种数据权限类型，可通过注解配置，业务代码无感知。
 * <p>
 * <b>工作原理：</b>
 * <ol>
 *     <li>拦截 {@code StatementHandler.prepare} 方法</li>
 *     <li>获取 Mapper 方法上的 {@code @DataPermission} 注解</li>
 *     <li>从 {@code DataPermissionContext} 获取当前用户权限信息</li>
 *     <li>根据权限类型选择对应的处理器，构建 SQL 过滤条件</li>
 *     <li>改写原始 SQL，添加 WHERE 条件</li>
 *     <li>执行改写后的 SQL</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>只拦截 SELECT 语句，不拦截 INSERT、UPDATE、DELETE</li>
 *     <li>如果用户是超级管理员（{@code isAdmin = true}），不添加任何过滤条件</li>
 *     <li>如果 Mapper 方法没有 {@code @DataPermission} 注解，不进行过滤</li>
 *     <li>如果 {@code DataPermissionContext} 为空，不进行过滤</li>
 *     <li>支持 Nacos 热配置，可动态开启/关闭数据权限</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RefreshScope
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        )
})
public class DataPermissionInterceptor implements Interceptor {

    /**
     * 数据权限处理器缓存（权限类型 -> 处理器）
     */
    private final Map<DataPermissionType, DataPermissionHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * 数据权限处理器列表（策略模式）
     */
    @Autowired
    private List<DataPermissionHandler> handlers;

    /**
     * 是否启用数据权限（从 Nacos 配置中心读取）
     */
    @Value("${datapermission.enabled:true}")
    private Boolean enabled;

    /**
     * 默认租户字段名（从 Nacos 配置中心读取）
     */
    @Value("${datapermission.default-tenant-column:tenant_id}")
    private String defaultTenantColumn;

    /**
     * 是否启用多租户过滤（从 Nacos 配置中心读取）
     */
    @Value("${datapermission.enable-tenant:false}")
    private Boolean enableTenant;

    /**
     * 拦截 StatementHandler 的 prepare 方法，进行数据权限过滤
     *
     * @param invocation 拦截器调用链
     * @return 拦截结果
     * @throws Throwable 拦截异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否启用数据权限
        if (!enabled) {
            return invocation.proceed();
        }

        // 获取 StatementHandler
        StatementHandler statementHandler = PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 只拦截 SELECT 语句
        if (SqlCommandType.SELECT != mappedStatement.getSqlCommandType()) {
            return invocation.proceed();
        }

        // 获取 Mapper 方法上的 @DataPermission 注解
        DataPermission annotation = getDataPermissionAnnotation(mappedStatement);
        if (annotation == null) {
            // 没有注解，不进行过滤
            return invocation.proceed();
        }

        // 获取当前用户的数据权限上下文
        DataPermissionContext context = DataPermissionContext.get();
        if (context == null) {
            log.warn("数据权限：上下文为空，跳过过滤");
            return invocation.proceed();
        }

        // 如果是超级管理员，不进行过滤
        if (Boolean.TRUE.equals(context.getIsAdmin())) {
            log.debug("数据权限：超级管理员，跳过过滤");
            return invocation.proceed();
        }

        // 获取 BoundSql
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // 构建数据权限过滤条件
        String condition = buildDataPermissionCondition(annotation, context);

        // 如果没有过滤条件，直接放行
        if (StringUtil.isEmpty(condition)) {
            return invocation.proceed();
        }

        // 改写 SQL
        String newSql = rewriteSql(originalSql, condition);
        log.debug("数据权限：原始 SQL：{}", originalSql);
        log.debug("数据权限：改写 SQL：{}", newSql);

        // 更新 BoundSql 中的 SQL
        metaObject.setValue("delegate.boundSql.sql", newSql);

        return invocation.proceed();
    }

    /**
     * 创建 MyBatis 拦截器
     *
     * @param target 目标对象
     * @return 拦截器
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    /**
     * 获取 Mapper 方法上的 @DataPermission 注解
     *
     * @param mappedStatement MappedStatement
     * @return DataPermission 注解，如果没有返回 null
     */
    private DataPermission getDataPermissionAnnotation(MappedStatement mappedStatement) {
        try {
            String mapperId = mappedStatement.getId();
            String className = mapperId.substring(0, mapperId.lastIndexOf('.'));
            String methodName = mapperId.substring(mapperId.lastIndexOf('.') + 1);

            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataPermission.class)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }
        } catch (Exception e) {
            log.warn("数据权限：获取注解失败", e);
        }
        return null;
    }

    /**
     * 构建数据权限过滤条件
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return SQL 过滤条件
     */
    private String buildDataPermissionCondition(DataPermission annotation, DataPermissionContext context) {
        // 获取权限类型（优先级：注解 > 上下文 > 默认值）
        DataPermissionType permissionType = annotation.type();
        if (permissionType == null && context.getPermissionType() != null) {
            permissionType = context.getPermissionType();
        }
        if (permissionType == null) {
            permissionType = DataPermissionType.SELF; // 默认使用仅本人权限
        }

        // 获取对应的处理器
        DataPermissionHandler handler = getHandler(permissionType);
        if (handler == null) {
            log.warn("数据权限：未找到处理器，权限类型：{}", permissionType);
            return null;
        }

        // 构建数据权限条件
        String dataPermissionCondition = handler.buildCondition(annotation, context);

        // 构建多租户条件
        String tenantCondition = buildTenantCondition(annotation, context);

        // 合并条件
        if (StringUtil.isNotEmpty(dataPermissionCondition) && StringUtil.isNotEmpty(tenantCondition)) {
            return dataPermissionCondition + " AND " + tenantCondition;
        } else if (StringUtil.isNotEmpty(dataPermissionCondition)) {
            return dataPermissionCondition;
        } else {
            return tenantCondition;
        }
    }

    /**
     * 构建多租户过滤条件
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return 多租户过滤条件
     */
    private String buildTenantCondition(DataPermission annotation, DataPermissionContext context) {
        // 检查是否启用多租户过滤（优先级：注解 > 全局配置）
        boolean enableTenantFilter = annotation.enableTenant() || (enableTenant != null && enableTenant);
        if (!enableTenantFilter) {
            return null;
        }

        // 检查租户 ID 是否存在
        if (context.getTenantId() == null) {
            log.warn("数据权限：多租户 - 租户 ID 为空，跳过过滤");
            return null;
        }

        // 获取租户字段名（优先级：注解 > 全局配置）
        String tenantColumn = StringUtil.isNotEmpty(annotation.tenantColumn())
                ? annotation.tenantColumn()
                : defaultTenantColumn;

        // 如果有表别名，添加别名前缀
        if (StringUtil.isNotEmpty(annotation.tableAlias())) {
            tenantColumn = annotation.tableAlias() + "." + tenantColumn;
        }

        String condition = tenantColumn + " = " + context.getTenantId();
        log.debug("数据权限：多租户 - 过滤条件：{}", condition);
        return condition;
    }

    /**
     * 改写 SQL，添加 WHERE 条件
     * <p>
     * 根据原始 SQL 是否包含 WHERE 子句，采用不同的改写策略：
     * <ul>
     *     <li>已有 WHERE：在 WHERE 后添加条件，使用 AND 连接</li>
     *     <li>无 WHERE：在 ORDER BY、GROUP BY、LIMIT 等关键字前添加 WHERE 子句</li>
     * </ul>
     *
     * @param originalSql 原始 SQL
     * @param condition   过滤条件
     * @return 改写后的 SQL
     */
    private String rewriteSql(String originalSql, String condition) {
        String lowerSql = originalSql.toLowerCase();

        if (lowerSql.contains("where")) {
            // 已有 WHERE 子句，使用 AND 连接
            int whereIndex = lowerSql.indexOf("where");
            int insertIndex = whereIndex + 5; // "where" 长度为 5

            return originalSql.substring(0, insertIndex) + " (" + condition + ") AND " + originalSql.substring(insertIndex);
        } else {
            // 没有 WHERE 子句，添加 WHERE
            // 查找 ORDER BY、GROUP BY、LIMIT 等关键字的位置
            int orderByIndex = lowerSql.indexOf("order by");
            int groupByIndex = lowerSql.indexOf("group by");
            int limitIndex = lowerSql.indexOf("limit");

            // 找到最早出现的关键字位置
            int insertIndex = originalSql.length();
            if (orderByIndex > 0 && orderByIndex < insertIndex) {
                insertIndex = orderByIndex;
            }
            if (groupByIndex > 0 && groupByIndex < insertIndex) {
                insertIndex = groupByIndex;
            }
            if (limitIndex > 0 && limitIndex < insertIndex) {
                insertIndex = limitIndex;
            }

            return originalSql.substring(0, insertIndex) + " WHERE " + condition + " " + originalSql.substring(insertIndex);
        }
    }

    /**
     * 获取数据权限处理器
     *
     * @param permissionType 权限类型
     * @return 数据权限处理器
     */
    private DataPermissionHandler getHandler(DataPermissionType permissionType) {
        // 先从缓存获取，如果没有就再从处理器里面获取
        DataPermissionHandler handler = handlerMap.get(permissionType);
        if (handler != null) {
            return handler;
        }
        // 从处理器列表中查找
        for (DataPermissionHandler h : handlers) {
            if (h.getSupportType() == permissionType) {
                handlerMap.put(permissionType, h);
                return h;
            }
        }
        return null;
    }
}