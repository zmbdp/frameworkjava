package com.zmbdp.common.domain.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 树形节点基类
 * <p>
 * 提供树形结构的基础属性，可以直接使用或继承扩展。<br>
 * 适用于菜单树、组织架构树、分类树等场景。
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 方式1：直接使用
 * TreeNode<Long> node = new TreeNode<>();
 * node.setId(1L);
 * node.setParentId(0L);
 * node.setData(menuEntity);
 *
 * // 方式2：继承扩展
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class MenuNode extends TreeNode<Long> {
 *     private String name;
 *     private String url;
 *     private Integer sort;
 * }
 * }</pre>
 *
 * @param <ID> 节点 ID 类型
 * @author 稚名不带撇
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode<ID> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点 ID
     */
    private ID id;

    /**
     * 父节点 ID
     */
    private ID parentId;

    /**
     * 子节点列表
     */
    private List<TreeNode<ID>> children;

    /**
     * 节点数据（可选，用于存储业务对象）
     */
    private Object data;

    /**
     * 构造函数（不包含 data）
     *
     * @param id       节点 ID
     * @param parentId 父节点 ID
     */
    public TreeNode(ID id, ID parentId) {
        this.id = id;
        this.parentId = parentId;
    }
}