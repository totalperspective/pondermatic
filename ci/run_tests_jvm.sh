#!/bin/sh

echo "Running JVM tests"
rm -rf target
clojure -X:test :dirs "[\"src/example\" \"test\"]" :patterns "[\"example.*\" \"pondermatic.*\"]"
