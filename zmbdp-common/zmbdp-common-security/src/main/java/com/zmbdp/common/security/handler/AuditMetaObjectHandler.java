package com.zmbdp.common.security.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zmbdp.common.core.utils.ServletUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * MyBatis-Plus 审计字段自动填充处理器
 * <p>
 * 在 insert / update 操作时自动填充 BaseEntity 中的审计字段：
 * <ul>
 *     <li>insert 时填充：createBy、createTime、updateBy、updateTime</li>
 *     <li>update 时填充：updateBy、updateTime</li>
 * </ul>
 * <p>
 * 当前操作人 ID 通过 {@link TokenService} 从请求上下文中解析 JWT Token 获取。
 * 在非 Web 环境（如定时任务、异步线程）或 Token 解析失败时，操作人字段保持为 null，
 * 不影响数据写入流程。
 *
 * @author 稚名不带撇
 */
@Slf4j
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * Token 服务（可选依赖）
     * <p>
     * 用于从当前请求的 JWT Token 中解析登录用户信息。<br>
     * 设置为可选依赖，不存在时操作人字段不填充。
     */
    @Autowired(required = false)
    private TokenService tokenService;

    /**
     * JWT Token 密钥
     * <p>
     * 配置项：jwt.token.secret。<br>
     * 用于解析 JWT Token 获取当前登录用户。
     */
    @Value("${jwt.token.secret}")
    private String jwtSecret;

    /**
     * 插入时自动填充
     * <p>
     * 填充 createBy、createTime、updateBy、updateTime 四个字段。<br>
     * 其中 updateBy、updateTime 也一并填充，因为新增的数据也需要记录"最后修改信息"。
     *
     * @param metaObject MyBatis-Plus 反射对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        Long currentUserId = getCurrentUserId();

        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateTime", Date.class, now);
    }

    /**
     * 更新时自动填充
     * <p>
     * 只填充 updateBy、updateTime 两个字段。<br>
     * createBy、createTime 在 insert 时已确定，update 时不修改。
     *
     * @param metaObject MyBatis-Plus 反射对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateBy", Long.class, getCurrentUserId());
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }

    /**
     * 获取当前登录用户 ID
     * <p>
     * 通过 TokenService 解析当前请求中的 JWT Token 获取用户信息。<br>
     * 在以下情况下返回 null：
     * <ul>
     *     <li>TokenService 未注入（非 Web 环境）</li>
     *     <li>jwtSecret 未配置</li>
     *     <li>当前线程没有 HTTP 请求上下文（异步线程、定时任务等）</li>
     *     <li>Token 解析失败或用户未登录</li>
     * </ul>
     * <p>
     * 任何异常都会被捕获，不会影响数据写入流程。
     *
     * @return 当前登录用户 ID，获取失败返回 null
     */
    private Long getCurrentUserId() {
        try {
            if (tokenService == null || StringUtil.isEmpty(jwtSecret)) {
                return null;
            }
            HttpServletRequest request = ServletUtil.getRequest();
            if (request == null) {
                return null;
            }
            LoginUserDTO loginUser = tokenService.getLoginUser(request, jwtSecret);
            return loginUser != null ? loginUser.getUserId() : null;
        } catch (Exception e) {
            log.debug("自动填充审计字段时获取当前用户失败: {}", e.getMessage());
            return null;
        }
    }
}