package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * 树形结构工具类
 * <p>
 * 提供全面的树形结构处理能力，支持任意类型的树节点对象。<br>
 * 支持菜单树、组织架构树、分类树、权限树等多种业务的树结构常见操作。
 * <p>
 * <b>主要功能：</b>
 * <ul>
 *     <li>树形结构构建：将扁平列表（如数据库返回的 List）高效转为树形结构</li>
 *     <li>树形遍历：支持深度优先、广度优先等多种遍历方式，以及节点批量操作</li>
 *     <li>树形查找：任意条件查找单个/多个节点，查找节点路径、祖先、后代</li>
 *     <li>树转列表：支持将树结构“拉平成”列表，按需返回所有节点</li>
 *     <li>子树提取：快速获取任意节点的指定层级子树，或某节点的全部后代</li>
 *     <li>过滤/排序：按条件过滤树节点，或对整棵树递归排序</li>
 *     <li>统计分析：支持节点总数、最大深度、叶子节点数/提取等统计</li>
 * </ul>
 * <b>典型用法示例：</b>
 * <ul>
 *     <li>build() —— 扁平数据一键转树</li>
 *     <li>toList() —— 树转全部节点列表</li>
 *     <li>findFirst()/findPath() —— 查找节点/路径</li>
 *     <li>filter()/sort() —— 过滤、排序树</li>
 *     <li>getSubTree()/getDescendants() —— 获取子树、后代节点</li>
 * </ul>
 * <b>适用场景：</b>
 * <ul>
 *     <li>菜单树、组织/部门架构、权限树、商品分类、文件目录、自定义层级结构等</li>
 *     <li>支持树的懒加载、分页加载、按层级截取、业务数据（如启用禁用）筛选等场景</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeUtil {

    /**
     * 构建树形结构（指定根节点 ID）
     * <p>
     * 将扁平化的列表转换为树形结构，从指定的根节点 ID 开始构建。<br>
     * 适用于已知根节点 ID 的场景，如菜单树、组织架构树等。
     * <p>
     * <b>重要提示：</b>
     * <ul>
     *     <li>此方法会<b>直接修改原始list中节点的children属性</b></li>
     *     <li>每次调用都会先清空所有节点的children，避免重复构建时累加</li>
     *     <li>如果需要保留原始数据，请传入list的副本</li>
     * </ul>
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(n)，需要额外的 Map 存储节点映射</li>
     *     <li>使用 HashMap 实现 O(1) 的父节点查找</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 定义树节点类
     * @Data
     * public class MenuNode {
     *     private Long id;
     *     private Long parentId;
     *     private String name;
     *     private List<MenuNode> children;
     * }
     *
     * // 构建树形结构（从根节点 0 开始）
     * List<MenuNode> menuList = menuService.findAll();
     * List<MenuNode> tree = TreeUtil.build(
     *     menuList,
     *     0L,
     *     MenuNode::getId,
     *     MenuNode::getParentId,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren
     * );
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 list 为 null 或空，返回空列表（不会抛出异常）</li>
     *     <li>rootId 为根节点的 ID，通常为 0 或 null</li>
     *     <li>如果存在孤儿节点（父节点不存在），该节点会被忽略</li>
     *     <li>如果存在 ID 重复的节点，保留第一个节点</li>
     *     <li>返回的是根节点列表（可能有多个根节点）</li>
     *     <li>原始列表不会被修改，返回的是新构建的树结构</li>
     * </ul>
     *
     * @param list           扁平化的节点列表，可以为 null 或空列表
     * @param rootId         根节点的 ID（通常为 0 或 null）
     * @param idGetter       获取节点 ID 的函数，不能为 null
     * @param parentIdGetter 获取父节点 ID 的函数，不能为 null
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型（需要正确实现 equals 和 hashCode 方法）
     * @return 树形结构的根节点列表，如果 list 为 null 或空则返回空列表
     * @throws NullPointerException 当任何 Function 参数为 null 时抛出
     */
    public static <T, ID> List<T> build(
            List<T> list, ID rootId, Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter
    ) {
        // 如果是空的话就直接返回一个空列表
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 构建 ID -> 节点的映射表，用于 O(1) 时间复杂度查找父节点
        // 如果有重复ID，保留第一个节点 (o1, o2) -> o1
        Map<ID, T> nodeMap = list.stream().collect(
                Collectors.toMap(
                        idGetter, // key: 节点ID
                        Function.identity(), // value: 节点本身
                        (o1, o2) -> o1 // 冲突处理：保留第一个
                )
        );

        // 先清空所有节点的 children，避免重复调用 build 时累加子节点
        for (T node : list) {
            childrenSetter.accept(node, new ArrayList<>());
        }

        // 存储所有根节点（parentId == rootId 的节点）
        List<T> rootNodes = new ArrayList<>();

        // 遍历所有节点，构建父子关系
        for (T node : list) {
            ID parentId = parentIdGetter.apply(node);

            // 判断是否为根节点（父ID 等于指定的 根ID）
            if (Objects.equals(parentId, rootId)) {
                rootNodes.add(node);
            } else {
                // 非根节点：查找父节点并建立父子关系
                establishParentChildRelation(childrenGetter, node, nodeMap, parentId);
            }
        }

        return rootNodes;
    }

    /**
     * 构建树形结构（自动识别根节点）
     * <p>
     * 将扁平化的列表转换为树形结构，自动识别根节点（父节点 ID 不在列表中的节点）。<br>
     * 适用于不确定根节点 ID 的场景，或者数据中根节点的父 ID 各不相同的情况。
     * <p>
     * <b>重要提示：</b>
     * <ul>
     *     <li>此方法会<b>直接修改原始list中节点的children属性</b></li>
     *     <li>每次调用都会先清空所有节点的children，避免重复构建时累加</li>
     *     <li>如果需要保留原始数据，请传入list的副本</li>
     * </ul>
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(n)，需要额外的 Map 和 Set 存储</li>
     *     <li>自动识别根节点：parentId 为 null 或 parentId 不在节点列表中</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 构建树形结构（自动识别根节点）
     * List<MenuNode> menuList = menuService.findAll();
     * List<MenuNode> tree = TreeUtil.build(
     *     menuList,
     *     MenuNode::getId,
     *     MenuNode::getParentId,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren
     * );
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 list 为 null 或空，返回空列表（不会抛出异常）</li>
     *     <li>父节点 ID 为 null 的节点会被识别为根节点</li>
     *     <li>父节点 ID 不在列表中的节点也会被识别为根节点</li>
     *     <li>如果存在 ID 重复的节点，保留第一个节点</li>
     *     <li>原始列表不会被修改，返回的是新构建的树结构</li>
     * </ul>
     *
     * @param list           扁平化的节点列表，可以为 null 或空列表
     * @param idGetter       获取节点 ID 的函数，不能为 null
     * @param parentIdGetter 获取父节点 ID 的函数，不能为 null
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型（需要正确实现 equals 和 hashCode 方法）
     * @return 树形结构的根节点列表，如果 list 为 null 或空则返回空列表
     * @throws NullPointerException 当任何 Function 参数为 null 时抛出
     */
    public static <T, ID> List<T> build(
            List<T> list, Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter
    ) {
        // 空列表直接返回
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 构建 ID -> 节点的映射表，用于快速查找
        Map<ID, T> nodeMap = list.stream().collect(
                Collectors.toMap(
                        idGetter, // key: 节点ID
                        Function.identity(), // value: 节点本身
                        (o1, o2) -> o1 // 冲突处理：保留第一个
                )
        );

        // 先清空所有节点的 children，避免重复调用 build 时累加子节点
        for (T node : list) {
            childrenSetter.accept(node, new ArrayList<>());
        }

        // 收集所有存在的节点 ID（用于判断父节点是否存在）
        Set<ID> existingIds = nodeMap.keySet();

        // 存储根节点
        List<T> rootNodes = new ArrayList<>();

        // 遍历所有节点，构建父子关系
        for (T node : list) {
            ID parentId = parentIdGetter.apply(node);

            // 判断是否为根节点：
            // 1. 父节点ID 为 null
            // 2. 父节点ID 不在现有节点列表中（孤儿节点也视为根节点）
            if (parentId == null || !existingIds.contains(parentId)) {
                rootNodes.add(node);
            } else {
                // 非根节点：查找父节点并建立父子关系
                establishParentChildRelation(childrenGetter, node, nodeMap, parentId);
            }
        }

        return rootNodes;
    }

    /**
     * 建立父子节点关系
     * <p>
     * 将子节点添加到父节点的 children 列表中。<br>
     * 如果父节点的 children 列表为 null，会自动创建一个新的 ArrayList。
     *
     * @param childrenGetter 获取子节点列表的函数
     * @param node           子节点
     * @param nodeMap        节点 ID 到节点的映射
     * @param parentId       父节点 ID
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型
     */
    private static <T, ID> void establishParentChildRelation(
            Function<T, List<T>> childrenGetter,
            T node, Map<ID, T> nodeMap, ID parentId
    ) {
        // 从映射表中查找父节点
        T parentNode = nodeMap.get(parentId);
        if (parentNode != null) {
            // 获取父节点的子节点列表（build 方法已经初始化，不会为 null）
            List<T> children = childrenGetter.apply(parentNode);

            // 将当前节点添加到父节点的 children 列表中
            children.add(node);
        }
        // 如果父节点不存在，该节点会被忽略（孤儿节点）
    }

    /**
     * 将树形结构转换为扁平化列表（深度优先遍历）
     * <p>
     * 将树形结构按照深度优先的顺序转换为扁平化列表。<br>
     * 适用于需要遍历整棵树的场景，如导出所有节点、批量操作等。
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(h)，其中 h 为树的高度（递归调用栈）</li>
     *     <li>遍历顺序：先序遍历（根 -> 左 -> 右）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将树形结构转换为列表
     * List<MenuNode> tree = TreeUtil.build(...);
     * List<MenuNode> flatList = TreeUtil.toList(tree, MenuNode::getChildren);
     *
     * // 批量更新所有节点
     * flatList.forEach(node -> node.setUpdateTime(new Date()));
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @return 扁平化的节点列表，如果 tree 为 null 或空则返回空列表
     * @throws NullPointerException 当 childrenGetter 为 null 时抛出
     */
    public static <T> List<T> toList(List<T> tree, Function<T, List<T>> childrenGetter) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }

        // 创建结果列表，用于收集所有节点
        List<T> result = new ArrayList<>();

        // 遍历每个根节点，深度优先遍历并收集所有节点
        for (T node : tree) {
            traverseDepthFirst(node, childrenGetter, result::add);
        }

        return result;
    }

    /**
     * 将树形结构转换为 Map（ID -> 节点）
     * <p>
     * 将树形结构转换为以节点 ID 为 key、节点对象为 value 的 Map。<br>
     * 适用于需要通过 ID 快速查找节点的场景，时间复杂度从 O(n) 降低到 O(1)。
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(n)，需要存储所有节点</li>
     *     <li>如果存在 ID 重复的节点，保留第一个节点</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 将树转换为 Map，方便快速查找
     * List<MenuNode> tree = TreeUtil.build(...);
     * Map<Long, MenuNode> nodeMap = TreeUtil.toMap(
     *     tree,
     *     MenuNode::getId,
     *     MenuNode::getChildren
     * );
     *
     * // O(1) 时间复杂度查找节点
     * MenuNode node = nodeMap.get(10L);
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param idGetter       获取节点 ID 的函数，不能为 null
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型（需要正确实现 equals 和 hashCode 方法）
     * @return ID 到节点的映射 Map，如果 tree 为 null 或空则返回空 Map
     * @throws NullPointerException 当 idGetter 或 childrenGetter 为 null 时抛出
     */
    public static <T, ID> Map<ID, T> toMap(
            List<T> tree,
            Function<T, ID> idGetter,
            Function<T, List<T>> childrenGetter
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return new HashMap<>();
        }

        // 先将树转换为扁平列表
        List<T> flatList = toList(tree, childrenGetter);

        // 转换为 Map（ID -> 节点）
        return flatList.stream().collect(
                Collectors.toMap(
                        idGetter, // key: 节点ID
                        Function.identity(), // value: 节点本身
                        (o1, o2) -> o1 // 冲突处理：保留第一个
                )
        );
    }

    /**
     * 为树节点添加层级信息
     * <p>
     * 遍历树形结构，为每个节点设置其所在的层级（根节点层级为 1）。<br>
     * 适用于需要显示层级信息的场景，如树形表格、缩进显示等。
     * <p>
     * <b>重要提示：</b>
     * <ul>
     *     <li>此方法会<b>直接修改原始树节点的层级属性</b></li>
     *     <li>根节点的层级为 1，子节点层级依次递增</li>
     * </ul>
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(h)，其中 h 为树的高度（递归调用栈）</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 为树节点添加层级信息
     * List<MenuNode> tree = TreeUtil.build(...);
     * TreeUtil.enrichWithLevel(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setLevel
     * );
     *
     * // 现在每个节点都有层级信息
     * tree.forEach(node -> {
     *     System.out.println("节点: " + node.getName() + ", 层级: " + node.getLevel());
     * });
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param levelSetter    设置节点层级的函数，不能为 null
     * @param <T>            树节点类型
     * @throws NullPointerException 当 childrenGetter 或 levelSetter 为 null 时抛出
     */
    public static <T> void enrichWithLevel(
            List<T> tree,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, Integer> levelSetter
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return;
        }

        // 遍历每个根节点，从层级 1 开始
        for (T node : tree) {
            enrichWithLevel(node, childrenGetter, levelSetter, 1);
        }
    }

    /**
     * 为单个节点及其子树添加层级信息
     * <p>
     * 递归设置节点及其所有子节点的层级。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param levelSetter    设置节点层级的函数
     * @param level          当前层级（根节点为 1）
     * @param <T>            树节点类型
     */
    private static <T> void enrichWithLevel(
            T node, Function<T, List<T>> childrenGetter,
            BiConsumer<T, Integer> levelSetter, int level
    ) {
        if (node == null) {
            return;
        }

        // 设置当前节点的层级
        levelSetter.accept(node, level);

        // 获取子节点列表
        List<T> children = childrenGetter.apply(node);

        // 递归设置子节点的层级（层级 + 1）
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                enrichWithLevel(child, childrenGetter, levelSetter, level + 1);
            }
        }
    }

    /**
     * 深度优先遍历树形结构
     * <p>
     * 按照深度优先的顺序遍历树形结构，对每个节点执行指定的操作。<br>
     * 适用于需要对树中每个节点执行操作的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 打印所有节点的名称
     * TreeUtil.traverseDepthFirst(
     *     tree,
     *     MenuNode::getChildren,
     *     node -> System.out.println(node.getName())
     * );
     *
     * // 收集所有节点的 ID
     * List<Long> ids = new ArrayList<>();
     * TreeUtil.traverseDepthFirst(
     *     tree,
     *     MenuNode::getChildren,
     *     node -> ids.add(node.getId())
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param consumer       对每个节点执行的操作，不能为 null
     * @param <T>            树节点类型
     */
    public static <T> void traverseDepthFirst(List<T> tree, Function<T, List<T>> childrenGetter, Consumer<T> consumer) {
        if (CollectionUtils.isEmpty(tree)) {
            return;
        }

        for (T node : tree) {
            traverseDepthFirst(node, childrenGetter, consumer);
        }
    }

    /**
     * 深度优先遍历单个节点及其子树
     * <p>
     * 递归遍历节点及其所有子节点，先处理当前节点，再处理子节点。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param consumer       对每个节点执行的操作
     * @param <T>            树节点类型
     */
    private static <T> void traverseDepthFirst(T node, Function<T, List<T>> childrenGetter, Consumer<T> consumer) {
        if (node == null) {
            return;
        }

        // 先序遍历：先处理当前节点
        consumer.accept(node);

        // 获取当前节点的子节点列表
        List<T> children = childrenGetter.apply(node);

        // 递归处理每个子节点
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                traverseDepthFirst(child, childrenGetter, consumer);
            }
        }
    }

    /**
     * 广度优先遍历树形结构
     * <p>
     * 按照广度优先的顺序遍历树形结构，对每个节点执行指定的操作。<br>
     * 适用于需要按层级遍历树的场景，如层级展示、层级统计等。
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(w)，其中 w 为树的最大宽度（队列最大长度）</li>
     *     <li>遍历顺序：按层级从上到下，每层从左到右</li>
     *     <li>使用队列实现，非递归方式</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 按层级打印所有节点
     * TreeUtil.traverseBreadthFirst(
     *     tree,
     *     MenuNode::getChildren,
     *     node -> System.out.println(node.getName())
     * );
     *
     * // 按层级收集节点
     * List<MenuNode> levelOrder = new ArrayList<>();
     * TreeUtil.traverseBreadthFirst(tree, MenuNode::getChildren, levelOrder::add);
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param consumer       对每个节点执行的操作，不能为 null
     * @param <T>            树节点类型
     * @throws NullPointerException 当 childrenGetter 或 consumer 为 null 时抛出
     */
    public static <T> void traverseBreadthFirst(List<T> tree, Function<T, List<T>> childrenGetter, Consumer<T> consumer) {
        if (CollectionUtils.isEmpty(tree)) {
            return;
        }

        // 使用队列实现广度优先遍历（层序遍历）
        Queue<T> queue = new LinkedList<>(tree);

        while (!queue.isEmpty()) {
            // 从队列头部取出一个节点
            T node = queue.poll();

            // 处理当前节点
            consumer.accept(node);

            // 获取当前节点的所有子节点
            List<T> children = childrenGetter.apply(node);

            // 将子节点加入队列尾部（先进先出，保证层序遍历）
            if (CollectionUtils.isNotEmpty(children)) {
                queue.addAll(children);
            }
        }
    }

    /**
     * 查找符合条件的第一个节点
     * <p>
     * 在树形结构中查找第一个符合条件的节点（深度优先）。<br>
     * 适用于需要在树中查找特定节点的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查找 ID 为 10 的节点
     * MenuNode node = TreeUtil.findFirst(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> n.getId().equals(10L)
     * );
     *
     * // 查找名称为"系统管理"的节点
     * MenuNode sysNode = TreeUtil.findFirst(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> "系统管理".equals(n.getName())
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param predicate      查找条件，不能为 null
     * @param <T>            树节点类型
     * @return 第一个符合条件的节点，如果没有找到则返回 null
     */
    public static <T> T findFirst(List<T> tree, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        if (CollectionUtils.isEmpty(tree)) {
            return null;
        }

        // 遍历每个根节点，深度优先查找第一个符合条件的节点
        for (T node : tree) {
            T found = findFirst(node, childrenGetter, predicate);
            if (found != null) {
                return found; // 找到了，立即返回
            }
        }

        // 所有根节点的子树都没找到
        return null;
    }

    /**
     * 在单个节点及其子树中查找符合条件的第一个节点
     * <p>
     * 使用深度优先搜索，找到第一个符合条件的节点后立即返回。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param predicate      查找条件
     * @param <T>            树节点类型
     * @return 第一个符合条件的节点，如果没有找到则返回 null
     */
    private static <T> T findFirst(T node, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        if (node == null) {
            return null;
        }

        // 先判断当前节点是否符合条件
        if (predicate.test(node)) {
            // 符合的话直接返回
            return node;
        }

        // 当前节点不符合，继续在子节点中查找
        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                // 递归查找子节点
                T found = findFirst(child, childrenGetter, predicate);
                if (found != null) {
                    // 如果不为空就说明找到了
                    return found;
                }
            }
        }

        // 当前节点及其子树都没找到
        return null;
    }

    /**
     * 查找符合条件的所有节点
     * <p>
     * 在树形结构中查找所有符合条件的节点。<br>
     * 适用于需要批量查找节点的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查找所有启用的菜单
     * List<MenuNode> enabledMenus = TreeUtil.findAll(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> n.getEnabled()
     * );
     *
     * // 查找所有叶子节点（没有子节点的节点）
     * List<MenuNode> leafNodes = TreeUtil.findAll(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> CollectionUtils.isEmpty(n.getChildren())
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param predicate      查找条件，不能为 null
     * @param <T>            树节点类型
     * @return 所有符合条件的节点列表，如果没有找到则返回空列表
     */
    public static <T> List<T> findAll(List<T> tree, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        // 创建结果列表，用于收集所有符合条件的节点
        List<T> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(tree)) {
            return result;
        }

        // 遍历每个根节点，递归查找所有符合条件的节点
        for (T node : tree) {
            findAll(node, childrenGetter, predicate, result);
        }

        return result;
    }

    /**
     * 在单个节点及其子树中查找所有符合条件的节点
     * <p>
     * 递归遍历节点及其所有子节点，收集所有符合条件的节点。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param predicate      查找条件
     * @param result         结果列表（用于收集符合条件的节点）
     * @param <T>            树节点类型
     */
    private static <T> void findAll(
            T node, Function<T, List<T>> childrenGetter,
            Predicate<T> predicate, List<T> result
    ) {
        if (node == null) {
            return;
        }

        // 判断当前节点是否符合条件
        if (predicate.test(node)) {
            result.add(node); // 符合条件，加入结果列表
        }

        // 无论当前节点是否符合条件，都要继续递归查找子节点
        // （因为子节点可能符合条件）
        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                findAll(child, childrenGetter, predicate, result);
            }
        }
    }

    /**
     * 获取所有叶子节点
     * <p>
     * 获取树形结构中所有的叶子节点（没有子节点的节点）。<br>
     * 适用于需要获取树的末端节点的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取所有叶子节点
     * List<MenuNode> leafNodes = TreeUtil.getLeafNodes(tree, MenuNode::getChildren);
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @return 所有叶子节点列表，如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> getLeafNodes(List<T> tree, Function<T, List<T>> childrenGetter) {
        return findAll(tree, childrenGetter, node -> {
            List<T> children = childrenGetter.apply(node);
            return CollectionUtils.isEmpty(children);
        });
    }

    /**
     * 过滤树形结构
     * <p>
     * 根据条件过滤树形结构，保留符合条件的节点及其祖先节点。<br>
     * 如果父节点不符合条件但子节点符合，父节点也会被保留。
     * <p>
     * <b>重要提示：</b>
     * <ul>
     *     <li>此方法会<b>直接修改原始树节点的children</b>，不会创建新节点</li>
     *     <li>如果需要保留原始树，请在调用前先深拷贝树结构</li>
     *     <li>多次调用会基于上次修改的结果继续过滤</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 过滤出所有启用的菜单（包含其父节点）
     * List<MenuNode> filteredTree = TreeUtil.filter(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     n -> n.getEnabled()
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param predicate      过滤条件，不能为 null
     * @param <T>            树节点类型
     * @return 过滤后的树形结构，如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> filter(
            List<T> tree,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Predicate<T> predicate
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }

        // 过滤每个根节点及其子树
        List<T> result = new ArrayList<>();
        for (T node : tree) {
            // 递归过滤当前节点及其子树
            T filtered = filter(node, childrenGetter, childrenSetter, predicate);
            if (filtered != null) {
                result.add(filtered); // 保留符合条件的节点
            }
        }

        return result;
    }

    /**
     * 过滤单个节点及其子树
     * <p>
     * 递归过滤节点，保留符合条件的节点及其祖先节点。<br>
     * 如果节点本身不符合条件，但其子节点中有符合条件的，该节点也会被保留。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param childrenSetter 设置子节点列表的函数
     * @param predicate      过滤条件
     * @param <T>            树节点类型
     * @return 过滤后的节点，如果节点及其子树都不符合条件则返回 null
     */
    private static <T> T filter(
            T node, Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Predicate<T> predicate
    ) {
        if (node == null) {
            return null;
        }

        // 先递归过滤所有子节点
        List<T> children = childrenGetter.apply(node);
        List<T> filteredChildren = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                // 递归过滤每个子节点
                T filtered = filter(child, childrenGetter, childrenSetter, predicate);
                if (filtered != null) {
                    filteredChildren.add(filtered); // 子节点符合条件，保留
                }
            }
        }

        // 判断是否保留当前节点：
        // 1. 当前节点本身符合条件，或
        // 2. 当前节点有符合条件的子节点（需要保留父节点以维持树结构）
        if (predicate.test(node) || !filteredChildren.isEmpty()) {
            childrenSetter.accept(node, filteredChildren); // 更新子节点列表
            return node; // 保留当前节点
        }

        // 当前节点及其子树都不符合条件，过滤掉
        return null;
    }

    /**
     * 对树形结构进行排序
     * <p>
     * 对树形结构的每一层节点进行排序。<br>
     * 适用于需要对树节点排序的场景，如按名称、序号等排序。
     * <p>
     * <b>重要提示：</b>
     * <ul>
     *     <li>此方法会<b>直接修改原始树节点的children</b>，不会创建新节点</li>
     *     <li>如果需要保留原始树，请在调用前先深拷贝树结构</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 按排序号升序排序
     * List<MenuNode> sortedTree = TreeUtil.sort(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     Comparator.comparing(MenuNode::getSort)
     * );
     *
     * // 按名称排序
     * List<MenuNode> sortedByName = TreeUtil.sort(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     Comparator.comparing(MenuNode::getName)
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param comparator     排序比较器，不能为 null
     * @param <T>            树节点类型
     * @return 排序后的树形结构，如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> sort(
            List<T> tree,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Comparator<T> comparator
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }

        // 对当前层的节点进行排序
        List<T> sortedList = new ArrayList<>(tree);
        sortedList.sort(comparator);

        // 递归对每个节点的子节点进行排序
        for (T node : sortedList) {
            List<T> children = childrenGetter.apply(node);
            if (CollectionUtils.isNotEmpty(children)) {
                // 递归排序子节点
                List<T> sortedChildren = sort(children, childrenGetter, childrenSetter, comparator);
                // 更新节点的子节点列表为排序后的列表
                childrenSetter.accept(node, sortedChildren);
            }
        }

        return sortedList;
    }

    /**
     * 查找从根节点到目标节点的路径
     * <p>
     * 查找从根节点到目标节点的完整路径（包含目标节点）。<br>
     * 适用于需要获取节点路径的场景，如面包屑导航。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 查找 ID 为 10 的节点的路径
     * List<MenuNode> path = TreeUtil.findPath(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> n.getId().equals(10L)
     * );
     * // 结果：[根节点, 父节点, 目标节点]
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param predicate      查找条件，不能为 null
     * @param <T>            树节点类型
     * @return 从根节点到目标节点的路径列表，如果没有找到则返回空列表
     */
    public static <T> List<T> findPath(List<T> tree, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }

        // 遍历每个根节点，尝试查找路径
        for (T node : tree) {
            List<T> path = new ArrayList<>(); // 用于记录路径
            // 在当前根节点的子树中查找目标节点
            if (findPath(node, childrenGetter, predicate, path)) {
                return path; // 找到了，返回完整路径
            }
        }

        // 所有根节点的子树都没找到目标节点
        return new ArrayList<>();
    }

    /**
     * 在单个节点及其子树中查找路径
     * <p>
     * 使用回溯算法查找从当前节点到目标节点的路径。<br>
     * 如果找到目标节点，路径列表中会包含从根到目标的所有节点。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param predicate      查找条件
     * @param path           路径列表（用于记录从根到当前节点的路径）
     * @param <T>            树节点类型
     * @return 是否找到目标节点
     */
    private static <T> boolean findPath(
            T node, Function<T, List<T>> childrenGetter,
            Predicate<T> predicate, List<T> path
    ) {
        if (node == null) {
            return false;
        }

        // 将当前节点加入路径（尝试这条路径）
        path.add(node);

        // 判断当前节点是否为目标节点
        if (predicate.test(node)) {
            return true; // 找到了！路径已完整
        }

        // 当前节点不是目标，继续在子节点中查找
        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                // 递归查找子节点
                if (findPath(child, childrenGetter, predicate, path)) {
                    return true; // 在子树中找到了目标节点
                }
            }
        }

        // 当前节点及其子树都没找到目标节点
        // 回溯：从路径中移除当前节点，尝试其他路径
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * 获取树的最大深度
     * <p>
     * 获取树形结构的最大深度（根节点深度为 1）。<br>
     * 适用于需要了解树的层级深度的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取树的最大深度
     * int maxDepth = TreeUtil.getMaxDepth(tree, MenuNode::getChildren);
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @return 树的最大深度，如果 tree 为 null 或空则返回 0
     */
    public static <T> int getMaxDepth(List<T> tree, Function<T, List<T>> childrenGetter) {
        if (CollectionUtils.isEmpty(tree)) {
            return 0; // 空树深度为 0
        }

        // 遍历所有根节点，找出最大深度
        int maxDepth = 0;
        for (T node : tree) {
            // 计算当前根节点的深度（根节点深度为 1）
            int depth = getMaxDepth(node, childrenGetter, 1);
            // 取所有根节点中的最大深度
            maxDepth = Math.max(maxDepth, depth);
        }

        return maxDepth;
    }

    /**
     * 获取单个节点及其子树的最大深度
     * <p>
     * 递归计算节点的最大深度。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param currentDepth   当前深度（根节点为 1）
     * @param <T>            树节点类型
     * @return 最大深度
     */
    private static <T> int getMaxDepth(T node, Function<T, List<T>> childrenGetter, int currentDepth) {
        if (node == null) {
            return currentDepth - 1;
        }

        // 获取当前节点的子节点
        List<T> children = childrenGetter.apply(node);

        // 如果是叶子节点（没有子节点），返回当前深度
        if (CollectionUtils.isEmpty(children)) {
            return currentDepth;
        }

        // 有子节点，递归计算所有子节点的最大深度
        int maxDepth = currentDepth;
        for (T child : children) {
            // 递归计算子节点深度（深度 + 1）
            int depth = getMaxDepth(child, childrenGetter, currentDepth + 1);
            // 取最大值
            maxDepth = Math.max(maxDepth, depth);
        }

        return maxDepth;
    }

    /**
     * 统计树中节点的总数
     * <p>
     * 统计树形结构中所有节点的数量。<br>
     * 适用于需要统计节点数量的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 统计树中节点总数
     * int count = TreeUtil.countNodes(tree, MenuNode::getChildren);
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param <T>            树节点类型
     * @return 节点总数，如果 tree 为 null 或空则返回 0
     */
    public static <T> int countNodes(List<T> tree, Function<T, List<T>> childrenGetter) {
        if (CollectionUtils.isEmpty(tree)) {
            return 0; // 空树节点数为0
        }

        // 累加所有根节点及其子树的节点数
        int count = 0;
        for (T node : tree) {
            count += countNodes(node, childrenGetter);
        }

        return count;
    }

    /**
     * 统计单个节点及其子树的节点数量
     * <p>
     * 递归统计节点数量，包括当前节点和所有子节点。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param <T>            树节点类型
     * @return 节点数量（包括当前节点）
     */
    private static <T> int countNodes(T node, Function<T, List<T>> childrenGetter) {
        if (node == null) {
            return 0;
        }

        // 当前节点计数为 1
        int count = 1;

        // 获取子节点列表
        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
            // 递归统计每个子节点及其子树的节点数
            for (T child : children) {
                count += countNodes(child, childrenGetter);
            }
        }

        return count;
    }

    /**
     * 判断树中是否包含符合条件的节点
     * <p>
     * 判断树形结构中是否存在至少一个符合条件的节点。<br>
     * 适用于需要快速判断节点是否存在的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 判断是否存在 ID 为 10 的节点
     * boolean exists = TreeUtil.contains(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> n.getId().equals(10L)
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param predicate      判断条件，不能为 null
     * @param <T>            树节点类型
     * @return 如果存在符合条件的节点则返回 true，否则返回 false
     */
    public static <T> boolean contains(List<T> tree, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        return findFirst(tree, childrenGetter, predicate) != null;
    }

    /**
     * 获取指定节点的所有祖先节点
     * <p>
     * 获取从根节点到指定节点的所有祖先节点（不包含目标节点本身）。<br>
     * 适用于需要获取节点祖先链的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取 ID 为 10 的节点的所有祖先节点
     * List<MenuNode> ancestors = TreeUtil.getAncestors(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> n.getId().equals(10L)
     * );
     * // 结果：[根节点, 父节点]（不包含目标节点）
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param predicate      查找条件，不能为 null
     * @param <T>            树节点类型
     * @return 祖先节点列表（不包含目标节点），如果没有找到则返回空列表
     */
    public static <T> List<T> getAncestors(List<T> tree, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        // 先查找从根到目标节点的完整路径
        List<T> path = findPath(tree, childrenGetter, predicate);
        if (path.isEmpty()) {
            return new ArrayList<>(); // 没找到节点，返回空列表
        }

        // 移除最后一个元素（目标节点本身），剩下的就是祖先节点
        // 例如：路径是 [根节点, 父节点, 目标节点]，返回 [根节点, 父节点]
        return path.subList(0, path.size() - 1);
    }

    /**
     * 获取指定节点的所有后代节点
     * <p>
     * 获取指定节点的所有后代节点（不包含节点本身）。<br>
     * 适用于需要获取子树所有节点的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取 ID 为 10 的节点的所有后代节点
     * List<MenuNode> descendants = TreeUtil.getDescendants(
     *     tree,
     *     MenuNode::getChildren,
     *     n -> n.getId().equals(10L)
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param predicate      查找条件，不能为 null
     * @param <T>            树节点类型
     * @return 后代节点列表（不包含目标节点），如果没有找到则返回空列表
     */
    public static <T> List<T> getDescendants(List<T> tree, Function<T, List<T>> childrenGetter, Predicate<T> predicate) {
        // 先查找目标节点
        T node = findFirst(tree, childrenGetter, predicate);
        if (node == null) {
            return new ArrayList<>(); // 没找到节点，返回空列表
        }

        // 收集所有后代节点（不包含节点本身）
        List<T> descendants = new ArrayList<>();
        List<T> children = childrenGetter.apply(node);

        // 遍历所有子节点，深度优先收集所有后代
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                // 递归遍历子节点及其所有后代，加入结果列表
                traverseDepthFirst(child, childrenGetter, descendants::add);
            }
        }

        return descendants;
    }

    /**
     * 获取指定层级的子树
     * <p>
     * 获取所有根节点下面指定层级的子孙节点（不包括根节点本身）。<br>
     * 适用于懒加载树形菜单、分页加载树节点等场景。
     * <p>
     * <b>重要提示：</b>
     * <ul>
     *     <li>此方法会<b>直接修改原始树节点的children</b>，截断超出层级的子节点</li>
     *     <li>如果需要保留原始树，请在调用前先深拷贝树结构</li>
     *     <li>多次调用会基于上次修改的结果继续截断</li>
     * </ul>
     * <p>
     * <b>层级说明：</b>
     * <ul>
     *     <li>levels = 1：返回根节点下面 1 层（子节点），不包括根节点</li>
     *     <li>levels = 2：返回根节点下面 2 层（子节点 + 孙节点），不包括根节点</li>
     *     <li>levels = 3：返回根节点下面 3 层（子节点 + 孙节点 + 曾孙节点），不包括根节点</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取所有根节点下面 2 层（子节点 + 孙节点），不包括根节点
     * List<MenuNode> subTree = TreeUtil.getSubTree(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     2
     * );
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param levels         往下的层级数（1 = 下面 1 层，2 = 下面 2 层，以此类推）
     * @param <T>            树节点类型
     * @return 指定层级的子孙节点列表（不包括根节点），如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> getSubTree(
            List<T> tree, Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter, int levels
    ) {
        if (CollectionUtils.isEmpty(tree) || levels <= 0) {
            return new ArrayList<>();
        }

        // 收集所有根节点下面指定层级的子孙节点
        List<T> result = new ArrayList<>();
        for (T node : tree) {
            // 获取当前根节点的子节点
            List<T> children = childrenGetter.apply(node);
            if (CollectionUtils.isNotEmpty(children)) {
                // 收集该节点下面指定层级的子孙节点
                collectSubTreeChildren(children, childrenGetter, childrenSetter, levels - 1, result);
            }
        }

        return result;
    }

    /**
     * 获取指定节点的子树（通过 节点ID 查找）
     * <p>
     * 在整棵树中查找 指定ID 的节点，并返回该节点下面指定层级的子孙节点（不包括该节点本身）。<br>
     * 常用于树形结构的懒加载和按需分页等场景。
     * <p>
     * <b>层级说明：</b>
     * <ul>
     *     <li>levels = 1：返回目标节点下面 1 层（子节点），不包括目标节点</li>
     *     <li>levels = 2：返回目标节点下面 2 层（子节点 + 孙节点），不包括目标节点</li>
     *     <li>levels = 3：返回目标节点下面 3 层（子节点 + 孙节点 + 曾孙节点），不包括目标节点</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取 ID 为 1 的节点下面 2 层（子节点 + 孙节点），不包括节点 1
     * List<MenuNode> subTree = TreeUtil.getSubTree(
     *     tree,
     *     MenuNode::getId,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     1L,
     *     2
     * );
     * }</pre>
     *
     * @param tree           树结构根节点列表，可为 null 或空
     * @param idGetter       获取 节点ID 的方法，不能为 null
     * @param childrenGetter 获取子节点集合的方法，不能为 null
     * @param childrenSetter 设置子节点集合的方法，不能为 null
     * @param nodeId         目标节点ID
     * @param levels         往下的层级数（1 = 下面 1 层，2 = 下面 2 层，以此类推）
     * @param <T>            树节点类型
     * @param <ID>           节点ID 类型
     * @return 目标节点下面指定层级的子孙节点列表（不包括目标节点），没找到则返回空列表
     */
    public static <T, ID> List<T> getSubTree(
            List<T> tree, Function<T, ID> idGetter,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            ID nodeId, int levels
    ) {
        if (CollectionUtils.isEmpty(tree) || nodeId == null || levels <= 0) {
            return new ArrayList<>();
        }

        // 查找指定 ID 的节点
        T targetNode = findFirst(tree, childrenGetter, node -> Objects.equals(idGetter.apply(node), nodeId));
        if (targetNode == null) {
            return new ArrayList<>();
        }

        // 获取该节点的子节点
        List<T> children = childrenGetter.apply(targetNode);
        if (CollectionUtils.isEmpty(children)) {
            return new ArrayList<>();
        }

        // 收集该节点下面指定层级的子孙节点
        List<T> result = new ArrayList<>();
        collectSubTreeChildren(children, childrenGetter, childrenSetter, levels - 1, result);
        return result;
    }

    /**
     * 收集子节点下面指定层级的子孙节点
     * <p>
     * 遍历子节点列表，对每个子节点保留其下面指定层级的子孙节点，并收集到结果列表中。<br>
     * 用于 getSubTree 的两个重载方法，避免代码重复。
     *
     * @param children       子节点列表
     * @param childrenGetter 获取子节点列表的函数
     * @param childrenSetter 设置子节点列表的函数
     * @param levels         要保留的层级数
     * @param result         结果列表（用于收集处理后的子节点）
     * @param <T>            树节点类型
     */
    private static <T> void collectSubTreeChildren(
            List<T> children,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            int levels, List<T> result
    ) {
        for (T child : children) {
            // 对每个子节点，保留其下面指定层级
            T subTree = getSubTreeNode(child, childrenGetter, childrenSetter, levels);
            if (subTree != null) {
                result.add(subTree);
            }
        }
    }

    /**
     * 获取单个节点指定层级的子树
     * <p>
     * 递归获取节点及其指定层级的子节点。
     * <p>
     * <b>层级说明：</b>
     * <ul>
     *     <li>levels = 0：只保留当前节点，清空所有子节点</li>
     *     <li>levels = 1：保留当前节点及其直接子节点，清空孙节点</li>
     *     <li>levels = 2：保留当前节点、子节点、孙节点，清空曾孙节点</li>
     * </ul>
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param childrenSetter 设置子节点列表的函数
     * @param levels         要保留的子节点层级数（0 = 不保留子节点，1 = 保留 1 层子节点，2 = 保留 2 层子孙节点）
     * @param <T>            树节点类型
     * @return 指定层级的子树节点
     */
    private static <T> T getSubTreeNode(
            T node, Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter, int levels
    ) {
        if (node == null || levels < 0) {
            return null;
        }

        // 获取当前节点的子节点列表
        List<T> children = childrenGetter.apply(node);

        // 如果 levels = 0，表示不保留任何子节点
        if (levels == 0) {
            childrenSetter.accept(node, new ArrayList<>());
            return node;
        }

        // levels > 0：需要保留子节点，递归处理
        if (CollectionUtils.isNotEmpty(children)) {
            List<T> subChildren = new ArrayList<>();
            for (T child : children) {
                // 递归获取子节点的子树（层级减一）
                // levels = 1 时，子节点会收到 levels = 0，表示子节点不再保留其子节点
                T subChild = getSubTreeNode(child, childrenGetter, childrenSetter, levels - 1);
                if (subChild != null) {
                    subChildren.add(subChild);
                }
            }
            // 更新当前节点的子节点列表为处理后的子树
            childrenSetter.accept(node, subChildren);
        } else {
            // 当前节点没有子节点，保持为空列表
            childrenSetter.accept(node, new ArrayList<>());
        }

        return node;
    }

    // ==================== 安全版本方法（不修改原树结构） ====================

    /**
     * 构建树形结构（指定根节点 ID）- 安全版本
     * <p>
     * 将扁平化的列表转换为树形结构，从指定的根节点 ID 开始构建。<br>
     * <b>此方法不会修改原始数据</b>，会先进行深拷贝，适合需要保留原始数据的场景。
     * <p>
     * <b>与 build() 的区别：</b>
     * <ul>
     *     <li>build()：直接修改原始list中的节点，性能更好，适合一次性操作</li>
     *     <li>buildSafe()：先深拷贝再构建，不修改原始数据，适合需要保留原数据的场景</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * List<MenuNode> menuList = menuService.findAll();
     * // 构建树形结构，不影响原始 menuList
     * List<MenuNode> tree = TreeUtil.buildSafe(
     *     menuList,
     *     0L,
     *     MenuNode::getId,
     *     MenuNode::getParentId,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     MenuNode::new
     * );
     * // menuList 保持不变
     * }</pre>
     *
     * @param list           扁平化的节点列表，可以为 null 或空列表
     * @param rootId         根节点的 ID（通常为 0 或 null）
     * @param idGetter       获取节点 ID 的函数，不能为 null
     * @param parentIdGetter 获取父节点 ID 的函数，不能为 null
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param nodeSupplier   节点对象的创建函数（例如：MenuNode::new），用于深拷贝，不能为 null
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型（需要正确实现 equals 和 hashCode 方法）
     * @return 树形结构的根节点列表，如果 list 为 null 或空则返回空列表
     */
    public static <T, ID> List<T> buildSafe(
            List<T> list, ID rootId, Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 深拷贝列表，避免修改原始数据
        List<T> copiedList = BeanCopyUtil.copyListProperties(list, nodeSupplier);
        // 在拷贝的数据上构建树
        return build(copiedList, rootId, idGetter, parentIdGetter, childrenGetter, childrenSetter);
    }

    /**
     * 构建树形结构（自动识别根节点）- 安全版本
     * <p>
     * 将扁平化的列表转换为树形结构，自动识别根节点（父节点 ID 不在列表中的节点）。<br>
     * <b>此方法不会修改原始数据</b>，会先进行深拷贝，适合需要保留原始数据的场景。
     * <p>
     * <b>与 build() 的区别：</b>
     * <ul>
     *     <li>build()：直接修改原始list中的节点，性能更好，适合一次性操作</li>
     *     <li>buildSafe()：先深拷贝再构建，不修改原始数据，适合需要保留原数据的场景</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * List<MenuNode> menuList = menuService.findAll();
     * // 构建树形结构，不影响原始 menuList
     * List<MenuNode> tree = TreeUtil.buildSafe(
     *     menuList,
     *     MenuNode::getId,
     *     MenuNode::getParentId,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     MenuNode::new
     * );
     * // menuList 保持不变
     * }</pre>
     *
     * @param list           扁平化的节点列表，可以为 null 或空列表
     * @param idGetter       获取节点 ID 的函数，不能为 null
     * @param parentIdGetter 获取父节点 ID 的函数，不能为 null
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param nodeSupplier   节点对象的创建函数（例如：MenuNode::new），用于深拷贝，不能为 null
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型（需要正确实现 equals 和 hashCode 方法）
     * @return 树形结构的根节点列表，如果 list 为 null 或空则返回空列表
     */
    public static <T, ID> List<T> buildSafe(
            List<T> list, Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 深拷贝列表，避免修改原始数据
        List<T> copiedList = BeanCopyUtil.copyListProperties(list, nodeSupplier);
        // 在拷贝的数据上构建树
        return build(copiedList, idGetter, parentIdGetter, childrenGetter, childrenSetter);
    }

    /**
     * 过滤树形结构 - 安全版本
     * <p>
     * 根据条件过滤树形结构，保留符合条件的节点及其祖先节点。<br>
     * <b>此方法不会修改原始树</b>，会先进行深拷贝，适合需要保留原始树的场景。
     * <p>
     * <b>与 filter() 的区别：</b>
     * <ul>
     *     <li>filter()：直接修改原始树节点，性能更好，适合一次性操作</li>
     *     <li>filterSafe()：先深拷贝再过滤，不修改原始树，适合需要保留原树的场景</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 过滤出所有启用的菜单，不影响原始 tree
     * List<MenuNode> filteredTree = TreeUtil.filterSafe(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     n -> n.getEnabled(),
     *     MenuNode::new
     * );
     * // tree 保持不变
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param predicate      过滤条件，不能为 null
     * @param nodeSupplier   节点对象的创建函数（例如：MenuNode::new），用于深拷贝，不能为 null
     * @param <T>            树节点类型
     * @return 过滤后的树形结构，如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> filterSafe(
            List<T> tree,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Predicate<T> predicate,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }
        // 深拷贝树结构
        List<T> copiedTree = deepCopyTree(tree, childrenGetter, childrenSetter, nodeSupplier);
        // 在拷贝的树上进行过滤
        return filter(copiedTree, childrenGetter, childrenSetter, predicate);
    }

    /**
     * 对树形结构进行排序 - 安全版本
     * <p>
     * 对树形结构的每一层节点进行排序。<br>
     * <b>此方法不会修改原始树</b>，会先进行深拷贝，适合需要保留原始树的场景。
     * <p>
     * <b>与 sort() 的区别：</b>
     * <ul>
     *     <li>sort()：直接修改原始树节点，性能更好，适合一次性操作</li>
     *     <li>sortSafe()：先深拷贝再排序，不修改原始树，适合需要保留原树的场景</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 按排序号升序排序，不影响原始 tree
     * List<MenuNode> sortedTree = TreeUtil.sortSafe(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     Comparator.comparing(MenuNode::getSort),
     *     MenuNode::new
     * );
     * // tree 保持不变
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param comparator     排序比较器，不能为 null
     * @param nodeSupplier   节点对象的创建函数（例如：MenuNode::new），用于深拷贝，不能为 null
     * @param <T>            树节点类型
     * @return 排序后的树形结构，如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> sortSafe(
            List<T> tree,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Comparator<T> comparator,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }
        // 深拷贝树结构
        List<T> copiedTree = deepCopyTree(tree, childrenGetter, childrenSetter, nodeSupplier);
        // 在拷贝的树上进行排序
        return sort(copiedTree, childrenGetter, childrenSetter, comparator);
    }

    /**
     * 获取指定层级的子树 - 安全版本
     * <p>
     * 获取所有根节点下面指定层级的子孙节点（不包括根节点本身）。<br>
     * <b>此方法不会修改原始树</b>，会先进行深拷贝，适合需要保留原始树的场景。
     * <p>
     * <b>与 getSubTree() 的区别：</b>
     * <ul>
     *     <li>getSubTree()：直接修改原始树节点，性能更好，适合一次性操作</li>
     *     <li>getSubTreeSafe()：先深拷贝再截取，不修改原始树，适合需要保留原树的场景</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取所有根节点下面 2 层，不影响原始 tree
     * List<MenuNode> subTree = TreeUtil.getSubTreeSafe(
     *     tree,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     2,
     *     MenuNode::new
     * );
     * // tree 保持不变
     * }</pre>
     *
     * @param tree           树形结构的根节点列表，可以为 null 或空列表
     * @param childrenGetter 获取子节点列表的函数，不能为 null
     * @param childrenSetter 设置子节点列表的函数，不能为 null
     * @param levels         往下的层级数（1 = 下面 1 层，2 = 下面 2 层，以此类推）
     * @param nodeSupplier   节点对象的创建函数（例如：MenuNode::new），用于深拷贝，不能为 null
     * @param <T>            树节点类型
     * @return 指定层级的子孙节点列表（不包括根节点），如果 tree 为 null 或空则返回空列表
     */
    public static <T> List<T> getSubTreeSafe(
            List<T> tree, Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter, int levels,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(tree) || levels <= 0) {
            return new ArrayList<>();
        }
        // 深拷贝树结构
        List<T> copiedTree = deepCopyTree(tree, childrenGetter, childrenSetter, nodeSupplier);
        // 在拷贝的树上进行截取
        return getSubTree(copiedTree, childrenGetter, childrenSetter, levels);
    }

    /**
     * 获取指定节点的子树（通过节点ID查找）- 安全版本
     * <p>
     * 在整棵树中查找指定ID的节点，并返回该节点下面指定层级的子孙节点（不包括该节点本身）。<br>
     * <b>此方法不会修改原始树</b>，会先进行深拷贝，适合需要保留原始树的场景。
     * <p>
     * <b>与 getSubTree() 的区别：</b>
     * <ul>
     *     <li>getSubTree()：直接修改原始树节点，性能更好，适合一次性操作</li>
     *     <li>getSubTreeSafe()：先深拷贝再截取，不修改原始树，适合需要保留原树的场景</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取 ID 为 1 的节点下面2层，不影响原始 tree
     * List<MenuNode> subTree = TreeUtil.getSubTreeSafe(
     *     tree,
     *     MenuNode::getId,
     *     MenuNode::getChildren,
     *     MenuNode::setChildren,
     *     1L,
     *     2,
     *     MenuNode::new
     * );
     * // tree 保持不变
     * }</pre>
     *
     * @param tree           树结构根节点列表，可为 null 或空
     * @param idGetter       获取节点ID的方法，不能为null
     * @param childrenGetter 获取子节点集合的方法，不能为null
     * @param childrenSetter 设置子节点集合的方法，不能为null
     * @param nodeId         目标节点ID
     * @param levels         往下的层级数（1=下面1层，2=下面2层，以此类推）
     * @param nodeSupplier   节点对象的创建函数（例如：MenuNode::new），用于深拷贝，不能为 null
     * @param <T>            树节点类型
     * @param <ID>           节点ID类型
     * @return 目标节点下面指定层级的子孙节点列表（不包括目标节点），没找到则返回空列表
     */
    public static <T, ID> List<T> getSubTreeSafe(
            List<T> tree, Function<T, ID> idGetter,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            ID nodeId, int levels,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(tree) || nodeId == null || levels <= 0) {
            return new ArrayList<>();
        }
        // 深拷贝树结构
        List<T> copiedTree = deepCopyTree(tree, childrenGetter, childrenSetter, nodeSupplier);
        // 在拷贝的树上进行截取
        return getSubTree(copiedTree, idGetter, childrenGetter, childrenSetter, nodeId, levels);
    }

    /**
     * 深拷贝树结构（安全版本方法的通用逻辑）
     * <p>
     * 递归深拷贝树结构，保持父子关系。<br>
     * 用于所有安全版本方法，避免代码重复。
     * <p>
     * <b>算法说明：</b>
     * <ul>
     *     <li>时间复杂度：O(n)，其中 n 为节点数量</li>
     *     <li>空间复杂度：O(h)，其中 h 为树的高度（递归调用栈）</li>
     *     <li>递归拷贝每个节点及其子节点，保持树结构</li>
     * </ul>
     *
     * @param tree           原始树结构
     * @param childrenGetter 获取子节点列表的函数
     * @param childrenSetter 设置子节点列表的函数
     * @param nodeSupplier   节点对象的创建函数
     * @param <T>            树节点类型
     * @return 深拷贝后的树结构
     */
    private static <T> List<T> deepCopyTree(
            List<T> tree,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Supplier<T> nodeSupplier
    ) {
        if (CollectionUtils.isEmpty(tree)) {
            return new ArrayList<>();
        }

        // 递归拷贝每个根节点及其子树
        List<T> copiedTree = new ArrayList<>();
        for (T node : tree) {
            T copiedNode = deepCopyNode(node, childrenGetter, childrenSetter, nodeSupplier);
            if (copiedNode != null) {
                copiedTree.add(copiedNode);
            }
        }

        return copiedTree;
    }

    /**
     * 深拷贝单个节点及其子树
     * <p>
     * 递归拷贝节点及其所有子节点，保持树结构。
     *
     * @param node           当前节点
     * @param childrenGetter 获取子节点列表的函数
     * @param childrenSetter 设置子节点列表的函数
     * @param nodeSupplier   节点对象的创建函数
     * @param <T>            树节点类型
     * @return 拷贝后的节点
     */
    private static <T> T deepCopyNode(
            T node,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            Supplier<T> nodeSupplier
    ) {
        if (node == null) {
            return null;
        }

        // 拷贝当前节点（浅拷贝属性）
        T copiedNode = BeanCopyUtil.copyProperties(node, nodeSupplier);

        // 获取原节点的子节点列表
        List<T> children = childrenGetter.apply(node);

        // 递归拷贝所有子节点
        if (CollectionUtils.isNotEmpty(children)) {
            List<T> copiedChildren = new ArrayList<>();
            for (T child : children) {
                T copiedChild = deepCopyNode(child, childrenGetter, childrenSetter, nodeSupplier);
                if (copiedChild != null) {
                    copiedChildren.add(copiedChild);
                }
            }
            // 设置拷贝节点的子节点列表
            childrenSetter.accept(copiedNode, copiedChildren);
        } else {
            // 没有子节点，设置为空列表
            childrenSetter.accept(copiedNode, new ArrayList<>());
        }

        return copiedNode;
    }
}