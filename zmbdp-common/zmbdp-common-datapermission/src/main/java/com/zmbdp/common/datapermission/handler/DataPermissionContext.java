package com.zmbdp.common.datapermission.handler;

import com.zmbdp.common.datapermission.enums.DataPermissionType;
import lombok.Data;

import java.util.List;

/**
 * 数据权限上下文
 * <p>
 * 用于存储当前用户的数据权限信息，包括用户 ID、部门 ID、权限类型、租户 ID 等。<br>
 * 这些信息会在 MyBatis 拦截器中用于构建 SQL 过滤条件。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>用户登录时，将权限信息存入上下文（ThreadLocal 或 Redis）</li>
 *     <li>MyBatis 拦截器从上下文获取权限信息，构建 SQL 过滤条件</li>
 *     <li>请求结束时，清理上下文，避免内存泄漏</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Data
public class DataPermissionContext {

    /**
     * 当前用户 ID
     * <p>
     * 用于构建 {@code SELF} 权限的过滤条件
     */
    private Long userId;

    /**
     * 当前用户所属部门 ID
     * <p>
     * 用于构建 {@code DEPT} 权限的过滤条件
     */
    private Long deptId;

    /**
     * 当前用户所属部门及子部门 ID 列表
     * <p>
     * 用于构建 {@code DEPT_AND_CHILD} 权限的过滤条件
     */
    private List<Long> deptIds;

    /**
     * 数据权限类型
     * <p>
     * 用于确定使用哪种权限过滤规则
     */
    private DataPermissionType permissionType;

    /**
     * 租户 ID（多租户场景）
     * <p>
     * 用于构建多租户过滤条件
     */
    private Long tenantId;

    /**
     * 是否是超级管理员
     * <p>
     * 超级管理员不受数据权限限制，可以查看所有数据
     */
    private Boolean isAdmin;

    /**
     * ThreadLocal 存储当前线程的数据权限上下文
     */
    private static final ThreadLocal<DataPermissionContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的数据权限上下文
     *
     * @param context 数据权限上下文
     */
    public static void set(DataPermissionContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程的数据权限上下文
     *
     * @return 数据权限上下文
     */
    public static DataPermissionContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清理当前线程的数据权限上下文
     * <p>
     * 建议在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}