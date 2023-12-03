(ns pondermatic.engine
  (:require [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]
            [pondermatic.rules :as rules]
            [pondermatic.portal :as p]
            [pondermatic.db :as db]
            [pondermatic.rules.production :as prp]
            [odoyle.rules :as o]
            [hasch.core :as h]
            [missionary.core :as m]
            [asami.core :as d]
            [pondermatic.flow :as f]
            [clojure.walk :as w]))

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
  (tap> msg)
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
                retractions (sort-by first (datums false))]
            (tap> {:assertions (p/table assertions)
                   :retractions (p/table retractions)})
            (when (seq retractions)
              (sh/|> rule-session (rules/retract* retractions)))
            (when (seq assertions)
              (sh/|> rule-session (rules/insert* assertions)))))))))

(defn add-base-rules [conn rules]
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
           [?id :rule/when ?when-id]
           [?id :rule/then ?then-id]
           :then
           (let [rule {::type ::rule-info
                       ::hash (h/uuid5 (h/edn-hash [?ident ?when-id ?then-id]))}]
             (sh/|> rules (rules/insert ?ident rule)))]
          ::rules
          [:what
           [?id ::type ::rule-info]
           [?id ::hash ?hash {:then not=}]]})
        rules< (flow/split (sh/|< rules (rules/query ::rules)))]
    (reduce (fn [rules rule]
              (sh/|> rules (rules/add-rule rule)))
            rules
            ruleset)
    (flow/drain (m/ap (let [{:keys [?id]} (m/?> rules<)
                            db (db/db! conn)
                            {:keys [:rule/when :rule/then]} (db/lookup-entity db [:db/ident ?id] :nested? true)
                            what (prp/pattern->what when)
                            entitiy-lvars (->> what
                                               (map first)
                                               (filter symbol?)
                                               distinct)
                            rule (o/->rule
                                  ?id
                                  {:what what
                                   :then-finally
                                   (fn then-finally [session]
                                     (let [matches (o/query-all session ?id)
                                           bindings (map (partial reduce-kv (fn [m k v]
                                                                              (assoc m (symbol k) v))
                                                                  {})
                                                         matches)
                                           ids (->> bindings
                                                    (mapcat (fn [match]
                                                              (prn match)
                                                              (map (partial get match) entitiy-lvars)))
                                                    (remove nil?)
                                                    distinct)
                                           db (db/db! conn)
                                           entities (reduce (fn [m id]
                                                              (assoc m id (d/entity db id)))
                                                            {}
                                                            ids)
                                           production (map (fn [b]
                                                             (prp/unify-pattern then (assoc b 'entities entities)))
                                                           bindings)]
                                       (tap> {:rule ?id
                                              :type (if (:local/id then)
                                                      ::local
                                                      ::db)
                                              :production production})
                                       (if (:local/id then)
                                         (sh/|> rules (rules/insert* (map (juxt :local/id identity)
                                                                          production)))
                                         (sh/|> conn {:tx-data production}))))})]
                        (tap> {::add-rule (p/table what)})
                        (sh/|> rules (rules/add-rule rule))
                        rule))
                ::update-rules)))

(defn ->engine [conn rules]
  (add-base-rules conn rules)
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

(defn conn*> [engine]
  (m/sp
   (-> engine
       (sh/|!> ::conn)
       m/?
       (sh/|!> ::db/db-uri)
       m/?
       d/connect)))

(defn rule-atom [engine]
  (let [atom (atom nil)]
    (f/run (m/sp (let [rules (m/? (rules> engine))]
                   (prn rules)
                   (sh/->atom rules atom))))
    atom))

(defmethod dispatch :->db [{:keys [::conn] :as e} [_ data]]
  (sh/|> conn {:tx-data data})
  e)

(defmethod dispatch :->rules [{:keys [::rules] :as e} [_ msg]]
  (sh/|> rules msg)
  e)
