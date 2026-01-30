package com.zmbdp.common.log.config;

import com.zmbdp.common.log.aspect.LogActionAspect;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 日志自动配置类
 * <p>
 * 自动配置日志功能相关的 Bean，包括切面和存储服务扫描。
 * <p>
 * <b>自动配置内容：</b>
 * <ul>
 *     <li>{@link LogActionAspect}：操作日志切面</li>
 *     <li>所有 {@link com.zmbdp.common.log.service.ILogStorageService} 实现类：日志存储服务</li>
 *     <li>{@link com.zmbdp.common.log.mapper.OperationLogMapper}：操作日志 Mapper（仅在存在数据源时扫描）</li>
 * </ul>
 * <p>
 * <b>存储服务说明：</b>
 * <ul>
 *     <li>存储服务都会注册为 Bean，运行时根据配置或注解动态选择</li>
 *     <li>支持的存储类型：console（默认）、database、file、redis、mq</li>
 *     <li>可通过 Nacos 配置 {@code log.storage-type} 或注解 {@code @LogAction(storageType)} 指定</li>
 *     <li>优先级：方法注解 > 类注解 > Nacos 全局配置</li>
 *     <li>存储服务 Bean 名称：consoleLogStorageService、databaseLogStorageService、fileLogStorageService、redisLogStorageService、mqLogStorageService</li>
 * </ul>
 * <p>
 * <b>Mapper 扫描说明：</b>
 * <ul>
 *     <li>Mapper 扫描仅在存在 {@link SqlSessionFactory} Bean 时生效</li>
 *     <li>如果应用没有配置数据源，Mapper 不会被扫描，但不会影响其他功能</li>
 *     <li>数据库存储服务在没有 Mapper 时会优雅降级，只记录警告日志</li>
 * </ul>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Configuration
@ComponentScan("com.zmbdp.common.log.service.impl")
public class LogAutoConfiguration {

    /**
     * 创建操作日志切面
     *
     * @return 操作日志切面
     */
    @Bean
    public LogActionAspect logActionAspect() {
        log.info("初始化操作日志切面");
        return new LogActionAspect();
    }

    /**
     * Mapper 扫描配置（仅在存在数据源时生效）
     * <p>
     * 当应用配置了数据源时，自动扫描并注册 {@link com.zmbdp.common.log.mapper.OperationLogMapper}。
     * 如果应用没有配置数据源（如文件服务、网关服务），此配置不会生效，避免启动失败。
     *
     * @author 稚名不带撇
     */
    @Slf4j
    @Configuration
    @ConditionalOnBean(SqlSessionFactory.class)
    @MapperScan("com.zmbdp.common.log.mapper")
    static class LogMapperConfiguration {
        public LogMapperConfiguration() {
            log.info("初始化操作日志 Mapper 扫描（检测到数据源配置）");
        }
    }
}
