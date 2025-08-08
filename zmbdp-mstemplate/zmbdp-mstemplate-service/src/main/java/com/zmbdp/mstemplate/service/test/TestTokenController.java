package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.common.security.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/test/token")
public class TestTokenController {

    @Autowired
    private TokenService tokenService;

    @Value("${jwt.token.secret}")
    private String secret;

    /**
     * 获取 token
     *
     * @return token
     */
    @RequestMapping("/get")
    public String get() {
        // 添加日志打印密钥长度和内容（仅用于调试，生产环境不要打印密钥）
        System.out.println("Secret length: " + secret.length());
        System.out.println("Secret: " + secret);

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(1L);
        loginUserDTO.setUserFrom("test");
        loginUserDTO.setUserName("test");
        loginUserDTO.setLoginTime(System.currentTimeMillis());
        TokenDTO token = tokenService.createToken(loginUserDTO, secret);
        System.out.println("Token: " + token.getAccessToken());
        return token.getAccessToken();
    }
    // 生成的 token： eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyX2lkIjoxLCJ1c2VyX2Zyb20iOiJ0ZXN0IiwidXNlcl9rZXkiOiIzMmE4YjNlNC01NjY4LTRhNzAtYjg3Ni1mYWM4YzAxNTBmM2MiLCJ1c2VybmFtZSI6InRlc3QifQ.GsdrAyyc9xORoWZLSe_jp9rKwMLSRUtra2c6zadsve8HWcg4H2H2OWOiSSn5uAlhY4r78uWi3vq-7XcES7kHBQ

    // 然后根据这个 token 获取数据
    @GetMapping("/getLoginUser")
    public LoginUserDTO getLoginUser(String token) {
        return tokenService.getLoginUser(token, secret);
    }

    /**
     * 删除用户登录状态
     *
     * @param token JWT token
     * @return 操作结果
     */
    @GetMapping("/delLoginUser")
    public String delLoginUser(String token) {
        try {
            tokenService.delLoginUser(token, secret);
            return "用户登录状态删除成功";
        } catch (Exception e) {
            return "删除失败: " + e.getMessage();
        }
    }

    /**
     * 测试令牌续期功能
     *
     * @param token JWT token
     * @return 操作结果
     */
    @GetMapping("/verifyToken")
    public String verifyToken(String token) {
        try {
            // 先获取用户信息
            LoginUserDTO user = tokenService.getLoginUser(token, secret);
            if (user != null) {
                // 执行令牌续期
                tokenService.verifyToken(user);
                return "令牌续期成功";
            } else {
                return "未找到用户信息，无法续期";
            }
        } catch (Exception e) {
            return "续期失败: " + e.getMessage();
        }
    }

    /**
     * 根据用户 ID 和来源删除登录状态
     *
     * @param userId 用户ID
     * @param userFrom 用户来源
     * @return 操作结果
     */
    @GetMapping("/delLoginUserById")
    public String delLoginUserById(Long userId, String userFrom) {
        try {
            tokenService.delLoginUser(userId, userFrom);
            return "用户登录状态删除成功";
        } catch (Exception e) {
            return "删除失败: " + e.getMessage();
        }
    }

    /**
     * 设置用户身份信息
     *
     * @param token JWT token
     * @return 操作结果
     */
    @GetMapping("/setLoginUser")
    public String setLoginUser(String token) {
        try {
            // 先获取用户信息
            LoginUserDTO user = tokenService.getLoginUser(token, secret);
            if (user != null) {
                // 设置用户身份信息
                user.setUserName("demo");
                tokenService.setLoginUser(user);
                return "用户身份信息设置成功";
            } else {
                return "未找到用户信息，无法设置";
            }
        } catch (Exception e) {
            return "设置失败: " + e.getMessage();
        }
    }
}
