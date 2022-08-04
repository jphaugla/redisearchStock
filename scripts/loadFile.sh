# curl -X POST -F "upload=@aadr.us.txt" "http://localhost:8085/api/1.0/upload-csv-file"
#  this works for java
# curl -v -X POST 'http://localhost:5000/upload-csv-file?directory=data/daily/us/nyse%20etfs'
#  this works for python
curl -v -X POST 'http://localhost:5000/upload-csv-file?directory=/data/daily/us/nyse%20etfs'
