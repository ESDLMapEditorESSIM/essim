FROM anapsix/alpine-java:latest

WORKDIR /opt

COPY target/essim.jar /opt
COPY src/main/resources/json /opt/json
COPY src/main/resources/tree /opt/tree
COPY version.txt /opt

ENV HTTP_SERVER_SCHEME http
ENV HTTP_SERVER_HOSTNAME localhost
ENV HTTP_SERVER_PORT 8080
ENV HTTP_SERVER_PATH essim
ENV MONGODB_HOST localhost
ENV MONGODB_PORT 27017
ENV GRAFANA_ADMIN_USER admin
ENV GRAFANA_ADMIN_PASSWORD admin
ENV GRAFANA_EXTERNAL_URL http://localhost:3000
ENV PROFILE_QUERY_TIMEOUT 45
ENV CONSOLE_LOG_LEVEL error
ENV FILE_LOG_LEVEL debug
ENV LOG_FILE_LOCATION /tmp
ENV MAX_LOG_FILE_SIZE 10MB
ENV MAX_LOG_HISTORY_DAYS 60
ENV LOG_FILES_SIZE_CAP 2GB

CMD java -Xms8G -XX:+UseParallelGC -XX:GCTimeRatio=9 -jar essim.jar