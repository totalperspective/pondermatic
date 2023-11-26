(ns pondermatic.engine
  (:require [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]
            [pondermatic.rules :as rules]
            [pondermatic.portal :as p]))

(defn datum->eav [[e a v]]
  [e a v])

(def db-diff
  (sh/|<=  (map :tx-data)
           (map #(group-by last %))
           (map #(update % true (partial mapv datum->eav)))
           (map #(update % false (partial mapv datum->eav)))))

(defn msg-type [_ cmd]
  (prn cmd)
  (first cmd))

(defmulti dispatch msg-type)

(defn engine [{:keys [::conn ::rules ::dispose:db=>rules] :as env} msg]
  (prn msg)
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
          (let [assertions (datums true)
                retractions (datums false)]
            (tap> {:assertions (p/table assertions)
                   :retractions (p/table retractions)})
            (sh/|> rule-session (rules/retract* retractions))
            (sh/|> rule-session (rules/insert* assertions))))))))

(defn ->engine [conn rules]
  (let [dispose:db=>rules (db=>rules conn rules)]
    (->> (hash-map ::conn conn
                   ::rules rules
                   ::dispose:db=>rules dispose:db=>rules)
         (sh/engine engine)
         sh/actor)))

(defmethod dispatch :db/upsert [{:keys [::conn] :as e} [_ data]]
  (sh/|> conn {:tx-data data})
  e)
