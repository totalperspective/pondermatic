(ns user
  (:require [pondermatic.db :as db]
            [missionary.core :as m]
            [portal.api :as p]
            [asami.core :as d]
            [clojure.string :as str]))

(defonce _ (let [p (p/open {:launcher :vs-code})]
             (add-tap #'p/submit)
             p)) ; Add portal as a tap> target

(defn tap [prefix]
  (map (fn [x]
         (prn {prefix x})
         x)))

(defn run [flow]
  (flow #(tap> {"Success" %}) #(tap> {"Error" %
                                      :info (str/split-lines (.-stack %))})))

(defn drain [prefix flow]
  (run (m/eduction (tap prefix) flow)))

(def db-name
  (str (gensym "user")))
(def db-uri (db/name->mem-uri db-name))

(def conn (db/conn db-uri))
(def db (db/db conn))
;; (drain (db/<db conn) ::db)

(defn -main
  []
  (let [txs [[{:db/ident "id1" :name :test}]
             [{:db/ident "id2" :name :test2}]
             [{:db/ident "id3" :name :test3 :other-name :other}]]
        query '[:find ?name :where [_ :name ?name]]]
    (run (db/transact* conn txs))
    (run (db/q query db)))
  (d/delete-database db-uri))

(defonce reload? (atom 0))

(when @reload?
  (-main))

(swap! reload? inc)

(comment
  (defn crash [^Throwable e]                                ;; let it crash philosophy
    (.printStackTrace e)
    (.exit js/process -1))

  (defn actor
    ([init] (actor init crash))
    ([init fail]
     (let [self (m/mbx)]
       ((m/sp
         (loop [b init]
           (recur (b self (m/? self)))))
        nil fail)
       self)))

  (def counter
    (let [beh (fn beh [n]
                (fn [self cust]
                  (cust n)
                  (beh (inc n))))]
      (actor (beh 0))))

  (counter prn)                                             ;; prints 0
  (counter prn)                                             ;; prints 1
  (counter prn)                                             ;; prints 2
  )
