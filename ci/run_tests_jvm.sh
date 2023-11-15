#!/bin/sh

echo "Running JVM tests"
clojure -X:test :dirs "[\"example\" \"src\"]" :patterns "[\"example.*\" \"pondermatic.*\"]"
