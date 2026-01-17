package com.zmbdp.mstemplate.service.service;

import com.zmbdp.common.domain.domain.Result;
import com.zmbdp.common.idempotent.annotation.Idempotent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 幂等性测试服务类
 * 用于测试Service层方法的幂等性功能
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class IdempotentTestService {

    /**
     * 测试Service层方法的幂等性
     * 从HTTP请求头获取Token
     */
    @Idempotent(
            headerName = "Idempotent-Token",
            expireTime = 300,
            message = "请勿重复提交"
    )
    public Result<String> testIdempotentMethod() {
        log.info("=== Service层方法执行 - 幂等性测试 ===");
        // 模拟业务逻辑
        return Result.success("Service层方法执行成功");
    }
}
