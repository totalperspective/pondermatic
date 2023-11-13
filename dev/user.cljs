(ns user
  (:require [pondermatic.db :as db]
            [pondermatic.rules :as rules]
            [pondermatic.flow :as f]
            [portal.api :as p]
            [asami.core :as d]))

(defonce _ (let [p (p/open {:launcher :vs-code})]
             (add-tap #'p/submit)
             p)) ; Add portal as a tap> target

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
    (f/run (db/transact* conn txs))
    (f/run (db/q query db)))
  (d/delete-database db-uri))

(defonce reload? (atom 0))

(when @reload?
  (-main))

(swap! reload? inc)

(rules/run-test)
