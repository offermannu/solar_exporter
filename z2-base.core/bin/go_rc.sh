#!/bin/sh
#
# start script for init scripts (launches in background and creates PID file)
# 
cd $(dirname $0)
. ./env.sh
`java -Xmx16M -jar z.jar - -np` 2>/dev/null &
echo $! > home.pid
