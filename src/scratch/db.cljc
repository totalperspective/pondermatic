

(ns scratch.db
  (:import (missionary Cancelled))
  (:require [asami.core :as d]
            [asami.datom]
            [missionary.core :as m]
            [clojure.core.protocols :as ccp]
            [clojure.datafy :refer [datafy]]
            [portal.console :as log]))

(extend-protocol ccp/Datafiable
  asami.datom.Datom
  (datafy [^asami.datom.Datom datom]
    (asami.datom/as-vec datom)))

(defn name->mem-uri [db-name]
  (str "asami:mem://" db-name))

(defn conn [db-uri]
  (fn [success failure]
    (if-let [conn (d/connect db-uri)]
      (success conn)
      (failure (Cancelled.)))))

(defn db [<conn]
  (m/sp
   (let [conn (m/? <conn)]
     (d/db conn))))

(defn q [query <db]
  (m/sp
   (let [db (m/? <db)]
     (d/q query db))))

(defn transact [<conn tx]
  (m/sp
   (let [conn (m/? <conn)]
     @(d/transact conn tx))))

(defn <transact [conn take-tx]
  (m/sp
   (loop [tx-report nil]
     (let [tx (m/? take-tx)]
       (if (identical? tx take-tx)
         tx-report
         (let [tx-report (m/? (transact conn tx))]
           (log/trace {:task :transact
                       :tx tx
                       :tx-report tx-report})
           (recur tx-report)))))))

(defn <each [give xs]
  (m/sp
   (loop [xs (seq xs)]
     (if-some [[x & xs] xs]
       (do (log/trace {:task :each
                       :x x})
           (m/? (give x))
           (recur xs))
       (m/? (give give))))))

(defn transact* [conn txs]
  (let [stream (m/rdv)]
    (m/join {} (<each stream txs) (<transact conn stream))))
