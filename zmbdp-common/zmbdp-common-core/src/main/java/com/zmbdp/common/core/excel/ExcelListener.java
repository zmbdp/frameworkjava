package com.zmbdp.common.core.excel;

import com.alibaba.excel.read.listener.ReadListener;

/**
 * Excel 导入监听器接口<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>扩展 EasyExcel 的 {@link ReadListener} 接口</li>
 *     <li>提供获取导入结果的方法</li>
 *     <li>用于自定义 Excel 导入处理逻辑</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>实现该接口创建自定义监听器</li>
 *     <li>或使用 {@link DefaultExcelListener} 默认实现</li>
 *     <li>在 {@link com.zmbdp.common.core.utils.ExcelUtil#importExcel} 方法中使用</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * public class CustomExcelListener&lt;T&gt; implements ExcelListener&lt;T&gt; {
 *     // 实现接口方法
 * }
 * </pre>
 *
 * @author 稚名不带撇
 */
public interface ExcelListener<T> extends ReadListener<T> {

    /**
     * 获取 Excel 导入结果
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>在 Excel 导入完成后获取结果</li>
     *     <li>获取导入成功的数据和错误信息</li>
     * </ul>
     *
     * @return ExcelResult&lt;T&gt; Excel 导入结果对象
     */
    ExcelResult<T> getExcelResult();
}
