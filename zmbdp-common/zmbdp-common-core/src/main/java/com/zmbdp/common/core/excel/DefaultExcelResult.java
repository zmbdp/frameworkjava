package com.zmbdp.common.core.excel;

import cn.hutool.core.util.StrUtil;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 默认导入结果实现类<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>实现 {@link ExcelResult} 接口</li>
 *     <li>存储导入成功的数据列表</li>
 *     <li>存储导入过程中的错误信息列表</li>
 *     <li>生成导入回执信息</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>通常由 {@link DefaultExcelListener} 内部创建和使用</li>
 *     <li>通过 {@link ExcelListener#getExcelResult()} 获取实例</li>
 *     <li>调用 {@link #getAnalysis()} 获取导入回执信息</li>
 * </ol>
 *
 * @author 稚名不带撇
 */
public class DefaultExcelResult<T> implements ExcelResult<T> {

    /**
     * 导入成功的数据对象列表
     */
    @Setter
    private List<T> list;

    /**
     * 导入过程中的错误信息列表
     */
    @Setter
    private List<String> errorList;

    /**
     * 无参构造方法
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>创建空的导入结果对象</li>
     *     <li>初始化空的数据列表和错误列表</li>
     * </ul>
     */
    public DefaultExcelResult() {
        this.list = new ArrayList<>();
        this.errorList = new ArrayList<>();
    }

    /**
     * 带参构造方法
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>直接指定数据列表和错误列表创建结果对象</li>
     *     <li>从其他结果对象复制数据</li>
     * </ul>
     *
     * @param list      导入成功的数据对象列表
     * @param errorList 导入过程中的错误信息列表
     */
    public DefaultExcelResult(List<T> list, List<String> errorList) {
        this.list = list;
        this.errorList = errorList;
    }

    /**
     * 复制构造方法
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>从其他 ExcelResult 对象复制数据</li>
     *     <li>创建新的结果对象实例</li>
     * </ul>
     *
     * @param excelResult 其他 ExcelResult 对象
     */
    public DefaultExcelResult(ExcelResult<T> excelResult) {
        this.list = excelResult.getList();
        this.errorList = excelResult.getErrorList();
    }

    /**
     * 获取导入成功的数据列表
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>获取所有校验通过并成功导入的数据</li>
     *     <li>用于后续业务处理</li>
     * </ul>
     *
     * @return List&lt;T&gt; 导入成功的数据对象列表
     */
    @Override
    public List<T> getList() {
        return list;
    }

    /**
     * 获取导入过程中的错误信息列表
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>获取所有导入失败的行的错误信息</li>
     *     <li>用于错误提示和日志记录</li>
     * </ul>
     *
     * @return List&lt;String&gt; 错误信息列表
     */
    @Override
    public List<String> getErrorList() {
        return errorList;
    }

    /**
     * 获取导入回执信息
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>生成用户友好的导入结果提示信息</li>
     *     <li>显示成功导入的数量</li>
     *     <li>在有错误时返回空字符串</li>
     * </ul>
     *
     * @return String 导入回执信息，格式如："恭喜您，全部读取成功！共10条" 或 "读取失败，未解析到数据"
     */
    @Override
    public String getAnalysis() {
        int successCount = list.size();
        int errorCount = errorList.size();
        if (successCount == 0) {
            return "读取失败，未解析到数据";
        } else {
            if (errorCount == 0) {
                return StrUtil.format("恭喜您，全部读取成功！共{}条", successCount);
            } else {
                return "";
            }
        }
    }
}