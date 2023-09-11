FROM tomcat:8-jdk8-temurin-jammy

RUN apt -y update && apt -y install git maven

RUN git clone https://github.com/cs6-uniwue/Variance-Viewer.git
RUN mvn clean install -f Variance-Viewer/pom.xml
RUN cp Variance-Viewer/target/Variance-Viewer.war /usr/local/tomcat/webapps/Variance-Viewer.war

EXPOSE 8080