package com.zmbdp.common.core.excel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import com.zmbdp.common.core.annotation.excel.CellMerge;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 单元格合并策略类<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>基于 {@link CellMerge} 注解实现单元格合并</li>
 *     <li>支持相同值的连续单元格自动合并</li>
 *     <li>支持指定列索引或使用字段顺序</li>
 *     <li>空值跳过，不进行合并</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>在实体类的字段上添加 {@link CellMerge} 注解</li>
 *     <li>在导出 Excel 时传入 {@link CellMergeStrategy} 策略</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * public class UserDTO {
 *     &#64;CellMerge
 *     private String department;  // 相同部门会自动合并
 *
 *     private String name;
 * }
 *
 * ExcelUtil.exportExcel(list, "用户列表", UserDTO.class, true, response);
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@AllArgsConstructor
public class CellMergeStrategy extends AbstractMergeStrategy {

    /**
     * 需要合并的数据列表
     */
    private List<?> list;

    /**
     * 是否有标题行（true 表示第一行是标题，从第二行开始合并）
     */
    private boolean hasTitle;

    /**
     * 重复单元格信息（内部类）
     *
     * <p>用于记录需要合并的单元格值和起始行索引</p>
     *
     * @author 稚名不带撇
     */
    @Data
    @AllArgsConstructor
    static class RepeatCell {

        /**
         * 列值（用于判断是否需要合并）
         */
        private Object value;

        /**
         * 当前行索引（合并起始行）
         */
        private int current;
    }

    /**
     * 处理单元格合并逻辑
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>扫描实体类字段上的 {@link CellMerge} 注解</li>
     *     <li>计算相同值的连续单元格范围</li>
     *     <li>生成合并区域列表</li>
     * </ul>
     *
     * @param list     需要合并的数据列表
     * @param hasTitle 是否有标题行
     * @return List&lt;CellRangeAddress&gt; 合并区域列表
     */
    @SneakyThrows
    private static List<CellRangeAddress> handle(List<?> list, boolean hasTitle) {
        // 初始化合并区域列表
        List<CellRangeAddress> cellList = new ArrayList<>();

        // 如果数据列表为空，直接返回空列表
        if (CollectionUtils.isEmpty(list)) {
            return cellList;
        }

        // 获取数据对象的类型，用于反射获取字段和方法
        Class<?> clazz = list.get(0).getClass();
        // 获取类中声明的所有字段
        Field[] fields = clazz.getDeclaredFields();

        // 收集需要合并的字段（带有 @CellMerge 注解的字段）
        List<Field> mergeFields = new ArrayList<>();
        // 收集每个合并字段对应的列索引
        List<Integer> mergeFieldsIndex = new ArrayList<>();

        // 遍历所有字段，查找带有 @CellMerge 注解的字段
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.isAnnotationPresent(CellMerge.class)) {
                CellMerge cm = field.getAnnotation(CellMerge.class);
                mergeFields.add(field);
                // 如果注解的 index 为 -1，使用字段在类中的顺序作为列索引；否则使用注解指定的索引
                mergeFieldsIndex.add(cm.index() == -1 ? i : cm.index());
            }
        }

        // 计算数据行的起始索引：如果有标题行，数据从第2行（索引1）开始；否则从第1行（索引0）开始
        int rowIndex = hasTitle ? 1 : 0;

        // 用于记录每个字段的当前值和起始行索引（用于判断是否需要合并）
        Map<Field, RepeatCell> map = new HashMap<>();

        // 遍历数据列表，计算相同值的连续单元格范围
        for (int i = 0; i < list.size(); i++) {
            // 遍历每个需要合并的字段
            for (int j = 0; j < mergeFields.size(); j++) {
                Field field = mergeFields.get(j);
                // 根据字段名生成 getter 方法名（如：name -> getName）
                String name = field.getName();
                String methodName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
                // 通过反射获取 getter 方法
                Method readMethod = clazz.getMethod(methodName);
                // 调用 getter 方法获取当前行的字段值
                Object val = readMethod.invoke(list.get(i));

                // 获取该字段对应的列索引
                int colNum = mergeFieldsIndex.get(j);

                // 如果是第一次遇到该字段，记录当前值和行索引
                if (!map.containsKey(field)) {
                    map.put(field, new RepeatCell(val, i));
                } else {
                    // 获取之前记录的单元格信息
                    RepeatCell repeatCell = map.get(field);
                    Object cellValue = repeatCell.getValue();

                    // 如果之前的值是空值，跳过不合并（空值不参与合并）
                    if (cellValue == null || "".equals(cellValue)) {
                        // 更新为当前值，继续下一行
                        map.put(field, new RepeatCell(val, i));
                        continue;
                    }

                    // 如果当前值与之前的值不同，说明需要结束之前的合并区域
                    if (!cellValue.equals(val)) {
                        // 如果连续相同值的行数大于1（即 i - repeatCell.getCurrent() > 1），才需要合并
                        if (i - repeatCell.getCurrent() > 1) {
                            // 创建合并区域：从起始行到当前行的上一行，同一列
                            cellList.add(new CellRangeAddress(
                                    repeatCell.getCurrent() + rowIndex,  // 起始行（加上标题行偏移）
                                    i + rowIndex - 1,                     // 结束行（当前行的上一行）
                                    colNum,                               // 列索引
                                    colNum                                // 同一列
                            ));
                        }
                        // 更新为新的值和起始行索引
                        map.put(field, new RepeatCell(val, i));
                    }
                    // 如果当前值与之前的值相同，且是最后一行，需要合并到最后一行
                    else if (i == list.size() - 1) {
                        // 如果最后一行与起始行不是同一行（即 i > repeatCell.getCurrent()），需要合并
                        if (i > repeatCell.getCurrent()) {
                            // 创建合并区域：从起始行到最后一行，同一列
                            cellList.add(new CellRangeAddress(
                                    repeatCell.getCurrent() + rowIndex,  // 起始行
                                    i + rowIndex,                        // 结束行（最后一行）
                                    colNum,                              // 列索引
                                    colNum                               // 同一列
                            ));
                        }
                    }
                }
            }
        }
        return cellList;
    }

    /**
     * 执行单元格合并操作
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>在 Excel 写入过程中被 EasyExcel 框架调用</li>
     *     <li>将计算好的合并区域应用到 Sheet 中</li>
     *     <li>只在第一个单元格（第2行第1列）时执行合并操作</li>
     * </ul>
     *
     * @param sheet            Excel Sheet 对象
     * @param cell             当前单元格
     * @param head             表头信息
     * @param relativeRowIndex 相对行索引
     */
    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        List<CellRangeAddress> cellList = handle(list, hasTitle);
        // judge the list is not null
        if (CollectionUtils.isNotEmpty(cellList)) {
            // the judge is necessary
            if (cell.getRowIndex() == 1 && cell.getColumnIndex() == 0) {
                // 创建居中样式（在循环外创建，避免重复创建）
                CellStyle centerStyle = sheet.getWorkbook().createCellStyle();
                centerStyle.setAlignment(HorizontalAlignment.CENTER);
                centerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
                // 设置自动换行，确保垂直居中效果更好
                centerStyle.setWrapText(false);

                for (CellRangeAddress item : cellList) {
                    sheet.addMergedRegion(item);

                    // 对合并区域内的所有行和单元格应用居中样式
                    int firstRow = item.getFirstRow();
                    int lastRow = item.getLastRow();
                    int colIndex = item.getFirstColumn();

                    // 遍历合并区域内的所有行
                    for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            Cell mergedCell = row.getCell(colIndex);
                            if (mergedCell != null) {
                                // 设置合并单元格的样式为居中（水平和垂直都居中）
                                mergedCell.setCellStyle(centerStyle);
                            } else {
                                // 如果单元格不存在，创建一个并设置样式
                                mergedCell = row.createCell(colIndex);
                                mergedCell.setCellStyle(centerStyle);
                            }
                        }
                    }
                }
            }
        }
    }
}
