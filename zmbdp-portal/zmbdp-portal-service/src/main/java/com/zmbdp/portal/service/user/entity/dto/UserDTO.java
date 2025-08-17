package com.zmbdp.portal.service.user.entity.dto;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.portal.service.user.entity.vo.UserVo;
import lombok.Data;

/**
 * C端用户 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class UserDTO extends LoginUserDTO {

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 对象转换
     *
     * @return VO 对象
     */
    public UserVo convertToVO() {
        UserVo userVo = new UserVo();
        BeanCopyUtil.copyProperties(this, userVo);
        userVo.setNickName(this.getUserName());
        return userVo;
    }
}