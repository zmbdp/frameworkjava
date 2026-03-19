package com.zmbdp.mstemplate.service.test;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB Handler 示例
 *
 * <p>本类演示了在微服务模板中如何接入 XXL-JOB 分布式调度。
 * 复制新业务服务时，参照此示例添加 {@link XxlJob} 注解即可完成执行器注册。
 *
 * <p><b>使用步骤：</b>
 * <ol>
 *   <li>在 pom.xml 中引入 {@code zmbdp-common-xxljob} 依赖；
 *   <li>在 bootstrap.yml 的 shared-configs 中加载 {@code share-xxljob-{env}.yaml}；
 *   <li>编写 {@code @XxlJob("handlerName")} 方法，方法名即为调度中心配置的 JobHandler；
 *   <li>在 XXL-JOB 管控台「执行器管理」中确认服务已自动注册；
 *   <li>在「任务管理」中新增任务，填写对应的 JobHandler 名称并配置 CRON 表达式。
 * </ol>
 *
 * @author 稚名不带撇
 */
@Slf4j
@Component
public class TestXxlJobController {

    /**
     * 简单任务示例
     *
     * <p>演示基本的任务执行、日志记录与成功/失败上报。
     * 在调度中心的「执行日志」页可以实时查看 {@link XxlJobHelper#log} 输出的内容。
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() {
        XxlJobHelper.log("[XXL-JOB] demoJobHandler 开始执行，参数: {}", XxlJobHelper.getJobParam());
        log.info("[XXL-JOB] demoJobHandler 开始执行");

        // ===== 在此处编写业务逻辑 =====
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("[XXL-JOB] 收到任务参数: {}", jobParam);
        log.info("[XXL-JOB] 收到任务参数: {}", jobParam);
        // ===== 业务逻辑结束 =====

        XxlJobHelper.handleSuccess("demoJobHandler 执行成功");
        log.info("[XXL-JOB] demoJobHandler 执行成功");
    }

    /**
     * 分片广播任务示例
     *
     * <p>当执行器集群部署时，XXL-JOB 会将任务分片分发给每个实例。
     * 每个实例通过 {@link XxlJobHelper#getShardIndex()} 获取自己的分片序号，
     * 通过 {@link XxlJobHelper#getShardTotal()} 获取总分片数，
     * 从而实现数据的并行处理（如：按用户 ID 取模分片处理）。
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("[XXL-JOB] 分片广播任务执行，分片序号: {}, 总分片数: {}", shardIndex, shardTotal);
        log.info("[XXL-JOB] 分片广播任务执行，分片序号: {}, 总分片数: {}", shardIndex, shardTotal);

        // ===== 分片处理示例（按 shardIndex 和 shardTotal 过滤数据） =====
        // List<Order> orders = orderService.listBySharding(shardIndex, shardTotal);
        // orders.forEach(order -> processOrder(order));
        // ===== 分片处理结束 =====

        XxlJobHelper.handleSuccess(String.format("分片 [%d/%d] 处理完成", shardIndex + 1, shardTotal));
    }
}
