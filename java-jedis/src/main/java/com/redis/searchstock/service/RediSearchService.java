package com.redis.searchstock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.redis.searchstock.domain.Ticker;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.*;

import redis.clients.jedis.search.*;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.aggr.SortedField;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


import java.io.*;

import java.net.URI;
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

    JedisPooled client;
    private static final String Prefix="ticker:";

    String indexName = "idx:movie"; // default name
    String redisUrl = "redis://localhost:6379"; // default name
    ObjectMapper mapper = new ObjectMapper();
    int min_load_date;
    int current_load_date;

    @PostConstruct
    private void init() throws URISyntaxException {
        log.info("Init RediSearchService");

        // Get the configuration from the application properties/environment
        indexName = env.getProperty("redis.index","Ticker");
        redisUrl = env.getProperty("redis.url","redis://localhost:6379");

        log.info("Configuration Index: " + indexName + " - redisUrl: " + redisUrl);

        client = new JedisPooled(new URI(redisUrl));

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
    public Map<String, Object> search(String queryString, int offset, int limit, String sortBy, boolean ascending) {
        // Let's put all the informations in a Map top make it easier to return JSON object
        // no need to have "predefine mapping"
        Map<String, Object> returnValue = new HashMap<>();
        Map<String, Object> resultMeta = new HashMap<>();

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
        resultMeta.put("queryString", queryString);

        // Get the total number of documents and other information for this query:
        resultMeta.put("totalResults", queryResult.getTotalResults());
        resultMeta.put("offset", offset);
        resultMeta.put("limit", limit);

        returnValue.put("meta", resultMeta);

        // the docs are returned as an array of document, with the document itself being a list of k/v json documents
        // not the easiest to manipulate
        // the `raw_docs` is used to view the structure
        // the `docs` will contain the list of document that is more developer friendly
        //      capture in  https://github.com/RediSearch/JRediSearch/issues/121
        // returnValue.put("raw_docs", queryResult.docs);
        returnValue.put("raw_docs", queryResult.getDocuments());


        // remove the properties array and create attributes
        List<Map<String, Object>> docsToReturn = new ArrayList<>();
        List<Document> docs = queryResult.getDocuments();

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

        return returnValue;
    }

    public Map<String, Object> search(String queryString) {
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
            String min_date = (String) stringRedisTemplate.opsForHash().get("process_control", "oldest_value");
            String current_date = (String) stringRedisTemplate.opsForHash().get("process_control", "current_value");
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
                        ticker.setMostRecent("true");
                    } else {
                        ticker.setMostRecent("false");
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


}