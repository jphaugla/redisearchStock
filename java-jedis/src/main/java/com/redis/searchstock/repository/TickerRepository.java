package com.redis.searchstock.repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.searchstock.domain.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Repository
public class TickerRepository {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    ObjectMapper mapper = new ObjectMapper();
    public  String create(Ticker ticker) {
        // log.info("in uploadCSVFile");
        String[] tickershort = ticker.createTickerShortGeography();
        // String tickershort = "AAA";
        // log.info("after createtickershort " + tickershort);
        ticker.setTickershort(tickershort[0]);
        ticker.setGeography(tickershort[1]);
        // some test code to eliminate nulls
        // ticker.setGeography("US");
        // ticker.setMostRecent("true");
        // ticker.setExchange("US NYSE");
        Map<Object,Object> TickerHash = mapper.convertValue(ticker, Map.class);
        String tickerKey = ticker.createID();
        // log.info("tickerKey is " + tickerKey);
        redisTemplate.opsForHash().putAll(tickerKey, TickerHash);

        return "Success\n";
    }

}
