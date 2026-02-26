package com.zmbdp.common.datapermission.handler.impl;

import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.handler.DataPermissionContext;
import com.zmbdp.common.datapermission.handler.DataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 全部数据权限处理器
 * <p>
 * 不添加任何过滤条件，用户可以查看所有数据。<br>
 * 适用于超级管理员、系统管理员等角色。
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class AllDataPermissionHandler implements DataPermissionHandler {

    /**
     * 获取支持的数据权限类型
     *
     * @return 数据权限类型枚举
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.ALL;
    }

    /**
     * 构建全部数据权限过滤条件
     *
     * @param annotation 数据权限注解
     * @param context    数据权限上下文
     * @return 数据权限过滤条件
     */
    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        // 全部数据权限，不添加任何过滤条件
        log.debug("数据权限：ALL - 不添加过滤条件");
        return null;
    }
}