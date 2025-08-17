package com.zmbdp.admin.service.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.admin.service.user.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sys_user 表的 mapper
 *
 * @author 稚名不带撇
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /**
     * 根据手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return 用户信息
     */
    SysUser selectByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    /**
     * 查询用户列表
     *
     * @param sysUser 查询条件
     * @return 用户列表
     */
    List<SysUser> selectList(SysUser sysUser);
}
