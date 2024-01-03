#!/bin/sh

echo "Running JVM tests"
clojure -X:test :dirs "[\"src/example\" \"test\"]" :patterns "[\"example.*\" \"pondermatic.*\"]"
