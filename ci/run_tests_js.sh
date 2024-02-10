#!/bin/sh

echo "Running NodeJS tests"
rm -rf out
npx shadow-cljs release :npm :esm || exit $?
npx --node-options="--experimental-vm-modules" jest
