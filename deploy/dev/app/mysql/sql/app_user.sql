use frameworkjava_dev;
drop table if exists `app_user`;
CREATE TABLE `app_user`
(
    `id`           bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `nick_name`    varchar(64) NULL DEFAULT NULL COMMENT '昵称',
    `phone_number` varchar(64) NULL DEFAULT NULL COMMENT '电话',
    `open_id`      varchar(64) NULL DEFAULT NULL COMMENT '微信openId',
    `email` varchar(64) NULL DEFAULT NULL COMMENT '用户邮箱',
    `avatar`       varchar(255) NULL DEFAULT NULL COMMENT '头像',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_phone`(`phone_number`) USING BTREE,
    UNIQUE INDEX `uk_open_id`(`open_id`) USING BTREE,
    UNIQUE KEY `uk_email` (`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10000001 CHARACTER SET = utf8mb4 COMMENT = '应用端人员表';

insert into app_user(id, nick_name, phone_number, avatar)
values (10000001, '张三', 'a18068623144ba94f3bd274b5fa03960', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
       (10000002, '李四', '162e23ccd2cacf9735837fd21c8c12f3', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
       (10000003, '王五', 'bffcf97491241c60861c03b435701ce5', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
       (10000004, '赵六', 'd6a1d54ff74316d266b0bb63c0fe0bab', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000005, '孙七', '1e62b414bcfe6e1b5a99d4f2019c6cb6', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000006, '周八', 'ca63b6556f38647c358385534b25ff62', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000007, '吴九', '39f09de35c9eca694778ae521670b7f4', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000008, '郑十', 'a2067e4e24d6871065e7cafbe60d7fb7', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000009, '王十一', '93ed48f51a32985375207cc9866b8e7a', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000010, '张十二', 'd1d10c10a2d93aabeccaf1296747ffe8', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000011, '李十三', '7d9d5a28617a1b57fba5d71017e5a868', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000012, '王十四', '5c14078e19b3f21d11e8067192804c9a', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000013, '赵十五', 'f4f54e4df074d9fb4bdbfd8c8024044a', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000014, '孙十六', 'fbd54ab5adda16cd567dff9155be26d7', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000015, '周十七', 'fb3bfe259c47c61a8f9a97b363bb3b88', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000016, '吴十八', '87e3644ea837df4f24edb7651b1a0ef0', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000017, '郑十九', '401f648065bc3b2eec12013f7bc3cdbf', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000018, '王二十', 'b9bb1565d6a5fa91bb9e094da7fc7917', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png'),
        (10000019, '张二十一', 'afd4ec0ae74b20b764a80eb3cf87dafc', 'https://framework-java-web.oss-cn-shanghai.aliyuncs.com/folder/default_avatar.png');
