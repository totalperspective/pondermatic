#!/bin/sh
TP_PONDERMATIC_VERSION=`cat VERSION`
clojure -T:build clean
clojure -T:build jar :version '"'$TP_PONDERMATIC_VERSION'"'
clojure -T:build install :version '"'$TP_PONDERMATIC_VERSION'"'
./build-shadow.sh
