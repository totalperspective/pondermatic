(ns example.clone
  (:require [pondermatic.index :as p]
            [asami.core :as d]))

(reset! d/connections {})

(def engine-1 (p/create-engine (str (gensym "example.clone")) true))

(p/sh engine-1 {:->db (p/ruleset '[{:id :test
                                    :rule/when {:id 1 :key ?value}
                                    :rule/then {:id 2 :key ?value}}])})

(p/sh engine-1 {:->db (p/dataset [{:id 1 :key :value}])})

(def engine-2 (p/copy engine-1))

(p/sh engine-2 {:->db (p/dataset [{:id 1 :key' :value2}])})
