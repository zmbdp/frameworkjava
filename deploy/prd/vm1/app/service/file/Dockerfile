FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /workspace
LABEL maintainer="JavaFH@163.com"
COPY ./*.jar /workspace/app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]