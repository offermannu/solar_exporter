# !/bin/bash
cd $(dirname $0)
. ./env.sh
pid=$(cat home.pid)
kill $pid

#
# wait for it to disappear
#
ps -p $pid 2>&1 > /dev/null
status=$?
while [ "$status" == "0" ]
do
  sleep 1
  ps -p $pid 2>&1 > /dev/null
  status=$?
done


