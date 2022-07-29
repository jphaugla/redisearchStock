package com.redis.searchstock.controller;

import com.redis.searchstock.service.RediSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;


import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*")
@RequestMapping("/")
@RestController
public class RediSearchRestController {

    @Inject
    RediSearchService rediSearchService;

    @GetMapping("/status")
    public String status() {
        return "OK";
    }

    @GetMapping("/search/")
    public Map<String,Object> search(
            @RequestParam(name="search_column")String searchColumn,
            @RequestParam(name="search_string")String searchString,
            @RequestParam(name="most_recent", defaultValue = "false")String mostRecent,
            @RequestParam(name="sort_column", defaultValue="")String sortBy,
            @RequestParam(name="ascending", defaultValue="false")boolean ascending) {
        String TickerSearch = '@' + searchColumn + ':' + searchString + '*';

        if (mostRecent == "true") {
            TickerSearch = TickerSearch + "@mostrecent:{ true }";
        }
        log.info("before search TickerSearch = " + TickerSearch);
        return rediSearchService.search(TickerSearch, 0, 15, sortBy, ascending);
    }

    @GetMapping("/index")
    public String index() {

        rediSearchService.createIndex();

        return "Done";
    }


    @GetMapping("/movies/search")
    public Map<String,Object> search(
            @RequestParam(name="q")String query,
            @RequestParam(name="offset", defaultValue="0")int offset,
            @RequestParam(name="limit", defaultValue="10")int limit,
            @RequestParam(name="sortby", defaultValue="")String sortBy,
            @RequestParam(name="ascending", defaultValue="true")boolean ascending) {
        return rediSearchService.search(query, offset, limit, sortBy,ascending);
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

    @PostMapping("/upload-csv-file")
    public String uploadCSVFile(@RequestParam("directory") String directory) throws IOException {
        // log.info("in uploadCSVFile");
        return rediSearchService.loadStockFiles(directory);
    }

}
