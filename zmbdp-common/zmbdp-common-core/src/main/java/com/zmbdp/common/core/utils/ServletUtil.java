package com.zmbdp.common.core.utils;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.domain.domain.Result;
import jakarta.servlet.http.HttpServletRequest;
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
 *
 * @author 稚名不带撇
 */
public class ServletUtil {

    /**
     * 内容编码
     *
     * @param str 内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, CommonConstants.UTF8);
        } catch (UnsupportedEncodingException e) {
            return StringUtil.EMPTY;
        }
    }

    /**
     * 设置 webflux 模型响应
     *
     * @param response ServerHttpResponse
     * @param code     响应状态码
     * @param value    响应内容
     * @return 无
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value, int code) {
        return webFluxResponseWriter(response, HttpStatus.OK, value, code);
    }

    /**
     * 设置 webflux 模型响应
     *
     * @param response ServerHttpResponse
     * @param status   http 状态码
     * @param code     响应状态码
     * @param value    响应内容
     * @return 无
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, Object value, int code) {
        //修改了
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, value, code);
    }

    /**
     * 设置 webflux 模型响应
     *
     * @param response    ServerHttpResponse
     * @param contentType content-type
     * @param status      http 状态码
     * @param code        响应状态码
     * @param value       响应内容
     * @return 无
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String contentType, HttpStatus status, Object value, int code) {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        Result<?> result = Result.fail(code, value.toString());
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.classToJson(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }

    /**
     * 获取 request
     *
     * @return request 对象
     */
    public static HttpServletRequest getRequest() {
        try {
            return Objects.requireNonNull(getRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 request 属性信息
     *
     * @return request 属性
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
