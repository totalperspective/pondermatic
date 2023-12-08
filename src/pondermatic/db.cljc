(ns pondermatic.db
  (:require [asami.core :as d]
            [asami.datom :as datom]
            [pondermatic.shell :as sh :refer [|> |< |<=]]
            [pondermatic.flow :as f]
            [pyramid.pull :as pp]
            [pyramid.core :as p]
            [asami.memory]
            [hyperfiddle.rcf :refer [tests]]
            [missionary.core :as m]
            [clojure.edn :as edn]
            [clojure.walk :as w]
            [pondermatic.portal :as p.p]))

(defn name->mem-uri [db-name]
  (str "asami:mem://" db-name))

(defn idents [tx]
  (let [idents! (atom [])]
    (w/postwalk (fn [node]
                  #_{:clj-kondo/ignore [:unresolved-symbol]}
                  (if (instance? #?(:clj clojure.lang.IMapEntry :cljs cljs.core.MapEntry)  node)
                    (let [[attr val] node]
                      (when (= attr :db/ident)
                        (swap! idents! conj val))
                      node)
                    node))
                tx)
    @idents!))

(defn transactor
  [{:keys [::db-uri]} tx]
  (tap> tx)
  (when-not (= tx sh/done)
    (let [idents (->> tx
                      :tx-data
                      idents
                      (remove nil?)
                      (map #(do {:db/ident %})))
          ident-tx-data (-> db-uri
                            d/connect
                            (d/transact {:tx-data idents})
                            deref
                            :tx-data)]
      (tap> (p.p/table idents))
      (-> db-uri
          d/connect
          (d/transact tx)
          deref
          (update :tx-data (partial concat ident-tx-data))
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

(defn db! [{:keys [::db-uri]}]
  (d/db (d/connect db-uri)))

(defn q [query & args]
  (|<= (map :db-after)
       (map #(apply d/q query % args))))

(defn entity [id & {:keys [nested?] :or {nested? false}}]
  (|<= (map :db-after)
       (map #(d/entity % id nested?))))

(defn upsert-name [attr]
  (edn/read-string (str attr "'")))

(defn upsert [tx]
  (w/postwalk (fn [node]
                #_{:clj-kondo/ignore [:unresolved-symbol]}
                (if (instance? #?(:clj clojure.lang.IMapEntry :cljs cljs.core.MapEntry)  node)
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
   (lookup-entity db [:db/ident :other] :not-found ::not-found) := ::not-found
   (-> (lookup-entity db [:movie/title "Explorers"]) :movie/title) := "Explorers"
   (-> (lookup-entity db [:db/ident :first]) :movie/title) := "Explorers"
   (-> db
       (pull [{[:db/ident :first] [:movie/title :movie/release-year]}])
       (get [:db/ident :first]))
   := #:movie{:title "Explorers", :release-year 1985}))
