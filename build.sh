#!/bin/sh

if [ -z "$1" ]; then
  TP_PONDERMATIC_VERSION=$(cat VERSION)
else
  TP_PONDERMATIC_VERSION=$1
fi

clojure -T:build clean || exit $?
clojure -T:build jar :version '"'$TP_PONDERMATIC_VERSION'"' || exit $?
clojure -T:build install :version '"'$TP_PONDERMATIC_VERSION'"' || exit $?
./build-shadow.sh || exit $?
