package com.zmbdp.gateway.filter;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.HttpConstants;
import com.zmbdp.common.domain.constants.SecurityConstants;
import com.zmbdp.common.domain.constants.TokenConstants;
import com.zmbdp.common.domain.constants.UserConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.common.security.utils.JwtUtil;
import com.zmbdp.gateway.config.IgnoreWhiteProperties;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关鉴权
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
@RefreshScope
public class AuthFilter implements GlobalFilter, Ordered {

    /**
     * 白名单配置类
     */
    @Autowired
    private IgnoreWhiteProperties ignoreWhiteProperties;

    /**
     * 缓存服务类
     */
    @Autowired
    private RedisService redisService;

    /**
     * jwt 密钥
     */
    @Value("${jwt.token.secret}")
    private String secret;


    /**
     * 鉴权
     *
     * @param exchange 服务器 Web 交换
     * @param chain    网关过滤器链
     * @return 服务器 Web 响应
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求
        ServerHttpRequest request = exchange.getRequest();
        // 获取请求路径，看看是否在白名单中
        String url = request.getURI().getPath();
        // 看看是否在白名单中
        try {
            if (StringUtil.matches(url, ignoreWhiteProperties.getWhites())) {
                // 在白名单中，放行走下一个过滤器
                return chain.filter(exchange);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 拿到令牌
        String token = getToken(request);
        if (StringUtils.isEmpty(token)) {
            // 如果为空就拼接 webFlux 响应体内容
            return unauthorizedResponse(exchange, ResultCode.TOKEN_EMPTY);
        }
        // 判断是否过期，先从令牌中拿到 redis 的 key
        Claims claims = JwtUtil.parseToken(token, secret);
        if (claims == null) {
            return unauthorizedResponse(exchange, ResultCode.TOKEN_INVALID);
        }
        String userKey = JwtUtil.getUserKey(claims);
//        String userKey = JwtUtil.getUserKey(token, secret);
        if (!redisService.hasKey(getTokenKey(userKey))) {
            // 拿到令牌，但是用户信息为空，说明令牌已过期
            return unauthorizedResponse(exchange, ResultCode.LOGIN_STATUS_OVERTIME);
        }
        // 再判断用户来源是否合法
        String userFrom = JwtUtil.getUserFrom(claims);
        if (
                url.contains(HttpConstants.SYS_USER_PATH) && // 如果路径是系统路径
                        !UserConstants.USER_FROM_TU_B.equals(userFrom) // 但是用户不是系统来源的话（相当于 C端用户在 C端拿到的 jwt 想在 B端使用）
        ) {
            return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
        }
        // 根据令牌获取用户信息给下面的 controller 层
        String userId = JwtUtil.getUserId(claims);
        String userName = JwtUtil.getUserName(claims);
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(userName)) {
            return unauthorizedResponse(exchange, ResultCode.TOKEN_CHECK_FAILED);
        }
        // 设置用户状态到 header 请求中取
        ServerHttpRequest.Builder mutate = request.mutate();
        addHeader(mutate, SecurityConstants.USER_KEY, userKey);
        addHeader(mutate, SecurityConstants.USER_ID, userId);
        addHeader(mutate, SecurityConstants.USERNAME, userName);
        addHeader(mutate, SecurityConstants.USER_FROM, userFrom);
        return chain.filter(exchange);
    }

    /**
     * 根据 http 请求获取 token
     *
     * @param request 请求
     * @return token令牌
     */
    private String getToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHENTICATION);
        // 裁剪令牌前缀
        if (StringUtils.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
            token = token.replaceFirst(TokenConstants.PREFIX, "");
        }
        return token;
    }

    /**
     * 未授权返回
     *
     * @param exchange   ServerWebExchange
     * @param resultCode 结果码
     * @return 无
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, ResultCode resultCode) {
        log.error("AuthFilter.unauthorizedResponse err [鉴权处理异常] 请求路径: {}", exchange.getRequest().getPath());
        int retCode = Integer.parseInt(String.valueOf(resultCode.getCode()).substring(0, 3));
        return ServletUtil.webFluxResponseWriter(exchange.getResponse(), HttpStatus.valueOf(retCode), resultCode.getErrMsg(), resultCode.getCode());
    }

    /**
     * 获取缓存 key
     *
     * @param token token 信息
     * @return redis 中的 key
     */
    private String getTokenKey(String token) {
        return TokenConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 添加 header
     *
     * @param mutate 请求
     * @param name   key
     * @param value  值
     */
    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value) {
        if (value == null) {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = ServletUtil.urlEncode(valueStr);
        mutate.header(name, valueEncode);
    }

    /**
     * 获取过滤器的执行顺序
     *
     * @return 过滤器的执行顺序
     */
    @Override
    public int getOrder() {
        return -200;
    }
}
