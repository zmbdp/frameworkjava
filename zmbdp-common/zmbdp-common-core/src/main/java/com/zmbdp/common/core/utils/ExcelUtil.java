package com.zmbdp.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.zmbdp.common.core.excel.*;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Excel 工具类（基于 EasyExcel）<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>支持 Excel 文件的导入和导出</li>
 *     <li>支持数据校验和错误收集</li>
 *     <li>支持单元格合并功能</li>
 *     <li>支持模板导出（单表和多表）</li>
 *     <li>自动处理大数值精度问题</li>
 *     <li>自动适配列宽</li>
 * </ul>
 *
 * <p>
 * 使用前准备：
 * <ol>
 *     <li>实体类需要使用 EasyExcel 注解标记字段</li>
 *     <li>模板文件需要放置在 resource 目录下</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * // 导入
 * ExcelResult&lt;UserDTO&gt; result = ExcelUtil.importExcel(inputStream, UserDTO.class, true);
 *
 * // 导出
 * ExcelUtil.exportExcel(list, "用户列表", UserDTO.class, response);
 * </pre>
 *
 * <p>
 * 工具类说明：
 * <ul>
 *     <li>不允许实例化</li>
 *     <li>所有方法均为静态方法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelUtil {

    /**
     * 同步导入 Excel（适用于小数据量）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>数据量较小的 Excel 文件导入</li>
     *     <li>不需要数据校验的场景</li>
     *     <li>需要一次性获取所有数据的场景</li>
     * </ul>
     *
     * @param is    Excel 文件输入流
     * @param clazz 数据对象类型（需要与 Excel 表头对应）
     * @param <T>   数据对象泛型
     * @return List&lt;T&gt; 转换后的数据对象列表
     */
    public static <T> List<T> inputExcel(InputStream is, Class<T> clazz) {
        return EasyExcel.read(is).head(clazz).autoCloseStream(false).sheet().doReadSync();
    }

    /**
     * 使用校验监听器导入 Excel（异步导入，同步返回）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要数据校验的导入场景</li>
     *     <li>需要获取导入错误信息的场景</li>
     *     <li>数据量较大的 Excel 文件导入</li>
     * </ul>
     *
     * @param is         Excel 文件输入流
     * @param clazz      数据对象类型（需要与 Excel 表头对应）
     * @param isValidate 是否启用数据校验，true 表示启用，false 表示不校验
     * @param <T>        数据对象泛型
     * @return ExcelResult&lt;T&gt; Excel 导入结果对象（包含成功数据和错误信息）
     */
    public static <T> ExcelResult<T> inputExcel(InputStream is, Class<T> clazz, boolean isValidate) {
        DefaultExcelListener<T> listener = new DefaultExcelListener<>(isValidate);
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * 使用自定义监听器导入 Excel（异步导入，自定义返回）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>需要自定义导入处理逻辑的场景</li>
     *     <li>需要实现自定义数据校验或转换的场景</li>
     *     <li>需要实时处理导入数据的场景</li>
     * </ul>
     *
     * @param is       Excel 文件输入流
     * @param clazz    数据对象类型（需要与 Excel 表头对应）
     * @param listener 自定义监听器（实现 ExcelListener 接口）
     * @param <T>      数据对象泛型
     * @return ExcelResult&lt;T&gt; Excel 导入结果对象（由监听器返回）
     */
    public static <T> ExcelResult<T> inputExcel(InputStream is, Class<T> clazz, ExcelListener<T> listener) {
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * 导出 Excel 文件（不合并单元格）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>导出数据到 Excel 文件并直接响应给前端</li>
     *     <li>不需要合并单元格的场景</li>
     *     <li>简单的数据导出需求</li>
     * </ul>
     *
     * @param list      导出数据集合
     * @param sheetName 工作表名称（也会作为文件名）
     * @param clazz     实体类类型（用于生成表头）
     * @param response  HTTP 响应对象
     * @param <T>       数据对象泛型
     */
    public static <T> void outputExcel(List<T> list, String sheetName, Class<T> clazz, HttpServletResponse response) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            outputExcel(list, sheetName, clazz, false, os);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.EXCEL_EXPORT_FAILED);
        }
    }

    /**
     * 导出 Excel 文件（支持单元格合并）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>导出数据到 Excel 文件并直接响应给前端</li>
     *     <li>需要合并相同值的单元格的场景</li>
     *     <li>实体类字段使用了 {@link com.zmbdp.common.core.annotation.excel.CellMerge} 注解</li>
     * </ul>
     *
     * @param list      导出数据集合
     * @param sheetName 工作表名称（也会作为文件名）
     * @param clazz     实体类类型（用于生成表头）
     * @param merge     是否合并单元格，true 表示合并，false 表示不合并
     * @param response  HTTP 响应对象
     * @param <T>       数据对象泛型
     */
    public static <T> void outputExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, HttpServletResponse response) {
        try {
            resetResponse(sheetName, response);
            ServletOutputStream os = response.getOutputStream();
            outputExcel(list, sheetName, clazz, merge, os);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.EXCEL_EXPORT_FAILED);
        }
    }

    /**
     * 导出 Excel 文件到输出流（不合并单元格）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>导出数据到指定的输出流</li>
     *     <li>不需要合并单元格的场景</li>
     *     <li>需要自定义输出目标的场景（如文件、内存等）</li>
     * </ul>
     *
     * @param list      导出数据集合
     * @param sheetName 工作表名称
     * @param clazz     实体类类型（用于生成表头）
     * @param os        输出流
     * @param <T>       数据对象泛型
     */
    public static <T> void outputExcel(List<T> list, String sheetName, Class<T> clazz, OutputStream os) {
        outputExcel(list, sheetName, clazz, false, os);
    }

    /**
     * 导出 Excel 文件到输出流（支持单元格合并）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>导出数据到指定的输出流</li>
     *     <li>需要合并相同值的单元格的场景</li>
     *     <li>实体类字段使用了 {@link com.zmbdp.common.core.annotation.excel.CellMerge} 注解</li>
     *     <li>需要自定义输出目标的场景（如文件、内存等）</li>
     * </ul>
     *
     * @param list      导出数据集合
     * @param sheetName 工作表名称
     * @param clazz     实体类类型（用于生成表头）
     * @param merge     是否合并单元格，true 表示合并，false 表示不合并
     * @param os        输出流
     * @param <T>       数据对象泛型
     */
    public static <T> void outputExcel(List<T> list, String sheetName, Class<T> clazz, boolean merge, OutputStream os) {
        ExcelWriterSheetBuilder builder = EasyExcel.write(os, clazz)
                .autoCloseStream(false)
                // 自动适配
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 大数值自动转换 防止失真
                .registerConverter(new ExcelBigNumberConverter())
                .sheet(sheetName);
        if (merge) {
            // 合并处理器
            builder.registerWriteHandler(new CellMergeStrategy(list, true));
        }
        builder.doWrite(list);
    }

    /**
     * 单表多数据模板导出（模板格式为 {.属性}）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>使用预定义的 Excel 模板进行数据填充</li>
     *     <li>模板中每个对象占一行，使用 {.属性名} 格式填充数据</li>
     *     <li>导出数据到 HTTP 响应</li>
     * </ul>
     *
     * <p>模板示例：</p>
     * <pre>
     * 姓名        | 年龄 | 部门
     * {.name}    | {.age} | {.dept}
     * </pre>
     *
     * @param filename     文件名（不包含扩展名）
     * @param templatePath 模板路径（resource 目录下的相对路径，包含文件名），例如：excel/temp.xlsx
     *                     注意：模板文件必须放置在启动类对应的 resource 目录下
     * @param data         模板需要的数据列表（每个元素对应一行数据）
     * @param response     HTTP 响应对象
     */
    public static void exportTemplate(List<Object> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            // 设置响应头
            resetResponse(filename, response);
            // 响应流
            ServletOutputStream os = response.getOutputStream();
            // 模板导出
            exportTemplate(data, templatePath, os);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.EXCEL_EXPORT_FAILED);
        }
    }

    /**
     * 单表多数据模板导出（模板格式为 {.属性}）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>使用预定义的 Excel 模板进行数据填充</li>
     *     <li>模板中每个对象占一行，使用 {.属性名} 格式填充数据</li>
     *     <li>导出数据到指定的输出流</li>
     * </ul>
     *
     * <p>模板示例：</p>
     * <pre>
     * 姓名        | 年龄 | 部门
     * {.name}    | {.age} | {.dept}
     * </pre>
     *
     * @param templatePath 模板路径（resource 目录下的相对路径，包含文件名），例如：excel/temp.xlsx
     *                     注意：模板文件必须放置在启动类对应的 resource 目录下
     * @param data         模板需要的数据列表（每个元素对应一行数据）
     * @param os           输出流
     */
    public static void exportTemplate(List<Object> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = EasyExcel.write(os)
                .withTemplate(templateResource.getStream())
                .autoCloseStream(false)
                // 大数值自动转换 防止失真
                .registerConverter(new ExcelBigNumberConverter())
                .build();
        WriteSheet writeSheet = EasyExcel.writerSheet().build();
        if (CollUtil.isEmpty(data)) {
            throw new ServiceException(ResultCode.INVALID_PARA);
        }
        // 单表多数据导出 模板格式为 {.属性}
        for (Object d : data) {
            excelWriter.fill(d, writeSheet);
        }
        excelWriter.finish();
    }

    /**
     * 多表多数据模板导出（模板格式为 {key.属性}）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>使用预定义的 Excel 模板进行多组数据填充</li>
     *     <li>模板中可以使用多个不同的数据列表，使用 {key.属性名} 格式填充</li>
     *     <li>Map 的 key 对应模板中的 key，value 可以是单个对象或对象集合</li>
     *     <li>导出数据到 HTTP 响应</li>
     * </ul>
     *
     * <p>模板示例：</p>
     * <pre>
     * 用户列表：
     * 姓名        | 年龄
     * {users.name} | {users.age}
     *
     * 部门列表：
     * 部门名称
     * {departments.name}
     * </pre>
     *
     * @param filename     文件名（不包含扩展名）
     * @param templatePath 模板路径（resource 目录下的相对路径，包含文件名），例如：excel/temp.xlsx
     *                     注意：模板文件必须放置在启动类对应的 resource 目录下
     * @param data         模板需要的数据映射（key 对应模板中的 key，value 为数据对象或数据列表）
     * @param response     HTTP 响应对象
     */
    public static void exportTemplateMultiList(Map<String, Object> data, String filename, String templatePath, HttpServletResponse response) {
        try {
            resetResponse(filename, response);
            ServletOutputStream os = response.getOutputStream();
            exportTemplateMultiList(data, templatePath, os);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.EXCEL_EXPORT_FAILED);
        }
    }

    /**
     * 多表多数据模板导出（模板格式为 {key.属性}）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>使用预定义的 Excel 模板进行多组数据填充</li>
     *     <li>模板中可以使用多个不同的数据列表，使用 {key.属性名} 格式填充</li>
     *     <li>Map 的 key 对应模板中的 key，value 可以是单个对象或对象集合</li>
     *     <li>导出数据到指定的输出流</li>
     * </ul>
     *
     * <p>模板示例：</p>
     * <pre>
     * 用户列表：
     * 姓名        | 年龄
     * {users.name} | {users.age}
     *
     * 部门列表：
     * 部门名称
     * {departments.name}
     * </pre>
     *
     * @param templatePath 模板路径（resource 目录下的相对路径，包含文件名），例如：excel/temp.xlsx
     *                     注意：模板文件必须放置在启动类对应的 resource 目录下
     * @param data         模板需要的数据映射（key 对应模板中的 key，value 为数据对象或数据列表）
     * @param os           输出流
     */
    public static void exportTemplateMultiList(Map<String, Object> data, String templatePath, OutputStream os) {
        ClassPathResource templateResource = new ClassPathResource(templatePath);
        ExcelWriter excelWriter = EasyExcel.write(os)
                .withTemplate(templateResource.getStream())
                .autoCloseStream(false)
                // 大数值自动转换 防止失真
                .registerConverter(new ExcelBigNumberConverter())
                .build();
        WriteSheet writeSheet = EasyExcel.writerSheet().build();
        if (CollUtil.isEmpty(data)) {
            throw new ServiceException(ResultCode.INVALID_PARA);
        }
        for (Map.Entry<String, Object> map : data.entrySet()) {
            // 设置列表后续还有数据
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
            if (map.getValue() instanceof Collection) {
                // 多表导出必须使用 FillWrapper
                excelWriter.fill(new FillWrapper(map.getKey(), (Collection<?>) map.getValue()), fillConfig, writeSheet);
            } else {
                excelWriter.fill(map.getValue(), writeSheet);
            }
        }
        excelWriter.finish();
    }

    /**
     * 重置 HTTP 响应头（用于文件下载）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>设置响应头为 Excel 文件下载格式</li>
     *     <li>编码文件名以支持中文</li>
     *     <li>设置 Content-Type 为 Excel MIME 类型</li>
     * </ul>
     *
     * @param sheetName 工作表名称（会转换为文件名）
     * @param response  HTTP 响应对象
     * @throws UnsupportedEncodingException 编码异常
     */
    private static void resetResponse(String sheetName, HttpServletResponse response) throws UnsupportedEncodingException {
        String filename = encodingFilename(sheetName);
        FileUtil.setAttachmentResponseHeader(response, filename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
    }

    /**
     * 解析导出值（将数字转换为对应的文本，格式：0=男,1=女,2=未知）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将数据库中的枚举值转换为可读的文本</li>
     *     <li>支持单个值和多个值（使用分隔符分隔）的转换</li>
     *     <li>用于 Excel 导出时的数据转换</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * convertByExp("0", "0=男,1=女,2=未知", ",")  // 返回 "男"
     * convertByExp("0,1", "0=男,1=女,2=未知", ",")  // 返回 "男,女"
     * </pre>
     *
     * @param propertyValue 属性值（要转换的值，可以是单个值或分隔符分隔的多个值）
     * @param converterExp  转换表达式（格式：key=value,key=value），使用逗号分隔
     * @param separator     分隔符（用于分割多个值时的分隔符）
     * @return String 转换后的文本值
     */
    public static String convertByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(CommonConstants.COMMA_SEPARATOR);
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            // 添加数组长度检查，防止格式错误导致数组越界
            if (itemArray.length < 2) {
                continue; // 跳过格式错误的项
            }
            if (StringUtil.containsAny(propertyValue, separator)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[0].equals(value)) {
                        propertyString.append(itemArray[1]).append(separator);
                        break;
                    }
                }
            } else {
                if (itemArray[0].equals(propertyValue)) {
                    return itemArray[1];
                }
            }
        }
        return StringUtil.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 反向解析值（将文本转换为对应的数字，格式：男=0,女=1,未知=2）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>将可读的文本转换回数据库中的枚举值</li>
     *     <li>支持单个值和多个值（使用分隔符分隔）的转换</li>
     *     <li>用于 Excel 导入时的数据转换</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * reverseByExp("男", "男=0,女=1,未知=2", ",")  // 返回 "0"
     * reverseByExp("男,女", "男=0,女=1,未知=2", ",")  // 返回 "0,1"
     * </pre>
     *
     * @param propertyValue 属性值（要转换的文本，可以是单个值或分隔符分隔的多个值）
     * @param converterExp  转换表达式（格式：value=key,value=key），使用逗号分隔
     * @param separator     分隔符（用于分割多个值时的分隔符）
     * @return String 转换后的数字值
     */
    public static String reverseByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        String[] convertSource = converterExp.split(CommonConstants.COMMA_SEPARATOR);
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            // 添加数组长度检查，防止格式错误导致数组越界
            if (itemArray.length < 2) {
                continue; // 跳过格式错误的项
            }
            if (StringUtil.containsAny(propertyValue, separator)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[1].equals(value)) {
                        propertyString.append(itemArray[0]).append(separator);
                        break;
                    }
                }
            } else {
                if (itemArray[1].equals(propertyValue)) {
                    return itemArray[0];
                }
            }
        }
        return StringUtil.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 编码文件名（添加 UUID 前缀）
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>为文件名添加唯一标识，避免文件名冲突</li>
     *     <li>确保下载的文件名唯一</li>
     *     <li>自动添加 .xlsx 扩展名</li>
     * </ul>
     *
     * @param filename 原始文件名（不包含扩展名）
     * @return String 编码后的文件名（格式：UUID_文件名.xlsx）
     */
    public static String encodingFilename(String filename) {
        return IdUtil.fastSimpleUUID() + "_" + filename + ".xlsx";
    }
}