package com.zmbdp.admin.service.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmbdp.admin.api.config.domain.dto.*;
import com.zmbdp.admin.api.config.domain.vo.DictionaryDataVo;
import com.zmbdp.admin.api.config.domain.vo.DictionaryTypeVO;
import com.zmbdp.admin.service.config.domain.entity.SysDictionaryData;
import com.zmbdp.admin.service.config.domain.entity.SysDictionaryType;
import com.zmbdp.admin.service.config.mapper.SysDictionaryDataMapper;
import com.zmbdp.admin.service.config.mapper.SysDictionaryTypeMapper;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 字典服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class SysDictionaryServiceImpl implements ISysDictionaryService {

    /**
     * 字典类型表 mapper
     */
    @Autowired
    private SysDictionaryTypeMapper sysDictionaryTypeMapper;

    /**
     * 字典数据表 mapper
     */
    @Autowired
    private SysDictionaryDataMapper sysDictionaryDataMapper;

    /*=============================================    前端调用    =============================================*/

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
     * 查询字典类型列表
     *
     * @param dictionaryTypeListReqDTO 查询字典类型列表 DTO
     * @return 字典类型列表
     */
    @Override
    public BasePageVO<DictionaryTypeVO> listType(DictionaryTypeListReqDTO dictionaryTypeListReqDTO) {
        BasePageVO<DictionaryTypeVO> result = new BasePageVO<>();
        LambdaQueryWrapper<SysDictionaryType> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtil.isNotBlank(dictionaryTypeListReqDTO.getValue())) {
            queryWrapper.like(SysDictionaryType::getValue, dictionaryTypeListReqDTO.getValue());
        }
        if (StringUtil.isNotBlank(dictionaryTypeListReqDTO.getTypeKey())) {
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
        // 先拷贝 data 里面的数据
        List<DictionaryTypeVO> list = BeanCopyUtil.copyListProperties(page.getRecords(), DictionaryTypeVO::new);
        // 然后外面的公共数据先 set 进去
        result.setTotals((int) page.getTotal());
        result.setTotalPages((int) page.getPages());
        // 插入返回对象返回
        result.setList(list);
        return result;
    }

    /**
     * 修改字典类型
     *
     * @param dictionaryTypeWriteReqDTO 修改字典类型 DTO
     * @return 数据库的 id
     */
    @Override
    public Long editType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        // 查询数据库中是否存在
        SysDictionaryType sysDictionaryType = sysDictionaryTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictionaryType>()
                        .eq(SysDictionaryType::getTypeKey,
                                dictionaryTypeWriteReqDTO.getTypeKey())
        );
        if (sysDictionaryType == null) {
            log.warn("SysDictionaryServiceImpl.editType: [字典类型不存在: {} ]", dictionaryTypeWriteReqDTO);
            throw new ServiceException("字典类型不存在");
        }
        // 存在的话就判断字典类型的键是否存在一致的, 如果说，键不一样，但是值一样了，那么就返回字典类型已存在,
        // 因为键不允许修改，添加的时候也不允许重复，所以说键肯定不会相同
        if (sysDictionaryTypeMapper.selectOne(new LambdaQueryWrapper<SysDictionaryType>()
                // 查出不一样的键
                .ne(SysDictionaryType::getTypeKey, dictionaryTypeWriteReqDTO.getTypeKey())
                // 查出一样的值
                .eq(SysDictionaryType::getValue, dictionaryTypeWriteReqDTO.getValue())
        ) != null) {
            log.warn("SysDictionaryServiceImpl.editType: [字典类型值已存在: {} ]", dictionaryTypeWriteReqDTO);
            throw new ServiceException("字典类型的值已存在");
        }
        // 说明是符合要求的，直接改就行了
        sysDictionaryType.setValue(dictionaryTypeWriteReqDTO.getValue());
        if (StringUtil.isNotBlank(dictionaryTypeWriteReqDTO.getRemark())) {
            sysDictionaryType.setRemark(dictionaryTypeWriteReqDTO.getRemark());
        }
        sysDictionaryTypeMapper.updateById(sysDictionaryType);
        return sysDictionaryType.getId();
    }

    /**
     * 新增字典数据
     *
     * @param dictionaryDataAddReqDTO 新增字典数据 DTO
     * @return 数据库的 id
     */
    @Override
    public Long addData(DictionaryDataAddReqDTO dictionaryDataAddReqDTO) {
        // 先查 字典类型 key 是否存在，只有存在才能加
        if (
                sysDictionaryTypeMapper.selectOne(
                        new LambdaQueryWrapper<SysDictionaryType>()
                                // 看看 字典类型的 key 是否存在，不存在的话就不能加
                                .eq(SysDictionaryType::getTypeKey, dictionaryDataAddReqDTO.getTypeKey()
                                )) == null
        ) {
            log.warn("SysDictionaryServiceImpl.addData: [字典类型不存在: {} ]", dictionaryDataAddReqDTO);
            throw new ServiceException("字典类型不存在");
        }
        // 再看字典数据的 key 和 value 是否重复，不重复才能加
        SysDictionaryData sysDictionaryData = sysDictionaryDataMapper.selectOne(
                new LambdaQueryWrapper<SysDictionaryData>()
                        .eq(SysDictionaryData::getDataKey, dictionaryDataAddReqDTO.getDataKey())
                        .or()
                        .eq(SysDictionaryData::getValue, dictionaryDataAddReqDTO.getValue())
        );
        if (sysDictionaryData != null) {
            log.warn("SysDictionaryServiceImpl.addData: [字典数据键或值已存在: {} ]", dictionaryDataAddReqDTO);
            throw new ServiceException("字典数据键或值已存在");
        }
        // 说明没查到，那就插入
        sysDictionaryData = new SysDictionaryData();
        BeanCopyUtil.copyProperties(dictionaryDataAddReqDTO, sysDictionaryData);
        sysDictionaryDataMapper.insert(sysDictionaryData);
        return sysDictionaryData.getId();
    }

    /**
     * 关键词搜索字典数据列表
     *
     * @param dictionaryDataListReqDTO 字典数据列表 DTO
     * @return 符合要求的字典数据列表数据
     */
    @Override
    public BasePageVO<DictionaryDataVo> listData(DictionaryDataListReqDTO dictionaryDataListReqDTO) {
        BasePageVO<DictionaryDataVo> result = new BasePageVO<>();
        LambdaQueryWrapper<SysDictionaryData> queryWrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        queryWrapper.eq(SysDictionaryData::getTypeKey, dictionaryDataListReqDTO.getTypeKey());
        if (StringUtil.isNotBlank(dictionaryDataListReqDTO.getValue())) {
            queryWrapper.like(SysDictionaryData::getValue, dictionaryDataListReqDTO.getValue());
        }
        // 根据排序字段升序排序
        queryWrapper.orderByAsc(SysDictionaryData::getSort);
        // 如果说排序字段一样，就根据 id 升序排序
        queryWrapper.orderByAsc(SysDictionaryData::getId);
        // 开始查询数据
        Page<SysDictionaryData> page = sysDictionaryDataMapper.selectPage(
                new Page<>(dictionaryDataListReqDTO.getPageNo().longValue(), dictionaryDataListReqDTO.getPageSize().longValue()),
                queryWrapper
        );
        // 然后再构建返回参数
        List<DictionaryDataVo> list = BeanCopyUtil.copyListProperties(page.getRecords(), DictionaryDataVo::new);
        result.setTotals((int) page.getTotal());
        result.setTotalPages((int) page.getPages());
        result.setList(list);
        return result;
    }

    /**
     * 编辑字典数据
     *
     * @param dictionaryDataEditReqDTO 编辑字典数据 DTO
     * @return 数据库的 id
     */
    @Override
    public Long editData(DictionaryDataEditReqDTO dictionaryDataEditReqDTO) {
        // 根据字典数据 key 来查
        // 先判断是否存在
        SysDictionaryData sysDictionaryData = sysDictionaryDataMapper.selectOne(
                new LambdaQueryWrapper<SysDictionaryData>()
                        .eq(SysDictionaryData::getDataKey, dictionaryDataEditReqDTO.getDataKey())
        );
        // 如果说不存在就抛异常
        if (sysDictionaryData == null) {
            log.warn("SysDictionaryServiceImpl.editData: [字典数据不存在: {} ]", dictionaryDataEditReqDTO);
            throw new ServiceException("字典数据不存在");
        }
        // 然后再看字典数据的 value 是否已经存在
        if (sysDictionaryDataMapper.selectOne(new LambdaQueryWrapper<SysDictionaryData>()
                // 如果说字典数据 key 不一样的情况下，value 一样了，就说明字典数据 value 已经存在了，就抛异常
                .ne(SysDictionaryData::getDataKey, dictionaryDataEditReqDTO.getDataKey())
                .eq(SysDictionaryData::getValue, dictionaryDataEditReqDTO.getValue())) != null
        ) {
            log.warn("SysDictionaryServiceImpl.editData: [字典数据值已存在: {} ]", dictionaryDataEditReqDTO);
            throw new ServiceException("字典数据值已存在");
        }
        // 说明符合要求，可以修改，然后构建数据，直接执行就行了
        sysDictionaryData.setValue(dictionaryDataEditReqDTO.getValue());
        if (dictionaryDataEditReqDTO.getSort() != null) {
            sysDictionaryData.setSort(dictionaryDataEditReqDTO.getSort());
        }
        if (StringUtil.isNotBlank(dictionaryDataEditReqDTO.getRemark())) {
            sysDictionaryData.setRemark(dictionaryDataEditReqDTO.getRemark());
        }
        sysDictionaryDataMapper.updateById(sysDictionaryData);
        return sysDictionaryData.getId();
    }

    /*=============================================    远程调用    =============================================*/

    /**
     * 获取某个字典类型下的所有字典数据
     *
     * @param typeKey 字典类型键
     * @return 字典数据列表
     */
    @Override
    public List<DictionaryDataDTO> selectDictDataByType(String typeKey) {
        // 先获取到表里符合要求的数据
        List<SysDictionaryData> list = sysDictionaryDataMapper.selectList(new LambdaQueryWrapper<SysDictionaryData>()
                .eq(SysDictionaryData::getTypeKey, typeKey)
        );
        // 然后直接 BeanCopy 转换
        List<DictionaryDataDTO> result = BeanCopyUtil.copyListProperties(list, DictionaryDataDTO::new);
        return result;
    }

    /**
     * 获取多个字典类型下的所有字典数据
     *
     * @param typeKeys 字典类型键列表
     * @return 字典数据列表，哈希  字典类型键 -> 字典数据列表
     */
    @Override
    public Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(List<String> typeKeys) {
        if (typeKeys == null || typeKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        // 根据传过来的字典类型 key 获取数据
        List<SysDictionaryData> list = sysDictionaryDataMapper.selectList(new LambdaQueryWrapper<SysDictionaryData>()
                .in(SysDictionaryData::getTypeKey, typeKeys)
        );
        List<DictionaryDataDTO> dtoList = BeanCopyUtil.copyListProperties(list, DictionaryDataDTO::new);
        Map<String, List<DictionaryDataDTO>> result = new LinkedHashMap<>();
        // 然后再循环分组
        for (DictionaryDataDTO dictionaryDataDTO : dtoList) {
            // 先拿到字典类型 key
            String typeKey = dictionaryDataDTO.getTypeKey();
            if (!result.containsKey(typeKey)) {
                result.put(typeKey, new ArrayList<>());
            }
            result.get(typeKey).add(dictionaryDataDTO);
        }
        return result;
    }

    /**
     * 根据字典数据业务主键获取字典数据对象
     *
     * @param dataKey 字典数据业务主键
     * @return 该字典数据的对象
     */
    @Override
    public DictionaryDataDTO getDicDataByKey(String dataKey) {
        // 根据字典数据业务主键获取字典数据对象
        SysDictionaryData dictionaryData = sysDictionaryDataMapper.selectOne(new LambdaQueryWrapper<SysDictionaryData>()
                .eq(SysDictionaryData::getDataKey, dataKey)
        );
        // 对象转换
        DictionaryDataDTO result = new DictionaryDataDTO();
        BeanCopyUtil.copyProperties(dictionaryData, result);
        return result;
    }

    /**
     * 根据多个字典数据业务主键获取多个字典数据对象
     *
     * @param dataKeys 多个字典数据业务主键
     * @return 字典数据列表
     */
    @Override
    public List<DictionaryDataDTO> getDicDataByKeys(List<String> dataKeys) {
        if (dataKeys == null || dataKeys.isEmpty()) {
            return Collections.emptyList();
        }
        // 获取符合要求的所有数据
        List<SysDictionaryData> list = sysDictionaryDataMapper.selectList(new LambdaQueryWrapper<SysDictionaryData>()
                .in(SysDictionaryData::getDataKey, dataKeys)
        );
        // 然后直接转换对象
        List<DictionaryDataDTO> result = BeanCopyUtil.copyListProperties(list, DictionaryDataDTO::new);
        return result;
    }

}
