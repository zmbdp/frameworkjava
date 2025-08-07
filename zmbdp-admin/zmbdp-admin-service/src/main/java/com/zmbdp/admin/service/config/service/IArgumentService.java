package com.zmbdp.admin.service.config.service;

import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;

/**
 * 参数服务接口
 *
 * @author 稚名不带撇
 */
public interface IArgumentService {
    /**
     * 新增参数
     *
     * @param argumentAddReqDTO 新增参数请求 DTO
     * @return 数据库的 id
     */
    Long addArgument(ArgumentAddReqDTO argumentAddReqDTO);
}
