package com.zmbdp.admin.service.user.domain.dto;

import com.zmbdp.admin.service.user.domain.vo.SysUserLoginVO;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import lombok.Data;

/**
 * B端登录用户信息 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class SysUserLoginDTO extends LoginUserDTO {

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 身份
     */
    private String identity;

    /**
     * 状态
     */
    private String status;

    /**
     * B端用户 登录信息 DTO 转 VO
     *
     * @return B端用户 登录信息 VO
     */
    public SysUserLoginVO convertToVO() {
        SysUserLoginVO sysUserLoginVO = new SysUserLoginVO();
        BeanCopyUtil.copyProperties(this, sysUserLoginVO);
        return sysUserLoginVO;
    }
}