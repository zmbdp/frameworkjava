package com.zmbdp.mstemplate.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    @PostMapping("/list/add")
    public Result<Void> listAdd() {
        String key = "listkey";
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        list.add("f");
        list.add("h");
        list.add("g");
        list.add("h");
        list.add("h");
        list.add("l");
        list.add("i");
        list.add("j");
        list.add("h");
        list.add("k");
        list.add("l");
        list.add("m");
        list.add("n");
        list.add("h");
        list.add("o");
        list.add("p");
        list.add("h");
        list.add("l");
        list.add("q");
        list.add("r");
        list.add("o");
        list.add("s");
        list.add("l");
        list.add("l");
        list.add("l");
        list.add("t");
        list.add("u");
        list.add("v");
        list.add("o");
        list.add("l");
        list.add("w");
        list.add("x");
        list.add("h");
        list.add("o");
        list.add("y");
        list.add("g");
        log.info(redisService.setCacheList(key, list).toString()); // 添加

        log.info(redisService.leftPushForList(key, "1").toString()); // 头插
        redisService.leftPopForList(key); // 头删
        redisService.leftPopForList(key, 2); // 批量头删
        log.info(redisService.rightPushForList(key, "2").toString()); // 尾插
        redisService.rightPopForList(key); // 尾删
        redisService.rightPopForList(key, 2); // 批量尾删
        log.info(redisService.removeLeftForList(key, "o").toString()); // 删除第一个匹配的元素，从左往右
        log.info(redisService.removeLeftForList(key, "o", 3).toString()); // 删除 k 个匹配的元素，从左往右
        log.info(redisService.removeRightForList(key, "l").toString()); // 删除第一个匹配的元素，从右往左
        log.info(redisService.removeRightForList(key, "l", 3).toString()); // 删除 k 个匹配的元素，从右往左
        log.info(redisService.removeAllForList(key, "h").toString()); // 删除所有匹配的元素
        redisService.removeForAllList(key); // 删除所有元素
        log.info(redisService.setCacheList(key, list).toString()); // 添加
        redisService.retainListRange(key, 0, 5); // 保留范围内的元素
        redisService.setElementAtIndex(key, 0, "555"); // 修改指定索引的元素
        log.info(redisService.getCacheList(key, String.class).toString()); // 获取所有数据

        String listKey = "list:region:test";
        List<Map<String, RegionTest>> testList = new ArrayList<>();
        RegionTest regionTest = new RegionTest();
        regionTest.setId(1L);
        regionTest.setName("测试");
        regionTest.setFullName("测试");
        Map<String, RegionTest> map = new HashMap<>();
        map.put("1", regionTest);
        map.put("2", regionTest);
        Map<String, RegionTest> map2 = new HashMap<>();
        map2.put("1", regionTest);
        map2.put("2", regionTest);
        testList.add(map);
        testList.add(map2);
        redisService.setCacheList(listKey, testList);
        List<Map<String, RegionTest>> testList1 = redisService.getCacheList(listKey, new TypeReference<List<Map<String, RegionTest>>>() {});
        log.info("testList1:{}", testList1);
        log.info(redisService.getCacheListByRange(key, 0, 9, String.class).toString()); // 获取 list 指定范围的元素
//        log.info(redisService.getCacheListByRange(key, -3, 0, String.class).toString()); // 获取 list 指定范围的元素
        log.info(redisService.getCacheListByRange(listKey, 0, 9, new TypeReference<List<Map<String, RegionTest>>>() {}).toString()); // 获取 list 指定范围的元素
        log.info(String.valueOf(redisService.getCacheListSize(key))); // 获取 list 长度
        log.info(String.valueOf(redisService.getCacheListSize(listKey))); // 获取 list 长度
        return Result.success();
    }

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
