package com.zmbdp.mstemplate.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/test/redis")
public class TestRedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/add")
    public Result<Void> add() {
//        redisService.setCacheObject("test", "abc");
//        redisService.setCacheObject("testABC", "abc", 15l, TimeUnit.SECONDS);
//
//        Boolean aBoolean = redisService.setCacheObjectIfAbsent("demoefg", "efg", 15l, TimeUnit.SECONDS);
//        if (!aBoolean) {
//            return Result.fail(ResultCode.FAILED.getCode(), ResultCode.FAILED.getErrMsg());
//        }

        User user = new User();
        user.setName("张三");
        user.setAge(11);
        redisService.setCacheObject("userKey", user);
        User userKey = redisService.getCacheObject("userKey", User.class);
        log.info("userKey: {}", userKey);

//        redisService.incr("testCountKey");
//        log.info("testCountKey: {}", redisService.getCacheObject("testCountKey", Long.class));
//
//        redisService.decr("testCountKey");
//        log.info("testCountKey: {}", redisService.getCacheObject("testCountKey", Long.class));
//
//
        RegionTest testRegion = new RegionTest();
        testRegion.setId(1L);
        testRegion.setName("北京");
        testRegion.setFullName("北京市");
        testRegion.setCode("110000");

        List<Map<String, RegionTest>> list = new ArrayList<>();
        Map<String, RegionTest> map = new LinkedHashMap<>();
        map.put("beijing", testRegion);
        list.add(map);
        list.add(map);
        list.add(map);
        list.add(map);
        list.add(map);

        redisService.setCacheObject("testList",list);

        return Result.success();
    }

    @GetMapping("/get")
    public Result<Void> get() {
//        String str = redisService.getCacheObject("test", String.class);
//        log.info(str);
//        User user = redisService.getCacheObject("userKey", User.class);
//        log.info("user:{}", user);

        //将redis中的数据获取出来  对象的类型不会产生泛型擦除问题
        List<Map<String, RegionTest>> testList = redisService.getCacheObject("testList", new TypeReference<List<Map<String, RegionTest>>>() {
        });
        System.out.println(testList);
        return Result.success();
    }
//
//    @PostMapping("/list/add")
//    public Result<Void> listAdd() {
//        String key = "listkey";
//        List<String> list = new ArrayList<>();
//        list.add("a");
//        list.add("b");
//        list.add("c");
//        list.add("d");
//        list.add("e");
//        list.add("c");
//        list.add("f");
//        list.add("g");
//        list.add("c");
//        list.add("h");
//        list.add("i");
//        list.add("c");
//        redisService.setCacheList(key, list);
////        redisService.leftPushForList(key, "p");
//        return Result.success();
//    }
//
//    @DeleteMapping("/list/delete")
//    public Result<Void> listDel() {
//        String key = "listkey";
////        redisService.rightPopForList(key);
////        redisService.removeAllForList(key, "c");
//        redisService.removeForAllList(key);
//        return Result.success();
//    }
//
//    @GetMapping("/list/get")
//    public Result<Void> listGet() {
//        String key = "listkey";
////        List<String> cacheList = redisService.getCacheList(key, String.class);
//        List<String> cacheList = redisService.getCacheListByRange(key,6, 2, String.class);
//        System.out.println(cacheList);
//        return Result.success();
//    }
//
//    @PostMapping("/type/add")
//    public Result<Void> typeAdd() {
////        String key = "setkey";
////        redisService.addMember(key, "a");
////        redisService.addMember(key, "a", "b", "c", "d");
//
////        String key = "zsetkey";
////        redisService.addMemberZSet(key, "a", 2.9);
////        redisService.addMemberZSet(key, "b", 3.9);
////        redisService.addMemberZSet(key, "c", 13.9);
////        redisService.addMemberZSet(key, "d", 1);
//
//        String key = "user1";   //mapkey
////        User user1 = new User();
////        user1.setName("张三");
////        user1.setAge(77);
////        redisService.setCacheMap(key, JsonUtil.convertToMap(user1));
//
//        redisService.setCacheMapValue(key, "address", "比特就业课");
//
//        String mapKey2 = "user2";
//        redisService.setCacheMapValue(mapKey2, "address", "比特就业课");
//        return Result.success();
//    }
//
//    @DeleteMapping("/type/delete")
//    public Result<Void> typeDel(){
////        String key = "zsetkey";
////        redisService.removeZSetByScore(key, 1, 3.9);
////        redisService.delMemberZSet(key, "c");
//
////        String key = "setkey";
////        List<String> keyList = new ArrayList<>();
////        keyList.add("user1");
////        keyList.add("user2");
////        redisService.deleteObject(keyList);
//        boolean cad = redisService.cad("delkey", "abc");
//        System.out.println(cad);
//        return Result.success();
//    }
//
//    @GetMapping("/type/get")
//    public Result<Void> typeGet(){
////        String key = "zsetkey";
////        Set<String> cacheZSet = redisService.getCacheZSetDesc(key, new TypeReference<LinkedHashSet<String>>() {
////        });
////        System.out.println(cacheZSet);
//        String key = "user1";
//        List<String> hkeys = new ArrayList<>();
//        hkeys.add("name");
//        hkeys.add("address");
//        List<String> multiCacheMapValue = redisService.getMultiCacheMapValue(key, hkeys, new TypeReference<List<String>>() {
//        });
//        System.out.println(multiCacheMapValue);
//        return Result.success();
//    }
}
