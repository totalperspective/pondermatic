(ns pondermatic.eval
  (:require [sci.core :as sci]
            [pondermatic.reader :as pr]
            [clojure.string :as str]
            [hasch.core :as h]
            [tick.core :as t]
            [tick.locale-en-us]
            [inflections.core :as i]
            [camel-snake-kebab.core :as csk]
            [pondermatic.data :as pd]
            #?(:clj
               [clojure.math :as math]
               :cljs
               [cljs.math :as math])
            #?(:cljs
               [java.time :refer [LocalDate LocalDateTime Period Duration]]))
  #?(:clj
     (:import [java.time LocalDate LocalDateTime Period Duration])))

(def nss
  (let [add_ #(str/replace % " " "_")
        normalize (comp add_ str/lower-case)]
    {'hash {'uuid pd/uuid-hash
            'squuid h/squuid
            'b64 h/b64-hash}
     'inflection {'plural i/plural
                  'singular i/singular
                  'ordinalize i/ordinalize}
     'case {'normalize normalize
            'upper str/upper-case
            'lower str/lower-case
            'camel (comp csk/->camelCase normalize)
            'kebab (comp csk/->kebab-case normalize)
            'snake (comp csk/->snake_case normalize)}
     'math (sci/copy-ns #?(:clj clojure.math :cljs cljs.math)
                        (sci/create-ns #?(:clj 'clojure.math :cljs 'cljs.math))
                        {:exclude []})
     'str (sci/copy-ns clojure.string
                       (sci/create-ns 'clojure.string)
                       {:exclude []})
     't (sci/copy-ns tick.core (sci/create-ns 'tick.core) {:exclude []})}))

(def locals {'LocalDate LocalDate
             'LocalDateTime LocalDateTime
             'Period Period
             'Duration Duration})

(def default-scope
  (reduce-kv (fn [m ns fns]
               (reduce-kv (fn [m fn impl]
                            (let [sym (symbol (str ns "." fn))]
                              (assoc m sym impl)))
                          m
                          fns))
             locals
             nss))

(defn eval-string
  ([s]
   (eval-string s {}))
  ([s opts]
   (let [opts (merge-with merge
                          {:namespaces {'user default-scope}
                           :readers @pr/!readers}
                          opts)]
     (sci/eval-string s opts))))
