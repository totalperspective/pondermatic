(ns pondermatic.engine
  (:refer-clojure :exclude [import])
  (:require [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]
            [pondermatic.rules :as rules]
            [pondermatic.portal.utils :as p]
            [pondermatic.db :as db]
            [pondermatic.rules.production :as prp]
            [odoyle.rules :as o]
            [hasch.core :as h]
            [missionary.core :as m]
            [asami.core :as d]
            [pondermatic.flow :as f]
            [clojure.walk :as w]
            [pondermatic.eval :as pe]
            [pondermatic.portal.utils :as portal]
            [pondermatic.portal.utils :as ppu]
            [portal.console :as log]
            [pondermatic.portal.utils :as p.util]
            [pondermatic.data :refer [uuid-hash]]))

(def type-name ::type)
(def rule-type ::rule)

(defn datum->eav [[e a v]]
  [e a v])

(def db-diff
  (sh/|<=  (map :tx-data)
           (map #(group-by last %))
           (map #(update % true (partial mapv datum->eav)))
           (map #(update % false (partial mapv datum->eav)))))

(defn msg-type [_ cmd]
  (first cmd))

(defmulti dispatch msg-type)

(defn engine [{:keys [::conn ::rules ::dispose:db=>rules] :as env} msg]
  (log/debug msg)
  (if (= msg sh/done)
    (do
      (sh/|> conn sh/done)
      (sh/|> rules sh/done)
      (dispose:db=>rules))
    (if (map? msg)
      (reduce dispatch env (seq msg))
      (dispatch env msg))))

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
       (flow/tapper
        (fn update-session [datums]
          (let [datums (kw->ds datums)
                assertions (sort-by first (datums true))
                assert-attrs (into #{} (map (partial take 2) assertions))
                retractions (remove (fn [[e a _]]
                                      (assert-attrs [e a]))
                                    (sort-by first (datums false)))]
            (log/debug {:assertions (p/table assertions)
                        :retractions (p/table retractions)})
            (when (seq retractions)
              (sh/|> rule-session (rules/retract* retractions)))
            (when (seq assertions)
              (sh/|> rule-session (rules/insert* assertions)))))))))

(defn add-base-rules [conn rules env]
  (let [ruleset
        (o/ruleset
         {::collection-head
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
           :then
           (try
             (log/info {:upsert/rule ?id})
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
                   when-fn (fn [session match]
                             (->> pred-l-vars
                                  (map keyword)
                                  (map match)
                                  (zipmap pred-l-vars)
                                  (prp/unify-pattern preds)
                                  (map str)
                                  (map pe/eval-string)
                                  (every? identity)))
                   rule-spec {:what what
                              :when when-fn
                              :then-finally
                              (fn then-finally [session]
                                (let [matches (o/query-all session ?id)
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
                                    (sh/|> conn {:tx-data production}))))}
                   rule (o/->rule ?id rule-spec)]
               (log/debug {?id (update rule-spec :what ppu/table)})
               (sh/|> rules (rules/add-rule rule))
               rule)
             (catch #?(:clj Exception :cljs js/Error) e
               (log/error e)))]})]
    (sh/|> rules (rules/add-rules ruleset))))

(defn ->engine [conn rules]
  (add-base-rules conn rules '{!= not=})
  (let [dispose:db=>rules (db=>rules conn rules)]
    (->> (hash-map ::conn conn
                   ::rules rules
                   ::dispose:db=>rules dispose:db=>rules)
         (sh/engine engine)
         sh/actor)))

(defn conn> [engine]
  (sh/|!> engine ::conn))

(defn rules> [engine]
  (sh/|!> engine ::rules))

(defn db-uri> [engine]
  (m/sp (let [conn (conn> engine)
              db-uri (m/? (sh/|!> conn ::db/db-uri))]
          db-uri)))

(defn rule-atom [engine]
  (let [atom (atom nil)]
    (f/run (m/sp (let [rules (m/? (rules> engine))]
                   (sh/->atom rules atom))))
    atom))

(defmethod dispatch :->db [{:keys [::conn] :as e} [_ data]]
  (sh/|> conn {:tx-data data})
  e)

(defmethod dispatch :+>db [{:keys [::conn] :as e} [_ data]]
  (sh/|> conn {:tx-data (db/upsert data)})
  e)

(defmethod dispatch :!>db [{:keys [::conn] :as e} [_ data]]
  (sh/|> conn data)
  e)

(defmethod dispatch :->rules [{:keys [::rules] :as e} [_ msg]]
  (sh/|> rules msg)
  e)

(defn q>< [engine q & args]
  (m/sp (let [conn (m/? (conn> engine))
              >q (apply db/q> q args)]
          (flow/updates
           (sh/|< conn >q)))))

(defn entity>< [engine ident nested?]
  (m/sp (let [conn (m/? (conn> engine))
              >entity (db/entity> ident :nested? nested?)]
          (flow/updates
           (sh/|< conn >entity)))))

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

(defn export< [engine]
  (m/sp
   (let [conn (m/? (conn> engine))]
     (prn conn)
     (m/? (db/export< conn)))))

(defn import [engine data]
  (prn :import data)
  (sh/|> engine {:!>db {:tx-triples data}}))
