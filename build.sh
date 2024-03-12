#!/bin/sh
TP_PONDERMATIC_VERSION=`cat VERSION`
clojure -T:build clean || exit $?
clojure -T:build jar :version '"'$TP_PONDERMATIC_VERSION'"' || exit $?
clojure -T:build install :version '"'$TP_PONDERMATIC_VERSION'"' || exit $?
./build-shadow.sh || exit $?
