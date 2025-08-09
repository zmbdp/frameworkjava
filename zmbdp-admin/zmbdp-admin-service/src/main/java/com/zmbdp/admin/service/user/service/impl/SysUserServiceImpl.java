package com.zmbdp.admin.service.user.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserListReqDTO;
import com.zmbdp.admin.service.user.domain.entity.SysUser;
import com.zmbdp.admin.service.user.mapper.SysUserMapper;
import com.zmbdp.admin.service.user.service.ISysUserService;
import com.zmbdp.common.core.utils.AESUtil;
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

import java.util.List;

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
     * 字典服务
     */
    @Autowired
    private ISysDictionaryService sysDictionaryService;

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

    /**
     * 新增或编辑用户
     *
     * @param sysUserDTO B端用户信息
     * @return 用户 ID
     */
    @Override
    public Long addOrEdit(SysUserDTO sysUserDTO) {
        SysUser sysUser = new SysUser();
        // 根据用户 ID 判断是新增还是编辑
        if (sysUserDTO.getUserId() == null) {
            // 说明是新增
            // 先执行各种各样的校验
            validateSysUser(sysUserDTO);
            // 判断完成后，执行新增用户逻辑
            sysUser.setPhoneNumber(
                    // 加密手机号
                    AESUtil.encryptHex(sysUserDTO.getPhoneNumber())
            );
            sysUser.setIdentity(sysUserDTO.getIdentity());
        }
        // 存储名字，密码，id 这些
        sysUser.setId(sysUserDTO.getUserId());
        sysUser.setNickName(sysUserDTO.getNickName());
        // 密码要加密
        sysUser.setPassword(DigestUtil.sha256Hex(sysUserDTO.getPassword()));
        // 判断用户状态 这个状态在数据库中是否存在
        if (sysDictionaryService.getDicDataByKey(sysUserDTO.getStatus()) == null) {
            throw new ServiceException("用户状态错误", ResultCode.INVALID_PARA.getCode());
        }
        sysUser.setStatus(sysUserDTO.getStatus());
        sysUser.setRemark(sysUserDTO.getRemark());
        // 根据主键判断是更新还是新增，主键存在并且数据库中也存在就是更新，不存在就是新增
        sysUserMapper.insertOrUpdate(sysUser);
        // 踢人逻辑
        // 表示如果这个用户在数据库中存在，让他强制下线，这就是踢人
        if (sysUserDTO.getUserId() != null && sysUserDTO.getStatus().equals(UserConstants.USER_DISABLE)) {
            tokenService.delLoginUser(sysUserDTO.getUserId(), UserConstants.USER_FROM_TU_B);
        }
        return sysUser.getId();
    }

    /**
     * 校验新增的用户信息
     *
     * @param sysUserDTO 用户信息
     * @throws ServiceException 校验失败时抛出异常
     */
    private void validateSysUser(SysUserDTO sysUserDTO) {
        // 先校验手机号
        if (!VerifyUtil.checkPhone(sysUserDTO.getPhoneNumber())) {
            throw new ServiceException("手机格式错误", ResultCode.INVALID_PARA.getCode());
        }
        // 校验密码
        if (StringUtils.isEmpty(sysUserDTO.getPassword()) || !sysUserDTO.checkPassword()) {
            throw new ServiceException("密码校验失败", ResultCode.INVALID_PARA.getCode());
        }
        // 手机号唯一性判断
        SysUser existSysUser = sysUserMapper.selectByPhoneNumber(AESUtil.encryptHex(sysUserDTO.getPhoneNumber()));
        if (existSysUser != null) {
            throw new ServiceException("当前手机号已注册", ResultCode.INVALID_PARA.getCode());
        }
        // 判断身份信息
        if (StringUtils.isEmpty(sysUserDTO.getIdentity()) || sysDictionaryService.getDicDataByKey(sysUserDTO.getIdentity()) == null) {
            throw new ServiceException("用户身份错误", ResultCode.INVALID_PARA.getCode());
        }
    }

    /**
     * 查询 B端用户
     *
     * @param sysUserListReqDTO 用户查询 DTO
     * @return B端用户列表
     */
    @Override
    public List<SysUserDTO> getUserList(SysUserListReqDTO sysUserListReqDTO) {

    }
}
