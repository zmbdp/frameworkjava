package com.zmbdp.mstemplate.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.redis.service.RedisService;
import com.zmbdp.mstemplate.service.domain.RegionTest;
import com.zmbdp.mstemplate.service.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/test/redis")
public class TestRedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/add")
    public Result<Void> add() {
        redisService.setCacheObject("test", "abc");
        redisService.setCacheObject("testABC", "abc", 15l, TimeUnit.SECONDS);

        Boolean aBoolean = redisService.setCacheObjectIfAbsent("demoefg", "efg", 15l, TimeUnit.SECONDS);
        if (!aBoolean) {
            return Result.fail(ResultCode.FAILED.getCode(), ResultCode.FAILED.getErrMsg());
        }

        User user = new User();
        user.setName("张三");
        user.setAge(11);
        redisService.setCacheObject("userKey", user);
        User userKey = redisService.getCacheObject("userKey", User.class);
        log.info("userKey: {}", userKey);

        redisService.incr("testCountKey");
        log.info("testCountKey: {}", redisService.getCacheObject("testCountKey", Long.class));

        redisService.decr("testCountKey");
        log.info("testCountKey: {}", redisService.getCacheObject("testCountKey", Long.class));


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

        redisService.setCacheObject("testList", list);
        redisService.setCacheObject("testKey", list);

        return Result.success();
    }

    @GetMapping("/get")
    public Result<Void> get() {
        String str = redisService.getCacheObject("test", String.class);
        log.info(str);
        User user = redisService.getCacheObject("userKey", User.class);
        log.info("bloom:{}", user);

        // 将 redis 中的数据获取出来  对象的类型不会产生泛型擦除问题
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
        List<Map<String, RegionTest>> testList1 = redisService.getCacheList(listKey, new TypeReference<List<Map<String, RegionTest>>>() {
        });
        log.info("testList1:{}", testList1);
        log.info(redisService.getCacheListByRange(key, 0, 9, String.class).toString()); // 获取 list 指定范围的元素
//        log.info(redisService.getCacheListByRange(key, -3, 0, String.class).toString()); // 获取 list 指定范围的元素
        log.info(redisService.getCacheListByRange(listKey, 0, 9, new TypeReference<List<Map<String, RegionTest>>>() {
        }).toString()); // 获取 list 指定范围的元素
        log.info(String.valueOf(redisService.getCacheListSize(key))); // 获取 list 长度
        log.info(String.valueOf(redisService.getCacheListSize(listKey))); // 获取 list 长度
        return Result.success();
    }

    @PostMapping("/comprehensive/test")
    public Result<Void> comprehensiveTest() {
        try {
            // 准备测试数据
            RegionTest region1 = new RegionTest();
            region1.setId(1L);
            region1.setName("北京");
            region1.setCode("110000");

            RegionTest region2 = new RegionTest();
            region2.setId(2L);
            region2.setName("上海");
            region2.setCode("310000");

            RegionTest region3 = new RegionTest();
            region3.setId(3L);
            region3.setName("广州");
            region3.setCode("440000");

            log.info("============================   测试 RedisService 方法   ============================");

            // 1. 测试 Set 相关方法
            log.info("--- 测试 Set 相关方法 ---");
            String setKey = "testSetKey";
            String setKey1 = "testSetKey1";
            String setKey2 = "testSetKey2";
            String emptySetKey = "emptySetKey";

            // 先清理可能存在的数据
            Set<String> existingMembers = redisService.getCacheSet(setKey, new TypeReference<Set<String>>() {
            });
            if (existingMembers != null && !existingMembers.isEmpty()) {
                redisService.deleteMember(setKey, existingMembers.toArray());
            }

            Set<String> existingMembers1 = redisService.getCacheSet(setKey1, new TypeReference<Set<String>>() {
            });
            if (existingMembers1 != null && !existingMembers1.isEmpty()) {
                redisService.deleteMember(setKey1, existingMembers1.toArray());
            }

            Set<String> existingMembers2 = redisService.getCacheSet(setKey2, new TypeReference<Set<String>>() {
            });
            if (existingMembers2 != null && !existingMembers2.isEmpty()) {
                redisService.deleteMember(setKey2, existingMembers2.toArray());
            }

            // addMember
            Long addCount = redisService.addMember(setKey, "member1", "member2", "member3");
            log.info("addMember 添加了 {} 个元素", addCount);

            // isMember (存在的元素)
            boolean isMemberExists = redisService.isMember(setKey, "member1");
            log.info("isMember 'member1' (存在): {}", isMemberExists);

            // isMember (不存在的元素)
            boolean isMemberNotExists = redisService.isMember(setKey, "member4");
            log.info("isMember 'member4' (不存在): {}", isMemberNotExists);

            // isMember (不存在的key)
            boolean isMemberInEmptySet = redisService.isMember(emptySetKey, "member1");
            log.info("isMember 'member1' (在空集合中): {}", isMemberInEmptySet);

            // 复杂泛型嵌套测试 - Set<List<Map<String, RegionTest>>>
            log.info("--- 复杂泛型嵌套测试 Set<List<Map<String, RegionTest>>> ---");
            String complexSetKey = "complexSetKey";

            // 构造复杂数据结构
            List<Map<String, RegionTest>> list1 = new ArrayList<>();
            Map<String, RegionTest> map1 = new HashMap<>();
            map1.put("region1", region1);
            map1.put("region2", region2);
            list1.add(map1);

            Map<String, RegionTest> map2 = new HashMap<>();
            map2.put("region3", region3);
            list1.add(map2);

            List<Map<String, RegionTest>> list2 = new ArrayList<>();
            Map<String, RegionTest> map3 = new HashMap<>();
            map3.put("region1", region1);
            list2.add(map3);

            // 添加复杂数据到Set
            redisService.addMember(complexSetKey, list1, list2);

            // 获取复杂泛型Set
            Set<List<Map<String, RegionTest>>> complexSet = redisService.getCacheSet(complexSetKey,
                    new TypeReference<Set<List<Map<String, RegionTest>>>>() {
                    });
            log.info("复杂泛型 Set<List<Map<String, RegionTest>>> 获取到 {} 个 List 元素", complexSet.size());

            // 遍历复杂数据结构
            for (List<Map<String, RegionTest>> list : complexSet) {
                log.info("  List包含 {} 个 Map 元素", list.size());
                for (Map<String, RegionTest> map : list) {
                    log.info("    Map包含 {} 个 RegionTest 元素", map.size());
                    for (Map.Entry<String, RegionTest> entry : map.entrySet()) {
                        log.info("      Key: {}, Value: {}", entry.getKey(),
                                entry.getValue() != null ? entry.getValue().getName() : "null");
                    }
                }
            }

            // 复杂泛型嵌套测试 - Set<Map<String, List<RegionTest>>>
            log.info("--- 复杂泛型嵌套测试 Set<Map<String, List<RegionTest>>> ---");
            String complexSetKey2 = "complexSetKey2";

            // 构造另一种复杂数据结构
            Map<String, List<RegionTest>> complexMap1 = new HashMap<>();
            List<RegionTest> regionList1 = new ArrayList<>();
            regionList1.add(region1);
            regionList1.add(region2);
            complexMap1.put("group1", regionList1);

            List<RegionTest> regionList2 = new ArrayList<>();
            regionList2.add(region3);
            complexMap1.put("group2", regionList2);

            Map<String, List<RegionTest>> complexMap2 = new HashMap<>();
            List<RegionTest> regionList3 = new ArrayList<>();
            regionList3.add(region1);
            complexMap2.put("group3", regionList3);

            // 添加复杂数据到 Set
            redisService.addMember(complexSetKey2, complexMap1, complexMap2);

            // 获取复杂泛型Set
            Set<Map<String, List<RegionTest>>> complexSet2 = redisService.getCacheSet(complexSetKey2,
                    new TypeReference<Set<Map<String, List<RegionTest>>>>() {
                    });
            log.info("复杂泛型 Set<Map<String, List<RegionTest>>> 获取到 {} 个 Map 元素", complexSet2.size());

            // 遍历复杂数据结构
            for (Map<String, List<RegionTest>> map : complexSet2) {
                log.info("  Map包含 {} 个List元素", map.size());
                for (Map.Entry<String, List<RegionTest>> entry : map.entrySet()) {
                    log.info("    Key: {}, Value包含 {} 个 RegionTest元素 ", entry.getKey(), entry.getValue().size());
                    for (RegionTest region : entry.getValue()) {
                        log.info("      Region: {}", region != null ? region.getName() : "null");
                    }
                }
            }

            // getCacheSet (复杂泛型)
            redisService.addMember(setKey1, region1, region2);
            redisService.addMember(setKey2, region2, region3);
            Set<RegionTest> setRegions = redisService.getCacheSet(setKey1, new TypeReference<Set<RegionTest>>() {
            });
            log.info("getCacheSet 获取到 {} 个 RegionTest对象 ", setRegions.size());

            // getCacheSet (空集合)
            Set<String> emptySet = redisService.getCacheSet(emptySetKey, new TypeReference<Set<String>>() {
            });
            log.info("getCacheSet (空集合) 获取到 {} 个元素", emptySet != null ? emptySet.size() : 0);

            // getCacheSetSize
            Long setSize = redisService.getCacheSetSize(setKey);
            log.info("getCacheSetSize: {}", setSize);

            // getCacheSetSize (空集合)
            Long emptySetSize = redisService.getCacheSetSize(emptySetKey);
            log.info("getCacheSetSize (空集合): {}", emptySetSize);

            // intersectToCacheSet
            Set<RegionTest> intersectSet = redisService.intersectToCacheSet(setKey1, setKey2, new TypeReference<Set<RegionTest>>() {
            });
            log.info("intersectToCacheSet 交集有 {} 个元素", intersectSet.size());

            // intersectToCacheSet (与空集合)
            Set<RegionTest> intersectWithEmptySet = redisService.intersectToCacheSet(setKey1, emptySetKey, new TypeReference<Set<RegionTest>>() {
            });
            log.info("intersectToCacheSet (与空集合) 交集有 {} 个元素", intersectWithEmptySet.size());

            // unionToCacheSet
            Set<RegionTest> unionSet = redisService.unionToCacheSet(setKey1, setKey2, new TypeReference<Set<RegionTest>>() {
            });
            log.info("unionToCacheSet 并集有 {} 个元素", unionSet.size());

            // unionToCacheSet (与空集合)
            Set<RegionTest> unionWithEmptySet = redisService.unionToCacheSet(setKey1, emptySetKey, new TypeReference<Set<RegionTest>>() {
            });
            log.info("unionToCacheSet (与空集合) 并集有 {} 个元素", unionWithEmptySet.size());

            // differenceToCacheSet
            Set<RegionTest> diffSet = redisService.differenceToCacheSet(setKey1, setKey2, new TypeReference<Set<RegionTest>>() {
            });
            log.info("differenceToCacheSet 差集有 {} 个元素", diffSet.size());

            // differenceToCacheSet (与空集合)
            Set<RegionTest> diffWithEmptySet = redisService.differenceToCacheSet(setKey1, emptySetKey, new TypeReference<Set<RegionTest>>() {
            });
            log.info("differenceToCacheSet (与空集合) 差集有 {} 个元素", diffWithEmptySet.size());

            // moveMemberCacheSet (存在的元素)
            String sourceKey = "sourceSet";
            String destKey = "destSet";
            redisService.addMember(sourceKey, "movableMember");
            Boolean moved = redisService.moveMemberCacheSet(sourceKey, destKey, "movableMember");
            log.info("moveMemberCacheSet 移动结果 (存在元素): {}", moved);

            // moveMemberCacheSet (不存在的元素)
            Boolean movedNotExists = redisService.moveMemberCacheSet(sourceKey, destKey, "notExistsMember");
            log.info("moveMemberCacheSet 移动结果 (不存在元素): {}", movedNotExists);

            // deleteMember (存在的元素)
            Long deleteCount = redisService.deleteMember(setKey, "member1", "member2");
            log.info("deleteMember 删除了 {} 个元素", deleteCount);

            // deleteMember (不存在的元素)
            Long deleteNotExistsCount = redisService.deleteMember(setKey, "member4", "member5");
            log.info("deleteMember 删除了 {} 个不存在的元素", deleteNotExistsCount);

            // 2. 测试 ZSet 相关方法
            log.info("--- 测试 ZSet 相关方法 ---");
            String zsetKey = "testZSetKey";
            String emptyZSetKey = "emptyZSetKey";

            // addMemberZSet
            Boolean addedZSet = redisService.addMemberZSet(zsetKey, "zmember1", 1.0);
            log.info("addMemberZSet 添加结果: {}", addedZSet);
            redisService.addMemberZSet(zsetKey, "zmember2", 2.0);
            redisService.addMemberZSet(zsetKey, "zmember3", 3.0);

            // getZSetSize
            Long zsetSize = redisService.getZSetSize(zsetKey);
            log.info("getZSetSize: {}", zsetSize);

            // getZSetSize (空集合)
            Long emptyZSetSize = redisService.getZSetSize(emptyZSetKey);
            log.info("getZSetSize (空集合): {}", emptyZSetSize);

            // incrementZSetScore (存在的元素进行加分操作)
            Double newScore = redisService.incrementZSetScore(zsetKey, "zmember1", 1.5);
            log.info("incrementZSetScore 新分数 (存在元素): {}", newScore);

            // incrementZSetScore (不存在的元素进行加分操作)
            Double newScoreNotExists = redisService.incrementZSetScore(zsetKey, "zmember4", 1.5);
            log.info("incrementZSetScore 新分数 (不存在元素): {}", newScoreNotExists);

            // getZSetScore (存在的元素)
            Double score = redisService.getZSetScore(zsetKey, "zmember1");
            log.info("getZSetScore 'zmember1' 分数: {}", score);

            // getZSetScore (不存在的元素)
            Double scoreNotExists = redisService.getZSetScore(zsetKey, "zmember4");
            log.info("getZSetScore 'zmember4' 分数 (不存在): {}", scoreNotExists);

            // getZSetScore (不存在的 key)
            Double scoreEmptyKey = redisService.getZSetScore(emptyZSetKey, "zmember1");
            log.info("getZSetScore 'zmember1' 分数 (空key): {}", scoreEmptyKey);

            // getZSetRank (存在的元素)
            Long rank = redisService.getZSetRank(zsetKey, "zmember1");
            log.info("getZSetRank 'zmember1' 排名: {}", rank);
            Long rank3 = redisService.getZSetRank(zsetKey, "zmember3");
            log.info("getZSetRank 'zmember3' 排名: {}", rank3);
            Long rank4 = redisService.getZSetRank(zsetKey, "zmember4");
            log.info("getZSetRank 'zmember4' 排名: {}", rank4);

            // getZSetRank (不存在的元素)
            Long rankNotExists = redisService.getZSetRank(zsetKey, "zmember4");
            log.info("getZSetRank 'zmember4' 排名 (不存在): {}", rankNotExists);

            // getZSetReverseRank (存在的元素)
            Long reverseRank = redisService.getZSetReverseRank(zsetKey, "zmember1");
            log.info("getZSetReverseRank 'zmember1' 逆序排名: {}", reverseRank);

            // getZSetReverseRank (不存在的元素)
            Long reverseRankNotExists = redisService.getZSetReverseRank(zsetKey, "zmember5");
            log.info("getZSetReverseRank 'zmember5' 逆序排名 (不存在): {}", reverseRankNotExists);

            // 复杂泛型嵌套测试 - ZSet<List<Map<String, RegionTest>>>
            log.info("--- 复杂泛型嵌套测试 ZSet<List<Map<String, RegionTest>>> ---");
            String complexZSetKey = "complexZSetKey";

            // 添加复杂数据到 ZSet
            redisService.addMemberZSet(complexZSetKey, list1, 1.0);
            redisService.addMemberZSet(complexZSetKey, list2, 2.0);

            // 获取复杂泛型 ZSet (升序)
            Set<List<Map<String, RegionTest>>> complexZSet = redisService.getCacheZSet(complexZSetKey,
                    new TypeReference<LinkedHashSet<List<Map<String, RegionTest>>>>() {
                    });
            log.info("复杂泛型 ZSet<List<Map<String, RegionTest>>> (升序) 获取到 {} 个 List 元素", complexZSet.size());

            // 获取复杂泛型 ZSet (降序)
            Set<List<Map<String, RegionTest>>> complexZSetDesc = redisService.getCacheZSetDesc(complexZSetKey,
                    new TypeReference<LinkedHashSet<List<Map<String, RegionTest>>>>() {
                    });
            log.info("复杂泛型 ZSet<List<Map<String, RegionTest>>> (降序) 获取到 {} 个 List 元素", complexZSetDesc.size());

            // 复杂泛型嵌套测试 - ZSet<Map<String, List<RegionTest>>>
            log.info("--- 复杂泛型嵌套测试 ZSet<Map<String, List<RegionTest>>> ---");
            String complexZSetKey2 = "complexZSetKey2";

            // 添加复杂数据到 ZSet
            redisService.addMemberZSet(complexZSetKey2, complexMap1, 1.0);
            redisService.addMemberZSet(complexZSetKey2, complexMap2, 2.0);

            // 获取复杂泛型 ZSet (升序)
            Set<Map<String, List<RegionTest>>> complexZSet2 = redisService.getCacheZSet(complexZSetKey2,
                    new TypeReference<LinkedHashSet<Map<String, List<RegionTest>>>>() {
                    });
            log.info("复杂泛型 ZSet<Map<String, List<RegionTest>>> (升序) 获取到 {} 个 Map 元素", complexZSet2.size());

            // 获取复杂泛型 ZSet (降序)
            Set<Map<String, List<RegionTest>>> complexZSetDesc2 = redisService.getCacheZSetDesc(complexZSetKey2,
                    new TypeReference<LinkedHashSet<Map<String, List<RegionTest>>>>() {
                    });
            log.info("复杂泛型 ZSet<Map<String, List<RegionTest>>> (降序) 获取到 {} 个 Map 元素", complexZSetDesc2.size());

            // getZSetRange (复杂泛型)
            String zsetRegionKey = "testZSetRegionKey";
            redisService.addMemberZSet(zsetRegionKey, region1, 1.0);
            redisService.addMemberZSet(zsetRegionKey, region2, 2.0);
            Set<RegionTest> zsetRange = redisService.getZSetRange(zsetRegionKey, 0, -1, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetRange 获取到 {} 个 RegionTest 对象", zsetRange.size());

            // getZSetRange (空集合)
            Set<RegionTest> zsetRangeEmpty = redisService.getZSetRange(emptyZSetKey, 0, -1, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetRange (空集合) 获取到 {} 个 RegionTest 对象", zsetRangeEmpty != null ? zsetRangeEmpty.size() : 0);

            // getCacheZSet (复杂泛型)
            Set<RegionTest> cacheZSet = redisService.getCacheZSet(zsetRegionKey, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getCacheZSet 获取到 {} 个 RegionTest 对象", cacheZSet.size());

            // getCacheZSet (空集合)
            Set<RegionTest> cacheZSetEmpty = redisService.getCacheZSet(emptyZSetKey, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getCacheZSet (空集合) 获取到 {} 个 RegionTest 对象", cacheZSetEmpty != null ? cacheZSetEmpty.size() : 0);

            // getZSetRangeDesc (复杂泛型)
            Set<RegionTest> zsetRangeDesc = redisService.getZSetRangeDesc(zsetRegionKey, 0, -1, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetRangeDesc 获取到 {} 个 RegionTest 对象", zsetRangeDesc.size());

            // getZSetRangeDesc (空集合)
            Set<RegionTest> zsetRangeDescEmpty = redisService.getZSetRangeDesc(emptyZSetKey, 0, -1, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetRangeDesc (空集合) 获取到 {} 个 RegionTest 对象", zsetRangeDescEmpty != null ? zsetRangeDescEmpty.size() : 0);

            // getCacheZSetDesc (复杂泛型)
            Set<RegionTest> cacheZSetDesc = redisService.getCacheZSetDesc(zsetRegionKey, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getCacheZSetDesc 获取到 {} 个 RegionTest 对象", cacheZSetDesc.size());

            // getCacheZSetDesc (空集合)
            Set<RegionTest> cacheZSetDescEmpty = redisService.getCacheZSetDesc(emptyZSetKey, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getCacheZSetDesc (空集合) 获取到 {} 个 RegionTest 对象", cacheZSetDescEmpty != null ? cacheZSetDescEmpty.size() : 0);

            // getZSetRangeByScore (复杂泛型)
            Set<RegionTest> zsetRangeByScore = redisService.getZSetRangeByScore(zsetRegionKey, 0, 2.0, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetRangeByScore 获取到 {} 个RegionTest对象", zsetRangeByScore.size());

            // getZSetRangeByScore (空集合)
            Set<RegionTest> zsetRangeByScoreEmpty = redisService.getZSetRangeByScore(emptyZSetKey, 0, 2.0, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetRangeByScore (空集合) 获取到 {} 个 RegionTest 对象", zsetRangeByScoreEmpty != null ? zsetRangeByScoreEmpty.size() : 0);

            // getZSetReverseRangeByScore (复杂泛型)
            Set<RegionTest> zsetReverseRangeByScore = redisService.getZSetReverseRangeByScore(zsetRegionKey, 0, 2.0, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetReverseRangeByScore 获取到 {} 个 RegionTest 对象", zsetReverseRangeByScore.size());

            // getZSetReverseRangeByScore (空集合)
            Set<RegionTest> zsetReverseRangeByScoreEmpty = redisService.getZSetReverseRangeByScore(emptyZSetKey, 0, 2.0, new TypeReference<LinkedHashSet<RegionTest>>() {
            });
            log.info("getZSetReverseRangeByScore (空集合) 获取到 {} 个 RegionTest 对象", zsetReverseRangeByScoreEmpty != null ? zsetReverseRangeByScoreEmpty.size() : 0);

            // delMemberZSet (存在的元素)
            Long delCount = redisService.delMemberZSet(zsetKey, "zmember1");
            log.info("delMemberZSet 删除了 {} 个元素", delCount);

            // delMemberZSet (不存在的元素)
            Long delNotExistsCount = redisService.delMemberZSet(zsetKey, "zmember4");
            log.info("delMemberZSet 删除了 {} 个不存在的元素", delNotExistsCount);

            // removeZSetByScore
            Long removeByScoreCount = redisService.removeZSetByScore(zsetKey, 2.0, 3.0);
            log.info("removeZSetByScore 删除了 {} 个元素", removeByScoreCount);

            // removeZSetByScore (不存在的范围)
            Long removeByScoreNotExistsCount = redisService.removeZSetByScore(zsetKey, 10.0, 20.0);
            log.info("removeZSetByScore 删除了 {} 个不存在范围的元素", removeByScoreNotExistsCount);

            // 3. 测试 Hash 相关方法
            log.info("--- 测试 Hash 相关方法 ---");
            String hashKey = "testHashKey";
            String emptyHashKey = "emptyHashKey";

            // setCacheMap
            Map<String, String> testMap = new HashMap<>();
            testMap.put("field1", "value1");
            testMap.put("field2", "value2");
            redisService.setCacheMap(hashKey, testMap);
            log.info("setCacheMap 设置了 {} 个字段", testMap.size());

            // setCacheMapValue
            redisService.setCacheMapValue(hashKey, "field3", "value3");
            log.info("setCacheMapValue 设置了字段 field3");

            // getCacheMap
            Map<String, String> retrievedMap = redisService.getCacheMap(hashKey, String.class);
            log.info("getCacheMap 获取到 {} 个字段", retrievedMap.size());

            // getCacheMap (空 Hash)
            Map<String, String> retrievedEmptyMap = redisService.getCacheMap(emptyHashKey, String.class);
            log.info("getCacheMap (空Hash) 获取到 {} 个字段", retrievedEmptyMap != null ? retrievedEmptyMap.size() : 0);

            // 复杂泛型嵌套测试 - Hash<String, List<Map<String, RegionTest>>>
            log.info("--- 复杂泛型嵌套测试 Hash<String, List<Map<String, RegionTest>>> ---");
            String complexHashKey = "complexHashKey";

            // 构造复杂数据结构
            List<Map<String, RegionTest>> hashList1 = new ArrayList<>();
            Map<String, RegionTest> hashMap1 = new HashMap<>();
            hashMap1.put("region1", region1);
            hashMap1.put("region2", region2);
            hashList1.add(hashMap1);

            Map<String, RegionTest> hashMap2 = new HashMap<>();
            hashMap2.put("region3", region3);
            hashList1.add(hashMap2);

            List<Map<String, RegionTest>> hashList2 = new ArrayList<>();
            Map<String, RegionTest> hashMap3 = new HashMap<>();
            hashMap3.put("region1", region1);
            hashList2.add(hashMap3);

            // 设置复杂泛型 Hash
            redisService.setCacheMapValue(complexHashKey, "list1", hashList1);
            redisService.setCacheMapValue(complexHashKey, "list2", hashList2);

            // 获取复杂泛型Hash中的单个值
            List<Map<String, RegionTest>> retrievedHashList1 = redisService.getCacheMapValue(complexHashKey, "list1",
                    new TypeReference<List<Map<String, RegionTest>>>() {
                    });
            log.info("复杂泛型 Hash<String, List<Map<String, RegionTest>>> 获取到 list1 包含 {} 个Map元素",
                    retrievedHashList1 != null ? retrievedHashList1.size() : 0);

            // 获取整个复杂泛型 Hash
            Map<String, List<Map<String, RegionTest>>> retrievedComplexHash = redisService.getCacheMap(complexHashKey,
                    new TypeReference<Map<String, List<Map<String, RegionTest>>>>() {
                    });
            log.info("复杂泛型 getCacheMap<String, List<Map<String, RegionTest>>> 获取到 {} 个字段",
                    retrievedComplexHash != null ? retrievedComplexHash.size() : 0);

            // 复杂泛型嵌套测试 - Hash<String, Map<String, List<RegionTest>>>
            log.info("--- 复杂泛型嵌套测试 Hash<String, Map<String, List<RegionTest>>> ---");
            String complexHashKey2 = "complexHashKey2";

            // 设置复杂泛型 Hash
            redisService.setCacheMapValue(complexHashKey2, "map1", complexMap1);
            redisService.setCacheMapValue(complexHashKey2, "map2", complexMap2);

            // 获取复杂泛型Hash中的单个值
            Map<String, List<RegionTest>> retrievedHashMap1 = redisService.getCacheMapValue(complexHashKey2, "map1",
                    new TypeReference<Map<String, List<RegionTest>>>() {
                    });
            log.info("复杂泛型 Hash<String, Map<String, List<RegionTest>>> 获取到 map1 包含 {} 个List元素",
                    retrievedHashMap1 != null ? retrievedHashMap1.size() : 0);

            // 获取整个复杂泛型 Hash
            Map<String, Map<String, List<RegionTest>>> retrievedComplexHash2 = redisService.getCacheMap(complexHashKey2,
                    new TypeReference<Map<String, Map<String, List<RegionTest>>>>() {
                    });
            log.info("复杂泛型 getCacheMap<String, Map<String, List<RegionTest>>> 获取到 {} 个字段",
                    retrievedComplexHash2 != null ? retrievedComplexHash2.size() : 0);

            // getCacheMap (复杂泛型)
            String simpleComplexHashKey = "simpleComplexHashKey";
            Map<String, RegionTest> complexMap = new HashMap<>();
            complexMap.put("region1", region1);
            complexMap.put("region2", region2);
            redisService.setCacheMap(simpleComplexHashKey, complexMap);
            Map<String, RegionTest> retrievedComplexMap = redisService.getCacheMap(simpleComplexHashKey, new TypeReference<Map<String, RegionTest>>() {
            });
            log.info("简单复杂泛型 getCacheMap 获取到 {} 个字段", retrievedComplexMap.size());

            // getCacheMapValue (存在的字段)
            String value = redisService.getCacheMapValue(hashKey, "field1");
            log.info("getCacheMapValue 获取 field1 的值: {}", value);

            // getCacheMapValue (不存在的字段)
            String valueNotExists = redisService.getCacheMapValue(hashKey, "field4");
            log.info("getCacheMapValue 获取 field4 的值 (不存在): {}", valueNotExists);

            // getCacheMapValue (不存在的 key)
            String valueEmptyKey = redisService.getCacheMapValue(emptyHashKey, "field1");
            log.info("getCacheMapValue 获取 field1 的值 (空key): {}", valueEmptyKey);

            // getCacheMapValue (复杂泛型，存在的字段)
            RegionTest regionValue = redisService.getCacheMapValue(simpleComplexHashKey, "region1", new TypeReference<RegionTest>() {
            });
            log.info("复杂泛型 getCacheMapValue 获取 region1 的值: {}", regionValue != null ? regionValue.getName() : null);

            // getCacheMapValue (复杂泛型，不存在的字段)
            RegionTest regionValueNotExists = redisService.getCacheMapValue(simpleComplexHashKey, "region3", new TypeReference<RegionTest>() {
            });
            log.info("复杂泛型 getCacheMapValue 获取 region3 的值 (不存在): {}", regionValueNotExists);

            // getMultiCacheMapValue (存在的字段)
            List<String> hKeys = Arrays.asList("field1", "field2");
            List<String> multiValues = redisService.getMultiCacheMapValue(hashKey, hKeys, String.class);
            log.info("getMultiCacheMapValue 获取到 {} 个值", multiValues.size());

            // getMultiCacheMapValue (部分存在的字段)
            List<String> hKeysPartial = Arrays.asList("field1", "field4");
            List<String> multiValuesPartial = redisService.getMultiCacheMapValue(hashKey, hKeysPartial, String.class);
            log.info("getMultiCacheMapValue (部分存在) 获取到 {} 个值，其中null值个数: {}",
                    multiValuesPartial.size(), multiValuesPartial.stream().filter(Objects::isNull).count());

            // getMultiCacheMapValue (不存在的 key)
            List<String> multiValuesEmptyKey = redisService.getMultiCacheMapValue(emptyHashKey, hKeys, String.class);
            log.info("getMultiCacheMapValue (空key) 获取到 {} 个值", multiValuesEmptyKey != null ? multiValuesEmptyKey.size() : 0);

            // getMultiCacheMapValue (复杂泛型，存在的字段)
            List<RegionTest> multiComplexValues = redisService.getMultiCacheListValue(simpleComplexHashKey, Arrays.asList("region1", "region2"), new TypeReference<List<RegionTest>>() {
            });
            log.info("复杂泛型 getMultiCacheMapValue 获取到 {} 个值", multiComplexValues.size());

            // getMultiCacheMapValue (复杂泛型，部分存在的字段)
            List<RegionTest> multiComplexValuesPartial = redisService.getMultiCacheListValue(simpleComplexHashKey, Arrays.asList("region1", "region3"), new TypeReference<List<RegionTest>>() {
            });
            log.info("复杂泛型 getMultiCacheMapValue (部分存在) 获取到 {} 个值，其中null值个数: {}",
                    multiComplexValuesPartial.size(), multiComplexValuesPartial.stream().filter(Objects::isNull).count());

            // getCacheMapSize
            Long hashSize = redisService.getCacheMapSize(hashKey);
            log.info("getCacheMapSize: {}", hashSize);

            // getCacheMapSize (空 Hash)
            Long emptyHashSize = redisService.getCacheMapSize(emptyHashKey);
            log.info("getCacheMapSize (空Hash): {}", emptyHashSize);

            // getCacheMapKeys
            Set<String> hashKeys = redisService.getCacheMapKeys(hashKey);
            log.info("getCacheMapKeys 获取到 {} 个键", hashKeys.size());

            // getCacheMapKeys (空Hash)
            Set<String> emptyHashKeys = redisService.getCacheMapKeys(emptyHashKey);
            log.info("getCacheMapKeys (空Hash) 获取到 {} 个键", emptyHashKeys.size());

            // incrementCacheMapValue (Long, 存在的字段)
            redisService.setCacheMapValue(hashKey, "counter", "10");
            Long incrementedLong = redisService.incrementCacheMapValue(hashKey, "counter", 5L);
            log.info("incrementCacheMapValue (Long) 增加后值为: {}", incrementedLong);

            // incrementCacheMapValue (Long, 不存在的字段)
            Long incrementedLongNotExists = redisService.incrementCacheMapValue(hashKey, "newCounter", 5L);
            log.info("incrementCacheMapValue (Long, 不存在字段) 增加后值为: {}", incrementedLongNotExists);

            // incrementCacheMapValue (Double, 存在的字段)
            redisService.setCacheMapValue(hashKey, "doubleCounter", "10.5");
            Double incrementedDouble = redisService.incrementCacheMapValue(hashKey, "doubleCounter", 2.5);
            log.info("incrementCacheMapValue (Double) 增加后值为: {}", incrementedDouble);

            // incrementCacheMapValue (Double, 不存在的字段)
            Double incrementedDoubleNotExists = redisService.incrementCacheMapValue(hashKey, "newDoubleCounter", 2.5);
            log.info("incrementCacheMapValue (Double, 不存在字段) 增加后值为: {}", incrementedDoubleNotExists);

            // deleteCacheMapValue (存在的字段)
            boolean deleted = redisService.deleteCacheMapValue(hashKey, "field3");
            log.info("deleteCacheMapValue 删除结果 (存在字段): {}", deleted);

            // deleteCacheMapValue (不存在的字段)
            boolean deletedNotExists = redisService.deleteCacheMapValue(hashKey, "field4");
            log.info("deleteCacheMapValue 删除结果 (不存在字段): {}", deletedNotExists);

            // deleteCacheMapValues (存在的字段)
            Long deletedCount = redisService.deleteCacheMapValues(hashKey, "field1", "field2");
            log.info("deleteCacheMapValues 删除了 {} 个字段", deletedCount);

            // deleteCacheMapValues (部分存在的字段)
            Long deletedCountPartial = redisService.deleteCacheMapValues(hashKey, "field5", "field6");
            log.info("deleteCacheMapValues (部分存在) 删除了 {} 个字段", deletedCountPartial);

            log.info("=== 所有 RedisService 方法测试完成 ===");

        } catch (Exception e) {
            log.error("测试过程中发生异常", e);
            return Result.fail("测试失败: " + e.getMessage());
        }

        return Result.success();
    }

    /**
     * 测试获取Hash中多个数据的 Map 版本（复杂泛型嵌套）
     * 使用复杂的嵌套结构：Map<String, List<Map<String, Set<TestRegion>>>>
     * 这种结构可以测试深层嵌套的泛型处理能力
     */
    @PostMapping("/test/map")
    public Result<Void> testGetMultiCacheMapValueWithComplexGenerics() {
        String hashKey = "test:map:complex";

        // 创建测试数据
        RegionTest region1 = new RegionTest();
        region1.setId(1L);
        region1.setName("北京");
        region1.setFullName("北京市");
        region1.setCode("110000");

        RegionTest region2 = new RegionTest();
        region2.setId(2L);
        region2.setName("上海");
        region2.setFullName("上海市");
        region2.setCode("310000");

        RegionTest region3 = new RegionTest();
        region3.setId(3L);
        region3.setName("广州");
        region3.setFullName("广州市");
        region3.setCode("440000");

        // 构建复杂嵌套结构：List<Map<String, Set<RegionTest>>>
        List<Map<String, Set<RegionTest>>> complexList1 = new ArrayList<>();
        Map<String, Set<RegionTest>> map1 = new HashMap<>();
        Set<RegionTest> set1 = new HashSet<>();
        set1.add(region1);
        set1.add(region2);
        map1.put("northernCities", set1);
        complexList1.add(map1);

        Map<String, Set<RegionTest>> map2 = new HashMap<>();
        Set<RegionTest> set2 = new HashSet<>();
        set2.add(region3);
        map2.put("southernCities", set2);
        complexList1.add(map2);

        List<Map<String, Set<RegionTest>>> complexList2 = new ArrayList<>();
        Map<String, Set<RegionTest>> map3 = new HashMap<>();
        Set<RegionTest> set3 = new HashSet<>();
        set3.add(region1);
        set3.add(region3);
        map3.put("importantCities", set3);
        complexList2.add(map3);

        // 将复杂结构存储到 Redis Hash 中
        redisService.setCacheMapValue(hashKey, "data1", complexList1);
        redisService.setCacheMapValue(hashKey, "data2", complexList2);
        redisService.setCacheMapValue(hashKey, "data3", new ArrayList<Map<String, Set<RegionTest>>>());

        // 测试获取多个 Hash 字段的值，以 Map 形式返回
        List<String> hKeys = Arrays.asList("data1", "data2");
        Map<String, List<Map<String, Set<RegionTest>>>> result = redisService.getMultiCacheMapValue(
                hashKey,
                hKeys,
                new TypeReference<Map<String, List<Map<String, Set<RegionTest>>>>>() {
                }
        );

        log.info("复杂泛型嵌套测试 - Map 版本:");
        if (result != null) {
            result.forEach((key, value) -> {
                log.info("  键: {}, 值包含 {} 个 Map 元素", key, value.size());
                for (int i = 0; i < value.size(); i++) {
                    Map<String, Set<RegionTest>> map = value.get(i);
                    log.info("    Map {} 包含 {} 个键值对", i + 1, map.size());
                    for (Map.Entry<String, Set<RegionTest>> entry : map.entrySet()) {
                        log.info("      键: {}, 值包含 {} 个区域", entry.getKey(), entry.getValue().size());
                        for (RegionTest region : entry.getValue()) {
                            log.info("        区域: {}({})", region.getName(), region.getCode());
                        }
                    }
                }
            });
        } else {
            log.info("  获取结果为空");
            return Result.fail("获取结果为空");
        }
        return Result.success();
    }

    /**
     * 测试获取Hash中多个数据的Set版本（复杂泛型嵌套）
     * 使用复杂的嵌套结构：Set<Map<String, List<Set<TestRegion>>>>
     * 这种结构可以测试深层嵌套的泛型处理能力
     */
    @PostMapping("/test/set")
    public Result<Void> testGetMultiCacheSetValueWithComplexGenerics() {
        String hashKey = "test:set:complex";

        // 创建测试数据
        RegionTest region1 = new RegionTest();
        region1.setId(1L);
        region1.setName("北京");
        region1.setFullName("北京市");
        region1.setCode("110000");

        RegionTest region2 = new RegionTest();
        region2.setId(2L);
        region2.setName("上海");
        region2.setFullName("上海市");
        region2.setCode("310000");

        RegionTest region3 = new RegionTest();
        region3.setId(3L);
        region3.setName("广州");
        region3.setFullName("广州市");
        region3.setCode("440000");

        // 构建复杂嵌套结构：Map<String, List<Set<RegionTest>>>
        Map<String, List<Set<RegionTest>>> complexMap1 = new HashMap<>();
        List<Set<RegionTest>> list1 = new ArrayList<>();
        Set<RegionTest> set1 = new HashSet<>();
        set1.add(region1);
        set1.add(region2);
        list1.add(set1);

        Set<RegionTest> set2 = new HashSet<>();
        set2.add(region3);
        list1.add(set2);
        complexMap1.put("cityGroups", list1);

        Map<String, List<Set<RegionTest>>> complexMap2 = new HashMap<>();
        List<Set<RegionTest>> list2 = new ArrayList<>();
        Set<RegionTest> set3 = new HashSet<>();
        set3.add(region1);
        set3.add(region3);
        list2.add(set3);
        complexMap2.put("importantCities", list2);

        // 将复杂结构存储到 Redis Hash 中
        redisService.setCacheMapValue(hashKey, "group1", complexMap1);
        redisService.setCacheMapValue(hashKey, "group2", complexMap2);
        redisService.setCacheMapValue(hashKey, "group3", new HashMap<String, List<Set<RegionTest>>>());

        // 测试获取多个 Hash 字段的值，以 Set 形式返回
        List<String> hKeys = Arrays.asList("group1", "group2");
        Set<Map<String, List<Set<RegionTest>>>> result = redisService.getMultiCacheSetValue(
                hashKey,
                hKeys,
                new TypeReference<Set<Map<String, List<Set<RegionTest>>>>>() {
                }
        );

        log.info("复杂泛型嵌套测试 - Set 版本:");
        if (result != null) {
            log.info("  结果包含 {} 个 Map 元素", result.size());
            int groupIndex = 1;
            for (Map<String, List<Set<RegionTest>>> map : result) {
                log.info("    组 {} 包含 {} 个键值对", groupIndex, map.size());
                for (Map.Entry<String, List<Set<RegionTest>>> entry : map.entrySet()) {
                    log.info("      键: {}, 值包含 {} 个 Set 元素", entry.getKey(), entry.getValue().size());
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        Set<RegionTest> regionSet = entry.getValue().get(i);
                        log.info("        Set {} 包含 {} 个区域", i + 1, regionSet.size());
                        for (RegionTest region : regionSet) {
                            log.info("          区域: {}({})", region.getName(), region.getCode());
                        }
                    }
                }
                groupIndex++;
            }
        } else {
            log.info("  获取结果为空");
            return Result.fail("获取结果为空");
        }
        return Result.success();
    }

    /**
     * 测试 Redis 通用操作方法
     * 包括 expire、getExpire、hasKey、keys、renameKey、deleteObject 等方法
     */
    @PostMapping("/test/general")
    public Result<Void> testGeneralRedisOperations() {
        try {
            String testKey = "test:general:operations";
            String testKey2 = "test:general:operations2";

            // 清理可能存在的测试数据
            redisService.deleteObject(testKey);
            redisService.deleteObject(testKey2);

            // 1. 测试 setCacheObject 和 hasKey
            log.info("--- 测试 setCacheObject 和 hasKey ---");
            redisService.setCacheObject(testKey, "testValue");
            Boolean exists = redisService.hasKey(testKey);
            log.info("hasKey '{}': {}", testKey, exists);

            Boolean notExists = redisService.hasKey("nonexistent:key");
            log.info("hasKey 'nonexistent:key': {}", notExists);

            // 2. 测试 expire 和 getExpire
            log.info("--- 测试 expire 和 getExpire ---");
            Boolean expireResult = redisService.expire(testKey, 300L); // 5分钟
            log.info("expire '{}' 300秒: {}", testKey, expireResult);

            Long ttl = redisService.getExpire(testKey);
            log.info("getExpire '{}': {} 秒", testKey, ttl);

            // 测试指定时间单位的 expire
            redisService.setCacheObject(testKey2, "testValue2");
            Boolean expireWithUnitResult = redisService.expire(testKey2, 5L, TimeUnit.MINUTES);
            log.info("expire '{}' 5分钟 (指定单位): {}", testKey2, expireWithUnitResult);

            Long ttl2 = redisService.getExpire(testKey2);
            log.info("getExpire '{}': {} 秒", testKey2, ttl2);

            // 3. 测试 getExpire 对不存在键的处理
            log.info("--- 测试 getExpire 对不存在键的处理 ---");
            Long notExistsTtl = redisService.getExpire("nonexistent:key");
            log.info("getExpire 'nonexistent:key': {}", notExistsTtl);

            // 4. 测试 keys 方法
            log.info("--- 测试 keys 方法 ---");
            // 添加一些测试数据
            redisService.setCacheObject("test:pattern:1", "value1");
            redisService.setCacheObject("test:pattern:2", "value2");
            redisService.setCacheObject("test:other:1", "value3");

            Collection<String> allTestKeys = redisService.keys("test:*");
            log.info("keys 'test:*' 匹配到 {} 个键", allTestKeys.size());
            allTestKeys.forEach(key -> log.info("  匹配的键: {}", key));

            Collection<String> patternKeys = redisService.keys("test:pattern:*");
            log.info("keys 'test:pattern:*' 匹配到 {} 个键", patternKeys.size());
            patternKeys.forEach(key -> log.info("  匹配的键: {}", key));

            Collection<String> noMatchKeys = redisService.keys("nonexistent:*");
            log.info("keys 'nonexistent:*' 匹配到 {} 个键", noMatchKeys.size());

            // 5. 测试 renameKey 方法
            log.info("--- 测试 renameKey 方法 ---");
            String oldKey = "test:rename:old";
            String newKey = "test:rename:new";
            redisService.setCacheObject(oldKey, "renameTestValue");
            log.info("设置键 '{}' 值为 'renameTestValue'", oldKey);

            redisService.renameKey(oldKey, newKey);
            log.info("renameKey '{}' 重命名为 '{}'", oldKey, newKey);

            String newValue = redisService.getCacheObject(newKey, String.class);
            log.info("获取重命名后的键 '{}' 的值: {}", newKey, newValue);

            Boolean oldKeyExists = redisService.hasKey(oldKey);
            log.info("原键 '{}' 是否存在: {}", oldKey, oldKeyExists);

            // 6. 测试 deleteObject 方法
            log.info("--- 测试 deleteObject 方法 ---");
            // 删除单个键
            Boolean deleteSingleResult = redisService.deleteObject(newKey);
            log.info("deleteObject '{}' 结果: {}", newKey, deleteSingleResult);

            // 验证是否删除成功
            Boolean deletedKeyExists = redisService.hasKey(newKey);
            log.info("删除后的键 '{}' 是否存在: {}", newKey, deletedKeyExists);

            // 删除多个键
            List<String> keysToDelete = Arrays.asList("test:pattern:1", "test:pattern:2", "test:other:1");
            Long deleteMultipleResult = redisService.deleteObject(keysToDelete);
            log.info("deleteObject 删除 {} 个键，实际删除了 {} 个键", keysToDelete.size(), deleteMultipleResult);

            // 验证是否删除成功
            for (String key : keysToDelete) {
                Boolean keyExists = redisService.hasKey(key);
                log.info("键 '{}' 删除后是否存在: {}", key, keyExists);
            }

            // 删除不存在的键
            Long deleteNonexistentResult = redisService.deleteObject(Arrays.asList("nonexistent:key1", "nonexistent:key2"));
            log.info("deleteObject 删除不存在的键，结果: {}", deleteNonexistentResult);

            log.info("=== Redis 通用操作方法测试完成 ===");
            return Result.success();

        } catch (Exception e) {
            log.error("测试 Redis 通用操作方法过程中发生异常", e);
            return Result.fail("测试失败: " + e.getMessage());
        }
    }


}
