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
            [asami.core :as d]))

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

(defn db=>rules [conn rule-session]
  (-> conn
      (sh/|< db-diff)
      (flow/drain-using
       (flow/tapper
        (fn update-session [datums]
          (let [assertions (sort-by first (datums true))
                retractions (sort-by first (datums false))]
            (tap> {:assertions (p/table assertions)
                   :retractions (p/table retractions)})
            (sh/|> rule-session (rules/retract* retractions))
            (sh/|> rule-session (rules/insert* assertions))))))))

(defn add-base-rules [conn rules]
  (let [ruleset
        (o/ruleset
         {::update-rule
          [:what
           [?id ::type ::rule]
           [?id :db/ident ?ident]
           [?id :rule/name ?name]
           [?id :rule/when ?when-id]
           [?id :rule/then ?then-id]
           :then
           (let [rule {::type ::rule-info
                       ::hash (h/uuid5 (h/edn-hash [?name ?when-id ?then-id]))}]
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
                            {:keys [:rule/when]} (db/lookup-entity db [:db/ident ?id] :nested? true)
                            what (prp/pattern->what when)
                            rule (o/->rule
                                  ?id
                                  {:what what
                                   :then-finally
                                   (fn [_]
                                     (let [vars (->> what
                                                     flatten
                                                     (filter symbol?)
                                                     (map name)
                                                     (remove
                                                      (partial re-matches #"^\?production-id-\d+"))
                                                     (mapv symbol))
                                           db (db/db! conn)
                                           query {:find vars
                                                  :where what}
                                           bindings (map (partial zipmap vars)
                                                         (d/q query db))]
                                       (tap> {?id (p/table bindings)})))})]
                        (tap> {::add-rule (p/table what)})
                        (sh/|> rules (rules/add-rule rule))
                        rule))
                ::rules)))

(defn ->engine [conn rules]
  (add-base-rules conn rules)
  (let [dispose:db=>rules (db=>rules conn rules)]
    (->> (hash-map ::conn conn
                   ::rules rules
                   ::dispose:db=>rules dispose:db=>rules)
         (sh/engine engine)
         sh/actor)))

(defmethod dispatch :db/upsert [{:keys [::conn] :as e} [_ data]]
  (sh/|> conn {:tx-data data})
  e)
