FROM apache/skywalking-oap-server:9.7.0

# 下载 MySQL JDBC 驱动
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar /skywalking/oap-libs/

# 设置权限
RUN chmod 644 /skywalking/oap-libs/mysql-connector-j-8.0.33.jar

