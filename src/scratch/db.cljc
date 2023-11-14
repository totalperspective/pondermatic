(ns scratch.db
  (:import (missionary Cancelled))
  (:require [asami.core :as d]
            [asami.datom]
            [missionary.core :as m]
            [clojure.core.protocols :as ccp :refer [nav]]
            [clojure.datafy :refer [datafy]]))

(extend-protocol ccp/Datafiable
  asami.datom.Datom
  (datafy [^asami.datom.Datom datom]
    (asami.datom/as-vec datom)))

(def dbs (atom {}))

(defonce _ (add-watch d/connections ::dbs
                      (fn [_ _ old-v new-v]
                        (prn "Connection change" (keys old-v) (keys new-v))
                        (doall (for [[uri _] (seq old-v)]
                                 (do (prn "OLD?" uri)
                                     (when-not (new-v uri)
                                       (println "Removing" uri)
                                       (swap! dbs (fn [dbs]
                                                    (let [<db (get-in dbs [uri :uri])]
                                                      ;(<db)
                                                      (dissoc dbs uri))))))))
                        (doall (for [[uri db] (seq new-v)]
                                 (do (prn "NEW?" uri)
                                     (when-not (@dbs uri)
                                       (println "Adding" uri)
                                       (swap! dbs (fn [dbs]
                                                    (let [{!state :state name :name} db
                                                          <state (m/watch !state)]
                                                      (-> dbs
                                                          (assoc-in [name :db] <state)
                                                          (assoc-in [name :uri] uri))))))))))))

; (defonce >conns (m/signal (m/watch d/connections)))

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
           (tap> {:task :transact
                  :tx tx
                  :tx-report tx-report})
           (recur tx-report)))))))

(defn <each [give xs]
  (m/sp
   (loop [xs (seq xs)]
     (if-some [[x & xs] xs]
       (do (tap> {:task :each
                  :x x})
           (m/? (give x))
           (recur xs))
       (m/? (give give))))))

(defn transact* [conn txs]
  (let [stream (m/rdv)]
    (m/join {} (<each stream txs) (<transact conn stream))))
