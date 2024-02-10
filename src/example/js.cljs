(ns example.js
  (:require [pondermatic.index :as i]))

;; (defonce p (i/portal "vs-code"))

(def engine (i/create-engine "test" true))

(def q (i/q engine
            "[:find ?id ?v :where [?id :foo/key ?v]]"
            []
            prn))

(def e (i/entity* engine "test" prn))

(i/sh engine #js {"->db" (i/dataset #js [#js {"id" "test" "foo/key" "value" "foo/nothing" nil}])})

(q)
(e)

