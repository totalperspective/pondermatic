#!/bin/sh

echo "Running Browser tests"
rm -rf out
./node_modules/.bin/shadow-cljs -A:cljs:test release :browser-test || exit $?
./node_modules/.bin/karma start --single-run
