(ns pondermatic.core
  (:require [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.engine :as engine]
            [pondermatic.shell :as sh]
            [clojure.walk :as w]))

(defn ->engine [name]
  (let [db-uri (db/name->mem-uri name)
        conn (db/->conn db-uri)
        session (rules/->session)]
    (engine/->engine conn session)))

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

(def rule-type :pondermatic/rule)
