#!/usr/bin/env bash

# sends a Kako status query to /dev/ttyUSB
# Usage:
#   wrquery.sh "#xxz"
#
# Where "#xxz" is a Kako command without the trailing CR:
#        #     it's a query
#         xx   denotes the inverter address 01, 02, ...
#           z  z=0: read status;
#              z=3: read the total yield
#              z=s: read the serial number
#              z=7: delete internal counters *** NOT SUPPORTED ***
#              z=8: read the software version ARM and DSP
#              z=9: read the inverter type
#              z=v: read the software version of each module
# Examples:
#   Requests the status of inverter 02
#   > ./wrquery.sh "#020"
#
#   Requests the serial number of inverter 03
#   > ./wrquery.sh "#03s"

set -o errexit    # exit on error
set -o pipefail   # catch exitcodes in pipes
set -o nounset    # exit when variables are not declared
set -o monitor    # enable Job Control (don't kill sub-processes when shell exits)

# arg 1 defaults to ""
cmd=${1:-""}

# check if the cmd matches the supported syntax
# only the remote commands {0, 3, 8, 9, s, v} are supported!
if [[ "$cmd" =~ \#..[0389s] ]]; then
  stty -F /dev/ttyUSB0 9600 -echo raw
  echo -e "${cmd}\r" | tee /dev/ttyUSB0 && timeout 2s cat /dev/ttyUSB0 &
  echo -e "\r\n" | tee /dev/stdout

else
  echo "Missing or invalid command (cmd=\"${cmd}\")"
  echo "Usage: "
  echo "wrquery.sh [cmd]"
  echo "E.g: wrquery.sh #020"
fi