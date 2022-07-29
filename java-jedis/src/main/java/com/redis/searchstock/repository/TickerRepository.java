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
    private static final String Prefix="ticker:";
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    ObjectMapper mapper = new ObjectMapper();
    public String create (Ticker ticker) {
        // log.info("in uploadCSVFile");
        String[] parts = ticker.getTicker().split(Pattern.quote("."));
        // log.info("after split with parts " + parts[0]);
        String tickershort = parts[0];
        String geography = parts[1];
        // String tickershort = "AAA";
        // log.info("after createtickershort " + tickershort);
        ticker.setTickershort(tickershort);
        ticker.setGeography(geography);
        // some test code to eliminate nulls
        // ticker.setGeography("US");
        // ticker.setMostRecent("true");
        // ticker.setExchange("US NYSE");
        Map<Object,Object> TickerHash = mapper.convertValue(ticker, Map.class);
        String tickerKey = Prefix + ticker.getTicker() + ':' + ticker.getDate().toString();
        // log.info("tickerKey is " + tickerKey);
        stringRedisTemplate.opsForValue().set("beforeopsforhash", "now");
        redisTemplate.opsForHash().putAll(tickerKey, TickerHash);
        stringRedisTemplate.opsForValue().set("afteropsforhash", "now");
        return "Success\n";
    }

}
