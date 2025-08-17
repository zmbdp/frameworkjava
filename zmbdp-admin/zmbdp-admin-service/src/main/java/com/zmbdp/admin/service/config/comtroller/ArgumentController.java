package com.zmbdp.admin.service.config.comtroller;

import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentEditReqDTO;
import com.zmbdp.admin.api.config.domain.dto.ArgumentListReqDTO;
import com.zmbdp.admin.api.config.domain.vo.ArgumentVO;
import com.zmbdp.admin.api.config.frign.ArgumentServiceApi;
import com.zmbdp.admin.service.config.service.ISysArgumentServiceImpl;
import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.vo.BasePageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 参数服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/argument")
public class ArgumentController implements ArgumentServiceApi {

    /**
     * 参数服务
     */
    @Autowired
    private ISysArgumentServiceImpl argumentService;

    /*=============================================    前端调用    =============================================*/

    /**
     * 新增参数
     *
     * @param argumentAddReqDTO 新增参数请求 DTO
     * @return 数据库 id
     */
    @PostMapping("/add")
    public Result<Long> addArgument(@RequestBody @Validated ArgumentAddReqDTO argumentAddReqDTO) {
        return Result.success(argumentService.addArgument(argumentAddReqDTO));
    }

    /**
     * 获取参数列表, 模糊查询 name
     *
     * @param argumentListReqDTO 查看参数 DTO
     * @return 符合要求的参数列表
     */
    @GetMapping("/list")
    public Result<BasePageVO<ArgumentVO>> listArgument(@Validated ArgumentListReqDTO argumentListReqDTO) {
        return Result.success(argumentService.listArgument(argumentListReqDTO));
    }

    /**
     * 编辑参数
     *
     * @param argumentEditReqDTO 编辑参数 DTO
     * @return 数据库 id
     */
    @PostMapping("/edit")
    public Result<Long> editArgument(@RequestBody @Validated ArgumentEditReqDTO argumentEditReqDTO) {
        return Result.success(argumentService.editArgument(argumentEditReqDTO));
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
        return argumentService.getByConfigKey(configKey);
    }

    /**
     * 根据多个参数键查询多个参数对象
     *
     * @param configKeys 多个参数键
     * @return 多个参数对象
     */
    @Override
    public List<ArgumentDTO> getByConfigKeys(List<String> configKeys) {
        return argumentService.getByConfigKeys(configKeys);
    }
}
