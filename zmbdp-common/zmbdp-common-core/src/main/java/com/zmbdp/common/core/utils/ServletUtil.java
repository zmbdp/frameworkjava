package com.zmbdp.common.core.utils;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;

/**
 * Servlet 工具类
 * <p>
 * 提供 Servlet 和 WebFlux 相关的工具方法，包括请求对象获取、URL 编码、响应写入等功能。
 * 支持传统 Servlet 和响应式 WebFlux 两种场景。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li>获取当前 HTTP 请求对象（从 RequestContextHolder）</li>
 *     <li>获取请求属性信息</li>
 *     <li>URL 编码处理</li>
 *     <li>WebFlux 响应写入（支持统一响应格式）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 获取当前请求
 * HttpServletRequest request = ServletUtil.getRequest();
 * String userAgent = request.getHeader("User-Agent");
 *
 * // URL 编码
 * String encoded = ServletUtil.urlEncode("测试内容");
 *
 * // WebFlux 响应写入
 * return ServletUtil.webFluxResponseWriter(response, "错误信息", 500);
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有方法均为静态方法，不允许实例化</li>
 *     <li>getRequest() 依赖于 RequestContextHolder，必须在 Spring MVC 请求上下文中调用</li>
 *     <li>WebFlux 方法返回 Mono，需要在响应式链中使用</li>
 *     <li>如果不在请求上下文中，getRequest() 可能返回 null</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 生成无参私有的构造方法，避免外部通过 new 创建对象
public class ServletUtil {

    /**
     * URL 编码
     * <p>
     * 对字符串进行 URL 编码，使用 UTF-8 编码格式。
     * 如果编码失败，返回空字符串。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 编码中文字符
     * String encoded = ServletUtil.urlEncode("测试内容");
     * // 结果：类似 "%E6%B5%8B%E8%AF%95%E5%86%85%E5%AE%B9"
     *
     * // 编码特殊字符
     * String encoded2 = ServletUtil.urlEncode("hello world");
     * // 结果：类似 "hello+world" 或 "hello%20world"
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>使用 UTF-8 编码格式</li>
     *     <li>如果编码失败（理论上不会发生），返回空字符串</li>
     *     <li>如果 str 为 null，会抛出 NullPointerException</li>
     *     <li>空格可能被编码为 + 或 %20，取决于 URLEncoder 的实现</li>
     * </ul>
     *
     * @param str 需要编码的内容，不能为 null
     * @return 编码后的内容，如果编码失败则返回空字符串
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, CommonConstants.UTF8);
        } catch (UnsupportedEncodingException e) {
            return StringUtil.EMPTY;
        }
    }

    /**
     * 设置 WebFlux 响应（使用默认 HTTP 200 状态码）
     * <p>
     * 将响应内容写入 WebFlux 响应流，使用统一的 Result 格式包装。
     * 适用于 WebFlux 响应式编程场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在 WebFlux Controller 中使用
     * @GetMapping("/error")
     * public Mono<Void> handleError(ServerHttpResponse response) {
     *     return ServletUtil.webFluxResponseWriter(response, "操作失败", 500);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>HTTP 状态码固定为 200（OK）</li>
     *     <li>响应内容会被包装为 Result.fail(code, value.toString()) 格式</li>
     *     <li>Content-Type 设置为 application/json</li>
     *     <li>返回 Mono&lt;Void&gt;，需要在响应式链中使用</li>
     *     <li>如果 value 为 null，会转换为字符串 "null"</li>
     * </ul>
     *
     * @param response ServerHttpResponse 响应对象，不能为 null
     * @param value    响应内容（会被转换为字符串），可以为 null
     * @param code     业务状态码（用于 Result 对象）
     * @return Mono&lt;Void&gt; 响应式流，表示写入完成
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value, int code) {
        return webFluxResponseWriter(response, HttpStatus.OK, value, code);
    }

    /**
     * 设置 WebFlux 响应（指定 HTTP 状态码）
     * <p>
     * 将响应内容写入 WebFlux 响应流，使用统一的 Result 格式包装，可指定 HTTP 状态码。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用自定义 HTTP 状态码
     * return ServletUtil.webFluxResponseWriter(response, HttpStatus.BAD_REQUEST, "参数错误", 400);
     *
     * // 使用 500 错误
     * return ServletUtil.webFluxResponseWriter(response, HttpStatus.INTERNAL_SERVER_ERROR, "服务器错误", 500);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>可以自定义 HTTP 状态码（如 400、500 等）</li>
     *     <li>响应内容会被包装为 Result.fail(code, value.toString()) 格式</li>
     *     <li>Content-Type 设置为 application/json</li>
     *     <li>其他注意事项同 {@link #webFluxResponseWriter(ServerHttpResponse, Object, int)}</li>
     * </ul>
     *
     * @param response ServerHttpResponse 响应对象，不能为 null
     * @param status   HTTP 状态码（如 HttpStatus.BAD_REQUEST），不能为 null
     * @param value    响应内容（会被转换为字符串），可以为 null
     * @param code     业务状态码（用于 Result 对象）
     * @return Mono&lt;Void&gt; 响应式流，表示写入完成
     * @see #webFluxResponseWriter(ServerHttpResponse, Object, int)
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, Object value, int code) {
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, value, code);
    }

    /**
     * 设置 WebFlux 响应（完整参数）
     * <p>
     * 将响应内容写入 WebFlux 响应流，使用统一的 Result 格式包装，可指定 HTTP 状态码和 Content-Type。
     * 这是最灵活的版本，支持自定义所有响应参数。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 自定义 Content-Type
     * return ServletUtil.webFluxResponseWriter(response,
     *     "application/xml", HttpStatus.OK, "错误信息", 500);
     *
     * // 返回 XML 格式
     * return ServletUtil.webFluxResponseWriter(response,
     *     MediaType.APPLICATION_XML_VALUE, HttpStatus.BAD_REQUEST, "参数错误", 400);
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>可以自定义 Content-Type（如 application/json、application/xml 等）</li>
     *     <li>可以自定义 HTTP 状态码</li>
     *     <li>响应内容会被包装为 Result.fail(code, value.toString()) 格式</li>
     *     <li>其他注意事项同 {@link #webFluxResponseWriter(ServerHttpResponse, HttpStatus, Object, int)}</li>
     * </ul>
     *
     * @param response    ServerHttpResponse 响应对象，不能为 null
     * @param contentType Content-Type 值（如 "application/json"），不能为 null
     * @param status      HTTP 状态码（如 HttpStatus.BAD_REQUEST），不能为 null
     * @param value       响应内容（会被转换为字符串），可以为 null
     * @param code        业务状态码（用于 Result 对象）
     * @return Mono&lt;Void&gt; 响应式流，表示写入完成
     * @see #webFluxResponseWriter(ServerHttpResponse, HttpStatus, Object, int)
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String contentType, HttpStatus status, Object value, int code) {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        Result<?> result = Result.fail(code, value.toString());
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.classToJson(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }

    /**
     * 获取当前 HTTP 请求对象
     * <p>
     * 从 Spring 的 RequestContextHolder 中获取当前请求的 HttpServletRequest 对象。
     * 适用于需要在非 Controller 层获取请求信息的场景。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在 Service 层获取请求信息
     * HttpServletRequest request = ServletUtil.getRequest();
     * if (request != null) {
     *     String userAgent = request.getHeader("User-Agent");
     *     String ip = request.getRemoteAddr();
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 Spring MVC 请求上下文中调用，否则可能返回 null</li>
     *     <li>在异步线程中可能无法获取到请求对象</li>
     *     <li>在 WebFlux 响应式环境中无法使用（需要使用 ServerWebExchange）</li>
     *     <li>如果获取失败，返回 null（不会抛出异常）</li>
     * </ul>
     *
     * @return 当前请求的 HttpServletRequest 对象，如果获取失败则返回 null
     * @see #getRequestAttributes()
     */
    public static HttpServletRequest getRequest() {
        try {
            return Objects.requireNonNull(getRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前请求的属性信息
     * <p>
     * 从 Spring 的 RequestContextHolder 中获取当前请求的 ServletRequestAttributes 对象。
     * 可以用于获取请求对象、设置请求属性等操作。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 获取请求属性
     * ServletRequestAttributes attributes = ServletUtil.getRequestAttributes();
     * if (attributes != null) {
     *     HttpServletRequest request = attributes.getRequest();
     *     // 设置请求属性
     *     attributes.setAttribute("key", "value", RequestAttributes.SCOPE_REQUEST);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 Spring MVC 请求上下文中调用，否则可能返回 null</li>
     *     <li>在异步线程中可能无法获取到请求属性</li>
     *     <li>在 WebFlux 响应式环境中无法使用</li>
     *     <li>如果获取失败，返回 null（不会抛出异常）</li>
     * </ul>
     *
     * @return 当前请求的 ServletRequestAttributes 对象，如果获取失败则返回 null
     * @see #getRequest()
     */
    public static ServletRequestAttributes getRequestAttributes() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            return (ServletRequestAttributes) attributes;
        } catch (Exception e) {
            return null;
        }
    }
}