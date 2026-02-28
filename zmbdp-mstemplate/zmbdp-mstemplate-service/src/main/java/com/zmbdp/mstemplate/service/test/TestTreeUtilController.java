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
     * 直接调用此接口即可测试所有树形结构功能，无需传参
     *
     * @return 详细的测试结果
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
            buildTest.put("指定根节点构建", !tree1.isEmpty() ? "✅ 成功，根节点数: " + tree1.size() : "❌ 失败");

            // 2. 自动识别根节点构建树
            List<MenuNode> tree2 = TreeUtil.build(
                    flatList,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            buildTest.put("自动识别根节点", !tree2.isEmpty() ? "✅ 成功，根节点数: " + tree2.size() : "❌ 失败");

            // 3. 空列表测试
            List<MenuNode> emptyTree = TreeUtil.build(
                    new ArrayList<>(),
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );
            buildTest.put("空列表构建", emptyTree.isEmpty() ? "✅ 成功，返回空列表" : "❌ 失败");

        } catch (Exception e) {
            buildTest.put("错误", "❌ 树形结构构建测试异常: " + e.getMessage());
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

        } catch (Exception e) {
            traverseTest.put("错误", "❌ 树形结构遍历测试异常: " + e.getMessage());
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
            List<MenuNode> tree = TreeUtil.build(
                    flatList,
                    0L,
                    MenuNode::getId,
                    MenuNode::getParentId,
                    MenuNode::getChildren,
                    MenuNode::setChildren
            );

            // 1. 获取所有叶子节点
            List<MenuNode> leafNodes = TreeUtil.getLeafNodes(tree, MenuNode::getChildren);
            String leafNames = leafNodes.stream().map(MenuNode::getName).collect(Collectors.joining(", "));
            operationTest.put("获取叶子节点", !leafNodes.isEmpty() ? "✅ 成功，叶子节点: " + leafNames : "❌ 失败");

            // 2. 过滤树（只保留启用的节点）
            List<MenuNode> filteredTree = TreeUtil.filter(
                    tree,
                    MenuNode::getChildren,
                    MenuNode::setChildren,
                    MenuNode::getEnabled
            );
            int filteredCount = TreeUtil.countNodes(filteredTree, MenuNode::getChildren);
            operationTest.put("过滤树结构", filteredCount > 0 ? "✅ 成功，过滤后节点数: " + filteredCount : "❌ 失败");

            // 3. 排序树（按 sort 字段升序）
            List<MenuNode> sortedTree = TreeUtil.sort(
                    tree,
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
        List<MenuNode> filteredTree = TreeUtil.filter(
                tree,
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

        // 排序：按 sort 字段升序
        List<MenuNode> sortedTree = TreeUtil.sort(
                tree,
                MenuNode::getChildren,
                MenuNode::setChildren,
                Comparator.comparing(MenuNode::getSort)
        );

        // 获取排序前后的顺序
        List<String> beforeSort = new ArrayList<>();
        TreeUtil.traverseDepthFirst(tree, MenuNode::getChildren, node -> beforeSort.add(node.getName()));

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
     * 创建测试数据
     * 模拟一个菜单树结构：
     * - 系统管理 (1)
     * - 用户管理 (2)
     * - 角色管理 (3)
     * - 角色列表 (4)
     * - 权限分配 (5)
     * - 内容管理 (6)
     * - 文章管理 (7)
     * - 分类管理 (8)
     * - 设置 (9)
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
     * 树节点测试类
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