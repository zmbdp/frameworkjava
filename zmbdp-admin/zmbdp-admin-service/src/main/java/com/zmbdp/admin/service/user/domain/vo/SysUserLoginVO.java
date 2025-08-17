package com.zmbdp.admin.service.user.domain.vo;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.vo.LoginUserVO;
import lombok.Data;

/**
 * B端用户登录信息
 *
 * @author 稚名不带撇
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
     *
     * @return B端用户 登录信息 VO
     */
    public SysUserLoginVO convertToVO() {
        SysUserLoginVO sysUserLoginVO = new SysUserLoginVO();
        BeanCopyUtil.copyProperties(this, sysUserLoginVO);
        return sysUserLoginVO;
    }
}