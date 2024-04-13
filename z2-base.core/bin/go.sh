#!/bin/sh
#
# start
# 
cd $(dirname $0)
. ./env.sh
`java -jar z.jar $*`
