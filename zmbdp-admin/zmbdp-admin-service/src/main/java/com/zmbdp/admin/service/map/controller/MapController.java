package com.zmbdp.admin.service.map.controller;

import com.zmbdp.admin.api.map.domain.dto.LocationReqDTO;
import com.zmbdp.admin.api.map.domain.dto.PlaceSearchReqDTO;
import com.zmbdp.admin.api.map.domain.vo.RegionCityVO;
import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.admin.api.map.domain.vo.SearchPoiVO;
import com.zmbdp.admin.api.map.feign.MapServiceApi;
import com.zmbdp.admin.service.map.domain.dto.RegionCityDTO;
import com.zmbdp.admin.service.map.domain.dto.SearchPoiDTO;
import com.zmbdp.admin.service.map.domain.dto.SysRegionDTO;
import com.zmbdp.admin.service.map.service.IMapService;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.dto.BasePageDTO;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图服务
 *
 * @author 稚名不带撇
 */
@Slf4j
@RestController
@RequestMapping("/map")
public class MapController implements MapServiceApi {

    /**
     * 地图服务
     */
    @Autowired
    private IMapService mapService;

    /**
     * 获取城市列表
     *
     * @return 所有的城市列表
     */
    @Override
    public Result<List<RegionVO>> getCityList() {
        // 先用 dto 接收
        List<SysRegionDTO> cityListDTO = mapService.getCityList();
        // 再 copy 成 vo (要深拷贝，里面有复杂泛型)
        List<RegionVO> result = BeanCopyUtil.copyListProperties(cityListDTO, RegionVO::new);
        return Result.success(result);
    }

    /**
     * 根据城市拼音归类的查询
     *
     * @return 城市字母与城市列表的哈希
     */
    @Override
    public Result<Map<String, List<RegionVO>>> getCityPyList() {
        // 先用 dto 接收
        Map<String, List<SysRegionDTO>> pinyinList = mapService.getCityPylist();
        if (pinyinList == null) {
            log.error("获取城市列表失败");
            return Result.success();
        }
        // 再 copy 成 vo
        Map<String, List<RegionVO>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<SysRegionDTO>> entry : pinyinList.entrySet()) {
            result.put(entry.getKey(), BeanCopyUtil.copyListProperties(entry.getValue(), RegionVO.class));
        }
        return Result.success(result);
    }

    /**
     * 根据父级区域 ID 获取子集区域列表
     *
     * @param parentId 父级区域 ID
     * @return 子集区域列表
     */
    @Override
    public Result<List<RegionVO>> regionChildren(Long parentId) {
        List<SysRegionDTO> regionDTOS = mapService.getRegionChildren(parentId);
        if (regionDTOS == null) {
            log.error("获取子集区域列表失败");
            return Result.success();
        }
        List<RegionVO> result = BeanCopyUtil.copyListProperties(regionDTOS, RegionVO.class);
        return Result.success(result);
    }

    /**
     * 获取热门城市列表
     *
     * @return 城市列表
     */
    @Override
    public Result<List<RegionVO>> getHotCityList() {
        List<SysRegionDTO> hotCityListDTO = mapService.getHotCityList();
        if (hotCityListDTO == null) {
            log.error("获取热门城市列表失败");
            return Result.success();
        }
        List<RegionVO> result = BeanCopyUtil.copyListProperties(hotCityListDTO, RegionVO.class);
        return Result.success(result);
    }

    /**
     * 根据关键词搜索
     *
     * @param placeSearchReqDTO 搜索条件
     * @return 搜索结果
     */
    @Override
    public Result<BasePageVO<SearchPoiVO>> searchSuggestOnMap(@Validated PlaceSearchReqDTO placeSearchReqDTO) {
        BasePageDTO<SearchPoiDTO> basePageReqDTO = mapService.searchSuggestOnMap(placeSearchReqDTO);
        if (basePageReqDTO == null) {
            log.error("根据关键词搜索失败");
            return Result.success();
        }
        BasePageVO<SearchPoiVO> result = new BasePageVO<>();
        BeanCopyUtil.copyProperties(basePageReqDTO, result);
        return Result.success(result);
    }

    /**
     * 根据经纬度获取城市的信息
     *
     * @param locationReqDTO 经纬度信息
     * @return 城市信息
     */
    @Override
    public Result<RegionCityVO> locateCityByLocation(@Validated LocationReqDTO locationReqDTO) {
        RegionCityDTO regionCityDTO = mapService.getCityByLocation(locationReqDTO);
        if (regionCityDTO == null) {
            log.error("根据经纬度获取城市信息失败");
            return Result.success();
        }
        RegionCityVO result = new RegionCityVO();
        BeanCopyUtil.copyProperties(regionCityDTO, result);
        return Result.success(result);
    }
}