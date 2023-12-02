(ns pondermatic.core
  (:require [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.engine :as engine]
            [pondermatic.shell :as sh]
            [clojure.walk :as w]))

(defn ->engine [name & {:keys [:reset-db?] :or {reset-db? false}}]
  (let [db-uri (db/name->mem-uri name)
        conn (db/->conn db-uri reset-db?)
        session (rules/->session)]
    (engine/->engine conn session)))

(defn kw->qkw
  ([data]
   (kw->qkw data "data"))
  ([data ns]
   (w/postwalk (fn [node]
                 #_{:clj-kondo/ignore [:unresolved-symbol]}
                 (if (instance? #?(:clj clojure.lang.IMapEntry :cljs cljs.core.IMapEntry)  node)
                   (let [[attr val] node]
                     (if (and (keyword? attr) (nil? (namespace attr)))
                       [(keyword ns (name attr)) val]
                       [attr val]))
                   node))
               data)))

(defn id->ident
  ([data]
   (id->ident data :id))
  ([data id-attr]
   (w/postwalk (fn [node]
                 #_{:clj-kondo/ignore [:unresolved-symbol]}
                 (if (instance? #?(:clj clojure.lang.IMapEntry :cljs cljs.core.IMapEntry)  node)
                   (let [[attr val] node]
                     (if (= attr id-attr)
                       [:db/ident val]
                       [attr val]))
                   node))
               data)))

(def |> sh/|>)

(def |< sh/|<)

(def |>< sh/|><)

(def type-name engine/type-name)

(def rule-type engine/rule-type)

(defn ruleset [rules]
  (-> (map #(assoc % type-name rule-type) rules)
      id->ident
      kw->qkw))

(defn dataset [data & {:keys [id-attr ns] :or {id-attr :id ns "data"}}]
  (-> data
      (id->ident id-attr)
      (kw->qkw ns)))

(def conn> engine/conn>)

(def rule-atom engine/rule-atom)
