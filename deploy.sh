#!/bin/sh
TP_PONDERMATIC_DATE=`date -u '+%Y%m%d-%H%M%S'`
git tag v$TP_PONDERMATIC_DATE
clojure -T:build clean
clojure -T:build jar :version '"'$TP_PONDERMATIC_DATE'"'
clojure -T:build install :version '"'$TP_PONDERMATIC_DATE'"'

clojure -T:build deploy :version '"'$TP_PONDERMATIC_DATE'"'