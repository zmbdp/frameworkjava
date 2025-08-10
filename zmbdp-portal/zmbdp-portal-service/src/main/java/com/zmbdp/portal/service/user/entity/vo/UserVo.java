package com.zmbdp.portal.service.user.entity.vo;

import com.zmbdp.common.domain.domain.vo.LoginUserVO;
import lombok.Data;

/**
 * C端用户 VO
 */
@Data
public class UserVo extends LoginUserVO {

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;
}