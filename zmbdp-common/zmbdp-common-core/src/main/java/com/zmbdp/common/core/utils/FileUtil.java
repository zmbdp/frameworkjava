package com.zmbdp.common.core.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件处理工具类<br>
 *
 * <p>
 * 功能说明：
 * <ul>
 *     <li>扩展 Hutool 的 FileUtil，提供文件下载相关功能</li>
 *     <li>支持文件名的中文编码处理</li>
 *     <li>支持设置 HTTP 响应头用于文件下载</li>
 *     <li>支持跨域文件下载</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * <ol>
 *     <li>在文件下载接口中调用 {@link #setAttachmentResponseHeader} 设置响应头</li>
 *     <li>工具类会自动处理文件名的编码问题</li>
 * </ol>
 *
 * <p>
 * 示例：
 * <pre>
 * FileUtil.setAttachmentResponseHeader(response, "用户列表.xlsx");
 * </pre>
 *
 * <p>
 * 工具类说明：
 * <ul>
 *     <li>不允许实例化</li>
 *     <li>所有方法均为静态方法</li>
 *     <li>继承自 Hutool 的 FileUtil，可直接使用父类的方法</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtil extends cn.hutool.core.io.FileUtil {

    /**
     * 设置文件下载响应头
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>设置 HTTP 响应头用于文件下载</li>
     *     <li>处理中文文件名的编码问题</li>
     *     <li>支持跨域文件下载</li>
     *     <li>设置 Content-Disposition 头信息</li>
     * </ul>
     *
     * @param response     HTTP 响应对象
     * @param realFileName 真实文件名（包含中文）
     * @throws UnsupportedEncodingException 编码异常
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
     *
     * <p>该方法适用于：</p>
     * <ul>
     *     <li>对文件名进行 URL 编码</li>
     *     <li>处理文件名中的特殊字符</li>
     *     <li>将空格编码为 %20 而不是 +</li>
     * </ul>
     *
     * @param s 需要百分号编码的字符串
     * @return String 百分号编码后的字符串
     * @throws UnsupportedEncodingException 编码异常
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8);
        return encode.replaceAll("\\+", "%20");
    }
}
