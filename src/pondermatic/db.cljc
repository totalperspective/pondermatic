(ns pondermatic.db
  (:require [asami.core :as d]
            [asami.datom :as datom]
            [pondermatic.shell :as a :refer [|> |< |<=]]
            [pondermatic.flow :as f]))

(defn name->mem-uri [db-name]
  (str "asami:mem://" db-name))

(defn transactor
  [{:keys [::db-uri]} tx]
  (-> db-uri
      (d/connect)
      (d/transact tx)
      deref
      (update :tx-data (partial mapv datom/as-vec))
      (assoc ::db-uri db-uri)))

(defn ^:export ->conn [db-uri]
  (->> db-uri
       (assoc {} ::db-uri)
       (a/engine transactor)
       a/actor))

(defn ^:export q [query]
  (|<= (map :db-after)
       (map (partial d/q query))))

(defn ^:export entity [id]
  (|<= (map :db-after)
       (map #(d/entity % id))))

(defn ^:export upsert-name [attr]
  (symbol (str attr "'")))

(defn ^:export run-test []
  (let [conn (-> "test" name->mem-uri ->conn)
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
        (|> a/done))))
