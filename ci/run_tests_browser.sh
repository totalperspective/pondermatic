#!/bin/sh

echo "Running Browser tests"
./node_modules/.bin/shadow-cljs -A:cljs:test release :browser-test
./node_modules/.bin/karma start --single-run