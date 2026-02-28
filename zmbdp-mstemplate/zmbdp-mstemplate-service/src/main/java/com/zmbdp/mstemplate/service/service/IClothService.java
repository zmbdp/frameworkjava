package com.zmbdp.mstemplate.service.service;

/**
 * 服装服务接口
 *
 * @author 稚名不带撇
 */
public interface IClothService {

    /**
     * 获取服装价格
     *
     * @param proId 产品ID
     * @return 价格
     */
    Integer clothPriceGet(Long proId);
}