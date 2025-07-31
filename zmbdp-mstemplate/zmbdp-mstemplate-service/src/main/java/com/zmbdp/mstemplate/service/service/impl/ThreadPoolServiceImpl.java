package com.zmbdp.mstemplate.service.service.impl;

import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.mstemplate.service.service.ThreadPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ThreadPoolServiceImpl implements ThreadPoolService {

    @Async(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME) // 通过这个注解, 可以让这个方法或者类运行在异步线程中, 里面写的 bean 名称是我申请创建的 bean 的名称
    @Override
    public void info() {
        log.info("ThreadPoolServiceImpl thread name: {}", Thread.currentThread().getName());
    }
}
