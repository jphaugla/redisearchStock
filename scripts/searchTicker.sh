# curl -X GET -H "Content-Type: application/json" 'http://localhost:5000/search/?search_column=Ticker&search_string=aa&sort_column=Volume'
curl -X GET -H "Content-Type: application/json" 'http://localhost:5000/search/?search_column=Ticker&search_string=aa&sort_column=Volume&most_recent=true'
