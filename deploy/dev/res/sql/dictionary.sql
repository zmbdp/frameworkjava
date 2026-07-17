use frameworkjava_dev;
drop table if exists `sys_dictionary_type`;
create table `sys_dictionary_type`
(
    `id`       bigint(20) unsigned not null primary key comment '主键ID（雪花算法，由应用生成）',
    `type_key` varchar(64) default '' comment'字典类型键',
    `value`    varchar(64) default '' comment '字典类型值',
    `remark`   varchar(64) default '' comment '备注',
    `status`   tinyint(1) default 1 comment '字典类型状态 1正常 0停用',
    unique index idx_type_key (`type_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = DYNAMIC COMMENT = '字典类型表';
insert into `sys_dictionary_type` (id, type_key, value, remark, status)
values (1, 'admin', '管理员', '', 1),
       (2, 'common_status', '公共状态', '', 1);

drop table if exists `sys_dictionary_data`;
create table `sys_dictionary_data`
(
    `id`       bigint(20) unsigned not null primary key comment '主键ID（雪花算法，由应用生成）',
    `type_key` varchar(64) default '' comment '字典类型键',
    `data_key` varchar(64) default '' comment '字典数据键',
    `value`    varchar(64) default '' comment '字典数据值',
    `remark`   varchar(64) default '' comment '备注',
    `sort`     int(11) default 1 comment '排序',
    `status`   tinyint(1) default 1 comment '字典数据状态 1正常 0停用',
    key        idx_type_key (`type_key`),
    unique index ui(`type_key`, `data_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 ROW_FORMAT = DYNAMIC COMMENT = '字典数据表';
insert into `sys_dictionary_data` (id, type_key, data_key, value, remark, sort, status)
values (1, 'admin', 'super_admin', '超级管理员', '', 1, 1),
       (2, 'admin', 'platform_admin', '平台管理员', '', 1, 1),
       (3, 'common_status', 'enable', '启用', '', 1, 1),
       (4, 'common_status', 'disable', '停用', '', 1, 1);