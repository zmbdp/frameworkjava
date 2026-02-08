package com.zmbdp.common.log.config;

import com.zmbdp.common.log.filter.TraceIdFilter;
import com.zmbdp.common.log.interceptor.FeignTraceInterceptor;
import feign.RequestInterceptor;
import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置类
 * <p>
 * 负责配置 Feign 客户端的全局拦截器，实现链路追踪等功能。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>注册 Feign 请求拦截器</li>
 *     <li>实现跨服务的 traceId 传递</li>
 *     <li>支持全链路追踪</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>只有在 Feign 存在时才会生效（通过 @ConditionalOnClass 控制）</li>
 *     <li>所有 Feign 客户端都会应用此配置</li>
 *     <li>拦截器按照注册顺序执行</li>
 *     <li>如果需要添加其他拦截器，在此类中注册</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see FeignTraceInterceptor
 */
@Slf4j
@Configuration
@ConditionalOnClass(RequestInterceptor.class)  // 只有在 Feign 存在时才生效
public class FeignConfig {

    /**
     * 注册 Feign 链路追踪拦截器
     * <p>
     * 该拦截器会在所有 Feign 请求发送前执行，
     * 自动将 MDC 中的 traceId 添加到请求头中。
     *
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor feignTraceInterceptor() {
        log.info("注册 Feign 链路追踪拦截器");
        return new FeignTraceInterceptor();
    }

    @Bean
    public Filter traceIdFilter() {
        return new TraceIdFilter();
    }
}