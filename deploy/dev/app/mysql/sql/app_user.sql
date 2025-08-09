use frameworkjava_dev;
drop table if exists `app_user`;
create table `app_user`
(
    `id`           bigint(20) unsigned not null auto_increment comment '自增主键',
    `nick_name`    varchar(64) null default null comment '昵称',
    `phone_number` varchar(64) null default null comment '电话',
    `open_id`      varchar(64) null default null comment '微信openid',
    `avatar`       varchar(255) null default null comment '头像',
    primary key (`id`) using btree,
    unique index `uk_phone`(`phone_number`) using btree,
    unique index `uk_open_id`(`open_id`) using btree
) engine = innodb auto_increment = 10000001 character set = utf8mb4 comment = '应用端人员表';