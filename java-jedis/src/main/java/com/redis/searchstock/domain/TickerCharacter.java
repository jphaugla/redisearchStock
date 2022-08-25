package com.redis.searchstock.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.regex.Pattern;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TickerCharacter implements Serializable {
    // <TICKER>,<PER>,<DATE>,<TIME>,<OPEN>,<HIGH>,<LOW>,<CLOSE>,<VOL>,<OPENINT>
    private static final String Prefix="ticker:";
    private String id;
    private String geography;
    private String ticker;
    private String tickershort;
    private String per;
    private String date;
    private String time;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private String openint;
    private String mostrecent;
    private String exchange;

    public void createID() {
        id = Prefix + getTicker() + ':' + getDate().toString();
    }

    public String[] createTickerShortGeography() {
        String[] parts = getTicker().split(Pattern.quote("."));
        // log.info("after split with parts " + parts[0]);
        return parts;
    }
}
