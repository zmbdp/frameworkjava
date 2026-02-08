package com.zmbdp.common.monitor.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus 监控配置类
 *
 * <p>功能说明：
 * <ul>
 *   <li>配置 Prometheus 指标采集</li>
 *   <li>注册 JVM 相关指标（内存、GC、线程、类加载）</li>
 *   <li>注册系统相关指标（CPU、文件描述符）</li>
 *   <li>添加全局标签（应用名称、环境等）</li>
 * </ul>
 *
 * @author zmbdp
 * @since 2026-02-02
 */
@Configuration
public class PrometheusConfig {

    /**
     * 自定义 MeterRegistry，添加全局标签
     *
     * @return MeterRegistryCustomizer
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", "${spring.application.name:unknown}",
                        "environment", "${spring.profiles.active:dev}"
                );
    }

    /**
     * JVM 内存指标
     *
     * @return JvmMemoryMetrics
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM GC 指标
     *
     * @return JvmGcMetrics
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * JVM 线程指标
     *
     * @return JvmThreadMetrics
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * JVM 类加载指标
     *
     * @return ClassLoaderMetrics
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * 系统 CPU 指标
     *
     * @return ProcessorMetrics
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    /**
     * 文件描述符指标
     *
     * @return FileDescriptorMetrics
     */
    @Bean
    public FileDescriptorMetrics fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }
}