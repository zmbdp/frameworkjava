package com.zmbdp.admin.service.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.admin.api.appuser.domain.dto.AppUserListReqDTO;
import com.zmbdp.admin.service.user.domain.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * app_user 表的 mapper
 *
 * @author 稚名不带撇
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    /**
     * 根据 openId 查询用户信息
     * @param openId 用户微信 ID
     * @return C端用户
     */
    AppUser selectByOpenId(@Param("openId") String openId);

    /**
     * 根据手机号查询用户信息
     * @param phoneNumber 手机号
     * @return C端用户
     */
    AppUser selectByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * 查询总数
     * @param appUserListReqDTO 查询 C端用户 DTO
     * @return 用户总数
     */
    Long selectCount(AppUserListReqDTO appUserListReqDTO);

    /**
     * 分页查询 C端用户
     * @param appUserListReqDTO 查询 C端用户 DTO
     * @return 用户列表
     */
    List<AppUser> selectPage(AppUserListReqDTO appUserListReqDTO);
}
