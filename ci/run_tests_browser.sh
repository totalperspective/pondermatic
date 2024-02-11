#!/bin/sh

echo "Running Browser tests"
rm -rf out
npx shadow-cljs -A:cljs:test release :browser-test || exit $?
npx karma start --single-run
