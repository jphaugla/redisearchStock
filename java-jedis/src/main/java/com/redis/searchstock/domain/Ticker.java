package com.redis.searchstock.domain;

import com.opencsv.bean.CsvBindByName;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Ticker implements Serializable {
// <TICKER>,<PER>,<DATE>,<TIME>,<OPEN>,<HIGH>,<LOW>,<CLOSE>,<VOL>,<OPENINT>

    private String geography;
    @CsvBindByName(column = "<TICKER>")
    private String ticker;
    private String tickershort;
    @CsvBindByName(column = "<PER>")
    private String per;
    @CsvBindByName(column = "<DATE>")
    private Integer date;
    @CsvBindByName(column = "<TIME>")
    private Integer time;
    @CsvBindByName(column = "<OPEN>")
    private float open;
    @CsvBindByName(column = "<HIGH>")
    private float high;
    @CsvBindByName(column = "<LOW>")
    private float low;
    @CsvBindByName(column = "<CLOSE>")
    private float close;
    @CsvBindByName(column = "<VOL>")
    private float volume;
    @CsvBindByName(column = "<OPENINT>")
    private String openint;
    private String mostrecent;
    private String exchange;

}
