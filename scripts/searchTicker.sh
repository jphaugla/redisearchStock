# curl -X GET -H "Content-Type: application/json" 'http://localhost:5000/search/?search_column=ticker&search_string=aa&sort_column=volume'
curl -X GET -H "Content-Type: application/json" 'http://localhost:5000/search/?search_column=ticker&search_string=aa&sort_column=volume&most_recent=true'
