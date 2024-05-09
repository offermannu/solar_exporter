#!/usr/bin/env bash

# Grafana Export Dashboards
# @author
# @date
# $Id$

set -o errexit  # exit on error
set -o pipefail # catch exitcodes in pipes
set -o nounset  # exit when variables are not declared
# set -o xtrace   # debug

# Set magic variables for current file & dir
__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
__file="${__dir}/$(basename "${BASH_SOURCE[0]}")"
__base="$(basename ${__file} .sh)"

# ask for admin password and set API-URL with basic authentication
AUTH=""
HTTP_URL="https://solarlogger.wohnhof-wiesloch.de/api"
WGET_ARGS=
DASHBOARD_BASE_DIR="$__dir/dashboards"

#
# optional dashboard query
#
QUERY=${1:-}

function http_get() {
  path=$1
  out=$2
  curl --silent --header "Authorization: Bearer $AUTH" --header "Accept: application/json" --write "%{stderr}%{response_code}" "$HTTP_URL$path" > $out 2> _response_code.txt || true
  response_code=$(cat _response_code.txt)
  case $response_code in
    3* )
      error="Unexpected Redirect"
      ;;
    400)
      error="Bad Request"
      ;;
    401)
      error="Unauthorized - invalid user or password: $GRAFANA_USER:********"
      ;;
    403)
      error="Forbidden"
      ;;
    404)
      error="Not found"
      ;;
    4* )
      error="Client Error $response_code"
      ;;
    5*|6*|7*|8*|9*)
      error="Server Error $response_code"
      ;;
    000)
      error="Not reached (Server down or wrong URL)"
      ;;
  esac
  if [[ "${error:-}" != "" ]]; then
    echo "GET $HTTP_URL$path"
    echo $error
    exit 1
  fi
}

# check auth
echo "Checking authorization"
#http_get "/admin/stats" _stats.json
#result=$(jq -r '.message // "__SUCCESS__"' _stats.json)
#echo
#if [[ "__SUCCESS__" != "$result" ]]; then
#  echo "$result"
#  exit 1
#fi

# retrieve all dashboard UIDs and update dashboard files
echo "updating dashboard files in $DASHBOARD_BASE_DIR"
http_get "/search?type=dash-db&query=$QUERY" _uids.json
jq -r '.[].uid' _uids.json > _uids.txt
echo "Dashboards: "
cat _uids.txt
for uid in $(cat _uids.txt) ; do
  echo "Exporting dashboard uid=$uid"
  http_get "/dashboards/uid/$uid" _export.json
  # the whole json consists of
  # { "meta": {...}, "dashboard": {...} }

  # .meta.provisionedExternalId contains the path like "server/nosql.json" which matches the folder structure in the SVN workspace
  path=$(jq -r '.meta.provisionedExternalId' _export.json)
  if [[ -z "$path" ]]; then
    path="$uid.json"
  fi

  # .dashboard contains the dashboard definition - this is saved
  echo "updating $path"
  jq -r '.dashboard' _export.json > "$DASHBOARD_BASE_DIR/$path"
done

echo "done."
