package com.zmbdp.common.log.aspect;

import com.zmbdp.common.core.utils.LogExceptionUtil;
import com.zmbdp.common.core.utils.StringUtil;
import com.zmbdp.common.domain.constants.CommonConstants;
import com.zmbdp.common.log.annotation.LogAction;
import com.zmbdp.common.log.domain.dto.OperationLogDTO;
import com.zmbdp.common.log.router.LogConfigRouter;
import com.zmbdp.common.log.router.LogStorageRouter;
import com.zmbdp.common.log.service.ILogStorageService;
import com.zmbdp.common.log.strategy.impl.LogAnnotationMergeStrategy;
import com.zmbdp.common.log.strategy.impl.LogConditionEvaluateStrategy;
import com.zmbdp.common.log.strategy.impl.LogDtoBuildStrategy;
import com.zmbdp.common.log.strategy.impl.LogParamExtractStrategy;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * <p>
 * 拦截 {@link LogAction} 注解方法及全局默认记录，委托各策略组件和路由器完成上下文构建、条件评估、存储路由等。<br>
 * 职责仅限：拦截 → 编排调用 → 持久化。
 * <p>
 * <b>核心职责：</b>
 * <ul>
 *     <li>拦截带 @LogAction 注解的方法</li>
 *     <li>拦截全局默认记录的方法（Controller、Service）</li>
 *     <li>编排调用各策略组件完成日志处理</li>
 *     <li>异步或同步持久化日志</li>
 * </ul>
 * <p>
 * <b>依赖组件：</b>
 * <ul>
 *     <li>{@link LogDtoBuildStrategy}：构建日志 DTO</li>
 *     <li>{@link LogParamExtractStrategy}：提取方法参数和返回值</li>
 *     <li>{@link LogConditionEvaluateStrategy}：评估条件表达式</li>
 *     <li>{@link LogStorageRouter}：路由到存储服务</li>
 *     <li>{@link LogAnnotationMergeStrategy}：合并注解配置</li>
 *     <li>{@link LogConfigRouter}：读取全局配置</li>
 * </ul>
 *
 * @author 稚名不带撇
 * @see LogDtoBuildStrategy
 * @see LogConditionEvaluateStrategy
 * @see LogParamExtractStrategy
 * @see LogStorageRouter
 * @see LogAnnotationMergeStrategy
 * @see LogConfigRouter
 */
@Slf4j
@Aspect
@Component
@RefreshScope
public class LogActionAspect {

    /**
     * 日志 DTO 构建策略
     * <p>
     * 负责构建操作日志 DTO，包括基本信息（方法名、IP、时间等）和注解配置信息。
     */
    @Autowired
    private LogDtoBuildStrategy logDtoBuildStrategy;

    /**
     * 日志参数提取策略
     * <p>
     * 负责提取方法参数和返回值，支持 SpEL 表达式和敏感字段脱敏。
     */
    @Autowired
    private LogParamExtractStrategy logParamExtractStrategy;

    /**
     * 日志条件评估策略
     * <p>
     * 负责评估条件表达式（condition），决定是否记录日志。
     */
    @Autowired
    private LogConditionEvaluateStrategy logConditionEvaluateStrategy;

    /**
     * 日志存储路由器
     * <p>
     * 负责根据注解配置或全局配置路由到合适的存储服务（console、database、file、redis、mq）。
     */
    @Autowired
    private LogStorageRouter logStorageRouter;

    /**
     * 日志注解合并策略
     * <p>
     * 负责合并方法注解和类注解配置，方法注解优先级更高。
     */
    @Autowired
    private LogAnnotationMergeStrategy logAnnotationMergeStrategy;

    /**
     * 日志配置路由器
     * <p>
     * 负责从 Nacos 读取全局配置（是否启用日志、是否异步、是否全局记录等）。
     */
    @Autowired
    private LogConfigRouter logConfigRouter;


    /**
     * 定义切点：拦截带有 @LogAction 注解的方法或类
     * <p>
     * 匹配规则：
     * <ul>
     *     <li>方法上标注了 @LogAction 注解</li>
     *     <li>类上标注了 @LogAction 注解（类中所有方法都会被拦截）</li>
     * </ul>
     */
    @Pointcut("@annotation(com.zmbdp.common.log.annotation.LogAction) || @within(com.zmbdp.common.log.annotation.LogAction)")
    public void logActionPointcut() {
    }

    /**
     * 定义切点：全局默认记录（Controller、Service 的所有 public 方法）
     * <p>
     * 匹配规则：
     * <ul>
     *     <li>类上标注了 @RestController、@Controller 或 @Service 注解</li>
     *     <li>方法是 public 的</li>
     *     <li>排除日志存储服务实现类（避免递归）</li>
     *     <li>排除切面类本身（避免递归）</li>
     * </ul>
     * <p>
     * <b>注意：</b>需要在 Nacos 中配置 {@code log.global-record-enabled=true} 才会生效
     */
    @Pointcut(
            "(@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.stereotype.Service)) && " +
            "execution(public * *(..)) && " +
            "!within(com.zmbdp.common.log.service.impl..*) && " +
                    "!within(com.zmbdp.common.log.aspect.LogActionAspect)"
    )
    public void globalRecordPointcut() {
    }

    /**
     * 环绕通知：处理带有 @LogAction 注解的方法
     * <p>
     * 拦截所有标注了 @LogAction 注解的方法，记录操作日志。
     *
     * @param joinPoint 切点对象
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("logActionPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return processLogAction(joinPoint, true);
    }

    /**
     * 环绕通知：全局默认记录（不需要注解）
     * <p>
     * 拦截所有 Controller、Service 的 public 方法，记录基本日志信息。<br>
     * 需要在 Nacos 中配置 {@code log.global-record-enabled=true} 才会生效。
     *
     * @param joinPoint 切点对象
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("globalRecordPointcut() && !logActionPointcut()")
    public Object globalRecord(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!logConfigRouter.isGlobalRecordEnabled()) {
            return joinPoint.proceed();
        }
        if (getMethodAnnotation(joinPoint) != null) {
            return joinPoint.proceed();
        }
        return processGlobalRecord(joinPoint);
    }

    /**
     * 处理带有 @LogAction 注解的方法
     * <p>
     * 核心处理流程：
     * <ol>
     *     <li>合并方法注解和类注解配置</li>
     *     <li>检查日志功能是否启用</li>
     *     <li>构建日志 DTO 基本信息</li>
     *     <li>提取方法参数（如果配置了 recordParams）</li>
     *     <li>执行目标方法</li>
     *     <li>提取方法返回值（如果配置了 recordResult）</li>
     *     <li>评估条件表达式（如果配置了 condition）</li>
     *     <li>记录异常信息（如果发生异常且配置了 recordException）</li>
     *     <li>保存日志（异步或同步）</li>
     * </ol>
     *
     * @param joinPoint         切点对象
     * @param requireAnnotation 是否要求必须有注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    private Object processLogAction(ProceedingJoinPoint joinPoint, boolean requireAnnotation) throws Throwable {
        LogAction methodLogAction = getMethodAnnotation(joinPoint);
        LogAction classLogAction = getClassAnnotation(joinPoint);

        if (requireAnnotation && methodLogAction == null) {
            return joinPoint.proceed();
        }

        LogAction logAction = logAnnotationMergeStrategy.merge(methodLogAction, classLogAction);

        if (logAction == null || StringUtil.isEmpty(logAction.value())) {
            return joinPoint.proceed();
        }
        if (!logConfigRouter.isLogEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        OperationLogDTO logDTO = logDtoBuildStrategy.buildFromAnnotation(joinPoint, logAction);

        if (logAction.recordParams()) {
            try {
                logDTO.setParams(logParamExtractStrategy.extractParams(joinPoint, logAction));
            } catch (Exception e) {
                log.warn("提取方法参数失败: {}", e.getMessage());
            }
        }

        // 注意：条件评估移到方法执行后，确保 #result 变量可用
        Object result = null;
        boolean shouldSaveLog = true;  // 标志：是否应该保存日志
        try {
            result = joinPoint.proceed();
            logDTO.setStatus(CommonConstants.STATUS_SUCCESS);

            if (logAction.recordResult()) {
                try {
                    logDTO.setResult(logParamExtractStrategy.extractResult(result, logAction, joinPoint));
                } catch (Exception e) {
                    log.warn("提取方法返回值失败: {}", e.getMessage());
                }
            }

            if (StringUtil.isNotEmpty(logAction.condition())) {
                try {
                    if (!logConditionEvaluateStrategy.shouldRecord(joinPoint, logAction.condition(), result)) {
                        shouldSaveLog = false;  // 条件不满足，不保存日志
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("执行条件表达式失败: {}, 继续记录日志", e.getMessage());
                }
            }
        } catch (Throwable e) {
            logDTO.setStatus(CommonConstants.STATUS_FAILED);
            if (logAction.recordException()) {
                logDTO.setException(e.getClass().getName() + ": " + e.getMessage());
                logDTO.setExceptionStack(LogExceptionUtil.getStackTrace(e));
            }
            if (logAction.throwException()) {
                throw e;
            }
        } finally {
            if (shouldSaveLog) {  // 只有在条件满足时才保存日志
                logDTO.setCostTime(System.currentTimeMillis() - startTime);
                logDTO.setOperationTime(LocalDateTime.now());
                saveLog(logDTO, logAction);
            }
        }

        return result;
    }

    /**
     * 处理全局默认记录（不需要注解）
     * <p>
     * 简化的日志记录流程：
     * <ol>
     *     <li>检查日志功能是否启用</li>
     *     <li>构建基本日志信息（方法名、IP、耗时等）</li>
     *     <li>执行目标方法</li>
     *     <li>记录执行状态和异常信息</li>
     *     <li>保存日志（异步或同步）</li>
     * </ol>
     * <p>
     * <b>注意：</b>全局记录不会记录参数和返回值，只记录基本信息
     *
     * @param joinPoint 切点对象
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    private Object processGlobalRecord(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!logConfigRouter.isLogEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        OperationLogDTO logDTO = logDtoBuildStrategy.buildBasic(joinPoint);

        try {
            Object result = joinPoint.proceed();
            logDTO.setStatus(CommonConstants.STATUS_SUCCESS);
            return result;
        } catch (Throwable e) {
            logDTO.setStatus(CommonConstants.STATUS_FAILED);
            logDTO.setException(e.getClass().getName() + ": " + e.getMessage());
            logDTO.setExceptionStack(LogExceptionUtil.getStackTrace(e));
            throw e;
        } finally {
            logDTO.setCostTime(System.currentTimeMillis() - startTime);
            logDTO.setOperationTime(LocalDateTime.now());
            ILogStorageService storage = logStorageRouter.route(null);
            if (logConfigRouter.isAsyncEnabled()) {
                saveLogAsync(logDTO, storage);
            } else {
                storage.save(logDTO);
            }
        }
    }

    /**
     * 保存操作日志
     * <p>
     * 根据注解配置路由到对应的存储服务，支持异步或同步保存。
     * <p>
     * <b>存储方式：</b>
     * <ul>
     *     <li>console：控制台输出（默认）</li>
     *     <li>database：数据库存储</li>
     *     <li>file：文件存储</li>
     *     <li>redis：Redis 存储</li>
     *     <li>mq：消息队列存储</li>
     * </ul>
     * <p>
     * <b>异步/同步：</b>
     * <ul>
     *     <li>通过 Nacos 配置 {@code log.async-enabled} 控制</li>
     *     <li>异步保存不会阻塞业务线程，提高性能</li>
     * </ul>
     *
     * @param logDTO    操作日志 DTO
     * @param logAction 日志注解配置
     */
    private void saveLog(OperationLogDTO logDTO, LogAction logAction) {
        try {
            ILogStorageService storage = logStorageRouter.route(logAction);
            if (logConfigRouter.isAsyncEnabled()) {
                saveLogAsync(logDTO, storage);
            } else {
                storage.save(logDTO);
            }
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步保存操作日志
     * <p>
     * 使用 Spring 的 @Async 注解实现异步保存，不会阻塞业务线程。
     * <p>
     * <b>线程池配置：</b>
     * <ul>
     *     <li>使用 {@link CommonConstants#ASYNCHRONOUS_THREADS_BEAN_NAME} 线程池</li>
     *     <li>线程池配置在 {@code zmbdp-common-core} 模块中</li>
     * </ul>
     *
     * @param logDTO         操作日志 DTO
     * @param storageService 存储服务
     */
    @Async(CommonConstants.ASYNCHRONOUS_THREADS_BEAN_NAME)
    public void saveLogAsync(OperationLogDTO logDTO, ILogStorageService storageService) {
        storageService.save(logDTO);
    }

    /**
     * 获取方法上的 @LogAction 注解
     *
     * @param joinPoint 切点对象
     * @return LogAction 注解，如果不存在则返回 null
     */
    private LogAction getMethodAnnotation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return method.getAnnotation(LogAction.class);
    }

    /**
     * 获取类上的 @LogAction 注解
     *
     * @param joinPoint 切点对象
     * @return LogAction 注解，如果不存在则返回 null
     */
    private LogAction getClassAnnotation(ProceedingJoinPoint joinPoint) {
        return joinPoint.getTarget().getClass().getAnnotation(LogAction.class);
    }
}