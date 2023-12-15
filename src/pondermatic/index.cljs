(ns pondermatic.index
  (:require [pondermatic.core :as p]
            [pondermatic.rules :as r]
            [pondermatic.flow :as flow]
            [missionary.core :as m]
            [pondermatic.portal.utils :as portal]
            [clojure.walk :as w]
            [clojure.edn :as edn]
            [hasch.core :as h]))

;; (defn portal
;;   ([]
;;    (portal nil))
;;   ([launcher]
;;    (portal/start (keyword launcher))))

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
      (portal/trace 'ruleset)))

(defn dataset [dataset]
  (-> dataset
      (js->clj :keywordize-keys true)
      p/dataset
      (portal/trace 'dataset)))

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
             (tap> {:query q
                    :result (portal/table result)})
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
      ;;  :portal portal
       :dispose dispose!})
