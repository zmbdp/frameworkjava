package com.zmbdp.common.security.service;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.domain.constants.CacheConstants;
import com.zmbdp.common.domain.constants.SecurityConstants;
import com.zmbdp.common.domain.constants.TokenConstants;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.common.security.utils.JwtUtil;
import com.zmbdp.common.security.utils.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * token 服务
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class TokenService {

    /**
     * 代表 1 毫秒
     */
    private final static long MILLIS_SECOND = 1000;

    /**
     * 代表 1 分钟
     */
    private final static long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * 120分钟
     */
    private final static Long MILLIS_MINUTE_TEN = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /**
     * 过期时间 默认数量 720
     */
    private final static Long EXPIRE_TIME = CacheConstants.EXPIRATION;

    /**
     * token 的 KEY 前缀
     */
    private final static String ACCESS_TOKEN = TokenConstants.LOGIN_TOKEN_KEY;

    /**
     * redis服务
     */
    @Autowired
    private RedisService redisService;

    /**
     * 创建 token
     *
     * @param loginUserDTO 登录用户信息
     * @param secret       密钥
     * @return token
     */
    public TokenDTO createToken(LoginUserDTO loginUserDTO, String secret) {
        // 给用户创建一个 uuid 作为唯一标识
        String token = UUID.randomUUID().toString();
        loginUserDTO.setToken(token); // 设置用户的唯一标识
        // 设置到缓存中
        refreshToken(loginUserDTO);
        // 生成原始数据的声明
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.USER_ID, loginUserDTO.getUserId());
        claimsMap.put(SecurityConstants.USERNAME, loginUserDTO.getUserName());
        claimsMap.put(SecurityConstants.USER_FROM, loginUserDTO.getUserFrom());
        // 创建令牌返回
        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(JwtUtil.createToken(claimsMap, secret));
        tokenDTO.setExpires(EXPIRE_TIME);
        return tokenDTO;
    }

    /**
     * 根据令牌获取登陆用户的信息
     *
     * @param token  令牌
     * @param secret 密钥
     * @return 登陆用户信息
     */
    public LoginUserDTO getLoginUser(String token, String secret) {
        // 创建一个空的用户对象
        LoginUserDTO user = null;
        // 然后解析 token
        try {
            if (StringUtils.isNotEmpty(token)) {
                // 先从 jwt 中拿到 用户的 的 key
                String userKey = JwtUtil.getUserKey(token, secret);
                // 然后再拼接成 redis 的 key 查询出 bloom 对象, 看看 redis 能不能查询到
                user = redisService.getCacheObject(getTokenKey(userKey), LoginUserDTO.class);
            }
        } catch (Exception e) {
            log.warn("TokenService.getLoginUser get loginUser warn: [获取用户信息异常: {} ]", e.getMessage());
            throw new RuntimeException("获取用户信息异常");
        }
        return user;
    }

    /**
     * 根据请求来获取用户信息
     *
     * @param request 请求
     * @param secret  密钥
     * @return 用户信息
     */
    public LoginUserDTO getLoginUser(HttpServletRequest request, String secret) {
        String token = SecurityUtil.getToken(request);
        return getLoginUser(token, secret);
    }

    /**
     * 只需密钥就能获取用户信息
     *
     * @param secret 密钥
     * @return 用户信息
     */
    public LoginUserDTO getLoginUser(String secret) {
        return getLoginUser(ServletUtil.getRequest(), secret);
    }

    /**
     * 根据令牌删除用户登录态
     *
     * @param token  令牌
     * @param secret 密钥
     */
    public void delLoginUser(String token, String secret) {
        if (StringUtils.isNotEmpty(token)) {
            String useKey = JwtUtil.getUserKey(token, secret);
            redisService.deleteObject(getTokenKey(useKey));
        }
    }

    /**
     * 允许超管删除别人的登录状态
     *
     * @param userId   用户 ID
     * @param userFrom 用户来源
     */
    public void delLoginUser(Long userId, String userFrom) {
        if (userId == null) {
            return;
        }
        // 先把 redis 里面的 logintoken: 前缀的 key 全部拿出来
        Collection<String> tokenKeys = redisService.keys(ACCESS_TOKEN + "*");
        for (String tokenKey : tokenKeys) {
            // 找到这个用户对象
            LoginUserDTO user = redisService.getCacheObject(tokenKey, LoginUserDTO.class);
            // id 要一样, 来源也要一样才能删除
            if (user != null && user.getUserId().equals(userId) && user.getUserFrom().equals(userFrom)) {
                // 找到的话就直接删除
                redisService.deleteObject(tokenKey);
            }
        }
    }

    /**
     * 令牌续期, 不到 120 分钟就续期
     *
     * @param loginUserDTO 用户信息
     */
    public void verifyToken(LoginUserDTO loginUserDTO) {
        // 原先设定好的过期的时间
        long expireTime = loginUserDTO.getExpireTime();
        // 现在的时间
        long currentTime = System.currentTimeMillis();
        // 如果说设定好的时间减去现在的时间在 120 分钟之内的话就续期
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            // 刷新缓存
            refreshToken(loginUserDTO);
        }
    }

    /**
     * 设置用户身份信息，允许登录
     *
     * @param loginUserDTO 用户信息
     */
    public void setLoginUser(LoginUserDTO loginUserDTO) {
        if (loginUserDTO != null && StringUtils.isNotEmpty(loginUserDTO.getToken())) {
            refreshToken(loginUserDTO);
        }
    }

    /**
     * 缓存用户信息, 设置令牌有效期
     *
     * @param loginUserDTO 用户信息
     */
    private void refreshToken(LoginUserDTO loginUserDTO) {
        loginUserDTO.setLoginTime(System.currentTimeMillis());
        // 表示设置用户的过期时间是，用户当前登陆的时间加上 720 * 1 分钟的时间，就是 720 分钟
        loginUserDTO.setExpireTime(loginUserDTO.getLoginTime() + EXPIRE_TIME * MILLIS_MINUTE);
        // 根据随机产生用户标识生成 redis 的 key
        String userKey = getTokenKey(loginUserDTO.getToken());
        // 生成 loginUserDTO 缓存, 720 单位分钟
        redisService.setCacheObject(userKey, loginUserDTO, EXPIRE_TIME, TimeUnit.MINUTES);
    }

    /**
     * 获取 token key 的信息
     *
     * @param token token
     * @return redis 中的 tokenKey
     */
    private String getTokenKey(String token) {
        return ACCESS_TOKEN + token;
    }
}
