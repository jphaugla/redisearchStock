# redisearchStock
A stock ticker solution typeahead solution based on downloaded stock files.  Uses Python redisearch for an API and jquery with bootstrap ajax typeahead plugin.
 
![app screen](python/src/static/typeaheadStocks.png)

## Outline

- [Overview](#overview)
- [Initial Project Setup](#initial-project-setup)
- [Important Links](#important-linksnotes)
- [Instructions](#instructions)
  - [Create Environment](#create-environment)
  - [Download the datafiles](#download-the-datafiles)
  - [Set Environment Variables](#set-environment-variables)
  - [Multiple Deployment options](#multiple-options-for-creating-the-environment)
  - [Docker Compose Python](#docker-compose-python)
    - [Prepare the Load](#prepare-the-load)
  - [Deploy Python on Linux](#deploy-python)
    - [Create python environment](#create-python-environment)
    - [Prepare Ticker Load on Python](#prepare-ticker-on-python)
  - [Kubernetes](#kubernetes)
    - [Install Redis Enterprise](#install-redis-enterprise-k8s)
    - [Add Redisinsights](#add-redisinsights)
    - [Deploy application](#deploy-redis-searchstock-on-kubernetes)
    - [Publish Docker Image](#publish-docker-image)
    - [Add Memtier Benchmark](#memtier-benchmark)
  - [Use the Application](#use-the-application)
    - [Create Index](#create-index)
    - [Start Ticker Load](#start-ticker-load)
- [Troubleshooting](#troubleshooting)


## Overview

Uses python to load stock market daily values from download flat files.  Using a redisearch index, a web browser application written in jquery uses typeahead prioritizing stocks with the highest volume to provide type ahead suggestions.  When a stock is selected, more detail is displayed on the stock history.

## Important Links/Notes
* [bootstrap ajax typeahead example](https://github.com/biggora/bootstrap-ajax-typeahead)
* [Redis Stack](https://redis.com/blog/introducing-redis-stack/)
* [Redis Search](https://redis.io/docs/stack/search/)
* [Redis Insight](https://redis.io/docs/stack/insight/)
* [Stooq stock files](https://stooq.com/db/h/)
* [Run Python on k8s](https://opensource.com/article/18/1/running-python-application-kubernetes)
* [k8s persistent volume claim](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#claims-as-volumes)
* [GKE persistent volume claims](https://cloud.google.com/kubernetes-engine/docs/concepts/persistent-volumes)

### Bootstrap ajax typeahead
This plugin is in the repository so no setting is needed.  To learn more-follow the directions in the [bootstrap-ajax-typahead github](https://github.com/biggora/bootstrap-ajax-typeahead)

## Instructions

### Create environment
Clone the github 
```bash 
get clone https://github.com/jphaugla/redisearchStock.git
```

### Download the datafiles
* Download stock files here
 [Stooq stock files](https://stooq.com/db/h/)
* Once downloaded, move the file (it should be a directory called *data*) to the main redisearchStock directory
  * Can combine the various stooq files at the daily level by including world, us, etc under this daily directory
  * There is a separate file for each *stock* or *currency* with a long history of data.  See instructions below for setting the environment variables to limit history load
  * To simplify things, best to remove the spaces in the file directory such as "nasdaq stocks".  The spaces are dealt with in docker-compose but never cleaned this up in k8s.  Just easier to eliminate the spaces.
  
### Set environment variables

The docker compose file has the environment variables set for the redis connection and the location of the data files.  In k8s, the environment is set in the configmap.
This code uses redisearch.  The redis database must have redisearch installed.  In docker, the redis stack image contains all the modules.   In k8s, redisearch is added in the database yaml file. 
Check the environment variables for appropriateness. Especially check the TICKER_DATA_LOCATION because loading all of 
the US tickers with all of the history can be a lot of data on a laptop.  Here is an explanation of the environment variables.
Modify these values in docker-compose.yml or in the configmap for k8s.  If outside of a container, there is a file created with 
environment variable at scripts/app.env.

| variable             | Original Value  | Desccription                                                                                         |
|----------------------|-----------------|------------------------------------------------------------------------------------------------------|
| REDIS_HOST           | redis           | The name of the redis docker container                                                               |
| REDIS_PASSWORD       | <none>          | Redis Password                                                                                       |
| REDIS_PORT           | 6379            | redis port                                                                                           |     
| REDIS_INDEX          | Ticker          | redis port                                                                                           |     
| PROCESSES            | 6               | On larger machines, increasing this will increase load speed                                          |
| WRITE_JSON           | false           | flag to use JSON instead of Hash structures                                                          |
| PIPELINE             | true            | flag to use pipeline for each of the ticker files (only works on python)                                                    |
| OLDEST_VALUE         | 20220101        | Skip any records older than this date                                                                |   
| CURRENT_VALUE        | 20220414        |  Use this as current value.  This also sets mostrecent flag                                          |
| PROCESS_RECENTS      | false           | will set most recent flag for specified keys back to false (requires creation of specific redis set) |
|----------------------|-----------------|------------------------------------------------------------------------------------------------------|


### Multiple options for creating the environment:  
  * run with python docker-compose using a flask and redis container
  * installing for mac os
  * running on linux (probably in the cloud)
  * running on kubernetes (example uses GKE)
  * run using java instead of python This java implemention is in a [subfolder](java-jedis/README.md)

### Docker Compose Python

Build just needs to be done initially.  NOTE:  if building a new image for k8s, ensure the Dockerfile is doing a copy of the src directory
into the image and not relying on docker-compose mount of the src directory.  Additionally, docker can be run with the Java application.  See [java readme](java-jedis/README.md)
```bash
cd python
docker-compose build
docker-compose up -d 
```

### Deploy Python

Can be issues with running flask on linux at the time of installing requirements files
#### create python environment
```bash
cd src
python3 -m venv venv
source venv/bin/activate
```
* Use an environment file for locations
* Need to make sure the data location variables are set correctly
* Can also set the number of concurrent processes for the client using the "PROCESSES" environment variable

```bash
source scripts/app.env
```
#### Prepare Ticker on Python
* execute python scripts from the src directory
* if trouble with installing requirements, here are some links
[Flask on AWS Linux 2](https://thecodinginterface.com/blog/flask-aws-ec2-deployment/)
[Flask on Ubuntu](https://linuxize.com/post/how-to-install-flask-on-ubuntu-20-04/)
[Flask on RedHat](https://developers.redhat.com/blog/2018/06/05/install-python-flask-on-rhel)
```bash
cd src
pip install -r requirements.txt
```

### Kubernetes
This example is showing GKE steps-adjust accordingly for other versions
#### Install Redis Enterprise k8s
* Get to K8 namespace directory
```bash
cd k8s
```
* Follow [Redis Enterprise k8s installation instructions](https://github.com/RedisLabs/redis-enterprise-k8s-docs#installation) all the way through to step 4.  Use the demo namespace as instructed.
* For Step 5 
  * the admission controller steps are not necessary
  * but the webhook instructions are not necessary
* Don't do Step 6 as the databases for this github are in the k8s subdirectory of this github

##### Create redis enterprise database.
###### Verify cluster is ready
* check health using kubectl
```bash
kubectl get all
kubectl get rec
kubectl get pods
```
* check enterprise node cluster ui (optional steps just for demonstration purposes)
```bash
./getClusterUnPw.sh
```
* port forward so can access the redis cluster 
```bash
kubectl port-forward service/rec-ui 8443
```
* [https://localhost:8443](https://localhost:8443)

##### Create redis database
```bash
kubectl apply -f redis-enterprise-database.yml
```
##### verify database
* check for database in [management ui](https://localhost:8443)
* check database yaml output
* get port and password for database
* port-forward to the database
```bash
kubectl get redb/redis-enterprise-database -o yaml
./getDatabasePw.sh
kubectl port-forward service/redis-enterprise-database 10740
```
* connect to redis-cli - use the password and port from the output of ./getDatabasePw.sh
```bash
redis-cli -p 16379 -a rhliu76
```
* log into redis enterprise node and use rladmin
```bash
kubectl exec -it rec-0 -- bash
rladmin status extra all
```
##### change database
* can make changes to redis database from controller and verify in either rladmin or in the GUI
* use [https://localhost:8443](https://localhost:8443) to view the changes
* modify redis-enterprise-database.yml to enable replication and to increase database size
```bash
edit redis-enterprise-database.yml
kubectl apply -f redis-enterprise-database.yml
```
* will see these changes in the management ui and in rladmin

#### Add redisinsights
These instructions are based on [Install RedisInsights on k8s](https://docs.redis.com/latest/ri/installing/install-k8s/)
&nbsp;
The above instructions have two options for installing redisinights, this uses the second option to install[ without a service](https://docs.redis.com/latest/ri/installing/install-k8s/#create-the-redisinsight-deployment-without-a-service) (avoids creating a load balancer)
* copy the yml file above into a file named *redisinsight.yml* (this file is already in the k8s directory)
* create redisinsights
```bash
kubectl apply -f redisinsight.yml
kubectl port-forward deployment/redisinsight 8001
```
* from chrome or firefox open the browser using http://localhost:8001
* Click "I already have a database"
* Click "Connect to Redis Database"
* Create Connection to target redis database with following parameter entries

| Key      | Value                                     |
|----------|-------------------------------------------|
| host     | redis-enterprise-database.demo            |
| port     | 18154 (get from ./getDatabasepw.sh above) |
| name     | TargetDB                                  |
| Username | (leave blank)                             |
| Password | DrCh7J31 (from ./getDatabasepw.sh above) |
* click ok

#### Deploy redis-searchStock on Kubernetes

* must [log into docker](https://docs.docker.com/engine/reference/commandline/login/) to have access to the docker image
```bash
docker login
```
* modify, create the environmental variables by editing configmap.yml
  * can find the IP addresses and ports for each of the databases by running ```kubectl get services```
  * In the example below the IP address for the REDIS_HOST in the configmap.yaml is *10.28.16.188*
![services](python/src/static/k8sgetservices.png)
  * get the database password and port by running ```getDatabasePw```.  Put the returned password the configmap REDIS_PASSWORD and REDIS_PORT
* apply the configuration map
```bash
cd k8s
# change REDIS_PASSWORD and REDIS_PORT based on ./getDatabasePw
vi configmap.yaml
kubectl apply -f configmap.yaml
```
* deploy the redis-searchstock
* can switch between the java version and python version by changing image used in stock.yaml.  Jedis version is commented out.
```bash
kubectl apply -f pvc.yaml
kubectl apply -f stock.yaml
kubectl get pods
```
* port forward and continue with testing of the APIs
  * NOTE:  get exact name use ```kubectl get pods```
```bash
kubectl port-forward redis-searchstock-c568d9b6b-5t8v6 5000
```
#### Run search stock
* get the data files from [stooq](https://github.com/jphaugla/redisearchStock#download-the-datafiles)
* copy the stooq files into place.  Easiest to zip them before copying using ```tar -cvf all_tar.tgz data/daily```
  * can be issue with hidden files caused by running tar on Mac and deploying on linux see [troubleshooting](#troubleshooting)
```bash
kubectl cp all_tar.tgz redis-searchstock-c568d9b6b-5t8v6:/data
```

##### Publish docker image
The docker image used in k8s/stock.yaml is jphaugla/redis-searchstock:latest
If building own image, have docker login, change to personal docker account and change the stock.yaml
To build different version of the docker image:  docker-compose build, docker tag, docker publish 
```bash
cd python
docker login
docker-compose build
docker image tag redis-searchstock:latest jphaugla/redis-searchstock:latest
docker image push jphaugla/redis-searchstock:latest
```

#### Memtier benchmark
Can also optionally deploy memtier benchmark as a pod in the cluster
Before adding memtier.yml, must have extra node and then label the node
```bash
kubectl label nodes gke-jph-k8s-cluster-default-pool-6ecc6b17-zllk app=memtier
kubectl apply -f memtier.yml
```
#### Deploy redis-sql
Can deploy redis-sql to run SQL on top of redisearch
* Edit the URI string for connection to redisenterprise using ```./getDatabasePw.sh```
* deploy configmap and yaml for trino
```bash
cd k8s
kubectl apply -f trino-configmap.yaml
kubectl apply -f trino.yaml
```

Use redisinsights or the management UI to observe the benchmark results
## Use the application

### Create Index
There is python running in the flask container (appy.py) listening for API calls.  One of the API calls will recreate the index.  Use the following script to create the index
```bash
cd scripts
./redoIndex.sh
```

#### Prepare the load
```
* If PROCESS_RECENTS is set, set list of recent dates to specifically set the MostRecent flag to false
  * This is needed when loading the next set of values.  E.g.  Current data is 20220315 and want to ensure three previous dates are false for MostRecent.  
```bash
docker exec -it redis redis-cli
sadd remove_current 20220314 20220313 20220312 
```

#### Start Ticker Load
* verify the directory location specified in the script
```bash
cd scripts
./loadFile.sh
```

Can observe the load progress by watching the load for each file
```bash
docker exec -it redis redis-cli 
hgetall ticker_load
```
  * THIS IS HOW to start flask app server
  * However, it is already running as part of the flask container
 ```bash
docker exec -it flask bash -c "python appy.py"
 ```

Go to the stock type [ahead page](http://localhost:5000) and find the desired stock

These are a group of sample redis-cli queries to see 
```bash
redic-cli -f scripts/searchQueries.txt
```

There are scripts in the scripts directory in addition to loadFile.sh and redoIndex.sh.  Check these out for additional API calls available such as adding a ticker, deleting a ticker, getting one ticker field, removing one ticker field, etc.

### Troubleshooting
Moving the Stooq datafiles from apple (BSD) to Linux can cause an odd read errors in the loading file program
delete all these erroneous files.  I have now added some code to ignore hidden files in python version of code.
```bash
find data/daily -type f -iname "._*.*" -ls -delete
```
