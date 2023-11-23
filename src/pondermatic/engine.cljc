(ns pondermatic.engine
  (:require [pondermatic.shell :as sh]
            [pondermatic.flow :as flow]
            [pondermatic.rules :as rules]))

(defn datum->eav [[e a v]]
  [e a v])

(def db-diff
  (sh/|<=  (map :tx-data)
           (map #(group-by last %))
           (map #(update % true (partial mapv datum->eav)))
           (map #(update % false (partial mapv datum->eav)))))

(defn engine [{:keys [::conn ::rules ::dispose:db=>rules] :as env} msg]
  (if (= msg sh/done)
    (do
      (sh/|> conn sh/done)
      (sh/|> rules sh/done)
      (dispose:db=>rules))
    (do
      env)))

(defn db=>rules [conn rules]
  (-> conn
      (sh/|< db-diff)
      (flow/drain-using
       (fn [_ datums]
         (let [assertions (datums true)
               retractions (datums false)]
           (sh/|> rules (rules/retract* retractions))
           (sh/|> rules (rules/insert* assertions)))))))

(defn ^:export ->engine [conn rules]
  (let [dispose:db=>rules (db=>rules conn rules)]
    (->> (hash-map ::conn conn
                   ::rules rules
                   ::dispose:db=>rules dispose:db=>rules)
         (sh/engine engine)
         sh/actor)))
