#!/bin/sh

echo "Running NodeJS tests"
npx shadow-cljs release :npm :esm
npx --node-options=--experimental-vm-modules jest
