#!/bin/sh

echo "Running NodeJS tests"
./node_modules/.bin/shadow-cljs release :npm :esm
env NODE_OPTIONS=--experimental-vm-modules npm run test
