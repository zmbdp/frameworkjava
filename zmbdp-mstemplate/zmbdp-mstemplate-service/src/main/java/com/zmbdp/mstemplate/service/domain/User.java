package com.zmbdp.mstemplate.service.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户测试实体
 *
 * @author 稚名不带撇
 */
@Data
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 用户年龄
     */
    private int age;
}