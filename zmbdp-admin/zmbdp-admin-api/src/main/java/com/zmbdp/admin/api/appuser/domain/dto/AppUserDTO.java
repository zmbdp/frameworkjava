package com.zmbdp.admin.api.appuser.domain.dto;

import com.zmbdp.admin.api.appuser.domain.vo.AppUserVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * C端用户 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class AppUserDTO implements Serializable {

    /**
     * C端用户 ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 微信 ID
     */
    private String openId;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * DTO 对象转换 VO 对象
     * @return VO 对象
     */
    public AppUserVo convertToVO() {
        AppUserVo appUserVo = new AppUserVo();
        BeanUtils.copyProperties(this, appUserVo);
        return appUserVo;
    }
}