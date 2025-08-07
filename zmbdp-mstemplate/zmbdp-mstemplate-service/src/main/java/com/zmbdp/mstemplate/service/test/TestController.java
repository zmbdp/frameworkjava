package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {
    // /test/info
    @GetMapping("/info")
    public void info() {
        log.info("接口调用测试");
    }

    @GetMapping("/result")
    public Result<Void> result(int id) {
        if (id < 0) {
            return Result.fail();
        }
        return Result.success();
    }

    @GetMapping("/resultUser")
    public Result<User> resultId(int id) {
        if (id < 0) {
            return Result.fail(ResultCode.TOKEN_CHECK_FAILED.getCode(), ResultCode.TOKEN_CHECK_FAILED.getErrMsg());
        }
        User user = new User();
        user.setAge(50);
        user.setName("张三");
        return Result.success(user);
    }

    @GetMapping("/exception")
    public Result<Void> exception(int id) {
        if (id < 0) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        if (id == 1) {
            throw new ServiceException("id不能为1");
        }
        if (id == 1000) {
            throw new ServiceException("id不能为1000", ResultCode.ERROR_PHONE_FORMAT.getCode());
        }
        return Result.success();
    }

    @GetMapping("/copyMapProperties")
    public Result<Map<String, User>> copyMapProperties() {
        // 创建测试数据
        Map<String, RegionTest> sourceMap = new HashMap<>();

        // 添加正常数据
        RegionTest region1 = new RegionTest();
        region1.setId(1L);
        region1.setName("北京");
        region1.setCode("110000");
        region1.setParentId(0L);
        sourceMap.put("region1", region1);

        // 添加 null 值测试
        sourceMap.put("nullRegion", null);

        // 添加另一个正常数据
        RegionTest region2 = new RegionTest();
        region2.setId(2L);
        region2.setName("上海");
        region2.setCode("310000");
        region2.setParentId(0L);
        sourceMap.put("region2", region2);

        // 使用BeanCopyUtil.copyMapProperties进行拷贝
        Map<String, User> resultMap = BeanCopyUtil.copyMapProperties(sourceMap, User::new);

        return Result.success(resultMap);
    }

    @GetMapping("/copyMapListProperties")
    public Result<Map<String, List<User>>> copyMapListProperties() {
        // 创建测试数据
        Map<String, List<RegionTest>> sourceMap = new HashMap<>();

        // 添加一个包含多个元素的列表
        List<RegionTest> regionList1 = new ArrayList<>();
        RegionTest region1 = new RegionTest();
        region1.setId(1L);
        region1.setName("北京");
        region1.setCode("110000");
        regionList1.add(region1);

        RegionTest region2 = new RegionTest();
        region2.setId(2L);
        region2.setName("上海");
        region2.setCode("310000");
        regionList1.add(region2);

        sourceMap.put("regions", regionList1);

        // 添加一个空列表
        sourceMap.put("emptyList", new ArrayList<>());

        // 添加 null 列表
        sourceMap.put("nullList", null);

        // 添加包含 null 元素的列表
        List<RegionTest> regionList2 = new ArrayList<>();
        regionList2.add(null);
        RegionTest region3 = new RegionTest();
        region3.setId(3L);
        region3.setName("广州");
        region3.setCode("440000");
        regionList2.add(region3);
        sourceMap.put("listWithNull", regionList2);

        // 使用BeanCopyUtil.copyMapListProperties进行拷贝
        Map<String, List<User>> resultMap = BeanCopyUtil.copyMapListProperties(sourceMap, User::new);

        return Result.success(resultMap);
    }
}