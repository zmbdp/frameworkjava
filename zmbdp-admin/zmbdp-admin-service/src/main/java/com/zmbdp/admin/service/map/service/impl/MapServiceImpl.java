package com.zmbdp.admin.service.map.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.zmbdp.admin.api.map.constants.MapConstants;
import com.zmbdp.admin.service.map.domain.dto.SysRegionDTO;
import com.zmbdp.admin.service.map.domain.entity.SysRegion;
import com.zmbdp.admin.service.map.mapper.RegionMapper;
import com.zmbdp.admin.service.map.service.IMapService;
import com.zmbdp.common.cache.utils.CacheUtil;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.redis.service.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 地图服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class MapServiceImpl implements IMapService {

    /**
     * sys_region 的 mapper
     */
    @Autowired
    private RegionMapper regionMapper;

    /**
     * redis 服务对象
     */
    @Autowired
    private RedisService redisService;

    /**
     * 一级缓存服务对象
     */
    @Autowired
    private Cache<String, Object> caffeineCache;

    @PostConstruct
    public void initCityMap() {
        // 缓存预热
        // 直接设置到二级缓存和一级缓存中
        // 1 直接先查数据库
        List<SysRegion> cityList = regionMapper.selectAllRegion();
        // 2 在服务启动期间，缓存城市列表
        loadCityInfo(cityList);
        // 3 在服务启动期间，缓存城市归类列表
        loadCityPinyinInfo(cityList);
        // 4 在服务启动期间，缓存城市热点列表
        loadCityHotListInfo(cityList);
        log.info("缓存预热完成，一共有 {} 个城市", cityList.size());
    }

    /**
     * 缓存城市信息
     */
    private void loadCityInfo(List<SysRegion> cityList) {
        // 先对象转换城 DTO
        List<SysRegionDTO> result = BeanCopyUtil.copyListProperties(cityList, SysRegionDTO::new);
        CacheUtil.setL2Cache(redisService, MapConstants.CACHE_MAP_CITY_KEY, result, caffeineCache, 120L, TimeUnit.MINUTES);
    }

    /**
     * 初始化 A - Z 归类城市列表缓存内容
     */
    private void loadCityPinyinInfo(List<SysRegion> cityList) {
        // 先对象转换城 DTO
        List<SysRegionDTO> result = BeanCopyUtil.copyListProperties(cityList, SysRegionDTO::new);
        // 然后从 DTO 中获取拼音首字母，创建 A - Z 分类
        Map<String, List<SysRegionDTO>> map = new LinkedHashMap<>();
        for (SysRegionDTO sysRegionDTO : result) {
            String pinyin = sysRegionDTO.getPinyin();
            String key = pinyin.substring(0, 1).toUpperCase();
            // 如果 map 中不存在 key, 就创建
            if (!map.containsKey(key)) {
                List<SysRegionDTO> list = new ArrayList<>();
                list.add(sysRegionDTO);
                map.put(key, list);
            } else {
                // 如果说存在就直接加
                map.get(key).add(sysRegionDTO);
            }
        }
        // 然后再存到缓存中
        CacheUtil.setL2Cache(redisService, MapConstants.CACHE_MAP_CITY_PINYIN_KEY, map, caffeineCache, 120L, TimeUnit.MINUTES);
    }

    /**
     * 加载热门城市列表
     *
     * @param cityList
     */
    private void loadCityHotListInfo(List<SysRegion> cityList) {
        // 先获取热门城市数据
        // 2 设置 9 个热门城市
        String ids = "1,2,9,22,94,121,217,231,272";
        // 3 获取热门城市数据
        Set<Long> idList = new HashSet<>();
        for (String id : ids.split(",")) {
            idList.add(Long.valueOf(id));
        }
        List<SysRegion> sysRegionList = new ArrayList<>();
        for (SysRegion sysRegion : cityList) {
            // 如果是热门城市就存储
            if (idList.contains(sysRegion.getId())) {
                sysRegionList.add(sysRegion);
            }
        }
        List<SysRegionDTO> hotCityList = BeanCopyUtil.copyListProperties(sysRegionList, SysRegionDTO::new);
        // 4 设置缓存
        CacheUtil.setL2Cache(redisService, MapConstants.CACHE_MAP_HOT_CITY, hotCityList, caffeineCache, 120L, TimeUnit.MINUTES);
    }

    /**
     * 获取城市列表 V1 版本，从 db 中查询
     *
     * @return 城市列表
     */
    public List<SysRegionDTO> getCityListV1() {
        // 继续优化，使用看门狗分布式锁来确定只有一个线程需要查数据库
        // 先获取到一把锁
        Boolean lock = redisService.setCacheObjectIfAbsent(MapConstants.CACHE_MAP_CITY_KEY, "mapLocke", -1, TimeUnit.SECONDS); // 加锁
        // 如果说未获取到锁，那就返回空
        if (!lock) {
            return new ArrayList<>();
        }
        // 走到这里说明获取到锁了，但是还得判断一下缓存中是否有数据，如果说有的话，直接就返回
        List<SysRegionDTO> resultDTO = CacheUtil.getL2Cache(redisService, MapConstants.CACHE_MAP_CITY_KEY, new TypeReference<List<SysRegionDTO>>() {
        }, caffeineCache);
        if (resultDTO != null && !resultDTO.isEmpty()) {
            return resultDTO;
        }
        // 查数据库
        List<SysRegion> list = regionMapper.selectAllRegion();
        // 拷贝成 dto 返回
        resultDTO = BeanCopyUtil.copyListProperties(list, SysRegionDTO::new);
        // 然后再存储到缓存中
        CacheUtil.setL2Cache(redisService, MapConstants.CACHE_MAP_CITY_KEY, resultDTO, caffeineCache, 120L, TimeUnit.MINUTES);
        return resultDTO;
    }

    /**
     * 获取城市列表 V2 优化版本，从 redis 缓存中查询
     *
     * @return 城市列表
     */
    public List<SysRegionDTO> getCityListV2() {
        // 从 redis 里面拿，没有就走数据库，然后再存到 redis 里面
        List<SysRegionDTO> resultDTO = redisService.getCacheList(MapConstants.CACHE_MAP_CITY_KEY, new TypeReference<List<SysRegionDTO>>() {
        });
        if (resultDTO != null && !resultDTO.isEmpty()) {
            // 说明有数据，直接遍历返回符合要求的
            return resultDTO;
        }
        // 说明没有数据，则从数据库中查询
        resultDTO = getCityListV1();
        redisService.setCacheObject(MapConstants.CACHE_MAP_CITY_KEY, resultDTO, 120, TimeUnit.MINUTES);
        return resultDTO;
    }

    /**
     * 获取城市列表 V3 缓存优化版本，从 caffeine 缓存中查询, 查不到再查 redis, redis 还没有再从 db 查
     *
     * @return 城市列表
     */
    public List<SysRegionDTO> getCityListV3() {
        // 这里是从二级缓存和一级缓存中查
        List<SysRegionDTO> resultDTO = CacheUtil.getL2Cache(redisService, MapConstants.CACHE_MAP_CITY_KEY, new TypeReference<List<SysRegionDTO>>() {
        }, caffeineCache);
        if (resultDTO != null && !resultDTO.isEmpty()) {
            // 说明有数据，直接返回
            return resultDTO;
        }
        // 到这儿说明二级一级都没有，直接查数据库
        resultDTO = getCityListV1();
        CacheUtil.setL2Cache(redisService, MapConstants.CACHE_MAP_CITY_KEY, resultDTO, caffeineCache, 120L, TimeUnit.MINUTES);
        return resultDTO;
    }

    /**
     * 获取全量地区列表, 缓存预热方案，先预热所有的城市到缓存中，有的话直接返回了，没有再去查数据库
     *
     * @return 全量地区列表
     */
    @Override
    public List<SysRegionDTO> getCityList() {
        // 不应该请求数据库，因为此时流量大，数据库一定会挂
        List<SysRegionDTO> resultDTO = CacheUtil.getL2Cache(redisService, MapConstants.CACHE_MAP_CITY_KEY, new TypeReference<List<SysRegionDTO>>() {
        }, caffeineCache);
        if (resultDTO == null) {
            log.warn("缓存中无数据, resultDTO: {}", resultDTO);
            return getCityListV1();
        }
        return resultDTO;
    }

    /**
     * 根据城市拼音归类的查询
     *
     * @return 城市字母与城市列表的哈希
     */
    @Override
    public Map<String, List<SysRegionDTO>> getCityPylist() {
        Map<String, List<SysRegionDTO>> resultDTO = CacheUtil.getL2Cache(redisService, MapConstants.CACHE_MAP_CITY_PINYIN_KEY, new TypeReference<Map<String, List<SysRegionDTO>>>() {
        }, caffeineCache);
        return resultDTO;
    }

    /**
     * 根据父级区域 ID 获取子集区域列表
     *
     * @param parentId 父级区域 ID
     * @return 子集区域列表
     */
    @Override
    public List<SysRegionDTO> getRegionChildren(Long parentId) {
        // 先从缓存中拿取数据，然后根据 parentId 来判断是否需要返回
        // 1 入参可以参与构建缓存的 key
        String key = MapConstants.CACHE_MAP_CITY_CHILDREN_KEY + parentId;
        // 2 查缓存
        List<SysRegionDTO> resultRegions = CacheUtil.getL2Cache(redisService, key, new TypeReference<List<SysRegionDTO>>() {
        }, caffeineCache);
        if (resultRegions != null) {
            return resultRegions;
        }
        // 3 如果说没查询到，则从数据库中查询
        List<SysRegion> list = regionMapper.selectAllRegion();
        List<SysRegionDTO> result = new ArrayList<>();
        for (SysRegion sysRegion : list) {
            // 判断父节点不为空，并且父节点是符合的才返回
            if (sysRegion.getParentId() != null && sysRegion.getParentId().equals(parentId)) {
                SysRegionDTO sysRegionDTO = new SysRegionDTO();
                BeanUtils.copyProperties(sysRegion, sysRegionDTO);
                result.add(sysRegionDTO);
            }
        }
        // 4 设置缓存
        CacheUtil.setL2Cache(redisService, key, result, caffeineCache, 120L, TimeUnit.MINUTES);
        return result;
    }

    /**
     * 获取热门城市列表
     *
     * @return 城市列表
     */
    @Override
    public List<SysRegionDTO> getHotCityList() {
        // 先查一下缓存，看看有没有数据, 没有的话就去数据库找
        List<SysRegionDTO> resultDTO = CacheUtil.getL2Cache(redisService, MapConstants.CACHE_MAP_HOT_CITY, new TypeReference<List<SysRegionDTO>>() {
        }, caffeineCache);
        if (resultDTO != null && !resultDTO.isEmpty()) {
            return resultDTO;
        }
        // 2 设置 9 个热门城市
        String ids = "1,2,9,22,94,121,217,231,272";
        // 3 获取热门城市数据
        List<Long> idList = new ArrayList<>();
        resultDTO = new ArrayList<>();
        for (String id : ids.split(",")) {
            idList.add(Long.valueOf(id));
        }
        for (SysRegion sysRegion : regionMapper.selectBatchIds(idList)) {
            SysRegionDTO sysRegionDTO = new SysRegionDTO();
            BeanUtils.copyProperties(sysRegion, sysRegionDTO);
            resultDTO.add(sysRegionDTO);
        }
        // 4 设置缓存
        CacheUtil.setL2Cache(redisService, MapConstants.CACHE_MAP_HOT_CITY, resultDTO, caffeineCache, 120L, TimeUnit.MINUTES);
        return resultDTO;
    }
}
