#!/bin/sh
if [ -f .env ]; then
  source .env
fi

if [ -z "$CLOJARS_USERNAME" ]; then
  echo "CLOJARS_USERNAME is not set"
  exit 1
fi
if [ -z "$CLOJARS_PASSWORD" ]; then
  echo "CLOJARS_PASSWORD is not set"
  exit 1
fi
export CLOJARS_USERNAME=$CLOJARS_USERNAME
export CLOJARS_PASSWORD=$CLOJARS_PASSWORD

TP_PONDERMATIC_VERSION=`cat VERSION`

clojure -T:build deploy :version '"'$TP_PONDERMATIC_VERSION'"' :sign-releases true || exit $?
npm publish --access public || exit $?
