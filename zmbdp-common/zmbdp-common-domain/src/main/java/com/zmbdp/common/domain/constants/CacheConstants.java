package com.zmbdp.common.domain.constants;

/**
 * 缓存 token 的常量
 *
 * @author 稚名不带撇
 */
public class CacheConstants {
    /**
     * 缓存分割符
     */
    public final static String CACHE_SPLIT_COLON = ":";


    /**
     * 缓存 token 的有效期，默认 720（分钟）
     */
    public final static long EXPIRATION = 720;

    /**
     * 缓存 token 到多久就续期，默认 120（分钟）
     */
    public final static long REFRESH_TIME = 120;
}
