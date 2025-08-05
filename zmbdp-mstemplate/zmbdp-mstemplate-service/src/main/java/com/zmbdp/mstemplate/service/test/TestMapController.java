package com.zmbdp.mstemplate.service.test;

import com.zmbdp.admin.api.map.domain.dto.LocationReqDTO;
import com.zmbdp.admin.api.map.domain.dto.PlaceSearchReqDTO;
import com.zmbdp.admin.api.map.domain.vo.RegionCityVo;
import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.admin.api.map.domain.vo.SearchPoiVo;
import com.zmbdp.admin.api.map.feign.MapServiceApi;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test/map")
public class TestMapController {

    @Autowired
    private MapServiceApi mapServiceApi;

    /**
     * 获取全量城市列表
     *
     * @return 城市列表
     */
    @RequestMapping("/city_list")
    public Result<List<RegionVO>> city_list() {
        return mapServiceApi.getCityList();
    }

    /**
     * 根据城市拼音归类的查询
     *
     * @return 城市字母与城市列表的哈希
     */
    @RequestMapping("/city_pinyin_list")
    public Result<Map<String, List<RegionVO>>> city_pinyin_list() {
        return mapServiceApi.getCityPyList();
    }

    /**
     * 根据父级区域 ID 获取子集区域列表
     *
     * @param parentId 父级区域 ID
     * @return 子集区域列表
     */
    @RequestMapping("/region_children_list")
    public Result<List<RegionVO>> region_children_list(Long parentId) {
        return mapServiceApi.regionChildren(parentId);
    }

    /**
     * 获取热门城市列表
     *
     * @return 城市列表
     */
    @GetMapping("/city_hot_list")
    Result<List<RegionVO>> getHotCityList() {
        return mapServiceApi.getHotCityList();
    }

    /**
     * 根据关键词搜索
     *
     * @param placeSearchReqDTO 搜索条件
     * @return 搜索结果
     */
    @PostMapping("/search")
    Result<BasePageVO<SearchPoiVo>> searchSuggestOnMap(@RequestBody PlaceSearchReqDTO placeSearchReqDTO) {
        return mapServiceApi.searchSuggestOnMap(placeSearchReqDTO);
    }

    /**
     * 根据经纬度获取城市的信息
     *
     * @param locationReqDTO 经纬度信息
     * @return 城市信息
     */
    @PostMapping("/locate_city_by_location")
    Result<RegionCityVo> locateCityByLocation(@RequestBody LocationReqDTO locationReqDTO) {
        return mapServiceApi.locateCityByLocation(locationReqDTO);
    }
}
