package com.zmbdp.common.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zmbdp.common.core.domain.TestRegion;
import com.zmbdp.common.core.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonUtil 测试类
 *
 * @author 稚名不带撇
 */
public class JsonUtilTests {

    /**
     * 测试 类转 json
     */
    // {"id":1,"name":"中国","fullName":"中国","code":"100000"}
    @Test
    public void testClassToJson() {
        TestRegion testRegion = new TestRegion();
        testRegion.setId(1L);
        testRegion.setName("中国");
        testRegion.setFullName("中国");
        testRegion.setCode("100000");
        System.out.println(JsonUtil.classToJson(testRegion));
    }

    //    {
//        "id" : 1,
//            "name" : "中国",
//            "fullName" : "中国",
//            "code" : "100000"
//    }

    /**
     * 测试 类转 jsonPretty
     */
    @Test
    public void testClassToJsonPretty() {
        TestRegion testRegion = new TestRegion();
        testRegion.setId(1L);
        testRegion.setName("中国");
        testRegion.setFullName("中国");
        testRegion.setCode("100000");
        System.out.println(JsonUtil.classToJsonPretty(testRegion));
    }


    /**
     * 测试 json 转 类
     */
    @Test
    public void testJsonToClass() {
        String json = "{\"id\":1,\"name\":\"中国\",\"fullName\":\"中国\",\"code\":\"100000\"}";
        TestRegion testRegion = JsonUtil.jsonToClass(json, TestRegion.class);
        System.out.println(testRegion);
    }


    /**
     * 测试 json 转 List
     */
    @Test
    public void testStringToListObj() {
//        TestRegion testRegion = new TestRegion();
//        testRegion.setId(1L);
//        testRegion.setName("中国");
//        testRegion.setFullName("中国");
//        testRegion.setCode("100000");
//        List<TestRegion> list = new ArrayList<>();
//        list.add(testRegion);
//        System.out.println(JsonUtil.objToString(list));
        // 泛型擦除问题，json 转对象会出现 转换成 LinkedHashMap 的问题，就是泛型擦除问题，需要添加泛型参数，如 List<TestRegion>
        String json = "[{\"id\":1,\"name\":\"中国\",\"fullName\":\"中国\",\"code\":\"100000\"}]";
        List list = JsonUtil.jsonToClass(json, List.class);
        System.out.println(list);
        List<TestRegion> list1 = JsonUtil.jsonToList(json, TestRegion.class);
        System.out.println(list1);
    }


    /**
     * 测试 json 转 Map
     */
    @Test
    public void testJsonToMap() {
        TestRegion testRegion = new TestRegion();
        testRegion.setId(1L);
        testRegion.setName("中国");
        testRegion.setFullName("中国");
        testRegion.setCode("100000");
        LinkedHashMap<String, TestRegion> map = new LinkedHashMap<>();
        // 先转换成 json，然后再转换成 map
        map.put("test1", testRegion);
        Map<String, TestRegion> map1 = JsonUtil.jsonToMap(JsonUtil.classToJson(map), TestRegion.class);
        System.out.println(map1);
    }

    /**
     * json 转复杂的嵌套类型测试
     */
    @Test
    public void testJsonToClass2() {
        TestRegion testRegion = new TestRegion();
        testRegion.setId(1L);
        testRegion.setName("中国");
        testRegion.setFullName("中国");
        testRegion.setCode("100000");
        LinkedHashMap<String, TestRegion> map = new LinkedHashMap<>();
        // 构建一个复杂对象
        map.put("test1", testRegion);
        List<Map<String, TestRegion>> list = new ArrayList<>();
        list.add(map);
        // 先转换成 json
        String json = JsonUtil.classToJson(list);
        List<Map<String, TestRegion>> list1 = JsonUtil
                .jsonToClass(json, new TypeReference<List<Map<String, TestRegion>>>() {
                });
        System.out.println(list1);
    }
}


