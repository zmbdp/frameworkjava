package com.zmbdp.mstemplate.service.test;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.domain.domain.ResultCode;
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
}