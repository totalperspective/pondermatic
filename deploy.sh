#!/bin/sh
TP_PONDERMATIC_VERSION=`cat VERSION`

clojure -T:build deploy :version '"'$TP_PONDERMATIC_VERSION'"' :sign-releases true || exit $?
npm publish --access public || exit $?
