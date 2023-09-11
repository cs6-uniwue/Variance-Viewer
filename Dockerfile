FROM maven:3.5-jdk-8-alpine as builder

COPY src /usr/src/app/src
COPY pom.xml /usr/src/app

RUN mvn clean install -f /usr/src/app/pom.xml

FROM tomcat:8-jdk8-temurin-jammy

COPY --from=builder /usr/src/app/target/Variance-Viewer.war /usr/local/tomcat/webapps/Variance-Viewer.war

EXPOSE 8080