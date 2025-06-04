FROM eclipse-temurin:21-jdk

WORKDIR /opt

COPY target/essim.jar /opt
COPY src/main/resources/json /opt/json
COPY src/main/resources/tree /opt/tree

ENV HTTP_SERVER_SCHEME http
ENV HTTP_SERVER_HOSTNAME localhost
ENV HTTP_SERVER_PORT 8080
ENV MONGODB_HOST localhost
ENV MONGODB_PORT 27017
ENV GRAFANA_ADMIN_USER admin
ENV GRAFANA_ADMIN_PASSWORD admin
ENV GRAFANA_EXTERNAL_URL http://localhost:3000
ENV PROFILE_QUERY_TIMEOUT 45

CMD java -jar essim.jar
