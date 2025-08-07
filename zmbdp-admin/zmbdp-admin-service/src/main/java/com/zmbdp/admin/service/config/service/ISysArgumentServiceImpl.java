package com.zmbdp.admin.service.config.service;

import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentEditReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentListReqDTO;
import com.zmbdp.admin.api.config.domain.vo.ArgumentVO;
import com.zmbdp.common.domain.domain.vo.BasePageVO;

import java.util.List;

/**
 * 参数服务接口
 *
 * @author 稚名不带撇
 */
public interface ISysArgumentServiceImpl {

    /*=============================================    前端调用    =============================================*/

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

    /**
     * 编辑参数
     *
     * @param argumentEditReqDTO 编辑参数 DTO
     * @return 数据库 id
     */
    Long editArgument(ArgumentEditReqDTO argumentEditReqDTO);

    /*=============================================    远程调用    =============================================*/

    /**
     * 根据参数键查询参数对象
     *
     * @param configKey 参数键
     * @return 参数对象
     */
    ArgumentDTO getByConfigKey(String configKey);

    /**
     * 根据多个参数键查询多个参数对象
     *
     * @param configKeys 多个参数键
     * @return 多个参数对象
     */
    List<ArgumentDTO> getByConfigKeys(List<String> configKeys);
}
