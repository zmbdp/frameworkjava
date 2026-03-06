package com.zmbdp.common.datapermission.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.strategy.DataPermissionStrategy;
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
 * 基于 MyBatis 拦截器机制实现数据权限过滤，在 SQL 执行前动态改写 SQL，添加数据权限过滤条件。<br>
 * 采用策略模式支持多种权限类型（全部、仅本人、本部门、本部门及子部门、自定义），通过注解配置，业务代码无感知。<br>
 * 支持多租户隔离、表别名、Nacos 热配置等高级特性。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>拦截 MyBatis 的 {@code StatementHandler.prepare} 方法，在 SQL 执行前介入</li>
 *     <li>解析 Mapper 方法上的 {@code @DataPermission} 注解，获取权限配置</li>
 *     <li>从 {@code DataPermissionContext} 获取当前用户的权限信息（用户 ID、部门 ID 等）</li>
 *     <li>根据权限类型路由到对应的策略处理器，构建 SQL 过滤条件</li>
 *     <li>改写原始 SQL，将过滤条件添加到 WHERE 子句中</li>
 *     <li>支持多租户过滤，自动添加租户 ID 条件</li>
 * </ul>
 * <p>
 * <b>支持的权限类型：</b>
 * <ul>
 *     <li><b>ALL：</b>全部数据权限，不添加任何过滤条件（超级管理员）</li>
 *     <li><b>SELF：</b>仅本人数据，添加 {@code user_id = ?} 条件</li>
 *     <li><b>DEPT：</b>本部门数据，添加 {@code dept_id = ?} 条件</li>
 *     <li><b>DEPT_AND_CHILD：</b>本部门及子部门数据，添加 {@code dept_id IN (?, ?, ?)} 条件</li>
 *     <li><b>CUSTOM：</b>自定义过滤条件，使用注解中配置的 SQL 条件</li>
 * </ul>
 * <p>
 * <b>SQL 改写示例：</b>
 * <pre>{@code
 * // 原始 SQL
 * SELECT * FROM sys_user ORDER BY create_time DESC
 *
 * // 改写后（DEPT_AND_CHILD 权限）
 * SELECT * FROM sys_user WHERE (dept_id IN (10, 11, 12)) ORDER BY create_time DESC
 *
 * // 改写后（SELF 权限 + 多租户）
 * SELECT * FROM sys_user WHERE (user_id = 123) AND (tenant_id = 1) ORDER BY create_time DESC
 *
 * // 原始 SQL（已有 WHERE）
 * SELECT * FROM sys_user WHERE status = '1' ORDER BY create_time DESC
 *
 * // 改写后（DEPT 权限）
 * SELECT * FROM sys_user WHERE (dept_id = 10) AND status = '1' ORDER BY create_time DESC
 * }</pre>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // Mapper 接口方法
 * &#64;DataPermission(type = DataPermissionType.DEPT_AND_CHILD, deptColumn = "dept_id")
 * List&lt;SysUser&gt; selectUserList(SysUserQuery query);
 *
 * // 业务代码（无需修改）
 * List&lt;SysUser&gt; users = userMapper.selectUserList(query);
 * // 拦截器会自动添加数据权限过滤条件
 * }</pre>
 * <p>
 * <b>配置优先级：</b>
 * <ol>
 *     <li>注解参数（type、userColumn、deptColumn、tableAlias 等）</li>
 *     <li>上下文配置（DataPermissionContext.permissionType）</li>
 *     <li>Nacos 全局配置（datapermission.default-*）</li>
 *     <li>代码默认值（SELF 权限）</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>只拦截 SELECT 语句，不拦截 INSERT、UPDATE、DELETE（避免影响数据写入）</li>
 *     <li>如果用户是超级管理员（{@code isAdmin = true}），不添加任何过滤条件</li>
 *     <li>如果 Mapper 方法没有 {@code @DataPermission} 注解，不进行过滤</li>
 *     <li>如果 {@code DataPermissionContext} 为空，不进行过滤（兼容未登录场景）</li>
 *     <li>支持 Nacos 热配置，可通过 {@code datapermission.enabled} 动态开启/关闭</li>
 *     <li>使用 {@code @RefreshScope} 支持配置热更新，无需重启服务</li>
 *     <li>策略处理器使用 ConcurrentHashMap 缓存，避免重复查找，提升性能</li>
 *     <li>多租户过滤条件与数据权限条件通过 AND 连接，同时生效</li>
 *     <li>支持表别名（tableAlias），适配复杂 SQL（JOIN、子查询等）</li>
 *     <li>SQL 改写使用字符串拼接，不支持预编译参数，注意 SQL 注入风险（仅限内部权限数据）</li>
 * </ul>
 * <p>
 * <b>性能优化：</b>
 * <ul>
 *     <li>策略处理器缓存：首次查找后缓存到 Map，避免重复遍历</li>
 *     <li>注解解析缓存：可考虑使用 Caffeine 缓存注解解析结果（当前未实现）</li>
 *     <li>上下文检查前置：先检查开关、上下文、管理员，快速放行</li>
 *     <li>SQL 类型检查：只拦截 SELECT，其他类型直接放行</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.annotation.DataPermission
 * @see com.zmbdp.common.datapermission.context.DataPermissionContext
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType
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
     * 数据权限策略处理器缓存（权限类型 -> 处理器）
     * <p>
     * 使用 ConcurrentHashMap 缓存权限类型与处理器的映射关系，避免每次都遍历 strategies 列表。<br>
     * 首次查找时从 strategies 列表中匹配，找到后缓存到 Map 中，后续直接从 Map 获取。
     */
    private final Map<DataPermissionType, DataPermissionStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 数据权限策略处理器列表（策略模式）
     * <p>
     * Spring 自动注入所有 {@link DataPermissionStrategy} 接口的实现类，包括：
     * <ul>
     *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.AllDataPermissionStrategy}：全部数据权限</li>
     *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.SelfDataPermissionStrategy}：仅本人数据权限</li>
     *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.DeptDataPermissionStrategy}：本部门数据权限</li>
     *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.DeptAndChildDataPermissionStrategy}：本部门及子部门数据权限</li>
     *     <li>{@link com.zmbdp.common.datapermission.strategy.impl.CustomDataPermissionStrategy}：自定义数据权限</li>
     * </ul>
     * <p>
     * 通过 {@code getSupportType()} 方法匹配权限类型，找到对应的处理器。
     */
    @Autowired
    private List<DataPermissionStrategy> strategies;

    /**
     * 是否启用数据权限（从 Nacos 配置中心读取）
     * <p>
     * 全局开关，控制数据权限功能是否生效。<br>
     * 关闭后，所有数据权限过滤都会跳过，直接执行原始 SQL。
     * <p>
     * <b>配置项：</b>{@code datapermission.enabled}，默认值：{@code true}
     */
    @Value("${datapermission.enabled:true}")
    private Boolean enabled;

    /**
     * 默认租户字段名（从 Nacos 配置中心读取）
     * <p>
     * 多租户场景下，用于构建租户过滤条件的字段名。<br>
     * 如果注解中未指定 {@code tenantColumn}，则使用此默认值。
     * <p>
     * <b>配置项：</b>{@code datapermission.default-tenant-column}，默认值：{@code tenant_id}
     */
    @Value("${datapermission.default-tenant-column:tenant_id}")
    private String defaultTenantColumn;

    /**
     * 是否启用多租户过滤（从 Nacos 配置中心读取）
     * <p>
     * 全局开关，控制是否自动添加租户过滤条件。<br>
     * 开启后，所有 SQL 都会自动添加 {@code tenant_id = ?} 条件（除非注解中明确禁用）。
     * <p>
     * <b>配置项：</b>{@code datapermission.enable-tenant}，默认值：{@code false}<br>
     * <b>优先级：</b>注解配置 {@code enableTenant} > 全局配置
     */
    @Value("${datapermission.enable-tenant:false}")
    private Boolean enableTenant;

    /**
     * 拦截 StatementHandler 的 prepare 方法，进行数据权限过滤
     * <p>
     * 这是拦截器的核心方法，在 SQL 执行前介入，动态改写 SQL，添加数据权限过滤条件。<br>
     * 采用多层检查机制，快速放行不需要过滤的场景，提升性能。
     * <p>
     * <b>快速放行场景：</b>全局开关关闭、上下文为空、超级管理员、非 SELECT 语句、无注解、无过滤条件
     *
     * @param invocation 拦截器调用链，包含目标对象、方法、参数等信息
     * @return 拦截结果，通常是 {@code invocation.proceed()} 的返回值
     * @throws Throwable 拦截异常或 SQL 执行异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否启用数据权限
        if (!enabled) {
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
     * 创建 MyBatis 拦截器代理对象
     * <p>
     * MyBatis 插件机制要求实现此方法，用于创建目标对象的代理。<br>
     * 只对 {@code StatementHandler} 类型的对象创建代理，其他类型直接返回原对象。
     *
     * @param target 目标对象（可能是 Executor、StatementHandler、ParameterHandler、ResultSetHandler）
     * @return 代理对象或原对象
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
     * <p>
     * 通过反射从 {@code MappedStatement} 中提取 Mapper 接口的类名和方法名，<br>
     * 然后加载类并遍历方法，查找标注了 {@code @DataPermission} 注解的方法。
     * <p>
     * <b>解析流程：</b>
     * <ol>
     *     <li>从 {@code MappedStatement.getId()} 获取完整方法标识（如 {@code com.example.UserMapper.selectList}）</li>
     *     <li>提取类名（{@code com.example.UserMapper}）和方法名（{@code selectList}）</li>
     *     <li>通过 {@code Class.forName} 加载 Mapper 接口类</li>
     *     <li>遍历类的所有方法，匹配方法名</li>
     *     <li>检查方法是否标注了 {@code @DataPermission} 注解</li>
     *     <li>返回注解对象，如果不存在返回 null</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>只匹配方法名，不匹配参数类型（MyBatis 不支持方法重载）</li>
     *     <li>如果类加载失败或反射异常，返回 null，不影响 SQL 执行</li>
     *     <li>可考虑使用 Caffeine 缓存注解解析结果，提升性能（当前未实现）</li>
     * </ul>
     *
     * @param mappedStatement MyBatis 映射语句对象，包含 SQL、参数映射、结果映射等信息
     * @return DataPermission 注解对象，如果不存在返回 null
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
     * <p>
     * 根据注解配置和上下文信息，构建完整的 SQL 过滤条件（数据权限 + 多租户）。<br>
     * 采用策略模式，根据权限类型路由到对应的处理器，构建具体的过滤条件。
     * <p>
     * <b>权限类型优先级：</b>注解配置 > 上下文配置 > 默认值（SELF）<br>
     * <b>条件合并规则：</b>数据权限条件与租户条件通过 AND 连接
     *
     * @param annotation 数据权限注解，包含权限类型、字段名、表别名等配置
     * @param context    数据权限上下文，包含用户 ID、部门 ID、租户 ID 等信息
     * @return SQL 过滤条件，如果不需要过滤返回 null
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
        DataPermissionStrategy strategy = getStrategy(permissionType);
        if (strategy == null) {
            log.warn("数据权限：未找到处理器，权限类型：{}", permissionType);
            return null;
        }

        // 构建数据权限条件
        String dataPermissionCondition = strategy.buildCondition(annotation, context);

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
     * <p>
     * 根据注解配置和全局配置，决定是否添加租户过滤条件。<br>
     * 租户过滤条件会与数据权限条件通过 AND 连接，实现数据隔离。
     * <p>
     * <b>启用条件：</b>注解配置 {@code enableTenant} 或全局配置 {@code datapermission.enable-tenant=true}<br>
     * <b>字段名优先级：</b>注解配置 > 全局配置
     *
     * @param annotation 数据权限注解，包含租户字段名、表别名等配置
     * @param context    数据权限上下文，包含租户 ID
     * @return 多租户过滤条件，如果不需要过滤返回 null
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
            int insertIndex = findWhereInsertIndex(originalSql, lowerSql);

            return originalSql.substring(0, insertIndex) + " WHERE " + condition + " " + originalSql.substring(insertIndex);
        }
    }

    /**
     * 查找 SQL 适合插入 WHERE 子句的位置
     * <p>
     * 本方法用于判断在原始 SQL 语句中应当插入 WHERE 子句的位置。如果 SQL 中存在
     * <b>ORDER BY</b>、<b>GROUP BY</b>、<b>LIMIT</b> 等关键字，则应当插入到这些关键字之前；
     * 若不存在，则插入到末尾。
     * <p>
     * <b>实现细节：</b>
     * <ul>
     *     <li>识别受支持的 SQL 关键字，包括 "order by", "group by", "limit"（可扩展）</li>
     *     <li>遍历关键字，查找最靠前且大于0的位置，作为 WHERE 的插入点</li>
     *     <li>如果 SQL 语句均不包含这些关键字，则返回 SQL 末尾（即 length）</li>
     * </ul>
     * <p>
     * <b>举例说明：</b>
     * <pre>
     *     原始 SQL:  select * from sys_user order by id desc
     *     返回索引:   "order by" 的起始位置
     *
     *     原始 SQL:  select * from sys_user group by dept_id order by id desc
     *     返回索引:   "group by" 的起始位置（比 "order by" 更靠前）
     *
     *     原始 SQL:  select * from sys_user
     *     返回索引:   SQL 末尾
     * </pre>
     *
     * @param originalSql 原始 SQL 语句
     * @param lowerSql    SQL 语句的小写版本（提升效率，避免多次 toLowerCase）
     * @return 可以插入 WHERE 子句的合适索引位置
     */
    private int findWhereInsertIndex(String originalSql, String lowerSql) {
        // 支持的关键字列表，可扩展
        String[] keywords = {"order by", "group by", "limit"};

        int insertIndex = originalSql.length();
        for (String keyword : keywords) {
            int idx = lowerSql.indexOf(keyword);
            if (idx > 0 && idx < insertIndex) {
                insertIndex = idx;
            }
        }
        return insertIndex;
    }

    /**
     * 获取数据权限策略处理器
     * <p>
     * 根据权限类型从缓存或处理器列表中获取对应的策略处理器。<br>
     * 首次查找时会遍历 strategies 列表并缓存结果，后续直接从缓存获取，提升性能。
     * <p>
     * <b>查找流程：</b>
     * <ol>
     *     <li>先从缓存（strategyMap）中查找，命中则直接返回</li>
     *     <li>缓存未命中，遍历 strategies 列表，调用 {@code getSupportType()} 匹配权限类型</li>
     *     <li>找到后缓存到 strategyMap，下次直接从缓存获取</li>
     *     <li>未找到返回 null</li>
     * </ol>
     * <p>
     * <b>性能优化：</b>
     * <ul>
     *     <li>首次查找：O(n) 遍历 strategies 列表</li>
     *     <li>后续查找：O(1) 直接从 Map 获取</li>
     *     <li>ConcurrentHashMap 保证并发安全</li>
     * </ul>
     * <p>
     * <b>支持的权限类型：</b>
     * <ul>
     *     <li>{@code ALL}：{@link com.zmbdp.common.datapermission.strategy.impl.AllDataPermissionStrategy}</li>
     *     <li>{@code SELF}：{@link com.zmbdp.common.datapermission.strategy.impl.SelfDataPermissionStrategy}</li>
     *     <li>{@code DEPT}：{@link com.zmbdp.common.datapermission.strategy.impl.DeptDataPermissionStrategy}</li>
     *     <li>{@code DEPT_AND_CHILD}：{@link com.zmbdp.common.datapermission.strategy.impl.DeptAndChildDataPermissionStrategy}</li>
     *     <li>{@code CUSTOM}：{@link com.zmbdp.common.datapermission.strategy.impl.CustomDataPermissionStrategy}</li>
     * </ul>
     *
     * @param permissionType 数据权限类型枚举
     * @return 数据权限策略处理器，如果未找到返回 null
     */
    private DataPermissionStrategy getStrategy(DataPermissionType permissionType) {
        // 先从缓存获取，如果没有就再从处理器里面获取
        DataPermissionStrategy strategy = strategyMap.get(permissionType);
        if (strategy != null) {
            return strategy;
        }
        // 从处理器列表中查找
        for (DataPermissionStrategy h : strategies) {
            if (h.getSupportType() == permissionType) {
                strategyMap.put(permissionType, h);
                return h;
            }
        }
        return null;
    }
}