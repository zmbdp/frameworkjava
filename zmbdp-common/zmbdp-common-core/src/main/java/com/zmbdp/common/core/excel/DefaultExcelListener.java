package com.zmbdp.common.core.excel;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.core.utils.StreamUtil;
import com.zmbdp.common.core.utils.ValidatorUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * Excel 默认导入监听器<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>监听 Excel 导入过程中的数据读取和解析</li>
 *     <li>支持数据校验（基于 Jakarta Validation）</li>
 *     <li>自动收集导入成功的数据和错误信息</li>
 *     <li>处理单元格转换异常和校验异常</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>创建 {@link DefaultExcelListener} 实例，传入是否校验参数</li>
 *     <li>在 {@link com.zmbdp.common.core.utils.ExcelUtil#inputExcel} 方法中使用</li>
 *     <li>通过 {@link #getExcelResult()} 获取导入结果</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * DefaultExcelListener&lt;UserDTO&gt; listener = new DefaultExcelListener&lt;&gt;(true);
 * EasyExcel.read(inputStream, UserDTO.class, listener).sheet().doRead();
 * ExcelResult&lt;UserDTO&gt; result = listener.getExcelResult();
 * </pre>
 *
 * @author 稚名不带撇
 */
@Slf4j
@NoArgsConstructor
public class DefaultExcelListener<T> extends AnalysisEventListener<T> implements ExcelListener<T> {

    /**
     * 是否启用数据校验，默认为 true（启用校验）
     */
    private Boolean isValidate = Boolean.TRUE;

    /**
     * Excel 表头数据映射（列索引 -> 表头名称）
     */
    private Map<Integer, String> headMap;

    /**
     * Excel 导入结果对象（包含成功数据和错误信息）
     */
    private ExcelResult<T> excelResult;

    /**
     * 构造方法
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>创建带校验功能的导入监听器</li>
     *     <li>初始化导入结果对象</li>
     * </ul>
     *
     * @param isValidate 是否启用数据校验，true 表示启用，false 表示不校验
     */
    public DefaultExcelListener(boolean isValidate) {
        this.excelResult = new DefaultExcelResult<>();
        this.isValidate = isValidate;
    }

    /**
     * 处理导入过程中的异常
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>处理单元格数据转换异常（如类型转换错误）</li>
     *     <li>处理数据校验异常（如约束违反）</li>
     *     <li>收集错误信息到结果对象</li>
     *     <li>抛出异常以终止导入流程</li>
     * </ul>
     *
     * @param exception 异常对象
     * @param context   Excel 分析上下文
     * @throws Exception 抛出 ExcelAnalysisException 以终止导入
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        String errMsg = null;
        
        // 处理单元格数据转换异常（如类型转换错误、格式不匹配等）
        if (exception instanceof ExcelDataConvertException excelDataConvertException) {
            // 获取异常发生的行号和列号（从0开始，所以+1显示给用户）
            Integer rowIndex = excelDataConvertException.getRowIndex();
            Integer columnIndex = excelDataConvertException.getColumnIndex();
            // 格式化错误信息：第X行-第Y列-表头Z: 解析异常
            errMsg = StrUtil.format("第{}行-第{}列-表头{}: 解析异常<br/>",
                    rowIndex + 1, columnIndex + 1, headMap.get(columnIndex));
            // 仅在debug模式下记录详细错误日志
            if (log.isDebugEnabled()) {
                log.error(errMsg);
            }
        }
        
        // 处理数据校验异常（Jakarta Validation 校验失败）
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            // 获取所有校验失败的约束违规信息
            Set<ConstraintViolation<?>> constraintViolations = constraintViolationException.getConstraintViolations();
            // 将所有校验错误信息用逗号连接成字符串
            String constraintViolationsMsg = StreamUtil.join(constraintViolations, ConstraintViolation::getMessage, ", ");
            // 格式化错误信息：第X行数据校验异常: 具体错误信息
            errMsg = StrUtil.format("第{}行数据校验异常: {}", context.readRowHolder().getRowIndex() + 1, constraintViolationsMsg);
            // 仅在debug模式下记录详细错误日志
            if (log.isDebugEnabled()) {
                log.error(errMsg);
            }
        }
        
        // 将错误信息添加到结果对象的错误列表中
        excelResult.getErrorList().add(errMsg);
        // 抛出异常以终止导入流程，避免继续处理后续数据
        throw new ExcelAnalysisException(errMsg);
    }

    /**
     * 读取表头数据
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>在 Excel 导入开始时被调用</li>
     *     <li>保存表头信息用于异常提示</li>
     *     <li>记录表头映射关系</li>
     * </ul>
     *
     * @param headMap 表头数据映射（列索引 -> 表头名称）
     * @param context Excel 分析上下文
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        this.headMap = headMap;
        log.debug("解析到一条表头数据: {}", JsonUtil.classToJson(headMap));
    }

    /**
     * 读取每行数据
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>处理 Excel 中的每一行数据</li>
     *     <li>执行数据校验（如果启用）</li>
     *     <li>将校验通过的数据添加到结果列表</li>
     * </ul>
     *
     * @param data    解析后的数据对象
     * @param context Excel 分析上下文
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        if (isValidate) {
            ValidatorUtil.validate(data);
        }
        excelResult.getList().add(data);
    }

    /**
     * 所有数据解析完成
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>在 Excel 导入完成时被调用</li>
     *     <li>执行后续处理逻辑（如果需要）</li>
     * </ul>
     *
     * @param context Excel 分析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.debug("所有数据解析完成！");
    }

    /**
     * 获取 Excel 导入结果
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>获取导入成功的数据列表</li>
     *     <li>获取导入过程中的错误信息列表</li>
     *     <li>在导入完成后调用以获取结果</li>
     * </ul>
     *
     * @return ExcelResult&lt;T&gt; Excel 导入结果对象
     */
    @Override
    public ExcelResult<T> getExcelResult() {
        return excelResult;
    }
}
