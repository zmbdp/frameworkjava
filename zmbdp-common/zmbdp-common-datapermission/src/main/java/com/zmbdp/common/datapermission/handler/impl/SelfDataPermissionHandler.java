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
 * 仅本人数据权限处理器
 * <p>
 * 只能查看自己创建的数据，通过用户 ID 字段进行过滤。<br>
 * 适用于普通员工等角色。
 * <p>
 * <b>SQL 示例：</b>
 * <pre>
 * WHERE user_id = 123
 * WHERE u.user_id = 123  (使用表别名)
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RefreshScope
public class SelfDataPermissionHandler implements DataPermissionHandler {

    /**
     * 默认用户字段名（从 Nacos 配置中心读取）
     */
    @Value("${datapermission.default-user-column:user_id}")
    private String defaultUserColumn;

    /**
     * 获取支持的数据权限类型
     *
     * @return 数据权限类型枚举
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.SELF;
    }

    /**
     * 构建仅本人数据权限过滤条件
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return SQL 过滤条件，如果不需要过滤返回 null
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