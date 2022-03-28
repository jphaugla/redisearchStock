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

##  Notes for running outside of Docker
Follow most of the same steps as above with some changes

### Instead of docker to execute, use python virtualenv
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
Go to the stock type [ahead page](http://localhost:5000) and find the desired stock