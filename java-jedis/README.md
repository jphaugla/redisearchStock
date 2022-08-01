# RediSearchStock with Jedis

The [main readme](../README.md) is up one level.

## Important Links/Notes
- [Jedis and Spring version](https://stackoverflow.com/questions/72194259/is-it-possible-to-use-the-newest-jedis-in-spring-project)
- [Spring pull request including Jedis 4.x](https://github.com/spring-projects/spring-data-redis/pull/2287)
- [Spring 3.0 SNAPSHOT](https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/html/getting-started.html#getting-started.installing.java)

## Running the application in Docker

You can run build and run the application from docker using the following commands:

```shell script
cd java-redis
docker-compose build 
docker-compose up -d
```

This command will create a new image and build the maven project into it.

## Publish docker image
The docker image used in k8s/stock.yaml is jphaugla/redis-searchstock:latest
If building own image, have docker login, change to personal docker account and change the stock.yaml
To build different version of the docker image:  docker-compose build, docker tag, docker publish
```bash
cd python
docker login
docker-compose build
docker image tag jedis-searchstock:latest jphaugla/jedis-searchstock:latest
docker image push jphaugla/jedis-searchstock:latest
```

## Running from command line

* turn off the docker container so can run locally
```bash
docker stop jedis-stock
```
* install maven and java
NOTE:  this needs JAVA 17 or higher for compatibility with Spring 3.0
```bash
sudo apt-get install maven
sudo apt-get install def
```
### Compile application
```bash
mvn clean package
```

### Set Environment and Run
```bash
source ../scripts/setEnv.sh
./src/main/resources/runApplication.sh
```
