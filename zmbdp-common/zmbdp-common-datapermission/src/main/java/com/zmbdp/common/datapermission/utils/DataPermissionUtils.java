package com.zmbdp.common.datapermission.utils;

import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据权限工具类
 * <p>
 * 提供数据权限上下文管理的工具方法。<br>
 * 主要用于设置、获取、清理数据权限上下文，以及执行带权限控制的业务逻辑。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>上下文管理</b>：设置、获取、清理数据权限上下文</li>
 *     <li><b>快捷设置</b>：提供多种快捷方法设置常用权限类型</li>
 *     <li><b>自动清理</b>：支持自动清理上下文，避免内存泄漏</li>
 *     <li><b>Builder 模式</b>：支持链式调用，灵活配置权限信息</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 查询本人数据
 * DataPermissionUtils.setSelf(userId);
 * List<Order> orders = orderMapper.selectList();
 * DataPermissionUtils.clear();
 *
 * // 2. 查询本部门数据
 * DataPermissionUtils.setDept(userId, deptId);
 * List<Order> orders = orderMapper.selectList();
 * DataPermissionUtils.clear();
 *
 * // 3. 查询本部门及子部门数据
 * DataPermissionUtils.setDeptAndChild(userId, deptId, deptIds);
 * List<Order> orders = orderMapper.selectList();
 * DataPermissionUtils.clear();
 *
 * // 4. 自动清理上下文（推荐）
 * List<Order> orders = DataPermissionUtils.executeWithSelf(userId, () -> {
 *     return orderMapper.selectList();
 * });
 *
 * // 5. 自定义权限配置
 * DataPermissionUtils.builder()
 *     .userId(userId)
 *     .deptId(deptId)
 *     .tenantId(tenantId)
 *     .permissionType(DataPermissionType.DEPT)
 *     .build();
 * List<Order> orders = orderMapper.selectList();
 * DataPermissionUtils.clear();
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>使用完毕后必须调用 {@link #clear()} 清理上下文，避免内存泄漏</li>
 *     <li>推荐使用 {@code executeWith*} 系列方法，自动清理上下文</li>
 *     <li>上下文存储在 ThreadLocal 中，线程安全</li>
 *     <li>超级管理员（{@code isAdmin = true}）会跳过数据权限过滤</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see DataPermissionContext
 * @see DataPermissionType
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataPermissionUtils {

    /*=============================================    上下文管理    =============================================*/

    /**
     * 设置数据权限上下文
     * <p>
     * 设置当前线程的数据权限上下文，用于后续的数据权限过滤。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * DataPermissionContext context = new DataPermissionContext();
     * context.setUserId(userId);
     * context.setDeptId(deptId);
     * context.setPermissionType(DataPermissionType.DEPT);
     * DataPermissionUtils.set(context);
     *
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用完毕后必须调用 {@link #clear()} 清理上下文</li>
     *     <li>建议使用 try-finally 确保上下文被清理</li>
     *     <li>推荐使用 {@link #builder()} 构建上下文</li>
     * </ul>
     *
     * @param context 数据权限上下文，不能为 null
     * @see #get()
     * @see #clear()
     * @see #builder()
     */
    public static void set(DataPermissionContext context) {
        DataPermissionContext.set(context);
    }

    /**
     * 获取当前线程的数据权限上下文
     * <p>
     * 获取当前线程的数据权限上下文，用于读取权限信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * DataPermissionContext context = DataPermissionUtils.get();
     * if (context != null) {
     *     Long userId = context.getUserId();
     *     DataPermissionType type = context.getPermissionType();
     * }
     * }</pre>
     *
     * @return 数据权限上下文，如果未设置返回 null
     * @see #set(DataPermissionContext)
     */
    public static DataPermissionContext get() {
        return DataPermissionContext.get();
    }

    /**
     * 清理当前线程的数据权限上下文
     * <p>
     * 清理当前线程的数据权限上下文，避免内存泄漏。<br>
     * <b>使用完数据权限后必须调用此方法！</b>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * try {
     *     DataPermissionUtils.setSelf(userId);
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear(); // 必须清理
     * }
     * }</pre>
     *
     * @see #set(DataPermissionContext)
     */
    public static void clear() {
        DataPermissionContext.clear();
    }

    /*=============================================    快捷设置方法    =============================================*/

    /**
     * 设置"仅本人数据"权限
     * <p>
     * 设置数据权限为 {@link DataPermissionType#SELF}，只能查询当前用户的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询当前用户的订单
     * DataPermissionUtils.setSelf(userId);
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>SQL 示例：</b>
     * <pre>{@code
     * -- 原始 SQL
     * SELECT * FROM t_order WHERE status = '1'
     *
     * -- 添加权限过滤后
     * SELECT * FROM t_order WHERE status = '1' AND user_id = 4
     * }</pre>
     *
     * @param userId 当前用户 ID，不能为 null
     * @see DataPermissionType#SELF
     */
    public static void setSelf(Long userId) {
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(userId);
        context.setPermissionType(DataPermissionType.SELF);
        DataPermissionContext.set(context);
    }

    /**
     * 设置"本部门数据"权限
     * <p>
     * 设置数据权限为 {@link DataPermissionType#DEPT}，只能查询当前部门的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询本部门的订单
     * DataPermissionUtils.setDept(userId, deptId);
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>SQL 示例：</b>
     * <pre>{@code
     * -- 原始 SQL
     * SELECT * FROM t_order WHERE status = '1'
     *
     * -- 添加权限过滤后
     * SELECT * FROM t_order WHERE status = '1' AND dept_id = 5
     * }</pre>
     *
     * @param userId 当前用户 ID，不能为 null
     * @param deptId 当前部门 ID，不能为 null
     * @see DataPermissionType#DEPT
     */
    public static void setDept(Long userId, Long deptId) {
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(userId);
        context.setDeptId(deptId);
        context.setPermissionType(DataPermissionType.DEPT);
        DataPermissionContext.set(context);
    }

    /**
     * 设置"本部门及子部门数据"权限
     * <p>
     * 设置数据权限为 {@link DataPermissionType#DEPT_AND_CHILD}，可以查询当前部门及子部门的数据。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询本部门及子部门的订单
     * List<Long> deptIds = Arrays.asList(5L, 6L, 7L); // 当前部门 + 子部门
     * DataPermissionUtils.setDeptAndChild(userId, deptId, deptIds);
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>SQL 示例：</b>
     * <pre>{@code
     * -- 原始 SQL
     * SELECT * FROM t_order WHERE status = '1'
     *
     * -- 添加权限过滤后
     * SELECT * FROM t_order WHERE status = '1' AND dept_id IN (5, 6, 7)
     * }</pre>
     *
     * @param userId  当前用户 ID，不能为 null
     * @param deptId  当前部门 ID，不能为 null
     * @param deptIds 当前部门及子部门 ID 列表，不能为 null 或空
     * @see DataPermissionType#DEPT_AND_CHILD
     */
    public static void setDeptAndChild(Long userId, Long deptId, List<Long> deptIds) {
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(userId);
        context.setDeptId(deptId);
        context.setDeptIds(deptIds);
        context.setPermissionType(DataPermissionType.DEPT_AND_CHILD);
        DataPermissionContext.set(context);
    }

    /**
     * 设置"全部数据"权限（超级管理员）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#ALL}，可以查询所有数据。<br>
     * 通常用于超级管理员角色。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 超级管理员查询所有订单
     * DataPermissionUtils.setAll(userId);
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>SQL 示例：</b>
     * <pre>{@code
     * -- 原始 SQL
     * SELECT * FROM t_order WHERE status = '1'
     *
     * -- 不添加权限过滤（保持原样）
     * SELECT * FROM t_order WHERE status = '1'
     * }</pre>
     *
     * @param userId 当前用户 ID，不能为 null
     * @see DataPermissionType#ALL
     */
    public static void setAll(Long userId) {
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(userId);
        context.setPermissionType(DataPermissionType.ALL);
        context.setIsAdmin(true);
        DataPermissionContext.set(context);
    }

    /**
     * 设置"本人数据"权限（带租户）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#SELF}，并启用多租户过滤。<br>
     * 适用于 SaaS 系统，确保租户数据隔离。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询当前用户在当前租户下的订单
     * DataPermissionUtils.setSelfWithTenant(userId, tenantId);
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>SQL 示例：</b>
     * <pre>{@code
     * -- 原始 SQL
     * SELECT * FROM t_order WHERE status = '1'
     *
     * -- 添加权限过滤后
     * SELECT * FROM t_order WHERE status = '1' AND user_id = 4 AND tenant_id = 1
     * }</pre>
     *
     * @param userId   当前用户 ID，不能为 null
     * @param tenantId 当前租户 ID，不能为 null
     * @see DataPermissionType#SELF
     */
    public static void setSelfWithTenant(Long userId, Long tenantId) {
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(userId);
        context.setTenantId(tenantId);
        context.setPermissionType(DataPermissionType.SELF);
        DataPermissionContext.set(context);
    }

    /**
     * 设置"本部门数据"权限（带租户）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#DEPT}，并启用多租户过滤。<br>
     * 适用于 SaaS 系统，确保租户数据隔离。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询本部门在当前租户下的订单
     * DataPermissionUtils.setDeptWithTenant(userId, deptId, tenantId);
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     * <p>
     * <b>SQL 示例：</b>
     * <pre>{@code
     * -- 原始 SQL
     * SELECT * FROM t_order WHERE status = '1'
     *
     * -- 添加权限过滤后
     * SELECT * FROM t_order WHERE status = '1' AND dept_id = 5 AND tenant_id = 1
     * }</pre>
     *
     * @param userId   当前用户 ID，不能为 null
     * @param deptId   当前部门 ID，不能为 null
     * @param tenantId 当前租户 ID，不能为 null
     * @see DataPermissionType#DEPT
     */
    public static void setDeptWithTenant(Long userId, Long deptId, Long tenantId) {
        DataPermissionContext context = new DataPermissionContext();
        context.setUserId(userId);
        context.setDeptId(deptId);
        context.setTenantId(tenantId);
        context.setPermissionType(DataPermissionType.DEPT);
        DataPermissionContext.set(context);
    }

    /*=============================================    自动清理方法    =============================================*/

    /**
     * 执行带"仅本人数据"权限的业务逻辑（自动清理）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#SELF}，执行业务逻辑，并自动清理上下文。<br>
     * <b>推荐使用此方法，避免忘记清理上下文！</b>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询当前用户的订单（自动清理）
     * List<Order> orders = DataPermissionUtils.executeWithSelf(userId, () -> {
     *     return orderMapper.selectList();
     * });
     * }</pre>
     *
     * @param userId   当前用户 ID，不能为 null
     * @param supplier 业务逻辑，不能为 null
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值
     * @see #setSelf(Long)
     */
    public static <T> T executeWithSelf(Long userId, Supplier<T> supplier) {
        try {
            setSelf(userId);
            return supplier.get();
        } finally {
            clear();
        }
    }

    /**
     * 执行带"本部门数据"权限的业务逻辑（自动清理）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#DEPT}，执行业务逻辑，并自动清理上下文。<br>
     * <b>推荐使用此方法，避免忘记清理上下文！</b>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询本部门的订单（自动清理）
     * List<Order> orders = DataPermissionUtils.executeWithDept(userId, deptId, () -> {
     *     return orderMapper.selectList();
     * });
     * }</pre>
     *
     * @param userId   当前用户 ID，不能为 null
     * @param deptId   当前部门 ID，不能为 null
     * @param supplier 业务逻辑，不能为 null
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值
     * @see #setDept(Long, Long)
     */
    public static <T> T executeWithDept(Long userId, Long deptId, Supplier<T> supplier) {
        try {
            setDept(userId, deptId);
            return supplier.get();
        } finally {
            clear();
        }
    }

    /**
     * 执行带"本部门及子部门数据"权限的业务逻辑（自动清理）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#DEPT_AND_CHILD}，执行业务逻辑，并自动清理上下文。<br>
     * <b>推荐使用此方法，避免忘记清理上下文！</b>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询本部门及子部门的订单（自动清理）
     * List<Long> deptIds = Arrays.asList(5L, 6L, 7L);
     * List<Order> orders = DataPermissionUtils.executeWithDeptAndChild(userId, deptId, deptIds, () -> {
     *     return orderMapper.selectList();
     * });
     * }</pre>
     *
     * @param userId   当前用户 ID，不能为 null
     * @param deptId   当前部门 ID，不能为 null
     * @param deptIds  当前部门及子部门 ID 列表，不能为 null 或空
     * @param supplier 业务逻辑，不能为 null
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值
     * @see #setDeptAndChild(Long, Long, List)
     */
    public static <T> T executeWithDeptAndChild(Long userId, Long deptId, List<Long> deptIds, Supplier<T> supplier) {
        try {
            setDeptAndChild(userId, deptId, deptIds);
            return supplier.get();
        } finally {
            clear();
        }
    }

    /**
     * 执行带"本人数据"权限的业务逻辑（带租户，自动清理）
     * <p>
     * 设置数据权限为 {@link DataPermissionType#SELF}，并启用多租户过滤，执行业务逻辑，并自动清理上下文。<br>
     * <b>推荐使用此方法，避免忘记清理上下文！</b>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查询当前用户在当前租户下的订单（自动清理）
     * List<Order> orders = DataPermissionUtils.executeWithSelfWithTenant(userId, tenantId, () -> {
     *     return orderMapper.selectList();
     * });
     * }</pre>
     *
     * @param userId   当前用户 ID，不能为 null
     * @param tenantId 当前租户 ID，不能为 null
     * @param supplier 业务逻辑，不能为 null
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值
     * @see #setSelfWithTenant(Long, Long)
     */
    public static <T> T executeWithSelfWithTenant(Long userId, Long tenantId, Supplier<T> supplier) {
        try {
            setSelfWithTenant(userId, tenantId);
            return supplier.get();
        } finally {
            clear();
        }
    }

    /**
     * 执行带自定义权限的业务逻辑（自动清理）
     * <p>
     * 设置自定义数据权限上下文，执行业务逻辑，并自动清理上下文。<br>
     * <b>推荐使用此方法，避免忘记清理上下文！</b>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用自定义权限查询订单（自动清理）
     * DataPermissionContext context = DataPermissionUtils.builder()
     *     .userId(userId)
     *     .deptId(deptId)
     *     .tenantId(tenantId)
     *     .permissionType(DataPermissionType.DEPT)
     *     .build();
     *
     * List<Order> orders = DataPermissionUtils.executeWith(context, () -> {
     *     return orderMapper.selectList();
     * });
     * }</pre>
     *
     * @param context  数据权限上下文，不能为 null
     * @param supplier 业务逻辑，不能为 null
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值
     * @see #set(DataPermissionContext)
     */
    public static <T> T executeWith(DataPermissionContext context, Supplier<T> supplier) {
        try {
            set(context);
            return supplier.get();
        } finally {
            clear();
        }
    }

    /*=============================================    Builder 模式    =============================================*/

    /**
     * 创建数据权限上下文构建器
     * <p>
     * 使用 Builder 模式构建数据权限上下文，支持链式调用。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 构建自定义权限上下文
     * DataPermissionUtils.builder()
     *     .userId(userId)
     *     .deptId(deptId)
     *     .tenantId(tenantId)
     *     .permissionType(DataPermissionType.DEPT)
     *     .isAdmin(false)
     *     .build();
     *
     * try {
     *     List<Order> orders = orderMapper.selectList();
     * } finally {
     *     DataPermissionUtils.clear();
     * }
     * }</pre>
     *
     * @return 数据权限上下文构建器
     */
    public static ContextBuilder builder() {
        return new ContextBuilder();
    }

    /**
     * 数据权限上下文构建器
     * <p>
     * 使用 Builder 模式构建数据权限上下文，支持链式调用。
     */
    public static class ContextBuilder {

        /**
         * 数据权限上下文
         */
        private final DataPermissionContext context = new DataPermissionContext();

        /**
         * 设置用户 ID
         *
         * @param userId 用户 ID
         * @return 构建器
         */
        public ContextBuilder userId(Long userId) {
            context.setUserId(userId);
            return this;
        }

        /**
         * 设置部门 ID
         *
         * @param deptId 部门 ID
         * @return 构建器
         */
        public ContextBuilder deptId(Long deptId) {
            context.setDeptId(deptId);
            return this;
        }

        /**
         * 设置部门 ID 列表（包含子部门）
         *
         * @param deptIds 部门 ID 列表
         * @return 构建器
         */
        public ContextBuilder deptIds(List<Long> deptIds) {
            context.setDeptIds(deptIds);
            return this;
        }

        /**
         * 设置权限类型
         *
         * @param permissionType 权限类型
         * @return 构建器
         */
        public ContextBuilder permissionType(DataPermissionType permissionType) {
            context.setPermissionType(permissionType);
            return this;
        }

        /**
         * 设置租户 ID
         *
         * @param tenantId 租户 ID
         * @return 构建器
         */
        public ContextBuilder tenantId(Long tenantId) {
            context.setTenantId(tenantId);
            return this;
        }

        /**
         * 设置是否是超级管理员
         *
         * @param isAdmin 是否是超级管理员
         * @return 构建器
         */
        public ContextBuilder isAdmin(Boolean isAdmin) {
            context.setIsAdmin(isAdmin);
            return this;
        }

        /**
         * 构建并设置数据权限上下文
         * <p>
         * 构建数据权限上下文，并自动设置到 ThreadLocal 中。
         */
        public void build() {
            DataPermissionContext.set(context);
        }

        /**
         * 构建数据权限上下文（不自动设置）
         * <p>
         * 构建数据权限上下文，但不设置到 ThreadLocal 中。<br>
         * 可用于 {@link #executeWith(DataPermissionContext, Supplier)} 方法。
         *
         * @return 数据权限上下文
         */
        public DataPermissionContext buildContext() {
            return context;
        }
    }
}