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
            [portal.console :as log]
            [promesa.core :as pa]))

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
             (log/trace {:query q
                         :result (p.util/table result)})
             (cb (clj->js result)))))))


(defn entity [engine ident cb]
  (log/trace {:entity ident})
  (let [ident (-> ident
                  js->clj
                  str
                  edn/read-string)
        entity> (p/entity*> engine ident true)]
    (entity> (fn [entity]
               (log/trace {:entity entity
                           :ident ident})
               (cb (clj->js entity)))
             (fn [e]
               (cb nil e)))))

(defn entity* [engine ident cb]
  (entity entity ident cb)
  (let [ident (-> ident
                  js->clj
                  str
                  edn/read-string)
        entity<> (p/entity<> engine ident true)]
    (flow/drain
     (m/ap (let [entity< (m/? entity<>)
                 entity (m/?< entity<)]
             (log/trace {:entity entity})
             (cb (clj->js entity)))))))

(defn dispose! [task]
  (task))

(defn error-info [e]
  (-> e
      ex-data
      clj->js))

(defn ->promise-fn [cb-fn]
  (fn [& args]
    (let [p (pa/deferred)
          args (conj (vec args)
                     (fn
                       ([result]
                        (pa/resolve! p result))
                       ([_ e]
                        (pa/reject! p e))))]
      (apply cb-fn args)
      p)))

(defn log
  ([expr]
   (log nil expr))
  ([level expr]
   (let [expr (if (instance? js/Error expr)
                expr
                (js->clj expr :keywordize-keys true))]
     (condp = (keyword level)
       :debug (log/debug expr)
       :trace (log/trace expr)
       :info (log/info expr)
       :warn (log/warn expr)
       :error (log/error expr)
       :fatal (log/fatal expr)
       (if (instance? js/Error expr)
         (log/error expr)
         (log/log expr))))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def exports
  #js {:createEngine create-engine
       :ruleset ruleset
       :dataset dataset
       :sh sh
       :addRulesMsg add-rules-msg
       :q q
       :qP (->promise-fn q)
       :entity entity
       :entityP (->promise-fn entity)
       :watchEntity entity*
       :hashId hash-id
       :errorInfo error-info
       :portal portal
       :dispose dispose!
       :log log
       :pprint #(-> % js->clj pp/pprint)
       :addTap (fn
                 ([] (add-tap pp/pprint))
                 ([tap] (add-tap (-> tap))))})
