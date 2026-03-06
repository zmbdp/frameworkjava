package com.zmbdp.common.datapermission.strategy.impl;

import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.context.DataPermissionContext;
import com.zmbdp.common.datapermission.strategy.DataPermissionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义数据权限策略处理器
 * <p>
 * 使用注解中配置的自定义 SQL 条件进行过滤，提供最大的灵活性。<br>
 * 适用于复杂业务场景，需要自定义过滤逻辑，无法通过标准权限类型满足需求的情况。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>处理 {@code DataPermissionType.CUSTOM} 权限类型</li>
 *     <li>从注解中获取自定义 SQL 条件（{@code annotation.customCondition()}）</li>
 *     <li>直接返回自定义条件，不做任何处理</li>
 *     <li>支持任意复杂的 SQL 条件表达式</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>复杂业务规则：</b>需要多个字段组合过滤（如 {@code status = '1' AND region_id IN (1, 2, 3)}）</li>
 *     <li><b>动态条件：</b>根据业务逻辑动态构建过滤条件</li>
 *     <li><b>特殊权限：</b>标准权限类型无法满足的特殊需求</li>
 *     <li><b>临时方案：</b>快速实现特定场景的数据权限，无需新增处理器</li>
 * </ul>
 * <p>
 * <b>SQL 示例：</b>
 * <pre>{@code
 * // 原始 SQL
 * SELECT * FROM sys_order WHERE create_time > '2024-01-01'
 *
 * // 改写后（单条件）
 * SELECT * FROM sys_order WHERE (status = '1') AND create_time > '2024-01-01'
 *
 * // 改写后（多条件）
 * SELECT * FROM sys_order WHERE (status = '1' AND region_id IN (1, 2, 3)) AND create_time > '2024-01-01'
 * }</pre>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * // 单条件过滤
 * DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '1'");
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 *
 * // 多条件过滤
 * DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '1' AND region_id IN (1, 2, 3)");
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 *
 * // 使用表别名
 * DataPermission(type = DataPermissionType.CUSTOM, customCondition = "o.status = '1' AND o.region_id IN (1, 2, 3)");
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 *
 * // 复杂条件（OR、子查询等）
 * DataPermission(type = DataPermissionType.CUSTOM, customCondition = "(status = '1' OR status = '2') AND region_id IN (SELECT id FROM sys_region WHERE parent_id = 1)");
 * List<SysOrder> selectOrderList(SysOrderQuery query);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>自定义条件直接拼接到 SQL 中，<b>必须注意 SQL 注入风险</b></li>
 *     <li>不要在自定义条件中使用用户输入的参数，只能使用常量或配置值</li>
 *     <li>如果自定义条件为空，返回 null，不添加过滤条件（记录警告日志）</li>
 *     <li>自定义条件不需要包含 WHERE 关键字，拦截器会自动添加</li>
 *     <li>如果条件包含多个表达式，建议使用括号包裹（如 {@code (a = 1 OR b = 2)}）</li>
 *     <li>如果表有别名，需要在自定义条件中手动添加别名前缀（如 {@code o.status = '1'}）</li>
 *     <li>自定义条件不支持动态参数，如需动态条件，建议在业务代码中构建 SQL</li>
 *     <li>建议优先使用标准权限类型（SELF、DEPT、DEPT_AND_CHILD），只在无法满足时使用自定义</li>
 * </ul>
 * <p>
 * <b>安全建议：</b>
 * <ul>
 *     <li>自定义条件只能使用常量或配置值，不能使用用户输入</li>
 *     <li>如需使用动态值，建议通过上下文传递，在处理器中构建条件</li>
 *     <li>定期审计自定义条件，确保没有 SQL 注入风险</li>
 *     <li>建议在开发环境记录所有自定义条件，便于排查问题</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.datapermission.strategy.DataPermissionStrategy
 * @see com.zmbdp.common.datapermission.enums.DataPermissionType#CUSTOM
 * @see com.zmbdp.common.datapermission.annotation.DataPermission#customCondition()
 */
@Slf4j
@Component
public class CustomDataPermissionStrategy implements DataPermissionStrategy {

    /**
     * 获取处理器支持的权限类型
     *
     * @return 数据权限类型枚举 {@code CUSTOM}
     */
    @Override
    public DataPermissionType getSupportType() {
        return DataPermissionType.CUSTOM;
    }

    /**
     * 构建自定义数据权限过滤条件
     * <p>
     * 直接从注解中获取自定义 SQL 条件，不做任何处理，原样返回。<br>
     * 如果自定义条件为空，返回 null，不添加过滤条件。
     * <p>
     * <b>注意：</b>自定义条件直接拼接到 SQL 中，必须注意 SQL 注入风险
     *
     * @param annotation 数据权限注解，包含自定义 SQL 条件
     * @param context    数据权限上下文（此处理器不使用上下文信息）
     * @return 自定义 SQL 过滤条件，如果为空返回 null
     */
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