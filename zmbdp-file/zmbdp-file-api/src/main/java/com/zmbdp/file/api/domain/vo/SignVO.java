package com.zmbdp.file.api.domain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 签名返回值
 *
 * @author 稚名不带撇
 */
@Getter
@Setter
public class SignVO {

    /**
     * 签名
     */
    private String signature;

    /**
     * 存储桶
     */
    private String host;

    /**
     * 文件路径前缀
     */
    private String pathPrefix;

    /**
     * x-oss-credential
     */
    private String xOSSCredential;

    /**
     * x-oss-date
     */
    private String xOSSDate;

    /**
     * policy
     */
    private String policy;
}
