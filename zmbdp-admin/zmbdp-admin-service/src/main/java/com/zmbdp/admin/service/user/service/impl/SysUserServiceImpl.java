package com.zmbdp.admin.service.user.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserListReqDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserLoginDTO;
import com.zmbdp.admin.service.user.domain.entity.SysUser;
import com.zmbdp.admin.service.user.mapper.SysUserMapper;
import com.zmbdp.admin.service.user.service.ISysUserService;
import com.zmbdp.common.core.utils.AESUtil;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.core.utils.VerifyUtil;
import com.zmbdp.common.domain.constants.UserConstants;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import com.zmbdp.common.security.service.TokenService;
import com.zmbdp.common.security.utils.JwtUtil;
import com.zmbdp.common.security.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    @Value("${jwt.token.secret:}")
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
            throw new ServiceException("手机号或密码错误", ResultCode.INVALID_PARA.getCode());
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
            throw new ServiceException("手机号或密码错误", ResultCode.INVALID_PARA.getCode());
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
        // 获取登陆用户的 id
        Long userId = tokenService.getLoginUser(secret).getUserId();
        // 查询数据库他是平台管理员还是超管理员，只允许超级管理员新增/修改管理员的数据
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (
                sysUser != null &&
                StringUtils.isNoneEmpty(sysUser.getIdentity()) &&
                !sysUser.getIdentity().equals(UserConstants.SUPER_ADMIN)
        ) {
            log.warn("SysUserServiceImpl.addOrEdit: [平台管理员不能新增或修改账号信息, 平台管理员id: {} ]", sysUser.getId());
            throw new ServiceException("平台管理员不能新增或修改账号信息", ResultCode.FAILED.getCode());
        }
        sysUser = new SysUser();
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
            // 密码要加密, 新增肯定要加密码
            sysUser.setPassword(DigestUtil.sha256Hex(sysUserDTO.getPassword()));
        }
        // 存储名字，密码，id 这些
        sysUser.setId(sysUserDTO.getUserId());
        sysUser.setNickName(sysUserDTO.getNickName());
        sysUser.setIdentity(sysUserDTO.getIdentity());
        // 校验密码, 可能是编辑，编辑的话不传密码就是不编辑密码了
        if (StringUtils.isNotEmpty(sysUserDTO.getPassword())) {
            // 说明要修改密码
            if (!sysUserDTO.checkPassword()) {
                throw new ServiceException("密码校验失败", ResultCode.INVALID_PARA.getCode());
            }
            // 密码要加密
            sysUser.setPassword(DigestUtil.sha256Hex(sysUserDTO.getPassword()));
        }
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
        // 先转换成数据库的 DTO
        SysUser searchSysUser = new SysUser();
        BeanCopyUtil.copyProperties(sysUserListReqDTO, searchSysUser);
        searchSysUser.setPhoneNumber(
                AESUtil.encryptHex(sysUserListReqDTO.getPhoneNumber())
        );
        searchSysUser.setId(sysUserListReqDTO.getUserId());
        // 然后根据数据库的 DTO 去查询数据
        List<SysUser> sysUserList = sysUserMapper.selectList(searchSysUser);
        // 最后封装结果返回
        return sysUserList.stream()
                .map(sysUser -> {
                    SysUserDTO sysUserDTO = new SysUserDTO();
                    // 使用 Bean 拷贝工具拷贝大部分字段
                    BeanCopyUtil.copyProperties(sysUser, sysUserDTO);
                    // 特殊处理需要解密的手机号
                    sysUserDTO.setPhoneNumber(AESUtil.decryptHex(sysUser.getPhoneNumber()));
                    // 特殊处理字段名称不一致的情况
                    sysUserDTO.setUserId(sysUser.getId());
                    return sysUserDTO;
                }).collect(Collectors.toList());

    }

    /**
     * 获取 B端登录用户信息
     *
     * @return B端用户信息 DTO
     */
    @Override
    public SysUserLoginDTO getLoginUser() {
        // 直接从请求头中获取当前登录用户
        LoginUserDTO loginUserDTO = tokenService.getLoginUser(secret);
        // 然后再判断一下是否是正确的
        if (loginUserDTO == null || loginUserDTO.getUserId() == null) {
            throw new ServiceException("用户令牌有误", ResultCode.INVALID_PARA.getCode());
        }
        // 从数据库中获取用户信息
        SysUser sysUser = sysUserMapper.selectById(loginUserDTO.getUserId());
        if (sysUser == null) {
            throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
        }
        // 封装结果返回
        SysUserLoginDTO sysUserLoginDTO = new SysUserLoginDTO();
        // 赋值 redis 的属性
        BeanCopyUtil.copyProperties(loginUserDTO, sysUserLoginDTO);
        // 赋值数据库的属性
        BeanCopyUtil.copyProperties(sysUser, sysUserLoginDTO);
        return sysUserLoginDTO;
    }

    /**
     * 退出登录
     */
    @Override
    public void logout() {
        // 解析令牌, 拿出用户信息做个日志
        // 拿的是 JWT
        String Jwt = SecurityUtil.getToken();
        if (StringUtils.isEmpty(Jwt)) {
            return;
        }
        String userName = JwtUtil.getUserName(Jwt, secret);
        String userId = JwtUtil.getUserId(Jwt, secret);
        log.info("[{}] 退出了系统, 用户ID: {}", userName, userId);
        // 根据 jwt 删除用户缓存记录
        tokenService.delLoginUser(Jwt, secret);
    }
}
