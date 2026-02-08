package com.zmbdp.common.log.router;

import com.zmbdp.common.domain.constants.LogConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 日志配置路由器
 * <p>
 * 负责从配置中心（Nacos）读取日志相关的全局配置开关和默认值。<br>
 * 支持动态配置更新，无需重启应用。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *     <li>读取日志总开关：控制是否启用日志记录功能</li>
 *     <li>读取全局记录开关：控制是否启用全局默认日志记录</li>
 *     <li>读取异步开关：控制是否使用异步方式记录日志</li>
 *     <li>动态配置支持：支持从 Nacos 动态读取配置</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private LogConfigRouter logConfigRouter;
 *
 * // 检查日志功能是否启用
 * if (logConfigRouter.isLogEnabled()) {
 *     // 记录日志
 * }
 *
 * // 检查全局记录是否启用
 * if (logConfigRouter.isGlobalRecordEnabled()) {
 *     // 记录全局默认日志
 * }
 *
 * // 检查是否使用异步记录
 * if (logConfigRouter.isAsyncEnabled()) {
 *     // 异步记录日志
 * } else {
 *     // 同步记录日志
 * }
 * }</pre>
 * <p>
 * <b>Nacos 配置项：</b>
 * <ul>
 *     <li><b>log.enabled</b>：日志总开关（默认 true）
 *         <ul>
 *             <li>true：启用日志记录功能</li>
 *             <li>false：禁用日志记录功能（所有日志都不记录）</li>
 *         </ul>
 *     </li>
 *     <li><b>log.global-record.enabled</b>：全局记录开关（默认 false）
 *         <ul>
 *             <li>true：启用全局默认日志记录（所有 Controller、Service 方法都记录）</li>
 *             <li>false：禁用全局默认日志记录（仅记录带 @LogAction 注解的方法）</li>
 *         </ul>
 *     </li>
 *     <li><b>log.async.enabled</b>：异步记录开关（默认 true）
 *         <ul>
 *             <li>true：使用异步方式记录日志（不阻塞业务线程）</li>
 *             <li>false：使用同步方式记录日志（可能影响性能）</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * # Nacos 配置
 * log:
 *   enabled: true                    # 启用日志记录
 *   global-record:
 *     enabled: false                 # 禁用全局记录（仅记录带注解的方法）
 *   async:
 *     enabled: true                  # 启用异步记录
 * }</pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>配置支持动态更新，无需重启应用</li>
 *     <li>如果配置不存在，使用默认值</li>
 *     <li>日志总开关优先级最高，关闭后所有日志都不记录</li>
 *     <li>全局记录开关仅影响无注解的方法，不影响带注解的方法</li>
 *     <li>异步记录建议在生产环境启用，避免影响业务性能</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see LogConstants
 * @see Environment
 */
@Slf4j
@Component
public class LogConfigRouter {

    /**
     * Spring 环境对象
     * <p>
     * 用于从配置中心（Nacos）读取配置。
     */
    @Autowired
    private Environment environment;

    /**
     * 检查日志功能是否启用
     * <p>
     * 读取 Nacos 配置项 log.enabled，判断是否启用日志记录功能。<br>
     * 如果配置不存在，使用默认值 true。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>在切面中判断是否需要记录日志</li>
     *     <li>在日志处理流程开始前进行快速判断</li>
     *     <li>支持动态开关日志功能，无需重启应用</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * if (!logConfigRouter.isLogEnabled()) {
     *     // 日志功能已关闭，直接返回
     *     return joinPoint.proceed();
     * }
     * // 继续记录日志
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此开关优先级最高，关闭后所有日志都不记录</li>
     *     <li>建议在生产环境保持开启，仅在特殊情况下关闭</li>
     *     <li>配置支持动态更新，无需重启应用</li>
     * </ul>
     *
     * @return true 表示启用日志记录，false 表示禁用
     * @see LogConstants#NACOS_LOG_ENABLED_PREFIX
     * @see LogConstants#LOG_ENABLED_DEFAULT
     */
    public boolean isLogEnabled() {
        return getBool(LogConstants.NACOS_LOG_ENABLED_PREFIX, LogConstants.LOG_ENABLED_DEFAULT);
    }

    /**
     * 检查全局记录是否启用
     * <p>
     * 读取 Nacos 配置项 log.global-record.enabled，判断是否启用全局默认日志记录。<br>
     * 如果配置不存在，使用默认值 false。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>在切面中判断是否需要记录无注解方法的日志</li>
     *     <li>控制全局默认日志记录的开关</li>
     *     <li>支持动态开关全局记录功能，无需重启应用</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * if (!logConfigRouter.isGlobalRecordEnabled()) {
     *     // 全局记录已关闭，仅记录带注解的方法
     *     return joinPoint.proceed();
     * }
     * // 记录全局默认日志
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>此开关仅影响无注解的方法，不影响带 @LogAction 注解的方法</li>
     *     <li>建议在开发环境关闭，避免产生大量无用日志</li>
     *     <li>建议在生产环境根据需要开启，便于问题排查</li>
     *     <li>配置支持动态更新，无需重启应用</li>
     * </ul>
     *
     * @return true 表示启用全局记录，false 表示禁用
     * @see LogConstants#NACOS_LOG_GLOBAL_RECORD_ENABLED_PREFIX
     * @see LogConstants#GLOBAL_RECORD_ENABLED_DEFAULT
     */
    public boolean isGlobalRecordEnabled() {
        return getBool(LogConstants.NACOS_LOG_GLOBAL_RECORD_ENABLED_PREFIX, LogConstants.GLOBAL_RECORD_ENABLED_DEFAULT);
    }

    /**
     * 检查异步记录是否启用
     * <p>
     * 读取 Nacos 配置项 log.async.enabled，判断是否使用异步方式记录日志。<br>
     * 如果配置不存在，使用默认值 true。
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *     <li>在保存日志前判断使用同步还是异步方式</li>
     *     <li>控制日志记录的性能影响</li>
     *     <li>支持动态切换同步/异步模式，无需重启应用</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * if (logConfigRouter.isAsyncEnabled()) {
     *     // 异步记录日志
     *     saveLogAsync(logDTO, storageService);
     * } else {
     *     // 同步记录日志
     *     storageService.save(logDTO);
     * }
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>异步记录不会阻塞业务线程，建议在生产环境启用</li>
     *     <li>同步记录可能影响业务性能，仅在调试时使用</li>
     *     <li>异步记录可能导致日志丢失（如应用突然关闭）</li>
     *     <li>配置支持动态更新，无需重启应用</li>
     * </ul>
     *
     * @return true 表示使用异步记录，false 表示使用同步记录
     * @see LogConstants#NACOS_LOG_ASYNC_ENABLED_PREFIX
     * @see LogConstants#LOG_ASYNC_ENABLED_DEFAULT
     */
    public boolean isAsyncEnabled() {
        return getBool(LogConstants.NACOS_LOG_ASYNC_ENABLED_PREFIX, LogConstants.LOG_ASYNC_ENABLED_DEFAULT);
    }

    /**
     * 从配置中心读取布尔值配置
     * <p>
     * 从 Spring Environment 中读取指定键的布尔值配置。<br>
     * 如果配置不存在或无法解析，返回默认值。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>配置值会自动转换为 Boolean 类型</li>
     *     <li>如果配置值无法转换为 Boolean，返回 null</li>
     *     <li>如果配置值为 null，使用默认值</li>
     * </ul>
     *
     * @param key          配置键，不能为空
     * @param defaultValue 默认值
     * @return 配置值，如果不存在则返回默认值
     */
    private boolean getBool(String key, boolean defaultValue) {
        Boolean v = environment.getProperty(key, Boolean.class);
        return v != null ? v : defaultValue;
    }
}