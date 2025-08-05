package com.zmbdp.admin.service.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;
import com.zmbdp.admin.service.config.domain.entity.SysDictionaryType;
import com.zmbdp.admin.service.config.mapper.SysDictionaryTypeMapper;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 字典服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class SysDictionaryServiceImpl implements ISysDictionaryService {

    @Autowired
    private SysDictionaryTypeMapper sysDictionaryTypeMapper;

    /**
     * 新增字典类型
     *
     * @param dictionaryTypeWriteReqDTO 新增字典类型 DTO
     * @return Long
     */
    @Override
    public Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        // 构建查询语句
        LambdaQueryWrapper<SysDictionaryType> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.select(SysDictionaryType::getId)
                .eq(SysDictionaryType::getValue, dictionaryTypeWriteReqDTO.getValue()) // 查询语句的 value 等于 传过来的 value
                .or() // 或者
                .eq(SysDictionaryType::getTypeKey, dictionaryTypeWriteReqDTO.getTypeKey()) // 查询语句的 typeKey 等于 传过来的 typeKey
        ;
        SysDictionaryType sysDictionaryType = sysDictionaryTypeMapper.selectOne(lambdaQueryWrapper);
        if (sysDictionaryType != null) {
            log.warn("SysDictionaryServiceImpl.addType: [字典类型键或者值已存在: {} ]", sysDictionaryType);
            throw new ServiceException("字典类型键或者值已存在");
        }
        // 不存在的话直接插入
        sysDictionaryType = new SysDictionaryType();
        // 拷贝成数据库的对象
        BeanCopyUtil.copyProperties(dictionaryTypeWriteReqDTO, sysDictionaryType);
        sysDictionaryTypeMapper.insert(sysDictionaryType);
        return sysDictionaryType.getId();
    }
}
