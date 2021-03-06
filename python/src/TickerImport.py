#!/bin/python

import csv
import time
# import shlex, subprocess

import redis
from redis.commands.json.path import Path
import sys
import re
import datetime
from os import environ
import os.path
from multiprocessing import Pool

from Ticker import Ticker

maxInt = sys.maxsize


def main():
    # global redis_pool
    # print("PID %d: initializing redis pool..." % os.getpid())
    # redis_pool = redis.ConnectionPool(host='localhost', port=6379, db=0)

    ticker_file_location = environ.get('TICKER_FILE_LOCATION', "../data/ticker")
    print("passed in ticker file location " + ticker_file_location)


    load_directory(ticker_file_location)


def load_directory(ticker_file_location):
    print("Starting productimport.py at " + str(datetime.datetime.now()), flush=True)
    number_processes = int(environ.get('PROCESSES', '1'))
    print("passed in PROCESSES " + str(number_processes), flush=True)
    print("File directory is " + ticker_file_location + " processes is " + str(number_processes), flush=True)
    startTime = time.time()
    startTimeChar = str(datetime.datetime.now())
    print("process_files_parallel()" + str(startTime))
    for (dirpath, dirnames, filenames) in os.walk(ticker_file_location):
        # print("dirpath=" + dirpath)
        # print(dirnames)
        # print(filenames)
        process_files_parallel(dirpath, filenames, number_processes)
    # process_file("/data/daily/us/nysestocks/1/asix.us.txt")
    end_time = time.time()
    print("processing complete. start was " + startTimeChar + " end was " + str(datetime.datetime.now()) +
          " total time " + str(int(end_time - startTime)) + " seconds", flush=True)


def process_file(file_name):
    contains_txt = file_name.find("txt")
    process_dates = "false"
    process_recents = "false"
    not_recent_dates = set()
    do_load = True

    redis_server = environ.get('REDIS_SERVER', 'localhost')
    redis_port = int(environ.get('REDIS_PORT', '6379'))
    redis_password = environ.get('REDIS_PASSWORD', "")

    if redis_password is not None:
        conn = redis.Redis(redis_server, redis_port, password=redis_password,
                           decode_responses=True)
    else:
        conn = redis.Redis(redis_server, redis_port, decode_responses=True)
    # print("starting process_file with file name " + file_name + " contains text is " + str(contains_txt))
    if contains_txt == -1:
        print("skipping unkown file type " + file_name)
        return
    min_load_date = int(environ.get('OLDEST_VALUE'))
    current_load_date = int(environ.get('CURRENT_VALUE'))

    if environ.get('PROCESS_RECENTS') is not None:
        not_recent_dates = conn.smembers('remove_current')
        process_recents = environ.get('PROCESS_RECENTS')

    with open(file_name, mode="r", encoding='utf-8') as csv_file:
        # file is tab delimited
        csv_reader = csv.DictReader(csv_file, delimiter=',', quoting=csv.QUOTE_NONE)
        ticker_idx = 0
        ticker_loaded = 0
        #  go through all rows in the file
        start_time = str(datetime.datetime.now())
        short_file_name = os.path.basename(file_name)
        directory_name = os.path.dirname(file_name)
        # print("directory name is " + directory_name)
        market_identifier = directory_name.replace("/data/daily/", "")
        # print(market_identifier)
        final_market = re.sub('\d', "", market_identifier).replace('/', " ").upper()
        # print("final market  ", final_market)

        for row in csv_reader:
            #  increment ticker_idx and use as incremental part of the key
            # print(row)
            ticker_idx += 1
            nextTicker = Ticker(**row)
            do_load = True
            nextTicker.exchange = final_market
            # print(nextTicker)
            if int(nextTicker.date) >= current_load_date:
                nextTicker.mostrecent = 'true'
                # print("set mostrecent true")
            else:
                nextTicker.mostrecent = 'false'
                if int(nextTicker.date) < min_load_date:
                    do_load = False
                    # print("do_load should be false and not loading ticker date " + nextTicker.Date + " with min load date " + str(min_load_date))

            # clear any recent days
            if process_recents == "true":
                for date in not_recent_dates:
                    if conn.exists(nextTicker.TICKER_PREFIX + nextTicker.ticker + ':' + str(date)):
                        conn.hset(nextTicker.TICKER_PREFIX + nextTicker.ticker + ':' + str(date), 'mostrecent', 'false')
            if do_load:
                ticker_loaded += 1
                if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
                    conn.json().set(nextTicker.get_key(), Path.root_path(), nextTicker.__dict__)
                else:
                    conn.hset(nextTicker.get_key(), mapping=nextTicker.__dict__)
                # this write is for debug to know what line failed on
            conn.set("ticker_highest_idx" + short_file_name, ticker_idx)

            if ticker_idx % 50000 == 0:
                print(str(ticker_idx) + " rows from file " + short_file_name + " loaded to redis " + str(ticker_loaded))
                print("rows added from start " + start_time + " ended at " + str(datetime.datetime.now()))
        csv_file.close()
        # print(str(ticker_idx) + " rows from file " + short_file_name + " loaded to redis " + str(ticker_loaded))
        # print("rows added from start " + start_time + " ended at " + str(datetime.datetime.now()))
        conn.hset("ticker_load", short_file_name, "start:" + start_time + ":finished:" + str(datetime.datetime.now())
                  + ":rows_in_file:" + str(ticker_idx) + ":rows_loaded:" + str(ticker_loaded))
        conn.close()

def process_files_parallel(dirname, names, num_processes: int):
    # Process each file in parallel via Poll.map()
    print("starting process_files_parallel")
    pool = Pool(processes=num_processes)
    results = pool.map(process_file, [os.path.join(dirname, name)
                                      for name in names if (name.find("txt") > -1 and not name.startswith('.'))])


def process_files(dirname, names):
    ''' Process each file in via map() '''
    results = map(process_file, [os.path.join(dirname, name) for name in names])


if '__main__' == __name__:
    main()
