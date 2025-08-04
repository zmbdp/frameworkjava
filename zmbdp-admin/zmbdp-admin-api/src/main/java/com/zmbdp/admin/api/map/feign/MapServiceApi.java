package com.zmbdp.admin.api.map.feign;

import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.common.domain.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 地图服务
 *
 * @author 稚名不带撇
 */
@FeignClient(name = "zmbdp-admin-service", path = "/map")
public interface MapServiceApi {

    /**
     * 获取全量城市列表
     *
     * @return 城市列表
     */
    @GetMapping("/city_list")
    Result<List<RegionVO>> getCityList();

    /**
     * 根据城市拼音归类的查询
     *
     * @return 城市字母与城市列表的哈希
     */
    @GetMapping("/city_pinyin_list")
    Result<Map<String, List<RegionVO>>> getCityPyList();

    /**
     * 根据父级区域 ID 获取子集区域列表
     *
     * @param parentId 父级区域 ID
     * @return 子集区域列表
     */
    @GetMapping("/map/region_children_list")
    Result<List<RegionVO>> regionChildren(@RequestParam("parentId") Long parentId);

}
