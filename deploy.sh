#!/bin/sh
TP_PONDERMATIC_VERSION=`npm version | grep pondermatic | cut -d\' -f2`

clojure -T:build deploy :version '"'$TP_PONDERMATIC_VERSION'"'
npm publish --access public
