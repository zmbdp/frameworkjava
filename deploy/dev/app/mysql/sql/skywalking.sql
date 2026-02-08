-- SkyWalking 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `frameworkjava_skywalking_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `frameworkjava_skywalking_dev`;

-- 授权
GRANT ALL PRIVILEGES ON frameworkjava_skywalking_dev.* TO 'zmbdpdev'@'%';
FLUSH PRIVILEGES;

-- SkyWalking 9.x 会自动创建表结构，这里只需要创建数据库即可
-- 如果需要手动创建表结构，可以从 SkyWalking 官方获取 SQL 脚本
-- https://github.com/apache/skywalking/tree/master/oap-server/server-storage-plugin/storage-jdbc-hikaricp-plugin/src/main/resources

