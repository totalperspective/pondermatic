(ns pondermatic.index
  (:require [pondermatic.core :as p]
            [pondermatic.rules :as r]
            [pondermatic.flow :as flow]
            [missionary.core :as m]
            [pondermatic.portal.utils :as p.util]
            [clojure.walk :as w]
            [clojure.edn :as edn]
            [hasch.core :as h]
            [cljs.pprint :as pp]
            [pondermatic.portal.client :as portal]
            [portal.console :as log]))

(defn portal
  ([]
   (portal nil))
  ([launcher]
   (portal/start (when launcher
                   (keyword launcher)))))

(defn hash-id [js-obj]
  (-> js-obj
      (js->clj :keywordize-keys true)
      h/edn-hash
      h/uuid5))

(defn create-engine
  ([name]
   (create-engine name false))
  ([name reset-db?]
   (p/->engine name :reset-db? reset-db?)))

(defn ->edn [form]
  (->> form
       str
       edn/read-string
       (w/postwalk (fn [node]
                     #_{:clj-kondo/ignore [:unresolved-symbol]}
                     (if (instance? cljs.core.MapEntry  node)
                       (let [[attr val] node]
                         (if (symbol? attr)
                           [(keyword (str attr)) val]
                           [attr val]))
                       node)))))

(defn parse-rule [rule]
  (-> rule
      (update :rule/when ->edn)
      (update :rule/then ->edn)))

(defn parse-rules [rules]
  (mapv parse-rule rules))

(defn ruleset [ruleset]
  (-> ruleset
      (js->clj :keywordize-keys true)
      parse-rules
      p/ruleset
      (p.util/trace 'ruleset)))

(defn dataset [dataset]
  (-> dataset
      (js->clj :keywordize-keys true)
      p/dataset
      (p.util/trace 'dataset)))

(defn sh [engine msg]
  (p/|> engine (-> msg
                   (js->clj :keywordize-keys true)
                   (p.util/trace 'sh))))

(defn add-rules-msg [rules]
  (r/add-rules (-> rules
                   (js->clj :keywordize-keys true)
                   (p.util/trace 'add-rules-msg))))

(defn q [engine q args cb]
  (let [q (-> q
              edn/read-string
              (p.util/trace 'q))
        q<> (apply p/q<> engine q args)]
    (flow/drain
     (m/ap (let [q< (m/? q<>)
                 result (m/?< q<)]
             (log/debug {:query q
                         :result (p.util/table result)})
             (cb (clj->js result)))))))

(defn dispose! [task]
  (task))

(defn error-info [e]
  (-> e
      ex-data
      clj->js))

(def exports
  #js {:createEngine create-engine
       :ruleset ruleset
       :dataset dataset
       :sh sh
       :addRulesMsg add-rules-msg
       :q q
       :hashId hash-id
       :errorInfo error-info
       :portal portal
       :dispose dispose!
       :pprint #(-> % js->clj pp/pprint)
       :addTap (fn
                 ([] (add-tap pp/pprint))
                 ([tap] (add-tap (-> tap))))})
