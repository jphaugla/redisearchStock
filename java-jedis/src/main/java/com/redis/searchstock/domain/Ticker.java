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

    private String Geography;
    @CsvBindByName(column = "<TICKER>")
    private String Ticker;
    private String TickerShort;
    @CsvBindByName(column = "<PER>")
    private String Per;
    @CsvBindByName(column = "<DATE>")
    private Integer Date;
    @CsvBindByName(column = "<TIME>")
    private Integer Time;
    @CsvBindByName(column = "<OPEN>")
    private float Open;
    @CsvBindByName(column = "<HIGH>")
    private float High;
    @CsvBindByName(column = "<LOW>")
    private float Low;
    @CsvBindByName(column = "<CLOSE>")
    private float Close;
    @CsvBindByName(column = "<VOL>")
    private float Volume;
    @CsvBindByName(column = "<OPENINT>")
    private String OpenInt;
    private String MostRecent;
    private String Exchange;

}
