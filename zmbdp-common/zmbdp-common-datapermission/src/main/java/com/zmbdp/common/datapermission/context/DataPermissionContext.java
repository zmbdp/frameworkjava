package com.zmbdp.common.datapermission.context;

import com.zmbdp.common.datapermission.enums.DataPermissionType;
import lombok.Data;

import java.util.List;

/**
 * 数据权限上下文
 * <p>
 * 基于 ThreadLocal 存储当前线程（请求）的数据权限信息，为 MyBatis 拦截器提供权限过滤所需的上下文数据。<br>
 * 包含用户 ID、部门 ID、权限类型、租户 ID、管理员标识等核心字段，支持多种数据权限策略。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>存储当前用户的数据权限信息（用户 ID、部门 ID、权限类型等）</li>
 *     <li>提供线程安全的上下文访问（基于 ThreadLocal）</li>
 *     <li>为数据权限拦截器提供 SQL 过滤条件构建所需的数据</li>
 *     <li>支持多租户场景（租户 ID 隔离）</li>
 *     <li>支持超级管理员豁免（isAdmin 标识）</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>用户登录时：</b>从 JWT Token 或数据库中提取权限信息，调用 {@code set()} 方法存入上下文</li>
 *     <li><b>请求处理中：</b>MyBatis 拦截器调用 {@code get()} 方法获取权限信息，构建 SQL 过滤条件</li>
 *     <li><b>请求结束时：</b>调用 {@code clear()} 方法清理上下文，避免 ThreadLocal 内存泄漏</li>
 * </ul>
 * <p>
 * <b>典型使用流程：</b>
 * <pre>{@code
 * // 1. 用户登录后，设置数据权限上下文（通常在 Filter 或 Interceptor 中）
 * DataPermissionContext context = new DataPermissionContext();
 * context.setUserId(loginUser.getUserId());
 * context.setDeptId(loginUser.getDeptId());
 * context.setDeptIds(loginUser.getDeptIds());
 * context.setPermissionType(DataPermissionType.DEPT_AND_CHILD);
 * context.setTenantId(loginUser.getTenantId());
 * context.setIsAdmin(loginUser.getIsAdmin());
 * DataPermissionContext.set(context);
 *
 * // 2. MyBatis 拦截器自动从上下文获取权限信息，构建 SQL 过滤条件
 * // SELECT * FROM sys_user WHERE dept_id IN (10, 11, 12)
 *
 * // 3. 请求结束后，清理上下文（通常在 Filter 的 finally 块中）
 * DataPermissionContext.clear();
 * }</pre>
 * <p>
 * <b>权限类型说明：</b>
 * <ul>
 *     <li><b>ALL：</b>全部数据权限，不添加任何过滤条件（超级管理员）</li>
 *     <li><b>SELF：</b>仅本人数据，通过 {@code userId} 过滤</li>
 *     <li><b>DEPT：</b>本部门数据，通过 {@code deptId} 过滤</li>
 *     <li><b>DEPT_AND_CHILD：</b>本部门及子部门数据，通过 {@code deptIds} 过滤</li>
 *     <li><b>CUSTOM：</b>自定义过滤条件，通过注解配置</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>必须在请求结束时调用 {@code clear()} 方法，避免 ThreadLocal 内存泄漏</li>
 *     <li>如果 {@code isAdmin = true}，拦截器会跳过所有数据权限过滤</li>
 *     <li>如果上下文为空（{@code get() == null}），拦截器会跳过数据权限过滤</li>
 *     <li>多租户场景下，{@code tenantId} 会自动添加到 SQL 过滤条件中</li>
 *     <li>不同权限类型需要的字段不同：
 *         <ul>
 *             <li>SELF 需要 {@code userId}</li>
 *             <li>DEPT 需要 {@code deptId}</li>
 *             <li>DEPT_AND_CHILD 需要 {@code deptIds}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.interceptor.DataPermissionInterceptor
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 */
@Data
public class DataPermissionContext {

    /**
     * ThreadLocal 存储当前线程的数据权限上下文
     * <p>
     * 使用 ThreadLocal 保证线程安全，每个请求线程拥有独立的上下文副本。<br>
     * 必须在请求结束时调用 {@code clear()} 方法清理，避免内存泄漏。
     */
    private static final ThreadLocal<DataPermissionContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 当前用户 ID
     * <p>
     * 用于构建 {@code SELF} 权限的过滤条件，例如：{@code WHERE user_id = 123}
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>普通员工只能查看自己创建的数据</li>
     *     <li>个人中心查看自己的订单、工单等</li>
     * </ul>
     */
    private Long userId;

    /**
     * 当前用户所属部门 ID
     * <p>
     * 用于构建 {@code DEPT} 权限的过滤条件，例如：{@code WHERE dept_id = 10}
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>部门主管查看本部门的数据（不包含子部门）</li>
     *     <li>财务部门查看本部门的报销单</li>
     * </ul>
     */
    private Long deptId;

    /**
     * 当前用户所属部门及子部门 ID 列表
     * <p>
     * 用于构建 {@code DEPT_AND_CHILD} 权限的过滤条件，例如：{@code WHERE dept_id IN (10, 11, 12)}
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>部门经理查看本部门及所有子部门的数据</li>
     *     <li>区域经理查看所辖区域所有部门的数据</li>
     * </ul>
     * <p>
     * <b>注意：</b>需要提前查询并设置部门树的所有子部门 ID，拦截器不会自动查询
     */
    private List<Long> deptIds;

    /**
     * 数据权限类型
     * <p>
     * 用于确定使用哪种权限过滤规则，支持以下类型：
     * <ul>
     *     <li>{@code ALL}：全部数据（超级管理员）</li>
     *     <li>{@code SELF}：仅本人数据</li>
     *     <li>{@code DEPT}：本部门数据</li>
     *     <li>{@code DEPT_AND_CHILD}：本部门及子部门数据</li>
     *     <li>{@code CUSTOM}：自定义过滤条件</li>
     * </ul>
     * <p>
     * <b>优先级：</b>注解配置 > 上下文配置 > 默认值（SELF）
     */
    private DataPermissionType permissionType;

    /**
     * 租户 ID（多租户场景）
     * <p>
     * 用于构建多租户过滤条件，例如：{@code WHERE tenant_id = 1}
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>SaaS 系统中，不同租户的数据完全隔离</li>
     *     <li>集团公司中，不同子公司的数据隔离</li>
     * </ul>
     * <p>
     * <b>注意：</b>
     * <ul>
     *     <li>需要在 Nacos 中配置 {@code datapermission.enable-tenant=true} 才会生效</li>
     *     <li>租户过滤条件会与数据权限条件通过 AND 连接</li>
     * </ul>
     */
    private Long tenantId;

    /**
     * 是否是超级管理员
     * <p>
     * 超级管理员不受数据权限限制，可以查看所有数据。<br>
     * 如果 {@code isAdmin = true}，拦截器会直接放行，不添加任何过滤条件。
     */
    private Boolean isAdmin;

    /**
     * 设置当前线程的数据权限上下文
     * <p>
     * 通常在用户登录后、请求处理前调用，将用户的权限信息存入 ThreadLocal。<br>
     * 后续的 MyBatis 查询会自动从上下文中获取权限信息，构建 SQL 过滤条件。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. 用户登录后设置上下文（在 Filter 或 Interceptor 中）
     * DataPermissionContext context = new DataPermissionContext();
     * context.setUserId(loginUser.getUserId());
     * context.setDeptId(loginUser.getDeptId());
     * context.setDeptIds(loginUser.getDeptIds());
     * context.setPermissionType(DataPermissionType.DEPT_AND_CHILD);
     * context.setTenantId(loginUser.getTenantId());
     * context.setIsAdmin(loginUser.getIsAdmin());
     * DataPermissionContext.set(context);
     *
     * // 2. 从 JWT Token 中提取权限信息
     * String token = request.getHeader("Authorization");
     * LoginUserDTO loginUser = tokenService.getLoginUser(token, secret);
     * DataPermissionContext context = new DataPermissionContext();
     * context.setUserId(loginUser.getUserId());
     * context.setDeptId(loginUser.getDeptId());
     * // ... 设置其他字段
     * DataPermissionContext.set(context);
     *
     * // 3. 从网关下发的请求头中提取
     * Long userId = Long.valueOf(request.getHeader("X-User-Id"));
     * Long deptId = Long.valueOf(request.getHeader("X-Dept-Id"));
     * DataPermissionContext context = new DataPermissionContext();
     * context.setUserId(userId);
     * context.setDeptId(deptId);
     * DataPermissionContext.set(context);
     * }</pre>
     * <p>
     * <b>调用时机：</b>
     * <ul>
     *     <li>用户登录成功后，从数据库或缓存中加载权限信息</li>
     *     <li>请求拦截器（Filter/Interceptor）中，从 JWT Token 解析权限信息</li>
     *     <li>网关下发的请求头中提取权限信息</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在请求处理前调用，否则 MyBatis 拦截器无法获取权限信息</li>
     *     <li>必须在请求结束后调用 {@link #clear()} 清理上下文，避免内存泄漏</li>
     *     <li>不同权限类型需要设置不同的字段：SELF 需要 userId，DEPT 需要 deptId，DEPT_AND_CHILD 需要 deptIds</li>
     *     <li>如果 isAdmin 为 true，拦截器会跳过所有数据权限过滤</li>
     * </ul>
     *
     * @param context 数据权限上下文对象，包含用户 ID、部门 ID、权限类型等信息，不能为 null
     * @see #get()
     * @see #clear()
     */
    public static void set(DataPermissionContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程的数据权限上下文
     * <p>
     * MyBatis 拦截器会调用此方法获取当前用户的权限信息，用于构建 SQL 过滤条件。<br>
     * 如果返回 null，拦截器会跳过数据权限过滤。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. 在 MyBatis 拦截器中获取权限信息
     * DataPermissionContext context = DataPermissionContext.get();
     * if (context == null) {
     *     // 未设置上下文，跳过数据权限过滤
     *     return invocation.proceed();
     * }
     * if (Boolean.TRUE.equals(context.getIsAdmin())) {
     *     // 超级管理员，跳过数据权限过滤
     *     return invocation.proceed();
     * }
     * // 根据权限类型构建 SQL 过滤条件
     * String condition = buildCondition(context);
     *
     * // 2. 在业务代码中判断当前用户权限
     * DataPermissionContext context = DataPermissionContext.get();
     * if (context != null && context.getUserId() != null) {
     *     // 用户已登录，可以使用用户信息
     *     Long userId = context.getUserId();
     *     DataPermissionType type = context.getPermissionType();
     * }
     *
     * // 3. 在 AOP 切面中获取权限信息
     * DataPermissionContext context = DataPermissionContext.get();
     * if (context != null) {
     *     log.info("当前用户ID：{}，权限类型：{}", context.getUserId(), context.getPermissionType());
     * }
     * }</pre>
     * <p>
     * <b>调用时机：</b>
     * <ul>
     *     <li>MyBatis 拦截器执行 SQL 前，获取权限信息</li>
     *     <li>业务代码中需要判断当前用户权限时</li>
     *     <li>AOP 切面中需要记录用户操作日志时</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果返回 null，说明未设置上下文，拦截器会跳过数据权限过滤</li>
     *     <li>如果 {@code isAdmin = true}，拦截器会跳过数据权限过滤</li>
     *     <li>不要在异步线程中调用此方法，ThreadLocal 在异步线程中无法获取父线程的值</li>
     *     <li>获取到的对象是当前线程的副本，修改不会影响其他线程</li>
     * </ul>
     *
     * @return 数据权限上下文对象，如果未设置返回 null
     * @see #set(DataPermissionContext)
     * @see #clear()
     */
    public static DataPermissionContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清理当前线程的数据权限上下文
     * <p>
     * <b>必须在请求结束时调用</b>，避免 ThreadLocal 内存泄漏。<br>
     * 通常在 Filter 的 finally 块或拦截器的 afterCompletion 方法中调用。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 1. 在 Filter 中使用（推荐）
     * public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
     *     try {
     *         // 设置上下文
     *         DataPermissionContext context = buildContext(request);
     *         DataPermissionContext.set(context);
     *
     *         // 处理请求
     *         chain.doFilter(request, response);
     *     } finally {
     *         // 清理上下文（必须在 finally 块中）
     *         DataPermissionContext.clear();
     *     }
     * }
     *
     * // 2. 在 Interceptor 中使用
     * public class DataPermissionInterceptor implements HandlerInterceptor {
     *     @Override
     *     public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
     *         // 设置上下文
     *         DataPermissionContext context = buildContext(request);
     *         DataPermissionContext.set(context);
     *         return true;
     *     }
     *
     *     @Override
     *     public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
     *                                 Object handler, Exception ex) {
     *         // 清理上下文
     *         DataPermissionContext.clear();
     *     }
     * }
     *
     * // 3. 在异步任务中使用
     * @Async
     * public void asyncTask() {
     *     try {
     *         // 异步任务需要重新设置上下文
     *         DataPermissionContext context = new DataPermissionContext();
     *         // ... 设置字段
     *         DataPermissionContext.set(context);
     *
     *         // 执行任务
     *         doSomething();
     *     } finally {
     *         // 清理上下文
     *         DataPermissionContext.clear();
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 finally 块中调用，确保异常情况下也能清理</li>
     *     <li>如果不清理，会导致 ThreadLocal 内存泄漏</li>
     *     <li>线程池场景下，线程复用会导致上下文污染（下一个请求会读取到上一个请求的上下文）</li>
     *     <li>异步任务中需要重新设置上下文，因为 ThreadLocal 不会传递到子线程</li>
     *     <li>调用此方法后，{@link #get()} 会返回 null</li>
     * </ul>
     *
     * @see #set(DataPermissionContext)
     * @see #get()
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}