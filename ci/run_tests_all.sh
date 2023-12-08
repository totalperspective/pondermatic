#!/bin/bash

base="$(dirname "$0")"
./$base/run_tests_jvm.sh
./$base/run_tests_node.sh
./$base/run_tests_browser.sh
shadow-cljs compile :esm :npm
npm test
