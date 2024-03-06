(ns pondermatic.data
  (:require [hasch.core :as h]
            [hasch.benc :as hb]
            [clojure.core.protocols :as ccp]
            [tick.core :as t]
            [incognito.base :as ib]
            [incognito.transit :refer [incognito-write-handler]]
            [pondermatic.reader :as r]
            [cljc.java-time.period]
            [cljc.java-time.duration]
            [odoyle.rules :as o]
            [pondermatic.portal.utils :as p.util]
            [clojure.walk :as w]
            #?(:cljs
               [java.time :refer [LocalDate LocalDateTime Period Duration]])
            #?(:cljs [cognitect.transit :as transit]))
  #?(:clj
     (:import [java.time LocalDate LocalDateTime Period Duration])))

(defn ->type-sym [x]
  (-> x type pr-str symbol))

(def write-handlers
  (-> {}
      (into (map (juxt ->type-sym (constantly str))
                 [(t/date)
                  (t/date-time)
                  (t/new-duration 1 :seconds)
                  (t/new-period 1 :days)]))
      (into (map (juxt ->type-sym (constantly keys))
                 [(o/map->AlphaNode {})
                  (o/map->Binding {})
                  (o/map->Condition {})
                  (o/map->Fact {})
                  (o/map->JoinNode {})
                  (o/map->Match {})
                  (o/map->MemoryNode {})
                  (o/map->Rule {})
                  (o/map->Session {})
                  (o/map->Token {})]))))

(defn ^:private --coerce [this md-create-fn write-handlers]
  (hb/-coerce
   (ib/incognito-writer write-handlers this)
   md-create-fn
   write-handlers))

(extend-protocol
 hb/PHashCoercion
  Period
  (-coerce [this md-create-fn write-handlers]
    (--coerce this md-create-fn write-handlers))
  Duration
  (-coerce [this md-create-fn write-handlers]
    (--coerce this md-create-fn write-handlers))
  LocalDate
  (-coerce [this md-create-fn write-handlers]
    (--coerce this md-create-fn write-handlers))
  LocalDateTime
  (-coerce [this md-create-fn write-handlers]
    (--coerce this md-create-fn write-handlers)))

(defn uuid-hash [x]
  (h/uuid x :write-handlers write-handlers))

(extend-protocol
 ccp/Datafiable
  Period
  (datafy [d]
    (ib/incognito-writer write-handlers d))
  Duration
  (datafy [d]
    (ib/incognito-writer write-handlers d))
  LocalDate
  (datafy [d]
    (ib/incognito-writer write-handlers d))
  LocalDateTime
  (datafy [d]
    (ib/incognito-writer write-handlers d)))

(r/add-readers {'time/date t/date
                'time/date-time t/date-time
                'time/period cljc.java-time.period/parse
                'time/duration cljc.java-time.duration/parse})

(defn ->eql [node]
  (p.util/pprint
   (w/postwalk (fn [node]
                 (cond
                   (map-entry? node)
                   (let [[k v] node]
                     (if (or (map? v) (vector? v))
                       [k v]
                       [k nil]))

                   (map? node)
                   (reduce-kv (fn [a k v]
                                (conj a (if v
                                          {k v}
                                          k)))
                              []
                              node)

                   :else node))
               node)))

(defn ->log-safe [node]
  (p.util/pprint
   (w/postwalk (fn [node]
                 (let [node-str (pr-str node)]
                   (cond
                     (p.util/exception? node) node
                     (uuid? node) node
                     (record? node) (into {} node)
                     (fn? node) (pr-str node)
                     #?@(:cljs [(= \# (first node-str)) node-str])
                     :else node)))
               node)))

#?(:cljs
   (do
     (def transit-json-reader (transit/reader :json))

     (defn read-transit [msg]
       (transit/read transit-json-reader msg))

     (def transit-json-writer (transit/writer :json {:default-handler (incognito-write-handler write-handlers)}))

     (defn write-transit [msg]
       (try
         (transit/write transit-json-writer msg)
         (catch js/Error e
           (js/console.warn e)
           (js/console.warn (pr-str msg))
           (transit/write transit-json-writer (p.util/datafy-value
                                               (cond
                                                 (and (sequential? msg) (map? (last msg)))
                                                 (let [[id cmd msg] msg]
                                                   [id cmd (assoc msg :result e)])

                                                 (and (associative? msg) (:worker msg))
                                                 (assoc-in msg [:worker :result] e)

                                                 (and (associative? msg) (:result msg))
                                                 (assoc msg :result e)

                                                 (associative? msg)
                                                 {:err e}

                                                 (sequential? msg)
                                                 [nil :error e]

                                                 :else e))))))))
