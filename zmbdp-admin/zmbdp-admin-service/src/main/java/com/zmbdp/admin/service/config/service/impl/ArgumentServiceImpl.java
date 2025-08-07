package com.zmbdp.admin.service.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;
import com.zmbdp.admin.service.config.domain.entity.SysArgument;
import com.zmbdp.admin.service.config.mapper.SysArgumentMapper;
import com.zmbdp.admin.service.config.service.IArgumentService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 参数服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class ArgumentServiceImpl implements IArgumentService {

    @Autowired
    private SysArgumentMapper sysArgumentMapper;

    /**
     * 新增参数
     *
     * @param argumentAddReqDTO 新增参数请求 DTO
     * @return 数据库的 id
     */
    @Override
    public Long addArgument(ArgumentAddReqDTO argumentAddReqDTO) {
        // 查一下看看有没有重复的 参数业务主键 (configKey) 或者是 参数名称 (name)
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getConfigKey, argumentAddReqDTO.getConfigKey())
                .or()
                .eq(SysArgument::getName, argumentAddReqDTO.getName())
        );
        if (sysArgument != null) {
            log.warn("ArgumentServiceImpl.addArgument: [参数业务主键或参数名称重复: {} ]", argumentAddReqDTO);
            throw new ServiceException("参数业务主键或参数名称重复");
        }
        // 说明没有，直接转对象插入数据库
        sysArgument = new SysArgument();
        BeanCopyUtil.copyProperties(argumentAddReqDTO, sysArgument);
        sysArgumentMapper.insert(sysArgument);
        return sysArgument.getId();
    }
}
