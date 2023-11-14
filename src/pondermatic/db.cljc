(ns pondermatic.db
  (:require [asami.core :as d]
            [pondermatic.actor :as a :refer [|> |< |<=]]
            [pondermatic.flow :as f]))

(defn name->mem-uri [db-name]
  (str "asami:mem://" db-name))

(defn transactor
  [{:keys [::conn]} tx]
  (-> conn
      (d/transact tx)
      deref
      (assoc ::conn conn)))

(defn ->conn [db-uri]
  (->> (d/connect db-uri)
       (assoc {} ::conn)
       (a/engine transactor)
       a/actor))

(defn q [query]
  (|<= (map :db-after)
       (map (partial d/q query))))

(defn run-test []
  (let [conn (-> "test" name->mem-uri ->conn)
        first-movies [{:movie/title "Explorers"
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
        (f/drain ::movie-titles))
    (-> conn
        (|> {:tx-data first-movies})
        (|> a/done))))
