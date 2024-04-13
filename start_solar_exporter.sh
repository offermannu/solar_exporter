#!/usr/bin/env bash

set -o errexit    # exit on error
set -o pipefail   # catch exitcodes in pipes
set -o nounset    # exit when variables are not declared
set -o monitor    # enable Job Control (don't kill sub-processes when shell exits)

cd "$(dirname "$0")"/z2-base.core/bin && \
  java \
    -Djava.util.logging.config.file=logging.properties\
    -DcomponentName=de.atrium.solarlogger.main/run\
    -jar z_embedded.jar
