(ns example.js
  (:require [pondermatic.index :as i]))

(def engine (i/create-engine (str (gensym "test")) true))

(def q (i/q engine
            "[:find [?v ...]
              :where
              [?id :foo/key ?v]]"
            []
            #(-> % js->clj prn)))

(def e (i/entity* engine ":test" prn))
(i/sh engine #js {"->db" (i/dataset #js [#js {"id" "test" "foo/key" "value" "foo/nothing" nil}])})
(i/sh engine #js {"->db" (i/dataset #js [#js {"id" "test2" "foo/key" "value2" "foo/nothing" 1}])})

(i/stop engine)
(q)
(e)
