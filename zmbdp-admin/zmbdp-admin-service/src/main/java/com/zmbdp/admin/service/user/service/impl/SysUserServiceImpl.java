package com.zmbdp.admin.service.user.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.DigestUtil;
import com.zmbdp.admin.service.user.domain.entity.SysUser;
import com.zmbdp.common.core.utils.AESUtil;
import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.mapper.SysUserMapper;
import com.zmbdp.admin.service.user.service.ISysUserService;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.constants.UserConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.common.security.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * B端用户服务 service 层
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
@RefreshScope
public class SysUserServiceImpl implements ISysUserService {

    /**
     * 用户 mapper
     */
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * token 服务
     */
    @Autowired
    private TokenService tokenService;

    /**
     * token 密钥
     */
    @Value("${jwt.token.secret}")
    private String secret;

    /**
     * B端用户登录
     *
     * @param passwordLoginDTO B端用户信息
     * @return token 信息
     */
    @Override
    public TokenDTO login(PasswordLoginDTO passwordLoginDTO) {
        // 校验格式
        String phone = passwordLoginDTO.getPhone();
        if (!VerifyUtil.checkPhone(phone)) {
            throw new ServiceException("手机号不合理", ResultCode.INVALID_PARA.getCode());
        }
        // 然后加密手机号
        String phoneNumber = AESUtil.encryptHex(phone);
        // 根据加密后的手机号查库是否存在
        SysUser sysUser = sysUserMapper.selectByPhoneNumber(phoneNumber);
        if (sysUser == null) {
            throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
        }
        // 检查密码是否正确
        // 先解密
        String password = AESUtil.decryptHex(passwordLoginDTO.getPassword());
        if (StringUtils.isEmpty(password)) {
            throw new ServiceException("密码解析为空", ResultCode.INVALID_PARA.getCode());
        }
        // 然后再使用 DigestUtil.sha256Hex() 方法加密成不可逆的密码
        String passwordEncrypt = DigestUtil.sha256Hex(password);
        // 和数据库的比较
        if (!passwordEncrypt.equals(sysUser.getPassword())) {
            throw new ServiceException("密码不正确", ResultCode.INVALID_PARA.getCode());
        }
        // 校验用户的状态
        if (sysUser.getStatus().equals(UserConstants.USER_DISABLE)) {
            throw new ServiceException(ResultCode.USER_DISABLE);
        }
        // 设置登录信息
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(sysUser.getId());
        loginUserDTO.setUserName(sysUser.getNickName());
        loginUserDTO.setUserFrom(UserConstants.USER_FROM_TU_B);
        // 都成功之后设置 token 返回
        return tokenService.createToken(loginUserDTO, secret);
    }
}
