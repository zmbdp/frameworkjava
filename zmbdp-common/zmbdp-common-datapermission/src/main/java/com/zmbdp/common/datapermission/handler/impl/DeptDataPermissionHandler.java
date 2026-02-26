package com.zmbdp.common.datapermission.handler.impl;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.handler.DataPermissionContext;
import com.zmbdp.common.datapermission.handler.DataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 本部门数据权限处理器
 * <p>
 * 只能查看本部门的数据，不包含子部门。<br>
 * 适用于部门主管等角色。
 * <p>
 * <b>SQL 示例：</b>
 * <pre>
 * WHERE dept_id = 10
 * WHERE d.dept_id = 10  (使用表别名)
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RefreshScope
public class DeptDataPermissionHandler implements DataPermissionHandler {

    /**
     * 默认部门字段名（从 Nacos 配置中心读取）
     */
    @Value("${datapermission.default-dept-column:dept_id}")
    private String defaultDeptColumn;

    /**
     * 获取支持的数据权限类型
     *
     * @return 数据权限类型枚举
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.DEPT;
    }

    /**
     * 构建本部门数据权限过滤条件
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return SQL 过滤条件，如果不需要过滤返回 null
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