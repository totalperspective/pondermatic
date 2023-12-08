(ns pondermatic.index
  (:require [pondermatic.core :as p]
            [pondermatic.rules :as r]
            [pondermatic.flow :as flow]
            [missionary.core :as m]
            [pondermatic.portal :as portal]
            [clojure.edn :as edn]))

(defn portal [launcher]
  (portal/start (keyword launcher)))

(defn create-engine
  ([name]
   (create-engine name false))
  ([name reset-db?]
   (p/->engine name :reset-db? reset-db?)))

(defn ruleset [ruleset]
  (-> ruleset
      (js->clj :keywordize-keys true)
      (portal/trace 'ruleset)
      p/ruleset))

(defn dataset [dataset]
  (-> dataset
      (js->clj :keywordize-keys true)
      (portal/trace 'dataset)
      p/dataset))

(defn sh [engine msg]
  (p/|> engine (-> msg
                   (js->clj :keywordize-keys true)
                   (portal/trace 'sh))))

(defn add-rules-msg [rules]
  (r/add-rules (-> rules
                   (js->clj :keywordize-keys true)
                   (portal/trace 'add-rules-msg))))

(defn q [engine q args cb]
  (let [q (-> q
              edn/read-string
              (portal/trace 'q))
        q<> (apply p/q<> engine q args)]
    (flow/drain
     (m/ap (let [q< (m/? q<>)
                 result (m/?< q<)]
             (cb (clj->js result)))))))

(defn dispose! [task]
  (task))

(def exports
  #js {:createEngine create-engine
       :ruleset ruleset
       :dataset dataset
       :sh sh
       :addRulesMsg add-rules-msg
       :q q
       :portal portal
       :dispose dispose!})
