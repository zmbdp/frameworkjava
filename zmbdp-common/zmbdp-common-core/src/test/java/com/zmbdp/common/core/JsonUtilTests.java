package com.zmbdp.common.core;

import com.zmbdp.common.core.domain.TestRegion;
import com.zmbdp.common.core.utils.JsonUtil;
import org.junit.jupiter.api.Test;

public class JsonUtilTests {

    // {"id":1,"name":"中国","fullName":"中国","code":"100000"}
    @Test
    public void testObjToString() {
        TestRegion testRegion = new TestRegion();
        testRegion.setId(1L);
        testRegion.setName("中国");
        testRegion.setFullName("中国");
        testRegion.setCode("100000");
        System.out.println(JsonUtil.objToString(testRegion));
    }

//    {
//        "id" : 1,
//            "name" : "中国",
//            "fullName" : "中国",
//            "code" : "100000"
//    }
    @Test
    public void testObjToStringPretty() {
        TestRegion testRegion = new TestRegion();
        testRegion.setId(1L);
        testRegion.setName("中国");
        testRegion.setFullName("中国");
        testRegion.setCode("100000");
        System.out.println(JsonUtil.objToStringPretty(testRegion));
    }

    @Test
    public void testStringToObj() {
        String json = "{\"id\":1,\"name\":\"中国\",\"fullName\":\"中国\",\"code\":\"100000\"}";
        TestRegion testRegion = JsonUtil.stringToObj(json, TestRegion.class);
        System.out.println(testRegion);
    }
}


