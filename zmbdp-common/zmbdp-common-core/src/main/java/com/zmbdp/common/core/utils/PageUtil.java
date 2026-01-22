package com.zmbdp.common.core.utils;

/**
 * 分页工具类
 * <p>
 * 提供分页相关的计算方法，主要用于计算总页数。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>根据总记录数和每页大小计算总页数</li>
 *     <li>自动处理不能整除的情况（向上取整）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 计算总页数
 * int totalRecords = 100;
 * int pageSize = 10;
 * int totalPages = PageUtil.getTotalPages(totalRecords, pageSize);
 * // 结果：totalPages = 10
 *
 * // 不能整除的情况
 * int totalRecords2 = 101;
 * int pageSize2 = 10;
 * int totalPages2 = PageUtil.getTotalPages(totalRecords2, pageSize2);
 * // 结果：totalPages2 = 11（向上取整）
 *
 * // 在分页查询中使用
 * PageResult<UserDTO> pageResult = new PageResult<>();
 * pageResult.setTotal(totalRecords);
 * pageResult.setTotalPages(PageUtil.getTotalPages(totalRecords, pageSize));
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法</li>
 *     <li>如果 totals 为 0，返回 0</li>
 *     <li>如果 pageSize 为 0，会抛出除零异常</li>
 *     <li>不能整除时，会自动向上取整（加 1）</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
public class PageUtil {

    /**
     * 根据总记录数和每页大小计算总页数
     * <p>
     * 计算分页查询的总页数，如果总记录数不能被每页大小整除，会自动向上取整。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 能整除的情况
     * int totalPages = PageUtil.getTotalPages(100, 10); // 返回 10
     *
     * // 不能整除的情况（向上取整）
     * int totalPages2 = PageUtil.getTotalPages(101, 10); // 返回 11
     * int totalPages3 = PageUtil.getTotalPages(99, 10);  // 返回 10
     *
     * // 边界情况
     * int totalPages4 = PageUtil.getTotalPages(0, 10);   // 返回 0
     * int totalPages5 = PageUtil.getTotalPages(1, 10);   // 返回 1
     * }</pre>
     * <p>
     * <b>计算公式：</b>
     * <ul>
     *     <li>如果 totals % pageSize == 0，返回 totals / pageSize</li>
     *     <li>否则返回 totals / pageSize + 1（向上取整）</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 totals 为 0，返回 0</li>
     *     <li>如果 pageSize 为 0，会抛出 ArithmeticException（除零异常）</li>
     *     <li>不能整除时，会自动向上取整（加 1）</li>
     *     <li>负数的情况未做处理，建议传入非负数</li>
     * </ul>
     *
     * @param totals   总记录数，建议为非负数
     * @param pageSize 每页大小（分页大小），必须大于 0
     * @return 总页数，如果 totals 为 0 则返回 0
     * @throws ArithmeticException 当 pageSize 为 0 时抛出除零异常
     */
    public static int getTotalPages(int totals, int pageSize) {
        return totals % pageSize == 0 ? totals / pageSize : (totals / pageSize + 1);
    }
}