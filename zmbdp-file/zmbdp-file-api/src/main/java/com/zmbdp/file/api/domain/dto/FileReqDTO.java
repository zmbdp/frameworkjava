package com.zmbdp.file.api.domain.dto;

import lombok.Data;

/**
 * 文件信息
 *
 * @author 稚名不带撇
 */
@Data
public class FileReqDTO {

    /**
     * 文件访问地址
     */
    private String url;

    /**
     * 文件存储路径
     */
    //路径信息   /目录/文件名.后缀名
    private String key;

    /**
     * 文件名
     */
    private String name;
}