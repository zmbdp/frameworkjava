package com.zmbdp.common.datapermission.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 数据权限自动配置类
 * <p>
 * 自动装配数据权限相关组件，包括拦截器、处理器等。<br>
 * 支持通过配置开关控制是否启用数据权限。
 * <p>
 * <b>配置说明：</b>
 * <ul>
 *     <li>{@code datapermission.enabled}：是否启用数据权限，默认 true</li>
 *     <li>{@code datapermission.default-user-column}：默认用户字段名，默认 user_id</li>
 *     <li>{@code datapermission.default-dept-column}：默认部门字段名，默认 dept_id</li>
 *     <li>{@code datapermission.default-tenant-column}：默认租户字段名，默认 tenant_id</li>
 *     <li>{@code datapermission.enable-tenant}：是否启用多租户过滤，默认 false</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Configuration
@ComponentScan("com.zmbdp.common.datapermission")
@ConditionalOnProperty(prefix = "datapermission", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataPermissionAutoConfiguration {

    public DataPermissionAutoConfiguration() {
        log.info("数据权限模块已启用");
    }
}
