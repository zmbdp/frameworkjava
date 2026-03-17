-- ==================== XXL-Job 调度中心数据库初始化 ====================
USE `frameworkjava_xxljob_dev`;

SET NAMES utf8mb4;

-- ==================== 核心表 ====================

-- 任务信息表
CREATE TABLE `xxl_job_info`
(
    `id`                        INT(11)      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_group`                 INT(11)      NOT NULL COMMENT '执行器主键ID',
    `job_desc`                  VARCHAR(255) NOT NULL COMMENT '任务描述',
    `add_time`                  DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time`               DATETIME              DEFAULT NULL COMMENT '更新时间',
    `author`                    VARCHAR(64)           DEFAULT NULL COMMENT '作者',
    `alarm_email`               VARCHAR(255)          DEFAULT NULL COMMENT '报警邮件',
    `schedule_type`             VARCHAR(50)  NOT NULL DEFAULT 'NONE' COMMENT '调度类型：NONE-不调度，CRON-CRON表达式，FIX_RATE-固定速度',
    `schedule_conf`             VARCHAR(128)          DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
    `misfire_strategy`          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略：DO_NOTHING-忽略，FIRE_ONCE_NOW-立即执行一次',
    `executor_route_strategy`   VARCHAR(50)           DEFAULT NULL COMMENT '执行器路由策略：FIRST-第一个，LAST-最后一个，ROUND-轮询，RANDOM-随机等',
    `executor_handler`          VARCHAR(255)          DEFAULT NULL COMMENT '执行器任务handler名称',
    `executor_param`            VARCHAR(512)          DEFAULT NULL COMMENT '执行器任务参数',
    `executor_block_strategy`   VARCHAR(50)           DEFAULT NULL COMMENT '阻塞处理策略：SERIAL_EXECUTION-单机串行，DISCARD_LATER-丢弃后续调度，COVER_EARLY-覆盖之前调度',
    `executor_timeout`          INT(11)      NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
    `executor_fail_retry_count` INT(11)      NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `glue_type`                 VARCHAR(50)  NOT NULL COMMENT 'GLUE类型：BEAN-Spring Bean模式，GLUE_GROOVY-GLUE(Java)，GLUE_SHELL-GLUE(Shell)等',
    `glue_source`               MEDIUMTEXT COMMENT 'GLUE源代码',
    `glue_remark`               VARCHAR(128)          DEFAULT NULL COMMENT 'GLUE备注',
    `glue_updatetime`           DATETIME              DEFAULT NULL COMMENT 'GLUE更新时间',
    `child_jobid`               VARCHAR(255)          DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
    `trigger_status`            TINYINT(4)   NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
    `trigger_last_time`         BIGINT(13)   NOT NULL DEFAULT '0' COMMENT '上次调度时间',
    `trigger_next_time`         BIGINT(13)   NOT NULL DEFAULT '0' COMMENT '下次调度时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务信息表';

-- 任务日志表
CREATE TABLE `xxl_job_log`
(
    `id`                        BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_group`                 INT(11)      NOT NULL COMMENT '执行器主键ID',
    `job_id`                    INT(11)      NOT NULL COMMENT '任务主键ID',
    `executor_address`          VARCHAR(255) DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
    `executor_handler`          VARCHAR(255) DEFAULT NULL COMMENT '执行器任务handler',
    `executor_param`            VARCHAR(512) DEFAULT NULL COMMENT '执行器任务参数',
    `executor_sharding_param`   VARCHAR(20)  DEFAULT NULL COMMENT '执行器任务分片参数，格式如 1/2',
    `executor_fail_retry_count` INT(11)      NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `trigger_time`              DATETIME     DEFAULT NULL COMMENT '调度-时间',
    `trigger_code`              INT(11)      NOT NULL COMMENT '调度-结果',
    `trigger_msg`               TEXT COMMENT '调度-日志',
    `handle_time`               DATETIME     DEFAULT NULL COMMENT '执行-时间',
    `handle_code`               INT(11)      NOT NULL COMMENT '执行-状态',
    `handle_msg`                TEXT COMMENT '执行-日志',
    `alarm_status`              TINYINT(4)   NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    PRIMARY KEY (`id`),
    KEY                         `idx_trigger_time` (`trigger_time`),
    KEY                         `idx_handle_code` (`handle_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务日志表';

-- 任务日志报表（用于首页统计）
CREATE TABLE `xxl_job_log_report`
(
    `id`            INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `trigger_day`   DATETIME DEFAULT NULL COMMENT '调度-时间',
    `running_count` INT(11) NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
    `suc_count`     INT(11) NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
    `fail_count`    INT(11) NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
    `update_time`   DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_trigger_day` (`trigger_day`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务日志报表';

-- GLUE源码版本记录表
CREATE TABLE `xxl_job_logglue`
(
    `id`          INT(11)     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id`      INT(11)     NOT NULL COMMENT '任务主键ID',
    `glue_type`   VARCHAR(50) DEFAULT NULL COMMENT 'GLUE类型',
    `glue_source` MEDIUMTEXT COMMENT 'GLUE源代码',
    `glue_remark` VARCHAR(128) NOT NULL COMMENT 'GLUE备注',
    `add_time`    DATETIME    DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='GLUE源码版本记录表';

-- 执行器注册表
CREATE TABLE `xxl_job_registry`
(
    `id`             INT(11)      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `registry_group` VARCHAR(50)  NOT NULL COMMENT '注册组：EXECUTOR-执行器',
    `registry_key`   VARCHAR(255) NOT NULL COMMENT '注册key：执行器AppName',
    `registry_value` VARCHAR(255) NOT NULL COMMENT '注册value：执行器地址',
    `update_time`    DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY              `idx_g_k_v` (`registry_group`, `registry_key`, `registry_value`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='执行器注册表';

-- 执行器配置表
CREATE TABLE `xxl_job_group`
(
    `id`           INT(11)     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_name`     VARCHAR(64) NOT NULL COMMENT '执行器AppName',
    `title`        VARCHAR(12) NOT NULL COMMENT '执行器名称',
    `address_type` TINYINT(4)  NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    `address_list` TEXT COMMENT '执行器地址列表，多地址逗号分隔',
    `update_time`  DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='执行器配置表';

-- 用户表
CREATE TABLE `xxl_job_user`
(
    `id`         INT(11)     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`   VARCHAR(50) NOT NULL COMMENT '账号',
    `password`   VARCHAR(50) NOT NULL COMMENT '密码',
    `role`       TINYINT(4)  NOT NULL COMMENT '角色：0-普通用户、1-管理员',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 分布式锁表
CREATE TABLE `xxl_job_lock`
(
    `lock_name` VARCHAR(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分布式锁表';

-- ==================== 初始化数据 ====================

-- 初始化执行器（示例）
INSERT INTO `xxl_job_group`(`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
VALUES (1, 'zmbdp-admin-executor', 'Admin服务执行器', 0, NULL, NOW()),
       (2, 'zmbdp-portal-executor', 'Portal服务执行器', 0, NULL, NOW()),
       (3, 'zmbdp-file-executor', 'File服务执行器', 0, NULL, NOW());

-- 初始化示例任务（布隆过滤器重置任务）
INSERT INTO `xxl_job_info`(`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
                              `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
                              `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
                              `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`,
                              `glue_updatetime`, `child_jobid`, `trigger_status`, `trigger_last_time`,
                              `trigger_next_time`)
VALUES (1, 1, '布隆过滤器重置任务', NOW(), NOW(), 'zmbdpdev', '',
        'CRON', '0 0 4 * * ?', 'DO_NOTHING', 'FIRST',
        'resetBloomFilterJobHandler', '', 'SERIAL_EXECUTION', 0, 0,
        'BEAN', '', 'GLUE代码初始化', NOW(), '', 0, 0, 0);

-- 初始化管理员用户
-- 用户名：zmbdpdev  密码：Hf@173503494（MD5加密后）
INSERT INTO `xxl_job_user`(`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'zmbdpdev', 'c0b20f8a9f3316307ce238f020513d62', 1, NULL);

-- 初始化分布式锁
INSERT INTO `xxl_job_lock` (`lock_name`)
VALUES ('schedule_lock');
