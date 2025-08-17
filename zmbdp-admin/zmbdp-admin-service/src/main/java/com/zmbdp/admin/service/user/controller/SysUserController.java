package com.zmbdp.admin.service.user.controller;

import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserListReqDTO;
import com.zmbdp.admin.service.user.domain.vo.SysUserLoginVO;
import com.zmbdp.admin.service.user.domain.vo.SysUserVo;
import com.zmbdp.admin.service.user.service.ISysUserService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.TokenVO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B端用户服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/sys_user")
public class SysUserController {

    /**
     * B端用户服务
     */
    @Autowired
    private ISysUserService sysUserService;

    /**
     * B端用户登录
     *
     * @param passwordLoginDTO B端用户信息
     * @return token 信息
     */
    @PostMapping("/login/password")
    public Result<TokenVO> login(@Validated @RequestBody PasswordLoginDTO passwordLoginDTO) {
        TokenDTO tokenDTO = sysUserService.login(passwordLoginDTO);
        return Result.success(tokenDTO.convertToVo());
    }

    /**
     * 新增或编辑用户
     *
     * @param sysUserDTO B端用户信息
     * @return 用户 ID
     */
    @PostMapping("/add_edit")
    public Result<Long> addOrEditUser(@Validated @RequestBody SysUserDTO sysUserDTO) {
        return Result.success(sysUserService.addOrEdit(sysUserDTO));
    }

    /**
     * 查询 B端用户
     *
     * @param sysUserListReqDTO 用户查询 DTO
     * @return B端用户列表
     */
    @PostMapping("/list")
    public Result<List<SysUserVo>> getUserList(@RequestBody SysUserListReqDTO sysUserListReqDTO) {
        List<SysUserDTO> sysUserDTOS = sysUserService.getUserList(sysUserListReqDTO);
        List<SysUserVo> result = BeanCopyUtil.copyListProperties(sysUserDTOS, SysUserVo::new);
        return Result.success(result);
    }

    /**
     * 获取 B端登录用户信息
     *
     * @return B端用户信息 VO
     */
    @GetMapping("/login_info/get")
    public Result<SysUserLoginVO> getLoginUser() {
        return Result.success(sysUserService.getLoginUser().convertToVO());
    }

    /**
     * 退出登录
     *
     * @return void
     */
    @DeleteMapping("/logout")
    Result<Void> logout() {
        sysUserService.logout();
        return Result.success();
    }
}
