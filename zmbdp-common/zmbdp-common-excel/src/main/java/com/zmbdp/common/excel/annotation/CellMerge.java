package com.zmbdp.common.excel.annotation;

import com.zmbdp.common.excel.strategy.CellMergeStrategy;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.annotation.*;
import java.util.List;

/**
 * Excel 单元格合并注解
 * <p>
 * 用于标记需要合并的 Excel 列，相同值的连续单元格会自动合并。需配合 {@link CellMergeStrategy} 策略使用，
 * 在调用 {@link com.zmbdp.common.excel.util.ExcelUtil#outputExcel(List, String, Class, boolean, HttpServletResponse)}
 * 时设置 {@code merge=true} 启用合并功能。
 *
 * <p>
 * <b>合并规则：</b>
 * <ul>
 *     <li>只有连续相同值的单元格才会合并（非连续相同值不会合并）</li>
 *     <li>空值（null 或空字符串）不会参与合并，会被跳过</li>
 *     <li>合并后的单元格会自动应用居中样式（水平居中 + 垂直居中）</li>
 *     <li>至少需要 2 个连续相同值才会合并（单个单元格不合并）</li>
 * </ul>
 *
 * <p>
 * <b>使用方式：</b>
 * <ol>
 *     <li>在实体类字段上添加 {@code @CellMerge} 注解（通常与 {@code @ExcelProperty} 一起使用）</li>
 *     <li>调用 {@code ExcelUtil.outputExcel()} 时设置 {@code merge=true}</li>
 *     <li>ExcelUtil 会自动应用 {@link CellMergeStrategy} 策略进行合并</li>
 * </ol>
 *
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 示例 1：基本使用（使用字段顺序作为列索引）
 * public class UserDTO {
 *     @ExcelProperty("部门")
 *     @CellMerge  // 相同部门的连续单元格会自动合并
 *     private String department;
 *
 *     @ExcelProperty("姓名")
 *     private String name;  // 不合并
 * }
 *
 * // 导出时启用合并
 * ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, response);
 *
 * // 示例 2：指定列索引
 * public class OrderDTO {
 *     @ExcelProperty("订单号")
 *     private String orderNo;
 *
 *     @ExcelProperty("商品名称")
 *     private String productName;
 *
 *     @ExcelProperty("分类")
 *     @CellMerge(index = 0)  // 指定在第1列（索引0）合并
 *     private String category;
 * }
 *
 * // 示例 3：多个字段合并
 * public class ReportDTO {
 *     @ExcelProperty("年份")
 *     @CellMerge
 *     private String year;  // 年份列合并
 *
 *     @ExcelProperty("月份")
 *     @CellMerge
 *     private String month;  // 月份列合并
 *
 *     @ExcelProperty("金额")
 *     private BigDecimal amount;  // 不合并
 * }
 * }</pre>
 *
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>必须配合 {@code @ExcelProperty} 使用，否则字段不会被导出</li>
 *     <li>{@code index} 为 -1（默认值）时，使用字段在类中的声明顺序作为列索引</li>
 *     <li>{@code index} 指定具体值时，必须确保索引值在有效范围内（0 到字段总数-1）</li>
 *     <li>合并功能会增加导出时间，大数据量（>10000 行）时建议谨慎使用</li>
 *     <li>合并是基于值的 {@code equals()} 方法比较，确保字段类型正确实现了 equals 方法</li>
 *     <li>如果字段顺序与 Excel 列顺序不一致，建议使用 {@code index} 明确指定列索引</li>
 * </ul>
 *
 * <p>
 * <b>适用场景：</b>
 * <ul>
 *     <li>分组数据导出（如按部门、地区、时间等分组）</li>
 *     <li>报表类 Excel 导出，需要合并相同分类的单元格</li>
 *     <li>层级数据展示，提升 Excel 可读性</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see CellMergeStrategy
 * @see com.zmbdp.common.excel.util.ExcelUtil#outputExcel(List, String, Class, boolean, HttpServletResponse)
 * @see com.alibaba.excel.annotation.ExcelProperty
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CellMerge {

    /**
     * 列索引（从 0 开始）
     * <p>
     * 用于指定需要合并的列在 Excel 中的位置。当字段在类中的声明顺序与 Excel 列顺序不一致时，
     * 需要明确指定列索引。
     *
     * <p>
     * <b>取值说明：</b>
     * <ul>
     *     <li><b>-1（默认值）</b>：使用字段在类中的声明顺序作为列索引</li>
     *     <li><b>0 或正整数</b>：指定具体的列索引位置（从 0 开始，0 表示第 1 列）</li>
     * </ul>
     *
     * <p>
     * <b>使用建议：</b>
     * <ul>
     *     <li>如果字段顺序与 Excel 列顺序一致，使用默认值 -1 即可</li>
     *     <li>如果字段顺序与 Excel 列顺序不一致（如使用了 {@code @ExcelProperty#index}），
     *         需要明确指定 {@code index} 值</li>
     *     <li>确保指定的索引值在有效范围内（0 到字段总数-1），否则可能导致合并位置错误</li>
     * </ul>
     *
     * <p>
     * <b>示例：</b>
     * <pre>{@code
     * // 示例 1：使用默认值（字段顺序作为列索引）
     * @CellMerge  // index 默认为 -1，使用字段顺序
     * private String department;
     *
     * // 示例 2：指定列索引
     * @CellMerge(index = 2)  // 在第 3 列（索引 2）合并
     * private String category;
     *
     * // 示例 3：字段顺序与 Excel 列顺序不一致时
     * @ExcelProperty(index = 5)  // Excel 中在第 6 列
     * @CellMerge(index = 5)       // 合并时也要指定索引 5
     * private String region;
     * }</pre>
     *
     * @return 列索引，默认 -1（使用字段在类中的声明顺序）
     */
    int index() default -1;
}