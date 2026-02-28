package com.zmbdp.mstemplate.service.service.impl;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.mstemplate.service.service.ThreadPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 线程池服务实现类
 *
 * @author 稚名不带撇
 */
@Slf4j
@Service
public class ThreadPoolServiceImpl implements ThreadPoolService {

    /**
     * 打印线程池信息
     * 使用异步线程池执行
     */
    @Async(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
    @Override
    public void info() {
        log.info("ThreadPoolServiceImpl thread name: {}", Thread.currentThread().getName());
    }
}