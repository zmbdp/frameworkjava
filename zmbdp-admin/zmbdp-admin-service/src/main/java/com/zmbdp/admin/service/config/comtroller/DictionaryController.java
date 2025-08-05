package com.zmbdp.admin.service.config.comtroller;

import com.zmbdp.admin.api.config.domain.dto.DictionaryTypeWriteReqDTO;
import com.zmbdp.admin.api.config.frign.DictionaryFeignClient;
import com.zmbdp.admin.service.config.service.ISysDictionaryService;
import com.zmbdp.common.domain.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
