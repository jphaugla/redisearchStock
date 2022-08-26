package com.redis.searchstock.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.searchstock.domain.Ticker;
import com.redis.searchstock.service.RediSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import redis.clients.jedis.search.SearchResult;


import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@CrossOrigin(origins = "*")
@RequestMapping("/")
@RestController
@Configuration

public class RediSearchRestController implements WebMvcConfigurer {

    @Inject
    RediSearchService rediSearchService;
    ObjectMapper mapper = new ObjectMapper();
    @GetMapping("/status")
    public String status() {
        return "OK";
    }


    @GetMapping("/search/")
    String search(
            @RequestParam(name="search_column")String searchColumn,
            @RequestParam(name="search_string")String searchString,
            @RequestParam(name="most_recent", defaultValue = "false")String mostRecent,
            @RequestParam(name="sort_column", defaultValue="")String sortBy,
            @RequestParam(name="ascending", defaultValue= "false")boolean ascending) throws JsonProcessingException {

        String TickerSearch = '@' + searchColumn + ':' + searchString + '*';

        if (mostRecent.equals("true")) {
            TickerSearch = TickerSearch + "@mostrecent:{ true }";
        }
        log.info("before search TickerSearch = " + TickerSearch);
        SearchResult searchResult = rediSearchService.search(TickerSearch, 0, 15, sortBy, ascending);
        String returnList = rediSearchService.convertResults(searchResult);

        return returnList;
    }

    // this is returning all the rows for the one ticker in the box;
    @GetMapping("/oneticker/")
    public String oneTicker(
            @RequestParam(name="ticker")String getTicker,
            @RequestParam(name="sort_column", defaultValue="no")String sortBy) throws JsonProcessingException {
        String TickerSearch = "@ticker:" + getTicker;
        SearchResult searchResult = rediSearchService.search(TickerSearch,0,15, sortBy, Boolean.parseBoolean("false"));
        String returnList = rediSearchService.convertResults(searchResult);
        return returnList;
    }

    @GetMapping("/key")
    public Ticker getKey(
            @RequestParam(name="keyValue")String keyValue){
        return rediSearchService.getKey(keyValue);
    }

    @GetMapping("/field")
    public String getField(
            @RequestParam(name="keyValue")String keyValue,
            @RequestParam(name="fieldValue")String fieldValue){
        log.info("in controller getField");
        return rediSearchService.getField(keyValue, fieldValue);
    }

    @GetMapping("/index")
    public String index() {
        rediSearchService.createIndex();
        return "Done";
    }

    @PostMapping("/upload-csv-file")
    public String uploadCSVFile(@RequestParam("directory") String directory) throws IOException {
        // log.info("in uploadCSVFile");
        return rediSearchService.loadStockFiles(directory);
    }

    @PostMapping("/postTicker")
    public String postTicker(@RequestBody Ticker ticker) throws IOException {
        // log.info("in uploadCSVFile");
        return rediSearchService.createTicker(ticker);
    }

    @PutMapping("/setfield")
    public String setField(@RequestParam String key,
                           @RequestParam String field,
                           @RequestParam String value) {
        return rediSearchService.setField(key, field, value);
    }

    @GetMapping("/movies/group_by/{field}")
    public Map<String,Object> getMovieGroupBy(@PathVariable("field") String field) {
        return rediSearchService.getMovieGroupBy(field);
    }

    @DeleteMapping("/ticker/{tickerKey}")
    public String deleteTicker (@PathVariable String tickerKey) {

        return rediSearchService.deleteTicker(tickerKey.replace("\\",""));
    }

}
