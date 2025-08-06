package com.zmbdp.admin.service.config.comtroller;

import com.zmbdp.admin.api.config.domain.dto.DictionaryDataAddReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryDataListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;
import com.zmbdp.admin.api.config.domain.vo.DictionaryDataVo;
import com.zmbdp.admin.api.config.domain.vo.DictionaryTypeVO;
import com.zmbdp.admin.api.config.frign.DictionaryFeignClient;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 字典服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/dictionary_type")
public class DictionaryController implements DictionaryFeignClient {
    @Autowired
    private ISysDictionaryService sysDictionaryService;

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
     * 查询字典数据列表
     *
     * @param dictionaryTypeListReqDTO 字典数据列表 DTO
     * @return 符合条件的字典类型列表
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
}
