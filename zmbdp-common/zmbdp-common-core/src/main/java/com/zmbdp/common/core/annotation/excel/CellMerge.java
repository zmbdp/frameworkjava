package com.zmbdp.common.core.annotation.excel;

import com.zmbdp.common.core.excel.CellMergeStrategy;

import java.lang.annotation.*;

/**
 * Excel 列单元格合并注解<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>标注在实体类字段上，用于标记需要合并的列</li>
 *     <li>相同值的连续单元格会自动合并</li>
 *     <li>需搭配 {@link CellMergeStrategy} 策略使用</li>
 *     <li>支持指定列索引或使用字段顺序</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>在实体类的字段上添加该注解</li>
 *     <li>在导出 Excel 时启用合并功能（merge=true）</li>
 *     <li>ExcelUtil 会自动应用 {@link CellMergeStrategy} 策略</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * public class UserDTO {
 *     &#64;CellMerge
 *     private String department;  // 相同部门会自动合并
 *
 *     &#64;CellMerge(index = 2)
 *     private String group;  // 指定在第3列（索引2）合并
 *
 *     private String name;  // 不合并
 * }
 * </pre>
 *
 * <p>
 * 注意事项：
 * <ul>
 *     <li>空值不会进行合并</li>
 *     <li>只有连续相同的值才会合并</li>
 *     <li>index 为 -1 时使用字段在类中的顺序作为列索引</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CellMerge {

    /**
     * 列索引（从 0 开始）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>指定合并列的索引位置</li>
     *     <li>当为 -1 时，使用字段在类中的顺序作为列索引</li>
     *     <li>当指定具体索引时，使用指定的索引值</li>
     * </ul>
     *
     * @return int 列索引，默认 -1（使用字段顺序）
     */
    int index() default -1;
}