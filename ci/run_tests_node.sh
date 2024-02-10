#!/bin/sh

echo "Running NodeJS tests"
./node_modules/.bin/shadow-cljs -A:cljs:test release :test || exit $?
node --enable-source-maps out/node-tests.js
