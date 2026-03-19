-- # 1、初始化数据库：创建nacos外接数据库frameworkjava_nacos_test和脚手架业务数据库frameworkjava_test
-- # 2、创建用户，用户名：zmbdptest 密码：Hf@173503494
-- # 3、授予zmbdptest用户特定权限

CREATE database if NOT EXISTS `frameworkjava_nacos_test` default character set utf8mb4 collate utf8mb4_general_ci;
CREATE database if NOT EXISTS `frameworkjava_test` default character set utf8mb4 collate utf8mb4_general_ci;
CREATE database if NOT EXISTS `frameworkjava_xxljob_test` default character set utf8mb4 collate utf8mb4_general_ci;

CREATE USER 'zmbdptest'@'%' IDENTIFIED BY 'Hf@173503494';
grant replication slave, replication client on *.* to 'zmbdptest'@'%';

GRANT ALL PRIVILEGES ON frameworkjava_nacos_test.* TO  'zmbdptest'@'%';
GRANT ALL PRIVILEGES ON frameworkjava_test.* TO  'zmbdptest'@'%';
GRANT ALL PRIVILEGES ON frameworkjava_xxljob_test.* TO  'zmbdptest'@'%';

FLUSH PRIVILEGES;
