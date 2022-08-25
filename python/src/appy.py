#!/bin/python
from flask import Flask, jsonify, request, render_template, Response, json
from flask_bootstrap import Bootstrap

from Ticker import Ticker

import jsonpickle
import TickerImport
from RedisClient import RedisClient

app = Flask(__name__)
app.debug = True
bootstrap = Bootstrap()

print("beginning of appy.py now")
db = RedisClient()


@app.route('/', defaults={'path': ''}, methods=['PUT', 'GET', 'POST'])
@app.route('/<path:path>', methods=['PUT', 'GET', 'POST', 'DELETE'])
def home(path):
    return_string = ""
    print("the request method is " + request.method + " path is " + path)

    if request.method == 'DELETE':
        print("delete with path = " + path )
        return_status = db.delete_key(path)
        print("status of " + str(return_status))
        return_string = jsonify(str(return_status), 201)

    elif request.method == 'GET':
        print("GET Method with path " + path)
        if path == 'search/':
            search_column = request.args.get("search_column")
            # print("search column is " + search_column)
            search_str = request.args.get("search_string")
            sort_by = request.args.get("sort_column")
            print("search string is " + search_str)
            ticker_search = "@" + str(search_column) + ":" + str(search_str) + "*"
            most_recent = request.args.get("most_recent")
            if most_recent is not None and most_recent == "true":
                ticker_search = ticker_search + " @mostrecent:{ true }"
            print("TickerSearch is " + ticker_search, flush=True)
            search_return = db.ft_search(ticker_search, sort_by)
            print("total number returned is " + str(search_return.total), flush=True)
            ticker_results = db.process_index_search_results(search_return)
            return_string = jsonpickle.encode(ticker_results)
            # return_string = TickerResults
            print("final return string", flush=True)
            print(return_string, flush=True)
        # this is returning all the rows for the one ticker in the box
        elif path == 'oneticker/':
            get_ticker = request.args.get("ticker")
            print("reporting ticket is ", get_ticker, flush=True)
            sort_by = request.args.get("sort_column")
            ticker_search = "@ticker:" + get_ticker
            print("TickerSearch is " + ticker_search, flush=True)
            search_return = db.ft_search(ticker_search, sort_by)
            print("number returned is " + str(search_return.total), flush=True)
            print("page number " + str(len(search_return.docs)), flush=True)
            ticker_results = db.process_index_search_results(search_return)
            return_string = jsonpickle.encode(ticker_results)
            print(return_string, flush=True)
        elif path == 'index':
            db.recreate_index()
            return_string = "Done"
        elif path == 'key':
            get_key = request.args.get("keyValue")
            return_status = db.get_ticker(get_key)
            return_string = jsonify(str(return_status), 201)
        elif path == 'field':
            get_key = request.args.get("keyValue")
            get_field = request.args.get("field")
            print("get field key is " + get_key + " field is " + get_field, flush=True)
            return_status = db.get_ticker_field(get_key, get_field)
            return_string = jsonify(str(return_status), 201)
        else:
            print("in the GET before call to index.htm with path ")
            response = app.send_static_file('index.html')
            response.headers['Content-Type'] = 'text/html'
            return_string = response
    elif request.method == 'POST':
        if path == 'upload-csv-file':
            get_directory = request.args.get("directory")
            print("loading files with this directory " + get_directory, flush=True)
            TickerImport.load_directory(get_directory)
            return_string = "Done"
        else:
            return_status = 0
            print('in POST')
            event = request.json
            print('event is %s ' % event)
            nextTicker = Ticker(**event)
            return_status = db.write_ticker(nextTicker)
            return_string = jsonify(str(return_status), 201)
    elif request.method == "PUT":
        get_field = request.args.get("field")
        get_value = request.args.get("value")
        get_key = request.args.get("key")
        print("setting on key = " + get_key + " field = " + get_field + " with value of " + str(get_value))
        db.set_field(get_key, get_field, get_value)

    return return_string


@app.after_request  # blueprint can also be app~~
def after_request(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, DELETE'
    if response.headers['Content-Type'] != 'text/html':
        response.headers['Content-Type'] = 'application/json'
    return response


def isInt(s):
    try:
        int(s)
        return True
    except ValueError:
        return False


if __name__ == "__main__":
    app.run(host='0.0.0.0')
