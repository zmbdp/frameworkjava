package com.zmbdp.admin.service.map.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmbdp.admin.service.map.domain.entity.SysRegion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * sys_region 表的 mapper
 *
 * @author 稚名不带撇
 */
@Mapper
public interface RegionMapper extends BaseMapper<SysRegion> {

    /**
     * 获取全量的城市列表
     *
     * @return 全量城市列表
     */
    List<SysRegion> selectAllRegion();
}
