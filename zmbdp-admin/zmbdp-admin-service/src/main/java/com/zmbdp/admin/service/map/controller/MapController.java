package com.zmbdp.admin.service.map.controller;

import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.admin.api.map.feign.MapServiceApi;
import com.zmbdp.admin.service.map.domain.dto.SuggestSearchDTO;
import com.zmbdp.admin.service.map.domain.dto.SysRegionDTO;
import com.zmbdp.admin.service.map.service.IMapService;
import com.zmbdp.admin.service.map.service.impl.QQMapServiceImpl;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
        // 再 copy 成 vo
        List<RegionVO> resultVO = BeanCopyUtil.copyListProperties(cityListDTO, RegionVO::new);
        return Result.success(resultVO);
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
        // 再 copy 成 vo
        Map<String, List<RegionVO>> resultVO = new LinkedHashMap<>();
        for (Map.Entry<String, List<SysRegionDTO>> entry : pinyinList.entrySet()) {
            resultVO.put(entry.getKey(), BeanCopyUtil.copyListProperties(entry.getValue(), RegionVO::new));
        }
        return Result.success(resultVO);
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
        List<RegionVO> regionVO = BeanCopyUtil.copyListProperties(regionDTOS, RegionVO::new);
        return Result.success(regionVO);
    }

    /**
     * 获取热门城市列表
     *
     * @return 城市列表
     */
    @Override
    public Result<List<RegionVO>> getHotCityList() {
        List<SysRegionDTO> hotCityListDTO = mapService.getHotCityList();
        List<RegionVO> regionVO = BeanCopyUtil.copyListProperties(hotCityListDTO, RegionVO::new);
        return Result.success(regionVO);
    }
}
