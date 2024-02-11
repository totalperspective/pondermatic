#!/bin/sh

echo "Running JVM tests"
rm -rf target
clojure -X:test :dirs "[\"src\" \"test\"]" :patterns "[\"example.*\" \"pondermatic.*\"]"
