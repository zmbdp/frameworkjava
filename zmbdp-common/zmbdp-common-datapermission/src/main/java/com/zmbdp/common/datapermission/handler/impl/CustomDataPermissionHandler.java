package com.zmbdp.common.datapermission.handler.impl;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.handler.DataPermissionContext;
import com.zmbdp.common.datapermission.handler.DataPermissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义数据权限处理器
 * <p>
 * 使用注解中配置的自定义 SQL 条件进行过滤。<br>
 * 适用于复杂业务场景，需要自定义过滤逻辑。
 * <p>
 * <b>SQL 示例：</b>
 * <pre>
 * WHERE status = '1'
 * WHERE status = '1' AND region_id IN (1, 2, 3)
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class CustomDataPermissionHandler implements DataPermissionHandler {

    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.CUSTOM;  
    }

    @Override
    public String buildCondition(DataPermission annotation, DataPermissionContext context) {
        String customCondition = annotation.customCondition();
        
        if (StringUtil.isEmpty(customCondition)) {
            log.warn("数据权限：CUSTOM - 自定义条件为空，跳过过滤");
            return null;
        }

        log.debug("数据权限：CUSTOM - 过滤条件：{}", customCondition);
        return customCondition;
    }
}

