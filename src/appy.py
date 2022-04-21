#!/bin/python
from flask import Flask, jsonify, request, render_template, Response, json
from flask_bootstrap import Bootstrap
import redis
from redis.commands.json.path import Path
from redis import ResponseError
from Ticker import Ticker

from os import environ

from redis.commands.search.field import TextField, TagField, NumericField
from redis.commands.search.indexDefinition import IndexDefinition, IndexType
from redis.commands.search.query import NumericFilter, Query
import jsonpickle


app = Flask(__name__)
app.debug = True
bootstrap = Bootstrap()
if environ.get('REDIS_SERVER') is not None:
    redis_server = environ.get('REDIS_SERVER')
    print("passed in redis server is " + redis_server)
else:
    redis_server = 'localhost'
    print("no passed in redis server variable ")

if environ.get('REDIS_PORT') is not None:
    redis_port = int(environ.get('REDIS_PORT'))
    print("passed in redis port is " + str(redis_port))
else:
    redis_port = 6379
    print("no passed in redis port variable ")

print("beginning of appy.py now")

@app.route('/', defaults={'path': ''}, methods=['PUT', 'GET'])
@app.route('/<path:path>', methods=['PUT', 'GET', 'DELETE'])
def home(path):
    print("the request method is " + request.method + " path is " + path)
    if environ.get('REDIS_PASSWORD') is not None:
        redis_password = environ.get('REDIS_PASSWORD')
        print("passed in redis password is " + redis_password)

    if redis_password is not None:
        db = redis.StrictRedis(redis_server, redis_port, password=redis_password,
                                 decode_responses=True)
    else:
        db = redis.StrictRedis(redis_server, redis_port, decode_responses=True)

    if request.method == 'PUT':
        # if prod_idx is set it is a replace
        # if proc_idx is not set, is insert
        print('in PUT')
        event = request.json
        print('event is %s ' % event)
        nextTicker = Ticker(**event)
        nextTicker.set_key()
        # event['updated'] = int(time.time())
        # db.hmset(path, event)
        if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
            db.json().set(nextTicker.key_name, Path.rootPath(), nextTicker.__dict__)
        else:
            db.hset(nextTicker.key_name, mapping=nextTicker.__dict__)
        return_string = jsonify(nextTicker.__dict__, 201)

    elif request.method == 'DELETE':
        return_status = db.delete(path)
        print("delete with path = " + path + " and status of " + str(return_status))
        return_string = jsonify(str(return_status), 201)

    elif request.method == 'GET':
        print("GET Method with path " + path)
        if path == 'search/':
            search_column = request.args.get("search_column")
            # print("search column is " + search_column)
            search_str = request.args.get("search_string")
            sort_by = request.args.get("sort_column")
            print("search string is " + search_str)
            TickerSearch = "@" + str(search_column) + ":" + str(search_str) + "*"
            most_recent = request.args.get("most_recent")
            if most_recent is not None and most_recent == "true":
                TickerSearch = TickerSearch + " @MostRecent:true"
            q1 = Query(TickerSearch)
            if sort_by is not None:
                q1.sort_by(sort_by, asc=False)
            print("TickerSearch is " + TickerSearch)
            TickerReturn = db.ft(index_name="Ticker").search(q1)
            print("number returned is " + str(TickerReturn.total))
            # print("TickerReturn")
            # print(TickerReturn)
            # print("TickerReturn docs 0")
            # print(TickerReturn.docs[0])
            # print("TickerReturn docs 0 id")
            # print(TickerReturn.docs[0].id)
            # print("TickerReturn docs 0 TickerShort")
            # print(TickerReturn.docs[0].TickerShort)
            TickerResults = []
            for i in range(min(TickerReturn.total - 1, 9)):
                results = TickerReturn.docs[i]
                TickerResults.append(results)
                print(results)
            # return_string = jsonify(TickerResults, 200)
            return_string = jsonpickle.encode(TickerResults)
        # category passed in will be Category name, return Category attributes
        elif path == 'oneticker/':
            get_ticker = request.args.get("ticker")
            print("reporting ticket is ", get_ticker)
            sort_by = request.args.get("sort_column")
            TickerSearch = "@Ticker:" + get_ticker
            q1 = Query(TickerSearch)
            if sort_by is not None:
                q1.sort_by(sort_by, asc=False)
            print("TickerSearch is " + TickerSearch)
            TickerReturn = db.ft(index_name="Ticker").search(q1)
            print("number returned is " + str(TickerReturn.total))
            # print("TickerReturn")
            # print(TickerReturn)
            # print("TickerReturn docs 0")
            # print(TickerReturn.docs[0])
            # print("TickerReturn docs 0 id")
            # print(TickerReturn.docs[0].id)
            # print("TickerReturn docs 0 TickerShort")
            # print(TickerReturn.docs[0].TickerShort)
            TickerResults = []
            for i in range(min(TickerReturn.total - 1, 9)):
                results = TickerReturn.docs[i]
                TickerResults.append(results)
                # print(results)
            # return_string = jsonify(TickerResults, 200)
            return_string = jsonpickle.encode(TickerResults)
        elif path == 'index':
            recreateIndex()
            return_string = "Done"
        else:
             print("in the GET before call to index.html")
             response=app.send_static_file('index.html')
             response.headers['Content-Type']='text/html'
             return_string = response


    return return_string
@app.after_request # blueprint can also be app~~
def after_request(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, DELETE'
    if (response.headers['Content-Type']!='text/html'):
        response.headers['Content-Type'] = 'application/json'
    return response


def recreateIndex():
    #  if environment is set to write to
    #  jason change the index type and the field prefix
    #  for JSON the field prefix is $.   for hash there is none
    if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
        useIndexType = IndexType.JSON
        fieldPrefix = "$."
    else:
        useIndexType = IndexType.HASH
        fieldPrefix = ""

    db = redis.StrictRedis(redis_server, redis_port, charset="utf-8", decode_responses=True)  # connect to server
    # no longer filtering the index on MostRecent just selecting on it
    # TickerDefinition = IndexDefinition(prefix=['ticker:'], index_type=useIndexType, score_field='Score', filter="@MostRecent=='true'")
    TickerDefinition = IndexDefinition(prefix=['ticker:'], index_type=useIndexType, score_field='Score')
    TickerSCHEMA = (
        TextField(fieldPrefix + "Ticker", as_name='Ticker', no_stem=True),
        TextField(fieldPrefix + "TickerShort", as_name='TickerShort', no_stem=True),
        # TagField(fieldPrefix + "Per", separator=";", as_name='Per'),
        TextField(fieldPrefix + "MostRecent", as_name='MostRecent', no_stem=True),
        NumericField(fieldPrefix + "Date", as_name='Date', sortable=True),
        # NumericField(fieldPrefix + "Open", as_name='Open'),
        # NumericField(fieldPrefix + "High", as_name='High'),
        # NumericField(fieldPrefix + "Low", as_name='Low'),
        # NumericField(fieldPrefix + "Close", as_name='Close'),
        NumericField(fieldPrefix + "Volume", as_name='Volume', sortable=True),
        # NumericField(fieldPrefix + "Score", as_name='Score'),
        #TagField(fieldPrefix + "OpenInt", separator=";", as_name='OpenInt')
    )

    print("before try on Ticker")
    try:
        db.ft(index_name="Ticker").create_index(TickerSCHEMA, definition=TickerDefinition)
    except redis.ResponseError:
        db.ft(index_name="Ticker").dropindex(delete_documents=False)
        db.ft(index_name="Ticker").create_index(TickerSCHEMA, definition=TickerDefinition)


def isInt(s):
    try:
        int(s)
        return True
    except ValueError:
        return False




if __name__ == "__main__":
    app.run(host='0.0.0.0')
