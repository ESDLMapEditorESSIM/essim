FROM eclipse-temurin:21-jdk

ARG VERSION=unknown
LABEL org.opencontainers.image.version=$VERSION

WORKDIR /opt

RUN echo "$VERSION" > /opt/version.txt

COPY target/essim.jar /opt
COPY src/main/resources/json /opt/json
COPY src/main/resources/tree /opt/tree
COPY src/main/resources/log4j2.xml /opt/log4j2.xml
RUN mkdir -p logs/simulation-logs

ENV HTTP_SERVER_SCHEME http
ENV HTTP_SERVER_HOSTNAME localhost
ENV HTTP_SERVER_PORT 8080
ENV MONGODB_HOST localhost
ENV MONGODB_PORT 27017
ENV GRAFANA_ADMIN_USER admin
ENV GRAFANA_ADMIN_PASSWORD admin
ENV GRAFANA_EXTERNAL_URL http://localhost:3000
ENV PROFILE_QUERY_TIMEOUT 45

CMD java -Dlog4j2.configurationFile=/opt/log4j2.xml -jar essim.jar
