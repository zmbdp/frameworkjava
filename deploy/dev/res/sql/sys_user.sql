use frameworkjava_dev;
drop table if exists `sys_user`;
create table `sys_user`
(
    `id`           bigint(20) unsigned not null comment '主键ID（雪花算法，由应用生成）',
    `nick_name`    varchar(64)  not null comment '昵称',
    `phone_number` varchar(64)  not null comment '电话',
    `password`     varchar(255) not null comment '密码',
    `identity`     varchar(16)  not null comment '身份',
    `remark`       varchar(50) null default null comment '备注',
    `status`       varchar(10)  not null comment '状态',
    primary key (`id`) using btree,
    unique index `uk_phone`(`phone_number`) using btree
) engine=innodb character set=utf8mb4 comment='管理端人员表';

insert into `sys_user` (id, nick_name, phone_number, password, identity, remark, status) values
(1, '稚名不带撇', '62a9bfed8dc2cc6e2c83eb628bd10d3e', '78199ef620f359d5a33b91d172d3acfeb13591719c53d3cfa14ade0614fcb1a6', 'super_admin', "超级管理员", 'enable');
