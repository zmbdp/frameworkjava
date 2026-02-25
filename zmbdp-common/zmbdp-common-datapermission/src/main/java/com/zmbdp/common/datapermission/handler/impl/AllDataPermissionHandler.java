package com.zmbdp.common.datapermission.handler.impl;

import com.zmbdp.common.core.utils.StringUtil;
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

    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.ALL;
    }

    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        // 全部数据权限，不添加任何过滤条件
        log.debug("数据权限：ALL - 不添加过滤条件");
        return null;
    }
}

