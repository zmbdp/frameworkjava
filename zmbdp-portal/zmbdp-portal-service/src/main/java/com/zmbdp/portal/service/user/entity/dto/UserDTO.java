package com.zmbdp.portal.service.user.entity.dto;

import com.zmbdp.common.security.domain.dto.LoginUserDTO;
import com.zmbdp.portal.service.user.entity.vo.UserVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * C端用户DTO
 */
@Data
public class UserDTO extends LoginUserDTO {

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 对象转换
     * @return
     */
    public UserVo convertToVO() {
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(this, userVo);
        userVo.setNickName(this.getUserName());
        return userVo;
    }
}