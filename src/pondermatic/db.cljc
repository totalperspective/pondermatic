(ns pondermatic.db
  (:require [asami.core :as d]
            [asami.datom :as datom]
            [pondermatic.shell :as sh :refer [|> |< |<=]]
            [pondermatic.flow :as f]
            [pyramid.pull :as pp]
            [pyramid.core :as p]
            [asami.memory]
            [hyperfiddle.rcf :refer [tests]]
            [clojure.walk :as w]
            [portal.console :as log]
            [pondermatic.reader :as pr]
            [missionary.core :as m]))

(defn name->mem-uri [db-name]
  (str "asami:mem://" db-name))

(defn idents [tx]
  (let [!idents (atom [])]
    (w/postwalk (fn [node]
                  #_{:clj-kondo/ignore [:unresolved-symbol]}
                  (if (map-entry? node)
                    (let [[attr val] node]
                      (when (= attr :db/ident)
                        (swap! !idents conj val))
                      node)
                    node))
                tx)
    @!idents))

(defn transactor
  [{:keys [::db-uri]} tx]
  (when-not (= tx sh/done)
    (let [tx (update tx :tx-data (partial remove nil?))
          conn (d/connect db-uri)
          idents (->> tx
                      :tx-data
                      idents
                      (remove nil?)
                      (map #(do {:db/ident %})))
          ident-tx-data (when (seq idents)
                          (-> conn
                              (d/transact {:tx-data idents})
                              deref
                              :tx-data))
          ident-datoms (vec ident-tx-data)]
      (log/debug tx)
      ;; (log/trace (p.p/table idents))
      (-> conn
          (d/transact tx)
          deref
          (update :tx-data (partial into ident-datoms))
          (update :tx-data (partial mapv datom/as-vec))
          (assoc ::db-uri db-uri)))))

(defn ->conn
  ([db-uri]
   (->conn db-uri false))
  ([db-uri delete-old?]
   (when delete-old?
     (d/delete-database db-uri))
   (assoc (->> db-uri
               (assoc {} ::db-uri)
               (sh/engine transactor)
               sh/actor)
          ::db-uri db-uri)))

(defn clone> [conn & {:keys [db-uri]}]
  (let [src-uri (::db-uri conn)
        db (-> src-uri d/connect d/db)
        uri (or db-uri (str (gensym (str src-uri "-"))))
        conn> (m/dfv)]
    (d/as-connection db uri)
    (conn> (->conn uri))))

(defn db! [{:keys [::db-uri]}]
  (d/db (d/connect db-uri)))

(defn db< [conn]
  (sh/|!> conn :db-after))

(defn ^:private -q [q db args]
  (when db
    (let [result (apply d/q q db args)]
      (log/trace {:db db :q q :args args :result result})
      result)))

(defn q> [query & args]
  (|<= (map :db-after)
       (map #(-q query % args))))

(defn ^:private -entity [db id nested?]
  (when db
    (d/entity db id nested?)))

(defn entity> [id & {:keys [nested?] :or {nested? false}}]
  (|<= (map :db-after)
       (map #(-entity % id nested?))))

(defn upsert-name [attr]
  (pr/-read-string (str attr "'")))

(defn upsert [tx]
  (w/postwalk (fn [node]
                #_{:clj-kondo/ignore [:unresolved-symbol]}
                (if (map-entry? node)
                  (let [[attr val] node]
                    (if (and (keyword? attr) (not= :db/ident attr))
                      [(upsert-name attr) val]
                      [attr val]))
                  node))
              tx))

(defn lookup-id [db [attr val]]
  (d/q '[:find ?id .
         :in $ ?attr ?val
         :where
         [?id ?attr ?val]]
       db attr val))

(defn lookup-entity [db lookup-ref & {:keys [not-found nested?]
                                      :or {not-found nil
                                           nested? false}}]
  (if-let [id (lookup-id db lookup-ref)]
    (d/entity db id nested?)
    not-found))

(defn pull [db eql]
  (p/pull db eql))

(defn export< [db]
  (sh/|!> db #(-> % ::db-uri d/connect d/export-data)))

(extend-protocol pp/IPullable
  asami.memory.MemoryDatabase
  (resolve-ref
    ([p lookup-ref]
     (lookup-entity p lookup-ref nil))
    ([p lookup-ref not-found]
     (lookup-entity p lookup-ref not-found))))

(tests
 (let [db-uri (name->mem-uri (namespace ::test))
       conn (->conn db-uri true)
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
       (|< (q> '[:find ?movie-title
                 :where [?m :movie/title ?movie-title]]))
       (f/drain :movie-titles))
   (-> conn
       (|> {:tx-data first-movies})
       (|> sh/done)))
 (let [conn (d/connect (name->mem-uri "test"))
       db (d/db conn)]
   (lookup-entity db [:db/ident :other] :not-found ::not-found) := ::not-found
   (-> (lookup-entity db [:movie/title "Explorers"]) :movie/title) := "Explorers"
   (-> (lookup-entity db [:db/ident :first]) :movie/title) := "Explorers"
   (-> db
       (pull [{[:db/ident :first] [:movie/title :movie/release-year]}])
       (get [:db/ident :first]))
   := #:movie{:title "Explorers", :release-year 1985}))
