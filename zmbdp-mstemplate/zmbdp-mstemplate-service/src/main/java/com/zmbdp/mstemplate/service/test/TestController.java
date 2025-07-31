package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
import com.zmbdp.common.domain.exception.ServiceException;
import com.zmbdp.mstemplate.service.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {
    // /test/info
    @GetMapping("/info")
    public void info() {
        log.info("接口调用测试");
    }

    @GetMapping("/result")
    public Result<Void> result(int id) {
        if (id < 0) {
            return Result.fail();
        }
        return Result.success();
    }

    @GetMapping("/resultUser")
    public Result<User> resultId(int id) {
        if (id < 0) {
            return Result.fail(ResultCode.TOKEN_CHECK_FAILED.getCode(), ResultCode.TOKEN_CHECK_FAILED.getErrMsg());
        }
        User user = new User();
        user.setAge(50);
        user.setName("张三");
        return Result.success(user);
    }

    @GetMapping("/exception")
    public Result<Void> exception(int id) {
        if (id < 0) {
            throw new ServiceException(ResultCode.INVALID_CODE);
        }
        if (id == 1) {
            throw new ServiceException("id不能为1");
        }
        if (id == 1000) {
            throw new ServiceException("id不能为1000", ResultCode.ERROR_PHONE_FORMAT.getCode());
        }
        return Result.success();
    }
}