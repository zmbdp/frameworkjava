package com.zmbdp.common.core.excel;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Excel 大数值转换器<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>解决 Excel 中数值精度丢失问题（Excel 数值最大精度为 15 位）</li>
 *     <li>大于 15 位的数值自动转换为字符串格式，防止精度丢失</li>
 *     <li>小于等于 15 位的数值保持为数字格式</li>
 *     <li>支持 Long 类型数据的导入和导出</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>在 {@link com.zmbdp.common.core.utils.ExcelUtil} 中已自动注册</li>
 *     <li>导出时自动应用，无需手动配置</li>
 *     <li>适用于包含 Long 类型字段的实体类</li>
 * </ol>
 *
 * <p>
 * 注意事项：
 * <ul>
 *     <li>Excel 中数值类型最大精度为 15 位，超过会丢失精度</li>
 *     <li>大于 15 位的数字会被转换为文本格式</li>
 *     <li>导入时会自动将文本转换回 Long 类型</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
public class ExcelBigNumberConverter implements Converter<Long> {

    /**
     * 支持的 Java 类型
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>声明转换器支持的 Java 数据类型</li>
     *     <li>由 EasyExcel 框架调用以匹配转换器</li>
     * </ul>
     *
     * @return Class&lt;Long&gt; 返回 Long.class，表示支持 Long 类型
     */
    @Override
    public Class<Long> supportJavaTypeKey() {
        return Long.class;
    }

    /**
     * 支持的 Excel 数据类型
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>声明转换器支持的 Excel 单元格数据类型</li>
     *     <li>大数值使用字符串格式存储</li>
     * </ul>
     *
     * @return CellDataTypeEnum 返回 STRING，表示支持字符串类型
     */
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    /**
     * 将 Excel 单元格数据转换为 Java 对象
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>Excel 导入时将单元格数据转换为 Long 类型</li>
     *     <li>支持字符串格式的数字转换</li>
     * </ul>
     *
     * @param cellData            Excel 单元格数据
     * @param contentProperty     Excel 内容属性
     * @param globalConfiguration 全局配置
     * @return Long 转换后的 Long 类型数据
     */
    @Override
    public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        return Convert.toLong(cellData.getData());
    }

    /**
     * 将 Java 对象转换为 Excel 单元格数据
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>Excel 导出时将 Long 类型数据转换为单元格数据</li>
     *     <li>大于 15 位的数值转换为字符串格式</li>
     *     <li>小于等于 15 位的数值保持为数字格式</li>
     * </ul>
     *
     * @param object              Java 对象（Long 类型）
     * @param contentProperty     Excel 内容属性
     * @param globalConfiguration 全局配置
     * @return WriteCellData&lt;Object&gt; Excel 单元格数据对象
     */
    @Override
    public WriteCellData<Object> convertToExcelData(Long object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (ObjectUtil.isNotNull(object)) {
            String str = Convert.toStr(object);
            if (str.length() > 15) {
                return new WriteCellData<>(str);
            }
            WriteCellData<Object> cellData = new WriteCellData<>(new BigDecimal(object));
            cellData.setType(CellDataTypeEnum.NUMBER);
            return cellData;
        }
        // object 为 null 时返回空字符串
        return new WriteCellData<>("");
    }
}