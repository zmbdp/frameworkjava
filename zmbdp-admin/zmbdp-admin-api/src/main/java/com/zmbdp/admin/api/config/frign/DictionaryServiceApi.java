package com.zmbdp.admin.api.config.frign;


import com.zmbdp.admin.api.config.domain.dto.DictionaryDataDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 字典服务远程调用 Api
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "dictionaryServiceApi", name = "zmbdp-admin-service", path = "/dictionary")
public interface DictionaryServiceApi {
    /**
     * 获取某个字典类型下的所有字典数据
     *
     * @param typeKey 字典类型键
     * @return 字典数据列表
     */
    @GetMapping("/type")
    List<DictionaryDataDTO> selectDictDataByType(@RequestParam("typeKey") String typeKey);

    /**
     * 获取多个字典类型下的所有字典数据
     *
     * @param typeKeys 字典类型键列表
     * @return 字典数据列表，哈希  字典类型键 -> 字典数据列表
     */
    @PostMapping("/types")
    Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(@RequestBody List<String> typeKeys);

    /**
     * 根据字典数据业务主键获取字典数据对象
     *
     * @param dataKey 字典数据业务主键
     * @return 该字典数据的对象
     */
    @GetMapping("/key")
    DictionaryDataDTO getDicDataByKey(@RequestParam("dataKey") String dataKey);

    /**
     * 根据多个字典数据业务主键获取多个字典数据对象
     *
     * @param dataKeys 多个字典数据业务主键
     * @return 字典数据列表
     */
    @PostMapping("/keys")
    List<DictionaryDataDTO> getDicDataByKeys(@RequestBody List<String> dataKeys);
}