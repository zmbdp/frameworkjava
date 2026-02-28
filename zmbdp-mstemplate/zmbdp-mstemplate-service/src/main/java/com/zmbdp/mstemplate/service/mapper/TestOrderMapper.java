package com.zmbdp.mstemplate.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.common.datapermission.annotation.DataPermission;
import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.mstemplate.service.domain.entity.TestOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试订单 Mapper
 * 用于测试数据权限功能
 *
 * @author 稚名不带撇
 */
@Mapper
public interface TestOrderMapper extends BaseMapper<TestOrder> {

    /**
     * 查询订单列表 - 使用默认配置（根据当前用户权限自动过滤）
     */
    @DataPermission
    List<TestOrder> selectListDefault();

    /**
     * 查询订单列表 - 仅本人数据
     */
    @DataPermission(type = DataPermissionType.SELF)
    List<TestOrder> selectListSelf();

    /**
     * 查询订单列表 - 本部门数据
     */
    @DataPermission(type = DataPermissionType.DEPT)
    List<TestOrder> selectListDept();

    /**
     * 查询订单列表 - 本部门及子部门数据
     */
    @DataPermission(type = DataPermissionType.DEPT_AND_CHILD)
    List<TestOrder> selectListDeptAndChild();

    /**
     * 查询订单列表 - 全部数据（超级管理员）
     */
    @DataPermission(type = DataPermissionType.ALL)
    List<TestOrder> selectListAll();

    /**
     * 查询订单列表 - 自定义条件
     */
    @DataPermission(type = DataPermissionType.CUSTOM, customCondition = "status = '2'")
    List<TestOrder> selectListCustom();

    /**
     * 查询订单列表 - 自定义用户字段名
     */
    @DataPermission(type = DataPermissionType.SELF, userColumn = "user_id")
    List<TestOrder> selectListCustomUserColumn();

    /**
     * 查询订单列表 - 自定义部门字段名
     */
    @DataPermission(type = DataPermissionType.DEPT, deptColumn = "dept_id")
    List<TestOrder> selectListCustomDeptColumn();

    /**
     * 查询订单列表 - 启用多租户过滤
     */
    @DataPermission(type = DataPermissionType.SELF, enableTenant = true, tenantColumn = "tenant_id")
    List<TestOrder> selectListWithTenant();

    /**
     * 查询订单列表 - 使用表别名
     */
    @DataPermission(type = DataPermissionType.SELF, tableAlias = "o", userColumn = "user_id")
    List<TestOrder> selectListWithAlias();

    /**
     * 根据订单号查询（不使用数据权限）
     */
    TestOrder selectByOrderNo(@Param("orderNo") String orderNo);
}