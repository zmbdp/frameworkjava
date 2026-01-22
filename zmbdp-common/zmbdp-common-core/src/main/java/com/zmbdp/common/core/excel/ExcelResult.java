package com.zmbdp.common.core.excel;

import java.util.List;

/**
 * Excel 导入结果接口<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>定义 Excel 导入结果的通用接口</li>
 *     <li>包含成功数据列表和错误信息列表</li>
 *     <li>提供导入回执信息生成方法</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>通过 {@link ExcelListener#getExcelResult()} 获取实现类实例</li>
 *     <li>调用 {@link #getList()} 获取成功导入的数据</li>
 *     <li>调用 {@link #getErrorList()} 获取错误信息</li>
 *     <li>调用 {@link #getAnalysis()} 获取导入回执</li>
 * </ol>
 *
 * @author 稚名不带撇
 */
public interface ExcelResult<T> {

    /**
     * 获取导入成功的数据对象列表
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>获取所有校验通过并成功导入的数据</li>
     *     <li>用于后续业务处理，如批量保存到数据库</li>
     * </ul>
     *
     * @return List&lt;T&gt; 导入成功的数据对象列表
     */
    List<T> getList();

    /**
     * 获取导入过程中的错误信息列表
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>获取所有导入失败的行的错误信息</li>
     *     <li>用于错误提示和日志记录</li>
     *     <li>向用户展示具体的错误位置和原因</li>
     * </ul>
     *
     * @return List&lt;String&gt; 错误信息列表，每个元素对应一行错误信息
     */
    List<String> getErrorList();

    /**
     * 获取导入回执信息
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>生成用户友好的导入结果提示信息</li>
     *     <li>显示成功导入的数量</li>
     *     <li>用于前端展示导入结果</li>
     * </ul>
     *
     * @return String 导入回执信息，格式如："恭喜您，全部读取成功！共10条" 或 "读取失败，未解析到数据"
     */
    String getAnalysis();
}