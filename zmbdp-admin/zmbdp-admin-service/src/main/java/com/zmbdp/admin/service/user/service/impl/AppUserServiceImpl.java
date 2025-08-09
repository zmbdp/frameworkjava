package com.zmbdp.admin.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.dto.AppUserDTO;
import com.zmbdp.admin.service.user.domain.entity.AppUser;
import com.zmbdp.admin.service.user.mapper.AppUserMapper;
import com.zmbdp.admin.service.user.service.IAppUserService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
     * C端用户的 mapper
     */
    @Autowired
    private AppUserMapper appUserMapper;

    /**
     * nacos 上的默认头像
     */
    @Value("${appuser.info.defaultAvatar}")
    private String defaultAvatar;

    /**
     * 根据微信 ID 注册用户
     *
     * @param openId 用户微信 ID
     * @return C端用户 DTO
     */
    @Override
    public AppUserDTO registerByOpenId(String openId) {
        // 微信 id 判空
        if (StringUtils.isEmpty(openId)) {
            throw new ServiceException("微信ID不能为空", ResultCode.INVALID_PARA.getCode());
        }
        // TODO: 不需要判重
//        // 查看是否存在
//        if (appUserMapper.selectByOpenId(openId) != null) {
//            throw new ServiceException("用户已存在", ResultCode.INVALID_PARA.getCode());
//        }
        // 属性赋值插入数据库
        AppUser appUser = new AppUser();
        appUser.setOpenId(openId);
        appUser.setNickName("Java脚手架用户" + (int) (Math.random() * 9000) + 1000);
        appUser.setAvatar(defaultAvatar);
        appUserMapper.insert(appUser);
        // 对象转换
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanCopyUtil.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }
}
