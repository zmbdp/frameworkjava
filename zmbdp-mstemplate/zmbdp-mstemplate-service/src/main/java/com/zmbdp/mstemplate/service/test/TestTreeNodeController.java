package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.core.utils.TreeUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.TreeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TreeNode 节点测试控制器
 * 使用 TreeNode 类测试 TreeUtil 的所有功能
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/test/treenode")
public class TestTreeNodeController {

    /**
     * 一键测试所有功能（使用 TreeNode）
     * <p>
     * 测试 TreeUtil 的所有功能，包括：构建、遍历、查找、过滤、排序、子树提取、统计分析、安全版本方法
     *
     * @return 详细的测试结果，包含每个功能的测试状态和数据
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> testAll() {
        log.info("=== 使用 TreeNode 测试所有树形结构功能 ===");
        Map<String, Object> result = new LinkedHashMap<>();

        // 准备测试数据
        List<TreeNode<Long>> flatList = createTestData();

        // ========== 树形结构构建测试 ==========
        Map<String, Object> buildTest = new LinkedHashMap<>();
        try {
            // 1. 指定根节点 ID 构建树
            List<TreeNode<Long>> tree1 = TreeUtil.build(
                    flatList,
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            Map<String, Object> validation1 = validateTreeStructure(tree1, 0L);
            boolean noDuplicate1 = validateTreeStructureNoDuplicate(tree1);
            boolean isValid1 = (Boolean) validation1.get("验证通过");
            buildTest.put("指定根节点构建-父子关系正确", isValid1 ? "✅ 成功" : "❌ 失败");
            if (!isValid1) {
                buildTest.put("指定根节点构建-父子关系错误", validation1.get("错误详情"));
            }
            buildTest.put("指定根节点构建-无重复节点", noDuplicate1 ? "✅ 成功" : "❌ 失败");
            buildTest.put("指定根节点构建-树结构", printTree(tree1));

            // 2. 自动识别根节点构建树
            List<TreeNode<Long>> flatList2 = createTestData();
            List<TreeNode<Long>> tree2 = TreeUtil.build(
                    flatList2,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            Map<String, Object> validation2 = validateTreeStructure(tree2, 0L);
            boolean noDuplicate2 = validateTreeStructureNoDuplicate(tree2);
            boolean isValid2 = (Boolean) validation2.get("验证通过");
            buildTest.put("自动识别根节点-父子关系正确", isValid2 ? "✅ 成功" : "❌ 失败");
            if (!isValid2) {
                buildTest.put("自动识别根节点-父子关系错误", validation2.get("错误详情"));
            }
            buildTest.put("自动识别根节点-无重复节点", noDuplicate2 ? "✅ 成功" : "❌ 失败");

            // 3. 空列表测试
            List<TreeNode<Long>> emptyTree = TreeUtil.build(
                    new ArrayList<>(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );
            buildTest.put("空列表构建", emptyTree.isEmpty() ? "✅ 成功" : "❌ 失败");

            result.put("1. 树形结构构建", buildTest);
        } catch (Exception e) {
            buildTest.put("异常", e.getMessage());
            result.put("1. 树形结构构建", buildTest);
        }

        // ========== 树形遍历测试 ==========
        Map<String, Object> traverseTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            // 1. 深度优先遍历
            List<Long> dfsResult = new ArrayList<>();
            TreeUtil.traverseDepthFirst(tree, TreeNode::getChildren, node -> dfsResult.add(node.getId()));
            traverseTest.put("深度优先遍历", dfsResult);

            // 2. 广度优先遍历
            List<Long> bfsResult = new ArrayList<>();
            TreeUtil.traverseBreadthFirst(tree, TreeNode::getChildren, node -> bfsResult.add(node.getId()));
            traverseTest.put("广度优先遍历", bfsResult);

            // 3. 树转列表
            List<TreeNode<Long>> flatResult = TreeUtil.toList(tree, TreeNode::getChildren);
            traverseTest.put("树转列表-节点数", flatResult.size());

            result.put("2. 树形遍历", traverseTest);
        } catch (Exception e) {
            traverseTest.put("异常", e.getMessage());
            result.put("2. 树形遍历", traverseTest);
        }

        // ========== 树形查找测试 ==========
        Map<String, Object> findTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            // 1. 查找第一个节点
            TreeNode<Long> found = TreeUtil.findFirst(tree, TreeNode::getChildren, n -> n.getId().equals(3L));
            findTest.put("查找节点3", found != null ? "✅ 找到" : "❌ 未找到");

            // 2. 查找所有节点
            List<TreeNode<Long>> allFound = TreeUtil.findAll(tree, TreeNode::getChildren, n -> n.getId() > 5L);
            findTest.put("查找ID>5的节点", allFound.stream().map(TreeNode::getId).collect(Collectors.toList()));

            // 3. 查找路径
            List<TreeNode<Long>> path = TreeUtil.findPath(tree, TreeNode::getChildren, n -> n.getId().equals(7L));
            findTest.put("节点7的路径", path.stream().map(TreeNode::getId).collect(Collectors.toList()));

            // 4. 获取祖先节点
            List<TreeNode<Long>> ancestors = TreeUtil.getAncestors(tree, TreeNode::getChildren, n -> n.getId().equals(7L));
            findTest.put("节点7的祖先", ancestors.stream().map(TreeNode::getId).collect(Collectors.toList()));

            // 5. 获取后代节点
            List<TreeNode<Long>> descendants = TreeUtil.getDescendants(tree, TreeNode::getChildren, n -> n.getId().equals(2L));
            findTest.put("节点2的后代", descendants.stream().map(TreeNode::getId).collect(Collectors.toList()));

            // 6. 获取叶子节点
            List<TreeNode<Long>> leafNodes = TreeUtil.getLeafNodes(tree, TreeNode::getChildren);
            findTest.put("叶子节点", leafNodes.stream().map(TreeNode::getId).collect(Collectors.toList()));

            // 7. 判断是否包含
            boolean contains = TreeUtil.contains(tree, TreeNode::getChildren, n -> n.getId().equals(10L));
            findTest.put("是否包含节点10", contains ? "✅ 包含" : "❌ 不包含");

            result.put("3. 树形查找", findTest);
        } catch (Exception e) {
            findTest.put("异常", e.getMessage());
            result.put("3. 树形查找", findTest);
        }

        // ========== 树形过滤测试 ==========
        Map<String, Object> filterTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            // 过滤：只保留ID<=5的节点
            List<TreeNode<Long>> filtered = TreeUtil.filter(
                    tree,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    n -> n.getId() <= 5L
            );

            Map<String, Object> validation = validateTreeStructure(filtered, 0L);
            boolean noDuplicate = validateTreeStructureNoDuplicate(filtered);
            boolean isValid = (Boolean) validation.get("验证通过");
            filterTest.put("过滤后-父子关系正确", isValid ? "✅ 成功" : "❌ 失败");
            if (!isValid) {
                filterTest.put("过滤后-父子关系错误", validation.get("错误详情"));
            }
            filterTest.put("过滤后-无重复节点", noDuplicate ? "✅ 成功" : "❌ 失败");
            filterTest.put("过滤后-树结构", printTree(filtered));

            result.put("4. 树形过滤", filterTest);
        } catch (Exception e) {
            filterTest.put("异常", e.getMessage());
            result.put("4. 树形过滤", filterTest);
        }

        // ========== 树形排序测试 ==========
        Map<String, Object> sortTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            // 按ID降序排序
            List<TreeNode<Long>> sorted = TreeUtil.sort(
                    tree,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    Comparator.comparing(TreeNode<Long>::getId).reversed()
            );

            Map<String, Object> validation = validateTreeStructure(sorted, 0L);
            boolean noDuplicate = validateTreeStructureNoDuplicate(sorted);
            boolean isValid = (Boolean) validation.get("验证通过");
            sortTest.put("排序后-父子关系正确", isValid ? "✅ 成功" : "❌ 失败");
            if (!isValid) {
                sortTest.put("排序后-父子关系错误", validation.get("错误详情"));
            }
            sortTest.put("排序后-无重复节点", noDuplicate ? "✅ 成功" : "❌ 失败");
            sortTest.put("排序后-树结构", printTree(sorted));

            result.put("5. 树形排序", sortTest);
        } catch (Exception e) {
            sortTest.put("异常", e.getMessage());
            result.put("5. 树形排序", sortTest);
        }

        // ========== 子树提取测试 ==========
        Map<String, Object> subTreeTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            // 1. 获取所有根节点下2层
            List<TreeNode<Long>> subTree1 = TreeUtil.getSubTree(
                    tree,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    2
            );
            subTreeTest.put("根节点下2层-节点数", subTree1.size());
            subTreeTest.put("根节点下2层-节点ID", subTree1.stream().map(TreeNode::getId).collect(Collectors.toList()));

            // 2. 获取指定节点下2层
            List<TreeNode<Long>> tree2 = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );
            List<TreeNode<Long>> subTree2 = TreeUtil.getSubTree(
                    tree2,
                    TreeNode::getId,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    1L,
                    2
            );
            subTreeTest.put("节点1下2层-节点数", subTree2.size());
            subTreeTest.put("节点1下2层-节点ID", subTree2.stream().map(TreeNode::getId).collect(Collectors.toList()));

            result.put("6. 子树提取", subTreeTest);
        } catch (Exception e) {
            subTreeTest.put("异常", e.getMessage());
            result.put("6. 子树提取", subTreeTest);
        }

        // ========== 统计分析测试 ==========
        Map<String, Object> statsTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );

            int maxDepth = TreeUtil.getMaxDepth(tree, TreeNode::getChildren);
            int nodeCount = TreeUtil.countNodes(tree, TreeNode::getChildren);

            statsTest.put("最大深度", maxDepth);
            statsTest.put("节点总数", nodeCount);

            result.put("7. 统计分析", statsTest);
        } catch (Exception e) {
            statsTest.put("异常", e.getMessage());
            result.put("7. 统计分析", statsTest);
        }

        // ========== 安全版本方法测试 ==========
        Map<String, Object> safeTest = new LinkedHashMap<>();
        try {
            List<TreeNode<Long>> originalList = createTestData();

            // 1. buildSafe 测试
            List<TreeNode<Long>> safeBuildTree = TreeUtil.buildSafe(
                    originalList,
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    TreeNode::new
            );
            Map<String, Object> validation1 = validateTreeStructure(safeBuildTree, 0L);
            boolean noDuplicate1 = validateTreeStructureNoDuplicate(safeBuildTree);
            boolean isValid1 = (Boolean) validation1.get("验证通过");
            safeTest.put("buildSafe-父子关系正确", isValid1 ? "✅ 成功" : "❌ 失败");
            if (!isValid1) {
                safeTest.put("buildSafe-父子关系错误", validation1.get("错误详情"));
            }
            safeTest.put("buildSafe-无重复节点", noDuplicate1 ? "✅ 成功" : "❌ 失败");

            // 2. filterSafe 测试
            List<TreeNode<Long>> tree = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );
            List<TreeNode<Long>> safeFiltered = TreeUtil.filterSafe(
                    tree,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    n -> n.getId() <= 5L,
                    TreeNode::new
            );
            Map<String, Object> validation2 = validateTreeStructure(safeFiltered, 0L);
            boolean noDuplicate2 = validateTreeStructureNoDuplicate(safeFiltered);
            boolean isValid2 = (Boolean) validation2.get("验证通过");
            safeTest.put("filterSafe-父子关系正确", isValid2 ? "✅ 成功" : "❌ 失败");
            if (!isValid2) {
                safeTest.put("filterSafe-父子关系错误", validation2.get("错误详情"));
            }
            safeTest.put("filterSafe-无重复节点", noDuplicate2 ? "✅ 成功" : "❌ 失败");

            // 3. sortSafe 测试
            List<TreeNode<Long>> tree2 = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );
            List<TreeNode<Long>> safeSorted = TreeUtil.sortSafe(
                    tree2,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    Comparator.comparing(TreeNode<Long>::getId).reversed(),
                    TreeNode::new
            );
            Map<String, Object> validation3 = validateTreeStructure(safeSorted, 0L);
            boolean noDuplicate3 = validateTreeStructureNoDuplicate(safeSorted);
            boolean isValid3 = (Boolean) validation3.get("验证通过");
            safeTest.put("sortSafe-父子关系正确", isValid3 ? "✅ 成功" : "❌ 失败");
            if (!isValid3) {
                safeTest.put("sortSafe-父子关系错误", validation3.get("错误详情"));
            }
            safeTest.put("sortSafe-无重复节点", noDuplicate3 ? "✅ 成功" : "❌ 失败");

            // 4. getSubTreeSafe 测试
            List<TreeNode<Long>> tree3 = TreeUtil.build(
                    createTestData(),
                    0L,
                    TreeNode::getId,
                    TreeNode::getParentId,
                    TreeNode::getChildren,
                    TreeNode::setChildren
            );
            List<TreeNode<Long>> safeSubTree = TreeUtil.getSubTreeSafe(
                    tree3,
                    TreeNode::getChildren,
                    TreeNode::setChildren,
                    2,
                    TreeNode::new
            );
            safeTest.put("getSubTreeSafe-节点数", safeSubTree.size());

            result.put("8. 安全版本方法", safeTest);
        } catch (Exception e) {
            safeTest.put("异常", e.getMessage());
            result.put("8. 安全版本方法", safeTest);
        }

        return Result.success(result);
    }

    /**
     * 创建测试数据
     * <p>
     * 创建一个包含 10 个节点的树形结构测试数据：
     * <ul>
     *     <li>根节点：1, 2</li>
     *     <li>1 的子节点：3, 4</li>
     *     <li>2 的子节点：5, 6</li>
     *     <li>3 的子节点：7, 8</li>
     *     <li>4 的子节点：9</li>
     *     <li>5 的子节点：10</li>
     * </ul>
     *
     * @return 扁平化的节点列表
     */
    private List<TreeNode<Long>> createTestData() {
        List<TreeNode<Long>> list = new ArrayList<>();
        list.add(new TreeNode<>(1L, 0L));
        list.add(new TreeNode<>(2L, 0L));
        list.add(new TreeNode<>(3L, 1L));
        list.add(new TreeNode<>(4L, 1L));
        list.add(new TreeNode<>(5L, 2L));
        list.add(new TreeNode<>(6L, 2L));
        list.add(new TreeNode<>(7L, 3L));
        list.add(new TreeNode<>(8L, 3L));
        list.add(new TreeNode<>(9L, 4L));
        list.add(new TreeNode<>(10L, 5L));
        return list;
    }

    /**
     * 验证树结构的父子关系是否正确
     * <p>
     * 递归验证树中每个节点的 parentId 是否与其实际父节点的 id 一致
     *
     * @param tree             树形结构的根节点列表
     * @param expectedParentId 期望的父节点 ID
     * @return 验证结果，包含"验证通过"和"错误详情"（如果有错误）
     */
    private Map<String, Object> validateTreeStructure(List<TreeNode<Long>> tree, Long expectedParentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        for (TreeNode<Long> node : tree) {
            if (!Objects.equals(node.getParentId(), expectedParentId)) {
                errors.add(String.format("节点%d的parentId应为%d，实际为%d", node.getId(), expectedParentId, node.getParentId()));
            }

            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                Map<String, Object> childValidation = validateTreeStructure(node.getChildren(), node.getId());
                if (!(Boolean) childValidation.get("验证通过")) {
                    errors.addAll((List<String>) childValidation.get("错误详情"));
                }
            }
        }

        result.put("验证通过", errors.isEmpty());  // 返回 Boolean 类型
        if (!errors.isEmpty()) {
            result.put("错误详情", errors);
        }
        return result;
    }

    /**
     * 验证树结构中是否有重复节点
     * <p>
     * 遍历整棵树，检查是否存在 ID 重复的节点
     *
     * @param tree 树形结构的根节点列表
     * @return true 表示无重复节点，false 表示存在重复节点
     */
    private boolean validateTreeStructureNoDuplicate(List<TreeNode<Long>> tree) {
        Set<Long> idSet = new HashSet<>();
        return checkNoDuplicate(tree, idSet);
    }

    /**
     * 递归检查节点列表中是否有重复 ID
     *
     * @param nodes 节点列表
     * @param idSet 已收集的节点 ID 集合
     * @return true 表示无重复，false 表示有重复
     */
    private boolean checkNoDuplicate(List<TreeNode<Long>> nodes, Set<Long> idSet) {
        for (TreeNode<Long> node : nodes) {
            if (idSet.contains(node.getId())) {
                return false;
            }
            idSet.add(node.getId());

            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                if (!checkNoDuplicate(node.getChildren(), idSet)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 打印树结构（用于调试）
     * <p>
     * 以缩进的方式打印整棵树的结构，便于查看树的层级关系
     *
     * @param tree 树形结构的根节点列表
     * @return 格式化的树结构字符串
     */
    private String printTree(List<TreeNode<Long>> tree) {
        StringBuilder sb = new StringBuilder();
        for (TreeNode<Long> node : tree) {
            printNode(node, 0, sb);
        }
        return sb.toString();
    }

    /**
     * 递归打印单个节点及其子节点
     *
     * @param node  当前节点
     * @param level 当前层级（用于控制缩进）
     * @param sb    字符串构建器
     */
    private void printNode(TreeNode<Long> node, int level, StringBuilder sb) {
        sb.append("  ".repeat(level)).append("└─ ID:").append(node.getId())
                .append(" (parentId:").append(node.getParentId()).append(")\n");

        if (node.getChildren() != null) {
            for (TreeNode<Long> child : node.getChildren()) {
                printNode(child, level + 1, sb);
            }
        }
    }
}

