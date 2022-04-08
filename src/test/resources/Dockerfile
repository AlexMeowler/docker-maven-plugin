# syntax=docker/dockerfile:1
FROM openjdk:11
EXPOSE 8080
ARG JAR_FILE=springbootweb.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]