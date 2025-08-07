use frameworkjava_dev;
create table `sys_argument`
(
    `id`         bigint(20) unsigned not null auto_increment primary key comment '自增主键',
    `name`       varchar(64)   default '' comment '参数名称',
    `config_key` varchar(64)   default '' comment '参数键名',
    `value`      varchar(1024) default '' comment '参数键值',
    `remark`     varchar(64)   default '' comment '备注',
    unique index idx_config_key (`config_key`)
) engine=innodb auto_increment=10000001 character set=utf8mb4 comment='系统参数表';