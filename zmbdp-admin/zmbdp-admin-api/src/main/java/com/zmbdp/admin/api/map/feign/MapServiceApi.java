package com.zmbdp.admin.api.map.feign;

import com.zmbdp.admin.api.map.domain.dto.LocationReqDTO;
import com.zmbdp.admin.api.map.domain.dto.PlaceSearchReqDTO;
import com.zmbdp.admin.api.map.domain.vo.RegionCityVo;
import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.admin.api.map.domain.vo.SearchPoiVo;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 地图服务远程调用 Api
 *
 * @author 稚名不带撇
 */
@FeignClient(contextId = "mapServiceApi", name = "zmbdp-admin-service", path = "/map")
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
    @GetMapping("/region_children_list")
    Result<List<RegionVO>> regionChildren(@RequestParam("parentId") Long parentId);

    /**
     * 获取热门城市列表
     *
     * @return 城市列表
     */
    @GetMapping("/city_hot_list")
    Result<List<RegionVO>> getHotCityList();

    /**
     * 根据关键词搜索
     *
     * @param placeSearchReqDTO 搜索条件
     * @return 搜索结果
     */
    @PostMapping("/search")
    Result<BasePageVO<SearchPoiVo>> searchSuggestOnMap(@RequestBody PlaceSearchReqDTO placeSearchReqDTO);

    /**
     * 根据经纬度获取城市的信息
     *
     * @param locationReqDTO 经纬度信息
     * @return 城市信息
     */
    @PostMapping("/locate_city_by_location")
    Result<RegionCityVo> locateCityByLocation(@RequestBody LocationReqDTO locationReqDTO);
}
