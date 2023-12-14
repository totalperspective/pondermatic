#!/bin/sh

echo "Running NodeJS tests"
npx shadow-cljs release :npm :esm
env NODE_OPTIONS=--experimental-vm-modules npx jest
