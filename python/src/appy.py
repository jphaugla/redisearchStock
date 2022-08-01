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
import TickerImport

app = Flask(__name__)
app.debug = True
bootstrap = Bootstrap()

redis_server = environ.get('REDIS_SERVER', 'localhost')
redis_port = int(environ.get('REDIS_PORT', '6379'))
redis_index = environ.get('REDIS_INDEX', 'ticker')

print("beginning of appy.py now")


@app.route('/', defaults={'path': ''}, methods=['PUT', 'GET', 'POST'])
@app.route('/<path:path>', methods=['PUT', 'GET', 'POST', 'DELETE'])
def home(path):
    redis_password = ""
    return_string = ""
    print("the request method is " + request.method + " path is " + path)
    if environ.get('REDIS_PASSWORD') is not None:
        redis_password = environ.get('REDIS_PASSWORD')
        # print("passed in redis password is " + redis_password)

    if redis_password is not None:
        db = redis.Redis(redis_server, redis_port, password=redis_password,
                               decode_responses=True)
    else:
        db = redis.Redis(redis_server, redis_port, decode_responses=True)

    if request.method == 'PUT':
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
                TickerSearch = TickerSearch + " @mostrecent:{ true }"
            q1 = Query(TickerSearch)
            if sort_by is not None:
                q1.sort_by(sort_by, asc=False)
            print("TickerSearch is " + TickerSearch, flush=True)
            TickerReturn = db.ft(index_name=redis_index).search(q1)
            print("total number returned is " + str(TickerReturn.total), flush=True)
            print("page number " + str(len(TickerReturn.docs)))
            # print("TickerReturn")
            # print(TickerReturn)
            # print("TickerReturn docs 0")
            # print("docs array 0 ", flush=True)
            # print(TickerReturn.docs[0], flush=True)
            # print("TickerReturn docs 0 id")
            # print(TickerReturn.docs[0].id, flush=True)
            # print("TickerReturn docs 0 json", flush=True)
            # print(TickerReturn.docs[0].json, flush=True)
            # print("TickerReturn docs 0 json tickershort", flush=True)
            TickerResults = []
            print("length of return " + str(len(TickerReturn.docs)))
            for i in range(len(TickerReturn.docs)):
                if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
                    print("in write json with ")
                    doc_results = TickerReturn.docs[i]
                    json_results = json.loads(doc_results.json)
                    results = Ticker(id=doc_results.id, payload=doc_results.payload,
                                     ticker=json_results["ticker"],
                                     tickershort=json_results["tickershort"], open=json_results["open"],
                                     close=json_results["close"], high=json_results["high"], low=json_results["low"],
                                     volume=json_results["volume"])
                else:
                    results = TickerReturn.docs[i]
                TickerResults.append(results)
                print("results prints")
                print(results, flush=True)
            # return_string = jsonify(TickerResults, 200)
            return_string = jsonpickle.encode(TickerResults)
            # return_string = TickerResults
            print("final return string", flush=True)
            print(return_string, flush=True)
        # this is returning all the rows for the one ticker in the box
        elif path == 'oneticker/':
            get_ticker = request.args.get("ticker")
            print("reporting ticket is ", get_ticker)
            sort_by = request.args.get("sort_column")
            TickerSearch = "@ticker:" + get_ticker
            q1 = Query(TickerSearch).paging(0, 200)
            if sort_by is not None:
                q1.sort_by(sort_by, asc=False)
            print("TickerSearch is " + TickerSearch)
            TickerReturn = db.ft(index_name=redis_index).search(q1)
            print("number returned is " + str(TickerReturn.total))
            print("page number " + str(len(TickerReturn.docs)))
            # print("TickerReturn")
            # print(TickerReturn)
            # print("TickerReturn docs 0")
            # print(TickerReturn.docs[0])
            # print("TickerReturn docs 0 id")
            # print(TickerReturn.docs[0].id)
            # print("TickerReturn.docs[0].json")
            # print(TickerReturn.docs[0].json)
            # print("TickerReturn docs 0 tickershort")
            # print(TickerReturn.docs[0].tickershort)
            TickerResults = []
            for i in range(len(TickerReturn.docs) - 1):
                if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
                    # since json version returns json instead of doc structure with hashes, must parse
                    doc_results = TickerReturn.docs[i]
                    json_results = json.loads(doc_results.json)
                    results = Ticker(id=doc_results.id, payload=doc_results.payload, date=doc_results.date, ticker=json_results["ticker"],
                                     tickershort=json_results["tickershort"], open=json_results["open"],
                                     close=json_results["close"], high=json_results["high"], low=json_results["low"])
                else:
                    results = TickerReturn.docs[i]

                TickerResults.append(results)
                # print("results")
                # print(results)
            # return_string = jsonify(TickerResults, 200)
            # print("jsonpickle.encode")
            return_string = jsonpickle.encode(TickerResults)
            # print(return_string)
        elif path == 'index':
            recreateIndex(db)
            return_string = "Done"
        else:
            print("in the GET before call to index.html")
            response = app.send_static_file('index.html')
            response.headers['Content-Type'] = 'text/html'
            return_string = response
    elif request.method == 'POST':
        if path == 'upload-csv-file':
            get_directory = request.args.get("directory")
            print("loading files with this directory " + get_directory)
            TickerImport.load_directory(get_directory)
            return_string = "Done"
    return return_string


@app.after_request  # blueprint can also be app~~
def after_request(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, DELETE'
    if (response.headers['Content-Type'] != 'text/html'):
        response.headers['Content-Type'] = 'application/json'
    return response


def recreateIndex(db):
    #  if environment is set to write to
    #  jason change the index type and the field prefix
    #  for JSON the field prefix is $.   for hash there is none
    if environ.get('WRITE_JSON') is not None and environ.get('WRITE_JSON') == "true":
        useIndexType = IndexType.JSON
        fieldPrefix = "$."
    else:
        useIndexType = IndexType.HASH
        fieldPrefix = ""

    # no longer filtering the index on MostRecent just selecting on it
    # TickerDefinition = IndexDefinition(prefix=['ticker:'], index_type=useIndexType, score_field='Score', filter="@MostRecent=='true'")
    TickerDefinition = IndexDefinition(prefix=['ticker:'], index_type=useIndexType)
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
    try:
        db.ft(index_name=redis_index).create_index(TickerSCHEMA, definition=TickerDefinition)
    except redis.ResponseError:
        db.ft(index_name=redis_index).dropindex(delete_documents=False)
        db.ft(index_name=redis_index).create_index(TickerSCHEMA, definition=TickerDefinition)


def isInt(s):
    try:
        int(s)
        return True
    except ValueError:
        return False


if __name__ == "__main__":
    app.run(host='0.0.0.0')
