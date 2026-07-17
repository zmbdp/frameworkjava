-- 1、初始化数据库：创建nacos 外置数据库 frameworkjava_nacos_dev 和脚手架业务数据库 frameworkjava_dev
-- 2、创建用户，用户名：zmbdpdev 密码：Hf@173503494
-- 3、授予zmbdpdev用户特定权限
CREATE DATABASE IF NOT EXISTS `frameworkjava_nacos_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `frameworkjava_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `frameworkjava_xxljob_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `frameworkjava_skywalking_dev` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'zmbdpdev'@'%' identified BY 'Hf@173503494';

GRANT replication slave, replication client on *.* to 'zmbdpdev'@'%';
GRANT ALL PRIVILEGES ON frameworkjava_nacos_dev.* to 'zmbdpdev'@'%';
GRANT ALL PRIVILEGES ON frameworkjava_dev.* to 'zmbdpdev'@'%';
GRANT ALL PRIVILEGES ON frameworkjava_xxljob_dev.* to 'zmbdpdev'@'%';
GRANT ALL PRIVILEGES ON frameworkjava_skywalking_dev.* TO 'zmbdpdev'@'%';
FLUSH PRIVILEGES;