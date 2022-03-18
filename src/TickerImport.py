#!/bin/python

import csv
import time
# import shlex, subprocess

import redis
from redis.commands.json.path import Path
import sys
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
    print("Starting productimport.py at " + str(datetime.datetime.now()))
    startTime = time.time()
    startTimeChar = str(datetime.datetime.now())

    if environ.get('TICKER_FILE_LOCATION') is not None:
        ticker_file_location = (environ.get('TICKER_FILE_LOCATION'))
        print("passed in ticker file location " + ticker_file_location)
    else:
        ticker_file_location = "../data/ticker"
        print("no passed in index file location ")
    if environ.get('PROCESSES') is not None:
        numberProcesses = int(environ.get('PROCESSES'))
        print("passed in PROCESSES " + str(numberProcesses))
    else:
        numberProcesses = 1
        print("no passed in number of processes ")


    print("process_files_parallel()" + str(startTime))
    for (dirpath, dirnames, filenames) in os.walk(ticker_file_location):
        # print("dirpath=" + dirpath)
        # print(dirnames)
        # print(filenames)
        process_files_parallel(dirpath, filenames, numberProcesses)
    # process_file("/data/daily/us/nysestocks/1/asix.us.txt")
    endTime = time.time()
    print("processing complete. start was " + startTimeChar + " end was " + str(datetime.datetime.now()) +
           " total time " + str(int(endTime - startTime)) + " seconds")

def process_file(file_name):

    redis_password = ""
    contains_txt = file_name.find("txt")
    process_dates = "false"
    process_recents = "false"
    not_recent_dates = set()
    do_load = True

    # print("starting process_file with file name " + file_name + " contains text is " + str(contains_txt))
    if contains_txt == -1:
        print("skipping unkown file type " + file_name)
        return

    if environ.get('REDIS_SERVER') is not None:
        redis_server = environ.get('REDIS_SERVER')
        # print("passed in redis server is " + redis_server)
    else:
        redis_server = 'localhost'
        # print("no passed in redis server variable ")

    if environ.get('REDIS_PORT') is not None:
        redis_port = int(environ.get('REDIS_PORT'))
        # print("passed in redis port is " + str(redis_port))
    else:
        redis_port = 6379
        # print("no passed in redis port variable ")


    if environ.get('REDIS_PASSWORD') is not None:
        redis_password = environ.get('REDIS_PASSWORD')
        print("passed in redis password is " + redis_password)

    if redis_password is not None:
        conn = redis.StrictRedis(redis_server, redis_port, password=redis_password,
                                 decode_responses=True)
    else:
        conn = redis.StrictRedis(redis_server, redis_port, decode_responses=True)

    if environ.get('PROCESS_DATES') is not None:
        process_dates = (environ.get('PROCESS_DATES'))
        # print("passed in PROCESS_DATES " + process_dates)
        min_load_date = int(conn.hget("process_control", "oldest_value"))
        current_load_date = int(conn.hget("process_control", "current_value"))

    if environ.get('PROCESS_RECENTS') is not None:
        not_recent_dates = conn.smembers('remove_current')
        process_recents = environ.get('PROCESS_RECENTS')

    with open(file_name) as csv_file:
        # file is tab delimited
        csv_reader = csv.DictReader(csv_file, delimiter=',', quoting=csv.QUOTE_NONE)
        ticker_idx = 0
        ticker_loaded = 0
        #  go through all rows in the file
        start_time = str(datetime.datetime.now())
        short_file_name = os.path.basename(file_name)

        for row in csv_reader:
            #  increment ticker_idx and use as incremental part of the key
            # print(row)
            ticker_idx += 1
            nextTicker = Ticker(**row)
            do_load = True
            # print(nextTicker)
            if process_dates == "true":

                if int(nextTicker.Date) >= current_load_date:
                    nextTicker.MostRecent = 'true'
                else:
                    nextTicker.MostRecent = 'false'
                    if int(nextTicker.Date) < min_load_date:
                        do_load = False
                        # print("do_load should be false and not loading ticker date " + nextTicker.Date + " with min load date " + str(min_load_date))

            # clear any recent days
            if process_recents == "true":
                for date in not_recent_dates:
                    if conn.exists(nextTicker.TICKER_PREFIX + nextTicker.Ticker + ':' + str(date)):
                        # print("writing MostRecent False " + nextTicker.TICKER_PREFIX + nextTicker.Ticker + ':' + str(date))
                        conn.hset(nextTicker.TICKER_PREFIX + nextTicker.Ticker + ':' + str(date), 'MostRecent', 'false')
            if do_load:
                ticker_loaded += 1
                if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
                    conn.json().set(nextTicker.get_key(), Path.rootPath(), nextTicker.__dict__)
                else:
                    conn.hset(nextTicker.get_key(), mapping=nextTicker.__dict__)
                # this write is for debug to know what line failed on
            conn.set("ticker_highest_idx" + short_file_name, ticker_idx)

            if ticker_idx % 50000 == 0:
                print(str(ticker_idx) + " rows from file " + short_file_name + " loaded to redis " + str(ticker_loaded))
                print ("rows added from start " + start_time + " ended at " + str(datetime.datetime.now()))
        csv_file.close()
        # print(str(ticker_idx) + " rows from file " + short_file_name + " loaded to redis " + str(ticker_loaded))
        # print("rows added from start " + start_time + " ended at " + str(datetime.datetime.now()))
        conn.hset("ticker_load", short_file_name + "start:" + start_time + ":finished:" + str(datetime.datetime.now()))
          + ":rows_in_file:" + str(ticker_idx) + ":rows_loaded:" + str(ticker_loaded)





def process_files_parallel(dirname, names, numProcesses: int):
    # Process each file in parallel via Poll.map()
    print("starting process_files_parallel")
    pool = Pool(processes=numProcesses)
    results = pool.map(process_file, [os.path.join(dirname, name) for name in names if (name.find("txt") > -1)])


def process_files(dirname, names):
    ''' Process each file in via map() '''
    results = map(process_file, [os.path.join(dirname, name) for name in names])


if '__main__' == __name__:
    main()
