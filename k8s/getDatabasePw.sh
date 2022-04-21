echo "information on redis-enterprise-database\n"
kubectl -n demo get secret redb-redis-enterprise-database -o go-template='{{range $k,$v := .data}}{{"### "}}{{$k}}{{"\n"}}{{$v|base64decode}}{{"\n\n"}}{{end}}'
# sleep is used because some of the commands are async
sleep 5
echo "\nredis-enterprise-database password\n"
kubectl -n demo get secret redb-redis-enterprise-database -o=jsonpath={.data.password} | base64 -d
