#!/bin/sh

echo "Running NodeJS tests"
rm -rf out
npx shadow-cljs -A:cljs:test release :test || exit $?
node --enable-source-maps out/node-tests.js
