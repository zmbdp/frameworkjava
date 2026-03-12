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
import java.util.function.Supplier;

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

        // 使用 BeanCopyUtil.copyMapProperties 进行拷贝
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
     * 测试带字段映射的所有新增 copyProperties 重载方法
     * <p>
     * 使用独立的 MappingSource / MappingTarget 对象，与上方深/浅拷贝测试对象完全隔离。<br>
     * MappingSource.nickName（String）→ MappingTarget.displayName（String）<br>
     * MappingSource.score（Integer）→ MappingTarget.point（Integer）<br>
     * 其余同名字段（id、parentId、children）走普通拷贝。
     */
    @GetMapping("/testFieldMapping")
    public Result<Map<String, Object>> testFieldMapping() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ==================== 构造测试数据（四层嵌套）====================
        // 第4层（叶子节点）
        MappingSource level4 = new MappingSource();
        level4.setId(4L);
        level4.setNickName("第4层-权限详情");
        level4.setScore(10);
        level4.setParentId(3L);
        level4.setSourceNode(new MappingSourceNode());
        level4.getSourceNode().setAge(14);
        level4.getSourceNode().setName("L4节点");
        level4.setChildren(new ArrayList<>());
        level4.setChildren1(new ArrayList<>());

        // 第3层
        MappingSource level3 = new MappingSource();
        level3.setId(3L);
        level3.setNickName("第3层-权限管理");
        level3.setScore(30);
        level3.setParentId(2L);
        level3.setSourceNode(new MappingSourceNode());
        level3.getSourceNode().setAge(13);
        level3.getSourceNode().setName("L3节点");
        level3.setChildren(new ArrayList<>());
        level3.setChildren1(Collections.singletonList(level4));

        // 第2层
        MappingSource level2 = new MappingSource();
        level2.setId(2L);
        level2.setNickName("第2层-用户管理");
        level2.setScore(60);
        level2.setParentId(1L);
        level2.setSourceNode(new MappingSourceNode());
        level2.getSourceNode().setAge(12);
        level2.getSourceNode().setName("L2节点");
        level2.setChildren(new ArrayList<>());
        level2.setChildren1(Collections.singletonList(level3));

        // 第2层（另一个子节点）
        MappingSource level2b = new MappingSource();
        level2b.setId(5L);
        level2b.setNickName("第2层-角色管理");
        level2b.setScore(55);
        level2b.setParentId(1L);
        level2b.setSourceNode(new MappingSourceNode());
        level2b.getSourceNode().setAge(15);
        level2b.getSourceNode().setName("L2b节点");
        level2b.setChildren(new ArrayList<>());
        level2b.setChildren1(new ArrayList<>());

        // 第1层（根节点）
        MappingSource root = new MappingSource();
        root.setId(1L);
        root.setNickName("第1层-系统管理");
        root.setScore(100);
        root.setParentId(0L);
        root.setSourceNode(new MappingSourceNode());
        root.getSourceNode().setAge(11);
        root.getSourceNode().setName("L1节点");
        // children 第3层（叶子）
        MappingSourceChildren sc3a = new MappingSourceChildren();
        sc3a.setAge(31);
        sc3a.setName("子项C-1");
        sc3a.setChildren(new ArrayList<>());

        MappingSourceChildren sc3b = new MappingSourceChildren();
        sc3b.setAge(32);
        sc3b.setName("子项C-2");
        sc3b.setChildren(new ArrayList<>());

        // children 第3层（另一个分支的叶子）
        MappingSourceChildren sc3c = new MappingSourceChildren();
        sc3c.setAge(33);
        sc3c.setName("子项D-1");
        sc3c.setChildren(new ArrayList<>());

        // children 第2层
        MappingSourceChildren sc2a = new MappingSourceChildren();
        sc2a.setAge(21);
        sc2a.setName("子项B-1");
        sc2a.setChildren(Arrays.asList(sc3a, sc3b));

        MappingSourceChildren sc2b = new MappingSourceChildren();
        sc2b.setAge(22);
        sc2b.setName("子项B-2");
        sc2b.setChildren(Collections.singletonList(sc3c));

        // children 第1层（root 的直接子项）
        MappingSourceChildren sc1 = new MappingSourceChildren();
        sc1.setAge(10);
        sc1.setName("子项A");
        sc1.setChildren(Arrays.asList(sc2a, sc2b));
        sc1.setChildrenNode(new MappingSourceChildren());

        MappingSourceChildren sc2 = new MappingSourceChildren();
        sc2.setAge(20);
        sc2.setName("子项B");
        sc2.setChildren(Collections.singletonList(sc2a));

        root.setChildren(Arrays.asList(sc1, sc2));
        root.setChildren1(Arrays.asList(level2, level2b));

        MappingSource node2 = new MappingSource();
        node2.setId(10L);
        node2.setNickName("日志管理");
        node2.setScore(70);
        node2.setParentId(0L);
        node2.setSourceNode(new MappingSourceNode());
        node2.getSourceNode().setAge(30);
        node2.getSourceNode().setName("日志节点");
        node2.setChildren(new ArrayList<>());
        node2.setChildren1(Collections.singletonList(level2));

        // ==================== 1. 单对象浅拷贝 + 字段映射（.class） ====================
        MappingTarget shallow1 = BeanCopyUtil.copyProperties(root, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case1 = new LinkedHashMap<>();
        case1.put("说明", "浅拷贝：nickName->displayName，score->point，children是引用拷贝");
        case1.put("原nickName", root.getNickName());
        case1.put("拷贝后_displayName（应=系统管理）", shallow1.getDisplayName());
        case1.put("原score", root.getScore());
        case1.put("拷贝后_point（应=100）", shallow1.getPoint());
        result.put("1_单对象_浅拷贝_字段映射", case1);

        // ==================== 2. 单对象深拷贝 + 字段映射（::new） ====================
        MappingTarget deep1 = BeanCopyUtil.copyProperties(root, (Supplier<MappingTarget>) MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case2 = new LinkedHashMap<>();
        case2.put("说明", "深拷贝：nickName->displayName，score->point，children被递归深拷贝");
        case2.put("拷贝后_displayName（应=系统管理）", deep1.getDisplayName());
        case2.put("拷贝后_point（应=100）", deep1.getPoint());
        case2.put("拷贝后_children数量（应=2）", deep1.getChildren2() != null ? deep1.getChildren2().size() : 0);
        result.put("2_单对象_深拷贝_字段映射", case2);

        // ==================== 3. 单对象 source=null 测试 ====================
        MappingTarget nullShallow = BeanCopyUtil.copyProperties(null, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )

        );
        MappingTarget nullDeep = BeanCopyUtil.copyProperties(null, (Supplier<MappingTarget>) MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case3 = new LinkedHashMap<>();
        case3.put("说明", "source为null时应返回null");
        case3.put("浅拷贝结果（应为null）", nullShallow);
        case3.put("深拷贝结果（应为null）", nullDeep);
        result.put("3_单对象_null_测试", case3);

        // ==================== 4. 不传 mappings 退化为普通拷贝 ====================
        MappingTarget noMapping = BeanCopyUtil.copyProperties(root, MappingTarget.class);
        MappingTarget noMapping2 = BeanCopyUtil.copyProperties(root, MappingTarget::new);
        Map<String, Object> case4 = new LinkedHashMap<>();
        case4.put("说明", "不传mappings时退化为普通拷贝，displayName/point应为null");
        case4.put("拷贝后_displayName（应为null）", noMapping.getDisplayName());
        case4.put("拷贝后_point（应为null）", noMapping.getPoint());
        case4.put("拷贝后_id（应=1，同名字段正常拷贝）", noMapping.getId());
        result.put("4_单对象_不传mappings退化", case4);

        // ==================== 5. List 浅拷贝 + 字段映射（.class） ====================
        List<MappingSource> sourceList = Arrays.asList(root, node2);
        List<MappingTarget> shallowList = BeanCopyUtil.copyListProperties(sourceList, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case5 = new LinkedHashMap<>();
        case5.put("说明", "List浅拷贝：每个元素的nickName->displayName，score->point");
        case5.put("元素数量（应=2）", shallowList.size());
        case5.put("第1个_displayName（应=系统管理）", shallowList.get(0).getDisplayName());
        case5.put("第1个_point（应=100）", shallowList.get(0).getPoint());
        case5.put("第2个_displayName（应=日志管理）", shallowList.get(1).getDisplayName());
        case5.put("第2个_point（应=70）", shallowList.get(1).getPoint());
        result.put("5_List_浅拷贝_字段映射", case5);

        // ==================== 6. List 深拷贝 + 字段映射（::new） ====================
        List<MappingTarget> deepList = BeanCopyUtil.copyListProperties(sourceList, MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case6 = new LinkedHashMap<>();
        case6.put("说明", "List深拷贝：children被深拷贝，nickName->displayName，score->point");
        case6.put("元素数量（应=2）", deepList.size());
        case6.put("第1个_displayName（应=系统管理）", deepList.get(0).getDisplayName());
        case6.put("第1个_point（应=100）", deepList.get(0).getPoint());
        case6.put("第1个_children数量（应=2）", deepList.get(0).getChildren2() != null ? deepList.get(0).getChildren2().size() : 0);
        case6.put("第2个_displayName（应=日志管理）", deepList.get(1).getDisplayName());
        result.put("6_List_深拷贝_字段映射", case6);

        // ==================== 7. List source=null 测试 ====================
        List<MappingTarget> nullList1 = BeanCopyUtil.copyListProperties(null, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        List<MappingTarget> nullList2 = BeanCopyUtil.copyListProperties(null, MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case7 = new LinkedHashMap<>();
        case7.put("说明", "List source为null时应返回空集合");
        case7.put("浅拷贝结果_size（应=0）", nullList1.size());
        case7.put("深拷贝结果_size（应=0）", nullList2.size());
        result.put("7_List_null_测试", case7);

        // ==================== 8. Map 浅拷贝 + 字段映射（.class） ====================
        Map<String, MappingSource> sourceMap = new LinkedHashMap<>();
        sourceMap.put("root", root);
        sourceMap.put("node2", node2);
        sourceMap.put("nullValue", null);
        Map<String, MappingTarget> shallowMap = BeanCopyUtil.copyMapProperties(sourceMap, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case8 = new LinkedHashMap<>();
        case8.put("说明", "Map浅拷贝：nickName->displayName，score->point，null value不报错");
        case8.put("root_displayName（应=系统管理）", shallowMap.get("root") != null ? shallowMap.get("root").getDisplayName() : "null");
        case8.put("root_point（应=100）", shallowMap.get("root") != null ? shallowMap.get("root").getPoint() : "null");
        case8.put("node2_displayName（应=日志管理）", shallowMap.get("node2") != null ? shallowMap.get("node2").getDisplayName() : "null");
        case8.put("nullValue_结果（应=非null空对象）", shallowMap.get("nullValue"));
        result.put("8_Map_浅拷贝_字段映射", case8);

        // ==================== 9. Map 深拷贝 + 字段映射（::new） ====================
        Map<String, MappingTarget> deepMap = BeanCopyUtil.copyMapProperties(sourceMap, MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case9 = new LinkedHashMap<>();
        case9.put("说明", "Map深拷贝：children被深拷贝，nickName->displayName，score->point");
        case9.put("root_displayName（应=系统管理）", deepMap.get("root") != null ? deepMap.get("root").getDisplayName() : "null");
        case9.put("root_point（应=100）", deepMap.get("root") != null ? deepMap.get("root").getPoint() : "null");
        case9.put("root_children数量（应=2）", deepMap.get("root") != null && deepMap.get("root").getChildren2() != null ? deepMap.get("root").getChildren2().size() : 0);
        case9.put("node2_displayName（应=日志管理）", deepMap.get("node2") != null ? deepMap.get("node2").getDisplayName() : "null");
        result.put("9_Map_深拷贝_字段映射", case9);

        // ==================== 10. Map source=null 测试 ====================
        Map<String, MappingTarget> nullMap1 = BeanCopyUtil.copyMapProperties(null, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, MappingTarget> nullMap2 = BeanCopyUtil.copyMapProperties(null, MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case10 = new LinkedHashMap<>();
        case10.put("说明", "Map source为null时应返回空Map");
        case10.put("浅拷贝结果_size（应=0）", nullMap1.size());
        case10.put("深拷贝结果_size（应=0）", nullMap2.size());
        result.put("10_Map_null_测试", case10);

        // ==================== 11. MapList 浅拷贝 + 字段映射（.class） ====================
        Map<String, List<MappingSource>> sourceMapList = new LinkedHashMap<>();
        sourceMapList.put("group1", Arrays.asList(root, node2));
        sourceMapList.put("group2", Collections.singletonList(level2));
        sourceMapList.put("emptyGroup", new ArrayList<>());
        sourceMapList.put("nullGroup", null);
        Map<String, List<MappingTarget>> shallowMapList = BeanCopyUtil.copyMapListProperties(sourceMapList, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case11 = new LinkedHashMap<>();
        case11.put("说明", "MapList浅拷贝：nickName->displayName，score->point，空/null列表不报错");
        case11.put("group1_size（应=2）", shallowMapList.get("group1") != null ? shallowMapList.get("group1").size() : 0);
        case11.put("group1_第1个_displayName（应=系统管理）", shallowMapList.get("group1") != null && !shallowMapList.get("group1").isEmpty() ? shallowMapList.get("group1").get(0).getDisplayName() : "null");
        case11.put("group1_第1个_point（应=100）", shallowMapList.get("group1") != null && !shallowMapList.get("group1").isEmpty() ? shallowMapList.get("group1").get(0).getPoint() : "null");
        case11.put("emptyGroup_size（应=0）", shallowMapList.get("emptyGroup") != null ? shallowMapList.get("emptyGroup").size() : 0);
        case11.put("nullGroup_size（应=0）", shallowMapList.get("nullGroup") != null ? shallowMapList.get("nullGroup").size() : 0);
        result.put("11_MapList_浅拷贝_字段映射", case11);

        // ==================== 12. MapList 深拷贝 + 字段映射（::new） ====================
        Map<String, List<MappingTarget>> deepMapList = BeanCopyUtil.copyMapListProperties(sourceMapList, MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case12 = new LinkedHashMap<>();
        case12.put("说明", "MapList深拷贝：children被深拷贝，nickName->displayName，score->point");
        case12.put("group1_size（应=2）", deepMapList.get("group1") != null ? deepMapList.get("group1").size() : 0);
        case12.put("group1_第1个_displayName（应=系统管理）", deepMapList.get("group1") != null && !deepMapList.get("group1").isEmpty() ? deepMapList.get("group1").get(0).getDisplayName() : "null");
        case12.put("group1_第1个_point（应=100）", deepMapList.get("group1") != null && !deepMapList.get("group1").isEmpty() ? deepMapList.get("group1").get(0).getPoint() : "null");
        case12.put("group1_第1个_children数量（应=2）", deepMapList.get("group1") != null && !deepMapList.get("group1").isEmpty() && deepMapList.get("group1").get(0).getChildren2() != null ? deepMapList.get("group1").get(0).getChildren2().size() : 0);
        case12.put("emptyGroup_size（应=0）", deepMapList.get("emptyGroup") != null ? deepMapList.get("emptyGroup").size() : 0);
        case12.put("nullGroup_size（应=0）", deepMapList.get("nullGroup") != null ? deepMapList.get("nullGroup").size() : 0);
        result.put("12_MapList_深拷贝_字段映射", case12);

        // ==================== 13. MapList source=null 测试 ====================
        Map<String, List<MappingTarget>> nullMapList1 = BeanCopyUtil.copyMapListProperties(null, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, List<MappingTarget>> nullMapList2 = BeanCopyUtil.copyMapListProperties(null, MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case13 = new LinkedHashMap<>();
        case13.put("说明", "MapList source为null时应返回空Map");
        case13.put("浅拷贝结果_size（应=0）", nullMapList1.size());
        case13.put("深拷贝结果_size（应=0）", nullMapList2.size());
        result.put("13_MapList_null_测试", case13);

        // ==================== 14. 多字段映射同时生效 ====================
        MappingTarget multiMapping = BeanCopyUtil.copyProperties(root, MappingTarget.class,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(MappingSource::getId, MappingTarget::setId),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren,
                        MappingTarget::setChildren,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTargetChildren.class, // children 里面有个 List，所以一定要深拷贝，不然只会拷贝一层
                                BeanCopyUtil.mapping(MappingSourceChildren::getAge, MappingTargetChildren::setAge),
                                BeanCopyUtil.mapping(MappingSourceChildren::getName, MappingTargetChildren::setName)
                        )
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget.class,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        MappingTarget multiMapping2 = BeanCopyUtil.copyProperties(root, (Supplier<MappingTarget>) MappingTarget::new,
                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                BeanCopyUtil.mapping(MappingSource::getId, MappingTarget::setId),
                BeanCopyUtil.mapping(
                        MappingSource::getSourceNode,
                        MappingTarget::setTargetNode,
                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                ),
                BeanCopyUtil.mapping(
                        MappingSource::getChildren1,
                        MappingTarget::setChildren2,
                        list -> BeanCopyUtil.copyListProperties(list, MappingTarget::new,
                                BeanCopyUtil.mapping(MappingSource::getNickName, MappingTarget::setDisplayName),
                                BeanCopyUtil.mapping(MappingSource::getScore, MappingTarget::setPoint),
                                BeanCopyUtil.mapping(
                                        MappingSource::getSourceNode,
                                        MappingTarget::setTargetNode,
                                        sourceNode -> BeanCopyUtil.copyProperties(sourceNode, MappingTargetNode.class)
                                )
                        )
                )
        );
        Map<String, Object> case14 = new LinkedHashMap<>();
        case14.put("说明", "三个字段映射同时生效：nickName->displayName，score->point，id->id");
        case14.put("displayName（应=系统管理）", multiMapping.getDisplayName());
        case14.put("point（应=100）", multiMapping.getPoint());
        case14.put("id（应=1）", multiMapping.getId());
        result.put("14_多字段映射", case14);

        return Result.success(result);
    }

    /**
     * 字段映射测试用源对象
     * nickName、score 与目标对象字段名不一致，用于验证 mapping 映射
     */
    @Data
    public static class MappingSourceNode {
        private Integer age;
        private String name;
    }

    @Data
    public static class MappingSourceChildren {
        private Integer age;
        private String name;
        private MappingSourceChildren childrenNode;
        private List<MappingSourceChildren> children;
    }

    /**
     * 字段映射测试用源对象
     * nickName、score 与目标对象字段名不一致，用于验证 mapping 映射
     */
    @Data
    public static class MappingSource {
        private Long id;
        private String nickName; // 对应 MappingTarget.displayName
        private Integer score; // 对应 MappingTarget.point
        private Long parentId;
        private MappingSourceNode sourceNode;
        private List<MappingSourceChildren> children;
        private List<MappingSource> children1;
    }

    /**
     * 字段映射测试用源对象
     * nickName、score 与目标对象字段名不一致，用于验证 mapping 映射
     */
    @Data
    public static class MappingTargetNode {
        private Integer age;
        private String name;
    }

    @Data
    public static class MappingTargetChildren {
        private Integer age;
        private String name;
        private MappingTargetChildren childrenNode;
        private List<MappingTargetChildren> children;
    }

    /**
     * 字段映射测试用目标对象
     * displayName、point 与源对象字段名不一致，用于验证 mapping 映射
     */
    @Data
    public static class MappingTarget {
        private Long id;
        private String displayName;
        private Integer point;
        private Long parentId;
        private MappingTargetNode targetNode;
        private List<MappingTargetChildren> children;
        private List<MappingTarget> children2;
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