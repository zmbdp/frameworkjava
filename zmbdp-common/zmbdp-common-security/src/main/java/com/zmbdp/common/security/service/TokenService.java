package com.zmbdp.common.security.service;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务类
 * <p>
 * 提供 Token 的创建、验证、刷新、删除等功能，用于管理用户登录状态。
 * 基于 JWT（JSON Web Token）和 Redis 实现，支持 Token 的自动续期机制。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *     <li><b>Token 创建</b>：为用户创建唯一的 Token，并存储用户信息到 Redis</li>
 *     <li><b>Token 验证</b>：验证 Token 有效性并获取用户信息</li>
 *     <li><b>Token 续期</b>：当 Token 剩余时间少于 120 分钟时自动续期</li>
 *     <li><b>Token 删除</b>：支持单个用户和批量删除用户登录状态</li>
 *     <li><b>自动过期</b>：Token 默认有效期为 720 分钟（12 小时）</li>
 * </ul>
 * <p>
 * <b>Token 结构：</b>
 * <ul>
 *     <li>JWT Token：包含用户标识、用户ID、用户名、用户来源等信息</li>
 *     <li>Redis Key：{@code login_tokens:uuid}，存储完整的用户信息（{@link LoginUserDTO}）</li>
 *     <li>UUID：每个用户登录时生成唯一的 UUID 作为用户标识</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * // 1. 创建 Token（用户登录时）
 * @Autowired
 * private TokenService tokenService;
 *
 * public TokenDTO login(LoginUserDTO loginUser) {
 *     // 创建 Token，返回 JWT Token 和过期时间
 *     TokenDTO tokenDTO = tokenService.createToken(loginUser, "your-secret-key");
 *     // tokenDTO.getAccessToken() - JWT Token
 *     // tokenDTO.getExpires() - 过期时间（分钟）
 *     return tokenDTO;
 * }
 *
 * // 2. 获取用户信息（验证 Token）
 * LoginUserDTO user = tokenService.getLoginUser("jwt-token-string", "your-secret-key");
 * if (user != null) {
 *     // 用户已登录，可以使用用户信息
 *     Long userId = user.getUserId();
 *     String username = user.getUserName();
 * }
 *
 * // 3. Token 续期（在拦截器中调用）
 * tokenService.verifyToken(loginUser);
 * // 如果 Token 剩余时间少于 120 分钟，会自动续期到 720 分钟
 *
 * // 4. 删除 Token（用户登出时）
 * tokenService.delLoginUser("jwt-token-string", "your-secret-key");
 *
 * // 5. 删除指定用户的登录状态（管理员操作）
 * tokenService.delLoginUser(userId, "admin");
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>Token 默认有效期为 720 分钟（12 小时）</li>
 *     <li>当 Token 剩余时间少于 120 分钟时，会自动续期到 720 分钟</li>
 *     <li>Token 存储在 Redis 中，Key 格式：{@code login_tokens:uuid}</li>
 *     <li>每个用户登录时会生成唯一的 UUID 作为用户标识</li>
 *     <li>JWT Token 中包含用户基本信息，但不包含敏感信息</li>
 *     <li>删除用户登录状态时，需要同时匹配用户ID和用户来源</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see com.zmbdp.common.security.domain.dto.LoginUserDTO
 * @see com.zmbdp.common.security.domain.dto.TokenDTO
 * @see com.zmbdp.common.security.utils.JwtUtil
 */
@Slf4j
@Component
public class TokenService {

    /**
     * 1 毫秒的毫秒数
     * <p>
     * 用于时间单位转换，1 秒 = 1000 毫秒。
     */
    private final static long MILLIS_SECOND = 1000;

    /**
     * 1 分钟的毫秒数
     * <p>
     * 用于时间单位转换，1 分钟 = 60 秒 = 60000 毫秒。
     */
    private final static long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * Token 续期阈值（120 分钟）
     * <p>
     * 当 Token 剩余时间少于 120 分钟时，会自动续期到 720 分钟。
     * 计算公式：{@code CacheConstants.REFRESH_TIME * MILLIS_MINUTE}
     */
    private final static Long MILLIS_MINUTE_TEN = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /**
     * Token 过期时间（默认 720 分钟）
     * <p>
     * Token 在 Redis 中的过期时间，默认值为 720 分钟（12 小时）。
     * 从 {@link com.zmbdp.common.domain.constants.CacheConstants#EXPIRATION} 获取。
     */
    private final static Long EXPIRE_TIME = CacheConstants.EXPIRATION;

    /**
     * Token 在 Redis 中的 Key 前缀
     * <p>
     * 完整的 Key 格式：{@code login_tokens:uuid}，其中 uuid 是用户登录时生成的唯一标识。
     * 从 {@link com.zmbdp.common.domain.constants.TokenConstants#LOGIN_TOKEN_KEY} 获取。
     */
    private final static String ACCESS_TOKEN = TokenConstants.LOGIN_TOKEN_KEY;

    /**
     * Redis 服务
     * <p>
     * 用于存储和获取用户登录信息，Token 存储在 Redis 中。
     */
    @Autowired
    private RedisService redisService;

    /**
     * 创建 Token
     * <p>
     * 为用户创建登录 Token，包括：
     * <ol>
     *     <li>生成唯一的 UUID 作为用户标识</li>
     *     <li>将用户信息存储到 Redis（Key：{@code login_tokens:uuid}）</li>
     *     <li>生成 JWT Token（包含用户标识、用户ID、用户名、用户来源等信息）</li>
     *     <li>返回 Token 和过期时间</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 用户登录时创建 Token
     * LoginUserDTO loginUser = new LoginUserDTO();
     * loginUser.setUserId(123L);
     * loginUser.setUserName("admin");
     * loginUser.setUserFrom("admin");
     *
     * TokenDTO tokenDTO = tokenService.createToken(loginUser, "your-secret-key");
     * // tokenDTO.getAccessToken() - JWT Token 字符串
     * // tokenDTO.getExpires() - 过期时间（720 分钟）
     *
     * // 返回给客户端
     * return Result.success(tokenDTO);
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>生成 UUID 作为用户唯一标识</li>
     *     <li>将 UUID 设置到 {@code loginUserDTO.token}</li>
     *     <li>调用 {@link #refreshToken(LoginUserDTO)} 将用户信息存储到 Redis</li>
     *     <li>构建 JWT Claims（包含用户标识、用户ID、用户名、用户来源）</li>
     *     <li>使用密钥生成 JWT Token</li>
     *     <li>返回 TokenDTO（包含 JWT Token 和过期时间）</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>每个用户登录时会生成唯一的 UUID，确保不同登录会话的 Token 不冲突</li>
     *     <li>用户信息存储在 Redis 中，Key 格式：{@code login_tokens:uuid}</li>
     *     <li>JWT Token 中包含用户基本信息，但不包含敏感信息</li>
     *     <li>Token 默认有效期为 720 分钟（12 小时）</li>
     *     <li>密钥必须与验证 Token 时使用的密钥一致</li>
     * </ul>
     *
     * @param loginUserDTO 登录用户信息，不能为 null，需要包含用户ID、用户名、用户来源等信息
     * @param secret       JWT 签名密钥，不能为 null 或空字符串
     * @return TokenDTO 对象，包含 JWT Token（{@code accessToken}）和过期时间（{@code expires}，单位：分钟）
     * @see com.zmbdp.common.security.domain.dto.TokenDTO
     * @see com.zmbdp.common.security.domain.dto.LoginUserDTO
     * @see com.zmbdp.common.security.utils.JwtUtil#createToken(Map, String)
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
     * 根据 Token 获取登录用户信息
     * <p>
     * 从 JWT Token 中解析用户标识，然后从 Redis 中获取完整的用户信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 从请求头获取 Token
     * String token = request.getHeader("Authorization");
     * // 移除 "Bearer " 前缀（如果有）
     * if (token != null && token.startsWith("Bearer ")) {
     *     token = token.substring(7);
     * }
     *
     * // 获取用户信息
     * LoginUserDTO user = tokenService.getLoginUser(token, "your-secret-key");
     * if (user != null) {
     *     // 用户已登录，可以使用用户信息
     *     Long userId = user.getUserId();
     *     String username = user.getUserName();
     * } else {
     *     // Token 无效或已过期
     *     throw new ServiceException(ResultCode.UNAUTHORIZED, "未登录或Token已过期");
     * }
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>验证 Token 是否为空</li>
     *     <li>从 JWT Token 中解析用户标识（UUID）</li>
     *     <li>根据用户标识构建 Redis Key：{@code login_tokens:uuid}</li>
     *     <li>从 Redis 中获取用户信息（{@link LoginUserDTO}）</li>
     *     <li>返回用户信息，如果 Token 无效或已过期返回 null</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 Token 为空，返回 null</li>
     *     <li>如果 JWT Token 解析失败，会抛出异常并返回 null</li>
     *     <li>如果 Redis 中不存在用户信息，返回 null</li>
     *     <li>密钥必须与创建 Token 时使用的密钥一致</li>
     *     <li>如果发生异常，会记录警告日志并抛出运行时异常</li>
     * </ul>
     *
     * @param token  JWT Token 字符串，可以为 null 或空字符串（返回 null）
     * @param secret JWT 签名密钥，不能为 null 或空字符串
     * @return 登录用户信息（{@link LoginUserDTO}），如果 Token 无效或已过期返回 null
     * @throws RuntimeException 如果 Token 解析失败或获取用户信息时发生异常
     * @see com.zmbdp.common.security.domain.dto.LoginUserDTO
     * @see com.zmbdp.common.security.utils.JwtUtil#getUserKey(String, String)
     */
    public LoginUserDTO getLoginUser(String token, String secret) {
        // 创建一个空的用户对象
        LoginUserDTO user = null;
        // 然后解析 token
        try {
            if (StringUtil.isNotEmpty(token)) {
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
     * 根据 HTTP 请求获取登录用户信息
     * <p>
     * 从 HTTP 请求头中提取 Token，然后调用 {@link #getLoginUser(String, String)} 获取用户信息。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在拦截器或控制器中获取用户信息
     * @Autowired
     * private TokenService tokenService;
     *
     * public void doFilter(HttpServletRequest request, HttpServletResponse response) {
     *     LoginUserDTO user = tokenService.getLoginUser(request, "your-secret-key");
     *     if (user != null) {
     *         // 用户已登录，继续处理请求
     *         // 可以将用户信息存储到 ThreadLocal 中
     *         SecurityContextHolder.setUser(user);
     *     } else {
     *         // Token 无效或已过期，返回未授权错误
     *         response.setStatus(401);
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Token 从请求头 {@code Authorization} 中获取</li>
     *     <li>如果请求头中没有 Token，返回 null</li>
     *     <li>会自动处理 Token 前缀（如 "Bearer "）</li>
     *     <li>密钥必须与创建 Token 时使用的密钥一致</li>
     * </ul>
     *
     * @param request HTTP 请求对象，不能为 null
     * @param secret  JWT 签名密钥，不能为 null 或空字符串
     * @return 登录用户信息（{@link LoginUserDTO}），如果 Token 无效或已过期返回 null
     * @see #getLoginUser(String, String)
     * @see com.zmbdp.common.security.utils.SecurityUtil#getToken(HttpServletRequest)
     */
    public LoginUserDTO getLoginUser(HttpServletRequest request, String secret) {
        String token = SecurityUtil.getToken(request);
        return getLoginUser(token, secret);
    }

    /**
     * 从当前请求中获取登录用户信息（便捷方法）
     * <p>
     * 从当前线程的 HTTP 请求中提取 Token，然后获取用户信息。
     * 内部调用 {@link com.zmbdp.common.core.utils.ServletUtil#getRequest()} 获取当前请求。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在控制器或服务中直接获取当前登录用户
     * @Autowired
     * private TokenService tokenService;
     *
     * @GetMapping("/user/info")
     * public Result<LoginUserDTO> getCurrentUser() {
     *     LoginUserDTO user = tokenService.getLoginUser("your-secret-key");
     *     if (user != null) {
     *         return Result.success(user);
     *     } else {
     *         return Result.fail(ResultCode.UNAUTHORIZED, "未登录");
     *     }
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>必须在 HTTP 请求上下文中调用（不能在非 Web 环境中使用）</li>
     *     <li>如果当前线程没有 HTTP 请求，可能抛出异常</li>
     *     <li>Token 从请求头中获取</li>
     *     <li>密钥必须与创建 Token 时使用的密钥一致</li>
     * </ul>
     *
     * @param secret JWT 签名密钥，不能为 null 或空字符串
     * @return 登录用户信息（{@link LoginUserDTO}），如果 Token 无效或已过期返回 null
     * @see #getLoginUser(HttpServletRequest, String)
     * @see com.zmbdp.common.core.utils.ServletUtil#getRequest()
     */
    public LoginUserDTO getLoginUser(String secret) {
        return getLoginUser(ServletUtil.getRequest(), secret);
    }

    /**
     * 根据 Token 删除用户登录状态
     * <p>
     * 从 JWT Token 中解析用户标识，然后删除 Redis 中对应的用户信息。
     * 通常用于用户登出操作。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 用户登出
     * @PostMapping("/logout")
     * public Result<String> logout(HttpServletRequest request) {
     *     String token = SecurityUtil.getToken(request);
     *     tokenService.delLoginUser(token, "your-secret-key");
     *     return Result.success("登出成功");
     * }
     *
     * // 强制下线（管理员操作）
     * @PostMapping("/admin/forceLogout")
     * public Result<String> forceLogout(@RequestParam String token) {
     *     tokenService.delLoginUser(token, "your-secret-key");
     *     return Result.success("强制下线成功");
     * }
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>验证 Token 是否为空</li>
     *     <li>从 JWT Token 中解析用户标识（UUID）</li>
     *     <li>根据用户标识构建 Redis Key：{@code login_tokens:uuid}</li>
     *     <li>从 Redis 中删除用户信息</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 Token 为空，不执行任何操作</li>
     *     <li>如果 JWT Token 解析失败，不会抛出异常（静默失败）</li>
     *     <li>删除操作是幂等的，即使 Key 不存在也不会报错</li>
     *     <li>删除后，该 Token 将无法再用于获取用户信息</li>
     * </ul>
     *
     * @param token  JWT Token 字符串，如果为 null 或空字符串则不执行任何操作
     * @param secret JWT 签名密钥，不能为 null 或空字符串
     * @see com.zmbdp.common.security.utils.JwtUtil#getUserKey(String, String)
     */
    public void delLoginUser(String token, String secret) {
        if (StringUtil.isNotEmpty(token)) {
            String useKey = JwtUtil.getUserKey(token, secret);
            redisService.deleteObject(getTokenKey(useKey));
        }
    }

    /**
     * 根据用户ID和用户来源删除用户登录状态（管理员操作）
     * <p>
     * 查找所有匹配用户ID和用户来源的登录 Token，并删除对应的用户信息。
     * 用于管理员强制下线指定用户的所有登录会话。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 管理员强制下线用户
     * @PostMapping("/admin/forceLogoutUser")
     * public Result<String> forceLogoutUser(@RequestParam Long userId) {
     *     // 删除该用户的所有登录状态
     *     tokenService.delLoginUser(userId, "admin");
     *     return Result.success("强制下线成功");
     * }
     *
     * // 删除指定来源的用户登录状态
     * tokenService.delLoginUser(123L, "portal");
     * // 只会删除用户来源为 "portal" 的登录状态
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>验证用户ID是否为空</li>
     *     <li>查找 Redis 中所有以 {@code login_tokens:} 开头的 Key</li>
     *     <li>遍历所有 Key，获取对应的用户信息</li>
     *     <li>如果用户ID和用户来源都匹配，删除该 Key</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果用户ID为 null，不执行任何操作</li>
     *     <li>会遍历所有登录 Token，性能可能较慢（建议在后台任务中使用）</li>
     *     <li>必须同时匹配用户ID和用户来源才会删除</li>
     *     <li>删除操作是幂等的，即使没有匹配的 Token 也不会报错</li>
     *     <li>适用于管理员强制下线用户的场景</li>
     * </ul>
     *
     * @param userId   用户ID，如果为 null 则不执行任何操作
     * @param userFrom 用户来源（如 "admin"、"portal"），不能为 null，用于精确匹配
     * @see com.zmbdp.common.redis.service.RedisService#keys(String)
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
     * 验证并续期 Token
     * <p>
     * 检查 Token 的剩余时间，如果剩余时间少于 120 分钟，则自动续期到 720 分钟。
     * 通常在每个请求的拦截器中调用，确保活跃用户的 Token 不会过期。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 在拦截器中验证并续期 Token
     * @Component
     * public class AuthInterceptor implements HandlerInterceptor {
     *     @Autowired
     *     private TokenService tokenService;
     *
     *     @Override
     *     public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object account) {
     *         // 获取用户信息
     *         LoginUserDTO user = tokenService.getLoginUser(request, "your-secret-key");
     *         if (user != null) {
     *             // 验证并续期 Token（如果剩余时间少于 120 分钟，自动续期）
     *             tokenService.verifyToken(user);
     *             return true;
     *         }
     *         return false;
     *     }
     * }
     * }</pre>
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>获取 Token 的过期时间（{@code loginUserDTO.getExpireTime()}）</li>
     *     <li>获取当前时间（{@code System.currentTimeMillis()}）</li>
     *     <li>计算剩余时间：过期时间 - 当前时间</li>
     *     <li>如果剩余时间 <= 120 分钟，调用 {@link #refreshToken(LoginUserDTO)} 续期</li>
     *     <li>续期后，Token 有效期重置为 720 分钟</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>续期阈值：120 分钟（{@code MILLIS_MINUTE_TEN}）</li>
     *     <li>续期后有效期：720 分钟（{@code EXPIRE_TIME}）</li>
     *     <li>如果剩余时间大于 120 分钟，不执行续期操作</li>
     *     <li>续期操作会更新 Redis 中用户信息的过期时间</li>
     *     <li>建议在每个请求的拦截器中调用，确保活跃用户的 Token 不会过期</li>
     * </ul>
     *
     * @param loginUserDTO 登录用户信息，不能为 null，需要包含过期时间（{@code expireTime}）
     * @see #refreshToken(LoginUserDTO)
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
     * 设置用户身份信息（允许登录）
     * <p>
     * 将用户信息存储到 Redis 中，用于设置或更新用户登录状态。
     * 如果用户信息不为空且包含 Token，则刷新 Token 到 Redis。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 设置用户登录状态
     * LoginUserDTO loginUser = new LoginUserDTO();
     * loginUser.setToken("uuid-string");
     * loginUser.setUserId(123L);
     * loginUser.setUserName("admin");
     * tokenService.setLoginUser(loginUser);
     * // 用户信息已存储到 Redis，Token 有效期为 720 分钟
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果用户信息为 null 或 Token 为空，不执行任何操作</li>
     *     <li>内部调用 {@link #refreshToken(LoginUserDTO)} 存储用户信息</li>
     *     <li>Token 有效期为 720 分钟</li>
     *     <li>会更新登录时间和过期时间</li>
     * </ul>
     *
     * @param loginUserDTO 登录用户信息，如果为 null 或 Token 为空则不执行任何操作
     * @see #refreshToken(LoginUserDTO)
     */
    public void setLoginUser(LoginUserDTO loginUserDTO) {
        if (loginUserDTO != null && StringUtil.isNotEmpty(loginUserDTO.getToken())) {
            refreshToken(loginUserDTO);
        }
    }

    /**
     * 刷新 Token（缓存用户信息并设置过期时间）
     * <p>
     * 将用户信息存储到 Redis 中，并设置 Token 的有效期。
     * 会更新用户的登录时间和过期时间。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>设置登录时间：{@code System.currentTimeMillis()}</li>
     *     <li>计算过期时间：登录时间 + 720 分钟</li>
     *     <li>设置过期时间到用户信息中</li>
     *     <li>构建 Redis Key：{@code login_tokens:uuid}</li>
     *     <li>将用户信息存储到 Redis，过期时间为 720 分钟</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Token 有效期为 720 分钟（{@code EXPIRE_TIME}）</li>
     *     <li>登录时间设置为当前时间（毫秒）</li>
     *     <li>过期时间 = 登录时间 + 720 分钟</li>
     *     <li>Redis Key 格式：{@code login_tokens:uuid}（uuid 来自 {@code loginUserDTO.getToken()}）</li>
     *     <li>如果 Redis Key 已存在，会覆盖原有数据</li>
     * </ul>
     *
     * @param loginUserDTO 登录用户信息，不能为 null，需要包含 Token（UUID）
     * @see com.zmbdp.common.redis.service.RedisService#setCacheObject(String, Object, long, TimeUnit)
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
     * 获取 Token 在 Redis 中的 Key
     * <p>
     * 将用户标识（UUID）转换为 Redis Key 格式。
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 用户标识：abc-123-def-456
     * String tokenKey = getTokenKey("abc-123-def-456");
     * // 返回：login_tokens:abc-123-def-456
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>Key 格式：{@code login_tokens:uuid}</li>
     *     <li>前缀来自 {@code ACCESS_TOKEN} 常量（{@code TokenConstants.LOGIN_TOKEN_KEY}）</li>
     *     <li>用于在 Redis 中存储和获取用户信息</li>
     * </ul>
     *
     * @param token 用户标识（UUID），不能为 null
     * @return Redis Key 字符串，格式：{@code login_tokens:uuid}
     */
    private String getTokenKey(String token) {
        return ACCESS_TOKEN + token;
    }
}