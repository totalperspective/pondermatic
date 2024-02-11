#!/bin/sh

echo "Running NodeJS tests"
./build-shadow.sh || exit $?
npm test
