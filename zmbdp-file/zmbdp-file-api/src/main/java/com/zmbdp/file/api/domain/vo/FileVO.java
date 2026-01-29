package com.zmbdp.file.api.domain.vo;

import lombok.Data;

/**
 * 文件返回对象
 *
 * @author 稚名不带撇
 */
@Data
public class FileVO {

    /**
     * 文件访问地址
     */
    private String url;

    /**
     * 文件存储路径信息:<p>
     * /目录/文件名.后缀名
     */
    private String key;

    /**
     * 文件名
     */
    private String name;
}