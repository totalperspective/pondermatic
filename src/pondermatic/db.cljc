(ns pondermatic.db
  (:require [asami.core :as d]
            [asami.datom :as datom]
            [pondermatic.shell :as sh :refer [|> |< |<=]]
            [pondermatic.flow :as f]
            [pyramid.pull :as pp]
            [pyramid.core :as p]
            [asami.memory]
            [hyperfiddle.rcf :refer [tests]]
            [clojure.pprint]))

(defn name->mem-uri [db-name]
  (str "asami:mem://" db-name))

(defn transactor
  [{:keys [::db-uri]} tx]
  (when-not (= tx sh/done)
    (-> db-uri
        (d/connect)
        (d/transact tx)
        deref
        (update :tx-data (partial mapv datom/as-vec))
        (assoc ::db-uri db-uri))))

(defn ->conn [db-uri]
  (->> db-uri
       (assoc {} ::db-uri)
       (sh/engine transactor)
       sh/actor))

(defn q [query]
  (|<= (map :db-after)
       (map (partial d/q query))))

(defn entity [id]
  (|<= (map :db-after)
       (map #(d/entity % id))))

(defn upsert-name [attr]
  (symbol (str attr "'")))

(defn lookup-id [db [attr val]]
  (d/q '[:find ?id .
         :in $ ?attr ?val
         :where
         [?id ?attr ?val]]
       db attr val))

(defn lookup-entity [db lookup-ref not-found]
  (if-let [id (lookup-id db lookup-ref)]
    (d/entity db id)
    not-found))

(defn pull [db eql]
  (p/pull db eql))

(extend-protocol pp/IPullable
  asami.memory.MemoryDatabase
  (resolve-ref
    ([p lookup-ref]
     (lookup-entity p lookup-ref nil))
    ([p lookup-ref not-found]
     (lookup-entity p lookup-ref not-found))))

(tests
 (let [db-uri (name->mem-uri "test")
       conn (->conn db-uri)
       first-movies [{:db/ident :first
                      :movie/title "Explorers"
                      :movie/genre "adventure/comedy/family"
                      :movie/release-year 1985}
                     {:movie/title "Demolition Man"
                      :movie/genre "action/sci-fi/thriller"
                      :movie/release-year 1993}
                     {:movie/title "Johnny Mnemonic"
                      :movie/genre "cyber-punk/action"
                      :movie/release-year 1995}
                     {:movie/title "Toy Story"
                      :movie/genre "animation/adventure"
                      :movie/release-year 1995}]]
   (-> conn
       (|< (q '[:find ?movie-title
                :where [?m :movie/title ?movie-title]]))
       (f/drain :movie-titles))
   (-> conn
       (|> {:tx-data first-movies})
       (|> sh/done)))
 (let [conn (d/connect (name->mem-uri "test"))
       db (d/db conn)]
   (lookup-entity db [:db/ident :other] ::not-found) := ::not-found
   (-> (lookup-entity db [:movie/title "Explorers"] ::not-found) :movie/title) := "Explorers"
   (-> (lookup-entity db [:db/ident :first] ::not-found) :movie/title) := "Explorers"
   (-> db
       (pull [{[:db/ident :first] [:movie/title :movie/release-year]}])
       (get [:db/ident :first]))
   := #:movie{:title "Explorers", :release-year 1985}))
