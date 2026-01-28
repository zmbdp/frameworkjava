package com.zmbdp.common.excel.util;

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
import com.zmbdp.common.core.utils.FileUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.excel.annotation.CellMerge;
import com.zmbdp.common.excel.converter.ExcelBigNumberConverter;
import com.zmbdp.common.excel.listener.DefaultExcelListener;
import com.zmbdp.common.excel.listener.ExcelListener;
import com.zmbdp.common.excel.result.ExcelResult;
import com.zmbdp.common.excel.strategy.CellMergeStrategy;
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
 * Excel 工具类（基于 EasyExcel）
 * <p>
 * 提供 Excel 文件的导入、导出、模板填充等功能，基于阿里巴巴的 EasyExcel 库实现。<br>
 * 支持大数据量处理、数据校验、单元格合并、模板导出等高级特性。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>支持 Excel 文件的导入和导出（.xlsx 格式）</li>
 *     <li>支持数据校验和错误收集</li>
 *     <li>支持单元格合并功能（基于注解）</li>
 *     <li>支持模板导出（单表和多表）</li>
 *     <li>自动处理大数值精度问题（防止科学计数法）</li>
 *     <li>自动适配列宽（根据内容自动调整）</li>
 *     <li>支持同步和异步导入</li>
 *     <li>支持自定义监听器</li>
 * </ul>
 * <p>
 * <b>使用前准备：</b>
 * <ol>
 *     <li>实体类需要使用 EasyExcel 注解标记字段（如 @ExcelProperty）</li>
 *     <li>模板文件需要放置在 resource 目录下</li>
 *     <li>导入时，Excel 表头必须与实体类字段对应</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 同步导入（小数据量）
 * List<UserDTO> list = ExcelUtil.inputExcel(inputStream, UserDTO.class);
 *
 * // 2. 带校验的导入
 * ExcelResult<UserDTO> result = ExcelUtil.inputExcel(inputStream, UserDTO.class, true);
 * List<UserDTO> successList = result.getList();
 * List<String> errorMsg = result.getErrorList();
 *
 * // 3. 简单导出
 * ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, response);
 *
 * // 4. 带合并单元格的导出
 * ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, response);
 *
 * // 5. 模板导出
 * ExcelUtil.exportTemplate(dataList, "导出文件", "excel/template.xlsx", response);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>导出到 HTTP 响应时，会自动设置响应头</li>
 *     <li>大数值（如 Long 类型）会自动转换为字符串，防止精度丢失</li>
 *     <li>模板路径必须是 resource 目录下的相对路径</li>
 *     <li>导入时如果数据校验失败，错误信息会收集到 ExcelResult 中</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.alibaba.excel.EasyExcel
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelUtil {

    /**
     * 同步导入 Excel（适用于小数据量）
     * <p>
     * 一次性读取整个 Excel 文件并转换为对象列表，适用于数据量较小（建议小于 1000 行）的场景。<br>
     * 该方法会阻塞直到所有数据读取完成。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 从文件输入流导入
     * FileInputStream fis = new FileInputStream("users.xlsx");
     * List<UserDTO> userList = ExcelUtil.inputExcel(fis, UserDTO.class);
     *
     * // 从 HTTP 请求导入
     * MultipartFile file = request.getFile("excel");
     * List<UserDTO> userList = ExcelUtil.inputExcel(file.getInputStream(), UserDTO.class);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>数据量较小的 Excel 文件导入（建议小于 1000 行）</li>
     *     <li>不需要数据校验的场景</li>
     *     <li>需要一次性获取所有数据的场景</li>
     *     <li>简单的数据导入需求</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果数据量较大，建议使用带校验的导入方法，避免内存溢出</li>
     *     <li>不会进行数据校验，所有数据都会被读取</li>
     *     <li>如果 Excel 格式错误或表头不匹配，可能抛出异常</li>
     *     <li>输入流不会被自动关闭，需要调用方自行处理</li>
     * </ul>
     *
     * @param is    Excel 文件输入流，不能为 null
     * @param clazz 数据对象类型（需要与 Excel 表头对应），必须使用 EasyExcel 注解标记字段
     * @param <T>   数据对象泛型
     * @return 转换后的数据对象列表，如果 Excel 为空则返回空列表
     * @throws RuntimeException 当 Excel 格式错误或读取失败时抛出异常
     */
    public static <T> List<T> inputExcel(InputStream is, Class<T> clazz) {
        return EasyExcel.read(is).head(clazz).autoCloseStream(false).sheet().doReadSync();
    }

    /**
     * 使用校验监听器导入 Excel（异步导入，同步返回）
     * <p>
     * 使用默认的数据校验监听器导入 Excel，支持数据校验和错误收集。<br>
     * 采用异步读取方式，适合大数据量导入，读取完成后同步返回结果。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 带校验的导入
     * ExcelResult<UserDTO> result = ExcelUtil.inputExcel(inputStream, UserDTO.class, true);
     * List<UserDTO> successList = result.getList();  // 成功导入的数据
     * List<String> errorList = result.getErrorList(); // 错误信息列表
     *
     * // 不带校验的导入
     * ExcelResult<UserDTO> result2 = ExcelUtil.inputExcel(inputStream, UserDTO.class, false);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>需要数据校验的导入场景</li>
     *     <li>需要获取导入错误信息的场景</li>
     *     <li>数据量较大的 Excel 文件导入（推荐使用）</li>
     *     <li>需要区分成功和失败数据的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>isValidate 为 true 时，会使用 JSR-303 注解进行数据校验</li>
     *     <li>校验失败的数据不会添加到成功列表，错误信息会收集到错误列表</li>
     *     <li>错误信息格式：第 N 行：错误原因</li>
     *     <li>即使部分数据校验失败，其他数据仍会正常导入</li>
     *     <li>输入流不会被自动关闭，需要调用方自行处理</li>
     * </ul>
     *
     * @param is         Excel 文件输入流，不能为 null
     * @param clazz      数据对象类型（需要与 Excel 表头对应），必须使用 EasyExcel 注解标记字段
     * @param isValidate 是否启用数据校验，true 表示启用 JSR-303 校验，false 表示不校验
     * @param <T>        数据对象泛型
     * @return Excel 导入结果对象，包含成功数据列表和错误信息列表
     * @throws RuntimeException 当 Excel 格式错误或读取失败时抛出异常
     * @see ExcelResult
     */
    public static <T> ExcelResult<T> inputExcel(InputStream is, Class<T> clazz, boolean isValidate) {
        DefaultExcelListener<T> listener = new DefaultExcelListener<>(isValidate);
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * 使用自定义监听器导入 Excel（异步导入，自定义返回）
     * <p>
     * 使用自定义的监听器导入 Excel，可以实现自定义的数据处理逻辑、校验规则或转换逻辑。<br>
     * 适合需要特殊处理逻辑的导入场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 创建自定义监听器
     * ExcelListener<UserDTO> customListener = new ExcelListener<UserDTO>() {
     *     @Override
     *     public void invoke(UserDTO data, AnalysisContext context) {
     *         // 自定义处理逻辑
     *         // 例如：实时保存到数据库
     *         userService.save(data);
     *     }
     *
     *     @Override
     *     public ExcelResult<UserDTO> getExcelResult() {
     *         // 返回自定义结果
     *         return new ExcelResult<>();
     *     }
     * };
     *
     * ExcelResult<UserDTO> result = ExcelUtil.inputExcel(inputStream, UserDTO.class, customListener);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>需要自定义导入处理逻辑的场景</li>
     *     <li>需要实现自定义数据校验或转换的场景</li>
     *     <li>需要实时处理导入数据的场景（如边读边保存）</li>
     *     <li>需要统计导入数据的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>listener 不能为 null</li>
     *     <li>监听器会在每行数据读取时被调用</li>
     *     <li>可以在监听器中实现批量处理逻辑以提高性能</li>
     *     <li>输入流不会被自动关闭，需要调用方自行处理</li>
     * </ul>
     *
     * @param is       Excel 文件输入流，不能为 null
     * @param clazz    数据对象类型（需要与 Excel 表头对应），必须使用 EasyExcel 注解标记字段
     * @param listener 自定义监听器（实现 ExcelListener 接口），不能为 null
     * @param <T>      数据对象泛型
     * @return Excel 导入结果对象（由监听器的 getExcelResult 方法返回）
     * @throws RuntimeException 当 Excel 格式错误或读取失败时抛出异常
     * @see ExcelListener
     */
    public static <T> ExcelResult<T> inputExcel(InputStream is, Class<T> clazz, ExcelListener<T> listener) {
        EasyExcel.read(is, clazz, listener).sheet().doRead();
        return listener.getExcelResult();
    }

    /**
     * 导出 Excel 文件到 HTTP 响应（不合并单元格）
     * <p>
     * 将数据列表导出为 Excel 文件并直接响应给前端，自动设置响应头，支持中文文件名。<br>
     * 适用于简单的数据导出需求，不需要合并单元格的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 导出用户列表
     * List<UserDTO> userList = userService.findAll();
     * ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, response);
     *
     * // 导出订单列表
     * List<OrderDTO> orderList = orderService.findAll();
     * ExcelUtil.outputExcel(orderList, "订单列表", OrderDTO.class, response);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>导出数据到 Excel 文件并直接响应给前端</li>
     *     <li>不需要合并单元格的场景</li>
     *     <li>简单的数据导出需求</li>
     *     <li>Web 接口导出功能</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>会自动设置响应头，包括 Content-Type 和文件名编码</li>
     *     <li>文件名会自动添加 UUID 前缀，避免文件名冲突</li>
     *     <li>如果 list 为 null 或空，会导出空表（只有表头）</li>
     *     <li>大数值会自动转换为字符串，防止精度丢失</li>
     *     <li>列宽会自动适配内容长度</li>
     *     <li>如果响应已提交，可能抛出异常</li>
     * </ul>
     *
     * @param list      导出数据集合，可以为 null 或空集合
     * @param sheetName 工作表名称（也会作为文件名），不能为 null
     * @param clazz     实体类类型（用于生成表头），必须使用 EasyExcel 注解标记字段
     * @param response  HTTP 响应对象，不能为 null
     * @param <T>       数据对象泛型
     * @throws ServiceException 当响应设置失败或导出失败时抛出异常
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
     * 导出 Excel 文件到 HTTP 响应（支持单元格合并）
     * <p>
     * 将数据列表导出为 Excel 文件并直接响应给前端，支持合并相同值的单元格。<br>
     * 需要在实体类字段上使用 {@link CellMerge} 注解标记需要合并的列。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 实体类示例
     * public class UserDTO {
     *     @CellMerge
     *     @ExcelProperty("部门")
     *     private String dept;
     *
     *     @ExcelProperty("姓名")
     *     private String name;
     * }
     *
     * // 导出（相同部门的单元格会合并）
     * List<UserDTO> userList = userService.findAll();
     * ExcelUtil.outputExcel(userList, "用户列表", UserDTO.class, true, response);
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>导出数据到 Excel 文件并直接响应给前端</li>
     *     <li>需要合并相同值的单元格的场景（如分组数据）</li>
     *     <li>实体类字段使用了 {@link CellMerge} 注解</li>
     *     <li>需要美化 Excel 格式的场景</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>merge 为 true 时，只有标记了 @CellMerge 注解的字段才会合并</li>
     *     <li>合并规则：相同列中连续相同值的单元格会被合并</li>
     *     <li>合并功能会增加导出时间，大数据量时建议谨慎使用</li>
     *     <li>其他注意事项同 {@link #outputExcel(List, String, Class, HttpServletResponse)}</li>
     * </ul>
     *
     * @param list      导出数据集合，可以为 null 或空集合
     * @param sheetName 工作表名称（也会作为文件名），不能为 null
     * @param clazz     实体类类型（用于生成表头），必须使用 EasyExcel 注解标记字段
     * @param merge     是否合并单元格，true 表示合并（需要 @CellMerge 注解），false 表示不合并
     * @param response  HTTP 响应对象，不能为 null
     * @param <T>       数据对象泛型
     * @throws ServiceException 当响应设置失败或导出失败时抛出异常
     * @see CellMerge
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
     *     <li>实体类字段使用了 {@link CellMerge} 注解</li>
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
     * 解析导出值（将数字转换为对应的文本）
     * <p>
     * 根据转换表达式将数字值转换为对应的文本值，用于 Excel 导出时的数据转换。<br>
     * 支持单个值和多个值（使用分隔符分隔）的转换。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 单个值转换
     * String gender = ExcelUtil.convertByExp("0", "0=男,1=女,2=未知", ",");
     * // 结果：gender = "男"
     *
     * // 多个值转换
     * String status = ExcelUtil.convertByExp("0,1", "0=待审核,1=已审核,2=已拒绝", ",");
     * // 结果：status = "待审核,已审核"
     *
     * // 在 EasyExcel 注解中使用
     * @ExcelProperty(value = "性别", converter = GenderConverter.class)
     * private String gender;
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>将数据库中的枚举值转换为可读的文本</li>
     *     <li>支持单个值和多个值（使用分隔符分隔）的转换</li>
     *     <li>用于 Excel 导出时的数据转换</li>
     *     <li>状态码、类型码等数字到文本的转换</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>converterExp 格式：key=value,key=value，使用逗号分隔多个映射</li>
     *     <li>separator 用于分割多个值，如果 propertyValue 包含分隔符，会分别转换每个值</li>
     *     <li>如果 propertyValue 在 converterExp 中找不到对应值，返回空字符串</li>
     *     <li>converterExp 格式错误（缺少 = 号）的项会被跳过</li>
     *     <li>多个值转换时，结果会使用相同的分隔符连接</li>
     * </ul>
     *
     * @param propertyValue 属性值（要转换的值，可以是单个值或分隔符分隔的多个值），不能为 null
     * @param converterExp  转换表达式（格式：key=value,key=value），使用逗号分隔，不能为 null
     * @param separator     分隔符（用于分割多个值时的分隔符），不能为 null
     * @return 转换后的文本值，如果找不到对应值则返回空字符串
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
     * 反向解析值（将文本转换为对应的数字）
     * <p>
     * 根据转换表达式将文本值转换为对应的数字值，用于 Excel 导入时的数据转换。
     * 与 {@link #convertByExp(String, String, String)} 功能相反，用于将用户输入的文本转换回数据库存储的值。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 单个值转换
     * String genderCode = ExcelUtil.reverseByExp("男", "男=0,女=1,未知=2", ",");
     * // 结果：genderCode = "0"
     *
     * // 多个值转换
     * String statusCode = ExcelUtil.reverseByExp("待审核,已审核", "待审核=0,已审核=1,已拒绝=2", ",");
     * // 结果：statusCode = "0,1"
     *
     * // 在导入监听器中使用
     * @Override
     * public void invoke(UserDTO data, AnalysisContext context) {
     *     String gender = data.getGender();
     *     String genderCode = ExcelUtil.reverseByExp(gender, "男=0,女=1,未知=2", ",");
     *     data.setGenderCode(genderCode);
     * }
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>将可读的文本转换回数据库中的枚举值</li>
     *     <li>支持单个值和多个值（使用分隔符分隔）的转换</li>
     *     <li>用于 Excel 导入时的数据转换</li>
     *     <li>用户输入文本到系统内部编码的转换</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>converterExp 格式：value=key,value=key（与 convertByExp 相反）</li>
     *     <li>separator 用于分割多个值，如果 propertyValue 包含分隔符，会分别转换每个值</li>
     *     <li>如果 propertyValue 在 converterExp 中找不到对应值，返回空字符串</li>
     *     <li>converterExp 格式错误（缺少 = 号）的项会被跳过</li>
     *     <li>多个值转换时，结果会使用相同的分隔符连接</li>
     * </ul>
     *
     * @param propertyValue 属性值（要转换的文本，可以是单个值或分隔符分隔的多个值），不能为 null
     * @param converterExp  转换表达式（格式：value=key,value=key），使用逗号分隔，不能为 null
     * @param separator     分隔符（用于分割多个值时的分隔符），不能为 null
     * @return 转换后的数字值，如果找不到对应值则返回空字符串
     * @see #convertByExp(String, String, String)
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
     * <p>
     * 为文件名添加 UUID 前缀，确保文件名唯一，避免下载时文件名冲突。
     * 自动添加 .xlsx 扩展名。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * String filename = ExcelUtil.encodingFilename("用户列表");
     * // 结果：类似 "a1b2c3d4e5f6_用户列表.xlsx"
     *
     * // 在导出时自动调用
     * ExcelUtil.outputExcel(list, "用户列表", UserDTO.class, response);
     * // 实际下载的文件名会是：UUID_用户列表.xlsx
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>为文件名添加唯一标识，避免文件名冲突</li>
     *     <li>确保下载的文件名唯一</li>
     *     <li>自动添加 .xlsx 扩展名</li>
     *     <li>支持多用户同时下载同名文件</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>UUID 使用 fastSimpleUUID() 生成，格式为 32 位十六进制字符串</li>
     *     <li>文件名格式：UUID_原始文件名.xlsx</li>
     *     <li>如果 filename 为 null，会抛出异常</li>
     *     <li>自动添加 .xlsx 扩展名，不需要在 filename 中包含扩展名</li>
     * </ul>
     *
     * @param filename 原始文件名（不包含扩展名），不能为 null
     * @return 编码后的文件名（格式：UUID_文件名.xlsx）
     * @throws NullPointerException 当 filename 为 null 时抛出异常
     */
    public static String encodingFilename(String filename) {
        return IdUtil.fastSimpleUUID() + "_" + filename + ".xlsx";
    }
}