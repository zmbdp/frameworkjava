package com.zmbdp.admin.service.config.service;

import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentListReqDTO;
import com.zmbdp.admin.api.config.domain.vo.ArgumentVO;
import com.zmbdp.common.domain.domain.vo.BasePageVO;

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

    /**
     * 获取参数列表, 模糊查询 name
     *
     * @param argumentListReqDTO 查看参数 DTO
     * @return 符合要求的参数列表
     */
    BasePageVO<ArgumentVO> listArgument(ArgumentListReqDTO argumentListReqDTO);
}
