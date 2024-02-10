#!/bin/sh
TP_PONDERMATIC_VERSION=`npm version | grep pondermatic | cut -d\' -f4`
clojure -T:build clean
clojure -T:build jar :version '"'$TP_PONDERMATIC_VERSION'"'
clojure -T:build install :version '"'$TP_PONDERMATIC_VERSION'"'
