package com.zmbdp.admin.service.user.service.impl;

import com.zmbdp.admin.api.appuser.domain.dto.AppUserDTO;
import com.zmbdp.admin.api.appuser.domain.dto.AppUserListReqDTO;
import com.zmbdp.admin.api.appuser.domain.dto.UserEditReqDTO;
import com.zmbdp.admin.service.timedtask.bloom.ResetBloomFilterTimedTask;
import com.zmbdp.admin.service.user.config.RabbitConfig;
import com.zmbdp.admin.service.user.domain.entity.AppUser;
import com.zmbdp.admin.service.user.mapper.AppUserMapper;
import com.zmbdp.admin.service.user.service.IAppUserService;
import com.zmbdp.common.bloomfilter.service.BloomFilterService;
import com.zmbdp.common.core.domain.dto.BasePageDTO;
import com.zmbdp.common.core.utils.AESUtil;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
     * 用户前缀
     */
    private static final String APP_USER_PREFIX = "app_user:";

    /**
     * 用户手机号前缀
     */
    private static final String APP_USER_PHONE_NUMBER_PREFIX = "app_user:phone_number:";

    /**
     * 用户微信 ID 前缀
     */
    private static final String APP_USER_OPEN_ID_PREFIX = "app_user:open_id:";

    /**
     * C端用户的 mapper
     */
    @Autowired
    private AppUserMapper appUserMapper;

    /**
     * nacos 上的默认头像
     */
    @Value("${appuser.info.defaultAvatar:}")
    private String defaultAvatar;

    /**
     * RabbitMQ 服务
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 布隆过滤器服务
     */
    @Autowired
    private BloomFilterService bloomFilterService;

    /**
     * 重置布隆过滤器任务
     */
    @Autowired
    private ResetBloomFilterTimedTask resetBloomFilterTimedTask;

    /**
     * 布隆过滤器预热
     */
    @PostConstruct
    public void initSysUser() {
        // 预热 C端用户信息
        refreshAppUserBloomFilter();
    }

    /**
     * 刷新布隆过滤器
     */
    private void refreshAppUserBloomFilter() {
        log.info("布隆过滤器初始化开始 -----");
        resetBloomFilterTimedTask.refreshAppUserBloomFilter();
        log.info("布隆过滤器初始化结束 -----");
    }

    /*=============================================    内部调用    =============================================*/

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
        // 属性赋值插入数据库
        AppUser appUser = new AppUser();
        appUser.setOpenId(openId);
        appUser.setNickName("Java脚手架用户" + (int) (Math.random() * 9000) + 1000);
        appUser.setAvatar(defaultAvatar);
        appUserMapper.insert(appUser);
        // 在把这个微信的 id 添加到布隆过滤器中
        bloomFilterService.put(APP_USER_OPEN_ID_PREFIX + appUser.getOpenId());
        bloomFilterService.put(APP_USER_PREFIX + String.valueOf(appUser.getId()));
        // 对象转换
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanCopyUtil.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }

    /**
     * 根据 openId 查询用户信息
     *
     * @param openId 用户微信 ID
     * @return C端用户 DTO
     */
    @Override
    public AppUserDTO findByOpenId(String openId) {
        // 先查询布隆过滤器
        if (StringUtils.isEmpty(openId) || !bloomFilterService.mightContain(APP_USER_OPEN_ID_PREFIX + openId)) {
            return null;
        }
        AppUser appUser = appUserMapper.selectByOpenId(openId);
        return appUser == null ? null : appUserToAppUserDTO(appUser);
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param phoneNumber 手机号
     * @return C端用户 DTO
     */
    @Override
    public AppUserDTO findByPhone(String phoneNumber) {
        // 先查询布隆过滤器
        if (StringUtils.isEmpty(phoneNumber) || !bloomFilterService.mightContain(APP_USER_PHONE_NUMBER_PREFIX + AESUtil.encryptHex(phoneNumber))) {
            return null;
        }
        AppUser appUser = appUserMapper.selectByPhoneNumber(AESUtil.encryptHex(phoneNumber));
        return appUser == null ? null : appUserToAppUserDTO(appUser);
    }

    /**
     * 根据手机号注册用户
     *
     * @param phoneNumber 手机号
     * @return C端用户 DTO
     */
    @Override
    public AppUserDTO registerByPhone(String phoneNumber) {
        // 判空
        if (StringUtils.isEmpty(phoneNumber)) {
            throw new ServiceException("待注册手机号为空", ResultCode.INVALID_PARA.getCode());
        }
        // 属性赋值插入数据库
        AppUser appUser = new AppUser();
        appUser.setPhoneNumber(AESUtil.encryptHex(phoneNumber));
        appUser.setNickName("Java脚手架用户" + (int) (Math.random() * 9000) + 1000);
        appUser.setAvatar(defaultAvatar);
        appUserMapper.insert(appUser);
        // 将手机号加密之后添加到布隆过滤器中, 一定要添加 appUser 的，确保注册失败之后里面存的也是空
        bloomFilterService.put(APP_USER_PHONE_NUMBER_PREFIX + appUser.getPhoneNumber());
        bloomFilterService.put(APP_USER_PREFIX + String.valueOf(appUser.getId()));
        // 对象转换返回
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanCopyUtil.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }

    /**
     * 编辑 C端用户
     *
     * @param userEditReqDTO C端用户 DTO
     */
    @Override
    public void edit(UserEditReqDTO userEditReqDTO) {
        // 根据 id 查询
        // 先查询布隆过滤器
        if (!bloomFilterService.mightContain(APP_USER_PREFIX + String.valueOf(userEditReqDTO.getUserId()))) {
            throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
        }
        AppUser appUser = appUserMapper.selectById(userEditReqDTO.getUserId());
        // 没查到
        if (appUser == null) {
            throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
        }
        // 查到了就赋值入库
        BeanCopyUtil.copyProperties(userEditReqDTO, appUser);
        appUserMapper.updateById(appUser);
        // 告诉所有服务更改了数据库，发送消息，转换成 DTO 发送
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanCopyUtil.copyProperties(appUser, appUserDTO);
        appUserDTO.setUserId(appUser.getId());
        try {
            // 使用广播发送，关注过这个队列的消费者都会接收到消息
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "", appUserDTO);
        } catch (Exception e) {
            log.error("编辑用户发送消息失败: {}", e.getMessage());
        }
    }

    /**
     * AppUser 转 AppUserDTO
     *
     * @param appUser appUser 数据表
     * @return appUserDTO 对象
     */
    private AppUserDTO appUserToAppUserDTO(AppUser appUser) {
        // 转换对象赋值
        AppUserDTO appUserDTO = new AppUserDTO();
        BeanCopyUtil.copyProperties(appUser, appUserDTO);
        // 额外处理手机号
        appUserDTO.setPhoneNumber(AESUtil.decryptHex(appUser.getPhoneNumber()));
        appUserDTO.setUserId(appUser.getId());
        return appUserDTO;
    }

    /**
     * 根据用户 ID 获取用户信息
     *
     * @param userId 用户 ID
     * @return C端用户 VO
     */
    @Override
    public AppUserDTO findById(Long userId) {
        // 对 userId 进行判空操作
        if (userId == null || !bloomFilterService.mightContain(APP_USER_PREFIX + String.valueOf(userId))) {
            return null;
        }
        // 查询 appUser 对象
        AppUser appUser = appUserMapper.selectById(userId);
        return appUser == null ? null : appUserToAppUserDTO(appUser);
    }

    /**
     * 根据用户 ID 列表获取用户列表信息
     *
     * @param userIds 用户 ID 列表
     * @return C端用户 DTO 列表
     */
    @Override
    public List<AppUserDTO> getUserList(List<Long> userIds) {
        // 对入参进行判空
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of();
        }
        // 查询 appUser 列表
        List<AppUser> appUserList = appUserMapper.selectBatchIds(userIds);

        // 对象转换
        return appUserList.stream().map(appUser -> {
            AppUserDTO appUserDTO = new AppUserDTO();
            BeanCopyUtil.copyProperties(appUser, appUserDTO);
            // 特殊处理手机号和 id
            appUserDTO.setPhoneNumber(AESUtil.decryptHex(appUser.getPhoneNumber()));
            appUserDTO.setUserId(appUser.getId());
            return appUserDTO;
        }).collect(Collectors.toList());
    }

    /*=============================================    前端调用    =============================================*/

    /**
     * 查询 C端用户
     *
     * @param appUserListReqDTO 查询 C端用户 DTO
     * @return C端用户分页结果
     */
    @Override
    public BasePageDTO<AppUserDTO> getUserList(AppUserListReqDTO appUserListReqDTO) {
        // 先转变手机号
        appUserListReqDTO.setPhoneNumber(AESUtil.encryptHex(appUserListReqDTO.getPhoneNumber()));
        BasePageDTO<AppUserDTO> result = new BasePageDTO<>();
        // 查询总数
        Long totals = appUserMapper.selectCount(appUserListReqDTO);
        if (totals == 0) {
            result.setTotals(0);
            result.setTotalPages(0);
            result.setList(new ArrayList<>());
            return result;
        }
        // 分页查询
        List<AppUser> appUserList = appUserMapper.selectPage(appUserListReqDTO);
        result.setTotals(totals.intValue());
        result.setTotalPages(BasePageDTO.calculateTotalPages(totals, appUserListReqDTO.getPageSize()));
        // 判断分页查询出来的结果是否为空，可能是超页 (查询第三页，可是第三页没有数据)
        if (CollectionUtils.isEmpty(appUserList)) {
            result.setList(new ArrayList<>());
            return result;
        }
        // 对象列表结果转换
        result.setList(appUserList.stream()
                .map(appUser -> {
                    AppUserDTO appUserDTO = new AppUserDTO();
                    BeanCopyUtil.copyProperties(appUser, appUserDTO);
                    appUserDTO.setUserId(appUser.getId());
                    appUserDTO.setPhoneNumber(AESUtil.decryptHex(appUser.getPhoneNumber()));
                    return appUserDTO;
                }).collect(Collectors.toList())
        );
        return result;
    }
}
