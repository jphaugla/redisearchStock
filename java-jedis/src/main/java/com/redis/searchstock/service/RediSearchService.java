package com.redis.searchstock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.gson.Gson;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.redis.searchstock.domain.Ticker;
import com.redis.searchstock.domain.TickerCharacter;
import jakarta.annotation.PostConstruct;

import org.json.JSONArray;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.*;

import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.SortedField;

import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


import java.io.*;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.redis.searchstock.repository.TickerRepository;

@Slf4j
@Service
public class RediSearchService {

    @Autowired
    private Environment env;
    @Autowired
    private TickerRepository tickerRepository;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    IndexDefinition.Type indexType = IndexDefinition.Type.HASH;
    String fieldPrefix = "";
    JedisPooled client;
    JedisCluster cluster_client;
    private static final Gson gson = new Gson();


    private static final String Prefix="ticker:";

    String indexName = "idx:movie"; // default name
    String redisHost = "localhost"; // default name
    int redisPort = 6379;
    String redisPassword = "";
    String writeJson = "false";

    ObjectMapper mapper = new ObjectMapper();
    int min_load_date;
    int current_load_date;

    @PostConstruct
    private void init() throws URISyntaxException {
        log.info("Init RediSearchService");
        redisHost = env.getProperty("redis.host", "localhost");
        writeJson = env.getProperty("write_json", "false");
        redisPort = Integer.parseInt(env.getProperty("redis.port", "6379"));
        indexName = env.getProperty("redis.index", "Ticker");
        redisPassword = env.getProperty("spring.redis.password", "");
        log.info("redisPassword is " + redisPassword);
        client = jedis_pooled_connection();

        if(env.getProperty("redis.oss").equals("true")) {
            //  set up a redis cluster to get the topology to create the index on each shard
            HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);
            log.info("redisPassword is " + redisPassword + "empty is " + redisPassword.isEmpty());
            if(redisPassword != null && !(redisPassword.isEmpty())) {
                log.info("before cluster create " + redisPassword);
                cluster_client = new JedisCluster(hostAndPort, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT,
                        10, redisPassword, new ConnectionPoolConfig());
            } else {
                cluster_client = new JedisCluster(hostAndPort, Protocol.DEFAULT_TIMEOUT, 10,
                        new ConnectionPoolConfig());
            }

        }

    }

    private JedisPooled jedis_pooled_connection() {
        // Get the configuration from the application properties/environment
        JedisPooled jedisPooled;

        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(50);
        HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);

        log.info("Configuration Index: " + indexName + " Host: " + redisHost + " Port " + String.valueOf(redisPort));
        if (redisPassword != null && !(redisPassword.isEmpty())) {
            String redisURL = "redis://:" + redisPassword + '@' + redisHost + ':' + String.valueOf(redisPort);
            log.info("redisURL is " + redisURL);
            jedisPooled = new JedisPooled(redisURL);
        } else {
            log.info(" no password");
            jedisPooled = new JedisPooled(hostAndPort);
        }
        return jedisPooled;
    }

    private UnifiedJedis jedis_connection(String host, Integer port) {
        // Get the configuration from the application properties/environment
        UnifiedJedis unifiedJedis;


        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(50);

        log.info("Configuration Index: " + indexName + " Host: " + host + " Port " + String.valueOf(port));

        if (env.getProperty("spring.redis.password") != null && !env.getProperty("spring.redis.password").isEmpty()) {
            String redisPassword = env.getProperty("spring.redis.password");
            unifiedJedis = new JedisPooled(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, redisPassword);
        } else {
            unifiedJedis = new JedisPooled(poolConfig, host , port);
        }
        return unifiedJedis;
    }

    /**
     * Execute the search query with
     * some parameter
     *
     * @param queryString
     * @param offset
     * @param limit
     * @param sortBy
     * @return an object with meta: query header and docs: the list of documents
     */
    public SearchResult search(String queryString, int offset, int limit, String sortBy, boolean ascending) {
        // Let's put all the informations in a Map top make it easier to return JSON object
        // no need to have "predefine mapping"
        Map<String, Object> returnValue = new HashMap<>();
        Map<String, Object> resultMeta = new HashMap<>();
        log.info("starting search with querystring" + queryString);
        // Create a simple query
        Query query = new Query(queryString)
                .setWithScores()
                .limit(offset, limit);
        // if sort by parameter add it to the query
        if (sortBy != null && !sortBy.isEmpty()) {
            query.setSortBy(sortBy, ascending); // Ascending by default
        }

        // Execute the query
        SearchResult queryResult = client.ftSearch(indexName, query);

        // Adding the query string for information purpose
        // resultMeta.put("queryString", queryString);

        // Get the total number of documents and other information for this query:
        // resultMeta.put("totalResults", queryResult.getTotalResults());
        // resultMeta.put("offset", offset);
        // resultMeta.put("limit", limit);

        // returnValue.put("meta", resultMeta);

        // the docs are returned as an array of document, with the document itself being a list of k/v json documents
        // not the easiest to manipulate
        // the `raw_docs` is used to view the structure
        // the `docs` will contain the list of document that is more developer friendly
        //      capture in  https://github.com/RediSearch/JRediSearch/issues/121
        // returnValue.put("raw_docs", queryResult.docs);
        // returnValue.put("raw_docs", queryResult.getDocuments();
        ///returnValue = queryResult.getDocuments();


        // remove the properties array and create attributes
       //  List<Map<String, Object>> docsToReturn = new ArrayList<>();
      /*  List<Document> docs = queryResult.getDocuments();

        for (Document doc : docs) {

            Map<String, Object> props = new HashMap<>();
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", doc.getId());
            meta.put("score", doc.getScore());
            doc.getProperties().forEach(e -> {
                props.put(e.getKey(), e.getValue());
            });

            Map<String, Object> docMeta = new HashMap<>();
            docMeta.put("meta", meta);
            docMeta.put("fields", props);
            docsToReturn.add(docMeta);
        }

        returnValue.put("docs", docsToReturn);
 */
        return queryResult;
    }


    public SearchResult search(String queryString) {
        return search(queryString, 0, 10, null, true);
    }

    public Map<String, Object> getMovieGroupBy(String groupByField) {
        Map<String, Object> result = new HashMap<>();

        // Create an aggregation query that list the genre
        // FT.AGGREGATE idx:movie "*" GROUPBY 1 @genre REDUCE COUNT 0 AS nb_of_movies SORTBY 2 @genre ASC
        AggregationBuilder aggregation = new AggregationBuilder()
                .groupBy("@" + groupByField, Reducers.count().as("nb_of_movies"))
                .sortBy(SortedField.asc("@" + groupByField))
                .limit(0, 1000); // get all rows

        AggregationResult aggrResult = client.ftAggregate(indexName, aggregation);
        int resultSize = aggrResult.getResults().size();

        List<Map<String, Object>> docsToReturn = new ArrayList<>();
        List<Map<String, Object>> results = aggrResult.getResults();

        result.put("totalResults", aggrResult.totalResults);

        List<Map<String, Object>> formattedResult = new ArrayList<>();

        // get all result rows and format them
        for (int i = 0; i < resultSize; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put(groupByField, aggrResult.getRow(i).getString(groupByField));
            entry.put("nb_of_movies", aggrResult.getRow(i).getLong("nb_of_movies"));
            formattedResult.add(entry);
        }
        result.put("rows", formattedResult);
        return result;
    }


    public String loadStockFiles(String directory) throws IOException {
        List<File> fileList = new ArrayList<>();
        log.info("in loadStockFiles with directory " + directory);
        if (directory.isEmpty()) {
            log.info("Please select a CSV file to upload.");
        } else {
            // stringRedisTemplate.opsForValue().set("startLoadStockFiles", "now");
            String min_date = env.getProperty("oldest_value");
            String current_date =  env.getProperty("current_value");
            log.info("min date is " + min_date + " current date is " + current_date);
            min_load_date = Integer.parseInt(min_date);
            current_load_date = Integer.parseInt(current_date);
            FileTraverse fileTraverse = new FileTraverse();
            Files.walkFileTree(Path.of(directory), fileTraverse);
            fileList = fileTraverse.getFileList();
            for(File inFile:fileList) {
                loadOneFile(inFile);
            }
        }
        return "OK";
    }
    public String loadOneFile(File inFile) throws IOException {
        String return_val = "";
            // create csv bean reader
        try (Reader reader = new BufferedReader(new FileReader(inFile))) {
            CsvToBean<Ticker> csvToBean = new CsvToBeanBuilder(reader)
                    .withType(Ticker.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreQuotations(true)
                    .build();
            // log.info("after CsvToBean");
            // get information about file directory
            String short_file_name = inFile.getName();
            String directory_name = inFile.getParent();
            // log.info("dir name " + directory_name);
            String market_identifier = directory_name.replace("/data","").replace("/daily","")
                    .replace("data","").replace("daily","");
            String final_exchange = market_identifier.replaceAll("[0-9]+", "").replace("/", " ").toUpperCase().trim();
            // log.info(final_exchange);
            // convert `CsvToBean` object to list of ticker
            List<Ticker> tickerList = csvToBean.parse();
            for(Ticker ticker : tickerList) {
                // log.info("in ticker loop");
                // log.info(ticker.getTicker());
                ticker.setExchange(final_exchange);
                if(ticker.getDate() >= min_load_date) {
                    if(ticker.getDate() >= current_load_date){
                        ticker.setMostrecent("true");
                    } else {
                        ticker.setMostrecent("false");
                    }
                    createTicker(ticker);
                }
            }

        } catch (Exception ex) {
            log.info("error");
        }
        return return_val;
    }
    public String createTicker(Ticker ticker) throws JsonProcessingException {
        String return_val = "";
        if(writeJson.equals("true")) {
            return_val = createJSONTicker(ticker);
        } else {
            return_val = tickerRepository.create(ticker);
        }
        return return_val;
    }

    public String createJSONTicker(Ticker ticker) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String[] parts = ticker.createTickerShortGeography();
        ticker.setTickershort(parts[0]);
        ticker.setGeography(parts[1]);
        // String json = ow.writeValueAsString(ticker);
        String json = gson.toJson(ticker);
        // log.info("json is " + json);
        // log.info("id is " + ticker.createID());
        String return_val = client.jsonSet(ticker.createID(), redis.clients.jedis.json.Path2.ROOT_PATH, json);
        // log.info("return_val in createJSONTicker is " + return_val);
        return return_val;
    }

    public void createIndex() {
        if (writeJson.equals("true")) {
            indexType = IndexDefinition.Type.JSON;
            fieldPrefix = "$.";
        } else {
            indexType = IndexDefinition.Type.HASH;
            fieldPrefix = "";
        }
        IndexDefinition indexRule = new IndexDefinition(indexType).setPrefixes(new String[]{Ticker.getPrefix()});
        Schema schema = new Schema()
                .addField(new Schema.TextField(FieldName.of(fieldPrefix + "ticker").as("ticker")))
                .addField(new Schema.TextField(FieldName.of(fieldPrefix + "tickershort").as("tickershort")))
                .addField(new Schema.Field(FieldName.of(fieldPrefix + "mostrecent").as("mostrecent"), Schema.FieldType.TAG))
                .addField(new Schema.Field(FieldName.of(fieldPrefix + "date").as("date"), Schema.FieldType.NUMERIC))
                .addField(new Schema.Field(FieldName.of(fieldPrefix + "volume").as("volume"), Schema.FieldType.NUMERIC));
        if(env.getProperty("redis.oss").equals("true")) {
            Map<String, ConnectionPool> clusterNodes = cluster_client.getClusterNodes();
            Collection<ConnectionPool> values = clusterNodes.values();
            values.forEach(jedisPool -> {
                try (UnifiedJedis jedis = new UnifiedJedis(jedisPool.getResource())) {
                    tryIndex(jedis, indexRule, schema);
                }
            });
        } else {
            tryIndex(client, indexRule, schema);
        }
    }
    public void tryIndex(UnifiedJedis jedis_client, IndexDefinition indexRule, Schema schema) {
        log.info("rebuilding index on ");
        try {
            jedis_client.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(indexRule), schema);
        } catch (Exception e) {
            jedis_client.ftDropIndex(indexName);
            jedis_client.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(indexRule), schema);
        }

    }

    public String convertHashResults(SearchResult searchResult) throws JsonProcessingException {
        List<Document> docs = searchResult.getDocuments();
        ArrayList<TickerCharacter> tickerList = new ArrayList<TickerCharacter>();
        for ( Document oneDoc : docs) {
            TickerCharacter tickerRec = new TickerCharacter();
            Iterable<Map.Entry<String, Object>> properties = oneDoc.getProperties();
            tickerRec.setId(oneDoc.getId());
            for (Map.Entry<String, Object> oneProp : properties) {
                String key = oneProp.getKey();
                String value = oneProp.getValue().toString();
                log.info("in properties key " + key + " value " + value);
                if (key.equals("ticker")) {
                    tickerRec.setTicker(value);
                    String[] parts = tickerRec.createTickerShortGeography();
                    tickerRec.setTickershort(parts[0]);
                    tickerRec.setGeography(parts[1]);
                } else if (key.equals("date")) {
                    tickerRec.setDate(value);
                } else if (key.equals("high")) {
                    tickerRec.setHigh (value);
                } else if (key.equals("low")) {
                    tickerRec.setLow(value);
                } else if (key.equals("open")) {
                    tickerRec.setOpen(value);
                } else if (key.equals("volume")) {
                    tickerRec.setVolume(value);
                } else if (key.equals("close")) {
                    tickerRec.setClose(value);
                } else if (key.equals("mostrecent")) {
                    tickerRec.setMostrecent(value);
                } else if (key.equals("exchange")) {
                    tickerRec.setExchange(value);
                } else if (key.equals("per")) {
                    tickerRec.setPer(value);
                } else if (key.equals("openint")) {
                    tickerRec.setOpenint(value);
                } else if (key.equals("time")) {
                    tickerRec.setTime(value);
                }
                // log.info(String.valueOf(oneProp));
                // log.info("value is " + value);
                //  log.info("key is " + key );
            }
            tickerList.add(tickerRec);
        }
        String jsonList = mapper.writeValueAsString(tickerList);
        log.info(jsonList);
        return (jsonList);
    }
    public String convertJSONResults(SearchResult searchResult) throws JsonProcessingException {
        List<Document> docs = searchResult.getDocuments();
        String jsonArray = "[";

        for ( Document oneDoc : docs) {
            log.info("oneDoc ");
            // log.info(oneDoc.toString());
            Iterable<Map.Entry<String, Object>> properties = oneDoc.getProperties();
            log.info("logproperties");
            log.info(properties.toString());
            for (Map.Entry<String, Object> oneProp : properties) {
                String key = oneProp.getKey();
                String value = oneProp.getValue().toString();
                log.info("in properties key " + key + " value " + value);
                if (key.equals("$")) {
                    TickerCharacter ticker = mapper.readValue(value, TickerCharacter.class);
                    String[] parts = ticker.createTickerShortGeography();
                    ticker.setTickershort(parts[0]);
                    ticker.setGeography(parts[1]);
                    String prettyTicker = mapper.writeValueAsString(ticker);
                    log.info("prettyTicker");
                    log.info(prettyTicker);
                    jsonArray = jsonArray + prettyTicker + ",";
                }
                // log.info(String.valueOf(oneProp));
                // log.info("value is " + value);
                //  log.info("key is " + key );
            }

        }
        // remove the last comman and then add end of array character
        if(jsonArray.length()>0) {
            jsonArray = jsonArray.substring(0, (jsonArray.length() - 1)) + "]";
        }
        log.info("jsonarry tostring");
        log.info(jsonArray.toString());
        return (jsonArray.toString());
    }

    public String convertResults(SearchResult searchResult) throws JsonProcessingException {
        String returnValue = "";
        if(writeJson.equals("true")) {
            returnValue = convertJSONResults(searchResult);
        } else {
            returnValue = convertHashResults(searchResult);
        }
        return returnValue;
    }

    public Ticker getKey(String keyValue) {
        Ticker returnTicker;
        if(writeJson.equals("true")) {
            returnTicker = getKeyJSON(keyValue);
        } else {
            returnTicker = getKeyHash(keyValue);
        }
        return returnTicker;
    }

    private Ticker getKeyJSON(String keyValue) {
        Ticker returnTicker = client.jsonGet(keyValue, Ticker.class);
        return returnTicker;
    }

    private Ticker getKeyHash(String keyValue) {
        Map<Object, Object> tickerHash = stringRedisTemplate.opsForHash().entries(keyValue);
        Ticker ticker = mapper.convertValue(tickerHash, Ticker.class);
        return ticker;
    }

    public String getField(String keyValue, String fieldValue) {
        log.info("in service getField key " + keyValue + " field " + fieldValue);
        String returnValue;
        if(writeJson.equals("true")) {
            returnValue = getFieldJSON(keyValue, fieldValue);
        } else {
            returnValue = getFieldHash(keyValue, fieldValue);
        }
        return returnValue;
    }
    private String getFieldJSON(String keyValue, String fieldValue) {
        Object result = client.jsonGet(keyValue, Path2.of(fieldValue));
        String returnValue = result.toString();
        return returnValue;
    }

    private String getFieldHash(String keyValue, String fieldValue) {
        String returnValue = (String)stringRedisTemplate.opsForHash().get(keyValue, fieldValue);
        return returnValue;
    }

    public String setField(String key, String field, String value) {
        log.info("in service setField key " + key + " field " + field + " value " + value);
        String returnValue;
        if(writeJson.equals("true")) {
            returnValue = setFieldJSON(key, field, value);
        } else {
            returnValue = setFieldHash(key, field, value);
        }
        return returnValue;
    }

    private String setFieldHash(String key, String field, String value) {
        stringRedisTemplate.opsForHash().put(key, field, value);
        return "Done\n";
    }

    private String setFieldJSON(String key, String field, String value) {
        //  this replaces the object
        //  Object result = client.jsonSet(key, Path2.ROOT_PATH, new JSONArray(new String[] {field + ':' + value}));
        Object result = client.jsonSet(key, Path2.of(field), value);
        String returnValue = result.toString();
        return returnValue;
    }

    public String deleteTicker(String tickerKey) {
        log.info("in service deleteTicker tickerkey " + tickerKey);
        return  String.valueOf(stringRedisTemplate.delete(tickerKey));
    }

}
