package com.zmbdp.gateway.handler;

import com.zmbdp.common.core.utils.JsonUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * 网关异常处理器
 *
 * @author 稚名不带撇
 */
@Slf4j
@Order(-1)
@Configuration
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * 响应式返回数据
     *
     * @param response 响应对象
     * @param status   http 状态码
     * @param value    响应内容
     * @param code     响应码
     * @return 无
     */
    private static Mono<Void> webFluxResponseWriter(
            ServerHttpResponse response, HttpStatus status,
            Object value, int code
    ) {
        // MediaType.APPLICATION_JSON_VALUE: 响应内容类型设置为 json
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, value, code);
    }

    /**
     * 响应式返回数据
     *
     * @param response    响应对象
     * @param contentType 响应内容类型
     * @param status      http 状态码
     * @param value       响应内容
     * @param code        响应码
     * @return 无
     */
    private static Mono<Void> webFluxResponseWriter(
            ServerHttpResponse response,
            String contentType, HttpStatus status,
            Object value, int code
    ) {
        response.setStatusCode(status); //设置 http 响应
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType); //设置响应体内容类型为 json
        Result<?> result = Result.fail(code, value.toString()); //按照约定响应数据结构，构建响应体内容
        DataBuffer dataBuffer = response.bufferFactory().wrap(JsonUtil.classToJson(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer)); //将响应体内容写入响应体
    }

    /**
     * 处理器
     *
     * @param exchange ServerWebExchange
     * @param ex       异常的信息
     * @return 无
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 先拿到响应式数据
        ServerHttpResponse response = exchange.getResponse();

        // 判断响应是否已经提交到客户端
        if (response.isCommitted()) {
            // 说明已经提交到客户端，无法再对这个响应进行常规的异常处理修改了，直接返回一个包含原始异常 ex 的 Mono.error(ex)
            return Mono.error(ex);
        }
        // 说明还没返回到前端，就设置相应相对应的格式还有 errMsg
        int retCode = ResultCode.ERROR.getCode();
        String errMsg = ResultCode.ERROR.getErrMsg(); // 兜底
        // 判断是不是未找到服务名称的异常
        if (ex instanceof NoResourceFoundException) {
            retCode = ResultCode.SERVICE_NOT_FOUND.getCode();
            errMsg = ResultCode.SERVICE_NOT_FOUND.getErrMsg();
        } else if (ex instanceof ServiceException) {
            // 如果是自定义异常，直接返回业务异常信息
            retCode = ((ServiceException) ex).getCode();
            errMsg = ex.getMessage();
        }
        // 按照统一状态码的特点，前三位是 http 状态码。从中截取 http 状态码
        int httpCode = Integer.parseInt(String.valueOf(retCode).substring(0, 3));
        log.error("[网关异常处理]请求路径: {},异常信息: {}, 服务未找到", exchange.getRequest().getPath(), ex.getMessage());
        // 拼接响应体内容
        return webFluxResponseWriter(response, HttpStatus.valueOf(httpCode), errMsg, retCode);
    }
}