package com.zmbdp.admin.service.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentEditReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentListReqDTO;
import com.zmbdp.admin.api.config.domain.vo.ArgumentVO;
import com.zmbdp.admin.service.config.domain.entity.SysArgument;
import com.zmbdp.admin.service.config.mapper.SysArgumentMapper;
import com.zmbdp.admin.service.config.service.ISysArgumentServiceImpl;
import com.zmbdp.common.core.utils.BeanCopyUtil;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 参数服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class SysSysArgumentServiceImplImpl implements ISysArgumentServiceImpl {

    /**
     * 参数表 Mapper
     */
    @Autowired
    private SysArgumentMapper sysArgumentMapper;

    /*=============================================    前端调用    =============================================*/

    /**
     * 新增参数
     *
     * @param argumentAddReqDTO 新增参数请求 DTO
     * @return 数据库的 id
     */
    @Override
    public Long addArgument(ArgumentAddReqDTO argumentAddReqDTO) {
        // 查一下看看有没有重复的 参数业务主键 (configKey) 或者是 参数名称 (name)
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getConfigKey, argumentAddReqDTO.getConfigKey())
                .or()
                .eq(SysArgument::getName, argumentAddReqDTO.getName())
        );
        if (sysArgument != null) {
            log.warn("SysArgumentServiceImplImpl.addArgument: [参数业务主键或参数名称重复: {} ]", argumentAddReqDTO);
            throw new ServiceException("参数业务主键或参数名称重复");
        }
        // 说明没有，直接转对象插入数据库
        sysArgument = new SysArgument();
        BeanCopyUtil.copyProperties(argumentAddReqDTO, sysArgument);
        sysArgumentMapper.insert(sysArgument);
        return sysArgument.getId();
    }

    /**
     * 获取参数列表, 模糊查询 name
     *
     * @param argumentListReqDTO 查看参数 DTO
     * @return 符合要求的参数列表
     */
    @Override
    public BasePageVO<ArgumentVO> listArgument(ArgumentListReqDTO argumentListReqDTO) {
        // 查询数据库，查出所有符合的参数列表
        LambdaQueryWrapper<SysArgument> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(argumentListReqDTO.getConfigKey())) {
            queryWrapper.eq(SysArgument::getConfigKey, argumentListReqDTO.getConfigKey());
        }
        if (StringUtils.isNotBlank(argumentListReqDTO.getName())) {
            queryWrapper.like(SysArgument::getName, argumentListReqDTO.getName());
        }
        Page<SysArgument> page = sysArgumentMapper.selectPage(
                new Page<>(argumentListReqDTO.getPageNo().longValue(), argumentListReqDTO.getPageSize().longValue()),
                queryWrapper
        );
        // 转换对象设置参数返回
        List<ArgumentVO> list = BeanCopyUtil.copyListProperties(page.getRecords(), ArgumentVO::new);
        BasePageVO<ArgumentVO> result = new BasePageVO<>();
        result.setTotals((int) page.getTotal());
        result.setTotalPages((int) page.getPages());
        result.setList(list);
        return result;
    }

    /**
     * 编辑参数
     *
     * @param argumentEditReqDTO 编辑参数 DTO
     * @return 数据库 id
     */
    @Override
    public Long editArgument(ArgumentEditReqDTO argumentEditReqDTO) {
        // 根据参数业务逐渐查询出信息
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getConfigKey, argumentEditReqDTO.getConfigKey())
        );
        if (sysArgument == null) {
            log.warn("SysArgumentServiceImplImpl.editArgument: [参数业务主键不存在: {} ]", argumentEditReqDTO);
            throw new ServiceException("参数业务主键不存在");
        }
        // 说明存在这个参数主键，但是名字不能重复，就是说再主键不同的情况下，name 相同了
        if (sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .ne(SysArgument::getConfigKey, argumentEditReqDTO.getConfigKey())
                .eq(SysArgument::getName, argumentEditReqDTO.getName())) != null
        ) {
            log.warn("SysArgumentServiceImplImpl.editArgument: [参数名称已存在: {} ]", argumentEditReqDTO);
            throw new ServiceException("参数名称已存在");
        }
        // 这里就说明可以修改了
        sysArgument.setName(argumentEditReqDTO.getName());
        sysArgument.setValue(argumentEditReqDTO.getValue());
        if (StringUtils.isNotBlank(argumentEditReqDTO.getRemark())) {
            sysArgument.setRemark(argumentEditReqDTO.getRemark());
        }
        sysArgumentMapper.updateById(sysArgument);
        return sysArgument.getId();
    }

    /*=============================================    远程调用    =============================================*/

    /**
     * 根据参数键查询参数对象
     *
     * @param configKey 参数键
     * @return 参数对象
     */
    @Override
    public ArgumentDTO getByConfigKey(String configKey) {
        // 直接查数据库就好了
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getConfigKey, configKey)
        );
        ArgumentDTO argumentDTO = new ArgumentDTO();
        if (sysArgument != null){
            BeanCopyUtil.copyProperties(sysArgument, argumentDTO);
        }
        return argumentDTO;
    }

    /**
     * 根据多个参数键查询多个参数对象
     *
     * @param configKeys 多个参数键
     * @return 多个参数对象
     */
    @Override
    public List<ArgumentDTO> getByConfigKeys(List<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysArgument> sysArguments = sysArgumentMapper.selectList(new LambdaQueryWrapper<SysArgument>()
                .in(SysArgument::getConfigKey, configKeys)
        );
        List<ArgumentDTO> result = BeanCopyUtil.copyListProperties(sysArguments, ArgumentDTO::new);
        return result;
    }

}
