package com.zmbdp.admin.service.config.service;

import com.zmbdp.admin.api.config.domain.dto.DictionaryDataAddReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryDataListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;
import com.zmbdp.admin.api.config.domain.vo.DictionaryTypeVO;
import com.zmbdp.common.domain.domain.vo.BasePageVO;

/**
 * 字典服务层的接口
 *
 * @author 稚名不带撇
 */
public interface ISysDictionaryService {

    /**
     * 新增字典类型
     *
     * @param dictionaryTypeWriteReqDTO 新增字典类型 DTO
     * @return 数据库的 id
     */
    Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO);

    /**
     * 查询字典数据列表
     *
     * @param dictionaryTypeListReqDTO 查询字典类型列表 DTO
     * @return 字典类型列表
     */
    BasePageVO<DictionaryTypeVO> listType(DictionaryTypeListReqDTO dictionaryTypeListReqDTO);

    /**
     * 修改字典类型
     *
     * @param dictionaryTypeWriteReqDTO 修改字典类型 DTO
     * @return 数据库的 id
     */
    Long editType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO);

    /**
     * 新增字典数据
     *
     * @param dictionaryDataAddReqDTO 新增字典数据 DTO
     * @return 数据库的 id
     */
    Long addData(DictionaryDataAddReqDTO dictionaryDataAddReqDTO);
}
