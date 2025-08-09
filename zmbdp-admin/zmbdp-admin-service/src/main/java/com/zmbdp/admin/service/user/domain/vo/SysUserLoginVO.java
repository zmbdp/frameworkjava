package com.zmbdp.admin.service.user.domain.vo;

import com.zmbdp.common.domain.domain.vo.LoginUserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * B端用户登录信息
 */
@Data
public class SysUserLoginVO extends LoginUserVO {

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
     * @return B端用户 登录信息 VO
     */
    public SysUserLoginVO convertToVO() {
        SysUserLoginVO sysUserLoginVO = new SysUserLoginVO();
        BeanUtils.copyProperties(this, sysUserLoginVO);
        return sysUserLoginVO;
    }
}