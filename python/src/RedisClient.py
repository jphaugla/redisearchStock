import redis
from redis.commands.search.field import TextField, TagField, NumericField
from redis.commands.search.indexDefinition import IndexDefinition, IndexType
from redis.commands.search.query import NumericFilter, Query
from redis.commands.json.path import Path

from os import environ

from flask import json

from Ticker import Ticker


class RedisClient:
    def __init__(self):
        self.redis_server = environ.get('REDIS_HOST', 'localhost')
        self.redis_port = int(environ.get('REDIS_PORT', '6379'))
        self.redis_password = environ.get('REDIS_PASSWORD', "")
        self.redis_index = environ.get('REDIS_INDEX', 'Ticker')
        self.write_json = environ.get('WRITE_JSON', "false")
        try:
            if self.redis_password is not None:
                self.conn = redis.Redis(self.redis_server, self.redis_port,
                                        password=self.redis_password, decode_responses=True)
            else:
                self.conn = redis.Redis(self.redis_server, self.redis_port,
                                        decode_responses=True)
        except redis.RedisError:
            print(f'Redis failed connection to {self.redis_server}:{self.redis_port}.')
            return

    def write_ticker(self, ticker):
        return_val = 0
        if self.write_json == "true":
            return_val = self.write_json_ticker(ticker)
        else:
            return_val = self.write_hash_ticker(ticker)
        return return_val

    def get_ticker(self, ticker_key):
        if self.write_json == "true":
            return_data = self.conn.json().get(ticker_key)
        else:
            return_data = self.conn.hgetall(ticker_key)
        return return_data

    def get_ticker_field(self, ticker_key, field):
        return_data = ""
        if self.write_json == "true":
            return_data = self.conn.json().get(ticker_key, field)
        else:
            return_data = self.conn.hgetall(ticker_key, field)
        return return_data

    def write_json_ticker(self, ticker):
        return self.conn.json().set(ticker.get_key(), Path.root_path(),
                             ticker.__dict__)

    def delete_key(self, path):
        return self.conn.delete(path)

    def set_field(self, key, field, value):
        if self.conn.exists(key):
            if self.write_json == "true":
                self.conn.json().set(key, field, value)
            else:
                self.hset(key, field, value)

    def write_hash_ticker(self, ticker):
        return self.conn.hset(ticker.get_key(), mapping=ticker.__dict__)

    def update_most_recent(self, ticker, in_date):
        ticker_key = ticker.TICKER_PREFIX + ticker.ticker + ':' + str(in_date)
        self.set_field(ticker_key, "mostrecent", "false")

    def update_load_tracker(self, short_file_name, ticker_idx):
        self.conn.set("ticker_highest_idx" + short_file_name, ticker_idx)

    def update_process_tracker(self, short_file_name, start_time, datetime, ticker_idx, ticker_loaded):
        self.conn.hset("ticker_load", short_file_name, "start:" + start_time + ":finished:" + str(datetime.datetime.now())
                  + ":rows_in_file:" + str(ticker_idx) + ":rows_loaded:" + str(ticker_loaded))

    def ft_search(self, query_string, sort_by):
        q1 = Query(query_string)
        if sort_by is not None:
            q1.sort_by(sort_by, asc=False)
        print("TickerSearch is " + query_string, flush=True)
        search_return = self.ft_query(q1)
        return search_return

    def ft_query(self, q1):
        ticker_return = self.conn.ft(index_name=self.redis_index).search(q1)
        return ticker_return

    def ft_create_index(self, ticker_schema, ticker_definition):
        try:
            self.conn.ft(index_name=self.redis_index).create_index(ticker_schema,
                                                                   definition=ticker_definition)
        except redis.ResponseError:
            self.conn.ft(index_name=self.redis_index).dropindex(delete_documents=False)
            self.conn.ft(index_name=self.redis_index).create_index(ticker_schema,
                                                                   definition=ticker_definition)

    def recreate_index(self):
        #  if environment is set to write to
        #  jason change the index type and the field prefix
        #  for JSON the field prefix is $.   for hash there is none
        if self.write_json == "true":
            useIndexType = IndexType.JSON
            fieldPrefix = "$."
        else:
            useIndexType = IndexType.HASH
            fieldPrefix = ""

        # no longer filtering the index on MostRecent just selecting on it
        # TickerDefinition = IndexDefinition(prefix=['ticker:'], index_type=useIndexType, score_field='Score', filter="@MostRecent=='true'")
        TickerDefinition = IndexDefinition(prefix=[Ticker.TICKER_PREFIX], index_type=useIndexType)
        TickerSCHEMA = (
            TextField(fieldPrefix + "ticker", as_name='ticker', no_stem=True),
            TextField(fieldPrefix + "tickershort", as_name='tickershort', no_stem=True),
            # TagField(fieldPrefix + "Per", separator=";", as_name='Per'),
            TagField(fieldPrefix + "mostrecent", as_name='mostrecent'),
            NumericField(fieldPrefix + "date", as_name='date', sortable=True),
            # NumericField(fieldPrefix + "open", as_name='open'),
            # NumericField(fieldPrefix + "high", as_name='high'),
            # NumericField(fieldPrefix + "low", as_name='low'),
            # NumericField(fieldPrefix + "close", as_name='close'),
            NumericField(fieldPrefix + "volume", as_name='volume', sortable=True),
            # NumericField(fieldPrefix + "score", as_name='score'),
            # TagField(fieldPrefix + "openint", separator=";", as_name='openint')
        )

        print("before try on Ticker")
        self.ft_create_index(TickerSCHEMA, TickerDefinition)

    def process_index_search_results(self, index_return_data):
        ticker_results = []
        for i in range(len(index_return_data.docs)):
            if self.write_json == "true":
                # since json version returns json instead of doc structure with hashes, must parse
                doc_results = index_return_data.docs[i]
                json_results = json.loads(doc_results.json)
                results = Ticker(id=doc_results.id, date=json_results["date"],
                                 ticker=json_results["ticker"],
                                 tickershort=json_results["tickershort"], open=json_results["open"],
                                 close=json_results["close"], high=json_results["high"], low=json_results["low"],
                                 volume=json_results["volume"])
            else:
                results = index_return_data.docs[i]

            ticker_results.append(results)
            # print("results")
            # print(results)
        # return_string = jsonify(TickerResults, 200)
        # print("jsonpickle.encode")

        return ticker_results
