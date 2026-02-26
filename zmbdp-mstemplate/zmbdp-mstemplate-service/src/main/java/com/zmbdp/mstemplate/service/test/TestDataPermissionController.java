package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.datapermission.enums.DataPermissionType;
import com.zmbdp.common.datapermission.utils.DataPermissionUtils;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.mstemplate.service.test.domain.entity.TestOrder;
import com.zmbdp.mstemplate.service.test.mapper.TestOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 数据权限功能测试控制器
 * 使用一键测试，无需手动传参
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/datapermission")
public class TestDataPermissionController {

    @Autowired
    private TestOrderMapper testOrderMapper;

    /*=============================================    一键测试接口    =============================================*/

    /**
     * 一键测试所有功能
     * 直接调用此接口即可测试所有数据权限功能，无需传参
     *
     * @return 详细的测试结果
     */
    @PostMapping("/all")
    public Result<Map<String, Object>> testAll() {
        log.info("=== 一键测试所有数据权限功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        // ========== 基础功能测试 ==========
        Map<String, Object> basic = new LinkedHashMap<>();
        try {
            // 1. 仅本人数据权限（user_id = 4）
            List<TestOrder> selfList = DataPermissionUtils.executeWithSelf(4L, () -> testOrderMapper.selectListSelf());
            basic.put("01-仅本人数据", formatResult(selfList, 2, "user_id=4的订单"));

            // 2. 本部门数据权限（dept_id = 5）
            List<TestOrder> deptList = DataPermissionUtils.executeWithDept(4L, 5L, () -> testOrderMapper.selectListDept());
            basic.put("02-本部门数据", formatResult(deptList, 4, "dept_id=5的订单"));

            // 3. 本部门及子部门数据权限（dept_id IN (2, 5, 6, 7)）
            List<TestOrder> deptAndChildList = DataPermissionUtils.executeWithDeptAndChild(2L, 2L, Arrays.asList(2L, 5L, 6L, 7L),
                    () -> testOrderMapper.selectListDeptAndChild());
            basic.put("03-本部门及子部门", formatResult(deptAndChildList, 6, "dept_id IN (2,5,6,7)的订单"));

            // 4. 全部数据权限（超级管理员）
            List<TestOrder> allList = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(1L).deptId(1L).isAdmin(true).buildContext(),
                    () -> testOrderMapper.selectListAll()
            );
            basic.put("04-全部数据(超管)", formatResult(allList, 10, "所有订单"));

            // 5. 自定义条件（status = '2'）
            List<TestOrder> customList = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(4L).deptId(5L).permissionType(DataPermissionType.CUSTOM).buildContext(),
                    () -> testOrderMapper.selectListCustom()
            );
            basic.put("05-自定义条件", formatResult(customList, 6, "status='2'的订单"));

        } catch (Exception e) {
            basic.put("错误", "❌ 基础功能测试异常: " + e.getMessage());
            log.error("基础功能测试异常", e);
        }
        result.put("基础功能", basic);

        // ========== 自定义字段测试 ==========
        Map<String, Object> customField = new LinkedHashMap<>();
        try {
            // 1. 自定义用户字段名
            List<TestOrder> customUserList = DataPermissionUtils.executeWithSelf(4L, () -> testOrderMapper.selectListCustomUserColumn());
            customField.put("01-自定义用户字段", formatResult(customUserList, 2, "user_id=4的订单"));

            // 2. 自定义部门字段名
            List<TestOrder> customDeptList = DataPermissionUtils.executeWithDept(4L, 5L, () -> testOrderMapper.selectListCustomDeptColumn());
            customField.put("02-自定义部门字段", formatResult(customDeptList, 4, "dept_id=5的订单"));

        } catch (Exception e) {
            customField.put("错误", "❌ 自定义字段测试异常: " + e.getMessage());
            log.error("自定义字段测试异常", e);
        }
        result.put("自定义字段", customField);

        // ========== 多租户测试 ==========
        Map<String, Object> tenant = new LinkedHashMap<>();
        try {
            // 1. 启用多租户过滤（tenant_id = 1）
            List<TestOrder> tenantList = DataPermissionUtils.executeWithSelfWithTenant(4L, 1L, () -> testOrderMapper.selectListWithTenant());
            tenant.put("01-多租户过滤", formatResult(tenantList, 2, "user_id=4 AND tenant_id=1的订单"));

            // 2. 切换租户（tenant_id = 2）
            List<TestOrder> tenant2List = DataPermissionUtils.executeWithSelfWithTenant(8L, 2L, () -> testOrderMapper.selectListWithTenant());
            tenant.put("02-切换租户", formatResult(tenant2List, 2, "user_id=8 AND tenant_id=2的订单"));

        } catch (Exception e) {
            tenant.put("错误", "❌ 多租户测试异常: " + e.getMessage());
            log.error("多租户测试异常", e);
        }
        result.put("多租户", tenant);

        // ========== 表别名测试 ==========
        Map<String, Object> alias = new LinkedHashMap<>();
        try {
            List<TestOrder> aliasList = DataPermissionUtils.executeWithSelf(4L, () -> testOrderMapper.selectListWithAlias());
            alias.put("01-使用表别名", formatResult(aliasList, 2, "o.user_id=4的订单"));

        } catch (Exception e) {
            alias.put("错误", "❌ 表别名测试异常: " + e.getMessage());
            log.error("表别名测试异常", e);
        }
        result.put("表别名", alias);

        // ========== 权限切换测试 ==========
        Map<String, Object> switchTest = new LinkedHashMap<>();
        try {
            // 1. 普通员工（user_id=4）查看自己的订单
            List<TestOrder> user4List = DataPermissionUtils.executeWithSelf(4L, () -> testOrderMapper.selectListSelf());
            switchTest.put("01-普通员工(user4)", formatResult(user4List, 2, "只能看到自己的2条订单"));

            // 2. 部门主管（user_id=3）查看本部门订单
            List<TestOrder> leader3List = DataPermissionUtils.executeWithDept(3L, 5L, () -> testOrderMapper.selectListDept());
            switchTest.put("02-部门主管(user3)", formatResult(leader3List, 4, "能看到本部门的4条订单"));

            // 3. 部门经理（user_id=2）查看本部门及子部门订单
            List<TestOrder> manager2List = DataPermissionUtils.executeWithDeptAndChild(2L, 2L, Arrays.asList(2L, 5L, 6L, 7L),
                    () -> testOrderMapper.selectListDeptAndChild());
            switchTest.put("03-部门经理(user2)", formatResult(manager2List, 6, "能看到本部门及子部门的6条订单"));

            // 4. 超级管理员（user_id=1）查看所有订单
            List<TestOrder> admin1List = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(1L).deptId(1L).isAdmin(true).buildContext(),
                    () -> testOrderMapper.selectListAll()
            );
            switchTest.put("04-超级管理员(user1)", formatResult(admin1List, 10, "能看到所有10条订单"));

        } catch (Exception e) {
            switchTest.put("错误", "❌ 权限切换测试异常: " + e.getMessage());
            log.error("权限切换测试异常", e);
        }
        result.put("权限切换", switchTest);

        // ========== 异常情况测试 ==========
        Map<String, Object> exception = new LinkedHashMap<>();
        try {
            // 1. 无上下文（应该跳过过滤）
            DataPermissionUtils.clear();
            List<TestOrder> noContextList = testOrderMapper.selectListSelf();
            exception.put("01-无上下文", formatResult(noContextList, 10, "跳过过滤，返回所有订单"));

            // 2. 超级管理员（应该跳过过滤）
            List<TestOrder> adminList = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(1L).deptId(1L).isAdmin(true).buildContext(),
                    () -> testOrderMapper.selectListSelf()
            );
            exception.put("02-超级管理员", formatResult(adminList, 10, "跳过过滤，返回所有订单"));

            // 3. 用户ID为空（应该跳过过滤）
            List<TestOrder> noUserIdList = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(null).deptId(5L).buildContext(),
                    () -> testOrderMapper.selectListSelf()
            );
            exception.put("03-用户ID为空", formatResult(noUserIdList, 10, "跳过过滤，返回所有订单"));

            // 4. 部门ID为空（应该跳过过滤）
            List<TestOrder> noDeptIdList = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(4L).deptId(null).buildContext(),
                    () -> testOrderMapper.selectListDept()
            );
            exception.put("04-部门ID为空", formatResult(noDeptIdList, 10, "跳过过滤，返回所有订单"));

        } catch (Exception e) {
            exception.put("错误", "❌ 异常情况测试异常: " + e.getMessage());
            log.error("异常情况测试异常", e);
        }
        result.put("异常情况", exception);

        result.put("测试说明", "所有测试完成，请查看日志获取详细信息");
        result.put("提示", "测试了所有DataPermission注解参数和各种权限类型");

        return Result.success(result);
    }

    /**
     * 快速测试 - 只测试核心功能
     *
     * @return 测试结果
     */
    @PostMapping("/quick")
    public Result<Map<String, Object>> testQuick() {
        log.info("=== 快速测试核心功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 1. 仅本人数据
            List<TestOrder> selfList = DataPermissionUtils.executeWithSelf(4L, () -> testOrderMapper.selectListSelf());
            result.put("仅本人数据", formatResult(selfList, 2, "user_id=4"));

            // 2. 本部门数据
            List<TestOrder> deptList = DataPermissionUtils.executeWithDept(4L, 5L, () -> testOrderMapper.selectListDept());
            result.put("本部门数据", formatResult(deptList, 4, "dept_id=5"));

            // 3. 全部数据
            List<TestOrder> allList = DataPermissionUtils.executeWith(
                    DataPermissionUtils.builder().userId(1L).deptId(1L).isAdmin(true).buildContext(),
                    () -> testOrderMapper.selectListAll()
            );
            result.put("全部数据", formatResult(allList, 10, "所有订单"));

        } catch (Exception e) {
            result.put("错误", "❌ 快速测试异常: " + e.getMessage());
            log.error("快速测试异常", e);
        }

        return Result.success(result);
    }

    /**
     * 获取测试说明
     */
    @GetMapping("/help")
    public Result<String> getTestHelp() {
        String help = """
                ============================ 数据权限功能测试说明 ============================
                
                【一键测试接口】
                
                1. POST /test/datapermission/all - 一键测试所有功能
                   说明：直接调用此接口即可测试所有数据权限功能，无需传参
                   测试内容：基础功能、自定义字段、多租户、表别名、权限切换、异常情况
                
                2. POST /test/datapermission/quick - 快速测试核心功能
                   说明：快速测试核心功能，测试时间更短
                
                3. GET /test/datapermission/help - 获取测试说明（本接口）
                
                【测试数据说明】
                - 部门结构：总公司(1) -> 研发部(2) -> 后端组(5)、前端组(6)、测试组(7)
                           总公司(1) -> 市场部(3) -> 市场一部(8)、市场二部(9)
                - 用户数据：user1(id=4, dept=5), user2(id=5, dept=5), user3(id=6, dept=6)
                - 订单数据：共10条订单，分布在不同部门和租户
                
                【权限类型说明】
                - SELF：仅本人数据（user_id = 当前用户ID）
                - DEPT：本部门数据（dept_id = 当前部门ID）
                - DEPT_AND_CHILD：本部门及子部门数据（dept_id IN (当前部门ID, 子部门ID...)）
                - ALL：全部数据（不添加过滤条件）
                - CUSTOM：自定义条件（使用注解中的 customCondition）
                
                【测试结果说明】
                - ✅ 表示测试通过
                - ❌ 表示测试失败
                - 结果格式：✅ 通过（实际: X条，期望: Y条）- 说明
                
                ======================================================================
                """;

        return Result.success(help);
    }

    /**
     * 查看测试数据
     */
    @GetMapping("/data")
    public Result<Map<String, Object>> getTestData() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 查询所有订单（不使用数据权限）
            DataPermissionUtils.clear();
            List<TestOrder> allOrders = testOrderMapper.selectListAll();

            // 按部门分组
            Map<Long, List<TestOrder>> byDept = new LinkedHashMap<>();
            Map<Long, List<TestOrder>> byUser = new LinkedHashMap<>();
            Map<Long, List<TestOrder>> byTenant = new LinkedHashMap<>();

            for (TestOrder order : allOrders) {
                byDept.computeIfAbsent(order.getDeptId(), k -> new ArrayList<>()).add(order);
                byUser.computeIfAbsent(order.getUserId(), k -> new ArrayList<>()).add(order);
                byTenant.computeIfAbsent(order.getTenantId(), k -> new ArrayList<>()).add(order);
            }

            result.put("总订单数", allOrders.size());
            result.put("按部门分组", formatGroupResult(byDept));
            result.put("按用户分组", formatGroupResult(byUser));
            result.put("按租户分组", formatGroupResult(byTenant));
            result.put("所有订单", allOrders);

        } catch (Exception e) {
            result.put("错误", "❌ 查询测试数据异常: " + e.getMessage());
            log.error("查询测试数据异常", e);
        }

        return Result.success(result);
    }

    /*=============================================    辅助方法    =============================================*/

    /**
     * 格式化查询结果
     *
     * @param list          查询结果列表
     * @param expectedCount 期望数量
     * @param description   说明
     * @return 格式化后的结果字符串
     */
    private String formatResult(List<TestOrder> list, int expectedCount, String description) {
        int actualCount = list != null ? list.size() : 0;
        boolean pass = actualCount == expectedCount;

        String result = pass
                ? String.format("✅ 通过（实际: %d条，期望: %d条）- %s", actualCount, expectedCount, description)
                : String.format("❌ 失败（实际: %d条，期望: %d条）- %s", actualCount, expectedCount, description);

        log.info(result);
        return result;
    }

    /**
     * 格式化分组结果
     */
    private Map<String, Integer> formatGroupResult(Map<Long, List<TestOrder>> groupMap) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<TestOrder>> entry : groupMap.entrySet()) {
            result.put("ID=" + entry.getKey(), entry.getValue().size());
        }
        return result;
    }
}

