package com.zmbdp.admin.service.map.service;

import com.zmbdp.admin.api.map.domain.dto.LocationReqDTO;
import com.zmbdp.admin.api.map.domain.dto.PlaceSearchReqDTO;
import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.admin.service.map.domain.dto.RegionCityDTO;
import com.zmbdp.admin.service.map.domain.dto.SearchPoiDTO;
import com.zmbdp.admin.service.map.domain.dto.SysRegionDTO;
import com.zmbdp.common.core.domain.dto.BasePageDTO;

import java.util.List;
import java.util.Map;

/**
 * 地图服务 service 层接口
 *
 * @author 稚名不带撇
 */
public interface IMapService {

    /**
     * 获取城市列表
     *
     * @return 城市列表
     */
    List<SysRegionDTO> getCityList();

    /**
     * 根据城市拼音归类的查询
     *
     * @return 城市字母与城市列表的哈希
     */
    Map<String, List<SysRegionDTO>> getCityPylist();

    /**
     * 根据父级区域 ID 获取子集区域列表
     *
     * @param parentId 父级区域 ID
     * @return 子集区域列表
     */
    List<SysRegionDTO> getRegionChildren(Long parentId);

    /**
     * 获取热门城市列表
     *
     * @return 城市列表
     */
    List<SysRegionDTO> getHotCityList();

    /**
     * 根据关键词搜索
     *
     * @param placeSearchReqDTO 搜索条件
     * @return 搜索结果
     */
    BasePageDTO<SearchPoiDTO> searchSuggestOnMap(PlaceSearchReqDTO placeSearchReqDTO);


    /**
     * 根据经纬度获取城市的信息
     *
     * @param locationReqDTO 经纬度信息
     * @return 城市信息
     */
    RegionCityDTO getCityByLocation(LocationReqDTO locationReqDTO);
}
