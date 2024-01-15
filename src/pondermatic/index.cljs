(ns pondermatic.index
  (:require [pondermatic.core :as p]
            [pondermatic.rules :as r]
            [pondermatic.flow :as flow]
            [missionary.core :as m]
            [pondermatic.portal.utils :as p.util]
            [pondermatic.rules.production :as prp]
            [clojure.walk :as w]
            [clojure.edn :as edn]
            [hasch.core :as h]
            [cljs.pprint :as pp]
            [pondermatic.portal.client :as portal]
            [portal.console :as log]
            [promesa.core :as pa]
            [edn-query-language.core :as eql]
            [cognitect.transit :as t]))

(def readers
  {'rule (fn [[id when then]]
           {:id id
            :rule/when when
            :rule/then then})
   'mutation (fn [mutation]
               (let [{:keys [call params query]} (eql/expr->ast mutation)]
                 {:mutation/call call
                  :mutation/params params
                  :mutation/query query}))
   'ruleset (fn [ruleset]
              (->> ruleset
                   (mapv (fn [[id rule]]
                           (assoc rule :id id)))
                   p/ruleset))
   'dataset (fn [dataset]
              (prn (meta dataset))
              (p/dataset dataset))})

(defn read-string [str]
  (edn/read-string {:readers readers
                    :default (fn [tag value]
                               {:reader.unknown/tag tag
                                :reader.unknown/value value})} str))

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
       read-string
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
  (-> (if (string? ruleset)
        (read-string ruleset)
        ruleset)
      (js->clj :keywordize-keys true)
      parse-rules
      p/ruleset
      (p.util/trace 'ruleset)))

(defn dataset [dataset]
  (-> (if (string? dataset)
        (read-string dataset)
        dataset)
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
              read-string
              (p.util/trace :parsed-query))
        args (js->clj args)
        q<> (apply p/q<> engine q args)]
    (flow/drain
     (m/ap (let [q< (m/? q<>)
                 result (m/?< q<)]
             (log/trace {:q/query q
                         :q/args args
                         :q/result (p.util/table result)})
             (cb (clj->js result)))))))


(defn entity [engine ident cb]
  (log/trace {:entity ident})
  (let [ident (-> ident
                  js->clj
                  str
                  read-string)
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
                  read-string)
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

(defn unify [expr-or-str env]
  (let [expr (if (string? expr-or-str)
               (read-string expr-or-str)
               (js->clj expr-or-str))
        env (reduce-kv (fn [m k v]
                         (let [k (if (= \? (first k))
                                   (symbol k)
                                   k)]
                           (assoc m k v)))
                       {}
                       (js->clj env))]
    (log/trace {:unify/expr expr
                :unfy/env env})
    (try
      (clj->js (prp/unify-pattern expr env))
      (catch js/Error e
        (log/error e)
        (throw e)))))

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

(def transit-json-reader (t/reader :json))

(def transit-json-writer (t/writer :json))

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
       :unify unify
       :pprint #(-> % js->clj pp/pprint)
       :addTap (fn
                 ([] (add-tap pp/pprint))
                 ([tap] (add-tap (-> tap))))
       :readString read-string
       :encode (partial t/write transit-json-writer)
       :decode (partial t/read transit-json-reader)})
