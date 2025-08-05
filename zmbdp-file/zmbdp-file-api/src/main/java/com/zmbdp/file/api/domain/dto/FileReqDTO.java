package com.zmbdp.file.api.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 文件信息
 */
@Getter
@Setter
public class FileReqDTO {

    private String url;

    //路径信息   /目录/文件名.后缀名
    private String key;

    private String name;
}
