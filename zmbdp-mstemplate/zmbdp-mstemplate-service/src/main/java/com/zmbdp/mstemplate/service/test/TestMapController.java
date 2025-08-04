package com.zmbdp.mstemplate.service.test;

import com.zmbdp.admin.api.map.domain.vo.RegionVO;
import com.zmbdp.admin.api.map.feign.MapServiceApi;
import com.zmbdp.common.domain.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test/map")
public class TestMapController {

    @Autowired
    private MapServiceApi mapServiceApi;

    @RequestMapping("/city_list")
    public Result<List<RegionVO>> city_list() {
        return mapServiceApi.getCityList();
    }

    @RequestMapping("/city_pinyin_list")
    public Result<Map<String, List<RegionVO>>> city_pinyin_list() {
        return mapServiceApi.getCityPyList();
    }
}
