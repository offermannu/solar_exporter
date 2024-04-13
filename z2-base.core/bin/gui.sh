#!/bin/sh
#
# start
# 
cd $(dirname $0)
. ./env.sh
./go.sh -mode debug - -gui
