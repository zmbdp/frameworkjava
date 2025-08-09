package com.zmbdp.admin.service.user.controller;

import com.zmbdp.admin.service.user.domain.dto.PasswordLoginDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserDTO;
import com.zmbdp.admin.service.user.domain.dto.SysUserListReqDTO;
import com.zmbdp.admin.service.user.domain.vo.SysUserVo;
import com.zmbdp.admin.service.user.service.ISysUserService;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.TokenVO;
import com.zmbdp.common.security.domain.dto.TokenDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * B端用户服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/sys_user")
public class SysUserController {

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
    public Result<Long> addOrEditUser(@RequestBody SysUserDTO sysUserDTO) {
        return Result.success(sysUserService.addOrEdit(sysUserDTO));
    }

    /**
     * 查询 B端用户
     * @param sysUserListReqDTO 用户查询 DTO
     * @return B端用户列表
     */
    @PostMapping("/list")
    public Result<List<SysUserVo>> getUserList(@RequestBody SysUserListReqDTO sysUserListReqDTO) {
        List<SysUserDTO> sysUserDTOS = sysUserService.getUserList(sysUserListReqDTO);
        return Result.success(sysUserDTOS.stream()
                .map(SysUserDTO::convertToVO)
                .collect(Collectors.toList())
        );
    }
}
