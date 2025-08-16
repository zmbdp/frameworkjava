# 命令中数据库相关信息、容器相关信息此前如有修改，请确保前后信息一致

# 进入容器
docker exec -it frameworkjava-mysql-slave bash

#1.登录
mysql -uroot -h127.0.0.1 -pHf@173503494
#2. 执行如下命令

CHANGE REPLICATION SOURCE TO SOURCE_HOST='你的云服务器1内网ip/你的虚拟机1内网ip', SOURCE_PORT=3308,SOURCE_USER='zmbdpdev',SOURCE_PASSWORD='Hf@173503494', SOURCE_SSL = 1,SOURCE_LOG_FILE='mysql-bin.000002',SOURCE_LOG_POS=2;
START REPLICA;
SHOW REPLICA STATUS\G;
