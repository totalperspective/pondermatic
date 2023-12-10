#!/bin/sh
TP_PONDERMATIC_VERSION=`npm version | grep pondermatic | cut -d\' -f4`

clojure -T:build deploy :version '"'$TP_PONDERMATIC_VERSION'"'
npm publish --access public
