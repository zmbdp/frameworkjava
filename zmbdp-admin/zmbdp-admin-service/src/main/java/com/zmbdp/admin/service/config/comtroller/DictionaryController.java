package com.zmbdp.admin.service.config.comtroller;

import com.zmbdp.admin.api.config.domain.dto.DictionaryDataListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeListReqDTO;
import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;
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
    @PostMapping("/add")
    public Result<Long> addType(@RequestBody @Validated DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        return Result.success(sysDictionaryService.addType(dictionaryTypeWriteReqDTO));
    }

    /**
     * 查询字典数据列表
     *
     * @param dictionaryTypeListReqDTO 字典数据列表DTO
     * @return BasePageVO
     */
    @GetMapping("/list")
    public Result<BasePageVO<DictionaryTypeVO>> listType(@Validated DictionaryTypeListReqDTO dictionaryTypeListReqDTO) {
        return Result.success(sysDictionaryService.listType(dictionaryTypeListReqDTO));
    }
}
