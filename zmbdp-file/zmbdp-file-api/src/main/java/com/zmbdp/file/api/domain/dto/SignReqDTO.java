package com.zmbdp.file.api.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 签名请求参数
 *
 * @author 稚名不带撇
 */
@Getter
@Setter
public class SignReqDTO {

    /**
     * 签名
     */
    private String signature;

    /**
     * 域名
     */
    private String host;

    /**
     * 路径前缀
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
     * 策略
     */
    private String policy;
}
