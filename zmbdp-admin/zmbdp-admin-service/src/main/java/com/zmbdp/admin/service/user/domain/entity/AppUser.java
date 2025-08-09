package com.zmbdp.admin.service.user.domain.entity;

import com.zmbdp.common.core.domain.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * C端用户表对应的实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseDO {
    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 微信ID
     */
    private String openId;

    /**
     * 用户头像
     */
    private String avatar;
}