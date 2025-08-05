package com.zmbdp.admin.service.config.service;

import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;

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
     * @return Long
     */
    Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO);
}
