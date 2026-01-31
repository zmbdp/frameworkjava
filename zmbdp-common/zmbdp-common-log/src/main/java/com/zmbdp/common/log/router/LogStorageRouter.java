package com.zmbdp.common.log.router;

import com.zmbdp.common.domain.constants.LogConstants;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.service.ILogStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 日志存储路由器
 * <p>
 * 采用策略模式设计，负责根据注解配置或全局配置自动路由到合适的日志存储服务。<br>
 * 支持多种存储方式（控制台、数据库、文件、Redis、消息队列等）。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li><b>策略路由：</b>根据存储类型自动选择合适的存储服务</li>
 *     <li><b>优先级控制：</b>方法注解 > 类注解 > Nacos 全局配置 > 默认（console）</li>
 *     <li><b>动态配置支持：</b>支持从 Nacos 动态读取存储类型配置</li>
 *     <li><b>异常容错处理：</b>存储服务不存在时使用默认服务</li>
 * </ul>
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *     <li>从注解中获取存储类型（storageType）</li>
 *     <li>如果注解未配置，从 Nacos 全局配置中获取</li>
 *     <li>如果全局配置也未配置，使用默认存储类型（console）</li>
 *     <li>根据存储类型构建 Bean 名称（如 database → databaseLogStorageService）</li>
 *     <li>从 Spring 容器中获取对应的存储服务 Bean</li>
 *     <li>如果 Bean 不存在，使用默认存储服务（consoleLogStorageService）</li>
 *     <li>返回存储服务实例</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @Autowired
 * private LogStorageRouter logStorageRouter;
 *
 * // 根据注解路由到存储服务
 * ILogStorageService storageService = logStorageRouter.route(logAction);
 * storageService.save(logDTO);
 *
 * // 使用全局配置路由
 * ILogStorageService storageService = logStorageRouter.route(null);
 * storageService.save(logDTO);
 * }</pre>
 * <p>
 * <b>支持的存储类型：</b>
 * <ul>
 *     <li><b>console</b>：控制台输出（默认，适合开发环境）</li>
 *     <li><b>database</b>：数据库存储（适合生产环境，便于查询）</li>
 *     <li><b>file</b>：文件存储（适合日志量大的场景）</li>
 *     <li><b>redis</b>：Redis 存储（适合临时存储和快速查询）</li>
 *     <li><b>mq</b>：消息队列存储（适合异步处理和解耦）</li>
 * </ul>
 * <p>
 * <b>配置示例：</b>
 * <pre>{@code
 * # Nacos 全局配置
 * log:
 *   storage-type: database    # 全局默认使用数据库存储
 *
 * # 注解配置（优先级更高）
 * @LogAction(value = "用户登录", storageType = "mq")
 * public Result login(LoginDTO loginDTO) { ... }
 * }</pre>
 * <p>
 * <b>扩展性：</b>
 * <ul>
 *     <li>如需支持新的存储方式，只需实现 {@link ILogStorageService} 接口</li>
 *     <li>Bean 名称规则：存储类型首字母大写 + "LogStorageService"（如 database → DatabaseLogStorageService）</li>
 *     <li>Spring 会自动注入新的存储服务实现，无需修改路由器代码</li>
 *     <li>符合开闭原则：对扩展开放，对修改关闭</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>所有存储服务实现类必须标注 {@code @Service} 或 {@code @Component} 注解</li>
 *     <li>Bean 名称必须符合命名规则（首字母小写的类名）</li>
 *     <li>如果存储服务不存在，会记录警告日志并使用默认服务</li>
 *     <li>默认存储服务（consoleLogStorageService）必须存在</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see ILogStorageService
 * @see com.zmbdp.common.log.service.impl.ConsoleLogStorageService
 * @see com.zmbdp.common.log.service.impl.DatabaseLogStorageService
 * @see com.zmbdp.common.log.service.impl.FileLogStorageService
 * @see com.zmbdp.common.log.service.impl.RedisLogStorageService
 * @see com.zmbdp.common.log.service.impl.MqLogStorageService
 */
@Slf4j
@Component
public class LogStorageRouter {

    /**
     * 默认存储服务（控制台输出）
     * <p>
     * 当无法找到指定的存储服务时，使用此默认服务。<br>
     * 适合开发环境，直接输出到控制台。
     */
    @Autowired
    @Qualifier("consoleLogStorageService")
    private ILogStorageService defaultStorageService;

    /**
     * Spring 应用上下文
     * <p>
     * 用于动态获取存储服务 Bean。
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Spring 环境对象
     * <p>
     * 用于从配置中心（Nacos）读取全局存储类型配置。
     */
    @Autowired
    private Environment environment;

    /**
     * 路由到合适的存储服务
     * <p>
     * 根据注解配置或全局配置选择合适的日志存储服务。<br>
     * 优先级：方法注解 > 类注解 > Nacos 全局配置 > 默认（console）。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *     <li>从注解中获取存储类型（storageType）</li>
     *     <li>如果注解未配置，从 Nacos 全局配置中获取（log.storage-type）</li>
     *     <li>如果全局配置也未配置，使用默认存储类型（console）</li>
     *     <li>根据存储类型构建 Bean 名称</li>
     *     <li>从 Spring 容器中获取对应的存储服务 Bean</li>
     *     <li>如果 Bean 不存在，记录警告日志并使用默认存储服务</li>
     *     <li>返回存储服务实例</li>
     * </ol>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 使用注解配置的存储类型
     * @LogAction(value = "用户登录", storageType = "database")
     * public Result login(LoginDTO loginDTO) { ... }
     * ILogStorageService storageService = logStorageRouter.route(logAction);
     * // 返回 databaseLogStorageService
     *
     * // 使用全局配置的存储类型
     * ILogStorageService storageService = logStorageRouter.route(null);
     * // 返回 Nacos 配置的存储服务，如果未配置则返回 consoleLogStorageService
     * }</pre>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>logAction 可以为 null，表示使用全局配置</li>
     *     <li>如果存储服务不存在，会记录警告日志并使用默认服务</li>
     *     <li>Bean 名称必须符合命名规则（首字母小写的类名）</li>
     *     <li>默认存储服务（consoleLogStorageService）必须存在</li>
     * </ul>
     *
     * @param logAction 合并后的注解（可为 null，则使用全局配置）
     * @return 存储服务实例，不会返回 null
     * @see #buildBeanName(String)
     * @see LogConstants#NACOS_LOG_STORAGE_TYPE_PREFIX
     * @see LogConstants#STORAGE_TYPE_DEFAULT
     */
    public ILogStorageService route(LogAction logAction) {
        // 获取存储类型（优先级：注解 > 全局配置 > 默认）
        String storageType = null;
        if (logAction != null) {
            storageType = logAction.storageType();
        }
        if (StringUtil.isEmpty(storageType)) {
            storageType = environment.getProperty(
                    LogConstants.NACOS_LOG_STORAGE_TYPE_PREFIX,
                    String.class,
                    LogConstants.STORAGE_TYPE_DEFAULT
            );
        }
        
        // 如果是默认存储类型，直接返回默认服务
        if (StringUtil.isEmpty(storageType) || LogConstants.STORAGE_TYPE_DEFAULT.equals(storageType)) {
            return defaultStorageService;
        }

        // 根据存储类型构建 Bean 名称并获取存储服务
        String beanName = buildBeanName(storageType);
        try {
            if (applicationContext.containsBean(beanName)) {
                return applicationContext.getBean(beanName, ILogStorageService.class);
            }
            log.warn("存储服务 Bean 不存在，使用默认: storageType={}, beanName={}", storageType, beanName);
        } catch (Exception e) {
            log.warn("获取存储服务失败，使用默认: {}", e.getMessage());
        }
        return defaultStorageService;
    }

    /**
     * 根据存储类型构建 Bean 名称
     * <p>
     * 将存储类型转换为对应的 Bean 名称。<br>
     * 命名规则：存储类型首字母大写 + "LogStorageService"，然后首字母小写。
     * <p>
     * <b>转换示例：</b>
     * <ul>
     *     <li>console → ConsoleLogStorageService → consoleLogStorageService</li>
     *     <li>database → DatabaseLogStorageService → databaseLogStorageService</li>
     *     <li>file → FileLogStorageService → fileLogStorageService</li>
     *     <li>redis → RedisLogStorageService → redisLogStorageService</li>
     *     <li>mq → MqLogStorageService → mqLogStorageService</li>
     * </ul>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *     <li>存储类型不能为空</li>
     *     <li>Bean 名称首字母必须小写（Spring 默认规则）</li>
     *     <li>实现类的类名必须符合命名规则</li>
     * </ul>
     *
     * @param storageType 存储类型，不能为空
     * @return Bean 名称（首字母小写）
     */
    private String buildBeanName(String storageType) {
        // 构建类名：首字母大写 + LogStorageService
        String className = Character.toUpperCase(storageType.charAt(0)) + storageType.substring(1) + "LogStorageService";
        // 转换为 Bean 名称：首字母小写
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
}

