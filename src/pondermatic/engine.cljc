(ns pondermatic.engine
  (:refer-clojure :exclude [import])
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]
            [pondermatic.rules :as rules]
            [pondermatic.db :as db]
            [pondermatic.rules.production :as prp]
            [odoyle.rules :as o]
            [missionary.core :as m]
            [asami.core :as d]
            [clojure.walk :as w]
            [portal.console :as log]
            [pondermatic.eval :as pe]
            [pondermatic.portal.utils :as p.util]
            #_{:clj-kondo/ignore [:unused-referred-var]}
            [pondermatic.data :refer [uuid-hash]]))

(def type-name ::type)
(def rule-type ::rule)

(def !conns (atom {}))

(defn datum->eav [[e a v]]
  [e a v])

(def db-diff
  (sh/|<=  (map :tx-data)
           (map #(group-by last %))
           (map #(update % true (partial mapv datum->eav)))
           (map #(update % false (partial mapv datum->eav)))))

(defn msg-type [_ cmd _]
  (first cmd))

(defmulti dispatch msg-type)

(defn engine-process [{:keys [::conn ::rules ::dispose:db=>rules ::iteration] :as env} msg]
  (log/debug msg)
  (if (= msg sh/done)
    (do
      (sh/|> conn sh/done)
      (sh/|> rules sh/done)
      (dispose:db=>rules))
    (let [q-conn? (sh/quiescent? conn)
          q-rules? (sh/quiescent? rules)]
      (vary-meta
       (assoc
        (if (map? msg)
          (let [{:keys [:cb]} msg
                msg (dissoc msg :cb)]
            (reduce (fn [env msg]
                      (vary-meta
                       (dispatch env msg cb)
                       merge (or (meta cb) {})))
                    env
                    (seq msg)))
          (dispatch env msg))
        ::actors {::conn? q-conn?
                  ::rules? q-rules?}
        ::iteration (if (and q-conn? q-rules?)
                      0
                      (inc (if (number? iteration)
                             iteration
                             0))))
       merge
       {::sh/safe-keys [::iteration ::actors]}))))

(defn kw->ds [data]
  (w/postwalk (fn [node]
                #_{:clj-kondo/ignore [:unresolved-symbol]}
                (cond
                  (= :a/empty-list node) []
                  (= :a/nil node) nil
                  :else node))
              data))

(defn db=>rules [conn rule-session]
  (-> conn
      (sh/|< db-diff)
      (flow/drain-using
       {::flow :db=>rules}
       (flow/tapper
        (fn update-session [datums]
          (let [datums (kw->ds datums)
                assertions (sort-by first (datums true))
                assert-attrs (into #{} (map (partial take 2) assertions))
                retractions (remove (fn [[e a _]]
                                      (assert-attrs [e a]))
                                    (sort-by first (datums false)))]
            (log/debug {:assertions (p.util/table assertions)
                        :retractions (p.util/table retractions)})
            (when (seq retractions)
              (sh/|> rule-session (rules/retract* retractions)))
            (when (seq assertions)
              (sh/|> rule-session (rules/insert* assertions)))))))))

(defn ->then-finally [?id entity-lvars then rules]
  (fn then-finally [session]
    (let [[{:keys [db-uri]}] (o/query-all session ::db-uri)
          conn (get @!conns db-uri)
          matches (o/query-all session ?id)
          bindings (map (partial reduce-kv (fn [m k v]
                                             (assoc m (symbol k) v))
                                 {})
                        matches)
          ids (->> bindings
                   (mapcat (fn [match]
                             (map (partial get match) entity-lvars)))
                   (remove nil?)
                   distinct)
          db (db/db! conn)
          entities (reduce (fn [m id]
                             (assoc m id (d/entity db id)))
                           {}
                           ids)
          production (prp/unify* then bindings {:id ?id :entities entities})
          local? (-> production
                     first
                     :local/id)]
      (log/debug {:rule ?id
                  :type (if local?
                          ::local
                          ::db)
                  :production production})
      (if local?
        (sh/|> rules (rules/insert* (map (juxt :local/id identity)
                                         production)))
        (sh/|> conn {:tx-data production})))))

(defn- ->rule [?id conn rules env]
  (let [db (db/db! conn)
        {:keys [:rule/when :rule/then]} (db/lookup-entity db [:db/ident ?id] :nested? true)
        what (prp/compile-what when)
        entity-lvars (->> what
                          (map first)
                          (filter symbol?)
                          distinct)
        preds (prp/compile-when when {'scope env})
        pred-l-vars (->> preds
                         (map flatten)
                         flatten
                         (filter symbol?)
                         distinct
                         (filter (fn [sym]
                                   (= \? (-> sym str first)))))
        when-fn (fn [_session match]
                  (->> pred-l-vars
                       (map keyword)
                       (map match)
                       (zipmap pred-l-vars)
                       (prp/unify-pattern preds)
                       (map pe/eval-string)
                       (every? identity)))
        rule-spec {:what what
                   :when when-fn
                   :then-finally
                   (->then-finally ?id entity-lvars then rules)}
        rule (o/->rule ?id rule-spec)]
    rule))

(defn retract-entity-tx [db ids]
  (d/q '[:find :db/retract ?id ?a ?v :where [?id ?a ?v] :in $ [?id ...]]
       db ids))

#_{:clj-kondo/ignore [:unused-binding]}
(defn add-base-rules [conn rules env]
  (let [ruleset
        (o/ruleset
         {::db-uri
          [:what
           [::db ::db-uri db-uri]]
          ::tombstones
          [:what
           [?id ::tombstone true]
           :then-finally
           (let [[{:keys [db-uri]}] (o/query-all session ::db-uri)
                 conn (get @!conns db-uri)
                 db (db/db! conn)
                 matches (o/query-all session ::tombstones)
                 txes (->> matches
                           (map #(get % '?id))
                           (map (partial retract-entity-tx db))
                           (reduce into []))]
             (sh/|> conn {:tx-data txes}))]
          ::collection-head
          [:what
           [?first-node :a/first ?first-id]
           [?entity-id ?attr ?first-node]
           :when
           (and (keyword? ?first-id)
                (keyword? ?attr)
                (not= "a" (namespace ?attr)))
           :then
           (o/insert! ?first-node {:p/contained-by ?entity-id
                                   :p/attr ?attr
                                   :p/index 1})]
          ::collection-tail
          [:what
           [?node :p/contained-by ?entity-id]
           [?node :p/attr ?attr]
           [?node :p/index ?index]
           [?node :a/rest ?rest-node]
           :then
           (o/insert! ?rest-node {:p/contained-by ?entity-id
                                  :p/attr ?attr
                                  :p/index (inc ?index)})]
          ::update-rule
          [:what
           [?id ::type ::rule]
           [?id :db/ident ?ident]
           [?id :rule/when ?when-id {:then not=}]
           [?id :rule/then ?then-id {:then not=}]
           :then-finally
           (let [matches (o/query-all session ::update-rule)
                 rule-hashes (mapv (fn [{:keys [?ident ?when-id ?then-id]}]
                                     [?ident
                                      {::type ::rule-info
                                       ::hash (uuid-hash [?ident ?when-id ?then-id])}])
                                   matches)]
             (sh/|> rules (rules/insert* rule-hashes)))]
          ::rules
          [:what
           [?id ::type ::rule-info]
           [?id ::hash ?hash {:then not=}]
           :then-finally
           (try
             (let [matches (o/query-all session ::rules)
                   rule* (map (fn [{:keys [?id]}]
                                (->rule ?id conn rules env))
                              matches)]
               (sh/|> rules (rules/add-rules rule*)))
             (catch #?(:clj Exception :cljs js/Error) e
               (log/error e)))]})]
    (sh/|> rules (rules/add-rules ruleset))))

(defn register-conn! [conn rules]
  (let [{:keys [db-uri]} conn]
    (log/trace {::register-conn! db-uri})
    (sh/|> rules (rules/insert ::db {::db-uri db-uri}))
    (swap! !conns assoc db-uri conn)))

(defn ->engine [conn rules]
  (add-base-rules conn rules '{!= not=})
  (register-conn! conn rules)
  (let [dispose:db=>rules (db=>rules conn rules)
        session {::conn conn
                 ::rules rules
                 ::dispose:db=>rules dispose:db=>rules}]
    (->> session
         (sh/engine engine-process)
         (sh/actor ::prefix))))

(defn conn> [engine]
  (sh/|!> engine ::conn))

(defn rules> [engine]
  (sh/|!> engine ::rules))

(defn quiescent?> [engine]
  (sh/|!> engine (comp (fnil deref (atom false)) ::quiescent?)))

(defn clone> [engine]
  (m/sp
   (let [conn (m/? (conn> engine))
         rules (m/? (rules> engine))
         conn' (m/? (db/clone> conn))
         rules' (m/? (rules/clone> rules))
         dispose:db=>rules (db=>rules conn' rules')
         session {::conn conn'
                  ::rules rules'
                  ::dispose:db=>rules dispose:db=>rules}]
     (register-conn! conn' rules')
     (log/trace session)
     (->> session
          (sh/engine engine-process)
          (sh/actor ::prefix)))))

(defn db-uri> [engine]
  (m/sp (let [conn (conn> engine)
              db-uri (m/? (sh/|!> conn ::db/db-uri))]
          db-uri)))

(defn rule-atom [engine]
  (let [atom (atom nil)]
    (flow/run (m/sp (let [rules (m/? (rules> engine))]
                      (sh/->atom rules atom)))
              :rule-atom)
    atom))


(defn retract-entities [ids]
  (map (fn [id] [id ::tombstone true]) ids))

(defn retract-entity [id]
  (retract-entities [id]))

(defmethod dispatch :->db [{:keys [::conn] :as e} [_ data] cb]
  (when (seq data)
    (sh/|> conn (with-meta {:tx-data data} {::sh/cb cb})))
  e)

(defmethod dispatch :+>db [{:keys [::conn] :as e} [_ data] cb]
  (when (seq data)
    (sh/|> conn (with-meta {:tx-data (db/upsert data)} {::sh/cb cb})))
  e)

(defn entity->id [entity]
  (cond
    (keyword? entity) entity
    (map? entity) (or (:db/ident entity)
                      (:db/id entity))
    :else nil))

(defmethod dispatch :-!>db [{:keys [::conn] :as e} [_ data] cb]
  (when (seq data)
    (let [ids (->> data
                   (map entity->id)
                   (filter identity))
          db (db/db! conn)
          tx (->> ids
                  (map :db/ident)
                  (filter identity)
                  (retract-entity-tx db))]
      (sh/|> conn (with-meta {:tx-triples tx} {::sh/cb cb}))))
  e)

(defmethod dispatch :!>db [{:keys [::conn] :as e} [_ data] cb]
  (sh/|> conn (if (keyword? data)
                (with-meta (list data) {::sh/cb cb})
                (with-meta data {::sh/cb cb})))
  e)

(defmethod dispatch :->rules [{:keys [::rules] :as e} [_ msg] cb]
  (log/trace {:->rules msg :cb cb})
  (sh/|> rules (with-meta msg {::sh/cb cb}))
  e)

(defmethod dispatch :noop [e _ cb]
  (log/trace {::noop e})
  (with-meta e {::sh/cb cb}))

(defn q>< [engine q & args]
  (m/sp (let [conn (m/? (conn> engine))
              >q (apply db/q> q args)]
          (sh/|< conn >q))))

(defn entity>< [engine ident nested?]
  (m/sp (let [conn (m/? (conn> engine))
              >entity (db/entity> ident :nested? nested?)]
          (sh/|< conn >entity))))

(defn entity< [engine ident nested?]
  (m/sp
   (let [conn (m/? (conn> engine))
         db (m/? (db/db< conn))]
     (when db
       (d/entity db ident nested?)))))

(defn query-rule>< [engine & args]
  (m/sp (let [rules (m/? (rules> engine))
              query-rule< (apply rules/query< args)]
          (sh/|< rules query-rule<))))

(defn t>< [engine]
  (m/sp (let [conn (m/? (conn> engine))
              >t (db/t>)]
          (sh/|< conn >t))))

(defn export< [engine]
  (m/sp
   (let [conn (m/? (conn> engine))]
     (m/? (db/export< conn)))))

(defn import [engine data]
  (sh/|> engine {:!>db {:tx-triples data}}))

