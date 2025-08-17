package com.zmbdp.admin.service.map.service.impl;

import com.zmbdp.admin.api.map.constants.MapConstants;
import com.zmbdp.admin.service.map.domain.dto.*;
import com.zmbdp.admin.service.map.domain.entity.SysRegion;
import com.zmbdp.admin.service.map.service.IMapProvider;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 地图服务的实现类
 *
 * @author 稚名不带撇
 */
@Data
@Slf4j
@Service
@RefreshScope
@ConditionalOnProperty(value = "map.type", havingValue = "qqmap")
public class QQMapServiceImpl implements IMapProvider {

    /**
     * 腾讯位置服务域名
     */
    @Value("${qqmap.apiServer:https://apis.map.qq.com}")
    private String apiServer;

    /**
     * 调用腾讯位置服务的秘钥
     */
    @Value("${qqmap.key:IWFBZ-FJPW7-JADXW-PURZL-2AJPQ-A5F46}")
    private String key;

    /**
     * RestTemplate
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 根据关键词搜索地点
     * @param suggestSearchDTO 搜索条件
     * @return 搜索结果
     */
    @Override
    public PoiListDTO searchQQMapPlaceByRegion(SuggestSearchDTO suggestSearchDTO) {
        // 拼接请求地址
        String url = String.format(
                apiServer + MapConstants.QQMAP_API_PLACE_SUGGESTION +
                        "?key=%s&region=%s&region_fix=%s&page_index=%s&page_size=%s&keyword=%s",
                key, suggestSearchDTO.getId(), suggestSearchDTO.getRegionFix(),
                suggestSearchDTO.getPageIndex(), suggestSearchDTO.getPageSize(),suggestSearchDTO.getKeyword()
        );
        // 使用 RestTemplate 发送请求
        ResponseEntity<PoiListDTO> response = restTemplate.getForEntity(url, PoiListDTO.class);
        // 判断结果
        if (!response.getStatusCode().is2xxSuccessful()) {
            // 假设说这个响应码不是以 2 开头的，那么一定是错误的
            log.error("[QQMapServiceImpl.searchQQMapPlaceByRegion 获取关键词查询结果服务]请求失败，返回结果: {}", response.getBody());
            throw new ServiceException(ResultCode.QQMAP_QUERY_FAILED);
        }
        // 然后直接返回 body 里面的值
        return response.getBody();
    }

    /**
     * 根据经纬度来获取区域信息
     *
     * @param locationDTO 经纬度
     * @return 区域信息
     */
    @Override
    public GeoResultDTO getQQMapDistrictByLonLat(LocationDTO locationDTO) {
        // 创建 url
        String url = String.format(apiServer + MapConstants.QQMAP_GEOCODER +
                        "?key=%s&location=%s",
                key, locationDTO.formatInfo()
        );
        // 使用 RestTemplate 发送请求
        ResponseEntity<GeoResultDTO> response =  restTemplate.getForEntity(url, GeoResultDTO.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("[QQMapServiceImpl.getQQMapDistrictByLonLat 根据经纬度来获取区域信息服务]请求失败，返回结果: {}", response.getBody());
            throw new ServiceException(ResultCode.QQMAP_QUERY_FAILED);
        }
        return response.getBody();
    }
}
