# redisearchStock
A simple stock ticker solution based on downloaded stock files.  Uses redisearch for an API and jquery with bootstrap ajax typeahead plugin.
![](src/static/typeaheadStocks.png)
## Initial project setup
Get this github code
```bash 
get clone https://github.com/jphaugla/redisearchStock.git
```
Two options for setting the environment are given:  
  * run with docker-compose using a flask and redis container
  * installing for mac os
  * running on linux (probably in the cloud)

## Important Links
* [bootstrap ajax typeahead example](https://github.com/biggora/bootstrap-ajax-typeahead)
* [Redis Stack](https://redis.com/blog/introducing-redis-stack/)
* [Redis Search](https://redis.io/docs/stack/search/)
* [Redis Insight](https://redis.io/docs/stack/insight/)
* [Stooq stock files](https://stooq.com/db/h/)


### Bootstrap ajax typeahead
This plugin needs to be in place-it is in the repository but follow the directions in the [bootstrap-ajax-typahead github](https://github.com/biggora/bootstrap-ajax-typeahead)

### Download the datafiles to the data subdirectory
* Download stock files here
 [Stooq stock files](https://stooq.com/db/h/)
* Once downloaded, move the file (it should be a directory called *data*) to the main redisearchStock directory
  * Can combine the various stooq files at the daily level by including world, us, etc under this daily directory
  * There is a separate file for each *stock* or *currency* with a long history of data.  See instructions below for setting the environment variables to limit history load
  

### Set environment

The docker compose file has the environment variables set for the redis connection and the location of the data files.
This code uses redisearch.  The redis database must have both of these modules installed.
As of this writing, this redismod docker image (which includes these modules) does not work on the m1 arm64 based mac.  
Default docker-compose is set to redismod.  Check the environment variables for appropriateness. Especially check the TICKER_DATA_LOCATION because loading all of 
the US tickers with all of the history can be a lot of data on a laptop.  Here is an explanation of the environment variables.
Modify these values in docker-compose.yml

| variable             | Original Value | Desccription                                                                                                                                                            |
|----------------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| REDIS_HOST           | redis          | The name of the redis docker container                                                                                                                                  |
| REDIS_PORT           | 6379           | redis port                                                                                                                                                              |
| TICKER_FILE_LOCATION | /data          | leave in /data but can use sub-directory to minimize load size                                                                                                          | 
| PROCESSES            | 6              | On larger machines, increases this will increase load speed                                                                                                             |
| WRITE_JSON           | false          | flag to use JSON instead of Hash structures                                                                                                                    |
| PROCESS_DATES        | true           | have date-based logic instead of just a simple initial load.  Allows for <br/> skipping any records old than a particular date (requires creation of specific redis hash) |   
| PROCESS_RECENTS      | false          | will set most recent flag for specified keys back to false    (requires creation of specific redis set)                                                             |

The created index is filtered to only the records where MostRecent is set to true

## docker compose startup
Build just needs to be done initially
```bash
docker-compose build
docker-compose up -d 
```


### load Tickers
* If PROCESS_DATES is set, these entries should be made (customize as needed)
  * This will load all the values for 2022 and set the current data to 20220315
```bash
docker exec -it redis redis-cli 
hset process_control oldest_value 20220101 current_value 20220315 
```
* If PROCESS_RECENTS is set, set list of recent dates to specifically set the MostRecent flag to false
  * This is needed when loading the next set of values.  E.g.  Current data is 20220315 and want to ensure three previous dates are false for MostRecent.  
```bash
docker exec -it redis redis-cli
sadd remove_current 20220314 20220313 20220312 
```
* make sure the TICKER_FILE_LOCATION is good and then start the load
```bash
docker exec -it flask bash -c "python TickerImport.py"
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
### Create Index
There is python running in the flask container (appy.py) listening for API calls.  One of the API calls will recreate the index.  Use the flowing script to create the index
```bash
cd scripts
./redoIndex.sh
```

```bash
redic-cli -f scripts/searchQueries.txt
```

## Instead of docker to execute, use python virtualenv
  * create a virtualenv
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
  * execute python scripts from the src directory
```bash
cd src
pip install -r requirements.txt
python TickerImport.py
```

## Instead, use kubernetes
This example is showing GKE steps-adjust accordingly for other versions
### Install Redis Enterprise k8s
* Get to K8 namespace directory
```bash
cd $DEMO
```
* Follow [Redis Enterprise k8s installation instructions](https://github.com/RedisLabs/redis-enterprise-k8s-docs#installation) all the way through to step 4.  Use the demo namespace as instructed.
* For Step 5, the admission controller steps are needed but the webhook instructions are not necessary
* Don't do Step 6 as the databases for this github are in the k8s subdirectory of this github
* Create redis enterprise database.  
```bash
kubectl apply -f redis-enterprise-database.yml

```
* Try cluster username and password script as well as databases password and port information scripts
```bash
./getDatabasePw.sh
./getClusterUnPw.sh
```
#### Add redisinsights
These instructions are based on [Install RedisInsights on k8s](https://docs.redis.com/latest/ri/installing/install-k8s/)
&nbsp;
The above instructions have two options for installing redisinights, this uses the second option to install[ without a service](https://docs.redis.com/latest/ri/installing/install-k8s/#create-the-redisinsight-deployment-without-a-service) (avoids creating a load balancer)
* copy the yml file above into a file named *redisinsight.yml*
* create redisinsights
```bash
kubectl apply -f redisinsight.yaml
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

## Deploy redis-searchstock on Kubernetes
* must log into docker to have access to the docker image
```bash
docker login
```
* modify, create the environmental variables by editing configmap.yml
  * can find the IP addresses and ports for each of the databases by running ```kubectl get services```
  * put the database password in for the redis password by running ```getDatabasePw```
* create the configuration map
```bash
cd k8s
kubectl apply -f configmap.yaml
```
* deploy the redis-searchstock
```bash
kubectl apply -f stock.yml
```
* port forward and continue with testing of the APIs
  * NOTE:  get exact name use ```kubectl get pods```
```bash
kubectl port-forward redis-searchstock-c568d9b6b-z2mnf 5000
```


Go to the stock type [ahead page](http://localhost:5000) and find the desired stock