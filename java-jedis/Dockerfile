FROM maven:3.8.6-openjdk-18 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:18
ENV DEBIAN_FRONTEND noninteractive
COPY --from=build /usr/src/app/target/redisearch-0.0.1-SNAPSHOT.jar /usr/app/redisearch-0.0.1-SNAPSHOT.jar
COPY --from=build /usr/src/app/src/main/resources/runApplication.sh /usr/app/runApplication.sh
EXPOSE 8080
ENTRYPOINT ["/usr/app/runApplication.sh"]
