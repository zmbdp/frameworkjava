package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.domain.User;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 基础功能测试控制器
 * 测试 Result、异常处理、BeanCopyUtil 等基础功能
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 基础接口调用测试
     */
    @GetMapping("/info")
    public void info() {
        log.info("接口调用测试");
    }

    /**
     * Result 返回值测试
     *
     * @param id ID参数
     * @return 结果
     */
    @GetMapping("/result")
    public Result<Void> result(int id) {
        if (id < 0) {
            return Result.fail();
        }
        return Result.success();
    }

    /**
     * Result 返回对象测试
     *
     * @param id ID参数
     * @return 用户对象
     */
    @GetMapping("/resultUser")
    public Result<User> resultId(int id) {
        if (id < 0) {
            return Result.fail(ResultCode.TOKEN_CHECK_FAILED.getCode(), ResultCode.TOKEN_CHECK_FAILED.getErrMsg());
        }
        User user = new User();
        user.setAge(50);
        user.setName("张三");
        return Result.success(user);
    }

    /**
     * 异常处理测试
     *
     * @param id ID参数
     * @return 结果
     */
    @GetMapping("/exception")
    public Result<Void> exception(int id) {
        if (id < 0) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        if (id == 1) {
            throw new ServiceException("id不能为1");
        }
        if (id == 1000) {
            throw new ServiceException("id不能为1000", ResultCode.ERROR_PHONE_FORMAT.getCode());
        }
        return Result.success();
    }

    /**
     * BeanCopyUtil.copyMapProperties 测试
     *
     * @return Map拷贝结果
     */
    @GetMapping("/copyMapProperties")
    public Result<Map<String, User>> copyMapProperties() {
        // 创建测试数据
        Map<String, RegionTest> sourceMap = new HashMap<>();

        // 添加正常数据
        RegionTest region1 = new RegionTest();
        region1.setId(1L);
        region1.setName("北京");
        region1.setCode("110000");
        region1.setParentId(0L);
        sourceMap.put("region1", region1);

        // 添加 null 值测试
        sourceMap.put("nullRegion", null);

        // 添加另一个正常数据
        RegionTest region2 = new RegionTest();
        region2.setId(2L);
        region2.setName("上海");
        region2.setCode("310000");
        region2.setParentId(0L);
        sourceMap.put("region2", region2);

        // 使用BeanCopyUtil.copyMapProperties进行拷贝
        Map<String, User> resultMap = BeanCopyUtil.copyMapProperties(sourceMap, User.class);

        return Result.success(resultMap);
    }

    /**
     * BeanCopyUtil.copyMapListProperties 测试
     *
     * @return Map<List>拷贝结果
     */
    @GetMapping("/copyMapListProperties")
    public Result<Map<String, List<User>>> copyMapListProperties() {
        // 创建测试数据
        Map<String, List<RegionTest>> sourceMap = new HashMap<>();

        // 添加一个包含多个元素的列表
        List<RegionTest> regionList1 = new ArrayList<>();
        RegionTest region1 = new RegionTest();
        region1.setId(1L);
        region1.setName("北京");
        region1.setCode("110000");
        regionList1.add(region1);

        RegionTest region2 = new RegionTest();
        region2.setId(2L);
        region2.setName("上海");
        region2.setCode("310000");
        regionList1.add(region2);

        sourceMap.put("regions", regionList1);

        // 添加一个空列表
        sourceMap.put("emptyList", new ArrayList<>());

        // 添加 null 列表
        sourceMap.put("nullList", null);

        // 添加包含 null 元素的列表
        List<RegionTest> regionList2 = new ArrayList<>();
        regionList2.add(null);
        RegionTest region3 = new RegionTest();
        region3.setId(3L);
        region3.setName("广州");
        region3.setCode("440000");
        regionList2.add(region3);
        sourceMap.put("listWithNull", regionList2);

        // 使用BeanCopyUtil.copyMapListProperties进行拷贝
        Map<String, List<User>> resultMap = BeanCopyUtil.copyMapListProperties(sourceMap, User.class);

        return Result.success(resultMap);
    }

    /**
     * 测试所有深拷贝方法
     * 深拷贝支持复杂泛型嵌套（对象嵌套 List、对象嵌套对象等）
     */
    @GetMapping("/testDeepCopy")
    public Result<Map<String, Object>> testCopy() {
        Map<String, Object> result = new HashMap<>();

        // 1. 测试 copyProperties - 单个对象深拷贝（对象嵌套 List）
        MenuNode sourceMenu = new MenuNode();
        sourceMenu.setId(1L);
        sourceMenu.setName("系统管理");
        sourceMenu.setParentId(0L);

        MenuNode child1 = new MenuNode();
        child1.setId(2L);
        child1.setName("用户管理");
        child1.setParentId(1L);

        MenuNode child2 = new MenuNode();
        child2.setId(3L);
        child2.setName("角色管理");
        child2.setParentId(1L);

        sourceMenu.setChildren(Arrays.asList(child1, child2));

        // 1. 测试 copyProperties - 单个对象深拷贝
        MenuNodeDTO copiedMenu = BeanCopyUtil.copyProperties(sourceMenu, MenuNodeDTO::new);
        Map<String, Object> data = new HashMap<>();
        data.put("原数据", sourceMenu);
        data.put("拷贝数据", copiedMenu);
        result.put("1_copyProperties_单个对象", data);

        // 2. 测试 copyListProperties - List深拷贝（List 中对象嵌套 List）
        List<MenuNode> sourceMenuList = List.of(sourceMenu);
        List<MenuNodeDTO> copiedMenuList = BeanCopyUtil.copyListProperties(sourceMenuList, MenuNodeDTO::new);
        data = new HashMap<>();
        data.put("原数据", sourceMenuList);
        data.put("拷贝数据", copiedMenuList);
        result.put("2_copyListProperties_List集合", data);

        // 3. 测试 copyMapProperties - Map 深拷贝（Map 的 value 嵌套 List）
        Map<String, MenuNode> sourceMenuMap = new HashMap<>();
        sourceMenuMap.put("system", sourceMenu);
        Map<String, MenuNodeDTO> copiedMenuMap = BeanCopyUtil.copyMapProperties(sourceMenuMap, MenuNodeDTO::new);
        data = new HashMap<>();
        data.put("原数据", sourceMenuMap);
        data.put("拷贝数据", copiedMenuMap);
        result.put("3_copyMapProperties_Map集合", data);

        // 4. 测试 copyMapListProperties(Supplier) - Map<List> 深拷贝（List 中对象嵌套 List）
        Map<String, List<MenuNode>> sourceMapList = new HashMap<>();
        sourceMapList.put("menus", List.of(sourceMenu));
        Map<String, List<MenuNodeDTO>> copiedMapList = BeanCopyUtil.copyMapListProperties(sourceMapList, MenuNodeDTO::new);
        data = new HashMap<>();
        data.put("原数据", sourceMapList);
        data.put("拷贝数据", copiedMapList);
        result.put("4_copyMapListProperties_MapList集合", data);

        return Result.success(result);
    }

    /**
     * 测试所有浅拷贝方法
     * 包括：普通对象（无嵌套）和复杂对象（有嵌套，用浅拷贝看区别）两种场景
     */
    @GetMapping("/testShallowCopy")
    public Result<Map<String, Object>> testShallowCopy() {
        Map<String, Object> result = new HashMap<>();

        // ========== 场景 1：普通对象（无嵌套 List） ==========
        RegionTest simpleRegion = new RegionTest();
        simpleRegion.setId(1L);
        simpleRegion.setName("北京");
        simpleRegion.setCode("110000");
        simpleRegion.setParentId(0L);

        // 1. copyProperties - 单个普通对象浅拷贝
        User copiedUser = BeanCopyUtil.copyProperties(simpleRegion, User.class);
        Map<String, Object> data = new HashMap<>();
        data.put("原数据", simpleRegion);
        data.put("拷贝数据", copiedUser);
        result.put("1_普通对象_copyProperties", data);

        // 2. copyListProperties - List 普通对象浅拷贝
        List<RegionTest> simpleRegionList = List.of(simpleRegion);
        List<User> copiedUserList = BeanCopyUtil.copyListProperties(simpleRegionList, User.class);
        data = new HashMap<>();
        data.put("原数据", simpleRegionList);
        data.put("拷贝数据", copiedUserList);
        result.put("2_普通对象_copyListProperties", data);

        // 3. copyMapProperties - Map 普通对象浅拷贝
        Map<String, RegionTest> simpleRegionMap = new HashMap<>();
        simpleRegionMap.put("region1", simpleRegion);
        Map<String, User> copiedUserMap = BeanCopyUtil.copyMapProperties(simpleRegionMap, User.class);
        data = new HashMap<>();
        data.put("原数据", simpleRegionMap);
        data.put("拷贝数据", copiedUserMap);
        result.put("3_普通对象_copyMapProperties", data);

        // 4. copyMapListProperties - Map<List> 普通对象浅拷贝
        Map<String, List<RegionTest>> simpleMapList = new HashMap<>();
        simpleMapList.put("regions", List.of(simpleRegion));
        Map<String, List<User>> copiedMapList = BeanCopyUtil.copyMapListProperties(simpleMapList, User.class);
        data = new HashMap<>();
        data.put("原数据", simpleMapList);
        data.put("拷贝数据", copiedMapList);
        result.put("4_普通对象_copyMapListProperties", data);

        // ========== 场景 2：复杂对象（有嵌套 List） ==========
        MenuNode complexMenu = new MenuNode();
        complexMenu.setId(1L);
        complexMenu.setName("系统管理");
        complexMenu.setParentId(0L);

        MenuNode child1 = new MenuNode();
        child1.setId(2L);
        child1.setName("用户管理");
        child1.setParentId(1L);

        MenuNode child2 = new MenuNode();
        child2.setId(3L);
        child2.setName("角色管理");
        child2.setParentId(1L);

        complexMenu.setChildren(Arrays.asList(child1, child2));

        // 5. copyProperties - 单个复杂对象浅拷贝（children 会是引用拷贝）
        MenuNodeDTO copiedComplexMenu = BeanCopyUtil.copyProperties(complexMenu, MenuNodeDTO.class);
        data = new HashMap<>();
        data.put("原数据", complexMenu);
        data.put("拷贝数据", copiedComplexMenu);
        result.put("5_复杂对象_copyProperties", data);

        // 6. copyListProperties - List 复杂对象浅拷贝（children 会是引用拷贝）
        List<MenuNode> complexMenuList = List.of(complexMenu);
        List<MenuNodeDTO> copiedComplexMenuList = BeanCopyUtil.copyListProperties(complexMenuList, MenuNodeDTO.class);
        data = new HashMap<>();
        data.put("原数据", complexMenuList);
        data.put("拷贝数据", copiedComplexMenuList);
        result.put("6_复杂对象_copyListProperties", data);

        // 7. copyMapProperties - Map 复杂对象浅拷贝（children 会是引用拷贝）
        Map<String, MenuNode> complexMenuMap = new HashMap<>();
        complexMenuMap.put("menu1", complexMenu);
        Map<String, MenuNodeDTO> copiedComplexMenuMap = BeanCopyUtil.copyMapProperties(complexMenuMap, MenuNodeDTO.class);
        data = new HashMap<>();
        data.put("原数据", complexMenuMap);
        data.put("拷贝数据", copiedComplexMenuMap);
        result.put("7_复杂对象_copyMapProperties", data);

        // 8. copyMapListProperties - Map<List> 复杂对象浅拷贝（children 会是引用拷贝）
        Map<String, List<MenuNode>> complexMapList = new HashMap<>();
        complexMapList.put("menus", List.of(complexMenu));
        Map<String, List<MenuNodeDTO>> copiedComplexMapList = BeanCopyUtil.copyMapListProperties(complexMapList, MenuNodeDTO.class);
        data = new HashMap<>();
        data.put("原数据", complexMapList);
        data.put("拷贝数据", copiedComplexMapList);
        result.put("8_复杂对象_copyMapListProperties", data);

        return Result.success(result);
    }

    /**
     * 测试用的菜单节点对象（包含嵌套 List）
     */
    @Data
    public static class MenuNode {
        private Long id;
        private String name;
        private Long parentId;
        private List<MenuNode> children;
    }

    /**
     * 测试用的菜单节点对象（包含嵌套 List）
     */
    @Data
    public static class MenuNodeDTO {
        private Long aaa;
        private Long id;
        private String name;
        private Long parentId;
        private List<MenuNodeDTO> children;
    }
}