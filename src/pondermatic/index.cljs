(ns pondermatic.index
  (:require [pondermatic.core :as p]
            [pondermatic.rules :as r]))

(defn create-engine
  ([name]
   (create-engine name false))
  ([name reset-db?]
   (p/->engine name :reset-db? reset-db?)))

(defn ruleset [ruleset]
  (-> ruleset
      js->clj
      p/ruleset))

(defn dataset [dataset]
  (-> dataset
      js->clj
      p/dataset))

(defn sh [engine msg]
  (p/|> engine (js->clj msg)))

(defn add-rules-msg [rules]
  (r/add-rules (js->clj rules)))

(def exports
  #js {:create-engine create-engine
       :ruleset ruleset
       :dataset dataset
       :sh sh
       :add-rules-msg add-rules-msg})
