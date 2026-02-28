package com.zmbdp.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 树形结构工具类
 * <p>
 * 提供便捷的树形结构构建、遍历、查找、转换等功能，支持任意类型的树节点对象。<br>
 * 适用于菜单树、组织架构树、分类树、权限树等各种树形数据结构的处理。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>树形结构构建：将扁平化列表转换为树形结构</li>
 *     <li>树形结构遍历：深度优先遍历、广度优先遍历</li>
 *     <li>树形结构查找：根据条件查找节点、查找路径</li>
 *     <li>树形结构转换：树转列表、提取所有叶子节点</li>
 *     <li>树形结构过滤：根据条件过滤节点</li>
 *     <li>树形结构排序：对树节点进行排序</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>菜单树构建与展示</li>
 *     <li>组织架构树管理</li>
 *     <li>商品分类树处理</li>
 *     <li>权限树构建</li>
 *     <li>文件目录树展示</li>
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
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 构建 ID -> 节点的映射，方便快速查找
        Map<ID, T> nodeMap = list.stream().collect(
                Collectors.toMap(
                        idGetter, Function.identity(),
                        (o1, o2) -> o1
                )
        );

        // 存储根节点
        List<T> rootNodes = new ArrayList<>();

        // 遍历所有节点，构建父子关系
        for (T node : list) {
            ID parentId = parentIdGetter.apply(node);

            // 判断是否为根节点
            if (Objects.equals(parentId, rootId)) {
                rootNodes.add(node);
            } else {
                // 查找父节点
                establishParentChildRelation(childrenGetter, childrenSetter, node, nodeMap, parentId);
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
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }

        // 构建 ID -> 节点的映射
        Map<ID, T> nodeMap = list.stream().collect(
                Collectors.toMap(
                        idGetter, Function.identity(),
                        (o1, o2) -> o1
                )
        );

        // 收集所有存在的 ID
        Set<ID> existingIds = nodeMap.keySet();

        // 存储根节点
        List<T> rootNodes = new ArrayList<>();

        // 遍历所有节点，构建父子关系
        for (T node : list) {
            ID parentId = parentIdGetter.apply(node);

            // 判断是否为根节点（父节点 ID 为 null 或父节点不存在）
            if (parentId == null || !existingIds.contains(parentId)) {
                rootNodes.add(node);
            } else {
                // 查找父节点
                establishParentChildRelation(childrenGetter, childrenSetter, node, nodeMap, parentId);
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
     * @param childrenSetter 设置子节点列表的函数
     * @param node           子节点
     * @param nodeMap        节点 ID 到节点的映射
     * @param parentId       父节点 ID
     * @param <T>            树节点类型
     * @param <ID>           节点 ID 类型
     */
    private static <T, ID> void establishParentChildRelation(
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter,
            T node, Map<ID, T> nodeMap, ID parentId
    ) {
        T parentNode = nodeMap.get(parentId);
        if (parentNode != null) {
            // 获取父节点的子节点列表（如果没有则创建）
            List<T> children = childrenGetter.apply(parentNode);
            if (children == null) {
                children = new ArrayList<>();
                childrenSetter.accept(parentNode, children);
            }
            children.add(node);
        }
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

        List<T> result = new ArrayList<>();
        for (T node : tree) {
            traverseDepthFirst(node, childrenGetter, result::add);
        }
        return result;
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

        // 先处理当前节点（先序遍历）
        consumer.accept(node);

        // 再递归处理子节点
        List<T> children = childrenGetter.apply(node);
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

        Queue<T> queue = new LinkedList<>(tree);

        while (!queue.isEmpty()) {
            T node = queue.poll();
            consumer.accept(node);

            List<T> children = childrenGetter.apply(node);
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

        for (T node : tree) {
            T found = findFirst(node, childrenGetter, predicate);
            if (found != null) {
                return found;
            }
        }

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

        // 先判断当前节点是否符合条件（提前返回，优化性能）
        if (predicate.test(node)) {
            return node;
        }

        // 再递归查找子节点
        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                T found = findFirst(child, childrenGetter, predicate);
                if (found != null) {
                    return found; // 找到后立即返回，避免不必要的遍历
                }
            }
        }

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
        List<T> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(tree)) {
            return result;
        }

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
            result.add(node);
        }

        // 递归查找子节点（无论当前节点是否符合条件，都要继续查找子节点）
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

        List<T> result = new ArrayList<>();
        for (T node : tree) {
            T filtered = filter(node, childrenGetter, childrenSetter, predicate);
            if (filtered != null) {
                result.add(filtered);
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

        // 递归过滤子节点
        List<T> children = childrenGetter.apply(node);
        List<T> filteredChildren = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                T filtered = filter(child, childrenGetter, childrenSetter, predicate);
                if (filtered != null) {
                    filteredChildren.add(filtered);
                }
            }
        }

        // 如果当前节点符合条件，或者有符合条件的子节点，则保留当前节点
        if (predicate.test(node) || !filteredChildren.isEmpty()) {
            childrenSetter.accept(node, filteredChildren);
            return node;
        }

        // 节点及其子树都不符合条件，返回 null
        return null;
    }

    /**
     * 对树形结构进行排序
     * <p>
     * 对树形结构的每一层节点进行排序。<br>
     * 适用于需要对树节点排序的场景，如按名称、序号等排序。
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

        // 对当前层排序
        List<T> sortedList = new ArrayList<>(tree);
        sortedList.sort(comparator);

        // 递归对子节点排序
        for (T node : sortedList) {
            List<T> children = childrenGetter.apply(node);
            if (CollectionUtils.isNotEmpty(children)) {
                List<T> sortedChildren = sort(children, childrenGetter, childrenSetter, comparator);
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

        for (T node : tree) {
            List<T> path = new ArrayList<>();
            if (findPath(node, childrenGetter, predicate, path)) {
                return path;
            }
        }

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

        // 将当前节点加入路径
        path.add(node);

        // 判断当前节点是否为目标节点
        if (predicate.test(node)) {
            return true;
        }

        // 递归查找子节点
        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                if (findPath(child, childrenGetter, predicate, path)) {
                    return true; // 找到目标节点，返回 true
                }
            }
        }

        // 如果没有找到，移除当前节点（回溯）
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
            return 0;
        }

        int maxDepth = 0;
        for (T node : tree) {
            int depth = getMaxDepth(node, childrenGetter, 1);
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

        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isEmpty(children)) {
            return currentDepth; // 叶子节点，返回当前深度
        }

        int maxDepth = currentDepth;
        for (T child : children) {
            int depth = getMaxDepth(child, childrenGetter, currentDepth + 1);
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
            return 0;
        }

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

        int count = 1; // 当前节点计数

        List<T> children = childrenGetter.apply(node);
        if (CollectionUtils.isNotEmpty(children)) {
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
        List<T> path = findPath(tree, childrenGetter, predicate);
        if (path.isEmpty()) {
            return new ArrayList<>();
        }

        // 移除最后一个元素（目标节点本身）
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
        T node = findFirst(tree, childrenGetter, predicate);
        if (node == null) {
            return new ArrayList<>();
        }

        List<T> descendants = new ArrayList<>();
        List<T> children = childrenGetter.apply(node);

        if (CollectionUtils.isNotEmpty(children)) {
            for (T child : children) {
                traverseDepthFirst(child, childrenGetter, descendants::add);
            }
        }

        return descendants;
    }
}