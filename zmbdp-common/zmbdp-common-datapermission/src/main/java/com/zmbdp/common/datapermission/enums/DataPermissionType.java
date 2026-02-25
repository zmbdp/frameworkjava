package com.zmbdp.common.datapermission.enums;

import lombok.Getter;

/**
 * 数据权限类型枚举
 * <p>
 * 定义不同的数据权限范围，用于控制用户可以访问的数据范围。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li><b>ALL</b>：超级管理员，可以查看所有数据</li>
 *     <li><b>DEPT</b>：部门级权限，只能查看本部门及下级部门的数据</li>
 *     <li><b>DEPT_AND_CHILD</b>：部门及子部门权限，查看本部门和所有子部门数据</li>
 *     <li><b>SELF</b>：个人权限，只能查看自己创建的数据</li>
 *     <li><b>CUSTOM</b>：自定义权限，通过自定义 SQL 条件控制</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Getter
public enum DataPermissionType {

    /**
     * 全部数据权限（不添加任何过滤条件）
     * <p>
     * 适用场景：超级管理员、系统管理员
     */
    ALL("all", "全部数据权限"),

    /**
     * 本部门数据权限（仅本部门，不包含子部门）
     * <p>
     * 适用场景：部门主管查看本部门数据
     */
    DEPT("dept", "本部门数据权限"),

    /**
     * 本部门及子部门数据权限
     * <p>
     * 适用场景：部门经理查看本部门及下属部门数据
     */
    DEPT_AND_CHILD("dept_and_child", "本部门及子部门数据权限"),

    /**
     * 仅本人数据权限
     * <p>
     * 适用场景：普通员工只能查看自己创建的数据
     */
    SELF("self", "仅本人数据权限"),

    /**
     * 自定义数据权限
     * <p>
     * 适用场景：特殊业务场景，需要自定义过滤条件
     */
    CUSTOM("custom", "自定义数据权限");

    /**
     * 权限类型编码
     */
    private final String code;

    /**
     * 权限类型描述
     */
    private final String description;

    DataPermissionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 权限类型编码
     * @return 数据权限类型枚举
     */
    public static DataPermissionType fromCode(String code) {
        for (DataPermissionType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return SELF; // 默认返回仅本人权限
    }
}

