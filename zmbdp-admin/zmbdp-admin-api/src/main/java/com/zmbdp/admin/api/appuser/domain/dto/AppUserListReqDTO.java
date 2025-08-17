package com.zmbdp.admin.api.appuser.domain.dto;

import com.zmbdp.common.domain.domain.dto.BasePageReqDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 查询 C端用户 DTO
 *
 * @author 稚名不带撇
 */
@Data
public class AppUserListReqDTO extends BasePageReqDTO implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 微信 openId
     */
    private String openId;
}