#!/bin/sh

echo "Running NodeJS tests"
./build-shadow.sh --pseudo-names || exit $?
npm test
