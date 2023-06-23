#FROM openjdk:17-jdk-alpine
FROM eclipse-temurin:17-jdk-alpine
MAINTAINER fedex.com
COPY target/api-aggregation-1.0.0.jar api-aggregation-1.0.0.jar
ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","/api-aggregation-1.0.0.jar"]