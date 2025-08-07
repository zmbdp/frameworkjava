package com.zmbdp.admin.service.config.service;

import com.zmbdp.admin.api.config.domain.dto.*;
import com.zmbdp.admin.api.config.domain.vo.DictionaryDataVo;
import com.zmbdp.admin.api.config.domain.vo.DictionaryTypeVO;
import com.zmbdp.common.domain.domain.vo.BasePageVO;

import java.util.List;
import java.util.Map;

/**
 * 字典服务层的接口
 *
 * @author 稚名不带撇
 */
public interface ISysDictionaryService {

    /*=============================================    前端调用    =============================================*/

    /**
     * 新增字典类型
     *
     * @param dictionaryTypeWriteReqDTO 新增字典类型 DTO
     * @return 数据库的 id
     */
    Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO);

    /**
     * 查询字典类型列表
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

    /**
     * 关键词搜索字典数据列表
     *
     * @param dictionaryDataListReqDTO 字典数据列表 DTO
     * @return 符合要求的字典数据列表数据
     */
    BasePageVO<DictionaryDataVo> listData(DictionaryDataListReqDTO dictionaryDataListReqDTO);

    /**
     * 编辑字典数据
     *
     * @param dictionaryDataEditReqDTO 编辑字典数据 DTO
     * @return 数据库的 id
     */
    Long editData(DictionaryDataEditReqDTO dictionaryDataEditReqDTO);


    /*=============================================    远程调用    =============================================*/

    /**
     * 获取某个字典类型下的所有字典数据
     *
     * @param typeKey 字典类型键
     * @return 字典数据列表
     */
    List<DictionaryDataDTO> selectDictDataByType(String typeKey);

    /**
     * 获取多个字典类型下的所有字典数据
     *
     * @param typeKeys 字典类型键列表
     * @return 字典数据列表，哈希  字典类型键 -> 字典数据列表
     */
    Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(List<String> typeKeys);

    /**
     * 根据字典数据业务主键获取字典数据对象
     *
     * @param dataKey 字典数据业务主键
     * @return 该字典数据的对象
     */
    DictionaryDataDTO getDicDataByKey(String dataKey);

    /**
     * 根据多个字典数据业务主键获取多个字典数据对象
     *
     * @param dataKeys 多个字典数据业务主键
     * @return 字典数据列表
     */
    List<DictionaryDataDTO> getDicDataByKeys(List<String> dataKeys);
}
