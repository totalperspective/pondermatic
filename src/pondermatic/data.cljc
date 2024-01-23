(ns pondermatic.data
  (:require [hasch.core :as h]
            [hasch.benc :as hb]
            [clojure.core.protocols :as ccp]
            [tick.core :as t]
            [incognito.base :as ib]
            #?(:cljs
               [java.time :refer [LocalDate LocalDateTime Period Duration]]))
  #?(:clj
     (:import [java.time LocalDate LocalDateTime Period Duration])))

(defn ->type-sym [x]
  (-> x type pr-str symbol))

(def write-handlers
  (into {} (map (juxt ->type-sym (constantly str))
                [(t/date)
                 (t/date-time)
                 (t/new-duration 1 :seconds)
                 (t/new-period 1 :days)])))

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
