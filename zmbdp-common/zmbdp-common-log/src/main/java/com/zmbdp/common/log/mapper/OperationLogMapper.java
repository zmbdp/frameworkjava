package com.zmbdp.common.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.common.log.domain.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper 接口
 * <p>
 * 提供操作日志的数据库操作方法。
 *
 * @author 稚名不带撇
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
