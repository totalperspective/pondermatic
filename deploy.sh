#!/bin/sh
TP_PONDERMATIC_VERSION=`cat VERSION`

clojure -T:build deploy :version '"'$TP_PONDERMATIC_VERSION'"'
npm publish --access public
