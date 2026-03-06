package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.core.utils.TreeUtil;
import com.zmbdp.common.domain.domain.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 树形结构工具类功能测试控制器
 * 一键测试 TreeUtil 的所有功能，无需手动传参
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/tree")
public class TestTreeUtilController {

    /**
     * 一键测试所有功能
     * <p>
     * 测试 TreeUtil 的所有功能，包括：构建、遍历、查找、过滤、排序、子树提取、统计分析、安全版本方法
     *
     * @return 详细的测试结果，包含每个功能的测试状态和数据
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> testAll() {
        log.info("=== 一键测试所有树形结构功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        // 准备测试数据
        List<MenuNode> flatList = createTestData();

        // ========== 树形结构构建测试 ==========
        Map<String, Object> buildTest = new LinkedHashMap<>();
        try {
            // 1. 指定根节点 ID 构建树
            List<MenuNode> tree1 = TreeUtil.build(
                    flatList,
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );

            // 完整验证树结构
            Map<String, Object> validation1 = validateTreeStructure(tree1, 0L);
            boolean noDuplicate1 = validateTreeStructureNoDuplicate(tree1);
            boolean isValid1 = (Boolean) validation1.get("验证通过");
            buildTest.put("指定根节点构建-父子关系正确", isValid1 ? "✅ 成功" : "❌ 失败");
            if (!isValid1) {
                buildTest.put("指定根节点构建-父子关系错误", validation1.get("错误详情"));
            }
            buildTest.put("指定根节点构建-无重复节点", noDuplicate1 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(tree1));
            buildTest.put("指定根节点构建-树结构", printTree(tree1));

            // 2. 自动识别根节点构建树
            List<MenuNode> flatList2 = createTestData();
            List<MenuNode> tree2 = TreeUtil.build(
                    flatList2,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );

            Map<String, Object> validation2 = validateTreeStructure(tree2, 0L);
            boolean noDuplicate2 = validateTreeStructureNoDuplicate(tree2);
            boolean isValid2 = (Boolean) validation2.get("验证通过");
            buildTest.put("自动识别根节点-父子关系正确", isValid2 ? "✅ 成功" : "❌ 失败");
            if (!isValid2) {
                buildTest.put("自动识别根节点-父子关系错误", validation2.get("错误详情"));
            }
            buildTest.put("自动识别根节点-无重复节点", noDuplicate2 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(tree2));

            // 3. 空列表测试
            List<MenuNode> emptyTree = TreeUtil.build(
                    new ArrayList<>(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            buildTest.put("空列表构建", emptyTree.isEmpty() ? "✅ 成功" : "❌ 失败");

            // 4. 重复构建测试
            List<MenuNode> flatList3 = createTestData();
            TreeUtil.build(flatList3, 0L, MenuNode::getId, MenuNode::getParentId, MenuNode::getChildren, MenuNode::setChildren);
            List<MenuNode> tree3 = TreeUtil.build(flatList3, 0L, MenuNode::getId, MenuNode::getParentId, MenuNode::getChildren, MenuNode::setChildren);

            Map<String, Object> validation3 = validateTreeStructure(tree3, 0L);
            boolean noDuplicate3 = validateTreeStructureNoDuplicate(tree3);
            boolean isValid3 = (Boolean) validation3.get("验证通过");
            buildTest.put("重复构建测试-父子关系正确", isValid3 ? "✅ 成功" : "❌ 失败");
            if (!isValid3) {
                buildTest.put("重复构建测试-父子关系错误", validation3.get("错误详情"));
            }
            buildTest.put("重复构建测试-无重复节点", noDuplicate3 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(tree3));

        } catch (Exception e) {
            buildTest.put("错误", "❌ 异常: " + e.getMessage());
            e.printStackTrace();
        }
        result.put("树形结构构建", buildTest);

        // ========== 树形结构遍历测试 ==========
        Map<String, Object> traverseTest = new LinkedHashMap<>();
        try {
            List<MenuNode> tree = TreeUtil.build(
                    flatList,
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );

            // 1. 深度优先遍历
            List<String> dfsResult = new ArrayList<>();
            TreeUtil.traverseDepthFirst(tree, MenuNode::getChildren, node -> dfsResult.add(node.getName()));
            traverseTest.put("深度优先遍历", !dfsResult.isEmpty() ? "✅ 成功，遍历节点数: " + dfsResult.size() : "❌ 失败");
            traverseTest.put("深度优先遍历顺序", String.join(" -> ", dfsResult));

            // 2. 广度优先遍历
            List<String> bfsResult = new ArrayList<>();
            TreeUtil.traverseBreadthFirst(tree, MenuNode::getChildren, node -> bfsResult.add(node.getName()));
            traverseTest.put("广度优先遍历", !bfsResult.isEmpty() ? "✅ 成功，遍历节点数: " + bfsResult.size() : "❌ 失败");
            traverseTest.put("广度优先遍历顺序", String.join(" -> ", bfsResult));

            // 3. 树转列表
            List<MenuNode> flatResult = TreeUtil.toList(tree, MenuNode::getChildren);
            traverseTest.put("树转列表", !flatResult.isEmpty() ? "✅ 成功，节点数: " + flatResult.size() : "❌ 失败");

            // 4. 树转 Map（新增测试）
            Map<Long, MenuNode> nodeMap = TreeUtil.toMap(tree, MenuNode::getId, MenuNode::getChildren);
            boolean mapCorrect = nodeMap.size() == flatResult.size();
            traverseTest.put("树转Map", mapCorrect ? "✅ 成功，Map大小: " + nodeMap.size() : "❌ 失败");

            // 验证 Map 中的节点是否正确
            MenuNode node3 = nodeMap.get(3L);
            boolean node3Correct = node3 != null && "角色管理".equals(node3.getName());
            traverseTest.put("树转Map-查找节点3", node3Correct ? "✅ 成功，找到: " + node3.getName() : "❌ 失败");

            MenuNode node5 = nodeMap.get(5L);
            boolean node5Correct = node5 != null && "权限分配".equals(node5.getName());
            traverseTest.put("树转Map-查找节点5", node5Correct ? "✅ 成功，找到: " + node5.getName() : "❌ 失败");

            // 5. 添加层级信息（新增测试）
            List<MenuNode> treeForLevel = TreeUtil.build(
                    createTestData(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            TreeUtil.enrichWithLevel(treeForLevel, MenuNode::getChildren, MenuNode::setLevel);

            // 验证层级信息
            List<String> levelInfo = new ArrayList<>();
            TreeUtil.traverseDepthFirst(treeForLevel, MenuNode::getChildren, node -> {
                levelInfo.add(node.getName() + "(层级" + node.getLevel() + ")");
            });
            traverseTest.put("添加层级信息", "✅ 成功");
            traverseTest.put("层级信息详情", String.join(", ", levelInfo));

            // 验证具体节点的层级
            MenuNode rootNode = TreeUtil.findFirst(treeForLevel, MenuNode::getChildren, n -> n.getId().equals(1L));
            MenuNode level2Node = TreeUtil.findFirst(treeForLevel, MenuNode::getChildren, n -> n.getId().equals(2L));
            MenuNode level3Node = TreeUtil.findFirst(treeForLevel, MenuNode::getChildren, n -> n.getId().equals(4L));

            boolean levelCorrect = rootNode != null && rootNode.getLevel() == 1 &&
                    level2Node != null && level2Node.getLevel() == 2 &&
                    level3Node != null && level3Node.getLevel() == 3;
            traverseTest.put("层级验证", levelCorrect ?
                    "✅ 成功，根节点层级=" + (rootNode != null ? rootNode.getLevel() : "null") +
                            ", 二级节点层级=" + (level2Node != null ? level2Node.getLevel() : "null") +
                            ", 三级节点层级=" + (level3Node != null ? level3Node.getLevel() : "null") :
                    "❌ 失败");

        } catch (Exception e) {
            traverseTest.put("错误", "❌ 树形结构遍历测试异常: " + e.getMessage());
            e.printStackTrace();
        }
        result.put("树形结构遍历", traverseTest);

        // ========== 树形结构查找测试 ==========
        Map<String, Object> findTest = new LinkedHashMap<>();
        try {
            List<MenuNode> tree = TreeUtil.build(
                    flatList,
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );

            // 1. 查找第一个节点
            MenuNode foundNode = TreeUtil.findFirst(tree, MenuNode::getChildren, n -> n.getId().equals(3L));
            findTest.put("查找第一个节点", foundNode != null ? "✅ 成功，找到: " + foundNode.getName() : "❌ 失败");

            // 2. 查找所有启用的节点
            List<MenuNode> enabledNodes = TreeUtil.findAll(tree, MenuNode::getChildren, MenuNode::getEnabled);
            findTest.put("查找所有启用节点", !enabledNodes.isEmpty() ? "✅ 成功，找到: " + enabledNodes.size() + " 个" : "❌ 失败");

            // 3. 查找路径
            List<MenuNode> path = TreeUtil.findPath(tree, MenuNode::getChildren, n -> n.getId().equals(5L));
            String pathStr = path.stream().map(MenuNode::getName).collect(Collectors.joining(" -> "));
            findTest.put("查找节点路径", !path.isEmpty() ? "✅ 成功，路径: " + pathStr : "❌ 失败");

            // 4. 判断是否包含节点
            boolean contains = TreeUtil.contains(tree, MenuNode::getChildren, n -> n.getName().equals("用户管理"));
            findTest.put("判断是否包含节点", contains ? "✅ 成功，找到节点" : "❌ 失败");

            // 5. 获取祖先节点
            List<MenuNode> ancestors = TreeUtil.getAncestors(tree, MenuNode::getChildren, n -> n.getId().equals(5L));
            String ancestorsStr = ancestors.stream().map(MenuNode::getName).collect(Collectors.joining(" -> "));
            findTest.put("获取祖先节点", !ancestors.isEmpty() ? "✅ 成功，祖先: " + ancestorsStr : "✅ 成功，无祖先（根节点）");

            // 6. 获取后代节点
            List<MenuNode> descendants = TreeUtil.getDescendants(tree, MenuNode::getChildren, n -> n.getId().equals(1L));
            findTest.put("获取后代节点", !descendants.isEmpty() ? "✅ 成功，后代数: " + descendants.size() : "✅ 成功，无后代（叶子节点）");

        } catch (Exception e) {
            findTest.put("错误", "❌ 树形结构查找测试异常: " + e.getMessage());
        }
        result.put("树形结构查找", findTest);

        // ========== 树形结构操作测试 ==========
        Map<String, Object> operationTest = new LinkedHashMap<>();
        try {
            // 1. 获取所有叶子节点
            List<MenuNode> tree1 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> leafNodes = TreeUtil.getLeafNodes(tree1, MenuNode::getChildren);
            String leafNames = leafNodes.stream().map(MenuNode::getName).collect(Collectors.joining(", "));
            operationTest.put("获取叶子节点", !leafNodes.isEmpty() ? "✅ 成功，叶子节点: " + leafNames : "❌ 失败");

            // 2. 过滤树（只保留启用的节点）
            List<MenuNode> tree2 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> filteredTree = TreeUtil.filter(
                    tree2,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    MenuNode::getEnabled
            );
            int filteredCount = TreeUtil.countNodes(filteredTree, MenuNode::getChildren);
            operationTest.put("过滤树结构", filteredCount > 0 ? "✅ 成功，过滤后节点数: " + filteredCount : "❌ 失败");

            // 3. 排序树（按 sort 字段升序）
            List<MenuNode> tree3 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> sortedTree = TreeUtil.sort(
                    tree3,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    Comparator.comparing(MenuNode::getSort)
            );
            List<String> sortedNames = new ArrayList<>();
            TreeUtil.traverseDepthFirst(sortedTree, MenuNode::getChildren, node -> sortedNames.add(node.getName()));
            operationTest.put("排序树结构", !sortedNames.isEmpty() ? "✅ 成功，排序后顺序: " + String.join(" -> ", sortedNames) : "❌ 失败");

        } catch (Exception e) {
            operationTest.put("错误", "❌ 树形结构操作测试异常: " + e.getMessage());
        }
        result.put("树形结构操作", operationTest);

        // ========== 树形结构统计测试 ==========
        Map<String, Object> statisticsTest = new LinkedHashMap<>();
        try {
            List<MenuNode> tree = TreeUtil.build(
                    flatList,
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );

            // 1. 获取最大深度
            int maxDepth = TreeUtil.getMaxDepth(tree, MenuNode::getChildren);
            statisticsTest.put("获取最大深度", maxDepth > 0 ? "✅ 成功，最大深度: " + maxDepth : "❌ 失败");

            // 2. 统计节点总数
            int totalCount = TreeUtil.countNodes(tree, MenuNode::getChildren);
            statisticsTest.put("统计节点总数", totalCount > 0 ? "✅ 成功，节点总数: " + totalCount : "❌ 失败");

            // 3. 验证节点数是否正确
            boolean countCorrect = totalCount == flatList.size();
            statisticsTest.put("验证节点数", countCorrect ? "✅ 成功，节点数正确" : "❌ 失败，节点数不匹配");

        } catch (Exception e) {
            statisticsTest.put("错误", "❌ 树形结构统计测试异常: " + e.getMessage());
        }
        result.put("树形结构统计", statisticsTest);

        // ========== 子树获取测试 ==========
        Map<String, Object> subTreeTest = new LinkedHashMap<>();
        try {
            // 1. 获取所有根节点及其下2层数据
            List<MenuNode> tree1 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> subTree2Levels = TreeUtil.getSubTree(
                    tree1,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    2
            );
            int count2Levels = TreeUtil.countNodes(subTree2Levels, MenuNode::getChildren);
            subTreeTest.put("获取根节点下2层", count2Levels > 0 ? "✅ 成功，节点数: " + count2Levels : "❌ 失败");

            // 2. 获取指定节点(ID=1)及其下1层数据
            List<MenuNode> tree2 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> subTreeById = TreeUtil.getSubTree(
                    tree2,
                    MenuNode::getId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    1L,
                    1
            );
            int countById = TreeUtil.countNodes(subTreeById, MenuNode::getChildren);
            subTreeTest.put("获取节点1下1层", countById > 0 ? "✅ 成功，节点数: " + countById : "❌ 失败");

            // 3. 获取指定节点(ID=1)及其下2层数据（使用新的树对象）
            List<MenuNode> tree2b = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> subTreeById2 = TreeUtil.getSubTree(
                    tree2b,
                    MenuNode::getId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    1L,
                    2
            );
            int countById2 = TreeUtil.countNodes(subTreeById2, MenuNode::getChildren);
            subTreeTest.put("获取节点1下2层", countById2 > 0 ? "✅ 成功，节点数: " + countById2 : "❌ 失败");

            // 4. 获取只有1层（只有根节点）
            List<MenuNode> tree3 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> subTree1Level = TreeUtil.getSubTree(
                    tree3,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    1
            );
            int count1Level = TreeUtil.countNodes(subTree1Level, MenuNode::getChildren);
            subTreeTest.put("获取所有根节点下1层", count1Level > 0 ? "✅ 成功，节点数: " + count1Level : "❌ 失败");

            // 5. 获取不存在的节点
            List<MenuNode> tree4 = TreeUtil.build(
                    createTestData(), // 每次都用新数据
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            List<MenuNode> notFound = TreeUtil.getSubTree(
                    tree4,
                    MenuNode::getId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    999L,
                    2
            );
            subTreeTest.put("获取不存在节点", notFound.isEmpty() ? "✅ 成功，返回空列表" : "❌ 失败");

        } catch (Exception e) {
            subTreeTest.put("错误", "❌ 子树获取测试异常: " + e.getMessage());
        }
        result.put("子树获取", subTreeTest);

        // ========== 安全版本方法测试 ==========
        Map<String, Object> safeMethodsTest = new LinkedHashMap<>();
        try {
            // 1. buildSafe 测试（指定根节点）
            List<MenuNode> originalList1 = createTestData();
            String originalStructure1 = captureTreeStructure(originalList1);

            List<MenuNode> treeSafe1 = TreeUtil.buildSafe(
                    originalList1,
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    MenuNode::new
            );

            String afterStructure1 = captureTreeStructure(originalList1);
            Map<String, Object> validation1 = validateTreeStructure(treeSafe1, 0L);
            boolean noDuplicate1 = validateTreeStructureNoDuplicate(treeSafe1);
            boolean isValid1 = (Boolean) validation1.get("验证通过");

            safeMethodsTest.put("buildSafe(指定根节点)-父子关系正确", isValid1 ? "✅ 成功" : "❌ 失败");
            if (!isValid1) {
                safeMethodsTest.put("buildSafe(指定根节点)-父子关系错误", validation1.get("错误详情"));
            }
            safeMethodsTest.put("buildSafe(指定根节点)-无重复节点", noDuplicate1 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(treeSafe1));
            safeMethodsTest.put("buildSafe(指定根节点)-原列表未改变",
                    originalStructure1.equals(afterStructure1) ? "✅ 成功" : "❌ 失败，原列表被修改");

            // 2. buildSafe 测试（自动识别根节点）
            List<MenuNode> originalList2 = createTestData();
            String originalStructure2 = captureTreeStructure(originalList2);

            List<MenuNode> treeSafe2 = TreeUtil.buildSafe(
                    originalList2,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    MenuNode::new
            );

            String afterStructure2 = captureTreeStructure(originalList2);
            Map<String, Object> validation2 = validateTreeStructure(treeSafe2, 0L);
            boolean noDuplicate2 = validateTreeStructureNoDuplicate(treeSafe2);
            boolean isValid2 = (Boolean) validation2.get("验证通过");

            safeMethodsTest.put("buildSafe(自动识别)-父子关系正确", isValid2 ? "✅ 成功" : "❌ 失败");
            if (!isValid2) {
                safeMethodsTest.put("buildSafe(自动识别)-父子关系错误", validation2.get("错误详情"));
            }
            safeMethodsTest.put("buildSafe(自动识别)-无重复节点", noDuplicate2 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(treeSafe2));
            safeMethodsTest.put("buildSafe(自动识别)-原列表未改变",
                    originalStructure2.equals(afterStructure2) ? "✅ 成功" : "❌ 失败，原列表被修改");

            // 3. filterSafe 测试
            List<MenuNode> treeForFilter = TreeUtil.build(
                    createTestData(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            String beforeFilterStructure = captureTreeStructure(treeForFilter);

            List<MenuNode> filteredSafe = TreeUtil.filterSafe(
                    treeForFilter,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    MenuNode::getEnabled,
                    MenuNode::new
            );

            String afterFilterStructure = captureTreeStructure(treeForFilter);
            Map<String, Object> filterValidation = validateTreeStructure(filteredSafe, 0L);
            boolean noDuplicateFilter = validateTreeStructureNoDuplicate(filteredSafe);
            boolean isValidFilter = (Boolean) filterValidation.get("验证通过");

            safeMethodsTest.put("filterSafe-过滤后父子关系正确", isValidFilter ? "✅ 成功" : "❌ 失败");
            if (!isValidFilter) {
                safeMethodsTest.put("filterSafe-父子关系错误", filterValidation.get("错误详情"));
            }
            safeMethodsTest.put("filterSafe-过滤后无重复节点", noDuplicateFilter ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(filteredSafe));
            safeMethodsTest.put("filterSafe-原树未改变",
                    beforeFilterStructure.equals(afterFilterStructure) ? "✅ 成功" : "❌ 失败，原树被修改");
            safeMethodsTest.put("filterSafe-过滤结果",
                    "过滤后节点数: " + TreeUtil.countNodes(filteredSafe, MenuNode::getChildren));

            // 4. sortSafe 测试
            List<MenuNode> treeForSort = TreeUtil.build(
                    createTestData(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            String beforeSortStructure = captureTreeStructure(treeForSort);

            List<MenuNode> sortedSafe = TreeUtil.sortSafe(
                    treeForSort,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    Comparator.comparing(MenuNode::getSort),
                    MenuNode::new
            );

            String afterSortStructure = captureTreeStructure(treeForSort);
            Map<String, Object> sortValidation = validateTreeStructure(sortedSafe, 0L);
            boolean noDuplicateSort = validateTreeStructureNoDuplicate(sortedSafe);
            boolean isValidSort = (Boolean) sortValidation.get("验证通过");

            safeMethodsTest.put("sortSafe-排序后父子关系正确", isValidSort ? "✅ 成功" : "❌ 失败");
            if (!isValidSort) {
                safeMethodsTest.put("sortSafe-父子关系错误", sortValidation.get("错误详情"));
            }
            safeMethodsTest.put("sortSafe-排序后无重复节点", noDuplicateSort ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(sortedSafe));
            safeMethodsTest.put("sortSafe-原树未改变",
                    beforeSortStructure.equals(afterSortStructure) ? "✅ 成功" : "❌ 失败，原树被修改");

            // 5. getSubTreeSafe 测试（按层级）
            List<MenuNode> treeForSubTree1 = TreeUtil.build(
                    createTestData(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            String beforeSubTree1Structure = captureTreeStructure(treeForSubTree1);

            List<MenuNode> subTreeSafe1 = TreeUtil.getSubTreeSafe(
                    treeForSubTree1,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    2,
                    MenuNode::new
            );

            String afterSubTree1Structure = captureTreeStructure(treeForSubTree1);
            boolean noDuplicateSubTree1 = validateTreeStructureNoDuplicate(subTreeSafe1);

            safeMethodsTest.put("getSubTreeSafe(层级)-原树未改变",
                    beforeSubTree1Structure.equals(afterSubTree1Structure) ? "✅ 成功" : "❌ 失败，原树被修改");
            safeMethodsTest.put("getSubTreeSafe(层级)-无重复节点", noDuplicateSubTree1 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(subTreeSafe1));
            safeMethodsTest.put("getSubTreeSafe(层级)-结果",
                    "获取到 " + TreeUtil.countNodes(subTreeSafe1, MenuNode::getChildren) + " 个节点");

            // 6. getSubTreeSafe 测试（按节点ID）
            List<MenuNode> treeForSubTree2 = TreeUtil.build(
                    createTestData(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            String beforeSubTree2Structure = captureTreeStructure(treeForSubTree2);

            List<MenuNode> subTreeSafe2 = TreeUtil.getSubTreeSafe(
                    treeForSubTree2,
                    MenuNode::getId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    1L,
                    2,
                    MenuNode::new
            );

            String afterSubTree2Structure = captureTreeStructure(treeForSubTree2);
            boolean noDuplicateSubTree2 = validateTreeStructureNoDuplicate(subTreeSafe2);

            safeMethodsTest.put("getSubTreeSafe(节点ID)-原树未改变",
                    beforeSubTree2Structure.equals(afterSubTree2Structure) ? "✅ 成功" : "❌ 失败，原树被修改");
            safeMethodsTest.put("getSubTreeSafe(节点ID)-无重复节点", noDuplicateSubTree2 ? "✅ 成功" : "❌ 失败: " + getTreeValidationDetails(subTreeSafe2));
            safeMethodsTest.put("getSubTreeSafe(节点ID)-结果",
                    "获取到 " + TreeUtil.countNodes(subTreeSafe2, MenuNode::getChildren) + " 个节点");

            // 7. 多次调用安全版本测试
            List<MenuNode> treeForMultiple = TreeUtil.build(
                    createTestData(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            String originalMultipleStructure = captureTreeStructure(treeForMultiple);

            // 第1次调用：获取节点1下1层
            List<MenuNode> result1 = TreeUtil.getSubTreeSafe(treeForMultiple, MenuNode::getId, MenuNode::getChildren, MenuNode::setChildren, 1L, 1, MenuNode::new);
            String afterCall1 = captureTreeStructure(treeForMultiple);
            boolean noDuplicateResult1 = validateTreeStructureNoDuplicate(result1);
            int count1 = TreeUtil.countNodes(result1, MenuNode::getChildren);

            // 第2次调用：获取节点1下2层
            List<MenuNode> result2 = TreeUtil.getSubTreeSafe(treeForMultiple, MenuNode::getId, MenuNode::getChildren, MenuNode::setChildren, 1L, 2, MenuNode::new);
            String afterCall2 = captureTreeStructure(treeForMultiple);
            boolean noDuplicateResult2 = validateTreeStructureNoDuplicate(result2);
            int count2 = TreeUtil.countNodes(result2, MenuNode::getChildren);

            // 第3次调用：过滤启用的节点
            List<MenuNode> result3 = TreeUtil.filterSafe(treeForMultiple, MenuNode::getChildren, MenuNode::setChildren, MenuNode::getEnabled, MenuNode::new);
            String afterCall3 = captureTreeStructure(treeForMultiple);
            Map<String, Object> result3Validation = validateTreeStructure(result3, 0L);
            boolean noDuplicateResult3 = validateTreeStructureNoDuplicate(result3);
            boolean isValidResult3 = (Boolean) result3Validation.get("验证通过");
            int count3 = TreeUtil.countNodes(result3, MenuNode::getChildren);

            boolean unchanged1 = originalMultipleStructure.equals(afterCall1);
            boolean unchanged2 = originalMultipleStructure.equals(afterCall2);
            boolean unchanged3 = originalMultipleStructure.equals(afterCall3);

            safeMethodsTest.put("多次调用-第1次结果正确",
                    (count1 > 0 && noDuplicateResult1) ? "✅ 成功，节点数: " + count1 : "❌ 失败");
            safeMethodsTest.put("多次调用-第1次原树未改变", unchanged1 ? "✅ 成功" : "❌ 失败");

            safeMethodsTest.put("多次调用-第2次结果正确",
                    (count2 > 0 && noDuplicateResult2) ? "✅ 成功，节点数: " + count2 : "❌ 失败");
            safeMethodsTest.put("多次调用-第2次原树未改变", unchanged2 ? "✅ 成功" : "❌ 失败");

            safeMethodsTest.put("多次调用-第3次结果父子关系正确", isValidResult3 ? "✅ 成功" : "❌ 失败");
            if (!isValidResult3) {
                safeMethodsTest.put("多次调用-第3次父子关系错误", result3Validation.get("错误详情"));
            }
            safeMethodsTest.put("多次调用-第3次结果无重复",
                    noDuplicateResult3 ? "✅ 成功，节点数: " + count3 : "❌ 失败: " + getTreeValidationDetails(result3));
            safeMethodsTest.put("多次调用-第3次原树未改变", unchanged3 ? "✅ 成功" : "❌ 失败");

            safeMethodsTest.put("多次调用-总结",
                    (unchanged1 && unchanged2 && unchanged3 && noDuplicateResult1 && noDuplicateResult2 && noDuplicateResult3)
                            ? "✅ 所有调用结果正确且未修改原树" : "❌ 存在问题");

        } catch (Exception e) {
            safeMethodsTest.put("错误", "❌ 安全版本方法测试异常: " + e.getMessage());
            e.printStackTrace();
        }
        result.put("安全版本方法", safeMethodsTest);

        log.info("=== 测试完成 ===");
        return Result.success(result);
    }

    /*=============================================    一键测试接口    =============================================*/

    /**
     * 测试树形结构构建
     *
     * @return 测试结果
     */
    @GetMapping("/build")
    public Result<Map<String, Object>> testBuild() {
        log.info("=== 测试树形结构构建 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        List<MenuNode> flatList = createTestData();

        // 指定根节点 ID 构建树
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        List<MenuNode> tree2 = TreeUtil.buildSafe(
                flatList,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren,
                MenuNode::new
        );

        result.put("原始数据", flatList);
        result.put("树形结构", tree);
        result.put("根节点数", tree.size());

        return Result.success(result);
    }

    /*=============================================    单独测试接口    =============================================*/

    /**
     * 测试树形结构遍历
     *
     * @return 测试结果
     */
    @GetMapping("/traverse")
    public Result<Map<String, Object>> testTraverse() {
        log.info("=== 测试树形结构遍历 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        List<MenuNode> flatList = createTestData();
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        // 深度优先遍历
        List<String> dfsResult = new ArrayList<>();
        TreeUtil.traverseDepthFirst(tree, MenuNode::getChildren, node -> dfsResult.add(node.getName()));

        // 广度优先遍历
        List<String> bfsResult = new ArrayList<>();
        TreeUtil.traverseBreadthFirst(tree, MenuNode::getChildren, node -> bfsResult.add(node.getName()));

        result.put("深度优先遍历", dfsResult);
        result.put("广度优先遍历", bfsResult);

        return Result.success(result);
    }

    /**
     * 测试树形结构查找
     *
     * @param nodeId 节点 ID
     * @return 测试结果
     */
    @GetMapping("/find")
    public Result<Map<String, Object>> testFind(@RequestParam(defaultValue = "3") Long nodeId) {
        log.info("=== 测试树形结构查找，节点ID: {} ===", nodeId);
        Map<String, Object> result = new LinkedHashMap<>();

        List<MenuNode> flatList = createTestData();
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        // 查找节点
        MenuNode foundNode = TreeUtil.findFirst(tree, MenuNode::getChildren, n -> n.getId().equals(nodeId));

        if (foundNode != null) {
            // 查找路径
            List<MenuNode> path = TreeUtil.findPath(tree, MenuNode::getChildren, n -> n.getId().equals(nodeId));
            String pathStr = path.stream().map(MenuNode::getName).collect(Collectors.joining(" -> "));

            // 获取祖先节点
            List<MenuNode> ancestors = TreeUtil.getAncestors(tree, MenuNode::getChildren, n -> n.getId().equals(nodeId));

            // 获取后代节点
            List<MenuNode> descendants = TreeUtil.getDescendants(tree, MenuNode::getChildren, n -> n.getId().equals(nodeId));

            result.put("找到的节点", foundNode);
            result.put("节点路径", pathStr);
            result.put("祖先节点", ancestors);
            result.put("后代节点", descendants);
        } else {
            result.put("结果", "未找到节点");
        }

        return Result.success(result);
    }

    /**
     * 测试树形结构过滤
     *
     * @return 测试结果
     */
    @GetMapping("/filter")
    public Result<Map<String, Object>> testFilter() {
        log.info("=== 测试树形结构过滤 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        // 每次都创建新的数据，避免被修改
        List<MenuNode> flatList = createTestData();
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        // 过滤：只保留启用的节点
        List<MenuNode> flatList2 = createTestData();  // 重新创建数据
        List<MenuNode> tree2 = TreeUtil.build(
                flatList2,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );
        List<MenuNode> filteredTree = TreeUtil.filter(
                tree2,
                MenuNode::getChildren,
                MenuNode::setChildren,
                MenuNode::getEnabled
        );

        result.put("原始树", tree);
        result.put("过滤后的树", filteredTree);
        result.put("原始节点数", TreeUtil.countNodes(tree, MenuNode::getChildren));
        result.put("过滤后节点数", TreeUtil.countNodes(filteredTree, MenuNode::getChildren));

        return Result.success(result);
    }

    /**
     * 测试树形结构排序
     *
     * @return 测试结果
     */
    @GetMapping("/sort")
    public Result<Map<String, Object>> testSort() {
        log.info("=== 测试树形结构排序 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        List<MenuNode> flatList = createTestData();
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        // 获取排序前的顺序
        List<String> beforeSort = new ArrayList<>();
        TreeUtil.traverseDepthFirst(tree, MenuNode::getChildren, node -> beforeSort.add(node.getName()));

        // 排序：按 sort 字段升序（使用新数据）
        List<MenuNode> flatList2 = createTestData();  // 重新创建数据
        List<MenuNode> tree2 = TreeUtil.build(
                flatList2,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );
        List<MenuNode> sortedTree = TreeUtil.sort(
                tree2,
                MenuNode::getChildren,
                MenuNode::setChildren,
                Comparator.comparing(MenuNode::getSort)
        );

        List<String> afterSort = new ArrayList<>();
        TreeUtil.traverseDepthFirst(sortedTree, MenuNode::getChildren, node -> afterSort.add(node.getName()));

        result.put("排序前顺序", beforeSort);
        result.put("排序后顺序", afterSort);
        result.put("排序后的树", sortedTree);

        return Result.success(result);
    }

    /**
     * 测试树形结构统计
     *
     * @return 测试结果
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> testStatistics() {
        log.info("=== 测试树形结构统计 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        List<MenuNode> flatList = createTestData();
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        // 统计信息
        int maxDepth = TreeUtil.getMaxDepth(tree, MenuNode::getChildren);
        int totalCount = TreeUtil.countNodes(tree, MenuNode::getChildren);
        List<MenuNode> leafNodes = TreeUtil.getLeafNodes(tree, MenuNode::getChildren);

        result.put("最大深度", maxDepth);
        result.put("节点总数", totalCount);
        result.put("叶子节点数", leafNodes.size());
        result.put("叶子节点", leafNodes.stream().map(MenuNode::getName).collect(Collectors.toList()));

        return Result.success(result);
    }

    /**
     * 测试获取指定层级的子树
     *
     * @param nodeId 节点ID（可选）
     * @param levels 层级数
     * @return 测试结果
     */
    @GetMapping("/subtree")
    public Result<Map<String, Object>> testSubTree(
            @RequestParam(required = false) Long nodeId,
            @RequestParam(defaultValue = "2") int levels
    ) {
        log.info("=== 测试获取子树，节点ID: {}, 层级: {} ===", nodeId, levels);
        Map<String, Object> result = new LinkedHashMap<>();

        // 每次都创建新的数据
        List<MenuNode> flatList = createTestData();
        List<MenuNode> tree = TreeUtil.build(
                flatList,
                0L,
                MenuNode::getId,
                MenuNode::getParentId,
                MenuNode::getChildren,
                MenuNode::setChildren
        );

        List<MenuNode> subTree;
        if (nodeId == null) {
            // 获取所有根节点及其指定层级的子树
            subTree = TreeUtil.getSubTree(
                    tree,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    levels
            );
            result.put("说明", "获取所有根节点及其下 " + levels + " 层的数据");
        } else {
            // 获取指定节点及其指定层级的子树
            subTree = TreeUtil.getSubTree(
                    tree,
                    MenuNode::getId,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    nodeId,
                    levels
            );
            result.put("说明", "获取节点 " + nodeId + " 及其下 " + levels + " 层的数据");
        }

        result.put("子树结构", subTree);
        result.put("子树节点数", TreeUtil.countNodes(subTree, MenuNode::getChildren));
        result.put("子树最大深度", TreeUtil.getMaxDepth(subTree, MenuNode::getChildren));

        return Result.success(result);
    }

    /**
     * 创建测试数据
     * <p>
     * 模拟一个包含 9 个节点的菜单树结构：
     * <ul>
     *     <li>第一层：系统管理(1)、内容管理(6)、设置(9)</li>
     *     <li>第二层：用户管理(2)、角色管理(3)、文章管理(7)、分类管理(8)</li>
     *     <li>第三层：角色列表(4)、权限分配(5)</li>
     * </ul>
     *
     * @return 扁平化的节点列表
     */
    private List<MenuNode> createTestData() {
        List<MenuNode> list = new ArrayList<>();

        // 第一层
        list.add(new MenuNode(1L, 0L, "系统管理", 1, true));
        list.add(new MenuNode(6L, 0L, "内容管理", 2, true));
        list.add(new MenuNode(9L, 0L, "设置", 3, false));

        // 第二层
        list.add(new MenuNode(2L, 1L, "用户管理", 1, true));
        list.add(new MenuNode(3L, 1L, "角色管理", 2, true));
        list.add(new MenuNode(7L, 6L, "文章管理", 1, true));
        list.add(new MenuNode(8L, 6L, "分类管理", 2, false));

        // 第三层
        list.add(new MenuNode(4L, 3L, "角色列表", 1, true));
        list.add(new MenuNode(5L, 3L, "权限分配", 2, true));

        return list;
    }

    /*=============================================    辅助方法    =============================================*/

    /**
     * 捕获树结构的完整快照（用于比较前后是否改变）
     * <p>
     * 递归遍历树结构，生成包含每个节点完整信息的字符串表示，
     * 包括节点的 ID、parentId、name、sort、enabled 以及 children 的完整结构
     *
     * @param obj 要捕获的对象（可以是 List 或 MenuNode）
     * @return 树结构的字符串表示
     */
    private String captureTreeStructure(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(captureTreeStructure(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof MenuNode node) {
            StringBuilder sb = new StringBuilder();
            sb.append("{id=").append(node.getId());
            sb.append(",pid=").append(node.getParentId());
            sb.append(",name=").append(node.getName());
            sb.append(",sort=").append(node.getSort());
            sb.append(",enabled=").append(node.getEnabled());

            List<MenuNode> children = node.getChildren();
            if (children == null) {
                sb.append(",children=null");
            } else if (children.isEmpty()) {
                sb.append(",children=[]");
            } else {
                sb.append(",children=[");
                for (int i = 0; i < children.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(captureTreeStructure(children.get(i)));
                }
                sb.append("]");
            }
            sb.append("}");
            return sb.toString();
        }

        return obj.toString();
    }

    /**
     * 完整验证树结构（检查每个节点的 ID、parentId、children 是否正确）
     * <p>
     * 递归验证树中每个节点的父子关系是否正确，确保子节点的 parentId 与父节点的 id 一致
     *
     * @param tree                 树形结构的根节点列表
     * @param expectedRootParentId 期望的根节点 parentId
     * @return 验证结果，包含"验证通过"和"错误详情"（如果有错误）
     */
    private Map<String, Object> validateTreeStructure(List<MenuNode> tree, Long expectedRootParentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        if (tree == null) {
            errors.add("树结构为null");
            result.put("验证通过", false);
            result.put("错误详情", errors);
            return result;
        }

        // 验证根节点
        for (MenuNode root : tree) {
            if (!Objects.equals(root.getParentId(), expectedRootParentId)) {
                errors.add("根节点ID=" + root.getId() + " 的parentId=" + root.getParentId() + "，期望=" + expectedRootParentId);
            }
            validateNode(root, errors);
        }

        result.put("验证通过", errors.isEmpty());
        if (!errors.isEmpty()) {
            result.put("错误详情", errors);
        }
        return result;
    }

    /**
     * 递归验证节点及其子节点
     * <p>
     * 检查节点的 children 是否为 null，以及每个子节点的 parentId 是否正确指向父节点
     *
     * @param node   当前节点
     * @param errors 错误信息列表
     */
    private void validateNode(MenuNode node, List<String> errors) {
        if (node == null) {
            errors.add("发现null节点");
            return;
        }

        List<MenuNode> children = node.getChildren();
        if (children == null) {
            errors.add("节点ID=" + node.getId() + " 的children为null，应该是空列表");
            return;
        }

        // 验证每个子节点的parentId是否指向当前节点
        for (MenuNode child : children) {
            if (child == null) {
                errors.add("节点ID=" + node.getId() + " 的children中包含null元素");
                continue;
            }

            if (!Objects.equals(child.getParentId(), node.getId())) {
                errors.add("子节点ID=" + child.getId() + " 的parentId=" + child.getParentId() +
                        "，但其父节点ID=" + node.getId() + "，不匹配！");
            }
            // 递归验证子节点
            validateNode(child, errors);
        }
    }

    /**
     * 打印树结构（用于调试）
     * <p>
     * 以缩进的方式打印整棵树的结构，显示每个节点的 ID、parentId、name 和子节点数量
     *
     * @param tree 树形结构的根节点列表
     * @return 格式化的树结构字符串
     */
    private String printTree(List<MenuNode> tree) {
        StringBuilder sb = new StringBuilder();
        for (MenuNode node : tree) {
            printNode(node, 0, sb);
        }
        return sb.toString();
    }

    /**
     * 递归打印单个节点及其子节点
     * <p>
     * 按层级缩进打印节点信息
     *
     * @param node  当前节点
     * @param level 当前层级（用于控制缩进）
     * @param sb    字符串构建器
     */
    private void printNode(MenuNode node, int level, StringBuilder sb) {
        String indent = "  ".repeat(level);
        sb.append(indent).append("├─ ID=").append(node.getId())
                .append(", parentId=").append(node.getParentId())
                .append(", name=").append(node.getName())
                .append(", children=").append(node.getChildren() == null ? "null" : node.getChildren().size())
                .append("\n");

        if (node.getChildren() != null) {
            for (MenuNode child : node.getChildren()) {
                printNode(child, level + 1, sb);
            }
        }
    }

    /**
     * 验证树结构中是否有重复节点
     * <p>
     * 遍历整棵树，检查是否存在 ID 重复的节点
     *
     * @param tree 树形结构的根节点列表
     * @return true 表示无重复节点，false 表示存在重复节点
     */
    private boolean validateTreeStructureNoDuplicate(List<MenuNode> tree) {
        Set<Long> visitedIds = new HashSet<>();
        return validateNodeNoDuplicate(tree, visitedIds);
    }

    /**
     * 递归验证节点列表中是否有重复 ID
     * <p>
     * 使用 Set 记录已访问的节点 ID，检测重复
     *
     * @param nodes      节点列表
     * @param visitedIds 已访问的节点 ID 集合
     * @return true 表示无重复，false 表示有重复
     */
    private boolean validateNodeNoDuplicate(List<MenuNode> nodes, Set<Long> visitedIds) {
        if (nodes == null) {
            return true;
        }

        for (MenuNode node : nodes) {
            // 检查节点ID是否重复
            if (visitedIds.contains(node.getId())) {
                return false;  // 发现重复节点
            }
            visitedIds.add(node.getId());

            // 递归检查子节点
            if (!validateNodeNoDuplicate(node.getChildren(), visitedIds)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取树验证详情（找出重复的节点）
     * <p>
     * 统计每个节点 ID 出现的次数，找出重复的节点
     *
     * @param tree 树形结构的根节点列表
     * @return 重复节点的详细信息，如果无重复则返回"无重复"
     */
    private String getTreeValidationDetails(List<MenuNode> tree) {
        Map<Long, Integer> idCount = new HashMap<>();
        countNodeIds(tree, idCount);

        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : idCount.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add("节点ID=" + entry.getKey() + " 出现了 " + entry.getValue() + " 次");
            }
        }

        return duplicates.isEmpty() ? "无重复" : String.join(", ", duplicates);
    }

    /**
     * 统计节点 ID 出现次数
     * <p>
     * 递归遍历树结构，统计每个节点 ID 出现的次数
     *
     * @param nodes   节点列表
     * @param idCount ID 出现次数的映射表
     */
    private void countNodeIds(List<MenuNode> nodes, Map<Long, Integer> idCount) {
        if (nodes == null) {
            return;
        }

        for (MenuNode node : nodes) {
            idCount.put(node.getId(), idCount.getOrDefault(node.getId(), 0) + 1);
            countNodeIds(node.getChildren(), idCount);
        }
    }

    /**
     * 树节点测试类
     * <p>
     * 用于测试 TreeUtil 的菜单节点实体类，包含基本的树节点属性和业务属性
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuNode {
        private Long id;
        private Long parentId;
        private String name;
        private Integer sort;
        private Boolean enabled;
        private Integer level;  // 新增：层级字段
        private List<MenuNode> children;

        public MenuNode(Long id, Long parentId, String name, Integer sort, Boolean enabled) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.sort = sort;
            this.enabled = enabled;
        }
    }
}