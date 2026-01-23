package com.zmbdp.common.core.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件处理工具类
 * <p>
 * 扩展 Hutool 的 FileUtil，提供文件下载相关的 HTTP 响应头设置功能。<br>
 * 主要解决中文文件名编码问题，支持跨域文件下载。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>支持文件名的中文编码处理（URL 编码）</li>
 *     <li>支持设置 HTTP 响应头用于文件下载</li>
 *     <li>支持跨域文件下载（设置 Access-Control-Expose-Headers）</li>
 *     <li>兼容不同浏览器的文件名编码格式</li>
 *     <li>继承 Hutool 的 FileUtil，可直接使用父类的文件操作方法</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 在文件下载接口中使用
 * @GetMapping("/download")
 * public void downloadFile(HttpServletResponse response) throws IOException {
 *     // 设置响应头
 *     FileUtil.setAttachmentResponseHeader(response, "用户列表.xlsx");
 *
 *     // 写入文件内容
 *     response.getOutputStream().write(fileBytes);
 * }
 *
 * // 支持中文文件名
 * FileUtil.setAttachmentResponseHeader(response, "测试文件.xlsx");
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>继承自 Hutool 的 FileUtil，可直接使用父类的方法</li>
 *     <li>文件名会自动进行 URL 编码，支持中文和特殊字符</li>
 *     <li>设置响应头后，浏览器会自动触发文件下载</li>
 *     <li>支持跨域场景，设置了 Access-Control-Expose-Headers</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see cn.hutool.core.io.FileUtil
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtil extends cn.hutool.core.io.FileUtil {

    /**
     * 设置文件下载响应头
     * <p>
     * 设置 HTTP 响应头，使浏览器将响应内容作为文件下载，而不是在浏览器中打开。
     * 自动处理中文文件名的编码问题，支持跨域文件下载。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 设置响应头
     * FileUtil.setAttachmentResponseHeader(response, "用户列表.xlsx");
     *
     * // 支持中文文件名
     * FileUtil.setAttachmentResponseHeader(response, "测试文件.xlsx");
     *
     * // 在下载接口中使用
     * @GetMapping("/download")
     * public void download(HttpServletResponse response) throws IOException {
     *     FileUtil.setAttachmentResponseHeader(response, "report.xlsx");
     *     // 写入文件内容...
     * }
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>设置 HTTP 响应头用于文件下载</li>
     *     <li>处理中文文件名的编码问题</li>
     *     <li>支持跨域文件下载</li>
     *     <li>设置 Content-Disposition 头信息</li>
     *     <li>Excel、PDF、图片等文件下载</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>会设置 Content-Disposition 头，格式：attachment; filename=...; filename*=utf-8''...</li>
     *     <li>文件名会自动进行 URL 编码，支持中文和特殊字符</li>
     *     <li>设置 Access-Control-Expose-Headers，支持跨域访问</li>
     *     <li>同时设置 download-filename 自定义头，方便前端获取文件名</li>
     *     <li>如果响应已提交，设置响应头可能无效</li>
     *     <li>realFileName 不能为 null</li>
     * </ul>
     *
     * @param response     HTTP 响应对象，不能为 null
     * @param realFileName 真实文件名（包含中文和扩展名），不能为 null
     * @throws UnsupportedEncodingException 当编码失败时抛出异常
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException {
        String percentEncodedFileName = percentEncode(realFileName);

        String contentDispositionValue = "attachment; filename=" +
                percentEncodedFileName +
                ";" +
                "filename*=" +
                "utf-8''" +
                percentEncodedFileName;

        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
        response.setHeader("Content-disposition", contentDispositionValue);
        response.setHeader("download-filename", percentEncodedFileName);
    }

    /**
     * 百分号编码工具方法（URL 编码，将 + 替换为 %20）
     * <p>
     * 对字符串进行 URL 编码，并将编码结果中的 + 号替换为 %20。
     * 主要用于文件名编码，确保空格被正确编码为 %20 而不是 +。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 编码文件名
     * String encoded = FileUtil.percentEncode("测试 文件.xlsx");
     * // 结果：类似 "测试%20文件.xlsx"
     *
     * // 编码包含特殊字符的文件名
     * String encoded2 = FileUtil.percentEncode("report(2024).xlsx");
     * }</pre>
     * <p>
     * <b>适用场景：</b>
     * <ul>
     *     <li>对文件名进行 URL 编码</li>
     *     <li>处理文件名中的特殊字符</li>
     *     <li>将空格编码为 %20 而不是 +（符合 RFC 3986 标准）</li>
     *     <li>HTTP 响应头中的文件名编码</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTF-8 编码进行 URL 编码</li>
     *     <li>会将 URLEncoder.encode 结果中的 + 替换为 %20</li>
     *     <li>这样做的原因是：+ 在 URL 中可能被解释为空格，但 %20 更明确</li>
     *     <li>如果 s 为 null，会抛出异常</li>
     * </ul>
     *
     * @param s 需要百分号编码的字符串，不能为 null
     * @return 百分号编码后的字符串
     * @throws UnsupportedEncodingException 当编码失败时抛出异常（实际上不会发生，因为使用 UTF-8）
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8);
        return encode.replaceAll("\\+", "%20");
    }
}