package com.redis.searchstock.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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


    @GetMapping("/movies/group_by/{field}")
    public Map<String,Object> getMovieGroupBy(@PathVariable("field") String field) {
        return rediSearchService.getMovieGroupBy(field);
    }

    @GetMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> getMOvieById(@PathVariable("movieId") String movieId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messsage", "This movie endpoint is not implemented in Java, use the Node.js Endpoint");
        return result;
    };

    @PostMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> saveMovie(@PathVariable("movieId") String movieId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messsage", "This movie endpoint is not implemented in Java, use the Node.js Endpoint");
        return result;
    };

    @GetMapping("/movies/{movieId}/comments")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> getMovieComments(@PathVariable("movieId") String movieId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messsage", "Comment API not implemented in Java, use the Node.js Endpoint");
        return result;
    };

    @PostMapping("/movies/{movieId}/comments")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> createMovieComments(@PathVariable("movieId") String movieId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messsage", "Comment API not implemented in Java, use the Node.js Endpoint");
        return result;
    };

    @GetMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> getCommentById(@PathVariable("commentId") String commentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messsage", "Comment API not implemented in Java, use the Node.js Endpoint");
        return result;
    };

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> deleteCommentById(@PathVariable("commentId") String commentId) {
        Map<String, Object> result = new HashMap<>();
        result.put("messsage", "Comment API not implemented in Java, use the Node.js Endpoint");
        return result;
    };

}
