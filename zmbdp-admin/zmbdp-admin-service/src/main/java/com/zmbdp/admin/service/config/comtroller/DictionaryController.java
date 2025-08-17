package com.zmbdp.admin.service.config.comtroller;

import com.zmbdp.admin.api.config.domain.dto.*;
import com.zmbdp.admin.api.config.domain.vo.DictionaryDataVo;
import com.zmbdp.admin.api.config.domain.vo.DictionaryTypeVO;
import com.zmbdp.admin.api.config.frign.DictionaryServiceApi;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/dictionary")
public class DictionaryController implements DictionaryServiceApi {

    /**
     * 字典服务
     */
    @Autowired
    private ISysDictionaryService sysDictionaryService;

    /*=============================================    前端调用    =============================================*/

    /**
     * 新增字典类型
     *
     * @param dictionaryTypeWriteReqDTO 新增字典类型 DTO
     * @return 数据库的 id
     */
    @PostMapping("/addType")
    public Result<Long> addType(@RequestBody @Validated DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        return Result.success(sysDictionaryService.addType(dictionaryTypeWriteReqDTO));
    }

    /**
     * 查询字典类型列表
     *
     * @param dictionaryTypeListReqDTO 查询字典类型列表 DTO
     * @return 字典类型列表
     */
    @GetMapping("/listType")
    public Result<BasePageVO<DictionaryTypeVO>> listType(@Validated DictionaryTypeListReqDTO dictionaryTypeListReqDTO) {
        return Result.success(sysDictionaryService.listType(dictionaryTypeListReqDTO));
    }

    /**
     * 修改字典类型
     *
     * @param dictionaryTypeWriteReqDTO 修改字典类型 DTO
     * @return 数据库的 id
     */
    @PostMapping("/editType")
    public Result<Long> editType(@RequestBody @Validated DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        return Result.success(sysDictionaryService.editType(dictionaryTypeWriteReqDTO));
    }

    /**
     * 新增字典数据
     *
     * @param dictionaryDataAddReqDTO 新增字典数据 DTO
     * @return 数据库的 id
     */
    @PostMapping("/addData")
    public Result<Long> addData(@RequestBody @Validated DictionaryDataAddReqDTO dictionaryDataAddReqDTO) {
        return Result.success(sysDictionaryService.addData(dictionaryDataAddReqDTO));
    }

    /**
     * 关键词搜索字典数据列表
     *
     * @param dictionaryDataListReqDTO 字典数据列表 DTO
     * @return 符合要求的字典数据列表数据
     */
    @GetMapping("/listData")
    public Result<BasePageVO<DictionaryDataVo>> listData(@Validated DictionaryDataListReqDTO dictionaryDataListReqDTO) {
        return Result.success(sysDictionaryService.listData(dictionaryDataListReqDTO));
    }

    /**
     * 编辑字典数据
     *
     * @param dictionaryDataEditReqDTO 编辑字典数据 DTO
     * @return 数据库的 id
     */
    @PostMapping("/editData")
    public Result<Long> editData(@RequestBody @Validated DictionaryDataEditReqDTO dictionaryDataEditReqDTO) {
        return Result.success(sysDictionaryService.editData(dictionaryDataEditReqDTO));
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
        return sysDictionaryService.selectDictDataByType(typeKey);
    }

    /**
     * 获取多个字典类型下的所有字典数据
     *
     * @param typeKeys 字典类型键列表
     * @return 字典数据列表，哈希  字典类型键 -> 字典数据列表
     */
    @Override
    public Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(List<String> typeKeys) {
        return sysDictionaryService.selectDictDataByTypes(typeKeys);
    }

    /**
     * 根据字典数据业务主键获取字典数据对象
     *
     * @param dataKey 字典数据业务主键
     * @return 该字典数据的对象
     */
    @Override
    public DictionaryDataDTO getDicDataByKey(String dataKey) {
        return sysDictionaryService.getDicDataByKey(dataKey);
    }

    /**
     * 根据多个字典数据业务主键获取多个字典数据对象
     *
     * @param dataKeys 多个字典数据业务主键
     * @return 字典数据列表
     */
    @Override
    public List<DictionaryDataDTO> getDicDataByKeys(List<String> dataKeys) {
        return sysDictionaryService.getDicDataByKeys(dataKeys);
    }
}
