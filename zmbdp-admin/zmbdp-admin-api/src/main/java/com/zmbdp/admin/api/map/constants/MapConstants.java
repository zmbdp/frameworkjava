package com.zmbdp.admin.api.map.constants;

/**
 * 地图常量信息
 *
 * @author 稚名不带撇
 */
public class MapConstants {

    /**
     * 城市级别
     */
    public final static Integer CITY_LEVEL = 2;

    /**
     * 城市列表缓存 key
     */
    public final static String CACHE_MAP_CITY_KEY = "map:city:id";

    /**
     * 城市拼音缓存 key
     */
    public final static String CACHE_MAP_CITY_PINYIN_KEY = "map:city:pinyin";

    /**
     * 城市区划缓存 key
     */
    public final static String CACHE_MAP_CITY_CHILDREN_KEY = "map:city:children:";

    /**
     * 热门城市缓存 key
     */
    public final static String CACHE_MAP_HOT_CITY = "map:city:hot";

    /**
     * 根据关键词搜索的接口路由
     */
    public final static String QQMAP_API_PLACE_SUGGESTION = "/ws/place/v1/suggestion";

    /**
     * 根据经纬度来获取区域信息的接口路由
     */
    public final static String QQMAP_GEOCODER = "/ws/geocoder/v1";

    /**
     * 热门城市键
     */
    public final static String CONFIG_KEY = "sys_hot_city";
}
