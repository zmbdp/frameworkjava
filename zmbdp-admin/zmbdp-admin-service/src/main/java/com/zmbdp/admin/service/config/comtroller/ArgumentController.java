package com.zmbdp.admin.service.config.comtroller;

import com.zmbdp.admin.api.config.domain.dto.ArgumentAddReqDTO;
import com.zmbdp.admin.api.config.frign.ArgumentServiceApi;
import com.zmbdp.admin.service.config.service.IArgumentService;
import com.zmbdp.common.domain.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 参数服务
 *
 * @author 稚名不带撇
 */
@RestController
@RequestMapping("/argument")
public class ArgumentController implements ArgumentServiceApi {

    @Autowired
    private IArgumentService argumentService;

    /**
     * 新增参数
     *
     * @param argumentAddReqDTO 新增参数请求 DTO
     * @return 数据库的 id
     */
    @PostMapping("/add")
    public Result<Long> addArgument(@RequestBody @Validated ArgumentAddReqDTO argumentAddReqDTO) {
        return Result.success(argumentService.addArgument(argumentAddReqDTO));
    }
}
