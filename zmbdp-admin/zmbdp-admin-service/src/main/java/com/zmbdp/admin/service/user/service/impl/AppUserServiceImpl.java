package com.zmbdp.admin.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.dto.AppUserDTO;
import com.zmbdp.admin.api.appuser.domain.vo.AppUserVo;
import com.zmbdp.admin.service.user.service.IAppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * C端用户服务 service
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
@RefreshScope
public class AppUserServiceImpl implements IAppUserService {

    /**
     * 根据微信 ID 注册用户
     *
     * @param openId 用户微信 ID
     * @return C端用户 VO
     */
    @Override
    public AppUserDTO registerByOpenId(String openId) {
        //
    }
}
