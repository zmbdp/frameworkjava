use frameworkjava_dev;
drop table if exists `sys_argument`;
create table `sys_argument`
(
    `id`         bigint(20) unsigned not null primary key comment '主键ID（雪花算法，由应用生成）',
    `name`       varchar(64)   default '' comment '参数名称',
    `config_key` varchar(64)   default '' comment '参数键名',
    `value`      varchar(1024) default '' comment '参数键值',
    `remark`     varchar(64)   default '' comment '备注',
    unique index idx_config_key (`config_key`)
) engine=innodb character set=utf8mb4 comment='系统参数表';
insert into `sys_argument`(`id`, `name`, `config_key`, `value`, `remark`) values
(1, '热门城市', 'sys_hot_city', '1,2,9,22,94,121', '热门城市参数');