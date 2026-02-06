package com.zmbdp.common.log.resolver;

import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用户上下文解析器
 * <p>
 * 负责从 Token、请求头等来源解析并填充操作者信息（userId、userName）到日志 DTO 中。<br>
 * 支持多种用户信息获取方式，按优先级依次尝试。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>从 Token 解析用户信息：通过 TokenService 解析 JWT Token 获取用户信息</li>
 *     <li>从请求头获取用户信息：从 HTTP 请求头中读取 userId 和 userName</li>
 *     <li>异常容错处理：解析失败时不影响日志记录，继续尝试其他方式</li>
 * </ul>
 * <p>
 * <b>解析优先级：</b>
 * <ol>
 *     <li>优先从 TokenService 解析 JWT Token 获取用户信息（需要配置 jwt.token.secret）</li>
 *     <li>如果 Token 解析失败，从请求头 userId 和 userName 获取</li>
 *     <li>如果都获取失败，用户信息字段保持为 null</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private UserContextResolver userContextResolver;
 *
 * OperationLogDTO logDTO = new OperationLogDTO();
 * // 填充用户信息
 * userContextResolver.fill(logDTO);
 * // logDTO.getUserId() 和 logDTO.getUserName() 已被填充
 * }</pre>
 * <p>
 * <b>配置说明：</b>
 * <ul>
 *     <li>jwt.token.secret：JWT Token 密钥（可选，如果配置则启用 Token 解析）</li>
 *     <li>TokenService：Token 服务（可选注入，如果存在则启用 Token 解析）</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>TokenService 为可选依赖（required = false），不存在时不影响功能</li>
 *     <li>所有异常都会被捕获，不会影响日志记录流程</li>
 *     <li>如果无法获取用户信息，userId 和 userName 保持为 null</li>
 *     <li>请求头中的 userId 需要是有效的数字格式，否则会被忽略</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see TokenService
 * @see OperationLogDTO
 */
@Slf4j
@Component
public class UserContextResolver {

    /**
     * Token 服务
     * <p>
     * 用于解析 JWT Token 获取用户信息。<br>
     * 设置为可选依赖（required = false），如果不存在则跳过 Token 解析。
     */
    @Autowired(required = false)
    private TokenService tokenService;

    /**
     * JWT Token 密钥
     * <p>
     * 配置项：jwt.token.secret，默认为空字符串。<br>
     * 如果未配置，则跳过 Token 解析。
     */
    @Value("${jwt.token.secret:}")
    private String jwtSecret;

    /**
     * 填充用户信息到日志 DTO
     * <p>
     * 按优先级依次尝试从不同来源获取用户信息并填充到日志 DTO 中。<br>
     * 所有异常都会被捕获，不会影响日志记录流程。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>检查 TokenService 和 jwtSecret 是否可用</li>
     *     <li>如果可用，尝试从 Token 解析用户信息</li>
     *     <li>如果 Token 解析成功，填充 userId 和 userName 并返回</li>
     *     <li>如果 Token 解析失败，从请求头获取 userId 和 userName</li>
     *     <li>如果请求头中存在 userId，尝试解析为 Long 类型并填充</li>
     *     <li>如果请求头中存在 userName，直接填充</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * OperationLogDTO logDTO = new OperationLogDTO();
     * userContextResolver.fill(logDTO);
     * // 用户信息已填充
     * Long userId = logDTO.getUserId();
     * String userName = logDTO.getUserName();
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>如果 logDTO 为 null，可能抛出 NullPointerException</li>
     *     <li>Token 解析失败不会抛出异常，会继续尝试从请求头获取</li>
     *     <li>请求头中的 userId 必须是有效的数字格式，否则会被忽略</li>
     *     <li>如果所有方式都失败，userId 和 userName 保持为 null</li>
     *     <li>异常会被记录到日志中，但不会影响日志记录流程</li>
     * </ul>
     *
     * @param logDTO 操作日志 DTO，不能为 null
     * @see TokenService#getLoginUser(String)
     * @see ServletUtil#getRequest()
     */
    public void fill(OperationLogDTO logDTO) {
        try {
            // 优先从 Token 解析用户信息
            if (tokenService != null && StringUtil.isNotEmpty(jwtSecret)) {
                try {
                    LoginUserDTO loginUser = tokenService.getLoginUser(jwtSecret);
                    if (loginUser != null) {
                        logDTO.setUserId(loginUser.getUserId());
                        logDTO.setUserName(loginUser.getUserName());
                        return;
                    }
                } catch (Exception e) {
                    // Token 解析失败，继续尝试其他方式
                    log.debug("从 Token 解析用户信息失败: {}", e.getMessage());
                }
            }

            // 从请求头获取用户信息
            HttpServletRequest request = ServletUtil.getRequest();
            if (request != null) {
                String userIdStr = request.getHeader("userId");
                if (StringUtil.isNotEmpty(userIdStr)) {
                    try {
                        logDTO.setUserId(Long.parseLong(userIdStr));
                    } catch (NumberFormatException e) {
                        log.debug("解析请求头 userId 失败: {}", userIdStr);
                    }
                }
                String userName = request.getHeader("userName");
                if (StringUtil.isNotEmpty(userName)) {
                    logDTO.setUserName(userName);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
        }
    }
}