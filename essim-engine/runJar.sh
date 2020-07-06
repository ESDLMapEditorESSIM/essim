#!/bin/bash

export GRAFANA_API_KEY=eyJrIjoicnRJdkFhQ2xBQ080V3RQQ2tIUWV5YXdoWXk5bEd0UXgiLCJuIjoiZXNzaW0ta2V5IiwiaWQiOjF9
export HTTP_SERVER_HOSTNAME=localhost
export HTTP_SERVER_PATH=essim
export HTTP_SERVER_PORT=8080
export HTTPS_SERVER_SCHEME=http
export MONGODB_HOST=localhost
export MONGODB_PORT=27017

java -jar target/essim.jar
