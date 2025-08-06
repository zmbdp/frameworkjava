package com.zmbdp.admin.service.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;
import com.zmbdp.admin.api.config.domain.vo.DictionaryTypeVO;
import com.zmbdp.admin.service.config.domain.entity.SysDictionaryType;
import com.zmbdp.admin.service.config.mapper.SysDictionaryTypeMapper;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * @return 数据库的 id
     */
    @Override
    public Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        // 构建查询语句
        LambdaQueryWrapper<SysDictionaryType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(SysDictionaryType::getId)
                .eq(SysDictionaryType::getValue, dictionaryTypeWriteReqDTO.getValue()) // 查询语句的 value 等于 传过来的 value
                .or() // 或者
                .eq(SysDictionaryType::getTypeKey, dictionaryTypeWriteReqDTO.getTypeKey()) // 查询语句的 typeKey 等于 传过来的 typeKey
        ;
        SysDictionaryType sysDictionaryType = sysDictionaryTypeMapper.selectOne(queryWrapper);
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

    /**
     * 查询字典数据列表
     *
     * @param dictionaryTypeListReqDTO 查询字典类型列表 DTO
     * @return 字典类型列表
     */
    @Override
    public BasePageVO<DictionaryTypeVO> listType(DictionaryTypeListReqDTO dictionaryTypeListReqDTO) {
        BasePageVO<DictionaryTypeVO> result = new BasePageVO<>();
        LambdaQueryWrapper<SysDictionaryType> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dictionaryTypeListReqDTO.getValue())) {
            queryWrapper.like(SysDictionaryType::getValue, dictionaryTypeListReqDTO.getValue());
        }
        if (StringUtils.isNotBlank(dictionaryTypeListReqDTO.getTypeKey())) {
            queryWrapper.eq(SysDictionaryType::getTypeKey, dictionaryTypeListReqDTO.getTypeKey());
        }
        Page<SysDictionaryType> page = sysDictionaryTypeMapper.selectPage(
                new Page<>(
                        // 传入第几页一页几条，就会查询记录并翻页
                        dictionaryTypeListReqDTO.getPageNo().longValue(),
                        dictionaryTypeListReqDTO.getPageSize().longValue()),
                queryWrapper // 查询语句
        );
        // 类型转换成返回的数据
        // 外面的公共数据先 set 进去
        result.setTotals(Integer.parseInt(String.valueOf(page.getTotal())));
        result.setTotalPages(Integer.parseInt(String.valueOf(page.getPages())));
        // 然后拷贝 data 里面的数据
        List<DictionaryTypeVO> list = BeanCopyUtil.copyListProperties(page.getRecords(), DictionaryTypeVO::new);
        // 插入返回对象返回
        result.setList(list);
        return result;
    }
}
