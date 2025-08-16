FROM openjdk:17
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /workspace
MAINTAINER JavaFh@163.com
COPY ./*.jar /workspace/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar