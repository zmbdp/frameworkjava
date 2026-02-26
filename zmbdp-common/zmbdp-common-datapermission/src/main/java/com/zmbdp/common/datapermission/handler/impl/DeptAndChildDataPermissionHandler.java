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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 本部门及子部门数据权限处理器
 * <p>
 * 可以查看本部门及所有子部门的数据。<br>
 * 适用于部门经理等角色。
 * <p>
 * <b>SQL 示例：</b>
 * <pre>
 * WHERE dept_id IN (10, 11, 12)
 * WHERE d.dept_id IN (10, 11, 12)  (使用表别名)
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RefreshScope
public class DeptAndChildDataPermissionHandler implements DataPermissionHandler {

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
        return DataPermissionType.DEPT_AND_CHILD;
    }

    /**
     * 构建本部门及子部门数据权限过滤条件
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return SQL 过滤条件，如果不需要过滤返回 null
     */
    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        if (context == null || context.getDeptIds() == null || context.getDeptIds().isEmpty()) {
            log.warn("数据权限：DEPT_AND_CHILD - 部门 ID 列表为空，跳过过滤");
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

        // 构建 IN 条件
        List<Long> deptIds = context.getDeptIds();
        String deptIdStr = deptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        String condition = deptColumn + " IN (" + deptIdStr + ")";
        log.debug("数据权限：DEPT_AND_CHILD - 过滤条件：{}", condition);
        return condition;
    }
}