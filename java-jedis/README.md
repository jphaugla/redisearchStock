# RediSearchStock with Jedis

The [main readme](../README.md) is up one level.

## Important Links/Notes
- [Jedis and Spring version](https://stackoverflow.com/questions/72194259/is-it-possible-to-use-the-newest-jedis-in-spring-project)
- [Spring pull request including Jedis 4.x](https://github.com/spring-projects/spring-data-redis/pull/2287)
- [Spring 3.0 SNAPSHOT](https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/html/getting-started.html#getting-started.installing.java)
- [GitHub deploying this java application with redis enterprise on AWS](https://github.com/jphaugla/tfmodule-aws-redis-enterprise)

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

### Install Java
#### on redhat
  * install java 
  * set java home
```bash
sudo yum install java-17-openjdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```
  * download and install maven following [these steps](https://linuxize.com/post/how-to-install-apache-maven-on-centos-7) - NOTE:  yum installs older version
  * this worked with java 17
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export M2_HOME=/opt/maven
export MAVEN_HOME=/opt/maven
export PATH=${M2_HOME}/bin:${PATH}
```

#### on ubuntu
```bash
mkdir binaries
cd binaries
apt install openjdk-18-jdk openjdk-18-jre
cat <<EOF | sudo tee /etc/profile.d/jdk18.sh
export JAVA_HOME=/usr/lib/jvm/java-18-openjdk-amd64
EOF
```
  * download and install maven flollowing [these steps](https://phoenixnap.com/kb/install-maven-on-ubuntu)  Note:  apt-get installs older version
### Change address in index.html
The IP address for the API call is set to localhost.  This must be changed for the IP address of the application server.  The index.html file is at [index.html](src/main/resources/index.html) 
replace all occurrences of localhost with the public ip address of the application server machine  

### Compile application
```bash
cd java-jedis
mvn clean package
```

### Set Environment and Run
edit the [app.env](../scripts/app.env) appropriately for desires and environment
```bash
source ../scripts/app.env
java -jar target/redisearch-0.0.1-SNAPSHOT.jar
```
