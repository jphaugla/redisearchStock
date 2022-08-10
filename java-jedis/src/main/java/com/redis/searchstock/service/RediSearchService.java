package com.redis.searchstock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.redis.searchstock.domain.Ticker;
import com.redis.searchstock.domain.TickerCharacter;
import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.*;

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

    private static final String Prefix="ticker:";

    String indexName = "idx:movie"; // default name
    String redisHost = "localhost"; // default name
    int redisPort = 6379;
    String redisPassword = "jasonrocks";

    ObjectMapper mapper = new ObjectMapper();
    int min_load_date;
    int current_load_date;

    @PostConstruct
    private void init() throws URISyntaxException {
        log.info("Init RediSearchService");
        redisHost = env.getProperty("redis.host", "localhost");
        redisPort = Integer.parseInt(env.getProperty("redis.port", "6379"));
        client = jedis_connection(redisHost, redisPort);
        indexName = env.getProperty("redis.index", "Ticker");

        if(env.getProperty("redis.oss").equals("true")) {
            //  set up a redis cluster to get the topology and can
            HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);
            if(env.getProperty("spring.redis.password") != null && !env.getProperty("spring.redis.password").isEmpty()) {
                String redisPassword = env.getProperty("spring.redis.password");
                log.info("before cluster create " + redisPassword);
                cluster_client = new JedisCluster(hostAndPort, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, 10, redisPassword, new ConnectionPoolConfig());
            } else {
                cluster_client = new JedisCluster(hostAndPort, Protocol.DEFAULT_TIMEOUT, 10, new ConnectionPoolConfig());
            }

        }

    }

    private JedisPooled jedis_connection(String host, Integer port) {
        // Get the configuration from the application properties/environment
        JedisPooled jedisPooled;


        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(50);

        log.info("Configuration Index: " + indexName + " Host: " + host + " Port " + String.valueOf(port));

        if (env.getProperty("spring.redis.password") != null && !env.getProperty("spring.redis.password").isEmpty()) {
            String redisPassword = env.getProperty("spring.redis.password");
            jedisPooled = new JedisPooled(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, redisPassword);
        } else {
            jedisPooled = new JedisPooled(poolConfig, host , port);
        }
        return jedisPooled;
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
            String final_exchange = market_identifier.replaceAll("[0-9]+", "").replace("/", " ").toUpperCase();
            // log.info(final_exchange);
            // convert `CsvToBean` object to list of ticker
            List<Ticker> tickerList = csvToBean.parse();
            for(Ticker ticker : tickerList) {
                // log.info("in ticker loop");
                // log.info(ticker.getTicker());
                // stringRedisTemplate.opsForValue().set("beforeCreateTicker", "now");
                ticker.setExchange(final_exchange);
                if(ticker.getDate() >= min_load_date) {
                    if(ticker.getDate() >= current_load_date){
                        ticker.setMostrecent("true");
                    } else {
                        ticker.setMostrecent("false");
                    }
                    String returnVal = tickerRepository.create(ticker);
                    // stringRedisTemplate.opsForValue().set("afterCreateTicker", "now");
                }
            }

        } catch (Exception ex) {
            log.info("error");
        }
        return "ok";
    }


    public void createIndex() {
        if (env.getProperty("write_json", "false") == "true") {
            indexType = IndexDefinition.Type.JSON;
            fieldPrefix = "$.";
        } else {
            indexType = IndexDefinition.Type.HASH;
            fieldPrefix = "";
        }
        IndexDefinition indexRule = new IndexDefinition(indexType).setPrefixes(new String[]{"ticker:"});
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
                Connection connection = jedisPool.getResource();
                String string_connect = connection.toString();
                log.info ("a connection is " + string_connect);
                String connectString = string_connect.replaceAll("[^\\d.:]", "");
                log.info("connectString is " + connectString);
                String[] connectArray = connectString.split(":");
                JedisPooled jedisPooled = jedis_connection(connectArray[0], Integer.parseInt(connectArray[1]));
                tryIndex(jedisPooled, indexRule, schema);
            });
        } else {
            tryIndex(client, indexRule, schema);
        }
    }
    public void tryIndex(JedisPooled jedis_client, IndexDefinition indexRule, Schema schema) {
        log.info("rebuilding index on " + jedis_client.getPool().getResource().toString());
        try {
            jedis_client.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(indexRule), schema);
        } catch (Exception e) {
            jedis_client.ftDropIndex(indexName);
            jedis_client.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(indexRule), schema);
        }

    }

    public String convertResults(SearchResult searchResult) throws JsonProcessingException {
        List<Document> docs = searchResult.getDocuments();
        ArrayList<TickerCharacter> tickerList = new ArrayList<TickerCharacter>();
        for ( Document oneDoc : docs) {
            TickerCharacter tickerRec = new TickerCharacter();
            Iterable<Map.Entry<String, Object>> properties = oneDoc.getProperties();
            tickerRec.setId(oneDoc.getId());
            for (Map.Entry<String, Object> oneProp : properties) {
                String key = oneProp.getKey();
                String value = oneProp.getValue().toString();
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
}