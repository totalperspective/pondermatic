(ns example.js
  (:require [pondermatic.index :as i]
            [hyperfiddle.rcf :refer [tests]]))

;; (defonce p (i/portal "vs-code"))

(def engine (i/create-engine "test" true))

(def q (i/q engine
            "[:find ?id ?v :where [?id :foo/key ?v]]"
            []
            prn))

(i/sh engine #js {"->db" (i/dataset #js [#js {"foo/key" "value" "foo/nothing" nil}])})

(q)

(tests
 (i/read-string "#ruleset []") := [])
